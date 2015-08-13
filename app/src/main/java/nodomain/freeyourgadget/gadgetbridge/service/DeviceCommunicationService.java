package nodomain.freeyourgadget.gadgetbridge.service;

import android.app.NotificationManager;
import android.app.Service;
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

import java.util.ArrayList;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.ServiceCommand;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class DeviceCommunicationService extends Service {
    public static final String ACTION_START
            = "nodomain.freeyourgadget.gadgetbridge.devicecommunicationservice.action.start";
    public static final String ACTION_CONNECT
            = "nodomain.freeyourgadget.gadgetbridge.devicecommunicationservice.action.connect";
    public static final String ACTION_NOTIFICATION_GENERIC
            = "nodomain.freeyourgadget.gadgetbridge.devicecommunicationservice.action.notification_generic";
    public static final String ACTION_NOTIFICATION_SMS
            = "nodomain.freeyourgadget.gadgetbridge.devicecommunicationservice.action.notification_sms";
    public static final String ACTION_NOTIFICATION_EMAIL
            = "nodomain.freeyourgadget.gadgetbridge.devicecommunicationservice.action.notification_email";
    public static final String ACTION_CALLSTATE
            = "nodomain.freeyourgadget.gadgetbridge.devicecommunicationservice.action.callstate";
    public static final String ACTION_SETTIME
            = "nodomain.freeyourgadget.gadgetbridge.devicecommunicationservice.action.settime";
    public static final String ACTION_SETMUSICINFO
            = "nodomain.freeyourgadget.gadgetbridge.devicecommunicationservice.action.setmusicinfo";
    public static final String ACTION_REQUEST_VERSIONINFO
            = "nodomain.freeyourgadget.gadgetbridge.devicecommunicationservice.action.request_versioninfo";
    public static final String ACTION_REQUEST_APPINFO
            = "nodomain.freeyourgadget.gadgetbridge.devicecommunicationservice.action.request_appinfo";
    public static final String ACTION_REQUEST_SCREENSHOT
            = "nodomain.freeyourgadget.gadgetbridge.devicecommunicationservice.action.request_screenshot";
    public static final String ACTION_STARTAPP
            = "nodomain.freeyourgadget.gadgetbridge.devicecommunicationservice.action.startapp";
    public static final String ACTION_DELETEAPP
            = "nodomain.freeyourgadget.gadgetbridge.devicecommunicationservice.action.deleteapp";
    public static final String ACTION_INSTALL
            = "nodomain.freeyourgadget.gadgetbridge.devicecommunicationservice.action.install";
    public static final String ACTION_REBOOT = "nodomain.freeyourgadget.gadgetbridge.devicecommunicationservice.action.reboot";
    public static final String ACTION_FETCH_ACTIVITY_DATA = "nodomain.freeyourgadget.gadgetbridge.devicecommunicationservice.action.fetch_activity_data";
    public static final String ACTION_DISCONNECT = "nodomain.freeyourgadget.gadgetbridge.devicecommunicationservice.action.disconnect";
    public static final String ACTION_FIND_DEVICE = "nodomain.freeyourgadget.gadgetbridge.devicecommunicationservice.action.find_device";
    public static final String ACTION_SET_ALARMS = "nodomain.freeyourgadget.gadgetbridge.devicecommunicationservice.action.set_alarms";

    public static final String EXTRA_PERFORM_PAIR = "perform_pair";

    private static final Logger LOG = LoggerFactory.getLogger(DeviceCommunicationService.class);
    public static final String EXTRA_DEVICE_ADDRESS = "device_address";

    private boolean mStarted = false;

    private GBDevice mGBDevice = null;
    private DeviceSupport mDeviceSupport;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(GBDevice.ACTION_DEVICE_CHANGED)) {
                GBDevice device = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
                if (mGBDevice.equals(device)) {
                    mGBDevice = device;
                    boolean enableReceivers = mDeviceSupport != null && (mDeviceSupport.useAutoConnect() || mGBDevice.isInitialized());
                    GB.setReceiversEnableState(enableReceivers, context);
                    GB.updateNotification(mGBDevice.getName() + " " + mGBDevice.getStateString(), context);
                } else {
                    LOG.error("Got ACTION_DEVICE_CHANGED from unexpected device: " + mGBDevice);
                }
            }
        }
    };

    @Override
    public void onCreate() {
        LOG.debug("DeviceCommunicationService is being created");
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

        if (!action.equals(ACTION_START) && !action.equals(ACTION_CONNECT)) {
            if (!mStarted) {
                // using the service before issuing ACTION_START
                LOG.info("Must start service with " + ACTION_START + " or " + ACTION_CONNECT + " before using it: " + action);
                return START_NOT_STICKY;
            }

            if (mDeviceSupport == null || (!isInitialized() && !mDeviceSupport.useAutoConnect())) {
                // trying to send notification without valid Bluetooth connection
                if (mGBDevice != null) {
                    // at least send back the current device state
                    mGBDevice.sendDeviceUpdateIntent(this);
                }
                return START_STICKY;
            }
        }

        // when we get past this, we should have valid mDeviceSupport and mGBDevice instances

        switch (action) {
            case ACTION_START:
                start();
                break;
            case ACTION_CONNECT:
                start(); // ensure started
                String btDeviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS);
                SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
                if (btDeviceAddress == null) {
                    btDeviceAddress = sharedPrefs.getString("last_device_address", null);
                } else {
                    sharedPrefs.edit().putString("last_device_address", btDeviceAddress).apply();
                }

                if (btDeviceAddress != null && !isConnected() && !isConnecting()) {
                    if (mDeviceSupport != null) {
                        mDeviceSupport.dispose();
                        mDeviceSupport = null;
                    }
                    try {
                        DeviceSupportFactory factory = new DeviceSupportFactory(this);
                        mDeviceSupport = factory.createDeviceSupport(btDeviceAddress);
                        if (mDeviceSupport != null) {
                            mGBDevice = mDeviceSupport.getDevice();
                            if (pair) {
                                mDeviceSupport.pair();
                            } else {
                                mDeviceSupport.connect();
                            }
                        }
                    } catch (Exception e) {
                        GB.toast(this, getString(R.string.cannot_connect, e.getMessage()), Toast.LENGTH_SHORT, GB.ERROR);
                        mDeviceSupport = null;
                        mGBDevice = null;
                    }
                }
                break;
            case ACTION_REQUEST_VERSIONINFO:
                if (mGBDevice.getFirmwareVersion() == null) {
                    mDeviceSupport.onFirmwareVersionReq();
                } else {
                    mGBDevice.sendDeviceUpdateIntent(this);
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
            case ACTION_REBOOT: {
                mDeviceSupport.onReboot();
                break;
            }
            case ACTION_FETCH_ACTIVITY_DATA: {
                mDeviceSupport.onFetchActivityData();
                break;
            }
            case ACTION_DISCONNECT: {
                mDeviceSupport.dispose();
                mDeviceSupport = null;
                break;
            }
            case ACTION_FIND_DEVICE: {
                boolean start = intent.getBooleanExtra("find_start", false);
                mDeviceSupport.onFindDevice(start);
                break;
            }
            case ACTION_CALLSTATE:
                ServiceCommand command = ServiceCommand.values()[intent.getIntExtra("call_command", 0)]; // UGLY

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
            case ACTION_REQUEST_APPINFO:
                mDeviceSupport.onAppInfoReq();
                break;
            case ACTION_REQUEST_SCREENSHOT:
                mDeviceSupport.onScreenshotReq();
                break;
            case ACTION_STARTAPP:
                UUID uuid = UUID.fromString(intent.getStringExtra("app_uuid"));
                mDeviceSupport.onAppStart(uuid);
                break;
            case ACTION_DELETEAPP:
                uuid = UUID.fromString(intent.getStringExtra("app_uuid"));
                mDeviceSupport.onAppDelete(uuid);
                break;
            case ACTION_INSTALL:
                Uri uri = intent.getParcelableExtra("uri");
                if (uri != null) {
                    LOG.info("will try to install app/fw");
                    mDeviceSupport.onInstallApp(uri);
                }
                break;
            case ACTION_SET_ALARMS:
                ArrayList<Alarm> alarms = intent.getParcelableArrayListExtra("alarms");
                mDeviceSupport.onSetAlarms(alarms);
                break;
        }

        return START_STICKY;
    }

    private void start() {
        if (!mStarted) {
            startForeground(GB.NOTIFICATION_ID, GB.createNotification(getString(R.string.gadgetbridge_running), this));
            mStarted = true;
        }
    }

    private boolean isConnected() {
        return mGBDevice != null && mGBDevice.isConnected();
    }

    private boolean isConnecting() {
        return mGBDevice != null && mGBDevice.isConnecting();
    }

    private boolean isInitialized() {
        return mGBDevice != null && mGBDevice.isInitialized();
    }

    @Override
    public void onDestroy() {
        LOG.debug("DeviceCommunicationService is being destroyed");
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
