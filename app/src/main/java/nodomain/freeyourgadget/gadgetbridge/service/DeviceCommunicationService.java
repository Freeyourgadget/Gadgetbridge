/*  Copyright (C) 2015-2018 Andreas Shimokawa, Avamander, Carsten Pfeiffer,
    Daniele Gobbetti, Daniel Hauck, Frank Slezak, ivanovlev, João Paulo Barraca,
    Julien Pivotto, Kasha, Sergey Trofimov, Steffen Liebergeld, Uwe Hermann,
    Taavi Eomäe

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
package nodomain.freeyourgadget.gadgetbridge.service;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.externalevents.*;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.*;

public class DeviceCommunicationService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final Logger LOG = LoggerFactory.getLogger(DeviceCommunicationService.class);
    @SuppressLint("StaticFieldLeak") // only used for test cases
    private static DeviceSupportFactory DEVICE_SUPPORT_FACTORY = null;
    private final String[] mMusicActions = {
            "com.android.music.metachanged",
            "com.android.music.playstatechanged",
            "com.android.music.queuechanged",
            "com.android.music.playbackcomplete",
            "net.sourceforge.subsonic.androidapp.EVENT_META_CHANGED",
            "com.maxmpz.audioplayer.TPOS_SYNC",
            "com.maxmpz.audioplayer.STATUS_CHANGED",
            "com.maxmpz.audioplayer.PLAYING_MODE_CHANGED",
            "com.spotify.music.metadatachanged",
            "com.spotify.music.playbackstatechanged"
    };
    private ArrayList<DeviceContainer> deviceContainerArrayList = new ArrayList<>();
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (GBDevice.ACTION_DEVICE_CHANGED.equals(action)) {
                GBDevice device = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
                DeviceContainer container = getContainerForDevice(device);

                if (container != null && container.getGBDevice() != null) {
                    boolean enableReceivers = container.getDeviceSupport() != null && (container.getDeviceSupport().useAutoConnect() || container.getGBDevice().isInitialized());
                    setReceiversEnableState(enableReceivers, container.getGBDevice().isInitialized(), DeviceHelper.getInstance().getCoordinator(device), container);
                    GB.updateNotification(container.getGBDevice(), context);
                }
            }
        }
    };
    private Boolean mServiceStarted = false;

    public DeviceCommunicationService() {

    }

    @Override
    public void onCreate() {
        LOG.debug("DeviceCommunicationService is being created");
        super.onCreate();
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter(GBDevice.ACTION_DEVICE_CHANGED));

        if (hasPrefs()) {
            getPrefs().getPreferences().registerOnSharedPreferenceChangeListener(this);
        }
    }

    private DeviceSupportFactory getDeviceSupportFactory() {
        if (DEVICE_SUPPORT_FACTORY != null) {
            return DEVICE_SUPPORT_FACTORY;
        }
        return new DeviceSupportFactory(this);
    }

    /**
     * For testing!
     *
     * @param factory to use for testing
     */
    public static void setDeviceSupportFactory(DeviceSupportFactory factory) {
        DEVICE_SUPPORT_FACTORY = factory;
    }

    @Override
    public synchronized int onStartCommand(Intent intent, int flags, int startId) {

        if (intent == null) {
            LOG.info("no intent");
            return START_NOT_STICKY;
        }

        String action = intent.getAction();
        boolean firstTime = intent.getBooleanExtra(EXTRA_CONNECT_FIRST_TIME, false);

        if (action == null) {
            LOG.info("no action");
            return START_NOT_STICKY;
        }

        LOG.debug("Service start command: " + action);
        if (action.equals(ACTION_START)) {
            start();
            return START_STICKY;
        }

        GBDevice intentGBDevice = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
        DeviceContainer intentGBDeviceContainer = getContainerForDevice(intentGBDevice);
        if (intentGBDeviceContainer == null) {
            if (intentGBDevice != null) {
                intentGBDevice.sendDeviceUpdateIntent(this);
            }
            return START_NOT_STICKY;
        }

        if (!action.equals(ACTION_START) && !action.equals(ACTION_CONNECT)) {
            if (!mServiceStarted) {
                // using the service before issuing ACTION_START
                LOG.info("Must start service with " + ACTION_START + " or " + ACTION_CONNECT + " before using it: " + action);
                return START_NOT_STICKY;
            }

            if (intentGBDeviceContainer.getDeviceSupport() == null || (!isInitialized(intentGBDevice) && !intentGBDeviceContainer.getDeviceSupport().useAutoConnect())) {
                // trying to send notification without valid Bluetooth connection
                if (intentGBDeviceContainer.getGBDevice() != null) {
                    // at least send back the current device state
                    intentGBDeviceContainer.getGBDevice().sendDeviceUpdateIntent(this);
                }
                return START_STICKY;
            }
        }

        // when we get past this, we should have valid mDeviceSupport and mGBDevice instances

        Prefs prefs = getPrefs();
        switch (action) {
/*            case ACTION_START: {
                start();
                break;
                }*/
            case ACTION_CONNECT: {
                start(); // ensure started
                String btDeviceAddress;
                btDeviceAddress = intentGBDevice.getAddress();

                boolean autoReconnect = GBPrefs.AUTO_RECONNECT_DEFAULT;
                if (prefs != null && prefs.getPreferences() != null) {
                    prefs.getPreferences().edit().putString("last_device_address", btDeviceAddress).apply();
                    autoReconnect = getGBPrefs().getAutoReconnect();
                }

                if (!isConnecting(intentGBDevice) && !isConnected(intentGBDevice)) {
                    setDeviceSupport(null, intentGBDeviceContainer);
                    try {
                        DeviceSupportFactory factory = getDeviceSupportFactory();
                        intentGBDeviceContainer.setDeviceSupportFactory(factory);
                        DeviceSupport deviceSupport = factory.createDeviceSupport(intentGBDevice);
                        if (deviceSupport != null) {
                            setDeviceSupport(deviceSupport, intentGBDeviceContainer);
                            if (firstTime) {
                                deviceSupport.connectFirstTime();
                            } else {
                                deviceSupport.setAutoReconnect(autoReconnect);
                                deviceSupport.connect();
                            }
                        } else {
                            GB.toast(this, getString(R.string.cannot_connect, "Can't create device support"), Toast.LENGTH_SHORT, GB.ERROR);
                        }
                    } catch (Exception e) {
                        GB.toast(this, getString(R.string.cannot_connect, e.getMessage()), Toast.LENGTH_SHORT, GB.ERROR, e);
                        setDeviceSupport(null, intentGBDeviceContainer);
                    }
                } else {
                    // send an update at least
                    intentGBDevice.sendDeviceUpdateIntent(this);
                }
                break;
            }
            case ACTION_REQUEST_DEVICEINFO: {
                intentGBDevice.sendDeviceUpdateIntent(this);
                break;
            }
            case ACTION_NOTIFICATION: {
                NotificationSpec notificationSpec = new NotificationSpec();
                notificationSpec.phoneNumber = intent.getStringExtra(EXTRA_NOTIFICATION_PHONENUMBER);
                notificationSpec.sender = intent.getStringExtra(EXTRA_NOTIFICATION_SENDER);
                notificationSpec.subject = intent.getStringExtra(EXTRA_NOTIFICATION_SUBJECT);
                notificationSpec.title = intent.getStringExtra(EXTRA_NOTIFICATION_TITLE);
                notificationSpec.body = intent.getStringExtra(EXTRA_NOTIFICATION_BODY);
                notificationSpec.sourceName = intent.getStringExtra(EXTRA_NOTIFICATION_SOURCENAME);
                notificationSpec.type = (NotificationType) intent.getSerializableExtra(EXTRA_NOTIFICATION_TYPE);
                notificationSpec.pebbleColor = (byte) intent.getSerializableExtra(EXTRA_NOTIFICATION_PEBBLE_COLOR);
                notificationSpec.id = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1);
                notificationSpec.flags = intent.getIntExtra(EXTRA_NOTIFICATION_FLAGS, 0);

                if (notificationSpec.type == NotificationType.GENERIC_SMS && notificationSpec.phoneNumber != null) {
                    notificationSpec.id = intentGBDeviceContainer.getRandom().nextInt(); // FIXME: add this in external SMS Receiver?
                    GBApplication.getIDSenderLookup().add(notificationSpec.id, notificationSpec.phoneNumber);
                }

                if (((notificationSpec.flags & NotificationSpec.FLAG_WEARABLE_REPLY) > 0)
                        || (notificationSpec.type == NotificationType.GENERIC_SMS && notificationSpec.phoneNumber != null)) {
                    // NOTE: maybe not where it belongs
                    if (prefs.getBoolean("pebble_force_untested", false)) {
                        // I would rather like to save that as an array in SharedPreferences
                        // this would work but I don't know how to do the same in the Settings Activity's xml
                        ArrayList<String> replies = new ArrayList<>();
                        for (int i = 1; i <= 16; i++) {
                            String reply = prefs.getString("canned_reply_" + i, null);
                            if (reply != null && !reply.equals("")) {
                                replies.add(reply);
                            }
                        }
                        notificationSpec.cannedReplies = replies.toArray(new String[replies.size()]);
                    }
                }

                intentGBDeviceContainer.getDeviceSupport().onNotification(notificationSpec);
                break;
            }
            case ACTION_DELETE_NOTIFICATION: {
                intentGBDeviceContainer.getDeviceSupport().onDeleteNotification(intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1));
                break;
            }
            case ACTION_ADD_CALENDAREVENT: {
                CalendarEventSpec calendarEventSpec = new CalendarEventSpec();
                calendarEventSpec.id = intent.getLongExtra(EXTRA_CALENDAREVENT_ID, -1);
                calendarEventSpec.type = intent.getByteExtra(EXTRA_CALENDAREVENT_TYPE, (byte) -1);
                calendarEventSpec.timestamp = intent.getIntExtra(EXTRA_CALENDAREVENT_TIMESTAMP, -1);
                calendarEventSpec.durationInSeconds = intent.getIntExtra(EXTRA_CALENDAREVENT_DURATION, -1);
                calendarEventSpec.title = intent.getStringExtra(EXTRA_CALENDAREVENT_TITLE);
                calendarEventSpec.description = intent.getStringExtra(EXTRA_CALENDAREVENT_DESCRIPTION);
                calendarEventSpec.location = intent.getStringExtra(EXTRA_CALENDAREVENT_LOCATION);
                intentGBDeviceContainer.getDeviceSupport().onAddCalendarEvent(calendarEventSpec);
                break;
            }
            case ACTION_DELETE_CALENDAREVENT: {
                long id = intent.getLongExtra(EXTRA_CALENDAREVENT_ID, -1);
                byte type = intent.getByteExtra(EXTRA_CALENDAREVENT_TYPE, (byte) -1);
                intentGBDeviceContainer.getDeviceSupport().onDeleteCalendarEvent(type, id);
                break;
            }
            case ACTION_REBOOT: {
                intentGBDeviceContainer.getDeviceSupport().onReboot();
                break;
            }
            case ACTION_HEARTRATE_TEST: {
                intentGBDeviceContainer.getDeviceSupport().onHeartRateTest();
                break;
            }
            case ACTION_FETCH_ACTIVITY_DATA: {
                intentGBDeviceContainer.getDeviceSupport().onFetchActivityData();
                break;
            }
            case ACTION_DISCONNECT: {
                intentGBDeviceContainer.getDeviceSupport().dispose();
                if (intentGBDevice.getState() == GBDevice.State.WAITING_FOR_RECONNECT) {
                    setReceiversEnableState(false, false, null, intentGBDeviceContainer);
                    intentGBDevice.setState(GBDevice.State.NOT_CONNECTED);
                    intentGBDevice.sendDeviceUpdateIntent(this);
                }
                intentGBDeviceContainer.setDeviceSupport(null);
                break;
            }
            case ACTION_FIND_DEVICE: {
                boolean start = intent.getBooleanExtra(EXTRA_FIND_START, false);
                intentGBDeviceContainer.getDeviceSupport().onFindDevice(start);
                break;
            }
            case ACTION_SET_CONSTANT_VIBRATION: {
                int intensity = intent.getIntExtra(EXTRA_VIBRATION_INTENSITY, 0);
                intentGBDeviceContainer.getDeviceSupport().onSetConstantVibration(intensity);
                break;
            }
            case ACTION_CALLSTATE: {
                CallSpec callSpec = new CallSpec();
                callSpec.command = intent.getIntExtra(EXTRA_CALL_COMMAND, CallSpec.CALL_UNDEFINED);
                callSpec.number = intent.getStringExtra(EXTRA_CALL_PHONENUMBER);
                callSpec.name = intent.getStringExtra(EXTRA_CALL_DISPLAYNAME);
                intentGBDeviceContainer.getDeviceSupport().onSetCallState(callSpec);
                break;
            }
            case ACTION_SETCANNEDMESSAGES: {
                int type = intent.getIntExtra(EXTRA_CANNEDMESSAGES_TYPE, -1);
                String[] cannedMessages = intent.getStringArrayExtra(EXTRA_CANNEDMESSAGES);

                CannedMessagesSpec cannedMessagesSpec = new CannedMessagesSpec();
                cannedMessagesSpec.type = type;
                cannedMessagesSpec.cannedMessages = cannedMessages;
                intentGBDeviceContainer.getDeviceSupport().onSetCannedMessages(cannedMessagesSpec);
                break;
            }
            case ACTION_SETTIME: {
                intentGBDeviceContainer.getDeviceSupport().onSetTime();
                break;
            }
            case ACTION_SETMUSICINFO: {
                MusicSpec musicSpec = new MusicSpec();
                musicSpec.artist = intent.getStringExtra(EXTRA_MUSIC_ARTIST);
                musicSpec.album = intent.getStringExtra(EXTRA_MUSIC_ALBUM);
                musicSpec.track = intent.getStringExtra(EXTRA_MUSIC_TRACK);
                musicSpec.duration = intent.getIntExtra(EXTRA_MUSIC_DURATION, 0);
                musicSpec.trackCount = intent.getIntExtra(EXTRA_MUSIC_TRACKCOUNT, 0);
                musicSpec.trackNr = intent.getIntExtra(EXTRA_MUSIC_TRACKNR, 0);
                intentGBDeviceContainer.getDeviceSupport().onSetMusicInfo(musicSpec);
                break;
            }
            case ACTION_SETMUSICSTATE: {
                MusicStateSpec stateSpec = new MusicStateSpec();
                stateSpec.shuffle = intent.getByteExtra(EXTRA_MUSIC_SHUFFLE, (byte) 0);
                stateSpec.repeat = intent.getByteExtra(EXTRA_MUSIC_REPEAT, (byte) 0);
                stateSpec.position = intent.getIntExtra(EXTRA_MUSIC_POSITION, 0);
                stateSpec.playRate = intent.getIntExtra(EXTRA_MUSIC_RATE, 0);
                stateSpec.state = intent.getByteExtra(EXTRA_MUSIC_STATE, (byte) 0);
                intentGBDeviceContainer.getDeviceSupport().onSetMusicState(stateSpec);
                break;
            }
            case ACTION_REQUEST_APPINFO: {
                intentGBDeviceContainer.getDeviceSupport().onAppInfoReq();
                break;
            }
            case ACTION_REQUEST_SCREENSHOT: {
                intentGBDeviceContainer.getDeviceSupport().onScreenshotReq();
                break;
            }
            case ACTION_STARTAPP: {
                UUID uuid = (UUID) intent.getSerializableExtra(EXTRA_APP_UUID);
                boolean start = intent.getBooleanExtra(EXTRA_APP_START, true);
                intentGBDeviceContainer.getDeviceSupport().onAppStart(uuid, start);
                break;
            }
            case ACTION_DELETEAPP: {
                UUID uuid = (UUID) intent.getSerializableExtra(EXTRA_APP_UUID);
                intentGBDeviceContainer.getDeviceSupport().onAppDelete(uuid);
                break;
            }
            case ACTION_APP_CONFIGURE: {
                UUID uuid = (UUID) intent.getSerializableExtra(EXTRA_APP_UUID);
                String config = intent.getStringExtra(EXTRA_APP_CONFIG);
                Integer id = null;
                if (intent.hasExtra(EXTRA_APP_CONFIG_ID)) {
                    id = intent.getIntExtra(EXTRA_APP_CONFIG_ID, 0);
                }
                intentGBDeviceContainer.getDeviceSupport().onAppConfiguration(uuid, config, id);
                break;
            }
            case ACTION_APP_REORDER: {
                UUID[] uuids = (UUID[]) intent.getSerializableExtra(EXTRA_APP_UUID);
                intentGBDeviceContainer.getDeviceSupport().onAppReorder(uuids);
                break;
            }
            case ACTION_INSTALL: {
                Uri uri = intent.getParcelableExtra(EXTRA_URI);
                if (uri != null) {
                    LOG.info("will try to install app/fw");
                    intentGBDeviceContainer.getDeviceSupport().onInstallApp(uri);
                }
                break;
            }
            case ACTION_SET_ALARMS: {
                ArrayList<Alarm> alarms = intent.getParcelableArrayListExtra(EXTRA_ALARMS);
                intentGBDeviceContainer.getDeviceSupport().onSetAlarms(alarms);
                break;
            }
            case ACTION_ENABLE_REALTIME_STEPS: {
                boolean enable = intent.getBooleanExtra(EXTRA_BOOLEAN_ENABLE, false);
                intentGBDeviceContainer.getDeviceSupport().onEnableRealtimeSteps(enable);
                break;
            }
            case ACTION_ENABLE_HEARTRATE_SLEEP_SUPPORT: {
                boolean enable = intent.getBooleanExtra(EXTRA_BOOLEAN_ENABLE, false);
                intentGBDeviceContainer.getDeviceSupport().onEnableHeartRateSleepSupport(enable);
                break;
            }
            case ACTION_SET_HEARTRATE_MEASUREMENT_INTERVAL: {
                Integer seconds = intent.getIntExtra(EXTRA_INTERVAL_SECONDS, 0);
                intentGBDeviceContainer.getDeviceSupport().onSetHeartRateMeasurementInterval(seconds);
                break;
            }
            case ACTION_ENABLE_REALTIME_HEARTRATE_MEASUREMENT: {
                boolean enable = intent.getBooleanExtra(EXTRA_BOOLEAN_ENABLE, false);
                intentGBDeviceContainer.getDeviceSupport().onEnableRealtimeHeartRateMeasurement(enable);
                break;
            }
            case ACTION_SEND_CONFIGURATION: {
                String config = intent.getStringExtra(EXTRA_CONFIG);
                intentGBDeviceContainer.getDeviceSupport().onSendConfiguration(config);
                break;
            }
            case ACTION_TEST_NEW_FUNCTION: {
                intentGBDeviceContainer.getDeviceSupport().onTestNewFunction();
                break;
            }
            case ACTION_SEND_WEATHER: {
                WeatherSpec weatherSpec = intent.getParcelableExtra(EXTRA_WEATHER);
                if (weatherSpec != null) {
                    intentGBDeviceContainer.getDeviceSupport().onSendWeather(weatherSpec);
                }
                break;
            }
        }

        return START_STICKY;
    }

    /**
     * Disposes the current DeviceSupport instance (if any) and sets a new device support instance
     * (if not null).
     *
     * @param deviceSupport to dispose
     */
    private void setDeviceSupport(@Nullable DeviceSupport deviceSupport, DeviceContainer deviceContainer) {
        if (deviceContainer == null) {
            GBDevice mGBDevice = deviceSupport != null ? deviceSupport.getDevice() : null;
            if (mGBDevice != null) {
                deviceContainerArrayList.add(new DeviceContainer(mGBDevice, deviceSupport));
            }
        } else {
            DeviceSupport mDeviceSupport = deviceContainer.getDeviceSupport();

            if (deviceSupport != mDeviceSupport && mDeviceSupport != null) {
                mDeviceSupport.dispose();
            }

            deviceContainer.setDeviceSupport(deviceSupport);
            deviceContainer.setGBDevice(deviceSupport != null ? deviceSupport.getDevice() : null);
        }
    }

    private DeviceContainer getContainerForDevice(GBDevice device) {
        if (device == null) {
            return null;
        }

        for (DeviceContainer container : deviceContainerArrayList) {
            if (container.getGBDevice().equals(device)) {
                return container;
            }
        }
        DeviceContainer container = new DeviceContainer(device);
        deviceContainerArrayList.add(container);
        return container;
    }

    private void start() {
        if (!mServiceStarted) {
            startForeground(GB.NOTIFICATION_ID, GB.createNotification(getString(R.string.gadgetbridge_running), this));
            mServiceStarted = true;
        }
    }

    public boolean isStarted() {
        return mServiceStarted;
    }

    private boolean isConnected(GBDevice gbDevice) {
        return gbDevice != null && gbDevice.isConnected();
    }

    private boolean isConnecting(GBDevice gbDevice) {
        return gbDevice != null && gbDevice.isConnecting();
    }

    private boolean isInitialized(GBDevice gbDevice) {
        return gbDevice != null && gbDevice.isInitialized();
    }

    private void setReceiversEnableState(boolean enable, boolean initialized, DeviceCoordinator coordinator, DeviceContainer deviceContainer) {
        LOG.info("Setting broadcast receivers to: " + enable);

        if (enable && initialized && coordinator != null && coordinator.supportsCalendarEvents()) {
            if (deviceContainer.getCalendarReceiver() == null && getPrefs().getBoolean("enable_calendar_sync", true)) {
                if (!(GBApplication.isRunningMarshmallowOrLater() && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_DENIED)) {
                    IntentFilter calendarIntentFilter = new IntentFilter();
                    calendarIntentFilter.addAction("android.intent.action.PROVIDER_CHANGED");
                    calendarIntentFilter.addDataScheme("content");
                    calendarIntentFilter.addDataAuthority("com.android.calendar", null);
                    deviceContainer.setCalendarReceiver(new CalendarReceiver(deviceContainer.getGBDevice()));
                    registerReceiver(deviceContainer.getCalendarReceiver(), calendarIntentFilter);
                }
            }
            if (deviceContainer.getAlarmReceiver() == null) {
                deviceContainer.setAlarmReceiver(new AlarmReceiver());
                registerReceiver(deviceContainer.getAlarmReceiver(), new IntentFilter("DAILY_ALARM"));
            }
        } else {
            if (deviceContainer.getCalendarReceiver() != null) {
                unregisterReceiver(deviceContainer.getCalendarReceiver());
                deviceContainer.setCalendarReceiver(null);
            }
            if (deviceContainer.getAlarmReceiver() != null) {
                unregisterReceiver(deviceContainer.getAlarmReceiver());
                deviceContainer.setAlarmReceiver(null);
            }
        }

        if (enable) {
            if (deviceContainer.getPhoneCallReceiver() == null) {
                deviceContainer.setPhoneCallReceiver(new PhoneCallReceiver());
                IntentFilter filter = new IntentFilter();
                filter.addAction("android.intent.action.PHONE_STATE");
                filter.addAction("android.intent.action.NEW_OUTGOING_CALL");
                registerReceiver(deviceContainer.getPhoneCallReceiver(), filter);
            }
            if (deviceContainer.getSMSReceiver() == null) {
                deviceContainer.setSMSReceiver(new SMSReceiver());
                registerReceiver(deviceContainer.getSMSReceiver(), new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));
            }
            if (deviceContainer.getPebbleReceiver() == null) {
                deviceContainer.setPebbleReceiver(new PebbleReceiver());
                registerReceiver(deviceContainer.getPebbleReceiver(), new IntentFilter("com.getpebble.action.SEND_NOTIFICATION"));
            }
            if (deviceContainer.getMusicPlaybackReceiver() == null) {
                deviceContainer.setMusicPlaybackReceiver(new MusicPlaybackReceiver());
                IntentFilter filter = new IntentFilter();
                for (String action : mMusicActions) {
                    filter.addAction(action);
                }
                registerReceiver(deviceContainer.getMusicPlaybackReceiver(), filter);
            }
            if (deviceContainer.getTimeChangeReceiver() == null) {
                deviceContainer.setTimeChangeReceiver(new TimeChangeReceiver());
                IntentFilter filter = new IntentFilter();
                filter.addAction("android.intent.action.TIME_SET");
                filter.addAction("android.intent.action.TIMEZONE_CHANGED");
                registerReceiver(deviceContainer.getTimeChangeReceiver(), filter);
            }
            if (deviceContainer.getBlueToothConnectReceiver() == null) {
                deviceContainer.setBlueToothConnectReceiver(new BluetoothConnectReceiver(this));
                registerReceiver(deviceContainer.getBlueToothConnectReceiver(), new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));
            }
            if (deviceContainer.getBlueToothPairingRequestReceiver() == null) {
                deviceContainer.setBlueToothPairingRequestReceiver(new BluetoothPairingRequestReceiver(this));
                registerReceiver(deviceContainer.getBlueToothPairingRequestReceiver(), new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST));
            }
            if (deviceContainer.getAlarmClockReceiver() == null) {
                deviceContainer.setAlarmClockReceiver(new AlarmClockReceiver());
                IntentFilter filter = new IntentFilter();
                filter.addAction(AlarmClockReceiver.ALARM_ALERT_ACTION);
                filter.addAction(AlarmClockReceiver.ALARM_DONE_ACTION);
                registerReceiver(deviceContainer.getAlarmClockReceiver(), filter);
            }
            if (deviceContainer.getCMWeatherReceiver() == null && coordinator != null && coordinator.supportsWeather()) {
                deviceContainer.setCMWeatherReceiver(new CMWeatherReceiver());
                registerReceiver(deviceContainer.getCMWeatherReceiver(), new IntentFilter("GB_UPDATE_WEATHER"));
            }
            if (deviceContainer.getOmniJawsObserver() == null && coordinator != null && coordinator.supportsWeather()) {
                try {
                    deviceContainer.setOmniJawsObserver(new OmniJawsObserver(new Handler()));
                    getContentResolver().registerContentObserver(OmniJawsObserver.WEATHER_URI, true, deviceContainer.getOmniJawsObserver());
                } catch (PackageManager.NameNotFoundException e) {
                    //Nothing wrong, it just means we're not running on omnirom.
                }
            }
        } else {
            if (deviceContainer.getPhoneCallReceiver() != null) {
                unregisterReceiver(deviceContainer.getPhoneCallReceiver());
                deviceContainer.setPhoneCallReceiver(null);
            }
            if (deviceContainer.getSMSReceiver() != null) {
                unregisterReceiver(deviceContainer.getSMSReceiver());
                deviceContainer.setSMSReceiver(null);
            }
            if (deviceContainer.getPebbleReceiver() != null) {
                unregisterReceiver(deviceContainer.getPebbleReceiver());
                deviceContainer.setPebbleReceiver(null);
            }
            if (deviceContainer.getMusicPlaybackReceiver() != null) {
                unregisterReceiver(deviceContainer.getMusicPlaybackReceiver());
                deviceContainer.setMusicPlaybackReceiver(null);
            }
            if (deviceContainer.getTimeChangeReceiver() != null) {
                unregisterReceiver(deviceContainer.getTimeChangeReceiver());
                deviceContainer.setTimeChangeReceiver(null);
            }
            if (deviceContainer.getBlueToothConnectReceiver() != null) {
                unregisterReceiver(deviceContainer.getBlueToothConnectReceiver());
                deviceContainer.setBlueToothConnectReceiver(null);
            }

            if (deviceContainer.getBlueToothPairingRequestReceiver() != null) {
                unregisterReceiver(deviceContainer.getBlueToothPairingRequestReceiver());
                deviceContainer.setBlueToothPairingRequestReceiver(null);
            }
            if (deviceContainer.getAlarmClockReceiver() != null) {
                unregisterReceiver(deviceContainer.getAlarmClockReceiver());
                deviceContainer.setAlarmClockReceiver(null);
            }
            if (deviceContainer.getCMWeatherReceiver() != null) {
                unregisterReceiver(deviceContainer.getCMWeatherReceiver());
                deviceContainer.setCMWeatherReceiver(null);
            }
            if (deviceContainer.getOmniJawsObserver() != null) {
                getContentResolver().unregisterContentObserver(deviceContainer.getOmniJawsObserver());
                deviceContainer.setOmniJawsObserver(null);
            }
        }
    }

    @Override
    public void onDestroy() {
        if (hasPrefs()) {
            getPrefs().getPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        LOG.debug("DeviceCommunicationService is being destroyed");
        super.onDestroy();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);

        for (DeviceContainer container : deviceContainerArrayList) {
            setReceiversEnableState(false, false, null, container); // disable BroadcastReceivers
            setDeviceSupport(null, container);
        }

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.cancel(GB.NOTIFICATION_ID); // need to do this because the updated notification won't be cancelled when service stops
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (GBPrefs.AUTO_RECONNECT.equals(key)) {
            boolean autoReconnect = getGBPrefs().getAutoReconnect();
            DeviceSupport mDeviceSupport = null; // TODO: Fix
            if (mDeviceSupport != null) {
                mDeviceSupport.setAutoReconnect(autoReconnect);
            }
        }
    }

    protected boolean hasPrefs() {
        return getPrefs().getPreferences() != null;
    }

    public Prefs getPrefs() {
        return GBApplication.getPrefs();
    }

    public GBPrefs getGBPrefs() {
        return GBApplication.getGBPrefs();
    }

    public ArrayList<DeviceContainer> getGBDevices() {
        return deviceContainerArrayList;
    }
}
