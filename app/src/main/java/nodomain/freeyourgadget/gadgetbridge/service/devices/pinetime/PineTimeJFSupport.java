/*  Copyright (C) 2020-2024 Andreas Shimokawa, Arjan Schrijver, Daniel
    Dakhno, Davis Mosenkovs, Ernst, FintasticMan, Gordon Williams, ITCactus,
    Jean-François Milants, JF, Johannes Krude, kieranc001, Patric Gruber,
    Petr Kadlec, Taavi Eomäe, uli

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.pinetime;

import static nodomain.freeyourgadget.gadgetbridge.devices.pinetime.weather.WeatherData.mapOpenWeatherConditionToCloudCover;
import static nodomain.freeyourgadget.gadgetbridge.devices.pinetime.weather.WeatherData.mapOpenWeatherConditionToPineTimeCondition;
import static nodomain.freeyourgadget.gadgetbridge.devices.pinetime.weather.WeatherData.mapOpenWeatherConditionToPineTimeObscuration;
import static nodomain.freeyourgadget.gadgetbridge.devices.pinetime.weather.WeatherData.mapOpenWeatherConditionToPineTimePrecipitation;
import static nodomain.freeyourgadget.gadgetbridge.devices.pinetime.weather.WeatherData.mapOpenWeatherConditionToPineTimeSpecial;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.CborEncoder;
import no.nordicsemi.android.dfu.DfuLogListener;
import no.nordicsemi.android.dfu.DfuProgressListener;
import no.nordicsemi.android.dfu.DfuProgressListenerAdapter;
import no.nordicsemi.android.dfu.DfuServiceController;
import no.nordicsemi.android.dfu.DfuServiceInitiator;
import no.nordicsemi.android.dfu.DfuServiceListenerHelper;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventCallControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventMusicControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.pinetime.PineTimeActivitySampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.pinetime.PineTimeDFUService;
import nodomain.freeyourgadget.gadgetbridge.devices.pinetime.PineTimeInstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.pinetime.PineTimeJFConstants;
import nodomain.freeyourgadget.gadgetbridge.devices.pinetime.weather.WeatherData;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.PineTimeActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NavigationInfoSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.model.WorldClock;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattCharacteristic;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.IntentListener;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.alertnotification.AlertCategory;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.alertnotification.AlertNotificationProfile;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.alertnotification.NewAlert;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.alertnotification.OverflowStrategy;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.battery.BatteryInfoProfile;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfoProfile;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class PineTimeJFSupport extends AbstractBTLEDeviceSupport implements DfuLogListener {
    private static final Logger LOG = LoggerFactory.getLogger(PineTimeJFSupport.class);
    private final GBDeviceEventVersionInfo versionCmd = new GBDeviceEventVersionInfo();
    private final GBDeviceEventBatteryInfo batteryCmd = new GBDeviceEventBatteryInfo();

    private final DeviceInfoProfile<PineTimeJFSupport> deviceInfoProfile;
    private final BatteryInfoProfile<PineTimeJFSupport> batteryInfoProfile;

    private final int MaxNotificationLength = 100;
    private final int CutNotificationTitleMinAt = 25;
    private int firmwareVersionMajor = 0;
    private int firmwareVersionMinor = 0;
    private int firmwareVersionPatch = 0;

    private final int DAY_SECONDS = (24 * 60 * 60);
    private int quarantinedSteps = 0;

    private final int WEATHER_GRACE_TIME = 10;

    /**
     * These are used to keep track when long strings haven't changed,
     * thus avoiding unnecessary transfers that are (potentially) very slow.
     * <p>
     * Makes the device's UI more responsive.
     */
    String lastAlbum;
    String lastTrack;
    String lastArtist;
    PineTimeInstallHandler handler;
    DfuServiceController controller;

    private final DfuProgressListener progressListener = new DfuProgressListenerAdapter() {
        private final LocalBroadcastManager manager = LocalBroadcastManager.getInstance(getContext());

        /**
         * Sets the progress bar to indeterminate or not, also makes it visible
         *
         * @param indeterminate if indeterminate
         */
        public void setIndeterminate(boolean indeterminate) {
            manager.sendBroadcast(new Intent(GB.ACTION_SET_PROGRESS_BAR).putExtra(GB.PROGRESS_BAR_INDETERMINATE, indeterminate));
        }

        /**
         * Sets the status text and logs it
         */
        public void setProgress(int progress) {
            manager.sendBroadcast(new Intent(GB.ACTION_SET_PROGRESS_BAR).putExtra(GB.PROGRESS_BAR_PROGRESS, progress));
        }

        /**
         * Sets the text that describes progress
         *
         * @param progressText text to display
         */
        public void setProgressText(String progressText) {
            manager.sendBroadcast(new Intent(GB.ACTION_SET_PROGRESS_TEXT).putExtra(GB.DISPLAY_MESSAGE_MESSAGE, progressText));
        }

        @Override
        public void onDeviceConnecting(final String mac) {
            this.setIndeterminate(true);
            this.setProgressText(getContext().getString(R.string.devicestatus_connecting));
        }

        @Override
        public void onDeviceConnected(final String mac) {
            this.setIndeterminate(true);
            this.setProgressText(getContext().getString(R.string.devicestatus_connected));
        }

        @Override
        public void onEnablingDfuMode(final String mac) {
            this.setIndeterminate(true);
            this.setProgressText(getContext().getString(R.string.devicestatus_upload_starting));
        }

        @Override
        public void onDfuProcessStarting(final String mac) {
            this.setIndeterminate(true);
            this.setProgressText(getContext().getString(R.string.devicestatus_upload_starting));
        }

        @Override
        public void onDfuProcessStarted(final String mac) {
            this.setIndeterminate(true);
            this.setProgressText(getContext().getString(R.string.devicestatus_upload_started));
        }

        @Override
        public void onDeviceDisconnecting(final String mac) {
            this.setProgressText(getContext().getString(R.string.devicestatus_disconnecting));
        }

        @Override
        public void onDeviceDisconnected(final String mac) {
            this.setIndeterminate(true);
            this.setProgressText(getContext().getString(R.string.devicestatus_disconnected));
        }

        @Override
        public void onDfuCompleted(final String mac) {
            this.setProgressText(getContext().getString(R.string.devicestatus_upload_completed));
            this.setIndeterminate(false);
            this.setProgress(100);

            handler = null;
            controller = null;
            DfuServiceListenerHelper.unregisterProgressListener(getContext(), progressListener);
            gbDevice.unsetBusyTask();
            // TODO: Request reconnection
        }

        @Override
        public void onFirmwareValidating(final String mac) {
            this.setIndeterminate(true);
            this.setProgressText(getContext().getString(R.string.devicestatus_upload_validating));
        }

        @Override
        public void onDfuAborted(final String mac) {
            this.setProgressText(getContext().getString(R.string.devicestatus_upload_aborted));
            gbDevice.unsetBusyTask();
        }

        @Override
        public void onError(final String mac, int error, int errorType, final String message) {
            this.setProgressText(getContext().getString(R.string.devicestatus_upload_failed));
            gbDevice.unsetBusyTask();
        }

        @Override
        public void onProgressChanged(final String mac,
                                      int percent,
                                      float speed,
                                      float averageSpeed,
                                      int segment,
                                      int totalSegments) {
            this.setProgress(percent);
            this.setIndeterminate(false);
            this.setProgressText(String.format(Locale.ENGLISH,
                    getContext().getString(R.string.firmware_update_progress),
                    percent, speed, averageSpeed, segment, totalSegments));
        }
    };

    public PineTimeJFSupport() {
        super(LOG);
        addSupportedService(GattService.UUID_SERVICE_ALERT_NOTIFICATION);
        addSupportedService(GattService.UUID_SERVICE_CURRENT_TIME);
        addSupportedService(GattService.UUID_SERVICE_DEVICE_INFORMATION);
        addSupportedService(GattService.UUID_SERVICE_BATTERY_SERVICE);
        addSupportedService(PineTimeJFConstants.UUID_SERVICE_MUSIC_CONTROL);
        addSupportedService(PineTimeJFConstants.UUID_SERVICE_WEATHER);
        addSupportedService(PineTimeJFConstants.UUID_SERVICE_SIMPLE_WEATHER);
        addSupportedService(PineTimeJFConstants.UUID_SERVICE_NAVIGATION);
        addSupportedService(PineTimeJFConstants.UUID_CHARACTERISTIC_ALERT_NOTIFICATION_EVENT);
        addSupportedService(PineTimeJFConstants.UUID_SERVICE_MOTION);
        addSupportedService(PineTimeJFConstants.UUID_SERVICE_HEART_RATE);

        IntentListener mListener = new IntentListener() {
            @Override
            public void notify(Intent intent) {
                String action = intent.getAction();
                if (DeviceInfoProfile.ACTION_DEVICE_INFO.equals(action)) {
                    handleDeviceInfo((nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfo) intent.getParcelableExtra(DeviceInfoProfile.EXTRA_DEVICE_INFO));
                } else if (BatteryInfoProfile.ACTION_BATTERY_INFO.equals(action)) {
                    handleBatteryInfo((nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.battery.BatteryInfo) intent.getParcelableExtra(BatteryInfoProfile.EXTRA_BATTERY_INFO));
                }
            }
        };

        deviceInfoProfile = new DeviceInfoProfile<>(this);
        deviceInfoProfile.addListener(mListener);
        addSupportedProfile(deviceInfoProfile);

        AlertNotificationProfile<PineTimeJFSupport> alertNotificationProfile = new AlertNotificationProfile<>(this);
        addSupportedProfile(alertNotificationProfile);

        batteryInfoProfile = new BatteryInfoProfile<>(this);
        batteryInfoProfile.addListener(mListener);
        addSupportedProfile(batteryInfoProfile);
    }

    private void handleBatteryInfo(nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.battery.BatteryInfo info) {
        batteryCmd.level = (short) info.getPercentCharged();
        handleGBDeviceEvent(batteryCmd);
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        TransactionBuilder builder = new TransactionBuilder("notification");

        String message;
        String source = null;
        String bodyOrSubject = nodomain.freeyourgadget.gadgetbridge.util.StringUtils.getFirstOf(notificationSpec.body, notificationSpec.subject);
        String senderOrTitle = nodomain.freeyourgadget.gadgetbridge.util.StringUtils.getFirstOf(notificationSpec.sender, notificationSpec.title);
        if (!nodomain.freeyourgadget.gadgetbridge.util.StringUtils.isNullOrEmpty(notificationSpec.sourceName)) {
            source = notificationSpec.sourceName;
        } else if (notificationSpec.type == NotificationType.GENERIC_SMS) {
            source = getContext().getString(R.string.pref_title_notifications_sms);
        }

        if (bodyOrSubject.length() > 0){
            if (isFirmwareAtLeastVersion0_15()) {
                if (!GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getBoolean(DeviceSettingsPreferenceConst.PREF_PREFIX_NOTIFICATION_WITH_APP, true)) {
                    source = null;
                }
                int cutLength = Math.max(CutNotificationTitleMinAt, MaxNotificationLength - 3 - bodyOrSubject.length() - (source != null ? source.length() + 2 : 0));
                if (cutLength < senderOrTitle.length() - 1) {
                    for (; cutLength > 0 && senderOrTitle.charAt(cutLength - 1) == ' '; cutLength--);
                    senderOrTitle = senderOrTitle.substring(0, cutLength) + ">";
                }
                message = nodomain.freeyourgadget.gadgetbridge.util.StringUtils.join(": ", source, senderOrTitle) + "\0" + bodyOrSubject;
            } else {
                message = bodyOrSubject;
            }
        } else {
            if (isFirmwareAtLeastVersion0_15()) {
                message = (source != null ? source : "") + "\0" + senderOrTitle;
            } else {
                message = senderOrTitle;
            }
        }

        NewAlert alert = new NewAlert(AlertCategory.CustomHuami, 1, message);
        AlertNotificationProfile<?> profile = new AlertNotificationProfile<>(this);
        profile.setMaxLength(MaxNotificationLength);
        profile.newAlert(builder, alert, OverflowStrategy.TRUNCATE);
        builder.queue(getQueue());
    }

    @Override
    public void onSetNavigationInfo(NavigationInfoSpec navigationInfoSpec) {
        TransactionBuilder builder = new TransactionBuilder("navigation info");
        if (navigationInfoSpec.instruction == null) {
            navigationInfoSpec.instruction = "";
        }
        if (navigationInfoSpec.distanceToTurn == null) {
            navigationInfoSpec.distanceToTurn = "";
        }
        safeWriteToCharacteristic(builder, PineTimeJFConstants.UUID_CHARACTERISTICS_NAVIGATION_NARRATIVE, navigationInfoSpec.instruction.getBytes(StandardCharsets.UTF_8));
        safeWriteToCharacteristic(builder, PineTimeJFConstants.UUID_CHARACTERISTICS_NAVIGATION_MAN_DISTANCE, navigationInfoSpec.distanceToTurn.getBytes(StandardCharsets.UTF_8));
        String iconname;
        switch (navigationInfoSpec.nextAction) {
            case NavigationInfoSpec.ACTION_CONTINUE:
                iconname = "continue";
                break;
            case NavigationInfoSpec.ACTION_TURN_LEFT:
                iconname = "turn-left";
                break;
            case NavigationInfoSpec.ACTION_TURN_LEFT_SLIGHTLY:
                iconname = "turn-slight-left";
                break;
            case NavigationInfoSpec.ACTION_TURN_LEFT_SHARPLY:
                iconname = "turn-sharp-left";
                break;
            case NavigationInfoSpec.ACTION_TURN_RIGHT:
                iconname = "turn-right";
                break;
            case NavigationInfoSpec.ACTION_TURN_RIGHT_SLIGHTLY:
                iconname = "turn-slight-right";
                break;
            case NavigationInfoSpec.ACTION_TURN_RIGHT_SHARPLY:
                iconname = "turn-sharp-right";
                break;
            case NavigationInfoSpec.ACTION_KEEP_LEFT:
                iconname = "continue-left";
                break;
            case NavigationInfoSpec.ACTION_KEEP_RIGHT:
                iconname = "continue-right";
                break;
            case NavigationInfoSpec.ACTION_UTURN_LEFT:
            case NavigationInfoSpec.ACTION_UTURN_RIGHT:
                iconname = "uturn";
                break;
            case NavigationInfoSpec.ACTION_ROUNDABOUT_RIGHT:
		iconname = "roundabout-right";
		break;
            case NavigationInfoSpec.ACTION_ROUNDABOUT_LEFT:
                iconname = "roundabout-left";
                break;
            case NavigationInfoSpec.ACTION_OFFROUTE:
                iconname = "close";
                break;
            default:
                iconname = "invalid";
                break;
        }

        safeWriteToCharacteristic(builder, PineTimeJFConstants.UUID_CHARACTERISTICS_NAVIGATION_FLAGS, iconname.getBytes(StandardCharsets.UTF_8));
        builder.queue(getQueue());
    }

    @Override
    public void onDeleteNotification(int id) {

    }

    @Override
    public void onSetTime() {
        // Since this is a standard we should generalize this in Gadgetbridge (properly)
        GregorianCalendar now = BLETypeConversions.createCalendar();
        byte[] bytesCurrentTime = BLETypeConversions.calendarToCurrentTime(now, 0);
        byte[] bytesLocalTime = BLETypeConversions.calendarToLocalTime(now);

        TransactionBuilder builder = new TransactionBuilder("set time");
        builder.write(getCharacteristic(GattCharacteristic.UUID_CHARACTERISTIC_CURRENT_TIME), bytesCurrentTime);
        builder.write(getCharacteristic(GattCharacteristic.UUID_CHARACTERISTIC_LOCAL_TIME), bytesLocalTime);
        builder.queue(getQueue());
    }

    @Override
    public void onSetCallState(CallSpec callSpec) {
        if (callSpec.command == CallSpec.CALL_INCOMING) {
            TransactionBuilder builder = new TransactionBuilder("incomingcall");

            String message;
            if (isFirmwareAtLeastVersion0_15() && callSpec.sourceName != null) {
                message = (byte) 0x01 + callSpec.sourceName + "\0" + callSpec.name;
            } else {
                message = (byte) 0x01 + callSpec.name;
            }

            NewAlert alert = new NewAlert(AlertCategory.IncomingCall, 1, message);
            AlertNotificationProfile<?> profile = new AlertNotificationProfile<>(this);
            profile.setMaxLength(MaxNotificationLength);
            profile.newAlert(builder, alert, OverflowStrategy.TRUNCATE);
            builder.queue(getQueue());
        }
    }

    @Override
    public void onInstallApp(Uri uri) {
        try {
            handler = new PineTimeInstallHandler(uri, getContext());

            if (handler.isValid()) {
                gbDevice.setBusyTask("firmware upgrade");
                DfuServiceInitiator starter = new DfuServiceInitiator(getDevice().getAddress())
                        .setDeviceName(getDevice().getName())
                        .setKeepBond(true)
                        .setForeground(false)
                        .setUnsafeExperimentalButtonlessServiceInSecureDfuEnabled(false)
                        .disableMtuRequest()
                        .setZip(uri);

                controller = starter.start(getContext(), PineTimeDFUService.class);
                DfuServiceListenerHelper.registerProgressListener(getContext(), progressListener);
                DfuServiceListenerHelper.registerLogListener(getContext(), this);

                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(new Intent(GB.ACTION_SET_PROGRESS_BAR)
                        .putExtra(GB.PROGRESS_BAR_INDETERMINATE, true)
                );
                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(new Intent(GB.ACTION_SET_PROGRESS_TEXT)
                        .putExtra(GB.DISPLAY_MESSAGE_MESSAGE, getContext().getString(R.string.devicestatus_upload_starting))
                );
            } else {
                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(new Intent(GB.ACTION_SET_PROGRESS_TEXT)
                        .putExtra(GB.DISPLAY_MESSAGE_MESSAGE, getContext().getString(R.string.fwinstaller_firmware_not_compatible_to_device)));
            }
        } catch (Exception ex) {
            GB.toast(getContext(), getContext().getString(R.string.updatefirmwareoperation_write_failed) + ":" + ex.getMessage(), Toast.LENGTH_LONG, GB.ERROR, ex);
            if (gbDevice.isBusy() && gbDevice.getBusyTask().equals("firmware upgrade")) {
                gbDevice.unsetBusyTask();
            }
        }
    }

    @Override
    public void onFindDevice(boolean start) {
        CallSpec callSpec = new CallSpec();
        callSpec.command = start ? CallSpec.CALL_INCOMING : CallSpec.CALL_END;
        callSpec.name = "Gadgetbridge";
        onSetCallState(callSpec);
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));
        requestDeviceInfo(builder);
        if (GBApplication.getPrefs().getBoolean("datetime_synconconnect", true)) {
            onSetTime();
        }
        setWorldClocks();
        builder.notify(getCharacteristic(PineTimeJFConstants.UUID_CHARACTERISTICS_MUSIC_EVENT), true);
        BluetoothGattCharacteristic alertNotificationEventCharacteristic = getCharacteristic(PineTimeJFConstants.UUID_CHARACTERISTIC_ALERT_NOTIFICATION_EVENT);
        if (alertNotificationEventCharacteristic != null) {
            builder.notify(alertNotificationEventCharacteristic, true);
        }

        if (getSupportedServices().contains(PineTimeJFConstants.UUID_SERVICE_MOTION)) {
            builder.notify(getCharacteristic(PineTimeJFConstants.UUID_CHARACTERISTIC_MOTION_STEP_COUNT), true);
            //builder.notify(getCharacteristic(PineTimeJFConstants.UUID_CHARACTERISTIC_MOTION_RAW_XYZ_VALUES), false); // issue #2527
        }

        builder.notify(getCharacteristic(PineTimeJFConstants.UUID_CHARACTERISTIC_HEART_RATE_MEASUREMENT), true);

        setInitialized(builder);
        batteryInfoProfile.requestBatteryInfo(builder);
        batteryInfoProfile.enableNotify(builder, true);

        builder.requestMtu(256);
        return builder;
    }

    @Override
    public void onSetMusicInfo(MusicSpec musicSpec) {
        try {
            TransactionBuilder builder = performInitialized("send playback info");

            if (musicSpec.album != null && !musicSpec.album.equals(lastAlbum)) {
                lastAlbum = musicSpec.album;
                safeWriteToCharacteristic(builder, PineTimeJFConstants.UUID_CHARACTERISTICS_MUSIC_ALBUM, musicSpec.album.getBytes());
            }
            if (musicSpec.track != null && !musicSpec.track.equals(lastTrack)) {
                lastTrack = musicSpec.track;
                safeWriteToCharacteristic(builder, PineTimeJFConstants.UUID_CHARACTERISTICS_MUSIC_TRACK, musicSpec.track.getBytes());
            }
            if (musicSpec.artist != null && !musicSpec.artist.equals(lastArtist)) {
                lastArtist = musicSpec.artist;
                safeWriteToCharacteristic(builder, PineTimeJFConstants.UUID_CHARACTERISTICS_MUSIC_ARTIST, musicSpec.artist.getBytes());
            }

            if (musicSpec.duration != MusicSpec.MUSIC_UNKNOWN) {
                safeWriteToCharacteristic(builder, PineTimeJFConstants.UUID_CHARACTERISTICS_MUSIC_LENGTH_TOTAL, intToBytes(musicSpec.duration));
            }
            if (musicSpec.trackNr != MusicSpec.MUSIC_UNKNOWN) {
                safeWriteToCharacteristic(builder, PineTimeJFConstants.UUID_CHARACTERISTICS_MUSIC_TRACK_NUMBER, intToBytes(musicSpec.trackNr));
            }
            if (musicSpec.trackCount != MusicSpec.MUSIC_UNKNOWN) {
                safeWriteToCharacteristic(builder, PineTimeJFConstants.UUID_CHARACTERISTICS_MUSIC_TRACK_TOTAL, intToBytes(musicSpec.trackCount));
            }

            builder.queue(getQueue());
        } catch (Exception e) {
            LOG.error("Error sending music info", e);
        }
    }

    @Override
    public void onSetMusicState(MusicStateSpec stateSpec) {
        try {
            TransactionBuilder builder = performInitialized("send playback state");

            if (stateSpec.state != MusicStateSpec.STATE_UNKNOWN) {
                byte[] state = new byte[1];
                if (stateSpec.state == MusicStateSpec.STATE_PLAYING) {
                    state[0] = 0x01;
                }
                safeWriteToCharacteristic(builder, PineTimeJFConstants.UUID_CHARACTERISTICS_MUSIC_STATUS, state);
            }

            if (stateSpec.playRate != MusicStateSpec.STATE_UNKNOWN) {
                safeWriteToCharacteristic(builder, PineTimeJFConstants.UUID_CHARACTERISTICS_MUSIC_PLAYBACK_SPEED, intToBytes(stateSpec.playRate));
            }

            if (stateSpec.position != MusicStateSpec.STATE_UNKNOWN) {
                safeWriteToCharacteristic(builder, PineTimeJFConstants.UUID_CHARACTERISTICS_MUSIC_POSITION, intToBytes(stateSpec.position));
            }

            if (stateSpec.repeat != MusicStateSpec.STATE_UNKNOWN) {
                safeWriteToCharacteristic(builder, PineTimeJFConstants.UUID_CHARACTERISTICS_MUSIC_REPEAT, intToBytes(stateSpec.repeat));
            }

            if (stateSpec.shuffle != MusicStateSpec.STATE_UNKNOWN) {
                safeWriteToCharacteristic(builder, PineTimeJFConstants.UUID_CHARACTERISTICS_MUSIC_SHUFFLE, intToBytes(stateSpec.repeat));
            }

            builder.queue(getQueue());

        } catch (Exception e) {
            LOG.error("Error sending music state", e);
        }

    }

    @Override
    public boolean onCharacteristicRead(BluetoothGatt gatt,
                                        BluetoothGattCharacteristic characteristic, int status) {
        if (super.onCharacteristicRead(gatt, characteristic, status)) {
            return true;
        }
        UUID characteristicUUID = characteristic.getUuid();

        LOG.info("Unhandled characteristic read: " + characteristicUUID);
        return false;
    }

    private int getUtcOffset(WorldClock clock) {
        int offsetMillisTimezone = TimeZone.getTimeZone(clock.getTimeZoneId()).getRawOffset();
        return (offsetMillisTimezone / (1000 * 60 * 15));
    }

    private void sendWorldClocks(List<? extends WorldClock> clocks) {
        final DeviceCoordinator coordinator = gbDevice.getDeviceCoordinator();
        if (coordinator.getWorldClocksSlotCount() == 0) {
            return;
        }

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            for (final WorldClock clock : clocks) {
                int utcOffsetInQuarterHours = getUtcOffset(clock);

                baos.write(utcOffsetInQuarterHours);

                baos.write(clock.getLabel().getBytes());

                baos.write(0);

                //pad string to 9 bytes
                while(baos.size() % 10 != 0){
                    baos.write(0);
                }
            }

            while(baos.size() < coordinator.getWorldClocksSlotCount() * 10){
                baos.write(-128); //invalid slot
                baos.write(new byte[9]);
            }

            TransactionBuilder builder = new TransactionBuilder("set world clocks");
            builder.write(getCharacteristic(PineTimeJFConstants.UUID_CHARACTERISTIC_WORLD_TIME), baos.toByteArray());
            builder.queue(getQueue());
        } catch (Exception e) {
            LOG.error("Error sending world clocks", e);
        }
    }

    private void setWorldClocks() {
        final List<? extends WorldClock> clocks = DBHelper.getWorldClocks(gbDevice);
        sendWorldClocks(clocks);
    }

    @Override
    public void onSetWorldClocks(ArrayList<? extends WorldClock> clocks) {
        sendWorldClocks(clocks);
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic) {
        if (super.onCharacteristicChanged(gatt, characteristic)) {
            return true;
        }

        UUID characteristicUUID = characteristic.getUuid();
        if (characteristicUUID.equals(PineTimeJFConstants.UUID_CHARACTERISTICS_MUSIC_EVENT)) {
            byte[] value = characteristic.getValue();
            GBDeviceEventMusicControl deviceEventMusicControl = new GBDeviceEventMusicControl();

            switch (value[0]) {
                case 0:
                    deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.PLAY;
                    break;
                case 1:
                    deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.PAUSE;
                    break;
                case 3:
                    deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.NEXT;
                    break;
                case 4:
                    deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.PREVIOUS;
                    break;
                case 5:
                    deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.VOLUMEUP;
                    break;
                case 6:
                    deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.VOLUMEDOWN;
                    break;
                default:
                    return false;
            }
            evaluateGBDeviceEvent(deviceEventMusicControl);
            return true;
        } else if (characteristicUUID.equals(PineTimeJFConstants.UUID_CHARACTERISTIC_ALERT_NOTIFICATION_EVENT)) {
            byte[] value = characteristic.getValue();
            GBDeviceEventCallControl deviceEventCallControl = new GBDeviceEventCallControl();
            switch (value[0]) {
                case 0:
                    deviceEventCallControl.event = GBDeviceEventCallControl.Event.REJECT;
                    break;
                case 1:
                    deviceEventCallControl.event = GBDeviceEventCallControl.Event.ACCEPT;
                    break;
                case 2:
                    deviceEventCallControl.event = GBDeviceEventCallControl.Event.IGNORE;
                    break;
                default:
                    return false;
            }
            evaluateGBDeviceEvent(deviceEventCallControl);
            return true;
        } else if (characteristicUUID.equals(PineTimeJFConstants.UUID_CHARACTERISTIC_MOTION_STEP_COUNT)) {
            int steps = BLETypeConversions.toUint32(characteristic.getValue());
            if (LOG.isDebugEnabled()) {
                LOG.debug("onCharacteristicChanged: MotionService:Steps=" + steps);
            }
            onReceiveStepsSample(steps);
            return true;
        } else if (characteristicUUID.equals(PineTimeJFConstants.UUID_CHARACTERISTIC_HEART_RATE_MEASUREMENT)) {
            int heartrate = Byte.toUnsignedInt(characteristic.getValue()[1]);
            if (LOG.isDebugEnabled()) {
                LOG.debug("onCharacteristicChanged: HeartRateMeasurement:HeartRate=" + heartrate);
            }
            onReceiveHeartRateMeasurement(heartrate);
            return true;
        }

        LOG.info("Unhandled characteristic changed: " + characteristicUUID);
        return false;
    }

    private void onSendWeatherCBOR(WeatherSpec weatherSpec) {
        if (weatherSpec.location != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                new CborEncoder(baos).encode(new CborBuilder()
                        .startMap() // This map is not fixed-size, which is not great, but it might come in a library update
                        .put("Timestamp", System.currentTimeMillis() / 1000L)
                        .put("Expires", 60 * 60 * 1 + WEATHER_GRACE_TIME) // 1h
                        .put("EventType", WeatherData.EventType.Location.value)
                        .put("Location", weatherSpec.location)
                        .put("Altitude", 0)
                        .put("Latitude", 0)
                        .put("Longitude", 0)
                        .end()
                        .build()
                );
            } catch (Exception e) {
                LOG.warn(String.valueOf(e));
            }
            byte[] encodedBytes = baos.toByteArray();
            TransactionBuilder builder = createTransactionBuilder("WeatherData");
            safeWriteToCharacteristic(builder,
                    PineTimeJFConstants.UUID_CHARACTERISTIC_WEATHER_DATA,
                    encodedBytes);

            builder.queue(getQueue());
        }

        // Current condition
        if (weatherSpec.currentCondition != null) {
            // We can't do anything with this?
        }

        // Current humidity
        if (weatherSpec.currentHumidity > 0) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                new CborEncoder(baos).encode(new CborBuilder()
                        .startMap() // This map is not fixed-size, which is not great, but it might come in a library update
                        .put("Timestamp", System.currentTimeMillis() / 1000L)
                        .put("Expires", 60 * 60 * 1 + WEATHER_GRACE_TIME) // 1h this should be the weather provider's interval, really
                        .put("EventType", WeatherData.EventType.Humidity.value)
                        .put("Humidity", (int) weatherSpec.currentHumidity)
                        .end()
                        .build()
                );
            } catch (Exception e) {
                LOG.warn(String.valueOf(e));
            }
            byte[] encodedBytes = baos.toByteArray();
            TransactionBuilder builder = createTransactionBuilder("WeatherData");
            safeWriteToCharacteristic(builder,
                    PineTimeJFConstants.UUID_CHARACTERISTIC_WEATHER_DATA,
                    encodedBytes);

            builder.queue(getQueue());
        }

        // Current temperature
        if (weatherSpec.currentTemp >= -273.15) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                new CborEncoder(baos).encode(new CborBuilder()
                        .startMap() // This map is not fixed-size, which is not great, but it might come in a library update
                        .put("Timestamp", System.currentTimeMillis() / 1000L)
                        .put("Expires", 60 * 60 * 1 + WEATHER_GRACE_TIME) // 1h this should be the weather provider's interval, really
                        .put("EventType", WeatherData.EventType.Temperature.value)
                        .put("Temperature", (int) ((weatherSpec.currentTemp - 273.15) * 100))
                        .put("DewPoint", (int) (-32768))
                        .end()
                        .build()
                );
            } catch (Exception e) {
                LOG.warn(String.valueOf(e));
            }
            byte[] encodedBytes = baos.toByteArray();
            TransactionBuilder builder = createTransactionBuilder("WeatherData");
            safeWriteToCharacteristic(builder,
                    PineTimeJFConstants.UUID_CHARACTERISTIC_WEATHER_DATA,
                    encodedBytes);

            builder.queue(getQueue());
        }

        // 24h temperature forecast
        // TODO: This is disabled until WeatherSpec contains how often this data is pushed
        /*
        if (weatherSpec.todayMinTemp >= -273.15 &&
                weatherSpec.todayMaxTemp >= -273.15) { // Some sanity checking, should really be nullable
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                new CborEncoder(baos).encode(new CborBuilder()
                        .startMap() // This map is not fixed-size, which is not great, but it might come in a library update
                        .put("Timestamp", System.currentTimeMillis() / 1000L)
                        .put("Expires", 60 * 60 * 24) // 24h, because the temperature is today's
                        .put("EventType", WeatherData.EventType.Temperature.value)
                        .put("Temperature", (int) ((((weatherSpec.todayMinTemp - 273.15) + (weatherSpec.todayMaxTemp - 273.15)) / 2) * 100))
                        .put("DewPoint", (int) (-32768))
                        .end()
                        .build()
                );
            } catch (Exception e) {
                LOG.warn(String.valueOf(e));
            }
            byte[] encodedBytes = baos.toByteArray();
            TransactionBuilder builder = createTransactionBuilder("WeatherData");
            safeWriteToCharacteristic(builder,
                    PineTimeJFConstants.UUID_CHARACTERISTIC_WEATHER_DATA,
                    encodedBytes);

            builder.queue(getQueue());
        }
        */

        // Wind speed
        if (weatherSpec.windSpeed != 0.0f) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                new CborEncoder(baos).encode(new CborBuilder()
                        .startMap() // This map is not fixed-size, which is not great, but it might come in a library update
                        .put("Timestamp", System.currentTimeMillis() / 1000L)
                        .put("Expires", 60 * 60 * 1 + WEATHER_GRACE_TIME) // 1h
                        .put("EventType", WeatherData.EventType.Wind.value)
                        .put("SpeedMin", (int) (weatherSpec.windSpeed / 60 / 60 * 1000))
                        .put("SpeedMax", (int) (weatherSpec.windSpeed / 60 / 60 * 1000))
                        .put("DirectionMin", (int) (0.71 * weatherSpec.windDirection))
                        .put("DirectionMax", (int) (0.71 * weatherSpec.windDirection))
                        .end()
                        .build()
                );
            } catch (Exception e) {
                LOG.warn(String.valueOf(e));
            }
            byte[] encodedBytes = baos.toByteArray();
            TransactionBuilder builder = createTransactionBuilder("WeatherData");
            safeWriteToCharacteristic(builder,
                    PineTimeJFConstants.UUID_CHARACTERISTIC_WEATHER_DATA,
                    encodedBytes);

            builder.queue(getQueue());
        }

        // Current weather condition
        if (mapOpenWeatherConditionToPineTimePrecipitation(weatherSpec.currentConditionCode) != WeatherData.PrecipitationType.Length) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                new CborEncoder(baos).encode(new CborBuilder()
                        .startMap() // This map is not fixed-size, which is not great, but it might come in a library update
                        .put("Timestamp", System.currentTimeMillis() / 1000L)
                        .put("Expires", 60 * 60 * 1 + WEATHER_GRACE_TIME) // 1h
                        .put("EventType", WeatherData.EventType.Precipitation.value)
                        .put("Type", (int) mapOpenWeatherConditionToPineTimePrecipitation(weatherSpec.currentConditionCode).value)
                        .put("Amount", (int) 0)
                        .end()
                        .build()
                );
            } catch (Exception e) {
                LOG.warn(String.valueOf(e));
            }
            byte[] encodedBytes = baos.toByteArray();
            TransactionBuilder builder = createTransactionBuilder("WeatherData");
            safeWriteToCharacteristic(builder,
                    PineTimeJFConstants.UUID_CHARACTERISTIC_WEATHER_DATA,
                    encodedBytes);

            builder.queue(getQueue());
        }

        if (mapOpenWeatherConditionToPineTimeObscuration(weatherSpec.currentConditionCode) != WeatherData.ObscurationType.Length) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                new CborEncoder(baos).encode(new CborBuilder()
                        .startMap() // This map is not fixed-size, which is not great, but it might come in a library update
                        .put("Timestamp", System.currentTimeMillis() / 1000L)
                        .put("Expires", 60 * 60 * 1 + WEATHER_GRACE_TIME) // 1h
                        .put("EventType", WeatherData.EventType.Obscuration.value)
                        .put("Type", (int) mapOpenWeatherConditionToPineTimeObscuration(weatherSpec.currentConditionCode).value)
                        .put("Amount", (int) 65535)
                        .end()
                        .build()
                );
            } catch (Exception e) {
                LOG.warn(String.valueOf(e));
            }
            byte[] encodedBytes = baos.toByteArray();
            TransactionBuilder builder = createTransactionBuilder("WeatherData");
            safeWriteToCharacteristic(builder,
                    PineTimeJFConstants.UUID_CHARACTERISTIC_WEATHER_DATA,
                    encodedBytes);

            builder.queue(getQueue());
        }

        if (mapOpenWeatherConditionToPineTimeSpecial(weatherSpec.currentConditionCode) != WeatherData.SpecialType.Length) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                new CborEncoder(baos).encode(new CborBuilder()
                        .startMap() // This map is not fixed-size, which is not great, but it might come in a library update
                        .put("Timestamp", System.currentTimeMillis() / 1000L)
                        .put("Expires", 60 * 60 * 1 + WEATHER_GRACE_TIME) // 1h
                        .put("EventType", WeatherData.EventType.Special.value)
                        .put("Type", mapOpenWeatherConditionToPineTimeSpecial(weatherSpec.currentConditionCode).value)
                        .end()
                        .build()
                );
            } catch (Exception e) {
                LOG.warn(String.valueOf(e));
            }
            byte[] encodedBytes = baos.toByteArray();
            TransactionBuilder builder = createTransactionBuilder("WeatherData");
            safeWriteToCharacteristic(builder,
                    PineTimeJFConstants.UUID_CHARACTERISTIC_WEATHER_DATA,
                    encodedBytes);

            builder.queue(getQueue());
        }

        if (mapOpenWeatherConditionToCloudCover(weatherSpec.currentConditionCode) != -1) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                new CborEncoder(baos).encode(new CborBuilder()
                        .startMap() // This map is not fixed-size, which is not great, but it might come in a library update
                        .put("Timestamp", System.currentTimeMillis() / 1000L)
                        .put("Expires", 60 * 60 * 1 + WEATHER_GRACE_TIME) // 1h
                        .put("EventType", WeatherData.EventType.Clouds.value)
                        .put("Amount", (int) (mapOpenWeatherConditionToCloudCover(weatherSpec.currentConditionCode)))
                        .end()
                        .build()
                );
            } catch (Exception e) {
                LOG.warn(String.valueOf(e));
            }
            byte[] encodedBytes = baos.toByteArray();
            TransactionBuilder builder = createTransactionBuilder("WeatherData");
            safeWriteToCharacteristic(builder,
                    PineTimeJFConstants.UUID_CHARACTERISTIC_WEATHER_DATA,
                    encodedBytes);

            builder.queue(getQueue());
        }

        LOG.debug("Wrote weather data");
    }

    private void onSendWeatherSimple(WeatherSpec weatherSpec) {
        long timestampLocal = weatherSpec.timestamp + Calendar.getInstance().getTimeZone().getOffset(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis()) / 1000L;

        ByteBuffer currentPacket = ByteBuffer.allocate(49).order(ByteOrder.LITTLE_ENDIAN);
        currentPacket.putLong(2, timestampLocal);
        currentPacket.putShort(10, (short) ((weatherSpec.currentTemp - 273.15) * 100));
        currentPacket.putShort(12, (short) ((weatherSpec.todayMinTemp - 273.15) * 100));
        currentPacket.putShort(14, (short) ((weatherSpec.todayMaxTemp - 273.15) * 100));
        if (weatherSpec.location != null) {
            byte[] locationBytes = nodomain.freeyourgadget.gadgetbridge.util.StringUtils.truncateToBytes(weatherSpec.location, 32);
            for (int i = 0; i < locationBytes.length; i++) {
                currentPacket.put(16 + i, locationBytes[i]);
            }
        }
        currentPacket.put(48, mapOpenWeatherConditionToPineTimeCondition(weatherSpec.currentConditionCode).value);

        TransactionBuilder currentBuilder = createTransactionBuilder("SimpleWeatherData");
        safeWriteToCharacteristic(currentBuilder,
            PineTimeJFConstants.UUID_CHARACTERISTIC_SIMPLE_WEATHER_DATA,
            currentPacket.array());

        currentBuilder.queue(getQueue());

        if (weatherSpec.forecasts == null) {
            return;
        }

        ByteBuffer forecastPacket = ByteBuffer.allocate(36).order(ByteOrder.LITTLE_ENDIAN);
        forecastPacket.put(0, (byte) 1);
        forecastPacket.putLong(2, timestampLocal);
        byte nbDays = (byte) Math.min(weatherSpec.forecasts.size(), 5);
        forecastPacket.put(10, nbDays);
        for (int i = 0; i < nbDays; i++) {
            forecastPacket.putShort(11 + i * 5, (short) ((weatherSpec.forecasts.get(i).minTemp - 273.15) * 100));
            forecastPacket.putShort(11 + i * 5 + 2, (short) ((weatherSpec.forecasts.get(i).maxTemp - 273.15) * 100));
            forecastPacket.put(11 + i * 5 + 4, mapOpenWeatherConditionToPineTimeCondition(weatherSpec.forecasts.get(i).conditionCode).value);
        }

        TransactionBuilder forecastBuilder = createTransactionBuilder("SimpleWeatherData");
        safeWriteToCharacteristic(forecastBuilder,
                PineTimeJFConstants.UUID_CHARACTERISTIC_SIMPLE_WEATHER_DATA,
                forecastPacket.array());

        forecastBuilder.queue(getQueue());
    }

    @Override
    public void onSendWeather(ArrayList<WeatherSpec> weatherSpecs) {
        WeatherSpec weatherSpec = weatherSpecs.get(0);

        if (this.firmwareVersionMajor < 1 || (this.firmwareVersionMajor == 1 && this.firmwareVersionMinor <= 7)) {
            // Not supported
            return;
        }

        if (this.firmwareVersionMajor == 1 && this.firmwareVersionMinor <= 13) {
            this.onSendWeatherCBOR(weatherSpec);
        } else {
            this.onSendWeatherSimple(weatherSpec);
        }
    }

    /**
     * Helper function that just converts an integer into a byte array
     */
    private static byte[] intToBytes(int source) {
        return ByteBuffer.allocate(4).putInt(source).array();
    }

    /**
     * This will check if the characteristic exists and can be written
     * <p>
     * Keeps backwards compatibility with firmware that can't take all the information
     */
    private void safeWriteToCharacteristic(TransactionBuilder builder, UUID uuid, byte[] data) {
        BluetoothGattCharacteristic characteristic = getCharacteristic(uuid);
        if (characteristic != null &&
                (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
            builder.write(characteristic, data);
        } else {
            LOG.warn("Tried to write to a characteristic that did not exist or was not writable!");
        }
    }

    private void setInitialized(TransactionBuilder builder) {
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()));
    }

    private void requestDeviceInfo(TransactionBuilder builder) {
        LOG.debug("Requesting Device Info!");
        deviceInfoProfile.requestDeviceInfo(builder);
    }

    private void handleDeviceInfo(nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfo info) {
        LOG.warn("Device info: " + info);
        versionCmd.hwVersion = info.getHardwareRevision();
        versionCmd.fwVersion = info.getFirmwareRevision();

        if (versionCmd.fwVersion != null && !versionCmd.fwVersion.isEmpty()) {
            // FW version format : "major.minor.patch". Ex : "0.8.2"
            String[] tokens = StringUtils.split(versionCmd.fwVersion, ".");
            if (tokens.length == 3) {
                firmwareVersionMajor = Integer.parseInt(tokens[0]);
                firmwareVersionMinor = Integer.parseInt(tokens[1]);
                firmwareVersionPatch = Integer.parseInt(tokens[2]);
            }
        }

        handleGBDeviceEvent(versionCmd);
    }

    private boolean isFirmwareAtLeastVersion0_15() {
        return firmwareVersionMajor > 0 || firmwareVersionMinor >= 15;
    }

    /**
     * Nordic DFU needs this function to log DFU-related messages
     */
    @Override
    public void onLogEvent(final String deviceAddress, final int level, final String message) {
        LOG.debug(message);
    }

    private void onReceiveStepsSample(int steps) {
        this.onReceiveStepsSample((int) (Calendar.getInstance().getTimeInMillis() / 1000l), steps);
    }

    private void onReceiveStepsSample(int timeStamp, int steps) {
        PineTimeActivitySample sample = new PineTimeActivitySample();

        int dayStepCount = this.getStepsOnDay(timeStamp);
        int prevoiusDayStepCount = this.getStepsOnDay(timeStamp - DAY_SECONDS);
        int diff = steps - dayStepCount;
        logDebug(String.format("onReceiveStepsSample: \ndayStepCount=%d, \nsteps=%d, \ndiff=%d, \nprevoiusDayStepCount=%d, ", dayStepCount, steps, diff, prevoiusDayStepCount));

        if (dayStepCount == 0) {
            if (quarantinedSteps == 0 && steps > 0) {
                logDebug("ignoring " + diff + " steps: ignore the first sync of the day as it could be outstanding values from the previous day");
                quarantinedSteps = steps;
                return;
            } else if (quarantinedSteps > 0 && steps == 0) {
                logDebug("dropp " + quarantinedSteps + " outstanding steps: those looks like outstanding values from the previous day");
                quarantinedSteps = 0;
            } else {
                logDebug("reset " + quarantinedSteps + " quarantined steps...");
                quarantinedSteps = 0;
            }
        }

        if (diff > 0) {
            logDebug("adding " + diff + " steps");

            sample.setSteps(diff);
            sample.setTimestamp(timeStamp);

            // since it's a local timestamp, it should NOT be treated as Activity because it will spoil activity charts
            sample.setRawKind(ActivityKind.UNKNOWN.getCode());

            this.addGBActivitySample(sample);

            Intent intent = new Intent(DeviceService.ACTION_REALTIME_SAMPLES)
                    .putExtra(GBDevice.EXTRA_DEVICE, getDevice())
                    .putExtra(DeviceService.EXTRA_REALTIME_SAMPLE, sample)
                    .putExtra(DeviceService.EXTRA_TIMESTAMP, sample.getTimestamp());
            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
        } else {
            logDebug("ignoring " + diff + " steps");
        }
    }

    private void onReceiveHeartRateMeasurement(int heartrate) {
        this.onReceiveHeartRateMeasurement((int) (Calendar.getInstance().getTimeInMillis() / 1000l), heartrate);
    }

    private void onReceiveHeartRateMeasurement(int timeStamp, int heartrate) {
        PineTimeActivitySample sample = new PineTimeActivitySample();

        logDebug(String.format("onReceiveHeartRateMeasurement: \nheartrate=%d", heartrate));

        if (heartrate > 0) {
            sample.setHeartRate(heartrate);
            sample.setTimestamp(timeStamp);
            // since it's a local timestamp, it should NOT be treated as Activity because it will spoil activity charts
            sample.setRawKind(ActivityKind.UNKNOWN.getCode());

            this.addGBActivitySample(sample);

            Intent intent = new Intent(DeviceService.ACTION_REALTIME_SAMPLES)
                    .putExtra(GBDevice.EXTRA_DEVICE, getDevice())
                    .putExtra(DeviceService.EXTRA_REALTIME_SAMPLE, sample)
                    .putExtra(DeviceService.EXTRA_TIMESTAMP, sample.getTimestamp());
            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
        } else {
            logDebug("ignoring heartrate of 0");
        }
    }

    /**
     * @param timeStamp Time stamp (in seconds)  at some point during the requested day.
     */
    private int getStepsOnDay(int timeStamp) {
        try (DBHandler dbHandler = GBApplication.acquireDB()) {

            Calendar dayStart = Calendar.getInstance();
            Calendar dayEnd = Calendar.getInstance();

            this.getDayStartEnd(timeStamp, dayStart, dayEnd);

            PineTimeActivitySampleProvider provider = new PineTimeActivitySampleProvider(this.getDevice(), dbHandler.getDaoSession());

            List<PineTimeActivitySample> samples = provider.getAllActivitySamples(
                    (int) (dayStart.getTimeInMillis() / 1000L),
                    (int) (dayEnd.getTimeInMillis() / 1000L));

            int totalSteps = 0;

            for (PineTimeActivitySample sample : samples) {
                totalSteps += sample.getSteps();
            }

            return totalSteps;

        } catch (Exception ex) {
            LOG.error(ex.getMessage());

            return 0;
        }
    }

    /**
     * @param timeStampUtc in seconds
     */
    private void getDayStartEnd(int timeStampUtc, Calendar start, Calendar end) {
        int timeOffsetToUtc = Calendar.getInstance().getTimeZone().getOffset(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis()) / 1000;
        int timeStampLocal = timeStampUtc + timeOffsetToUtc;
        int timeStampStartUtc = ((timeStampLocal / DAY_SECONDS) * DAY_SECONDS);
        int timeStampEndUtc = (timeStampStartUtc + DAY_SECONDS - 1);

        start.setTimeInMillis((timeStampStartUtc - timeOffsetToUtc) * 1000L);
        end.setTimeInMillis((timeStampEndUtc - timeOffsetToUtc) * 1000L);

        Calendar calTimeStamp = new GregorianCalendar();
        calTimeStamp.setTimeInMillis(timeStampUtc * 1000L);

        logDebug(String.format("getDayStartEnd: \ntimeStamp=%d (%s), \ntimeStampStartUtc=%d (-offset=%s), \ntimeStampEndUtc=%d (-offset=%s)", timeStampUtc, calTimeStamp.getTime(), timeStampStartUtc, start.getTime(), timeStampEndUtc, end.getTime()));
    }


    private void addGBActivitySamples(PineTimeActivitySample[] samples) {
        try (DBHandler dbHandler = GBApplication.acquireDB()) {

            User user = DBHelper.getUser(dbHandler.getDaoSession());
            Device device = DBHelper.getDevice(this.getDevice(), dbHandler.getDaoSession());

            PineTimeActivitySampleProvider provider = new PineTimeActivitySampleProvider(this.getDevice(), dbHandler.getDaoSession());

            for (PineTimeActivitySample sample : samples) {
                sample.setDevice(device);
                sample.setUser(user);
                sample.setProvider(provider);

                sample.setRawIntensity(ActivitySample.NOT_MEASURED);

                Optional<PineTimeActivitySample> storedSample = provider.getSampleForTimestamp(sample.getTimestamp());
                if (storedSample.isPresent()) {
                    sample.setHeartRate(Math.max(sample.getHeartRate(), storedSample.get().getHeartRate()));
                    sample.setSteps(Math.max(sample.getSteps(), storedSample.get().getSteps()));
                }

                provider.addGBActivitySample(sample);
            }

            GB.signalActivityDataFinish(getDevice());

        } catch (Exception ex) {
            GB.toast(getContext(), "Error saving samples: " + ex.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
            GB.updateTransferNotification(null, "Data transfer failed", false, 0, getContext());

            LOG.error(ex.getMessage());
        }
    }

    private void addGBActivitySample(PineTimeActivitySample sample) {
        this.addGBActivitySamples(new PineTimeActivitySample[]{sample});
    }

    private void logDebug(String logMessage) {
        logDebug(logMessage, logMessage);
    }

    private void logDebug(String logMessage, String toastMessage) {
        LOG.debug(logMessage);
        //GB.toast(getContext(), toastMessage, Toast.LENGTH_LONG, GB.WARN);
    }

    @Override
    public boolean getImplicitCallbackModify() {
        return true;
    }

    @Override
    public boolean getSendWriteRequestResponse() {
        return false;
    }
}
