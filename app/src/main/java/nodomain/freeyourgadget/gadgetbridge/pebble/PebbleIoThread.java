package nodomain.freeyourgadget.gadgetbridge.pebble;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.ParcelUuid;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.ZipInputStream;

import nodomain.freeyourgadget.gadgetbridge.AppManagerActivity;
import nodomain.freeyourgadget.gadgetbridge.GBCallControlReceiver;
import nodomain.freeyourgadget.gadgetbridge.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.GBDeviceIoThread;
import nodomain.freeyourgadget.gadgetbridge.GBMusicControlReceiver;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceCommand;
import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceCommandAppInfo;
import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceCommandAppManagementResult;
import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceCommandCallControl;
import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceCommandMusicControl;
import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceCommandSendBytes;
import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceCommandVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceProtocol;

public class PebbleIoThread extends GBDeviceIoThread {
    private static final String TAG = PebbleIoThread.class.getSimpleName();
    private static final int NOTIFICATION_ID = 2;

    private enum PebbleAppInstallState {
        UNKNOWN,
        APP_WAIT_SLOT,
        APP_START_INSTALL,
        APP_WAIT_TOKEN,
        APP_UPLOAD_CHUNK,
        APP_UPLOAD_COMMIT,
        APP_WAIT_COMMIT,
        APP_UPLOAD_COMPLETE,
        APP_REFRESH,
    }

    private final PebbleProtocol mPebbleProtocol;

    private BluetoothAdapter mBtAdapter = null;
    private BluetoothSocket mBtSocket = null;
    private InputStream mInStream = null;
    private OutputStream mOutStream = null;
    private boolean mQuit = false;
    private boolean mIsConnected = false;
    private boolean mIsInstalling = false;
    private int mConnectionAttempts = 0;

    /* app installation  */
    private Uri mInstallURI = null;
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

    public static Notification createInstallNotification(String text, boolean ongoing,
                                                         int percentage, Context context) {
        Intent notificationIntent = new Intent(context, AppManagerActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                notificationIntent, 0);

        NotificationCompat.Builder nb = new NotificationCompat.Builder(context)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(text)
                .setTicker(text)

                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setOngoing(ongoing);

        if (ongoing) {
            nb.setProgress(100, percentage, percentage == 0);
        }

        return nb.build();
    }

