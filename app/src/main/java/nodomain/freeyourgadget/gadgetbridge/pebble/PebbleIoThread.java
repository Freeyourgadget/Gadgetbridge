package nodomain.freeyourgadget.gadgetbridge.pebble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.ParcelUuid;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.ZipInputStream;

import nodomain.freeyourgadget.gadgetbridge.AppManagerActivity;
import nodomain.freeyourgadget.gadgetbridge.GB;
import nodomain.freeyourgadget.gadgetbridge.GBCallControlReceiver;
import nodomain.freeyourgadget.gadgetbridge.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.GBDeviceIoThread;
import nodomain.freeyourgadget.gadgetbridge.GBMusicControlReceiver;
import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceCommand;
import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceCommandAppInfo;
import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceCommandAppManagementResult;
import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceCommandCallControl;
import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceCommandMusicControl;
import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceCommandVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceProtocol;

public class PebbleIoThread extends GBDeviceIoThread {
    private static final String TAG = PebbleIoThread.class.getSimpleName();

    private enum PebbleAppInstallState {
        UNKNOWN,
        APP_WAIT_SLOT,
        APP_START_INSTALL,
        APP_WAIT_TOKEN,
        APP_UPLOAD_CHUNK,
        APP_UPLOAD_COMMIT,
        APP_WAIT_COMMMIT,
        APP_UPLOAD_COMPLETE,
        APP_REFRESH,
    }

    private final PebbleProtocol mmPebbleProtocol;

    private BluetoothAdapter mBtAdapter = null;
    private BluetoothSocket mBtSocket = null;
    private InputStream mmInStream = null;
    private OutputStream mmOutStream = null;
    private boolean mmQuit = false;
    private boolean mmIsConnected = false;
    private boolean mmIsInstalling = false;
    private int mmConnectionAttempts = 0;

    /* app installation  */
    private Uri mmInstallURI = null;
    private PBWReader mmPBWReader = null;
    private int mmAppInstallToken = -1;
    private ZipInputStream mmZis = null;
    private STM32CRC mmSTM32CRC = new STM32CRC();
    private PebbleAppInstallState mmInstallState = PebbleAppInstallState.UNKNOWN;
    private String[] mmFilesToInstall = null;
    private int mmCurrentFileIndex = -1;
    private int mmInstallSlot = -1;

    public PebbleIoThread(GBDevice gbDevice, GBDeviceProtocol gbDeviceProtocol, BluetoothAdapter btAdapter, Context context) {
        super(gbDevice, context);
        mmPebbleProtocol = (PebbleProtocol) gbDeviceProtocol;
        mBtAdapter = btAdapter;
    }

    protected boolean connect(String btDeviceAddress) {
        BluetoothDevice btDevice = mBtAdapter.getRemoteDevice(btDeviceAddress);
        ParcelUuid uuids[] = btDevice.getUuids();
        GBDevice.State originalState = gbDevice.getState();
        try {
            mBtSocket = btDevice.createRfcommSocketToServiceRecord(uuids[0].getUuid());
            mBtSocket.connect();
            mmInStream = mBtSocket.getInputStream();
            mmOutStream = mBtSocket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
            gbDevice.setState(originalState);
            mmInStream = null;
            mmOutStream = null;
            mBtSocket = null;
            return false;
        }
        gbDevice.setState(GBDevice.State.CONNECTED);
        gbDevice.sendDeviceUpdateIntent(getContext());
        GB.updateNotification("connected to " + btDevice.getName(), getContext());

        return true;
    }

