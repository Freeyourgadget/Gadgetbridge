package nodomain.freeyourgadget.gadgetbridge;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.ZipInputStream;

import nodomain.freeyourgadget.gadgetbridge.pebble.PBWReader;
import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceCommand;
import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceCommandAppInfo;
import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceCommandAppManagementResult;
import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceCommandCallControl;
import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceCommandMusicControl;
import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceCommandVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceProtocol;
import nodomain.freeyourgadget.gadgetbridge.protocol.MibandProtocol;
import nodomain.freeyourgadget.gadgetbridge.protocol.PebbleProtocol;

public class BluetoothCommunicationService extends Service {
    public static final String ACTION_START
            = "nodomain.freeyourgadget.gadgetbride.bluetoothcommunicationservice.action.start";
    public static final String ACTION_CONNECT
            = "nodomain.freeyourgadget.gadgetbride.bluetoothcommunicationservice.action.connect";
    public static final String ACTION_NOTIFICATION_GENERIC
            = "nodomain.freeyourgadget.gadgetbride.bluetoothcommunicationservice.action.notification_generic";
    public static final String ACTION_NOTIFICATION_SMS
            = "nodomain.freeyourgadget.gadgetbride.bluetoothcommunicationservice.action.notification_sms";
    public static final String ACTION_NOTIFICATION_EMAIL
            = "nodomain.freeyourgadget.gadgetbride.bluetoothcommunicationservice.action.notification_email";
    public static final String ACTION_CALLSTATE
            = "nodomain.freeyourgadget.gadgetbride.bluetoothcommunicationservice.action.callstate";
    public static final String ACTION_SETTIME
            = "nodomain.freeyourgadget.gadgetbride.bluetoothcommunicationservice.action.settime";
    public static final String ACTION_SETMUSICINFO
            = "nodomain.freeyourgadget.gadgetbride.bluetoothcommunicationservice.action.setmusicinfo";
    public static final String ACTION_REQUEST_VERSIONINFO
            = "nodomain.freeyourgadget.gadgetbride.bluetoothcommunicationservice.action.request_versioninfo";
    public static final String ACTION_REQUEST_APPINFO
            = "nodomain.freeyourgadget.gadgetbride.bluetoothcommunicationservice.action.request_appinfo";
    public static final String ACTION_DELETEAPP
            = "nodomain.freeyourgadget.gadgetbride.bluetoothcommunicationservice.action.deleteapp";
    public static final String ACTION_INSTALL_PEBBLEAPP
            = "nodomain.freeyourgadget.gadgetbride.bluetoothcommunicationservice.action.install_pebbbleapp";

    private static final String TAG = "CommunicationService";
    private static final int NOTIFICATION_ID = 1;
    private BluetoothAdapter mBtAdapter = null;
    private BluetoothSocket mBtSocket = null;
    private GBDeviceIoThread mGBDeviceIoThread = null;

    private boolean mStarted = false;

    private GBDevice mGBDevice = null;
    private GBDeviceProtocol mGBDeviceProtocol = null;

