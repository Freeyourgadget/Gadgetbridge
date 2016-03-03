package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.ParcelUuid;
import android.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventAppInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventAppManagement;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventAppMessage;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.pebble.PBWReader;
import nodomain.freeyourgadget.gadgetbridge.devices.pebble.PebbleInstallable;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceApp;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceIoThread;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.PebbleUtils;

public class PebbleIoThread extends GBDeviceIoThread {
    private static final Logger LOG = LoggerFactory.getLogger(PebbleIoThread.class);

    private static final UUID PEBBLE_UUID_RECONNECT = UUID.fromString("00000000-deca-fade-deca-deafdecacafe");
    private static final UUID PEBBLE_UUID_RECONNECT3X = UUID.fromString("a924496e-cc7c-4dff-8a9f-9a76cc2e9d50");

    public static final String PEBBLEKIT_ACTION_PEBBLE_CONNECTED = "com.getpebble.action.PEBBLE_CONNECTED";
    public static final String PEBBLEKIT_ACTION_PEBBLE_DISCONNECTED = "com.getpebble.action.PEBBLE_DISCONNECTED";
    public static final String PEBBLEKIT_ACTION_APP_ACK = "com.getpebble.action.app.ACK";
    public static final String PEBBLEKIT_ACTION_APP_NACK = "com.getpebble.action.app.NACK";
    public static final String PEBBLEKIT_ACTION_APP_RECEIVE = "com.getpebble.action.app.RECEIVE";
    public static final String PEBBLEKIT_ACTION_APP_RECEIVE_ACK = "com.getpebble.action.app.RECEIVE_ACK";
    public static final String PEBBLEKIT_ACTION_APP_RECEIVE_NACK = "com.getpebble.action.app.RECEIVE_NACK";
    public static final String PEBBLEKIT_ACTION_APP_SEND = "com.getpebble.action.app.SEND";
    public static final String PEBBLEKIT_ACTION_APP_START = "com.getpebble.action.app.START";
    public static final String PEBBLEKIT_ACTION_APP_STOP = "com.getpebble.action.app.STOP";

