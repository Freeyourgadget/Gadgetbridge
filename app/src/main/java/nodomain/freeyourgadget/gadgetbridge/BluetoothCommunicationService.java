package nodomain.freeyourgadget.gadgetbridge;

import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.GBDevice.State;
import nodomain.freeyourgadget.gadgetbridge.miband.MiBandSupport;
import nodomain.freeyourgadget.gadgetbridge.pebble.PebbleIoThread;
import nodomain.freeyourgadget.gadgetbridge.pebble.PebbleSupport;

public class BluetoothCommunicationService extends Service {
    public static final String ACTION_START
            = "nodomain.freeyourgadget.gadgetbride.bluetoothcommunicationservice.action.start";
    public static final String ACTION_PAIR
            = "nodomain.freeyourgadget.gadgetbride.bluetoothcommunicationservice.action.pair";
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
    public static final String EXTRA_PERFORM_PAIR = "perform_pair";

    private static final Logger LOG = LoggerFactory.getLogger(BluetoothCommunicationService.class);
    public static final String EXTRA_DEVICE_ADDRESS = "device_address";
    private GBDeviceIoThread mGBDeviceIoThread = null;

    private boolean mStarted = false;

    private GBDevice mGBDevice = null;
    private DeviceSupport mDeviceSupport;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(GBDevice.ACTION_DEVICE_CHANGED)) {
                GBDevice device = intent.getParcelableExtra("device");
                if (mGBDevice.equals(device)) {
                    mGBDevice = device;
                    GB.setReceiversEnableState(mDeviceSupport.useAutoConnect() || mGBDevice.isConnected(), context);
                    GB.updateNotification(mGBDevice.getName() + " " + mGBDevice.getStateString(), context);
                }
            }
        }
    };

    @Override
    public void onCreate() {
        LOG.debug("BluetoothCommunicationService is being created");
        super.onCreate();
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter(GBDevice.ACTION_DEVICE_CHANGED));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent == null) {
            LOG.info("no intent");
            return START_NOT_STICKY;
        }

        String action = intent.getAction();
        boolean pair = intent.getBooleanExtra(EXTRA_PERFORM_PAIR, false);

        if (action == null) {
            LOG.info("no action");
            return START_NOT_STICKY;
        }

        LOG.debug("Service startcommand: " + action);

        if (!mStarted && !action.equals(ACTION_START)) {
            // using the service before issuing ACTION_START
            LOG.info("Must start service with " + ACTION_START + " before using it: " + action);
            return START_NOT_STICKY;
        }

        if (mStarted && action.equals(ACTION_START)) {
            // using ACTION_START when the service has already been started
            return START_STICKY;
        }

        if (!action.equals(ACTION_START) && !action.equals(ACTION_CONNECT)) {
            if (mDeviceSupport == null || (!isConnected() && !mDeviceSupport.useAutoConnect())) {
                // trying to send notification without valid Bluetooth connection
                if (mGBDevice != null) {
                    // at least send back the current device state
                    mGBDevice.sendDeviceUpdateIntent(this);
                }
                return START_STICKY;
            }
        }

        switch (action) {
            case ACTION_CONNECT:
                //Check the system status
                BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();
                if (mBtAdapter == null) {
                    Toast.makeText(this, R.string.bluetooth_is_not_supported_, Toast.LENGTH_SHORT).show();
                } else if (!mBtAdapter.isEnabled()) {
                    Toast.makeText(this, R.string.bluetooth_is_disabled_, Toast.LENGTH_SHORT).show();
                } else {
                    String btDeviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS);
                    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
                    sharedPrefs.edit().putString("last_device_address", btDeviceAddress).apply();

                    if (btDeviceAddress != null && !isConnected() && !isConnecting()) {
                        if (mDeviceSupport != null) {
                            mDeviceSupport.dispose();
                            mDeviceSupport = null;
                        }
                        try {
                            BluetoothDevice btDevice = mBtAdapter.getRemoteDevice(btDeviceAddress);
                            if (btDevice.getName() == null || btDevice.getName().equals("MI")) { //FIXME: workaround for Miband not being paired
                                mGBDevice = new GBDevice(btDeviceAddress, "MI", DeviceType.MIBAND);
                                mDeviceSupport = new MiBandSupport();
                            } else if (btDevice.getName().indexOf("Pebble") == 0) {
                                mGBDevice = new GBDevice(btDeviceAddress, btDevice.getName(), DeviceType.PEBBLE);
                                mDeviceSupport = new PebbleSupport();
                            }
                            if (mDeviceSupport != null) {
                                mDeviceSupport.initialize(mGBDevice, mBtAdapter, this);
                                if (pair) {
                                    mDeviceSupport.pair();
                                } else {
                                    mDeviceSupport.connect();
                                }
                                if (mDeviceSupport instanceof AbstractBTDeviceSupport) {
                                    mGBDeviceIoThread = ((AbstractBTDeviceSupport) mDeviceSupport).getDeviceIOThread();
                                }
                            }
                        } catch (Exception e) {
                            Toast.makeText(this, R.string.cannot_connect_bt_address_invalid_, Toast.LENGTH_SHORT).show();
                            e.printStackTrace();
                        }
                    }
                }
                break;
            case ACTION_NOTIFICATION_GENERIC: {
                String title = intent.getStringExtra("notification_title");
                String body = intent.getStringExtra("notification_body");
                mDeviceSupport.onGenericNotification(title, body);
                break;
            }
            case ACTION_NOTIFICATION_SMS: {
                String sender = intent.getStringExtra("notification_sender");
                String body = intent.getStringExtra("notification_body");
                String senderName = getContactDisplayNameByNumber(sender);
                mDeviceSupport.onSMS(senderName, body);
                break;
            }
            case ACTION_NOTIFICATION_EMAIL: {
                String sender = intent.getStringExtra("notification_sender");
                String subject = intent.getStringExtra("notification_subject");
                String body = intent.getStringExtra("notification_body");
                mDeviceSupport.onEmail(sender, subject, body);
                break;
            }
            case ACTION_CALLSTATE:
                GBCommand command = GBCommand.values()[intent.getIntExtra("call_command", 0)]; // UGLY

                String phoneNumber = intent.getStringExtra("call_phonenumber");
                String callerName = null;
                if (phoneNumber != null) {
                    callerName = getContactDisplayNameByNumber(phoneNumber);
                }
                mDeviceSupport.onSetCallState(phoneNumber, callerName, command);
                break;
            case ACTION_SETTIME:
                mDeviceSupport.onSetTime(-1);
                break;
            case ACTION_SETMUSICINFO:
                String artist = intent.getStringExtra("music_artist");
                String album = intent.getStringExtra("music_album");
                String track = intent.getStringExtra("music_track");
                mDeviceSupport.onSetMusicInfo(artist, album, track);
                break;
            case ACTION_REQUEST_VERSIONINFO:
                if (mGBDevice != null && mGBDevice.getFirmwareVersion() == null) {
                    mDeviceSupport.onFirmwareVersionReq();
                } else {
                    mGBDevice.sendDeviceUpdateIntent(this);
                }
                break;
            case ACTION_REQUEST_APPINFO:
                mDeviceSupport.onAppInfoReq();
                break;
            case ACTION_DELETEAPP:
                int id = intent.getIntExtra("app_id", -1);
                int index = intent.getIntExtra("app_index", -1);
                mDeviceSupport.onAppDelete(id, index);
                break;
            case ACTION_INSTALL_PEBBLEAPP:
                String uriString = intent.getStringExtra("app_uri");
                if (uriString != null) {
                    LOG.info("will try to install app");
                    ((PebbleIoThread) mGBDeviceIoThread).installApp(Uri.parse(uriString));
                }
                break;
            case ACTION_START:
                startForeground(GB.NOTIFICATION_ID, GB.createNotification(getString(R.string.gadgetbridge_running), this));
                mStarted = true;
                break;
        }

        return START_STICKY;
    }

    private boolean isConnected() {
        return mGBDevice != null && mGBDevice.getState() == State.CONNECTED;
    }

    private boolean isConnecting() {
        return mGBDevice != null && mGBDevice.getState() == State.CONNECTING;
    }

    @Override
    public void onDestroy() {
        LOG.debug("BluetoothCommunicationService is being destroyed");
        super.onDestroy();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        GB.setReceiversEnableState(false, this); // disable BroadcastReceivers

        if (mDeviceSupport != null) {
            mDeviceSupport.dispose();
        }
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(GB.NOTIFICATION_ID); // need to do this because the updated notification wont be cancelled when service stops
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
}
