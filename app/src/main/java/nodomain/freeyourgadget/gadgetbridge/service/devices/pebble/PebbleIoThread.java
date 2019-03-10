/*  Copyright (C) 2015-2019 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, Julien Pivotto, Uwe Hermann

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.ParcelUuid;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.ExternalPebbleJSActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.appmanager.AbstractAppManagerFragment;
import nodomain.freeyourgadget.gadgetbridge.activities.appmanager.AppManagerActivity;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventAppInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventAppManagement;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventAppMessage;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.pebble.GBDeviceEventDataLogging;
import nodomain.freeyourgadget.gadgetbridge.devices.pebble.PBWReader;
import nodomain.freeyourgadget.gadgetbridge.devices.pebble.PebbleInstallable;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceApp;
import nodomain.freeyourgadget.gadgetbridge.service.devices.pebble.ble.PebbleLESupport;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceIoThread;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.PebbleUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import nodomain.freeyourgadget.gadgetbridge.util.WebViewSingleton;

class PebbleIoThread extends GBDeviceIoThread {
    private static final Logger LOG = LoggerFactory.getLogger(PebbleIoThread.class);

    private final Prefs prefs = GBApplication.getPrefs();

    private final PebbleProtocol mPebbleProtocol;
    private final PebbleSupport mPebbleSupport;
    private PebbleKitSupport mPebbleKitSupport;
    private final boolean mEnablePebblekit;

    private boolean mIsTCP = false;
    private BluetoothAdapter mBtAdapter = null;
    private BluetoothSocket mBtSocket = null;
    private Socket mTCPSocket = null; // for emulator
    private InputStream mInStream = null;
    private OutputStream mOutStream = null;
    private PebbleLESupport mPebbleLESupport;

    private boolean mQuit = false;
    private boolean mIsConnected = false;
    private boolean mIsInstalling = false;

    private PBWReader mPBWReader = null;
    private GBDeviceApp mCurrentlyInstallingApp = null;
    private int mAppInstallToken = -1;
    private InputStream mFis = null;
    private PebbleAppInstallState mInstallState = PebbleAppInstallState.UNKNOWN;
    private PebbleInstallable[] mPebbleInstallables = null;
    private int mCurrentInstallableIndex = -1;
    private int mInstallSlot = -2;
    private int mCRC = -1;
    private int mBinarySize = -1;
    private int mBytesWritten = -1;

    private void sendAppMessageJS(GBDeviceEventAppMessage appMessage) {
        sendAppMessage(appMessage);
        if (appMessage.type == GBDeviceEventAppMessage.TYPE_APPMESSAGE) {
            write(mPebbleProtocol.encodeApplicationMessageAck(appMessage.appUUID, (byte) appMessage.id));
        }
    }

    public static void sendAppMessage(GBDeviceEventAppMessage message) {
        final String jsEvent;
        try {
            WebViewSingleton.getInstance().checkAppRunning(message.appUUID);
        } catch (IllegalStateException ex) {
            LOG.warn("Unable to send app message: " + message, ex);
            return;
        }

        // TODO: handle ACK and NACK types with ids
        if (message.type != GBDeviceEventAppMessage.TYPE_APPMESSAGE) {
            jsEvent = (GBDeviceEventAppMessage.TYPE_NACK == GBDeviceEventAppMessage.TYPE_APPMESSAGE) ? "NACK" + message.id : "ACK" + message.id;
            LOG.debug("WEBVIEW received ACK/NACK:" + message.message + " for uuid: " + message.appUUID + " ID: " + message.id);
        } else {
            jsEvent = "appmessage";
        }

        final String appMessage = PebbleUtils.parseIncomingAppMessage(message.message, message.appUUID, message.id);
        LOG.debug("to WEBVIEW: event: " + jsEvent + " message: " + appMessage);
        WebViewSingleton.getInstance().invokeWebview(new WebViewSingleton.WebViewRunnable() {
            @Override
            public void invoke(WebView webView) {
                webView.evaluateJavascript("if (typeof Pebble == 'object') Pebble.evaluate('" + jsEvent + "',[" + appMessage + "]);", new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String s) {
                        //TODO: the message should be acked here instead of in PebbleIoThread
                        LOG.debug("Callback from appmessage: " + s);
                    }
                });
            }
        });
    }

    PebbleIoThread(PebbleSupport pebbleSupport, GBDevice gbDevice, GBDeviceProtocol gbDeviceProtocol, BluetoothAdapter btAdapter, Context context) {
        super(gbDevice, context);
        mPebbleProtocol = (PebbleProtocol) gbDeviceProtocol;
        mBtAdapter = btAdapter;
        mPebbleSupport = pebbleSupport;
        mEnablePebblekit = prefs.getBoolean("pebble_enable_pebblekit", false);
        mPebbleProtocol.setAlwaysACKPebbleKit(prefs.getBoolean("pebble_always_ack_pebblekit", false));
        mPebbleProtocol.setEnablePebbleKit(mEnablePebblekit);
    }

    private int readWithException(InputStream inputStream, byte[] buffer, int byteOffset, int byteCount) throws IOException {
        int ret = inputStream.read(buffer, byteOffset, byteCount);
        if (ret == -1) {
            throw new IOException("broken pipe");
        }
        return ret;
    }

    @Override
    protected boolean connect() {
        String deviceAddress = gbDevice.getAddress();
        GBDevice.State originalState = gbDevice.getState();
        gbDevice.setState(GBDevice.State.CONNECTING);
        gbDevice.sendDeviceUpdateIntent(getContext());
        try {
            // contains only one ":"? then it is addr:port
            int firstColon = deviceAddress.indexOf(":");
            if (firstColon == deviceAddress.lastIndexOf(":")) {
                mIsTCP = true;
                InetAddress serverAddr = InetAddress.getByName(deviceAddress.substring(0, firstColon));
                mTCPSocket = new Socket(serverAddr, Integer.parseInt(deviceAddress.substring(firstColon + 1)));
                mInStream = mTCPSocket.getInputStream();
                mOutStream = mTCPSocket.getOutputStream();
            } else {
                mIsTCP = false;
                if (gbDevice.getVolatileAddress() != null && prefs.getBoolean("pebble_force_le", false)) {
                    deviceAddress = gbDevice.getVolatileAddress();
                }
                BluetoothDevice btDevice = mBtAdapter.getRemoteDevice(deviceAddress);
                if (btDevice.getType() == BluetoothDevice.DEVICE_TYPE_LE) {
                    LOG.info("This is a Pebble 2 or Pebble-LE/Pebble Time LE, will use BLE");
                    mInStream = new PipedInputStream();
                    mOutStream = new PipedOutputStream();
                    mPebbleLESupport = new PebbleLESupport(this.getContext(), btDevice, (PipedInputStream) mInStream, (PipedOutputStream) mOutStream);
                } else {
                    ParcelUuid uuids[] = btDevice.getUuids();
                    if (uuids == null) {
                        return false;
                    }
                    for (ParcelUuid uuid : uuids) {
                        LOG.info("found service UUID " + uuid);
                    }

                    final UUID UuidSDP = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
                    mBtSocket = btDevice.createRfcommSocketToServiceRecord(UuidSDP);

                    //mBtSocket = btDevice.createRfcommSocketToServiceRecord(uuids[0].getUuid());
                    mBtSocket.connect();
                    mInStream = mBtSocket.getInputStream();
                    mOutStream = mBtSocket.getOutputStream();
                }
            }
            if (GBApplication.getGBPrefs().isBackgroundJsEnabled()) {
                Intent startIntent = new Intent(getContext(), ExternalPebbleJSActivity.class);
                startIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startIntent.putExtra(ExternalPebbleJSActivity.START_BG_WEBVIEW, true);
                getContext().startActivity(startIntent);
            } else {
                LOG.debug("Not enabling background Webview, is disabled in preferences.");
            }
        } catch (IOException e) {
            LOG.warn("error while connecting: " + e.getMessage(), e);
            gbDevice.setState(originalState);
            gbDevice.sendDeviceUpdateIntent(getContext());

            mInStream = null;
            mOutStream = null;
            mBtSocket = null;
            return false;
        }

        mPebbleProtocol.setForceProtocol(prefs.getBoolean("pebble_force_protocol", false));

        mIsConnected = true;
        write(mPebbleProtocol.encodeFirmwareVersionReq());
        gbDevice.setState(GBDevice.State.CONNECTED);
        gbDevice.sendDeviceUpdateIntent(getContext());

        return true;
    }

    @Override
    public void run() {
        mIsConnected = connect();
        if (!mIsConnected) {
            if (GBApplication.getGBPrefs().getAutoReconnect() && !mQuit) {
                gbDevice.setState(GBDevice.State.WAITING_FOR_RECONNECT);
                gbDevice.sendDeviceUpdateIntent(getContext());
            }
            return;
        }

        byte[] buffer = new byte[8192];
        enablePebbleKitSupport(true);
        mQuit = false;
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
                            } else if (mPBWReader.isLanguage() || mPebbleProtocol.mFwMajor >= 3) {
                                finishInstall(false); // FIXME: don't know yet how to detect success
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
                int bytes = readWithException(mInStream, buffer, 0, 4);

                while (bytes < 4) {
                    bytes += readWithException(mInStream, buffer, bytes, 4 - bytes);
                }

                ByteBuffer buf = ByteBuffer.wrap(buffer);
                buf.order(ByteOrder.BIG_ENDIAN);
                short length = buf.getShort();
                short endpoint = buf.getShort();
                if (length < 0 || length > 8192) {
                    LOG.info("invalid length " + length);
                    while (mInStream.available() > 0) {
                        readWithException(mInStream, buffer, 0, buffer.length); // read all
                    }
                    continue;
                }

                bytes = readWithException(mInStream, buffer, 4, length);
                while (bytes < length) {
                    bytes += readWithException(mInStream, buffer, bytes + 4, length - bytes);
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
                if (e.getMessage() != null && (e.getMessage().equals("broken pipe") || e.getMessage().contains("socket closed"))) { //FIXME: this does not feel right
                    LOG.info(e.getMessage());
                    mIsConnected = false;
                    int reconnectAttempts = prefs.getInt("pebble_reconnect_attempts", 10);
                    if (!mQuit && GBApplication.getGBPrefs().getAutoReconnect() && reconnectAttempts > 0) {
                        gbDevice.setState(GBDevice.State.WAITING_FOR_RECONNECT);
                        gbDevice.sendDeviceUpdateIntent(getContext());

                        long delaySeconds = 1;
                        while (reconnectAttempts-- > 0 && !mQuit && !mIsConnected) {
                            LOG.info("Trying to reconnect (attempts left " + reconnectAttempts + ")");
                            mIsConnected = connect();
                            if (!mIsConnected) {
                                try {
                                    Thread.sleep(delaySeconds * 1000);
                                } catch (InterruptedException ignored) {
                                }
                                if (delaySeconds < 64) {
                                    delaySeconds *= 2;
                                }
                            }
                        }
                    }
                    if (!mIsConnected) {
                        mBtSocket = null;
                        LOG.info("Bluetooth socket closed, will quit IO Thread");
                        break;
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
            mBtSocket = null;
        }

        enablePebbleKitSupport(false);

        if (mQuit) {
            gbDevice.setState(GBDevice.State.NOT_CONNECTED);
        } else {
            gbDevice.setState(GBDevice.State.WAITING_FOR_RECONNECT);
        }

        if (GBApplication.getGBPrefs().isBackgroundJsEnabled()) {
            WebViewSingleton.getInstance().disposeWebView();
        }

        gbDevice.sendDeviceUpdateIntent(getContext());
    }

    private void enablePebbleKitSupport(boolean enable) {
        if (enable && mEnablePebblekit) {
            mPebbleKitSupport = new PebbleKitSupport(getContext(), PebbleIoThread.this, mPebbleProtocol);
        } else {
            if (mPebbleKitSupport != null) {
                mPebbleKitSupport.close();
                mPebbleKitSupport = null;
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
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {
        }
    }

    @Override
    synchronized public void write(byte[] bytes) {
        if (bytes == null) {
            return;
        }
        // on FW < 3.0 block writes if app installation in in progress
        if (!mIsConnected || (mPebbleProtocol.mFwMajor < 3 && mIsInstalling && mInstallState != PebbleAppInstallState.WAIT_SLOT)) {
            return;
        }
        write_real(bytes);
    }

    // FIXME: parts are supporsed to be generic code
    private boolean evaluateGBDeviceEventPebble(GBDeviceEvent deviceEvent) {

        if (deviceEvent instanceof GBDeviceEventVersionInfo) {
            if (prefs.getBoolean("datetime_synconconnect", true)) {
                LOG.info("syncing time");
                write(mPebbleProtocol.encodeSetTime());
            }
            write(mPebbleProtocol.encodeEnableAppLogs(prefs.getBoolean("pebble_enable_applogs", false)));
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
                                installApp(Uri.fromFile(new File(PebbleUtils.getPbwCacheDir(),appMgmt.uuid.toString() + ".pbw")), appMgmt.token);
                            } catch (IOException e) {
                                LOG.error("Error installing app: " + e.getMessage(), e);
                            }
                            break;
                        default:
                            break;
                    }
                    break;
                case START:
                    LOG.info("got GBDeviceEventAppManagement START event for uuid: " + appMgmt.uuid);
                    if (GBApplication.getGBPrefs().isBackgroundJsEnabled()) {
                        if (mPebbleProtocol.hasAppMessageHandler(appMgmt.uuid)) {
                            WebViewSingleton.getInstance().stopJavascriptInterface();
                        } else {
                            WebViewSingleton.getInstance().runJavascriptInterface(gbDevice, appMgmt.uuid);
                        }
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
            if (GBApplication.getGBPrefs().isBackgroundJsEnabled()) {
                sendAppMessageJS((GBDeviceEventAppMessage) deviceEvent);
            }
            if (mEnablePebblekit) {
                LOG.info("Got AppMessage event");
                if (mPebbleKitSupport != null && ((GBDeviceEventAppMessage) deviceEvent).type == GBDeviceEventAppMessage.TYPE_APPMESSAGE) {
                    mPebbleKitSupport.sendAppMessageIntent((GBDeviceEventAppMessage) deviceEvent);
                }
            }
        } else if (deviceEvent instanceof GBDeviceEventDataLogging) {
            if (mEnablePebblekit) {
                LOG.info("Got Datalogging event");
                if (mPebbleKitSupport != null) {
                    mPebbleKitSupport.sendDataLoggingIntent((GBDeviceEventDataLogging) deviceEvent);
                }
            }
        }

        return false;
    }

    private void setToken(int token) {
        mAppInstallToken = token;
    }

    private void setInstallSlot(int slot) {
        if (mIsInstalling) {
            mInstallSlot = slot;
        }
    }

    synchronized private void writeInstallApp(byte[] bytes) {
        if (!mIsInstalling) {
            return;
        }
        LOG.info("got " + bytes.length + "bytes for writeInstallApp()");
        write_real(bytes);
    }

    void installApp(Uri uri, int appId) {
        if (mIsInstalling) {
            return;
        }

        String platformName = PebbleUtils.getPlatformName(gbDevice.getModel());

        try {
            mPBWReader = new PBWReader(uri, getContext(), platformName);
        } catch (FileNotFoundException e) {
            LOG.warn("file not found: " + e.getMessage(), e);
            return;
        } catch (IOException e) {
            LOG.warn("unable to read file: " + e.getMessage(), e);
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
            mCurrentlyInstallingApp = mPBWReader.getGBDeviceApp();
            if (mPebbleProtocol.mFwMajor >= 3 && !mPBWReader.isLanguage()) {
                if (appId == 0) {
                    // only install metadata - not the binaries
                    write(mPebbleProtocol.encodeInstallMetadata(mCurrentlyInstallingApp.getUUID(), mCurrentlyInstallingApp.getName(), mPBWReader.getAppVersion(), mPBWReader.getSdkVersion(), mPBWReader.getFlags(), mPBWReader.getIconId()));
                    write(mPebbleProtocol.encodeAppStart(mCurrentlyInstallingApp.getUUID(), true));
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
                    writeInstallApp(mPebbleProtocol.encodeAppDelete(mCurrentlyInstallingApp.getUUID()));
                }
            }
        }
    }

    private void finishInstall(boolean hadError) {
        if (!mIsInstalling) {
            return;
        }
        if (hadError) {
            GB.updateInstallNotification(getContext().getString(R.string.installation_failed_), false, 0, getContext());
        } else {
            GB.updateInstallNotification(getContext().getString(R.string.installation_successful), false, 0, getContext());
            if (mPebbleProtocol.mFwMajor >= 3) {
                String filenameSuffix;
                if (mCurrentlyInstallingApp != null) {
                    if (mCurrentlyInstallingApp.getType() == GBDeviceApp.Type.WATCHFACE) {
                        filenameSuffix = ".watchfaces";
                    } else {
                        filenameSuffix = ".watchapps";
                    }
                    AppManagerActivity.addToAppOrderFile(gbDevice.getAddress() + filenameSuffix, mCurrentlyInstallingApp.getUUID());
                    Intent refreshIntent = new Intent(AbstractAppManagerFragment.ACTION_REFRESH_APPLIST);
                    LocalBroadcastManager.getInstance(getContext()).sendBroadcast(refreshIntent);
                }
            }
        }
        mInstallState = PebbleAppInstallState.UNKNOWN;

        if (hadError && mAppInstallToken != -1) {
            writeInstallApp(mPebbleProtocol.encodeUploadCancel(mAppInstallToken));
        }

        mPBWReader = null;
        mIsInstalling = false;
        mCurrentlyInstallingApp = null;

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
            } catch (IOException ignored) {
            }
            mBtSocket = null;
        }
        if (mTCPSocket != null) {
            try {
                mTCPSocket.close();
            } catch (IOException ignored) {
            }
            mTCPSocket = null;
        }
        if (mPebbleLESupport != null) {
            mPebbleLESupport.close();
            mPebbleLESupport = null;
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