    final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());

    private final PebbleProtocol mPebbleProtocol;
    private final PebbleSupport mPebbleSupport;
    private final boolean mEnablePebblekit;

    private boolean mIsTCP = false;
    private BluetoothAdapter mBtAdapter = null;
    private BluetoothSocket mBtSocket = null;
    private BluetoothServerSocket mBtServerSocket = null;
    private Socket mTCPSocket = null; // for emulator
    private InputStream mInStream = null;
    private OutputStream mOutStream = null;
    private boolean mQuit = false;
    private boolean mIsConnected = false;
    private boolean mIsInstalling = false;

    private PBWReader mPBWReader = null;
    private int mAppInstallToken = -1;
    private InputStream mFis = null;
    private PebbleAppInstallState mInstallState = PebbleAppInstallState.UNKNOWN;
    private PebbleInstallable[] mPebbleInstallables = null;
    private int mCurrentInstallableIndex = -1;
    private int mInstallSlot = -2;
    private int mCRC = -1;
    private int mBinarySize = -1;
    private int mBytesWritten = -1;

    private final BroadcastReceiver mPebbleKitReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            LOG.info("Got action: " + action);
            UUID uuid;
            switch (action) {
                case PEBBLEKIT_ACTION_APP_START:
                case PEBBLEKIT_ACTION_APP_STOP:
                    uuid = (UUID) intent.getSerializableExtra("uuid");
                    if (uuid != null) {
                        write(mPebbleProtocol.encodeAppStart(uuid, action.equals(PEBBLEKIT_ACTION_APP_START)));
                    }
                    break;
                case PEBBLEKIT_ACTION_APP_SEND:
                    int transaction_id = intent.getIntExtra("transaction_id", -1);
                    uuid = (UUID) intent.getSerializableExtra("uuid");
                    String jsonString = intent.getStringExtra("msg_data");
                    LOG.info("json string: " + jsonString);

                    try {
                        JSONArray jsonArray = new JSONArray(jsonString);
                        write(mPebbleProtocol.encodeApplicationMessageFromJSON(uuid, jsonArray));
                        sendAppMessageAck(transaction_id);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                case PEBBLEKIT_ACTION_APP_ACK:
                    // we do not get a uuid and cannot map a transaction id to it, so we ack in PebbleProtocol early
                    /*
                    uuid = (UUID) intent.getSerializableExtra("uuid");
                    int transaction_id = intent.getIntExtra("transaction_id", -1);
                    if (transaction_id >= 0 && transaction_id <= 255) {
                        write(mPebbleProtocol.encodeApplicationMessageAck(uuid, (byte) transaction_id));
                    } else {
                        LOG.warn("illegal transacktion id " + transaction_id);
                    }
                    */
                    break;
            }
        }
    };

    private void sendAppMessageIntent(GBDeviceEventAppMessage appMessage) {
        Intent intent = new Intent();
        intent.setAction(PEBBLEKIT_ACTION_APP_RECEIVE);
        intent.putExtra("uuid", appMessage.appUUID);
        intent.putExtra("msg_data", appMessage.message);
        intent.putExtra("transaction_id", appMessage.id);
        LOG.info("broadcasting to uuid " + appMessage.appUUID + " transaction id: " + appMessage.id + " JSON: " + appMessage.message);
        getContext().sendBroadcast(intent);
    }

    private void sendAppMessageAck(int transactionId) {
        if (transactionId > 0 && transactionId <= 255) {
            Intent intent = new Intent();
            intent.setAction(PEBBLEKIT_ACTION_APP_RECEIVE_ACK);
            intent.putExtra("transaction_id", transactionId);
            LOG.info("broadcasting ACK (transaction id " + transactionId + ")");
            getContext().sendBroadcast(intent);
        }
    }

    public PebbleIoThread(PebbleSupport pebbleSupport, GBDevice gbDevice, GBDeviceProtocol gbDeviceProtocol, BluetoothAdapter btAdapter, Context context) {
        super(gbDevice, context);
        mPebbleProtocol = (PebbleProtocol) gbDeviceProtocol;
        mBtAdapter = btAdapter;
        mPebbleSupport = pebbleSupport;
        mEnablePebblekit = sharedPrefs.getBoolean("pebble_enable_pebblekit", false);
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
                if (uuids == null) {
                    return false;
                }
                for (ParcelUuid uuid : uuids) {
                    LOG.info("found service UUID " + uuid);
                }
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
        enablePebbleKitReceiver(mIsConnected);
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
                            mFis = mPBWReader.getInputStreamFile(pi.getFileName());
                            mCRC = pi.getCRC();
                            mBinarySize = pi.getFileSize();
                            mBytesWritten = 0;
                            writeInstallApp(mPebbleProtocol.encodeUploadStart(pi.getType(), mInstallSlot, mBinarySize, mPBWReader.isLanguage() ? "lang" : null));
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
                                int read = mFis.read(buffer, bytes, 2000 - bytes);
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
                            } else if (mPBWReader.isLanguage() || mPebbleProtocol.isFw3x) {
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

                GBDeviceEvent deviceEvents[] = mPebbleProtocol.decodeResponse(buffer);
                if (deviceEvents == null) {
                    LOG.info("unhandled message to endpoint " + endpoint + " (" + length + " bytes)");
                } else {
                    for (GBDeviceEvent deviceEvent : deviceEvents) {
                        if (deviceEvent == null) {
                            continue;
                        }
                        if (!evaluateGBDeviceEventPebble(deviceEvent)) {
                            mPebbleSupport.evaluateGBDeviceEvent(deviceEvent);
                        }
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
                    mIsConnected = false;
                    int reconnectAttempts = Integer.valueOf(sharedPrefs.getString("pebble_reconnect_attempts", "10"));
                    if (reconnectAttempts > 0) {
                        gbDevice.setState(GBDevice.State.CONNECTING);
                        gbDevice.sendDeviceUpdateIntent(getContext());
                        while (reconnectAttempts-- > 0 && !mQuit && !mIsConnected) {
                            LOG.info("Trying to reconnect (attempts left " + reconnectAttempts + ")");
                            mIsConnected = connect(gbDevice.getAddress());
                        }
                    }
                    if (!mIsConnected && !mQuit) {
                        try {
                            gbDevice.setState(GBDevice.State.WAITING_FOR_RECONNECT);
                            gbDevice.sendDeviceUpdateIntent(getContext());
                            UUID reconnectUUID = mPebbleProtocol.isFw3x ? PEBBLE_UUID_RECONNECT3X : PEBBLE_UUID_RECONNECT;
                            mBtServerSocket = mBtAdapter.listenUsingRfcommWithServiceRecord("PebbleReconnectListener", reconnectUUID);
                            mBtSocket = mBtServerSocket.accept();
                            LOG.info("incoming connection on reconnect uuid (" + reconnectUUID + "), will connect actively");
                            mBtSocket.close();
                            mIsConnected = connect(gbDevice.getAddress());
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            LOG.info("error while reconnecting");
                        }
                    }
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
        enablePebbleKitReceiver(false);
        mBtSocket = null;
        gbDevice.setState(GBDevice.State.NOT_CONNECTED);
        gbDevice.sendDeviceUpdateIntent(getContext());
    }

    private void enablePebbleKitReceiver(boolean enable) {

        if (enable && mEnablePebblekit) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(PEBBLEKIT_ACTION_APP_ACK);
            intentFilter.addAction(PEBBLEKIT_ACTION_APP_NACK);
            intentFilter.addAction(PEBBLEKIT_ACTION_APP_SEND);
            intentFilter.addAction(PEBBLEKIT_ACTION_APP_START);
            intentFilter.addAction(PEBBLEKIT_ACTION_APP_STOP);
            try {
                getContext().registerReceiver(mPebbleKitReceiver, intentFilter);
            } catch (IllegalArgumentException e) {
                // ignore
            }
        } else {
            try {
                getContext().unregisterReceiver(mPebbleKitReceiver);
            } catch (IllegalArgumentException e) {
                // ignore
            }
        }
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
            LOG.error("Error writing.", e);
        }
    }

    @Override
    synchronized public void write(byte[] bytes) {
        if (bytes == null) {
            return;
        }
        // block writes if app installation in in progress
        if (mIsConnected && (!mIsInstalling || mInstallState == PebbleAppInstallState.WAIT_SLOT)) {
            write_real(bytes);
        }
    }

    // FIXME: parts are supporsed to be generic code
    private boolean evaluateGBDeviceEventPebble(GBDeviceEvent deviceEvent) {

        if (deviceEvent instanceof GBDeviceEventVersionInfo) {
            if (sharedPrefs.getBoolean("datetime_synconconnect", true)) {
                LOG.info("syncing time");
                write(mPebbleProtocol.encodeSetTime());
            }
            write(mPebbleProtocol.encodeReportDataLogSessions());
            gbDevice.setState(GBDevice.State.INITIALIZED);
            return false;
        } else if (deviceEvent instanceof GBDeviceEventAppManagement) {
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
                                LOG.error("Error installing app: " + e.getMessage(), e);
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
        } else if (deviceEvent instanceof GBDeviceEventAppInfo) {
            LOG.info("Got event for APP_INFO");
            GBDeviceEventAppInfo appInfoEvent = (GBDeviceEventAppInfo) deviceEvent;
            setInstallSlot(appInfoEvent.freeSlot);
            return false;
        } else if (deviceEvent instanceof GBDeviceEventAppMessage) {
            if (mEnablePebblekit) {
                LOG.info("Got AppMessage event");
                sendAppMessageIntent((GBDeviceEventAppMessage) deviceEvent);
            }
        }

        return false;
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

        if (uri.equals(Uri.parse("fake://health"))) {
            write(mPebbleProtocol.encodeActivateHealth(true));
            write(mPebbleProtocol.encodeSetSaneDistanceUnit(true));
            return;
        }

        String platformName  = PebbleUtils.getPlatformName(gbDevice.getHardwareVersion());

        try {
            mPBWReader = new PBWReader(uri, getContext(), platformName);
        } catch (FileNotFoundException e) {
            LOG.warn("file not found!");
            return;
        }

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
            if (mPebbleProtocol.isFw3x && !mPBWReader.isLanguage()) {
                if (appId == 0) {
                    // only install metadata - not the binaries
                    write(mPebbleProtocol.encodeInstallMetadata(app.getUUID(), app.getName(), mPBWReader.getAppVersion(), mPBWReader.getSdkVersion(), mPBWReader.getFlags(), mPBWReader.getIconId()));
                    write(mPebbleProtocol.encodeAppStart(app.getUUID(), true));
                } else {
                    // this came from an app fetch request, so do the real stuff
                    mIsInstalling = true;
                    mInstallSlot = appId;
                    mInstallState = PebbleAppInstallState.START_INSTALL;

                    writeInstallApp(mPebbleProtocol.encodeAppFetchAck());
                }
            } else {
                mIsInstalling = true;
                if (mPBWReader.isLanguage()) {
                    mInstallSlot = 0;
                    mInstallState = PebbleAppInstallState.START_INSTALL;

                    // unblock HACK
                    writeInstallApp(mPebbleProtocol.encodeGetTime());
                } else {
                    mInstallState = PebbleAppInstallState.WAIT_SLOT;
                    writeInstallApp(mPebbleProtocol.encodeAppDelete(app.getUUID()));
                }
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
        if (mFis != null) {
            try {
                mFis.close();
            } catch (IOException e) {
                // ignore
            }
        }
        mFis = null;
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
        if (mBtServerSocket != null) {
            try {
                mBtServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (mTCPSocket != null) {
            try {
                mTCPSocket.close();
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