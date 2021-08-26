/*  Copyright (C) 2016-2021 Andreas Shimokawa, Carsten Pfeiffer, JF, Sebastian
    Kranz, Taavi Eomäe

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.pinetime;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.UUID;

import no.nordicsemi.android.dfu.DfuLogListener;
import no.nordicsemi.android.dfu.DfuProgressListener;
import no.nordicsemi.android.dfu.DfuProgressListenerAdapter;
import no.nordicsemi.android.dfu.DfuServiceController;
import no.nordicsemi.android.dfu.DfuServiceInitiator;
import no.nordicsemi.android.dfu.DfuServiceListenerHelper;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventCallControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventMusicControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.pinetime.PineTimeDFUService;
import nodomain.freeyourgadget.gadgetbridge.devices.pinetime.PineTimeInstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.pinetime.PineTimeJFConstants;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
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
    private int firmwareVersionMajor = 0;
    private int firmwareVersionMinor = 0;
    private int firmwareVersionPatch = 0;
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
        addSupportedService(PineTimeJFConstants.UUID_CHARACTERISTIC_ALERT_NOTIFICATION_EVENT);

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
        if (notificationSpec.body == null) {
            notificationSpec.body = "";
        }

        if (isFirmwareAtLeastVersion0_15()) {
            String senderOrTitle = nodomain.freeyourgadget.gadgetbridge.util.StringUtils.getFirstOf(notificationSpec.sender, notificationSpec.title);
            message = senderOrTitle + "\0" + notificationSpec.body;
        } else {
            message = notificationSpec.body;
        }

        NewAlert alert = new NewAlert(AlertCategory.CustomHuami, 1, message);
        AlertNotificationProfile<?> profile = new AlertNotificationProfile<>(this);
        profile.setMaxLength(MaxNotificationLength);
        profile.newAlert(builder, alert, OverflowStrategy.TRUNCATE);
        builder.queue(getQueue());
    }

    @Override
    public void onDeleteNotification(int id) {

    }

    @Override
    public void onSetTime() {
        // Since this is a standard we should generalize this in Gadgetbridge (properly)
        GregorianCalendar now = BLETypeConversions.createCalendar();
        byte[] bytes = BLETypeConversions.calendarToRawBytes(now);
        byte[] tail = new byte[]{0, BLETypeConversions.mapTimeZone(now.getTimeZone(), BLETypeConversions.TZ_FLAG_INCLUDE_DST_IN_TZ)};
        byte[] all = BLETypeConversions.join(bytes, tail);

        TransactionBuilder builder = new TransactionBuilder("set time");
        builder.write(getCharacteristic(GattCharacteristic.UUID_CHARACTERISTIC_CURRENT_TIME), all);
        builder.queue(getQueue());
    }

    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {

    }

    @Override
    public void onSetCallState(CallSpec callSpec) {
        if (callSpec.command == CallSpec.CALL_INCOMING) {
            TransactionBuilder builder = new TransactionBuilder("incomingcall");
            String message = (byte) 0x01 + callSpec.name;
            NewAlert alert = new NewAlert(AlertCategory.IncomingCall, 1, message);
            AlertNotificationProfile<?> profile = new AlertNotificationProfile<>(this);
            profile.setMaxLength(MaxNotificationLength);
            profile.newAlert(builder, alert, OverflowStrategy.TRUNCATE);
            builder.queue(getQueue());
        }
    }

    @Override
    public void onSetCannedMessages(CannedMessagesSpec cannedMessagesSpec) {

    }

    @Override
    public void onEnableRealtimeSteps(boolean enable) {

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
                        .setMtu(517)
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
    public void onAppInfoReq() {

    }

    @Override
    public void onAppStart(UUID uuid, boolean start) {

    }

    @Override
    public void onAppDelete(UUID uuid) {

    }

    @Override
    public void onAppConfiguration(UUID appUuid, String config, Integer id) {

    }

    @Override
    public void onAppReorder(UUID[] uuids) {

    }

    @Override
    public void onFetchRecordedData(int dataTypes) {

    }

    @Override
    public void onReset(int flags) {

    }

    @Override
    public void onHeartRateTest() {

    }

    @Override
    public void onEnableRealtimeHeartRateMeasurement(boolean enable) {

    }

    @Override
    public void onFindDevice(boolean start) {
        CallSpec callSpec = new CallSpec();
        callSpec.command = start ? CallSpec.CALL_INCOMING : CallSpec.CALL_END;
        callSpec.name = "Gadgetbridge";
        onSetCallState(callSpec);
    }

    @Override
    public void onSetConstantVibration(int intensity) {

    }

    @Override
    public void onScreenshotReq() {

    }

    @Override
    public void onEnableHeartRateSleepSupport(boolean enable) {

    }

    @Override
    public void onSetHeartRateMeasurementInterval(int seconds) {

    }

    @Override
    public void onAddCalendarEvent(CalendarEventSpec calendarEventSpec) {

    }

    @Override
    public void onDeleteCalendarEvent(byte type, long id) {

    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));
        requestDeviceInfo(builder);
        onSetTime();
        builder.notify(getCharacteristic(PineTimeJFConstants.UUID_CHARACTERISTICS_MUSIC_EVENT), true);
        BluetoothGattCharacteristic alertNotificationEventCharacteristic = getCharacteristic(PineTimeJFConstants.UUID_CHARACTERISTIC_ALERT_NOTIFICATION_EVENT);
        if (alertNotificationEventCharacteristic != null) {
            builder.notify(alertNotificationEventCharacteristic, true);
        }
        setInitialized(builder);
        batteryInfoProfile.requestBatteryInfo(builder);
        batteryInfoProfile.enableNotify(builder,true);

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

    @Override
    public void onSendConfiguration(String config) {

    }

    @Override
    public void onReadConfiguration(String config) {

    }

    @Override
    public void onTestNewFunction() {

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
        }

        LOG.info("Unhandled characteristic changed: " + characteristicUUID);
        return false;
    }

    @Override
    public void onSendWeather(WeatherSpec weatherSpec) {

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

        if(versionCmd.fwVersion != null && !versionCmd.fwVersion.isEmpty()) {
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
}