    private void setReceiversEnableState(boolean enable) {
        final Class[] receiverClasses = {
                PhoneCallReceiver.class,
                SMSReceiver.class,
                K9Receiver.class,
                MusicPlaybackReceiver.class,
                //NotificationListener.class, // disabling this leads to loss of permission to read notifications
        };

        int newState;

        if (enable) {
            newState = PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
        } else {
            newState = PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        }

        PackageManager pm = getPackageManager();

        for (Class receiverClass : receiverClasses) {
            ComponentName compName = new ComponentName(getApplicationContext(), receiverClass);

            pm.setComponentEnabledSetting(compName, newState, PackageManager.DONT_KILL_APP);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }


    private Notification createNotification(String text) {
        Intent notificationIntent = new Intent(this, ControlCenter.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        return new NotificationCompat.Builder(this)
                .setContentTitle("Gadgetbridge")
                .setTicker(text)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setOngoing(true).build();
    }

    private void updateNotification(String text) {

        Notification notification = createNotification(text);

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTIFICATION_ID, notification);
    }

    private void evaluateGBCommandBundle(GBDeviceCommand deviceCmd) {
        switch (deviceCmd.commandClass) {
            case MUSIC_CONTROL:
                Log.i(TAG, "Got command for MUSIC_CONTROL");
                GBDeviceCommandMusicControl musicCmd = (GBDeviceCommandMusicControl) deviceCmd;
                Intent musicIntent = new Intent(GBMusicControlReceiver.ACTION_MUSICCONTROL);
                musicIntent.putExtra("command", musicCmd.command.ordinal());
                musicIntent.setPackage(this.getPackageName());
                sendBroadcast(musicIntent);
                break;
            case CALL_CONTROL:
                Log.i(TAG, "Got command for CALL_CONTROL");
                GBDeviceCommandCallControl callCmd = (GBDeviceCommandCallControl) deviceCmd;
                Intent callIntent = new Intent(GBCallControlReceiver.ACTION_CALLCONTROL);
                callIntent.putExtra("command", callCmd.command.ordinal());
                callIntent.setPackage(this.getPackageName());
                sendBroadcast(callIntent);
                break;
            case VERSION_INFO:
                Log.i(TAG, "Got command for VERSION_INFO");
                if (mGBDevice == null) {
                    return;
                }
                GBDeviceCommandVersionInfo infoCmd = (GBDeviceCommandVersionInfo) deviceCmd;
                mGBDevice.setFirmwareVersion(infoCmd.fwVersion);
                sendDeviceUpdateIntent();
                break;
            case APP_INFO:
                Log.i(TAG, "Got command for APP_INFO");
                GBDeviceCommandAppInfo appInfoCmd = (GBDeviceCommandAppInfo) deviceCmd;
                mGBDevice.setFreeAppSlot(appInfoCmd.freeSlot);
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
                LocalBroadcastManager.getInstance(this).sendBroadcast(appInfoIntent);
                break;
            case APP_MANAGEMENT_RES:
                GBDeviceCommandAppManagementResult appMgmtRes = (GBDeviceCommandAppManagementResult) deviceCmd;
                switch (appMgmtRes.type) {
                    case DELETE:
                        switch (appMgmtRes.result) {
                            case FAILURE:
                                Log.i(TAG, "failure removing app"); // TODO: report to AppManager
                                break;
                            case SUCCESS:
                                // refresh app list
                                mGBDeviceIoThread.write(mGBDeviceProtocol.encodeAppInfoReq());
                                break;
                            default:
                                break;
                        }
                        break;
                    case INSTALL:
                        switch (appMgmtRes.result) {
                            case FAILURE:
                                Log.i(TAG, "failure installing app"); // TODO: report to Installer
                                break;
                            case SUCCESS:
                                if (mGBDevice.getType() == GBDevice.Type.PEBBLE) {
                                    ((PebbleIoThread) mGBDeviceIoThread).setToken(appMgmtRes.token);
                                }
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

    private void sendDeviceUpdateIntent() {
        Intent deviceUpdateIntent = new Intent(ControlCenter.ACTION_REFRESH_DEVICELIST);
        deviceUpdateIntent.putExtra("device_address", mGBDevice.getAddress());
        deviceUpdateIntent.putExtra("device_state", mGBDevice.getState().ordinal());
        deviceUpdateIntent.putExtra("firmware_version", mGBDevice.getFirmwareVersion());

        LocalBroadcastManager.getInstance(this).sendBroadcast(deviceUpdateIntent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent == null) {
            Log.i(TAG, "no intent");
            return START_NOT_STICKY;
        }

        String action = intent.getAction();

        if (action == null) {
            Log.i(TAG, "no action");
            return START_NOT_STICKY;
        }

        if (!mStarted && !action.equals(ACTION_START)) {
            // using the service before issuing ACTION_START
            return START_NOT_STICKY;
        }

        if (mStarted && action.equals(ACTION_START)) {
            // using ACTION_START when the service has already been started
            return START_STICKY;
        }

        if (!action.equals(ACTION_START) && !action.equals(ACTION_CONNECT) && mBtSocket == null) {
            // trying to send notification without valid Blutooth socket
            return START_STICKY;
        }

        if (action.equals(ACTION_CONNECT)) {
            //Check the system status
            mBtAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBtAdapter == null) {
                Toast.makeText(this, "Bluetooth is not supported.", Toast.LENGTH_SHORT).show();
            } else if (!mBtAdapter.isEnabled()) {
                Toast.makeText(this, "Bluetooth is disabled.", Toast.LENGTH_SHORT).show();
            } else {
                String btDeviceAddress = intent.getStringExtra("device_address");
                SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
                sharedPrefs.edit().putString("last_device_address", btDeviceAddress).commit();

                if (btDeviceAddress != null && (mBtSocket == null || !mBtSocket.isConnected())) {
                    // currently only one thread allowed
                    if (mGBDeviceIoThread != null) {
                        mGBDeviceIoThread.quit();
                        try {
                            mGBDeviceIoThread.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    }
                    BluetoothDevice btDevice = mBtAdapter.getRemoteDevice(btDeviceAddress);
                    if (btDevice != null) {
                        GBDevice.Type deviceType = GBDevice.Type.UNKNOWN;
                        if (btDevice.getName() == null || btDevice.getName().equals("MI")) { //FIXME: workaround for Miband not being paired
                            deviceType = GBDevice.Type.MIBAND;
                            mGBDeviceProtocol = new MibandProtocol();
                            mGBDeviceIoThread = new MibandIoThread(btDeviceAddress);
                        } else if (btDevice.getName().indexOf("Pebble") == 0) {
                            deviceType = GBDevice.Type.PEBBLE;
                            mGBDeviceProtocol = new PebbleProtocol();
                            mGBDeviceIoThread = new PebbleIoThread(btDeviceAddress);
                        }
                        if (mGBDeviceProtocol != null) {
                            mGBDevice = new GBDevice(btDeviceAddress, btDevice.getName(), deviceType);
                            mGBDevice.setState(GBDevice.State.CONNECTING);
                            sendDeviceUpdateIntent();

                            mGBDeviceIoThread.start();
                        }
                    }
                }
            }
        } else if (action.equals(ACTION_NOTIFICATION_GENERIC)) {
            String title = intent.getStringExtra("notification_title");
            String body = intent.getStringExtra("notification_body");
            byte[] msg = mGBDeviceProtocol.encodeSMS(title, body);
            mGBDeviceIoThread.write(msg);
        } else if (action.equals(ACTION_NOTIFICATION_SMS)) {
            String sender = intent.getStringExtra("notification_sender");
            String body = intent.getStringExtra("notification_body");
            String senderName = getContactDisplayNameByNumber(sender);
            byte[] msg = mGBDeviceProtocol.encodeSMS(senderName, body);
            mGBDeviceIoThread.write(msg);
        } else if (action.equals(ACTION_NOTIFICATION_EMAIL)) {
            String sender = intent.getStringExtra("notification_sender");
            String subject = intent.getStringExtra("notification_subject");
            String body = intent.getStringExtra("notification_body");
            byte[] msg = mGBDeviceProtocol.encodeEmail(sender, subject, body);
            mGBDeviceIoThread.write(msg);
        } else if (action.equals(ACTION_CALLSTATE)) {
            GBCommand command = GBCommand.values()[intent.getIntExtra("call_command", 0)]; // UGLY
            String phoneNumber = intent.getStringExtra("call_phonenumber");
            String callerName = null;
            if (phoneNumber != null) {
                callerName = getContactDisplayNameByNumber(phoneNumber);
            }
            byte[] msg = mGBDeviceProtocol.encodeSetCallState(phoneNumber, callerName, command);
            mGBDeviceIoThread.write(msg);
        } else if (action.equals(ACTION_SETTIME)) {
            byte[] msg = mGBDeviceProtocol.encodeSetTime(-1);
            mGBDeviceIoThread.write(msg);
        } else if (action.equals(ACTION_SETMUSICINFO)) {
            String artist = intent.getStringExtra("music_artist");
            String album = intent.getStringExtra("music_album");
            String track = intent.getStringExtra("music_track");
            byte[] msg = mGBDeviceProtocol.encodeSetMusicInfo(artist, album, track);
            mGBDeviceIoThread.write(msg);
        } else if (action.equals(ACTION_REQUEST_VERSIONINFO)) {
            if (mGBDevice != null && mGBDevice.getFirmwareVersion() == null) {
                byte[] msg = mGBDeviceProtocol.encodeFirmwareVersionReq();
                mGBDeviceIoThread.write(msg);
            } else {
                sendDeviceUpdateIntent();
            }
        } else if (action.equals(ACTION_REQUEST_APPINFO)) {
            mGBDeviceIoThread.write(mGBDeviceProtocol.encodeAppInfoReq());
        } else if (action.equals(ACTION_DELETEAPP)) {
            int id = intent.getIntExtra("app_id", -1);
            int index = intent.getIntExtra("app_index", -1);
            mGBDeviceIoThread.write(mGBDeviceProtocol.encodeAppDelete(id, index));
        } else if (action.equals(ACTION_INSTALL_PEBBLEAPP)) {
            String uriString = intent.getStringExtra("app_uri");
            if (uriString != null && mGBDevice.getFreeAppSlot() != -1) {
                Log.i(TAG, "will try to install app in slot " + mGBDevice.getFreeAppSlot());
                ((PebbleIoThread) mGBDeviceIoThread).installApp(Uri.parse(uriString));
            }
        } else if (action.equals(ACTION_START)) {
            startForeground(NOTIFICATION_ID, createNotification("Gadgetbridge running"));
            mStarted = true;
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        setReceiversEnableState(false); // disable BroadcastReceivers

        if (mGBDeviceIoThread != null) {
            try {
                mGBDeviceIoThread.quit();
                mGBDeviceIoThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(NOTIFICATION_ID); // need to do this because the updated notification wont be cancelled when service stops
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private String getContactDisplayNameByNumber(String number) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
        String name = number;

        if (number == null || number.equals("")) {
            return name;
        }

        ContentResolver contentResolver = getContentResolver();
        Cursor contactLookup = contentResolver.query(uri, null, null, null, null);

        try {
            if (contactLookup != null && contactLookup.getCount() > 0) {
                contactLookup.moveToNext();
                name = contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
            }
        } finally {
            if (contactLookup != null) {
                contactLookup.close();
            }
        }

        return name;
    }

    private abstract class GBDeviceIoThread extends Thread {
        protected final String mmBtDeviceAddress;

        public GBDeviceIoThread(String btDeviceAddress) {
            mmBtDeviceAddress = btDeviceAddress;
        }

        private boolean connect(String btDeviceAddress) {
            return false;
        }

        public void run() {
        }

        synchronized public void write(byte[] bytes) {
        }

        public void quit() {
        }
    }

    private class MibandIoThread extends GBDeviceIoThread {
        public MibandIoThread(String btDeviceAddress) {
            super(btDeviceAddress);
        }

        // implement connect() run() write() and quit() here
    }

    private enum PebbleAppInstallState {
        UNKNOWN,
        APP_START_INSTALL,
        APP_WAIT_TOKEN,
        APP_UPLOAD_CHUNK,
        APP_UPLOAD_COMPLETE,
        RES_START_INSTALL,
        RES_WAIT_TOKEN,
        RES_UPLOAD_CHUNK,
        RES_UPLOAD_COMPLETE,
    }

    private class PebbleIoThread extends GBDeviceIoThread {
        private final PebbleProtocol mmPebbleProtocol;
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
        private PebbleAppInstallState mmInstallState = PebbleAppInstallState.UNKNOWN;

        public PebbleIoThread(String btDeviceAddress) {
            super(btDeviceAddress);
            mmPebbleProtocol = (PebbleProtocol) mGBDeviceProtocol;
        }

        private boolean connect(String btDeviceAddress) {
            BluetoothDevice btDevice = mBtAdapter.getRemoteDevice(btDeviceAddress);
            ParcelUuid uuids[] = btDevice.getUuids();
            try {
                mBtSocket = btDevice.createRfcommSocketToServiceRecord(uuids[0].getUuid());
                mBtSocket.connect();
                mmInStream = mBtSocket.getInputStream();
                mmOutStream = mBtSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
                mmInStream = null;
                mmOutStream = null;
                mBtSocket = null;
                return false;
            }
            mGBDevice.setState(GBDevice.State.CONNECTED);
            sendDeviceUpdateIntent();
            updateNotification("connected to " + btDevice.getName());

            return true;
        }

        public void run() {
            mmIsConnected = connect(mmBtDeviceAddress);
            setReceiversEnableState(mmIsConnected); // enable/disable BroadcastReceivers
            mmQuit = !mmIsConnected; // quit if not connected

            byte[] buffer = new byte[8192];
            int bytes;

            while (!mmQuit) {
                try {
                    if (mmIsInstalling) {
                        switch (mmInstallState) {
                            case APP_START_INSTALL:
                                Log.i(TAG, "start installing app binary");
                                mmPBWReader = new PBWReader(mmInstallURI, getApplicationContext());
                                mmZis = mmPBWReader.getInputStreamAppBinary();
                                int binarySize = mmPBWReader.getAppBinarySize();
                                writeInstallApp(mmPebbleProtocol.encodeUploadStart(PebbleProtocol.PUTBYTES_TYPE_BINARY, mGBDevice.getFreeAppSlot(), binarySize), -1);
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
                                    writeInstallApp(mmPebbleProtocol.encodeUploadChunk(mmAppInstallToken, buffer, bytes), -1);
                                    mmAppInstallToken = -1;
                                    mmInstallState = PebbleAppInstallState.APP_WAIT_TOKEN;
                                } else {
                                    mmInstallState = PebbleAppInstallState.UNKNOWN;
                                    mmIsInstalling = false;
                                    mmZis = null;
                                    mmAppInstallToken = -1;
                                }
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
                        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(BluetoothCommunicationService.this);
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
                        mGBDevice.setState(GBDevice.State.CONNECTING);
                        sendDeviceUpdateIntent();
                        updateNotification("connection lost, trying to reconnect");

                        while (mmConnectionAttempts++ < 10) {
                            Log.i(TAG, "Trying to reconnect (attempt " + mmConnectionAttempts + ")");
                            mmIsConnected = connect(mmBtDeviceAddress);
                            if (mmIsConnected)
                                break;
                        }
                        mmConnectionAttempts = 0;
                        if (!mmIsConnected) {
                            mBtSocket = null;
                            setReceiversEnableState(false);
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
            updateNotification("not connected");
            mGBDevice.setState(GBDevice.State.NOT_CONNECTED);
            sendDeviceUpdateIntent();
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

        public void setToken(int token) {
            mmAppInstallToken = token;
        }

        private void writeInstallApp(byte[] bytes, int length) {
            if (!mmIsInstalling) {
                return;
            }
            if (length == -1) {
                length = bytes.length;
            }
            Log.i(TAG, "got bytes for writeInstallApp()" + length);
            final char[] hexArray = "0123456789ABCDEF".toCharArray();
            char[] hexChars = new char[length * 2];
            for (int j = 0; j < length; j++) {
                int v = bytes[j] & 0xFF;
                hexChars[j * 2] = hexArray[v >>> 4];
                hexChars[j * 2 + 1] = hexArray[v & 0x0F];
            }
            Log.i(TAG, new String(hexChars));
            try {
                mmOutStream.write(bytes);
                mmOutStream.flush();
            } catch (IOException e) {
            }
        }

        public void installApp(Uri uri) {
            mmInstallState = PebbleAppInstallState.APP_START_INSTALL;
            mmIsInstalling = true;
            mmInstallURI = uri;
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
}
