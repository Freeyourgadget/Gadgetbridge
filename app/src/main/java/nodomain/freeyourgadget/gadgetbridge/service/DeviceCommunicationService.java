package nodomain.freeyourgadget.gadgetbridge.service;

import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.externalevents.K9Receiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.MusicPlaybackReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.PebbleReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.PhoneCallReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.SMSReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.TimeChangeReceiver;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;
import nodomain.freeyourgadget.gadgetbridge.model.ServiceCommand;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.ACTION_APP_CONFIGURE;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.ACTION_CALLSTATE;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.ACTION_CONNECT;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.ACTION_DELETEAPP;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.ACTION_DISCONNECT;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.ACTION_ENABLE_REALTIME_STEPS;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.ACTION_FETCH_ACTIVITY_DATA;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.ACTION_FIND_DEVICE;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.ACTION_HEARTRATE_TEST;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.ACTION_INSTALL;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.ACTION_NOTIFICATION;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.ACTION_REBOOT;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.ACTION_REQUEST_APPINFO;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.ACTION_REQUEST_DEVICEINFO;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.ACTION_REQUEST_SCREENSHOT;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.ACTION_SETMUSICINFO;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.ACTION_SETTIME;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.ACTION_SET_ALARMS;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.ACTION_START;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.ACTION_STARTAPP;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.EXTRA_ALARMS;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.EXTRA_APP_CONFIG;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.EXTRA_APP_START;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.EXTRA_APP_UUID;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.EXTRA_CALL_COMMAND;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.EXTRA_CALL_PHONENUMBER;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.EXTRA_DEVICE_ADDRESS;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.EXTRA_ENABLE_REALTIME_STEPS;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.EXTRA_FIND_START;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.EXTRA_MUSIC_ALBUM;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.EXTRA_MUSIC_ARTIST;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.EXTRA_MUSIC_TRACK;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.EXTRA_NOTIFICATION_BODY;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.EXTRA_NOTIFICATION_FLAGS;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.EXTRA_NOTIFICATION_ID;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.EXTRA_NOTIFICATION_PHONENUMBER;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.EXTRA_NOTIFICATION_SENDER;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.EXTRA_NOTIFICATION_SOURCENAME;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.EXTRA_NOTIFICATION_SUBJECT;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.EXTRA_NOTIFICATION_TITLE;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.EXTRA_NOTIFICATION_TYPE;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.EXTRA_PERFORM_PAIR;
import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.EXTRA_URI;

public class DeviceCommunicationService extends Service {
    private static final Logger LOG = LoggerFactory.getLogger(DeviceCommunicationService.class);

    private boolean mStarted = false;

    private DeviceSupportFactory mFactory;
    private GBDevice mGBDevice = null;
    private DeviceSupport mDeviceSupport;

    private PhoneCallReceiver mPhoneCallReceiver = null;
    private SMSReceiver mSMSReceiver = null;
    private K9Receiver mK9Receiver = null;
    private PebbleReceiver mPebbleReceiver = null;
    private MusicPlaybackReceiver mMusicPlaybackReceiver = null;
    private TimeChangeReceiver mTimeChangeReceiver = null;