    public static void updateInstallNotification(String text, boolean ongoing, int percentage, Context context) {
        Notification notification = createInstallNotification(text, ongoing, percentage, context);

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTIFICATION_ID, notification);
    }

    public PebbleIoThread(GBDevice gbDevice, GBDeviceProtocol gbDeviceProtocol, BluetoothAdapter btAdapter, Context context) {
        super(gbDevice, context);
        mPebbleProtocol = (PebbleProtocol) gbDeviceProtocol;
        mBtAdapter = btAdapter;
    }

    @Override
    protected boolean connect(String btDeviceAddress) {
        BluetoothDevice btDevice = mBtAdapter.getRemoteDevice(btDeviceAddress);
        ParcelUuid uuids[] = btDevice.getUuids();
        GBDevice.State originalState = gbDevice.getState();
        try {
            mBtSocket = btDevice.createRfcommSocketToServiceRecord(uuids[0].getUuid());
            mBtSocket.connect();
            mInStream = mBtSocket.getInputStream();
            mOutStream = mBtSocket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
            gbDevice.setState(originalState);
            mInStream = null;
            mOutStream = null;
            mBtSocket = null;
            return false;
        }
        gbDevice.setState(GBDevice.State.CONNECTED);
        gbDevice.sendDeviceUpdateIntent(getContext());

        mIsConnected = true;

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        if (sharedPrefs.getBoolean("datetime_synconconnect", true)) {
            Log.i(TAG, "syncing time");
            write(mPebbleProtocol.encodeSetTime(-1));
        }

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
                        case APP_WAIT_SLOT:
                            if (mInstallSlot == -1) {
                                finishInstall(true); // no slots available
                            } else if (mInstallSlot >= 0) {
                                mInstallState = PebbleAppInstallState.APP_START_INSTALL;
                                continue;
                            }
                            break;
                        case APP_START_INSTALL:
                            if (mPBWReader == null) {
                                mPBWReader = new PBWReader(mInstallURI, getContext());
                                mPebbleInstallables = mPBWReader.getPebbleInstallables();
                                mCurrentInstallableIndex = 0;
                                if (mPBWReader.isFirmware()) {
                                    writeInstallApp(mPebbleProtocol.encodeInstallFirmwareStart());
                                    mInstallSlot = 0;
                                    Log.i(TAG, "starting firmware installation");
                                }
                            }
                            Log.i(TAG, "start installing app binary");
                            PebbleInstallable pi = mPebbleInstallables[mCurrentInstallableIndex];
                            mZis = mPBWReader.getInputStreamFile(pi.getFileName());
                            mCRC = pi.getCRC();
                            mBinarySize = pi.getFileSize();
                            mBytesWritten = 0;
                            writeInstallApp(mPebbleProtocol.encodeUploadStart(pi.getType(), (byte) mInstallSlot, mBinarySize));
                            mInstallState = PebbleAppInstallState.APP_WAIT_TOKEN;
                            break;
                        case APP_WAIT_TOKEN:
                            if (mAppInstallToken != -1) {
                                Log.i(TAG, "got token " + mAppInstallToken);
                                mInstallState = PebbleAppInstallState.APP_UPLOAD_CHUNK;
                                continue;
                            }
                            break;
                        case APP_UPLOAD_CHUNK:
                            int bytes = 0;
                            do {
                                int read = mZis.read(buffer, bytes, 2000 - bytes);
                                if (read <= 0) break;
                                bytes += read;
                            } while (bytes < 2000);

                            if (bytes > 0) {
                                updateInstallNotification(getContext().getString(
                                        R.string.installing_binary_d_d, (mCurrentInstallableIndex + 1), mPebbleInstallables.length), true, (int) (((float) mBytesWritten / mBinarySize) * 100), getContext());
                                writeInstallApp(mPebbleProtocol.encodeUploadChunk(mAppInstallToken, buffer, bytes));
                                mBytesWritten += bytes;
                                mAppInstallToken = -1;
                                mInstallState = PebbleAppInstallState.APP_WAIT_TOKEN;
                            } else {
                                mInstallState = PebbleAppInstallState.APP_UPLOAD_COMMIT;
                                continue;
                            }
                            break;
                        case APP_UPLOAD_COMMIT:
                            writeInstallApp(mPebbleProtocol.encodeUploadCommit(mAppInstallToken, mCRC));
                            mAppInstallToken = -1;
                            mInstallState = PebbleAppInstallState.APP_WAIT_COMMIT;
                            break;
                        case APP_WAIT_COMMIT:
                            if (mAppInstallToken != -1) {
                                Log.i(TAG, "got token " + mAppInstallToken);
                                mInstallState = PebbleAppInstallState.APP_UPLOAD_COMPLETE;
                                continue;
                            }
                            break;
                        case APP_UPLOAD_COMPLETE:
                            writeInstallApp(mPebbleProtocol.encodeUploadComplete(mAppInstallToken));
                            if (++mCurrentInstallableIndex < mPebbleInstallables.length) {
                                mInstallState = PebbleAppInstallState.APP_START_INSTALL;
                            } else {
                                mInstallState = PebbleAppInstallState.APP_REFRESH;
                            }
                            break;
                        case APP_REFRESH:
                            if (mPBWReader.isFirmware()) {
                                writeInstallApp(mPebbleProtocol.encodeInstallFirmwareComplete());
                                finishInstall(false);
                            } else {
                                writeInstallApp(mPebbleProtocol.encodeAppRefresh(mInstallSlot));
                            }
                            break;
                        default:
                            break;
                    }
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
                    Log.i(TAG, "invalid length " + length);
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

                GBDeviceCommand deviceCmd = mPebbleProtocol.decodeResponse(buffer);
                if (deviceCmd == null) {
                    Log.i(TAG, "unhandled message to endpoint " + endpoint + " (" + length + " bytes)");
                } else {
                    evaluateGBDeviceCommand(deviceCmd);
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                if (e.getMessage().contains("socket closed")) { //FIXME: this does not feel right
                    Log.i(TAG, e.getMessage());
                    gbDevice.setState(GBDevice.State.CONNECTING);
                    gbDevice.sendDeviceUpdateIntent(getContext());

                    while (mConnectionAttempts++ < 10 && !mQuit) {
                        Log.i(TAG, "Trying to reconnect (attempt " + mConnectionAttempts + ")");
                        mIsConnected = connect(gbDevice.getAddress());
                        if (mIsConnected)
                            break;
                    }
                    mConnectionAttempts = 0;
                    if (!mIsConnected) {
                        mBtSocket = null;
                        Log.i(TAG, "Bluetooth socket closed, will quit IO Thread");
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

    @Override
    synchronized public void write(byte[] bytes) {
        // block writes if app installation in in progress
        if (mIsConnected && !mIsInstalling) {
            try {
                mOutStream.write(bytes);
                mOutStream.flush();
            } catch (IOException e) {
            }
        }
    }

    // FIXME: this does not belong here in this class, it is supporsed to be generic code
    private void evaluateGBDeviceCommand(GBDeviceCommand deviceCmd) {
        Context context = getContext();

        switch (deviceCmd.commandClass) {
            case MUSIC_CONTROL:
                Log.i(TAG, "Got command for MUSIC_CONTROL");
                GBDeviceCommandMusicControl musicCmd = (GBDeviceCommandMusicControl) deviceCmd;
                Intent musicIntent = new Intent(GBMusicControlReceiver.ACTION_MUSICCONTROL);
                musicIntent.putExtra("command", musicCmd.command.ordinal());
                musicIntent.setPackage(context.getPackageName());
                context.sendBroadcast(musicIntent);
                break;
            case CALL_CONTROL:
                Log.i(TAG, "Got command for CALL_CONTROL");
                GBDeviceCommandCallControl callCmd = (GBDeviceCommandCallControl) deviceCmd;
                Intent callIntent = new Intent(GBCallControlReceiver.ACTION_CALLCONTROL);
                callIntent.putExtra("command", callCmd.command.ordinal());
                callIntent.setPackage(context.getPackageName());
                context.sendBroadcast(callIntent);
                break;
            case VERSION_INFO:
                Log.i(TAG, "Got command for VERSION_INFO");
                if (gbDevice == null) {
                    return;
                }
                GBDeviceCommandVersionInfo infoCmd = (GBDeviceCommandVersionInfo) deviceCmd;
                gbDevice.setFirmwareVersion(infoCmd.fwVersion);
                gbDevice.setHardwareVersion(infoCmd.hwVersion);
                gbDevice.sendDeviceUpdateIntent(context);
                break;
            case APP_INFO:
                Log.i(TAG, "Got command for APP_INFO");
                GBDeviceCommandAppInfo appInfoCmd = (GBDeviceCommandAppInfo) deviceCmd;
                setInstallSlot(appInfoCmd.freeSlot);

                Intent appInfoIntent = new Intent(AppManagerActivity.ACTION_REFRESH_APPLIST);
                int appCount = appInfoCmd.apps.length;
                appInfoIntent.putExtra("app_count", appCount);
                for (Integer i = 0; i < appCount; i++) {
                    appInfoIntent.putExtra("app_name" + i.toString(), appInfoCmd.apps[i].getName());
                    appInfoIntent.putExtra("app_creator" + i.toString(), appInfoCmd.apps[i].getCreator());
                    appInfoIntent.putExtra("app_id" + i.toString(), appInfoCmd.apps[i].getId());
                    appInfoIntent.putExtra("app_index" + i.toString(), appInfoCmd.apps[i].getIndex());
                    appInfoIntent.putExtra("app_type" + i.toString(), appInfoCmd.apps[i].getType().ordinal());
                }
                LocalBroadcastManager.getInstance(context).sendBroadcast(appInfoIntent);
                break;
            case SEND_BYTES:
                GBDeviceCommandSendBytes sendBytes = (GBDeviceCommandSendBytes) deviceCmd;
                write(sendBytes.encodedBytes);
                break;

            case APP_MANAGEMENT_RES:
                GBDeviceCommandAppManagementResult appMgmtRes = (GBDeviceCommandAppManagementResult) deviceCmd;
                switch (appMgmtRes.type) {
                    case DELETE:
                        // right now on the Pebble we also receive this on a failed/successful installation ;/
                        switch (appMgmtRes.result) {
                            case FAILURE:
                                Log.i(TAG, "failure removing app"); // TODO: report to AppManager
                                finishInstall(true);
                                break;
                            case SUCCESS:
                                finishInstall(false);
                                // refresh app list
                                write(mPebbleProtocol.encodeAppInfoReq());
                                break;
                            default:
                                break;
                        }
                        break;
                    case INSTALL:
                        switch (appMgmtRes.result) {
                            case FAILURE:
                                Log.i(TAG, "failure installing app"); // TODO: report to Installer
                                finishInstall(true);
                                break;
                            case SUCCESS:
                                setToken(appMgmtRes.token);
                                break;
                            default:
                                break;
                        }
                        break;
                    default:
                        break;
                }
            default:
                break;
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
        int length = bytes.length;
        Log.i(TAG, "got " + length + "bytes for writeInstallApp()");
        try {
            mOutStream.write(bytes);
            mOutStream.flush();
        } catch (IOException e) {
        }
    }

    public void installApp(Uri uri) {
        if (mIsInstalling) {
            return;
        }
        write(mPebbleProtocol.encodeAppInfoReq()); // do this here to get run() out of its blocking read
        mInstallState = PebbleAppInstallState.APP_WAIT_SLOT;
        mInstallURI = uri;
        mIsInstalling = true;
    }

    public void finishInstall(boolean hadError) {
        if (!mIsInstalling) {
            return;
        }
        if (hadError) {
            updateInstallNotification(getContext().getString(R.string.installation_failed_), false, 0, getContext());
        } else {
            updateInstallNotification(getContext().getString(R.string.installation_successful), false, 0, getContext());
        }
        mInstallState = PebbleAppInstallState.UNKNOWN;

        if (hadError && mAppInstallToken != -1) {
            writeInstallApp(mPebbleProtocol.encodeUploadCancel(mAppInstallToken));
        }

        mPBWReader = null;
        mIsInstalling = false;
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
}