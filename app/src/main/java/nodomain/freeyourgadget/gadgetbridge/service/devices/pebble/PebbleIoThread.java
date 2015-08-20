package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.ParcelUuid;
import android.preference.PreferenceManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.ZipInputStream;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventAppInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventAppManagement;
import nodomain.freeyourgadget.gadgetbridge.devices.pebble.PBWReader;
import nodomain.freeyourgadget.gadgetbridge.devices.pebble.PebbleInstallable;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceApp;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceIoThread;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class PebbleIoThread extends GBDeviceIoThread {
    private static final Logger LOG = LoggerFactory.getLogger(PebbleIoThread.class);
    private final PebbleProtocol mPebbleProtocol;
    private final PebbleSupport mPebbleSupport;
    private boolean mIsTCP = false;
    private BluetoothAdapter mBtAdapter = null;
    private BluetoothSocket mBtSocket = null;
    private Socket mTCPSocket = null; // for emulator
    private InputStream mInStream = null;
    private OutputStream mOutStream = null;
    private boolean mQuit = false;
    private boolean mIsConnected = false;
    private boolean mIsInstalling = false;
    private int mConnectionAttempts = 0;

    private PBWReader mPBWReader = null;
    private int mAppInstallToken = -1;
    private ZipInputStream mZis = null;
    private PebbleAppInstallState mInstallState = PebbleAppInstallState.UNKNOWN;
    private PebbleInstallable[] mPebbleInstallables = null;
    private int mCurrentInstallableIndex = -1;
    private int mInstallSlot = -2;
    private int mCRC = -1;
    private int mBinarySize = -1;
    private int mBytesWritten = -1;

    public PebbleIoThread(PebbleSupport pebbleSupport, GBDevice gbDevice, GBDeviceProtocol gbDeviceProtocol, BluetoothAdapter btAdapter, Context context) {
        super(gbDevice, context);
        mPebbleProtocol = (PebbleProtocol) gbDeviceProtocol;
        mBtAdapter = btAdapter;
        mPebbleSupport = pebbleSupport;
    }


    @Override
    protected boolean connect(String btDeviceAddress) {
        GBDevice.State originalState = gbDevice.getState();
        try {
            // contains only one ":"? then it is addr:port
            int firstColon = btDeviceAddress.indexOf(":");
            if (firstColon == btDeviceAddress.lastIndexOf(":")) {
                mIsTCP = true;
                InetAddress serverAddr = InetAddress.getByName(btDeviceAddress.substring(0, firstColon));
                mTCPSocket = new Socket(serverAddr, Integer.parseInt(btDeviceAddress.substring(firstColon + 1)));
                mInStream = mTCPSocket.getInputStream();
                mOutStream = mTCPSocket.getOutputStream();
            } else {
                mIsTCP = false;
                BluetoothDevice btDevice = mBtAdapter.getRemoteDevice(btDeviceAddress);
                ParcelUuid uuids[] = btDevice.getUuids();
                mBtSocket = btDevice.createRfcommSocketToServiceRecord(uuids[0].getUuid());
                mBtSocket.connect();
                mInStream = mBtSocket.getInputStream();
                mOutStream = mBtSocket.getOutputStream();
            }
        } catch (IOException e) {
            e.printStackTrace();
            gbDevice.setState(originalState);
            mInStream = null;
            mOutStream = null;
            mBtSocket = null;
            return false;
        }

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        mPebbleProtocol.setForceProtocol(sharedPrefs.getBoolean("pebble_force_protocol", false));
        gbDevice.setState(GBDevice.State.CONNECTED);
        gbDevice.sendDeviceUpdateIntent(getContext());

        mIsConnected = true;
        write(mPebbleProtocol.encodeFirmwareVersionReq());
        return true;
    }

    @Override
    public void run() {
        gbDevice.setState(GBDevice.State.CONNECTING);
        gbDevice.sendDeviceUpdateIntent(getContext());

        mIsConnected = connect(gbDevice.getAddress());
        mQuit = !mIsConnected; // quit if not connected

        byte[] buffer = new byte[8192];

        while (!mQuit) {
            try {
                if (mIsInstalling) {
                    switch (mInstallState) {
                        case WAIT_SLOT:
                            if (mInstallSlot == -1) {
                                finishInstall(true); // no slots available
                            } else if (mInstallSlot >= 0) {
                                mInstallState = PebbleAppInstallState.START_INSTALL;
                                continue;
                            }
                            break;
                        case START_INSTALL:
                            LOG.info("start installing app binary");
                            PebbleInstallable pi = mPebbleInstallables[mCurrentInstallableIndex];
                            mZis = mPBWReader.getInputStreamFile(pi.getFileName());
                            mCRC = pi.getCRC();
                            mBinarySize = pi.getFileSize();
                            mBytesWritten = 0;
                            writeInstallApp(mPebbleProtocol.encodeUploadStart(pi.getType(), mInstallSlot, mBinarySize));
                            mAppInstallToken = -1;
                            mInstallState = PebbleAppInstallState.WAIT_TOKEN;
                            break;
                        case WAIT_TOKEN:
                            if (mAppInstallToken != -1) {
                                LOG.info("got token " + mAppInstallToken);
                                mInstallState = PebbleAppInstallState.UPLOAD_CHUNK;
                                continue;
                            }
                            break;
                        case UPLOAD_CHUNK:
                            int bytes = 0;
                            do {
                                int read = mZis.read(buffer, bytes, 2000 - bytes);
                                if (read <= 0) break;
                                bytes += read;
                            } while (bytes < 2000);

                            if (bytes > 0) {
                                GB.updateInstallNotification(getContext().getString(
                                        R.string.installing_binary_d_d, (mCurrentInstallableIndex + 1), mPebbleInstallables.length), true, (int) (((float) mBytesWritten / mBinarySize) * 100), getContext());
                                writeInstallApp(mPebbleProtocol.encodeUploadChunk(mAppInstallToken, buffer, bytes));
                                mBytesWritten += bytes;
                                mAppInstallToken = -1;
                                mInstallState = PebbleAppInstallState.WAIT_TOKEN;
                            } else {
                                mInstallState = PebbleAppInstallState.UPLOAD_COMMIT;
                                continue;
                            }
                            break;
                        case UPLOAD_COMMIT:
                            writeInstallApp(mPebbleProtocol.encodeUploadCommit(mAppInstallToken, mCRC));
                            mAppInstallToken = -1;
                            mInstallState = PebbleAppInstallState.WAIT_COMMIT;
                            break;
                        case WAIT_COMMIT:
                            if (mAppInstallToken != -1) {
                                LOG.info("got token " + mAppInstallToken);
                                mInstallState = PebbleAppInstallState.UPLOAD_COMPLETE;
                                continue;
                            }
                            break;
                        case UPLOAD_COMPLETE:
                            writeInstallApp(mPebbleProtocol.encodeUploadComplete(mAppInstallToken));
                            if (++mCurrentInstallableIndex < mPebbleInstallables.length) {
                                mInstallState = PebbleAppInstallState.START_INSTALL;
                            } else {
                                mInstallState = PebbleAppInstallState.APP_REFRESH;
                            }
                            break;
                        case APP_REFRESH:
                            if (mPBWReader.isFirmware()) {
                                writeInstallApp(mPebbleProtocol.encodeInstallFirmwareComplete());
                                finishInstall(false);
                            } else if (mPebbleProtocol.isFw3x) {
                                finishInstall(false); // FIXME: dont know yet how to detect success
                            } else {
                                writeInstallApp(mPebbleProtocol.encodeAppRefresh(mInstallSlot));
                            }
                            break;
                        default:
                            break;
                    }
                }
                if (mIsTCP) {
                    mInStream.skip(6);
                }
                int bytes = mInStream.read(buffer, 0, 4);

                if (bytes < 4) {
                    continue;
                }
                ByteBuffer buf = ByteBuffer.wrap(buffer);
                buf.order(ByteOrder.BIG_ENDIAN);
                short length = buf.getShort();
                short endpoint = buf.getShort();
                if (length < 0 || length > 8192) {
                    LOG.info("invalid length " + length);
                    while (mInStream.available() > 0) {
                        mInStream.read(buffer); // read all
                    }
                    continue;
                }

                bytes = mInStream.read(buffer, 4, length);
                while (bytes < length) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    bytes += mInStream.read(buffer, bytes + 4, length - bytes);
                }

                if (mIsTCP) {
                    mInStream.skip(2);
                }

                GBDeviceEvent deviceEvent = mPebbleProtocol.decodeResponse(buffer);
                if (deviceEvent == null) {
                    LOG.info("unhandled message to endpoint " + endpoint + " (" + length + " bytes)");
                } else {
                    if (!evaluateGBDeviceEventPebble(deviceEvent)) {
                        mPebbleSupport.evaluateGBDeviceEvent(deviceEvent);
                    }
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                if (e.getMessage().contains("socket closed")) { //FIXME: this does not feel right
                    LOG.info(e.getMessage());
                    gbDevice.setState(GBDevice.State.CONNECTING);
                    gbDevice.sendDeviceUpdateIntent(getContext());

                    while (mConnectionAttempts++ < 10 && !mQuit) {
                        LOG.info("Trying to reconnect (attempt " + mConnectionAttempts + ")");
                        mIsConnected = connect(gbDevice.getAddress());
                        if (mIsConnected)
                            break;
                    }
                    mConnectionAttempts = 0;
                    if (!mIsConnected) {
                        mBtSocket = null;
                        LOG.info("Bluetooth socket closed, will quit IO Thread");
                        mQuit = true;
                    }
                }
            }
        }
        mIsConnected = false;
        if (mBtSocket != null) {
            try {
                mBtSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        mBtSocket = null;
        gbDevice.setState(GBDevice.State.NOT_CONNECTED);
        gbDevice.sendDeviceUpdateIntent(getContext());
    }

    private void write_real(byte[] bytes) {
        try {
            if (mIsTCP) {
                ByteBuffer buf = ByteBuffer.allocate(bytes.length + 8);
                buf.order(ByteOrder.BIG_ENDIAN);
                buf.putShort((short) 0xfeed);
                buf.putShort((short) 1);
                buf.putShort((short) bytes.length);
                buf.put(bytes);
                buf.putShort((short) 0xbeef);
                mOutStream.write(buf.array());
                mOutStream.flush();
            } else {
                mOutStream.write(bytes);
                mOutStream.flush();
            }
        } catch (IOException e) {
        }
    }

    @Override
    synchronized public void write(byte[] bytes) {
        // block writes if app installation in in progress
        if (mIsConnected && (!mIsInstalling || mInstallState == PebbleAppInstallState.WAIT_SLOT)) {
            write_real(bytes);
        }
    }

    // FIXME: parts are supporsed to be generic code
    private boolean evaluateGBDeviceEventPebble(GBDeviceEvent deviceEvent) {

        switch (deviceEvent.eventClass) {
            case VERSION_INFO:
                SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                if (sharedPrefs.getBoolean("datetime_synconconnect", true)) {
                    LOG.info("syncing time");
                    write(mPebbleProtocol.encodeSetTime());
                }
                gbDevice.setState(GBDevice.State.INITIALIZED);
                return false;
            case APP_MANAGEMENT:
                GBDeviceEventAppManagement appMgmt = (GBDeviceEventAppManagement) deviceEvent;
                switch (appMgmt.type) {
                    case DELETE:
                        // right now on the Pebble we also receive this on a failed/successful installation ;/
                        switch (appMgmt.event) {
                            case FAILURE:
                                if (mIsInstalling) {
                                    if (mInstallState == PebbleAppInstallState.WAIT_SLOT) {
                                        // get the free slot
                                        writeInstallApp(mPebbleProtocol.encodeAppInfoReq());
                                    } else {
                                        finishInstall(true);
                                    }
                                } else {
                                    LOG.info("failure removing app");
                                }
                                break;
                            case SUCCESS:
                                if (mIsInstalling) {
                                    if (mInstallState == PebbleAppInstallState.WAIT_SLOT) {
                                        // get the free slot
                                        writeInstallApp(mPebbleProtocol.encodeAppInfoReq());
                                    } else {
                                        finishInstall(false);
                                        // refresh app list
                                        write(mPebbleProtocol.encodeAppInfoReq());
                                    }
                                } else {
                                    LOG.info("successfully removed app");
                                    write(mPebbleProtocol.encodeAppInfoReq());
                                }
                                break;
                            default:
                                break;
                        }
                        break;
                    case INSTALL:
                        switch (appMgmt.event) {
                            case FAILURE:
                                LOG.info("failure installing app"); // TODO: report to Installer
                                finishInstall(true);
                                break;
                            case SUCCESS:
                                setToken(appMgmt.token);
                                break;
                            case REQUEST:
                                LOG.info("APPFETCH request: " + appMgmt.uuid + " / " + appMgmt.token);
                                try {
                                    installApp(Uri.fromFile(new File(FileUtils.getExternalFilesDir() + "/pbw-cache/" + appMgmt.uuid.toString() + ".pbw")), appMgmt.token);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                break;
                            default:
                                break;
                        }
                        break;
                    default:
                        break;
                }
                return true;
            case APP_INFO:
                LOG.info("Got event for APP_INFO");
                GBDeviceEventAppInfo appInfoEvent = (GBDeviceEventAppInfo) deviceEvent;
                setInstallSlot(appInfoEvent.freeSlot);
                return false;
            default:
                return false;
        }
    }

    public void setToken(int token) {
        mAppInstallToken = token;
    }

    public void setInstallSlot(int slot) {
        if (mIsInstalling) {
            mInstallSlot = slot;
        }
    }

    private void writeInstallApp(byte[] bytes) {
        if (!mIsInstalling) {
            return;
        }
        LOG.info("got " + bytes.length + "bytes for writeInstallApp()");
        write_real(bytes);
    }

    public void installApp(Uri uri, int appId) {
        if (mIsInstalling) {
            return;
        }

        mPBWReader = new PBWReader(uri, getContext(), gbDevice.getHardwareVersion().equals("dvt") ? "basalt" : "aplite");
        mPebbleInstallables = mPBWReader.getPebbleInstallables();
        mCurrentInstallableIndex = 0;

        if (mPBWReader.isFirmware()) {
            LOG.info("starting firmware installation");
            mIsInstalling = true;
            mInstallSlot = 0;
            writeInstallApp(mPebbleProtocol.encodeInstallFirmwareStart());
            mInstallState = PebbleAppInstallState.START_INSTALL;

            /*
             * This is a hack for recovery mode, in which the blocking read has no timeout and the
             * firmware installation command does not return any ack.
             * In normal mode we would got at least out of the blocking read call after a while.
             *
             *
             * ... we should really not handle installation from thread that does the blocking read
             *
             */
            writeInstallApp(mPebbleProtocol.encodeGetTime());
        } else {
            GBDeviceApp app = mPBWReader.getGBDeviceApp();
            if (mPebbleProtocol.isFw3x) {
                if (appId == 0) {
                    // only install metadata - not the binaries
                    write(mPebbleProtocol.encodeInstallMetadata(app.getUUID(), app.getName(), mPBWReader.getAppVersion(), mPBWReader.getSdkVersion(), mPBWReader.getFlags(), mPBWReader.getIconId()));
                    GB.toast("To finish installation please start the watchapp on your Pebble", 5, GB.INFO);
                } else {
                    // this came from an app fetch request, so do the real stuff
                    mIsInstalling = true;
                    mInstallSlot = appId;
                    mInstallState = PebbleAppInstallState.START_INSTALL;

                    writeInstallApp(mPebbleProtocol.encodeAppFetchAck());
                }
            } else {
                mIsInstalling = true;
                mInstallState = PebbleAppInstallState.WAIT_SLOT;
                writeInstallApp(mPebbleProtocol.encodeAppDelete(app.getUUID()));
            }
        }
    }

    public void finishInstall(boolean hadError) {
        if (!mIsInstalling) {
            return;
        }
        if (hadError) {
            GB.updateInstallNotification(getContext().getString(R.string.installation_failed_), false, 0, getContext());
        } else {
            GB.updateInstallNotification(getContext().getString(R.string.installation_successful), false, 0, getContext());
        }
        mInstallState = PebbleAppInstallState.UNKNOWN;

        if (hadError && mAppInstallToken != -1) {
            writeInstallApp(mPebbleProtocol.encodeUploadCancel(mAppInstallToken));
        }

        mPBWReader = null;
        mIsInstalling = false;
        if (mZis != null) {
            try {
                mZis.close();
            } catch (IOException e) {
                // ignore
            }
        }
        mZis = null;
        mAppInstallToken = -1;
        mInstallSlot = -2;
    }

    @Override
    public void quit() {
        mQuit = true;
        if (mBtSocket != null) {
            try {
                mBtSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private enum PebbleAppInstallState {
        UNKNOWN,
        WAIT_SLOT,
        START_INSTALL,
        WAIT_TOKEN,
        UPLOAD_CHUNK,
        UPLOAD_COMMIT,
        WAIT_COMMIT,
        UPLOAD_COMPLETE,
        APP_REFRESH,
    }
}