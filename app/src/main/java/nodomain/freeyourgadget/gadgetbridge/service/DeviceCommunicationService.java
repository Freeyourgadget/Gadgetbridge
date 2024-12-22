/*  Copyright (C) 2015-2024 Andreas Böhler, Andreas Shimokawa, Arjan
    Schrijver, Avamander, Carsten Pfeiffer, Daniel Dakhno, Daniele Gobbetti,
    Daniel Hauck, Davis Mosenkovs, Dikay900, Dmitriy Bogdanov, Frank Slezak,
    Gabriele Monaco, Gordon Williams, ivanovlev, João Paulo Barraca, José
    Rebelo, Julien Pivotto, Kasha, keeshii, Martin, Matthieu Baerts, mvn23,
    NekoBox, Nephiel, Petr Vaněk, Sebastian Kranz, Sergey Trofimov, Steffen
    Liebergeld, Taavi Eomäe, TylerWilliamson, Uwe Hermann, Yoran Vulker

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.HeartRateUtils;
import nodomain.freeyourgadget.gadgetbridge.capabilities.loyaltycards.LoyaltyCard;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventCameraRemote;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.externalevents.AlarmClockReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.AlarmReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.BluetoothConnectReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.BluetoothPairingRequestReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.CMWeatherReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.CalendarReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.DeviceSettingsReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.GenericWeatherReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.IntentApiReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.LineageOsWeatherReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.MusicPlaybackReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.OmniJawsObserver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.OsmandEventReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.PebbleReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.PhoneCallReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.SMSReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.SilentModeReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.TimeChangeReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.TinyWeatherForecastGermanyReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.gps.GBLocationService;
import nodomain.freeyourgadget.gadgetbridge.externalevents.sleepasandroid.SleepAsAndroidReceiver;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceService;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.Contact;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NavigationInfoSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;
import nodomain.freeyourgadget.gadgetbridge.model.Reminder;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.model.WorldClock;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLEScanService;
import nodomain.freeyourgadget.gadgetbridge.service.receivers.AutoConnectIntervalReceiver;
import nodomain.freeyourgadget.gadgetbridge.service.receivers.GBAutoFetchReceiver;
import nodomain.freeyourgadget.gadgetbridge.util.EmojiConverter;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import nodomain.freeyourgadget.gadgetbridge.util.language.LanguageUtils;
import nodomain.freeyourgadget.gadgetbridge.util.language.Transliterator;

import static nodomain.freeyourgadget.gadgetbridge.model.DeviceService.*;

public class DeviceCommunicationService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static class DeviceStruct{
        private GBDevice device;
        private DeviceCoordinator coordinator;
        private DeviceSupport deviceSupport;

        public GBDevice getDevice() {
            return device;
        }

        public void setDevice(GBDevice device) {
            this.device = device;
        }

        public DeviceCoordinator getCoordinator() {
            return coordinator;
        }

        public void setCoordinator(DeviceCoordinator coordinator) {
            this.coordinator = coordinator;
        }

        public DeviceSupport getDeviceSupport() {
            return deviceSupport;
        }

        public void setDeviceSupport(DeviceSupport deviceSupport) {
            this.deviceSupport = deviceSupport;
        }
    }

    private static class FeatureSet {
        private boolean supportsWeather = false;
        private boolean supportsActivityDataFetching = false;
        private boolean supportsCalendarEvents = false;
        private boolean supportsMusicInfo = false;
        private boolean supportsNavigation = false;

        private boolean supportsSleepAsAndroid = false;

        public boolean supportsWeather() {
            return supportsWeather;
        }

        public void setSupportsWeather(boolean supportsWeather) {
            this.supportsWeather = supportsWeather;
        }

        public boolean supportsActivityDataFetching() {
            return supportsActivityDataFetching;
        }

        public void setSupportsActivityDataFetching(boolean supportsActivityDataFetching) {
            this.supportsActivityDataFetching = supportsActivityDataFetching;
        }

        public boolean supportsCalendarEvents() {
            return supportsCalendarEvents;
        }

        public void setSupportsCalendarEvents(boolean supportsCalendarEvents) {
            this.supportsCalendarEvents = supportsCalendarEvents;
        }

        public boolean supportsMusicInfo() {
            return supportsMusicInfo;
        }

        public void setSupportsMusicInfo(boolean supportsMusicInfo) {
            this.supportsMusicInfo = supportsMusicInfo;
        }

        public boolean supportsNavigation() {
            return supportsNavigation;
        }

        public void setSupportsNavigation(boolean supportsNavigation) {
            this.supportsNavigation = supportsNavigation;
        }

        public boolean supportsSleepAsAndroid() { return supportsSleepAsAndroid; }

        public void setSupportsSleepAsAndroid(boolean supportsSleepAsAndroid) {
            this.supportsSleepAsAndroid = supportsSleepAsAndroid;
        }

        public void logicalOr(DeviceCoordinator operand){
            if(operand.supportsCalendarEvents()){
                setSupportsCalendarEvents(true);
            }
            if(operand.supportsWeather()){
                setSupportsWeather(true);
            }
            if(operand.supportsActivityDataFetching()){
                setSupportsActivityDataFetching(true);
            }
            if(operand.supportsMusicInfo()){
                setSupportsMusicInfo(true);
            }
            if(operand.supportsNavigation()){
                setSupportsNavigation(true);
            }
            if (operand.supportsSleepAsAndroid()) {
                setSupportsSleepAsAndroid(true);
            }
        }
    }

    public static class DeviceNotFoundException extends GBException{
        private final String address;

        public DeviceNotFoundException(GBDevice device) {
            this.address = device.getAddress();
        }

        public DeviceNotFoundException(String address) {
            this.address = address;
        }

        @Nullable
        @Override
        public String getMessage() {
            return String.format("device %s not found cached", address);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(DeviceCommunicationService.class);
    @SuppressLint("StaticFieldLeak") // only used for test cases
    private static DeviceSupportFactory DEVICE_SUPPORT_FACTORY = null;

    private DeviceSupportFactory mFactory;
    private final ArrayList<DeviceStruct> deviceStructs = new ArrayList<>(1);
    private final HashMap<String, ArrayList<Intent>> cachedNotifications = new HashMap<>();

    private PhoneCallReceiver mPhoneCallReceiver = null;
    private SMSReceiver mSMSReceiver = null;
    private PebbleReceiver mPebbleReceiver = null;
    private MusicPlaybackReceiver mMusicPlaybackReceiver = null;
    private TimeChangeReceiver mTimeChangeReceiver = null;
    private BluetoothConnectReceiver mBlueToothConnectReceiver = null;
    private BluetoothPairingRequestReceiver mBlueToothPairingRequestReceiver = null;
    private AlarmClockReceiver mAlarmClockReceiver = null;
    private SilentModeReceiver mSilentModeReceiver = null;
    private GBAutoFetchReceiver mGBAutoFetchReceiver = null;
    private AutoConnectIntervalReceiver mAutoConnectInvervalReceiver = null;

    private AlarmReceiver mAlarmReceiver = null;
    private final List<CalendarReceiver> mCalendarReceiver = new ArrayList<>();
    private CMWeatherReceiver mCMWeatherReceiver = null;
    private LineageOsWeatherReceiver mLineageOsWeatherReceiver = null;
    private TinyWeatherForecastGermanyReceiver mTinyWeatherForecastGermanyReceiver = null;
    private GenericWeatherReceiver mGenericWeatherReceiver = null;
    private OmniJawsObserver mOmniJawsObserver = null;
    private final DeviceSettingsReceiver deviceSettingsReceiver = new DeviceSettingsReceiver();
    private final IntentApiReceiver intentApiReceiver = new IntentApiReceiver();
    private GBLocationService locationService = null;

    private OsmandEventReceiver mOsmandAidlHelper = null;

    private SleepAsAndroidReceiver mSleepAsAndroidReceiver = null;

    private HashMap<String, Long> deviceLastScannedTimestamps = new HashMap<>();

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

    private final int NOTIFICATIONS_CACHE_MAX = 10;  // maximum amount of notifications to cache per device while disconnected
    private boolean allowBluetoothIntentApi = false;
    private boolean reconnectViaScan = GBPrefs.RECONNECT_SCAN_DEFAULT;

    private final String API_LEGACY_COMMAND_BLUETOOTH_CONNECT = "nodomain.freeyourgadget.gadgetbridge.BLUETOOTH_CONNECT";
    private final String API_LEGACY_COMMAND_BLUETOOTH_DISCONNECT = "nodomain.freeyourgadget.gadgetbridge.BLUETOOTH_DISCONNECT";
    private final String API_LEGACY_ACTION_DEVICE_CONNECTED = "nodomain.freeyourgadget.gadgetbridge.BLUETOOTH_CONNECTED";
    private final String API_LEGACY_ACTION_DEVICE_SCANNED = "nodomain.freeyourgadget.gadgetbridge.BLUETOOTH_SCANNED";

    private void sendDeviceAPIBroadcast(String address, String action){
        if(!allowBluetoothIntentApi){
            LOG.debug("not sending API event due to settings");
            return;
        }
        Intent intent = new Intent(action);
        intent.putExtra("EXTRA_DEVICE_ADDRESS", address);

        sendBroadcast(intent);
    }

    private void sendDeviceConnectedBroadcast(String address){
        sendDeviceAPIBroadcast(address, API_LEGACY_ACTION_DEVICE_CONNECTED);
    }

    BroadcastReceiver bluetoothCommandReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!allowBluetoothIntentApi){
                GB.log("Connection API not allowed in settings", GB.ERROR, null);
                return;
            }
            Bundle extras = intent.getExtras();
            if (extras == null) {
                GB.log("no extras provided in Intent", GB.ERROR, null);
                return;
            }
            final String action = intent.getAction();
            if (action == null) {
                GB.log("Action for bluetooth command is null", GB.ERROR, null);
                return;
            }
            String address = extras.getString("EXTRA_DEVICE_ADDRESS", "");
            if (address.isEmpty()){
                GB.log("no bluetooth address provided in Intent", GB.ERROR, null);
                return;
            }
            GBDevice targetDevice = GBApplication.app()
                    .getDeviceManager()
                    .getDeviceByAddress(address);

            if (targetDevice == null){
                GB.log(String.format("device %s not registered", address), GB.ERROR, null);
                return;
            }

            switch (action) {
                case API_LEGACY_COMMAND_BLUETOOTH_CONNECT:
                    if (isDeviceConnected(address)){
                        GB.log(String.format("device %s already connected", address), GB.INFO, null);
                        sendDeviceConnectedBroadcast(address);

                        return;
                    }

                    GB.log(String.format("connecting to %s", address), GB.INFO, null);

                    GBApplication.deviceService(targetDevice).connect();
                    break;
                case API_LEGACY_COMMAND_BLUETOOTH_DISCONNECT:
                    GB.log(String.format("disconnecting from %s", address), GB.INFO, null);

                    GBApplication.deviceService(targetDevice).disconnect();
                    break;
            }
        }
    };

    /**
     * For testing!
     *
     * @param factory
     */
    @SuppressWarnings("JavaDoc")
    public static void setDeviceSupportFactory(DeviceSupportFactory factory) {
        DEVICE_SUPPORT_FACTORY = factory;
    }

    public static boolean isRunning(final Context context) {
        final ActivityManager manager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        if (manager == null) {
            return false;
        }
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (DeviceCommunicationService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public DeviceCommunicationService() {

    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(GBDevice.ACTION_DEVICE_CHANGED.equals(action)){
                GBDevice device = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);

                // create a new instance of the changed devices coordinator, in case it's capabilities changed
                DeviceStruct cachedStruct = getDeviceStructOrNull(device);
                if(cachedStruct != null) {
                    cachedStruct.setDevice(device);
                    DeviceCoordinator newCoordinator = device.getDeviceCoordinator();
                    cachedStruct.setCoordinator(newCoordinator);
                }
                updateReceiversState();

                GBDevice.DeviceUpdateSubject subject = (GBDevice.DeviceUpdateSubject) intent.getSerializableExtra(GBDevice.EXTRA_UPDATE_SUBJECT);

                if(subject == GBDevice.DeviceUpdateSubject.DEVICE_STATE && device.isInitialized()){
                    sendDeviceConnectedBroadcast(device.getAddress());
                    sendCachedNotifications(device);
                }else if(subject == GBDevice.DeviceUpdateSubject.DEVICE_STATE && (device.getState() == GBDevice.State.SCANNED)){
                    sendDeviceAPIBroadcast(device.getAddress(), API_LEGACY_ACTION_DEVICE_SCANNED);
                }
            }else if(BLEScanService.EVENT_DEVICE_FOUND.equals(action)){
                String deviceAddress = intent.getStringExtra(BLEScanService.EXTRA_DEVICE_ADDRESS);

                GBDevice target = GBApplication
                        .app()
                        .getDeviceManager()
                        .getDeviceByAddress(deviceAddress);

                if(target == null){
                    LOG.error("onReceive: device not found");
                    return;
                }

                if(!target.getDeviceCoordinator().isConnectable()){
                    int actualRSSI = intent.getIntExtra(BLEScanService.EXTRA_RSSI, 0);
                    Prefs prefs = new Prefs(
                            GBApplication.getDeviceSpecificSharedPrefs(target.getAddress())
                    );
                    long timeoutSeconds = prefs.getLong("devicesetting_scannable_debounce", 60);
                    long minimumUnseenSeconds = prefs.getLong("devicesetting_scannable_unseen", 0);
                    int thresholdRSSI = prefs.getInt("devicesetting_scannable_rssi", -100);

                    if(actualRSSI < thresholdRSSI){
                        LOG.debug("ignoring {} since RSSI is too low ({} < {})", deviceAddress, actualRSSI, thresholdRSSI);
                        return;
                    }

                    Long lastSeenTimestamp = deviceLastScannedTimestamps.get(deviceAddress);
                    deviceLastScannedTimestamps.put(deviceAddress, System.currentTimeMillis());

                    if(lastSeenTimestamp != null){
                        long secondsSince = (System.currentTimeMillis() - lastSeenTimestamp) / 1000;
                        if(secondsSince < minimumUnseenSeconds){
                            LOG.debug("ignoring {}, since only {} seconds passed (< {})", deviceAddress, secondsSince, minimumUnseenSeconds);
                            return;
                        }
                    }

                    target.setState(GBDevice.State.SCANNED);
                    target.sendDeviceUpdateIntent(DeviceCommunicationService.this, GBDevice.DeviceUpdateSubject.DEVICE_STATE);
                    new Handler().postDelayed(() -> {
                        if(target.getState() != GBDevice.State.SCANNED){
                            return;
                        }
                        deviceLastScannedTimestamps.put(target.getAddress(), System.currentTimeMillis());
                        target.setState(GBDevice.State.WAITING_FOR_SCAN);
                        target.sendDeviceUpdateIntent(DeviceCommunicationService.this, GBDevice.DeviceUpdateSubject.DEVICE_STATE);
                    }, timeoutSeconds * 1000);
                    return;
                }

                connectToDevice(target);
            }
        }
    };

    private void updateReceiversState(){
        boolean enableReceivers = false;
        boolean anyDeviceInitialized = false;
        List <GBDevice> devicesWithCalendar = new ArrayList<>();

        FeatureSet features = new FeatureSet();

        for(DeviceStruct struct: deviceStructs){
            DeviceSupport deviceSupport = struct.getDeviceSupport();
            if((deviceSupport != null && deviceSupport.useAutoConnect()) || isDeviceInitialized(struct.getDevice())){
                enableReceivers = true;
            }
            if(isDeviceInitialized(struct.getDevice())){
                anyDeviceInitialized = true;
            }

            DeviceCoordinator coordinator = struct.getCoordinator();
            if(coordinator != null){
                features.logicalOr(coordinator);
                if (coordinator.supportsCalendarEvents()){
                    devicesWithCalendar.add(struct.getDevice());
                }
            }
        }
        setReceiversEnableState(enableReceivers, anyDeviceInitialized, features, devicesWithCalendar);
    }

    private void registerInternalReceivers(){
        IntentFilter localFilter = new IntentFilter();
        localFilter.addAction(GBDevice.ACTION_DEVICE_CHANGED);
        localFilter.addAction(BLEScanService.EVENT_DEVICE_FOUND);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, localFilter);
    }

    private void registerExternalReceivers(){
        mBlueToothConnectReceiver = new BluetoothConnectReceiver(this);
        ContextCompat.registerReceiver(this, mBlueToothConnectReceiver, new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED), ContextCompat.RECEIVER_EXPORTED);

        mAutoConnectInvervalReceiver= new AutoConnectIntervalReceiver(this);
        ContextCompat.registerReceiver(this, mAutoConnectInvervalReceiver, new IntentFilter("GB_RECONNECT"), ContextCompat.RECEIVER_EXPORTED);

        IntentFilter bluetoothCommandFilter = new IntentFilter();
        bluetoothCommandFilter.addAction(API_LEGACY_COMMAND_BLUETOOTH_CONNECT);
        bluetoothCommandFilter.addAction(API_LEGACY_COMMAND_BLUETOOTH_DISCONNECT);
        ContextCompat.registerReceiver(this, bluetoothCommandReceiver, bluetoothCommandFilter, ContextCompat.RECEIVER_EXPORTED);

        final IntentFilter deviceSettingsIntentFilter = new IntentFilter();
        deviceSettingsIntentFilter.addAction(DeviceSettingsReceiver.COMMAND);
        ContextCompat.registerReceiver(this, deviceSettingsReceiver, deviceSettingsIntentFilter, ContextCompat.RECEIVER_EXPORTED);

        ContextCompat.registerReceiver(this, intentApiReceiver, intentApiReceiver.buildFilter(), ContextCompat.RECEIVER_EXPORTED);
    }

    @Override
    public void onCreate() {
        LOG.debug("DeviceCommunicationService is being created");
        super.onCreate();
        mFactory = getDeviceSupportFactory();

        registerInternalReceivers();
        registerExternalReceivers();

        if (hasPrefs()) {
            getPrefs().getPreferences().registerOnSharedPreferenceChangeListener(this);
            allowBluetoothIntentApi = getPrefs().getBoolean(GBPrefs.PREF_ALLOW_INTENT_API, false);
            reconnectViaScan = getPrefs().getAutoReconnectByScan();
        }

        startForeground();
        if(reconnectViaScan) {
            scanAllDevices();

            Intent scanServiceIntent = new Intent(this, BLEScanService.class);
            startService(scanServiceIntent);
        }
    }

    private void scanAllDevices(){
        List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
        for(GBDevice device : devices){
            if(!device.getDeviceCoordinator().getConnectionType().usesBluetoothLE()){
                continue;
            }
            if(device.getState() != GBDevice.State.NOT_CONNECTED){
                continue;
            }
            boolean shouldAutoConnect = getPrefs().getAutoReconnect(device);
            if(!shouldAutoConnect){
                continue;
            }
            createDeviceStruct(device);
            device.setState(GBDevice.State.WAITING_FOR_SCAN);
            device.sendDeviceUpdateIntent(this, GBDevice.DeviceUpdateSubject.DEVICE_STATE);
        }
    }

    private DeviceSupportFactory getDeviceSupportFactory() {
        if (DEVICE_SUPPORT_FACTORY != null) {
            return DEVICE_SUPPORT_FACTORY;
        }
        return new DeviceSupportFactory(this);
    }

    private DeviceStruct createDeviceStruct(GBDevice target){
        DeviceStruct registeredStruct = new DeviceStruct();
        registeredStruct.setDevice(target);
        registeredStruct.setCoordinator(target.getDeviceCoordinator());
        deviceStructs.add(registeredStruct);
        return registeredStruct;
    }

    private void connectToDevice(GBDevice device){
        connectToDevice(device, false);
    }

    private void connectToDevice(@Nullable final GBDevice device, boolean firstTime) {
        final List<GBDevice> gbDevs = new ArrayList<>();
        boolean fromExtra = false;

        final Prefs prefs = getPrefs();

        if (device != null) {
            if (!device.getDeviceCoordinator().isConnectable()) {
                GB.toast("Cannot connect to Scannable Device", Toast.LENGTH_SHORT, GB.INFO);
                return;
            }

            gbDevs.add(device);
            fromExtra = true;
        } else {
            List<GBDevice> gbAllDevs = GBApplication.app().getDeviceManager().getDevices();

            if (gbAllDevs != null && !gbAllDevs.isEmpty()) {
                if (prefs.getBoolean(GBPrefs.RECONNECT_ONLY_TO_CONNECTED, true)) {
                    Set<String> lastDeviceAddresses = prefs.getStringSet(GBPrefs.LAST_DEVICE_ADDRESSES, Collections.emptySet());

                    if (lastDeviceAddresses != null && !lastDeviceAddresses.isEmpty()) {
                        for (final GBDevice gbDev : gbAllDevs) {
                            // TODO volatile address
                            if (lastDeviceAddresses.contains(gbDev.getAddress())) {
                                gbDevs.add(gbDev);
                            }
                        }
                    }
                } else {
                    gbDevs.addAll(gbAllDevs);
                }
            }
        }

        if (gbDevs.isEmpty()) {
            return;
        }

        for (GBDevice gbDevice : gbDevs) {
            if (!gbDevice.getDeviceCoordinator().isConnectable()) {
                // we cannot connect to beacons, skip this device
                continue;
            }

            String btDeviceAddress = gbDevice.getAddress();

            boolean autoReconnect = GBPrefs.AUTO_RECONNECT_DEFAULT;
            if (prefs != null && prefs.getPreferences() != null) {
                autoReconnect = getPrefs().getAutoReconnect(gbDevice);
                if (!fromExtra && !autoReconnect) {
                    continue;
                }

                final Set<String> lastDeviceAddresses = new HashSet<>(prefs.getStringSet(GBPrefs.LAST_DEVICE_ADDRESSES, Collections.emptySet()));

                if (!lastDeviceAddresses.contains(btDeviceAddress)) {
                    lastDeviceAddresses.add(btDeviceAddress);
                    prefs.getPreferences().edit().putStringSet(GBPrefs.LAST_DEVICE_ADDRESSES, lastDeviceAddresses).apply();
                }
            }

            if (!fromExtra && !autoReconnect) {
                continue;
            }

            DeviceStruct registeredStruct = getDeviceStructOrNull(gbDevice);
            if (registeredStruct == null) {
                registeredStruct = createDeviceStruct(gbDevice);
            } else {
                final GBDevice deviceFromStruct = registeredStruct.getDevice();

                if (isDeviceConnecting(deviceFromStruct) || isDeviceConnected(deviceFromStruct)) {
                    continue;
                }

                try {
                    removeDeviceSupport(gbDevice);
                } catch (final DeviceNotFoundException e) {
                    LOG.error("connectToDevice(): Failed to remove device support: {}", e, e);
                }
            }

            try {
                final DeviceSupport deviceSupport = mFactory.createDeviceSupport(gbDevice);

                if (deviceSupport != null) {
                    setDeviceSupport(gbDevice, deviceSupport);

                    if (firstTime) {
                        deviceSupport.connectFirstTime();
                    } else {
                        deviceSupport.setAutoReconnect(autoReconnect);
                        deviceSupport.setScanReconnect(reconnectViaScan);
                        deviceSupport.connect();
                    }
                } else {
                    GB.toast(this, getString(R.string.cannot_connect, "Can't create device support"), Toast.LENGTH_SHORT, GB.ERROR);
                }
            } catch (Exception e) {
                GB.toast(this, getString(R.string.cannot_connect, e.getMessage()), Toast.LENGTH_SHORT, GB.ERROR, e);
            }

            registeredStruct.getDevice().sendDeviceUpdateIntent(this);
        }
    }

    @Override
    public synchronized int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            LOG.info("no intent");
            return START_STICKY;
        }

        String action = intent.getAction();

        if (action == null) {
            LOG.info("no action");
            return START_STICKY;
        }

        LOG.debug("Service startcommand: " + action);

        // when we get past this, we should have valid mDeviceSupport and mGBDevice instances

        GBDevice targetDevice = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);

        Prefs prefs = getPrefs();
        switch (action) {
            case ACTION_CONNECT:
                boolean firstTime = intent.getBooleanExtra(EXTRA_CONNECT_FIRST_TIME, false);
                connectToDevice(targetDevice, firstTime);
                break;
            default:
                ArrayList<GBDevice> targetedDevices = new ArrayList<>();
                if(targetDevice != null){
                    targetedDevices.add(targetDevice);
                }else{
                    for(GBDevice device : getGBDevices()){
                        if(isDeviceInitialized(device)){
                            targetedDevices.add(device);
                        } else if (isDeviceReconnecting(device) && action.equals(ACTION_NOTIFICATION) && GBApplication.getPrefs().getBoolean("notification_cache_while_disconnected", false)) {
                            if (!cachedNotifications.containsKey(device.getAddress())) {
                                cachedNotifications.put(device.getAddress(), new ArrayList<>());
                            }
                            ArrayList<Intent> notifCache = cachedNotifications.get(device.getAddress());
                            notifCache.add(intent);
                            if (notifCache.size() > NOTIFICATIONS_CACHE_MAX) {
                                // remove the oldest notification if the maximum is reached
                                notifCache.remove(0);
                            }
                        } else if (action.equals(ACTION_DELETE_NOTIFICATION)) {
                            ArrayList<Intent> notifCache = cachedNotifications.get(device.getAddress());
                            if (notifCache != null) {
                                int notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1);
                                ArrayList<Intent> toRemove = new ArrayList<>();
                                for (Intent cached : notifCache) {
                                    if (notifId == cached.getIntExtra(EXTRA_NOTIFICATION_ID, -1)) {
                                        toRemove.add(cached);
                                    }
                                }
                                notifCache.removeAll(toRemove);
                            }
                        } else if (action.equals(ACTION_DISCONNECT) && device.getState() != GBDevice.State.NOT_CONNECTED) {
                            targetedDevices.add(device);
                        }
                    }
                }
                for (GBDevice device1 : targetedDevices) {
                    try {
                        handleAction(intent, action, device1);
                    } catch (DeviceNotFoundException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        LOG.error("An exception was raised while handling the action {} for the device {}: ", action, device1, e);
                    }
                }
                break;
        }
        return START_STICKY;
    }

    /**
     * @param text original text
     * @return 'text' or a new String without non supported chars like emoticons, etc.
     */
    private String sanitizeNotifText(String text, GBDevice device) throws DeviceNotFoundException {
        if (text == null || text.length() == 0)
            return text;

        text = getDeviceSupport(device).customStringFilter(text);

        if (!getDeviceCoordinator(device).supportsUnicodeEmojis()) {
            return EmojiConverter.convertUnicodeEmojiToAscii(text, getApplicationContext());
        }

        return text;
    }

    private DeviceCoordinator getDeviceCoordinator(GBDevice device) throws DeviceNotFoundException {
        if(device == null){
            throw new DeviceNotFoundException("null");
        }
        for(DeviceStruct struct : deviceStructs){
            if(struct.getDevice().equals(device)){
                return struct.getCoordinator();
            }
        }
        throw new DeviceNotFoundException(device);
    }

    private void handleAction(Intent intent, String action, GBDevice device) throws DeviceNotFoundException {
        if(ACTION_DISCONNECT.equals(intent.getAction())) {
            try {
                removeDeviceSupport(device);
            } catch (DeviceNotFoundException e) {
                LOG.error("Trying to disconnect unknown device: ", e);
            }
            device.setState(GBDevice.State.NOT_CONNECTED);
            device.sendDeviceUpdateIntent(this);
            updateReceiversState();
            return;
        }

        DeviceSupport deviceSupport = getDeviceSupport(device);

        Prefs devicePrefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(device.getAddress()));

        final Transliterator transliterator = LanguageUtils.getTransliterator(device);

        // Copy the incoming intent to make sure we don't modify it before it gets passed to other devices
        Intent intentCopy = (Intent) intent.clone();

        for (String extra : GBDeviceService.transliterationExtras) {
            if (intentCopy.hasExtra(extra)) {
                // Ensure the text is sanitized (eg. emoji converted to ascii) before applying the transliterators
                // otherwise the emoji are removed before converting them
                String sanitizedText = sanitizeNotifText(intentCopy.getStringExtra(extra), device);
                if (transliterator != null) {
                    sanitizedText = transliterator.transliterate(sanitizedText);
                }
                intentCopy.putExtra(extra, sanitizedText);
            }
        }

        switch (action) {
            case ACTION_REQUEST_DEVICEINFO:
                device.sendDeviceUpdateIntent(this, GBDevice.DeviceUpdateSubject.NOTHING);
                break;
            case ACTION_NOTIFICATION: {
                int desiredId = intentCopy.getIntExtra(EXTRA_NOTIFICATION_ID, -1);
                NotificationSpec notificationSpec = new NotificationSpec(desiredId);
                notificationSpec.phoneNumber = intentCopy.getStringExtra(EXTRA_NOTIFICATION_PHONENUMBER);
                notificationSpec.sender = intentCopy.getStringExtra(EXTRA_NOTIFICATION_SENDER);
                notificationSpec.subject = intentCopy.getStringExtra(EXTRA_NOTIFICATION_SUBJECT);
                notificationSpec.title = intentCopy.getStringExtra(EXTRA_NOTIFICATION_TITLE);
                if(notificationSpec.title == null) {
                    notificationSpec.title = "";
                }
                notificationSpec.key = intentCopy.getStringExtra(EXTRA_NOTIFICATION_KEY);
                notificationSpec.body = intentCopy.getStringExtra(EXTRA_NOTIFICATION_BODY);
                if(notificationSpec.body == null) {
                    notificationSpec.body = "";
                }
                notificationSpec.sourceName = intentCopy.getStringExtra(EXTRA_NOTIFICATION_SOURCENAME);
                notificationSpec.type = (NotificationType) intentCopy.getSerializableExtra(EXTRA_NOTIFICATION_TYPE);
                notificationSpec.attachedActions = (ArrayList<NotificationSpec.Action>) intentCopy.getSerializableExtra(EXTRA_NOTIFICATION_ACTIONS);
                notificationSpec.pebbleColor = (byte) intentCopy.getSerializableExtra(EXTRA_NOTIFICATION_PEBBLE_COLOR);
                notificationSpec.flags = intentCopy.getIntExtra(EXTRA_NOTIFICATION_FLAGS, 0);
                notificationSpec.sourceAppId = intentCopy.getStringExtra(EXTRA_NOTIFICATION_SOURCEAPPID);
                notificationSpec.iconId = intentCopy.getIntExtra(EXTRA_NOTIFICATION_ICONID, 0);
                notificationSpec.picturePath = intent.getStringExtra(NOTIFICATION_PICTURE_PATH);
                notificationSpec.dndSuppressed = intentCopy.getIntExtra(EXTRA_NOTIFICATION_DNDSUPPRESSED, 0);

                if (notificationSpec.type == NotificationType.GENERIC_SMS && notificationSpec.phoneNumber != null) {
                    GBApplication.getIDSenderLookup().add(notificationSpec.getId(), notificationSpec.phoneNumber);
                }

                //TODO: check if at least one of the attached actions is a reply action instead?
                if ((notificationSpec.attachedActions != null && notificationSpec.attachedActions.size() > 0)
                        || (notificationSpec.type == NotificationType.GENERIC_SMS && notificationSpec.phoneNumber != null)) {
                    // NOTE: maybe not where it belongs
                    // I would rather like to save that as an array in SharedPreferences
                    // this would work but I dont know how to do the same in the Settings Activity's xml
                    ArrayList<String> replies = new ArrayList<>();
                    for (int i = 1; i <= 16; i++) {
                        String reply = devicePrefs.getString("canned_reply_" + i, null);
                        if (reply != null && !reply.equals("")) {
                            replies.add(reply);
                        }
                    }
                    notificationSpec.cannedReplies = replies.toArray(new String[0]);
                }

                deviceSupport.onNotification(notificationSpec);
                break;
            }
            case ACTION_DELETE_NOTIFICATION: {
                deviceSupport.onDeleteNotification(intentCopy.getIntExtra(EXTRA_NOTIFICATION_ID, -1));
                break;
            }
            case ACTION_ADD_CALENDAREVENT: {
                CalendarEventSpec calendarEventSpec = new CalendarEventSpec();
                calendarEventSpec.id = intentCopy.getLongExtra(EXTRA_CALENDAREVENT_ID, -1);
                calendarEventSpec.type = intentCopy.getByteExtra(EXTRA_CALENDAREVENT_TYPE, (byte) -1);
                calendarEventSpec.timestamp = intentCopy.getIntExtra(EXTRA_CALENDAREVENT_TIMESTAMP, -1);
                calendarEventSpec.durationInSeconds = intentCopy.getIntExtra(EXTRA_CALENDAREVENT_DURATION, -1);
                calendarEventSpec.allDay = intentCopy.getBooleanExtra(EXTRA_CALENDAREVENT_ALLDAY, false);
                calendarEventSpec.reminders =  (ArrayList<Long>) intentCopy.getSerializableExtra(EXTRA_CALENDAREVENT_REMINDERS);
                calendarEventSpec.title = intentCopy.getStringExtra(EXTRA_CALENDAREVENT_TITLE);
                calendarEventSpec.description = intentCopy.getStringExtra(EXTRA_CALENDAREVENT_DESCRIPTION);
                calendarEventSpec.location = intentCopy.getStringExtra(EXTRA_CALENDAREVENT_LOCATION);
                calendarEventSpec.calName = intentCopy.getStringExtra(EXTRA_CALENDAREVENT_CALNAME);
                calendarEventSpec.color = intentCopy.getIntExtra(EXTRA_CALENDAREVENT_COLOR, 0);
                deviceSupport.onAddCalendarEvent(calendarEventSpec);
                break;
            }
            case ACTION_DELETE_CALENDAREVENT: {
                long id = intentCopy.getLongExtra(EXTRA_CALENDAREVENT_ID, -1);
                byte type = intentCopy.getByteExtra(EXTRA_CALENDAREVENT_TYPE, (byte) -1);
                deviceSupport.onDeleteCalendarEvent(type, id);
                break;
            }
            case ACTION_RESET: {
                int flags = intentCopy.getIntExtra(EXTRA_RESET_FLAGS, 0);
                deviceSupport.onReset(flags);
                break;
            }
            case ACTION_HEARTRATE_TEST: {
                deviceSupport.onHeartRateTest();
                break;
            }
            case ACTION_FETCH_RECORDED_DATA: {
                int dataTypes = intentCopy.getIntExtra(EXTRA_RECORDED_DATA_TYPES, 0);
                deviceSupport.onFetchRecordedData(dataTypes);
                break;
            }
            case ACTION_FIND_DEVICE: {
                boolean start = intentCopy.getBooleanExtra(EXTRA_FIND_START, false);
                deviceSupport.onFindDevice(start);
                break;
            }
            case ACTION_PHONE_FOUND: {
                final boolean start = intentCopy.getBooleanExtra(EXTRA_FIND_START, false);
                deviceSupport.onFindPhone(start);
                break;
            }
            case ACTION_SET_CONSTANT_VIBRATION: {
                int intensity = intentCopy.getIntExtra(EXTRA_VIBRATION_INTENSITY, 0);
                deviceSupport.onSetConstantVibration(intensity);
                break;
            }
            case ACTION_CALLSTATE:
                CallSpec callSpec = new CallSpec();
                callSpec.command = intentCopy.getIntExtra(EXTRA_CALL_COMMAND, CallSpec.CALL_UNDEFINED);
                callSpec.number = intentCopy.getStringExtra(EXTRA_CALL_PHONENUMBER);
                callSpec.name = intentCopy.getStringExtra(EXTRA_CALL_DISPLAYNAME);
                callSpec.sourceName = intentCopy.getStringExtra(EXTRA_CALL_SOURCENAME);
                callSpec.sourceAppId = intentCopy.getStringExtra(EXTRA_CALL_SOURCEAPPID);
                callSpec.dndSuppressed = intentCopy.getIntExtra(EXTRA_CALL_DNDSUPPRESSED, 0);
                deviceSupport.onSetCallState(callSpec);
                break;
            case ACTION_SETCANNEDMESSAGES:
                int type = intentCopy.getIntExtra(EXTRA_CANNEDMESSAGES_TYPE, -1);
                String[] cannedMessages = intentCopy.getStringArrayExtra(EXTRA_CANNEDMESSAGES);

                CannedMessagesSpec cannedMessagesSpec = new CannedMessagesSpec();
                cannedMessagesSpec.type = type;
                cannedMessagesSpec.cannedMessages = cannedMessages;
                deviceSupport.onSetCannedMessages(cannedMessagesSpec);
                break;
            case ACTION_SETTIME:
                deviceSupport.onSetTime();
                break;
            case ACTION_SETMUSICINFO:
                MusicSpec musicSpec = new MusicSpec();
                musicSpec.artist = intentCopy.getStringExtra(EXTRA_MUSIC_ARTIST);
                musicSpec.album = intentCopy.getStringExtra(EXTRA_MUSIC_ALBUM);
                musicSpec.track = intentCopy.getStringExtra(EXTRA_MUSIC_TRACK);
                musicSpec.duration = intentCopy.getIntExtra(EXTRA_MUSIC_DURATION, 0);
                musicSpec.trackCount = intentCopy.getIntExtra(EXTRA_MUSIC_TRACKCOUNT, 0);
                musicSpec.trackNr = intentCopy.getIntExtra(EXTRA_MUSIC_TRACKNR, 0);
                deviceSupport.onSetMusicInfo(musicSpec);
                break;
            case ACTION_SET_PHONE_VOLUME:
                float phoneVolume = intentCopy.getFloatExtra(EXTRA_PHONE_VOLUME, 0);
                deviceSupport.onSetPhoneVolume(phoneVolume);
                break;
            case ACTION_SET_PHONE_SILENT_MODE:
                final int ringerMode = intentCopy.getIntExtra(EXTRA_PHONE_RINGER_MODE, -1);
                deviceSupport.onChangePhoneSilentMode(ringerMode);
                break;
            case ACTION_SETMUSICSTATE:
                MusicStateSpec stateSpec = new MusicStateSpec();
                stateSpec.shuffle = intentCopy.getByteExtra(EXTRA_MUSIC_SHUFFLE, (byte) 0);
                stateSpec.repeat = intentCopy.getByteExtra(EXTRA_MUSIC_REPEAT, (byte) 0);
                stateSpec.position = intentCopy.getIntExtra(EXTRA_MUSIC_POSITION, 0);
                stateSpec.playRate = intentCopy.getIntExtra(EXTRA_MUSIC_RATE, 0);
                stateSpec.state = intentCopy.getByteExtra(EXTRA_MUSIC_STATE, (byte) 0);
                deviceSupport.onSetMusicState(stateSpec);
                break;
            case ACTION_SETNAVIGATIONINFO:
                NavigationInfoSpec navigationInfoSpec = new NavigationInfoSpec();
                navigationInfoSpec.instruction = intentCopy.getStringExtra(EXTRA_NAVIGATION_INSTRUCTION);
                navigationInfoSpec.nextAction = intentCopy.getIntExtra(EXTRA_NAVIGATION_NEXT_ACTION,0);
                navigationInfoSpec.distanceToTurn = intentCopy.getStringExtra(EXTRA_NAVIGATION_DISTANCE_TO_TURN);
                navigationInfoSpec.ETA = intentCopy.getStringExtra(EXTRA_NAVIGATION_ETA);
                deviceSupport.onSetNavigationInfo(navigationInfoSpec);
                break;
            case ACTION_REQUEST_APPINFO:
                deviceSupport.onAppInfoReq();
                break;
            case ACTION_REQUEST_SCREENSHOT:
                deviceSupport.onScreenshotReq();
                break;
            case ACTION_STARTAPP: {
                UUID uuid = (UUID) intentCopy.getSerializableExtra(EXTRA_APP_UUID);
                boolean start = intentCopy.getBooleanExtra(EXTRA_APP_START, true);
                deviceSupport.onAppStart(uuid, start);
                break;
            }
            case ACTION_DOWNLOADAPP: {
                UUID uuid = (UUID) intentCopy.getSerializableExtra(EXTRA_APP_UUID);
                deviceSupport.onAppDownload(uuid);
                break;
            }
            case ACTION_DELETEAPP: {
                UUID uuid = (UUID) intentCopy.getSerializableExtra(EXTRA_APP_UUID);
                deviceSupport.onAppDelete(uuid);
                break;
            }
            case ACTION_APP_CONFIGURE: {
                UUID uuid = (UUID) intentCopy.getSerializableExtra(EXTRA_APP_UUID);
                String config = intentCopy.getStringExtra(EXTRA_APP_CONFIG);
                Integer id = null;
                if (intentCopy.hasExtra(EXTRA_APP_CONFIG_ID)) {
                    id = intentCopy.getIntExtra(EXTRA_APP_CONFIG_ID, 0);
                }
                deviceSupport.onAppConfiguration(uuid, config, id);
                break;
            }
            case ACTION_APP_REORDER: {
                UUID[] uuids = (UUID[]) intentCopy.getSerializableExtra(EXTRA_APP_UUID);
                deviceSupport.onAppReorder(uuids);
                break;
            }
            case ACTION_INSTALL:
                Uri uri = intentCopy.getParcelableExtra(EXTRA_URI);
                if (uri != null) {
                    LOG.info("will try to install app/fw");
                    deviceSupport.onInstallApp(uri);
                }
                break;
            case ACTION_SET_ALARMS:
                ArrayList<? extends Alarm> alarms = (ArrayList<? extends Alarm>) intentCopy.getSerializableExtra(EXTRA_ALARMS);
                deviceSupport.onSetAlarms(alarms);
                break;
            case ACTION_SET_REMINDERS:
                ArrayList<? extends Reminder> reminders = (ArrayList<? extends Reminder>) intentCopy.getSerializableExtra(EXTRA_REMINDERS);
                deviceSupport.onSetReminders(reminders);
                break;
            case ACTION_SET_LOYALTY_CARDS:
                final ArrayList<LoyaltyCard> loyaltyCards = (ArrayList<LoyaltyCard>) intentCopy.getSerializableExtra(EXTRA_LOYALTY_CARDS);
                deviceSupport.onSetLoyaltyCards(loyaltyCards);
                break;
            case ACTION_SET_WORLD_CLOCKS:
                ArrayList<? extends WorldClock> clocks = (ArrayList<? extends WorldClock>) intentCopy.getSerializableExtra(EXTRA_WORLD_CLOCKS);
                deviceSupport.onSetWorldClocks(clocks);
                break;
            case ACTION_SET_CONTACTS:
                ArrayList<? extends Contact> contacts = (ArrayList<? extends Contact>) intentCopy.getSerializableExtra(EXTRA_CONTACTS);
                deviceSupport.onSetContacts(contacts);
                break;
            case ACTION_ENABLE_REALTIME_STEPS: {
                boolean enable = intentCopy.getBooleanExtra(EXTRA_BOOLEAN_ENABLE, false);
                deviceSupport.onEnableRealtimeSteps(enable);
                break;
            }
            case ACTION_ENABLE_HEARTRATE_SLEEP_SUPPORT: {
                boolean enable = intentCopy.getBooleanExtra(EXTRA_BOOLEAN_ENABLE, false);
                deviceSupport.onEnableHeartRateSleepSupport(enable);
                break;
            }
            case ACTION_SET_HEARTRATE_MEASUREMENT_INTERVAL: {
                int seconds = intentCopy.getIntExtra(EXTRA_INTERVAL_SECONDS, 0);
                deviceSupport.onSetHeartRateMeasurementInterval(seconds);
                break;
            }
            case ACTION_ENABLE_REALTIME_HEARTRATE_MEASUREMENT: {
                boolean enable = intentCopy.getBooleanExtra(EXTRA_BOOLEAN_ENABLE, false);
                deviceSupport.onEnableRealtimeHeartRateMeasurement(enable);
                break;
            }
            case ACTION_SEND_CONFIGURATION: {
                String config = intentCopy.getStringExtra(EXTRA_CONFIG);
                deviceSupport.onSendConfiguration(config);
                break;
            }
            case ACTION_READ_CONFIGURATION: {
                String config = intentCopy.getStringExtra(EXTRA_CONFIG);
                deviceSupport.onReadConfiguration(config);
                break;
            }
            case ACTION_TEST_NEW_FUNCTION: {
                deviceSupport.onTestNewFunction();
                break;
            }
            case ACTION_SEND_WEATHER: {
                ArrayList<WeatherSpec> weatherSpecs = (ArrayList<WeatherSpec>) intentCopy.getSerializableExtra(EXTRA_WEATHER);
                if (weatherSpecs != null && !weatherSpecs.isEmpty()) {
                    deviceSupport.onSendWeather(weatherSpecs);
                }
                break;
            }
            case ACTION_SET_LED_COLOR:
                int color = intentCopy.getIntExtra(EXTRA_LED_COLOR, 0);
                if (color != 0) {
                    deviceSupport.onSetLedColor(color);
                }
                break;
            case ACTION_POWER_OFF:
                deviceSupport.onPowerOff();
                break;
            case ACTION_SET_FM_FREQUENCY:
                float frequency = intentCopy.getFloatExtra(EXTRA_FM_FREQUENCY, -1);
                if (frequency != -1) {
                    deviceSupport.onSetFmFrequency(frequency);
                }
                break;
            case ACTION_SET_GPS_LOCATION:
                final Location location = intentCopy.getParcelableExtra(EXTRA_GPS_LOCATION);
                deviceSupport.onSetGpsLocation(location);
                break;
            case ACTION_SLEEP_AS_ANDROID:
                if(device.getDeviceCoordinator().supportsSleepAsAndroid() && GBApplication.getPrefs().getString("sleepasandroid_device", new String()).equals(device.getAddress()))
                {
                    final String sleepAsAndroidAction = intentCopy.getStringExtra(EXTRA_SLEEP_AS_ANDROID_ACTION);
                    deviceSupport.onSleepAsAndroidAction(sleepAsAndroidAction, intentCopy.getExtras());
                }
                break;
            case ACTION_CAMERA_STATUS_CHANGE:
                final GBDeviceEventCameraRemote.Event event = GBDeviceEventCameraRemote.intToEvent(intentCopy.getIntExtra(EXTRA_CAMERA_EVENT, -1));
                String filename = null;
                if (event == GBDeviceEventCameraRemote.Event.TAKE_PICTURE) {
                    filename = intentCopy.getStringExtra(EXTRA_CAMERA_FILENAME);
                }
                deviceSupport.onCameraStatusChange(event, filename);
                break;
            case ACTION_REQUEST_MUSIC_LIST:
                deviceSupport.onMusicListReq();
                break;
            case ACTION_REQUEST_MUSIC_OPERATION:
                int operation = intentCopy.getIntExtra("operation", -1);
                int playlistIndex = intentCopy.getIntExtra("playlistIndex", -1);
                String playlistName = intentCopy.getStringExtra("playlistName");
                ArrayList<Integer> musics = (ArrayList<Integer>) intentCopy.getSerializableExtra("musicIds");
                deviceSupport.onMusicOperation(operation, playlistIndex, playlistName, musics);
                break;
        }
    }

    /**
     * Disposes the current DeviceSupport instance (if any) and sets a new device support instance
     * (if not null).
     *
     * @param deviceSupport deviceSupport to reokace/add
     */
    private void setDeviceSupport(GBDevice device, DeviceSupport deviceSupport) throws DeviceNotFoundException {
       DeviceStruct deviceStruct = getDeviceStruct(device);
       DeviceSupport cachedDeviceSupport = deviceStruct.getDeviceSupport();
       if (deviceSupport != cachedDeviceSupport && cachedDeviceSupport != null) {
           cachedDeviceSupport.dispose();
       }
       deviceStruct.setDeviceSupport(deviceSupport);
    }

    private void removeDeviceSupport(GBDevice device) throws DeviceNotFoundException {
        DeviceStruct struct = getDeviceStruct(device);
        if(struct.getDeviceSupport() != null){
            struct.getDeviceSupport().dispose();
        }
        struct.setDeviceSupport(null);
    }

    private DeviceStruct getDeviceStructOrNull(GBDevice device){
        DeviceStruct deviceStruct = null;
        try {
            deviceStruct = getDeviceStruct(device);
        } catch (DeviceNotFoundException e) {
            e.printStackTrace();
        }
        return deviceStruct;
    }

    public DeviceStruct getDeviceStruct(GBDevice device) throws DeviceNotFoundException {
        if(device == null){
            throw new DeviceNotFoundException("null");
        }
        for(DeviceStruct struct : deviceStructs){
            if(struct.getDevice().equals(device)){
                return struct;
            }
        }
        throw new DeviceNotFoundException(device);
    }

    public GBDevice getDeviceByAddress(String deviceAddress) throws DeviceNotFoundException {
        if(deviceAddress == null){
            throw new DeviceNotFoundException(deviceAddress);
        }
        for(DeviceStruct struct : deviceStructs){
            if(struct.getDevice().getAddress().equals(deviceAddress)){
                return struct.getDevice();
            }
        }
        throw new DeviceNotFoundException(deviceAddress);
    }

    public GBDevice getDeviceByAddressOrNull(String deviceAddress){
        GBDevice device = null;
        try {
            device = getDeviceByAddress(deviceAddress);
        } catch (DeviceNotFoundException e) {
            e.printStackTrace();
        }
        return device;
    }

    private DeviceSupport getDeviceSupport(GBDevice device) throws DeviceNotFoundException {
        if(device == null){
            throw new DeviceNotFoundException("null");
        }
        for(DeviceStruct struct : deviceStructs){
            if(struct.getDevice().equals(device)){
                if(struct.getDeviceSupport() == null)
                    throw new DeviceNotFoundException(device);

                return struct.getDeviceSupport();
            }
        }
        throw new DeviceNotFoundException(device);
    }

    private void startForeground() {
        GB.createNotificationChannels(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_DENIED)
                return;

            startForeground(GB.NOTIFICATION_ID, GB.createNotification(getString(R.string.gadgetbridge_running), this), ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE);
        } else {
            startForeground(GB.NOTIFICATION_ID, GB.createNotification(getString(R.string.gadgetbridge_running), this));
        }
    }

    private boolean isDeviceConnected(GBDevice device) {
        return isDeviceConnected(device.getAddress());
    }

    private boolean isDeviceConnected(String deviceAddress) {
        for(DeviceStruct struct : deviceStructs){
            if(struct.getDevice().getAddress().compareToIgnoreCase(deviceAddress) == 0){
                return struct.getDevice().isConnected();
            }
        }
        return false;
    }

    private boolean isDeviceConnecting(GBDevice device) {
        return isDeviceConnecting(device.getAddress());
    }

    private boolean isDeviceConnecting(String deviceAddress) {
        for(DeviceStruct struct : deviceStructs){
            if(struct.getDevice().getAddress().compareToIgnoreCase(deviceAddress) == 0){
                return struct.getDevice().isConnecting();
            }
        }
        return false;
    }

    private boolean isDeviceInitialized(GBDevice device) {
        return isDeviceInitialized(device.getAddress());
    }

    private boolean isDeviceInitialized(String deviceAddress) {
        for(DeviceStruct struct : deviceStructs){
            if(struct.getDevice().getAddress().compareToIgnoreCase(deviceAddress) == 0){
                return struct.getDevice().isInitialized();
            }
        }
        return false;
    }

    private boolean deviceStateEquals(GBDevice device, GBDevice.State... states){
        if((device = getDeviceByAddressOrNull(device.getAddress())) != null){
            for(GBDevice.State possibleState : states){
                if(device.getState() == possibleState){
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isDeviceReconnecting(GBDevice device) {
        if((device = getDeviceByAddressOrNull(device.getAddress())) != null){
            return device.getState().equalsOrHigherThan(GBDevice.State.NOT_CONNECTED);
        }
        return false;
    }

    private boolean deviceHasCalendarReceiverRegistered(GBDevice device){
        for (CalendarReceiver receiver: mCalendarReceiver){
            if(receiver.getGBDevice().equals(device)){
                return true;
            }
        }
        return false;
    }

    private void setReceiversEnableState(boolean enable, boolean initialized, FeatureSet features, List <GBDevice> devicesWithCalendar) {
        LOG.info("Setting broadcast receivers to: " + enable);

        if(enable && features == null){
            throw new RuntimeException("features cannot be null when enabling receivers");
        }

        if (enable && initialized && features.supportsCalendarEvents()) {
            for (GBDevice deviceWithCalendar : devicesWithCalendar) {
                if (!deviceHasCalendarReceiverRegistered(deviceWithCalendar)) {
                    if (!(GBApplication.isRunningMarshmallowOrLater() && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_DENIED)) {
                        CalendarReceiver receiver = new CalendarReceiver(this, deviceWithCalendar);
                        receiver.registerBroadcastReceivers();
                        mCalendarReceiver.add(receiver);
                    }
                }
            }
        } else {
            for (CalendarReceiver registeredReceiver: mCalendarReceiver) {
                registeredReceiver.dispose();
            }
            mCalendarReceiver.clear();
        }

        if (enable) {
            if (mPhoneCallReceiver == null) {
                mPhoneCallReceiver = new PhoneCallReceiver();
                IntentFilter filter = new IntentFilter();
                filter.addAction("android.intent.action.PHONE_STATE");
                filter.addAction("android.intent.action.NEW_OUTGOING_CALL");
                filter.addAction("nodomain.freeyourgadget.gadgetbridge.MUTE_CALL");
                ContextCompat.registerReceiver(this, mPhoneCallReceiver, filter, ContextCompat.RECEIVER_EXPORTED);
            }
            if (mSMSReceiver == null) {
                mSMSReceiver = new SMSReceiver();
                ContextCompat.registerReceiver(this, mSMSReceiver, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"), ContextCompat.RECEIVER_EXPORTED);
            }
            if (mPebbleReceiver == null) {
                mPebbleReceiver = new PebbleReceiver();
                ContextCompat.registerReceiver(this, mPebbleReceiver, new IntentFilter("com.getpebble.action.SEND_NOTIFICATION"), ContextCompat.RECEIVER_EXPORTED);
            }
            if (mMusicPlaybackReceiver == null && features.supportsMusicInfo()) {
                mMusicPlaybackReceiver = new MusicPlaybackReceiver();
                IntentFilter filter = new IntentFilter();
                for (String action : mMusicActions) {
                    filter.addAction(action);
                }
                ContextCompat.registerReceiver(this, mMusicPlaybackReceiver, filter, ContextCompat.RECEIVER_EXPORTED);
            }
            if (mTimeChangeReceiver == null) {
                mTimeChangeReceiver = new TimeChangeReceiver();
                IntentFilter filter = new IntentFilter();
                filter.addAction("android.intent.action.TIME_SET");
                filter.addAction("android.intent.action.TIMEZONE_CHANGED");
                filter.addAction(TimeChangeReceiver.ACTION_DST_CHANGED_OR_PERIODIC_SYNC);
                ContextCompat.registerReceiver(this, mTimeChangeReceiver, filter, ContextCompat.RECEIVER_EXPORTED);
                // Ensure alarm is scheduled after registering broadcast receiver
                // (this is important in case receiver was unregistered when the previous alarm arrived).
                TimeChangeReceiver.ifEnabledScheduleNextDstChangeOrPeriodicSync(this);
            }
            if (mBlueToothPairingRequestReceiver == null) {
                mBlueToothPairingRequestReceiver = new BluetoothPairingRequestReceiver(this);
                ContextCompat.registerReceiver(this, mBlueToothPairingRequestReceiver, new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST), ContextCompat.RECEIVER_EXPORTED);
            }
            if (mAlarmClockReceiver == null) {
                mAlarmClockReceiver = new AlarmClockReceiver();
                IntentFilter filter = new IntentFilter();
                filter.addAction(AlarmClockReceiver.ALARM_ALERT_ACTION);
                filter.addAction(AlarmClockReceiver.ALARM_DONE_ACTION);
                filter.addAction(AlarmClockReceiver.GOOGLE_CLOCK_ALARM_ALERT_ACTION);
                filter.addAction(AlarmClockReceiver.GOOGLE_CLOCK_ALARM_DONE_ACTION);
                ContextCompat.registerReceiver(this, mAlarmClockReceiver, filter, ContextCompat.RECEIVER_EXPORTED);
            }

            if (mSilentModeReceiver == null) {
                mSilentModeReceiver = new SilentModeReceiver();
                IntentFilter filter = new IntentFilter();
                filter.addAction(AudioManager.RINGER_MODE_CHANGED_ACTION);
                ContextCompat.registerReceiver(this, mSilentModeReceiver, filter, ContextCompat.RECEIVER_EXPORTED);
            }

            if (locationService == null) {
                locationService = new GBLocationService(this);
                LocalBroadcastManager.getInstance(this).registerReceiver(locationService, locationService.buildFilter());
            }

            if (mOsmandAidlHelper == null && features.supportsNavigation()) {
                mOsmandAidlHelper = new OsmandEventReceiver(this.getApplication());
            }

            // Weather receivers
            if (features.supportsWeather()) {
                if (GBApplication.isRunningOreoOrLater()) {
                    if (mLineageOsWeatherReceiver == null) {
                        mLineageOsWeatherReceiver = new LineageOsWeatherReceiver();
                        ContextCompat.registerReceiver(this, mLineageOsWeatherReceiver, new IntentFilter("GB_UPDATE_WEATHER"), ContextCompat.RECEIVER_EXPORTED);
                    }
                } else {
                    if (mCMWeatherReceiver == null) {
                        mCMWeatherReceiver = new CMWeatherReceiver();
                        ContextCompat.registerReceiver(this, mCMWeatherReceiver, new IntentFilter("GB_UPDATE_WEATHER"), ContextCompat.RECEIVER_EXPORTED);
                    }
                }
                if (mTinyWeatherForecastGermanyReceiver == null) {
                    mTinyWeatherForecastGermanyReceiver = new TinyWeatherForecastGermanyReceiver();
                    ContextCompat.registerReceiver(this, mTinyWeatherForecastGermanyReceiver, new IntentFilter("de.kaffeemitkoffein.broadcast.WEATHERDATA"), ContextCompat.RECEIVER_EXPORTED);
                }
                if (mGenericWeatherReceiver == null) {
                    mGenericWeatherReceiver = new GenericWeatherReceiver();
                    ContextCompat.registerReceiver(this, mGenericWeatherReceiver, new IntentFilter(GenericWeatherReceiver.ACTION_GENERIC_WEATHER), ContextCompat.RECEIVER_EXPORTED);
                }
                if (mOmniJawsObserver == null) {
                    try {
                        mOmniJawsObserver = new OmniJawsObserver(new Handler());
                        getContentResolver().registerContentObserver(OmniJawsObserver.WEATHER_URI, true, mOmniJawsObserver);
                    } catch (PackageManager.NameNotFoundException e) {
                        //Nothing wrong, it just means we're not running on omnirom.
                    }
                }
            }

            if (features.supportsSleepAsAndroid())
            {
                if (mSleepAsAndroidReceiver == null) {
                    mSleepAsAndroidReceiver = new SleepAsAndroidReceiver();
                    ContextCompat.registerReceiver(this, mSleepAsAndroidReceiver, new IntentFilter(), ContextCompat.RECEIVER_EXPORTED);
                }
            }

            if (GBApplication.getPrefs().getBoolean("auto_fetch_enabled", false) &&
                    features.supportsActivityDataFetching() && mGBAutoFetchReceiver == null) {
                mGBAutoFetchReceiver = new GBAutoFetchReceiver();
                ContextCompat.registerReceiver(this, mGBAutoFetchReceiver, new IntentFilter("android.intent.action.USER_PRESENT"), ContextCompat.RECEIVER_EXPORTED);
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

            if (mBlueToothPairingRequestReceiver != null) {
                unregisterReceiver(mBlueToothPairingRequestReceiver);
                mBlueToothPairingRequestReceiver = null;
            }
            if (mAlarmClockReceiver != null) {
                unregisterReceiver(mAlarmClockReceiver);
                mAlarmClockReceiver = null;
            }
            if (mSilentModeReceiver != null) {
                unregisterReceiver(mSilentModeReceiver);
                mSilentModeReceiver = null;
            }
            if (locationService != null) {
                LocalBroadcastManager.getInstance(this).unregisterReceiver(locationService);
                locationService.stopAll();
                locationService = null;
            }
            if (mCMWeatherReceiver != null) {
                unregisterReceiver(mCMWeatherReceiver);
                mCMWeatherReceiver = null;
            }
            if (mLineageOsWeatherReceiver != null) {
                unregisterReceiver(mLineageOsWeatherReceiver);
                mLineageOsWeatherReceiver = null;
            }
            if (mOmniJawsObserver != null) {
                getContentResolver().unregisterContentObserver(mOmniJawsObserver);
                mOmniJawsObserver = null;
            }
            if (mTinyWeatherForecastGermanyReceiver != null) {
                unregisterReceiver(mTinyWeatherForecastGermanyReceiver);
                mTinyWeatherForecastGermanyReceiver = null;
            }
            if (mOsmandAidlHelper != null) {
                mOsmandAidlHelper.cleanupResources();
                mOsmandAidlHelper = null;
            }
            if (mGBAutoFetchReceiver != null) {
                unregisterReceiver(mGBAutoFetchReceiver);
                mGBAutoFetchReceiver = null;
            }
            if (mGenericWeatherReceiver != null) {
                unregisterReceiver(mGenericWeatherReceiver);
                mGenericWeatherReceiver = null;
            }
            if (mSleepAsAndroidReceiver != null) {
                unregisterReceiver(mSleepAsAndroidReceiver);
                mSleepAsAndroidReceiver = null;
            }
        }
    }

    private void sendCachedNotifications(GBDevice device) {
        ArrayList<Intent> notifCache = cachedNotifications.get(device.getAddress());
        if (notifCache == null) return;
        try {
            while (notifCache.size() > 0) {
                handleAction(notifCache.remove(0), ACTION_NOTIFICATION, device);
            }
        } catch (DeviceNotFoundException e) {
            LOG.error("Error while sending cached notifications to "+device.getAliasOrName(), e);
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
        setReceiversEnableState(false, false, null, null); // disable BroadcastReceivers

        unregisterReceiver(mBlueToothConnectReceiver);
        mBlueToothConnectReceiver = null;

        unregisterReceiver(mAutoConnectInvervalReceiver);
        mAutoConnectInvervalReceiver.destroy();
        mAutoConnectInvervalReceiver = null;

        for(GBDevice device : getGBDevices()){
            try {
                removeDeviceSupport(device);
            } catch (DeviceNotFoundException e) {
                e.printStackTrace();
            }
        }
        GB.removeNotification(GB.NOTIFICATION_ID, this); // need to do this because the updated notification won't be cancelled when service stops

        unregisterReceiver(bluetoothCommandReceiver);
        unregisterReceiver(deviceSettingsReceiver);
        unregisterReceiver(intentApiReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (GBPrefs.DEVICE_AUTO_RECONNECT.equals(key)) {
            for(DeviceStruct deviceStruct : deviceStructs){
                boolean autoReconnect = getPrefs().getAutoReconnect(deviceStruct.getDevice());
                deviceStruct.getDeviceSupport().setAutoReconnect(autoReconnect);
            }
        }
        if (GBPrefs.CHART_MAX_HEART_RATE.equals(key) || GBPrefs.CHART_MIN_HEART_RATE.equals(key)) {
            HeartRateUtils.getInstance().updateCachedHeartRatePreferences();
        }
        if (GBPrefs.PREF_ALLOW_INTENT_API.equals(key)){
            allowBluetoothIntentApi = sharedPreferences.getBoolean(GBPrefs.PREF_ALLOW_INTENT_API, false);
            GB.log("allowBluetoothIntentApi changed to " + allowBluetoothIntentApi, GB.INFO, null);
        }
    }

    protected boolean hasPrefs() {
        return getPrefs().getPreferences() != null;
    }

    public GBPrefs getPrefs() {
        return GBApplication.getPrefs();
    }

    public GBDevice[] getGBDevices() {
        GBDevice[] devices = new GBDevice[deviceStructs.size()];
        for(int i = 0; i < devices.length; i++){
            devices[i] = deviceStructs.get(i).getDevice();
        }
        return devices;
    }
}