    public void run() {
        mmIsConnected = connect(gbDevice.getAddress());
        GB.setReceiversEnableState(mmIsConnected, getContext()); // enable/disable BroadcastReceivers
        mmQuit = !mmIsConnected; // quit if not connected

        byte[] buffer = new byte[8192];
        int bytes;

        while (!mmQuit) {
            try {
                if (mmIsInstalling) {
                    switch (mmInstallState) {
                        case APP_WAIT_SLOT:
                            if (mmInstallSlot != -1) {
                                GB.updateNotification("starting installation", getContext());
                                mmInstallState = PebbleAppInstallState.APP_START_INSTALL;
                                continue;
                            }
                            break;
                        case APP_START_INSTALL:
                            Log.i(TAG, "start installing app binary");
                            mmSTM32CRC.reset();
                            if (mmPBWReader == null) {
                                mmPBWReader = new PBWReader(mmInstallURI, getContext());
                                mmFilesToInstall = mmPBWReader.getFilesToInstall();
                                mmCurrentFileIndex = 0;
                            }
                            String fileName = mmFilesToInstall[mmCurrentFileIndex];
                            mmZis = mmPBWReader.getInputStreamFile(fileName);
                            int binarySize = mmPBWReader.getFileSize(fileName);
                            // FIXME: do not assume type from filename, parse json correctly in PBWReader
                            byte type = -1;
                            if (fileName.equals("pebble-app.bin")) {
                                type = PebbleProtocol.PUTBYTES_TYPE_BINARY;
                            } else if (fileName.equals("pebble-worker.bin")) {
                                type = PebbleProtocol.PUTBYTES_TYPE_WORKER;
                            } else if (fileName.equals("app_resources.pbpack")) {
                                type = PebbleProtocol.PUTBYTES_TYPE_RESOURCES;
                            } else {
                                finishInstall(true);
                                break;
                            }

                            writeInstallApp(mmPebbleProtocol.encodeUploadStart(type, (byte) mmInstallSlot, binarySize));
                            mmInstallState = PebbleAppInstallState.APP_WAIT_TOKEN;
                            break;
                        case APP_WAIT_TOKEN:
                            if (mmAppInstallToken != -1) {
                                Log.i(TAG, "got token " + mmAppInstallToken);
                                mmInstallState = PebbleAppInstallState.APP_UPLOAD_CHUNK;
                                continue;
                            }
                            break;
                        case APP_UPLOAD_CHUNK:
                            bytes = mmZis.read(buffer);

                            if (bytes != -1) {
                                mmSTM32CRC.addData(buffer, bytes);
                                writeInstallApp(mmPebbleProtocol.encodeUploadChunk(mmAppInstallToken, buffer, bytes));
                                mmAppInstallToken = -1;
                                mmInstallState = PebbleAppInstallState.APP_WAIT_TOKEN;
                            } else {
                                mmInstallState = PebbleAppInstallState.APP_UPLOAD_COMMIT;
                                continue;
                            }
                            break;
                        case APP_UPLOAD_COMMIT:
                            writeInstallApp(mmPebbleProtocol.encodeUploadCommit(mmAppInstallToken, mmSTM32CRC.getResult()));
                            mmAppInstallToken = -1;
                            mmInstallState = PebbleAppInstallState.APP_WAIT_COMMMIT;
                            break;
                        case APP_WAIT_COMMMIT:
                            if (mmAppInstallToken != -1) {
                                Log.i(TAG, "got token " + mmAppInstallToken);
                                mmInstallState = PebbleAppInstallState.APP_UPLOAD_COMPLETE;
                                continue;
                            }
                            break;
                        case APP_UPLOAD_COMPLETE:
                            writeInstallApp(mmPebbleProtocol.encodeUploadComplete(mmAppInstallToken));
                            if (++mmCurrentFileIndex < mmFilesToInstall.length) {
                                mmInstallState = PebbleAppInstallState.APP_START_INSTALL;
                            } else {
                                mmInstallState = PebbleAppInstallState.APP_REFRESH;
                            }
                            break;
                        case APP_REFRESH:
                            writeInstallApp(mmPebbleProtocol.encodeAppRefresh(mmInstallSlot));
                            break;
                        default:
                            break;
                    }
                }
                bytes = mmInStream.read(buffer, 0, 4);
                if (bytes < 4)
                    continue;

                ByteBuffer buf = ByteBuffer.wrap(buffer);
                buf.order(ByteOrder.BIG_ENDIAN);
                short length = buf.getShort();
                short endpoint = buf.getShort();
                if (length < 0 || length > 8192) {
                    Log.i(TAG, "invalid length " + length);
                    while (mmInStream.available() > 0) {
                        mmInStream.read(buffer); // read all
                    }
                    continue;
                }

                bytes = mmInStream.read(buffer, 4, length);
                if (bytes < length) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Log.i(TAG, "Read " + bytes + ", expected " + length + " reading remaining " + (length - bytes));
                    int bytes_rest = mmInStream.read(buffer, 4 + bytes, length - bytes);
                    bytes += bytes_rest;
                }

                if (length == 1 && endpoint == PebbleProtocol.ENDPOINT_PHONEVERSION) {
                    Log.i(TAG, "Pebble asked for Phone/App Version - repLYING!");
                    write(mmPebbleProtocol.encodePhoneVersion(PebbleProtocol.PHONEVERSION_REMOTE_OS_ANDROID));
                    write(mmPebbleProtocol.encodeFirmwareVersionReq());

                    // this does not really belong here, but since the pebble only asks for our version once it should do the job
                    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                    if (sharedPrefs.getBoolean("datetime_synconconnect", true)) {
                        Log.i(TAG, "syncing time");
                        write(mmPebbleProtocol.encodeSetTime(-1));
                    }
                } else if (endpoint != PebbleProtocol.ENDPOINT_DATALOG) {
                    GBDeviceCommand deviceCmd = mmPebbleProtocol.decodeResponse(buffer);
                    if (deviceCmd == null) {
                        Log.i(TAG, "unhandled message to endpoint " + endpoint + " (" + bytes + " bytes)");
                    } else {
                        evaluateGBCommandBundle(deviceCmd);
                    }
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
                    GB.updateNotification("connection lost, trying to reconnect", getContext());

                    while (mmConnectionAttempts++ < 10) {
                        Log.i(TAG, "Trying to reconnect (attempt " + mmConnectionAttempts + ")");
                        mmIsConnected = connect(gbDevice.getAddress());
                        if (mmIsConnected)
                            break;
                    }
                    mmConnectionAttempts = 0;
                    if (!mmIsConnected) {
                        mBtSocket = null;
                        GB.setReceiversEnableState(false, getContext());
                        Log.i(TAG, "Bluetooth socket closed, will quit IO Thread");
                        mmQuit = true;
                    }
                }
            }
        }
        mmIsConnected = false;
        if (mBtSocket != null) {
            try {
                mBtSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        mBtSocket = null;
        GB.updateNotification("not connected", getContext());
        gbDevice.setState(GBDevice.State.NOT_CONNECTED);
        gbDevice.sendDeviceUpdateIntent(getContext());
    }

    synchronized public void write(byte[] bytes) {
        // block writes if app installation in in progress
        if (mmIsConnected && !mmIsInstalling) {
            try {
                mmOutStream.write(bytes);
                mmOutStream.flush();
            } catch (IOException e) {
            }
        }
    }

    private void evaluateGBCommandBundle(GBDeviceCommand deviceCmd) {
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
                                write(mmPebbleProtocol.encodeAppInfoReq());
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
        mmAppInstallToken = token;
    }

    public void setInstallSlot(int slot) {
        if (mmIsInstalling) {
            mmInstallSlot = slot;
        }
    }

    private void writeInstallApp(byte[] bytes) {
        if (!mmIsInstalling) {
            return;
        }
        int length = bytes.length;
        Log.i(TAG, "got bytes for writeInstallApp()" + length);
                /*
            final char[] hexArray = "0123456789ABCDEF".toCharArray();
            char[] hexChars = new char[length * 2];
            for (int j = 0; j < length; j++) {
                int v = bytes[j] & 0xFF;
                hexChars[j * 2] = hexArray[v >>> 4];
                hexChars[j * 2 + 1] = hexArray[v & 0x0F];
            }
            Log.i(TAG, new String(hexChars));
				 */
        try {
            mmOutStream.write(bytes);
            mmOutStream.flush();
        } catch (IOException e) {
        }
    }

    public void installApp(Uri uri) {
        if (mmIsInstalling) {
            return;
        }
        write(mmPebbleProtocol.encodeAppInfoReq()); // do this here to get run() out of its blocking read
        mmInstallState = PebbleAppInstallState.APP_WAIT_SLOT;
        mmInstallURI = uri;
        mmIsInstalling = true;
    }

    public void finishInstall(boolean hadError) {
        if (!mmIsInstalling) {
            return;
        }
        if (hadError) {
            GB.updateNotification("installation failed!", getContext());
        } else {
            GB.updateNotification("installation successful", getContext());
        }
        mmInstallState = PebbleAppInstallState.UNKNOWN;

        if (hadError == true && mmAppInstallToken != -1) {
            writeInstallApp(mmPebbleProtocol.encodeUploadCancel(mmAppInstallToken));
        }

        mmPBWReader = null;
        mmIsInstalling = false;
        mmZis = null;
        mmAppInstallToken = -1;
        mmInstallSlot = -1;
    }

    public void quit() {
        mmQuit = true;
        if (mBtSocket != null) {
            try {
                mBtSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}