    private Random mRandom = new Random();

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(GBDevice.ACTION_DEVICE_CHANGED)) {
                GBDevice device = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
                if (mGBDevice.equals(device)) {
                    mGBDevice = device;
                    boolean enableReceivers = mDeviceSupport != null && (mDeviceSupport.useAutoConnect() || mGBDevice.isInitialized());
                    setReceiversEnableState(enableReceivers);
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
        mFactory = new DeviceSupportFactory(this);
    }

    @Override
    public synchronized int onStartCommand(Intent intent, int flags, int startId) {

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
                GBDevice gbDevice = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
                if (gbDevice == null) {
                    String btDeviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS);
                    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
                    if (sharedPrefs != null) { // may be null in test cases
                        if (btDeviceAddress == null) {
                            btDeviceAddress = sharedPrefs.getString("last_device_address", null);
                        } else {
                            sharedPrefs.edit().putString("last_device_address", btDeviceAddress).apply();
                        }
                    }
                    if (btDeviceAddress != null) {
                        gbDevice = DeviceHelper.getInstance().findAvailableDevice(btDeviceAddress, this);
                    }
                }

                if (gbDevice != null && !isConnecting() && !isConnected()) {
                    setDeviceSupport(null);
                    try {
                        DeviceSupport deviceSupport = mFactory.createDeviceSupport(gbDevice);
                        if (deviceSupport != null) {
                            setDeviceSupport(deviceSupport);
                            if (pair) {
                                deviceSupport.pair();
                            } else {
                                deviceSupport.connect();
                            }
                        } else {
                            GB.toast(this, getString(R.string.cannot_connect, "Can't create device support"), Toast.LENGTH_SHORT, GB.ERROR);
                        }
                    } catch (Exception e) {
                        GB.toast(this, getString(R.string.cannot_connect, e.getMessage()), Toast.LENGTH_SHORT, GB.ERROR, e);
                        setDeviceSupport(null);
                    }
                } else if (mGBDevice != null) {
                    // send an update at least
                    mGBDevice.sendDeviceUpdateIntent(this);
                }
                break;
            case ACTION_REQUEST_DEVICEINFO:
                mGBDevice.sendDeviceUpdateIntent(this);
                break;
            case ACTION_NOTIFICATION: {
                NotificationSpec notificationSpec = new NotificationSpec();
                notificationSpec.phoneNumber = intent.getStringExtra(EXTRA_NOTIFICATION_PHONENUMBER);
                notificationSpec.sender = intent.getStringExtra(EXTRA_NOTIFICATION_SENDER);
                notificationSpec.subject = intent.getStringExtra(EXTRA_NOTIFICATION_SUBJECT);
                notificationSpec.title = intent.getStringExtra(EXTRA_NOTIFICATION_TITLE);
                notificationSpec.body = intent.getStringExtra(EXTRA_NOTIFICATION_BODY);
                notificationSpec.type = (NotificationType) intent.getSerializableExtra(EXTRA_NOTIFICATION_TYPE);
                notificationSpec.id = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1);
                notificationSpec.flags = intent.getIntExtra(EXTRA_NOTIFICATION_FLAGS, 0);
                notificationSpec.sourceName = intent.getStringExtra(EXTRA_NOTIFICATION_SOURCENAME);
                if (notificationSpec.type == NotificationType.SMS && notificationSpec.phoneNumber != null) {
                    notificationSpec.sender = getContactDisplayNameByNumber(notificationSpec.phoneNumber);

                    notificationSpec.id = mRandom.nextInt(); // FIXME: add this in external SMS Receiver?
                    GBApplication.getIDSenderLookup().add(notificationSpec.id, notificationSpec.phoneNumber);
                }
                if (((notificationSpec.flags & NotificationSpec.FLAG_WEARABLE_REPLY) > 0)
                        || (notificationSpec.type == NotificationType.SMS && notificationSpec.phoneNumber != null)) {
                    // NOTE: maybe not where it belongs
                    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
                    if (sharedPrefs.getBoolean("pebble_force_untested", false)) {
                        // I would rather like to save that as an array in ShadredPreferences
                        // this would work but I dont know how to do the same in the Settings Activity's xml
                        ArrayList<String> replies = new ArrayList<>();
                        for (int i = 1; i <= 16; i++) {
                            String reply = sharedPrefs.getString("canned_reply_" + i, null);
                            if (reply != null && !reply.equals("")) {
                                replies.add(reply);
                            }
                        }
                        notificationSpec.cannedReplies = replies.toArray(new String[replies.size()]);
                    }
                }
                mDeviceSupport.onNotification(notificationSpec);
                break;
            }
            case ACTION_REBOOT: {
                mDeviceSupport.onReboot();
                break;
            }
            case ACTION_HEARTRATE_TEST: {
                mDeviceSupport.onHeartRateTest();
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
                boolean start = intent.getBooleanExtra(EXTRA_FIND_START, false);
                mDeviceSupport.onFindDevice(start);
                break;
            }
            case ACTION_CALLSTATE:
                ServiceCommand command = (ServiceCommand) intent.getSerializableExtra(EXTRA_CALL_COMMAND);

                String phoneNumber = intent.getStringExtra(EXTRA_CALL_PHONENUMBER);
                String callerName = null;
                if (phoneNumber != null) {
                    callerName = getContactDisplayNameByNumber(phoneNumber);
                }
                mDeviceSupport.onSetCallState(phoneNumber, callerName, command);
                break;
            case ACTION_SETTIME:
                mDeviceSupport.onSetTime();
                break;
            case ACTION_SETMUSICINFO:
                String artist = intent.getStringExtra(EXTRA_MUSIC_ARTIST);
                String album = intent.getStringExtra(EXTRA_MUSIC_ALBUM);
                String track = intent.getStringExtra(EXTRA_MUSIC_TRACK);
                mDeviceSupport.onSetMusicInfo(artist, album, track);
                break;
            case ACTION_REQUEST_APPINFO:
                mDeviceSupport.onAppInfoReq();
                break;
            case ACTION_REQUEST_SCREENSHOT:
                mDeviceSupport.onScreenshotReq();
                break;
            case ACTION_STARTAPP: {
                UUID uuid = (UUID) intent.getSerializableExtra(EXTRA_APP_UUID);
                boolean start = intent.getBooleanExtra(EXTRA_APP_START, true);
                mDeviceSupport.onAppStart(uuid, start);
                break;
            }
            case ACTION_DELETEAPP: {
                UUID uuid = (UUID) intent.getSerializableExtra(EXTRA_APP_UUID);
                mDeviceSupport.onAppDelete(uuid);
                break;
            }
            case ACTION_APP_CONFIGURE: {
                UUID uuid = (UUID) intent.getSerializableExtra(EXTRA_APP_UUID);
                String config = intent.getStringExtra(EXTRA_APP_CONFIG);
                mDeviceSupport.onAppConfiguration(uuid, config);
            }
            case ACTION_INSTALL:
                Uri uri = intent.getParcelableExtra(EXTRA_URI);
                if (uri != null) {
                    LOG.info("will try to install app/fw");
                    mDeviceSupport.onInstallApp(uri);
                }
                break;
            case ACTION_SET_ALARMS:
                ArrayList<Alarm> alarms = intent.getParcelableArrayListExtra(EXTRA_ALARMS);
                mDeviceSupport.onSetAlarms(alarms);
                break;
            case ACTION_ENABLE_REALTIME_STEPS:
                boolean enable = intent.getBooleanExtra(EXTRA_ENABLE_REALTIME_STEPS, false);
                mDeviceSupport.onEnableRealtimeSteps(enable);
                break;
        }

        return START_STICKY;
    }

    /**
     * For testing!
     *
     * @param factory
     */
    public void setDeviceSupportFactory(DeviceSupportFactory factory) {
        mFactory = factory;
    }

    /**
     * Disposes the current DeviceSupport instance (if any) and sets a new device support instance
     * (if not null).
     *
     * @param deviceSupport
     */
    private void setDeviceSupport(@Nullable DeviceSupport deviceSupport) {
        if (deviceSupport != mDeviceSupport && mDeviceSupport != null) {
            mDeviceSupport.dispose();
            mDeviceSupport = null;
            mGBDevice = null;
        }
        mDeviceSupport = deviceSupport;
        mGBDevice = mDeviceSupport != null ? mDeviceSupport.getDevice() : null;
    }

    private void start() {
        if (!mStarted) {
            startForeground(GB.NOTIFICATION_ID, GB.createNotification(getString(R.string.gadgetbridge_running), this));
            mStarted = true;
        }
    }

    public boolean isStarted() {
        return mStarted;
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


    private void setReceiversEnableState(boolean enable) {
        LOG.info("Setting broadcast receivers to: " + enable);

        if (enable) {
            if (mPhoneCallReceiver == null) {
                mPhoneCallReceiver = new PhoneCallReceiver();
                IntentFilter filter = new IntentFilter();
                filter.addAction("android.intent.action.PHONE_STATE");
                filter.addAction("android.intent.action.NEW_OUTGOING_CALL");
                registerReceiver(mPhoneCallReceiver, filter);
            }
            if (mSMSReceiver == null) {
                mSMSReceiver = new SMSReceiver();
                registerReceiver(mSMSReceiver, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));
            }
            if (mK9Receiver == null) {
                mK9Receiver = new K9Receiver();
                IntentFilter filter = new IntentFilter();
                filter.addDataScheme("email");
                filter.addAction("com.fsck.k9.intent.action.EMAIL_RECEIVED");
                registerReceiver(mK9Receiver, filter);
            }
            if (mPebbleReceiver == null) {
                mPebbleReceiver = new PebbleReceiver();
                registerReceiver(mPebbleReceiver, new IntentFilter("com.getpebble.action.SEND_NOTIFICATION"));
            }
            if (mMusicPlaybackReceiver == null) {
                mMusicPlaybackReceiver = new MusicPlaybackReceiver();
                registerReceiver(mMusicPlaybackReceiver, new IntentFilter("com.android.music.metachanged"));
            }
            if (mTimeChangeReceiver == null) {
                mTimeChangeReceiver = new TimeChangeReceiver();
                IntentFilter filter = new IntentFilter();
                filter.addAction("android.intent.action.TIME_SET");
                filter.addAction("android.intent.action.TIMEZONE_CHANGED");
                registerReceiver(mTimeChangeReceiver, filter);
            }
        } else {
            if (mPhoneCallReceiver != null) {
                unregisterReceiver(mPhoneCallReceiver);
                mPhoneCallReceiver = null;
            }
            if (mSMSReceiver != null) {
                unregisterReceiver(mSMSReceiver);
                mSMSReceiver = null;
            }
            if (mK9Receiver != null) {
                unregisterReceiver(mK9Receiver);
                mK9Receiver = null;
            }
            if (mPebbleReceiver != null) {
                unregisterReceiver(mPebbleReceiver);
                mPebbleReceiver = null;
            }
            if (mMusicPlaybackReceiver != null) {
                unregisterReceiver(mMusicPlaybackReceiver);
                mMusicPlaybackReceiver = null;
            }
            if (mTimeChangeReceiver != null) {
                unregisterReceiver(mTimeChangeReceiver);
                mTimeChangeReceiver = null;
            }
        }
    }

    @Override
    public void onDestroy() {
        LOG.debug("DeviceCommunicationService is being destroyed");
        super.onDestroy();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        setReceiversEnableState(false); // disable BroadcastReceivers

        setDeviceSupport(null);
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

        try (Cursor contactLookup = getContentResolver().query(uri, null, null, null, null)) {
            if (contactLookup != null && contactLookup.getCount() > 0) {
                contactLookup.moveToNext();
                name = contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
            }
        } catch (SecurityException e) {
            // ignore, just return name below
        }

        return name;
    }
}
