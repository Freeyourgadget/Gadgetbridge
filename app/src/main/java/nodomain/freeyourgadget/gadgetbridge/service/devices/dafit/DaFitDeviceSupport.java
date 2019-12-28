/*  Copyright (C) 2019 krzys_h

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.dafit;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.Logging;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.dafit.DaFitConstants;
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
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.IntentListener;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.battery.BatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.battery.BatteryInfoProfile;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfo;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfoProfile;

// TODO: figure out the training data

public class DaFitDeviceSupport extends AbstractBTLEDeviceSupport {

    private static final Logger LOG = LoggerFactory.getLogger(DaFitDeviceSupport.class);

    private final DeviceInfoProfile<DaFitDeviceSupport> deviceInfoProfile;
    private final BatteryInfoProfile<DaFitDeviceSupport> batteryInfoProfile;
    private final GBDeviceEventVersionInfo versionCmd = new GBDeviceEventVersionInfo();
    private final GBDeviceEventBatteryInfo batteryCmd = new GBDeviceEventBatteryInfo();
    private final IntentListener mListener = new IntentListener() {
        @Override
        public void notify(Intent intent) {
            String s = intent.getAction();
            if (Objects.equals(s, DeviceInfoProfile.ACTION_DEVICE_INFO)) {
                handleDeviceInfo((DeviceInfo) intent.getParcelableExtra(DeviceInfoProfile.EXTRA_DEVICE_INFO));
            }
            if (Objects.equals(s, BatteryInfoProfile.ACTION_BATTERY_INFO)) {
                handleBatteryInfo((BatteryInfo) intent.getParcelableExtra(BatteryInfoProfile.EXTRA_BATTERY_INFO));
            }
        }
    };

    public static final int MTU = 20; // TODO: there seems to be some way to change this value...?
    private DaFitPacketIn packetIn = new DaFitPacketIn();

    public DaFitDeviceSupport() {
        super(LOG);

        addSupportedService(GattService.UUID_SERVICE_GENERIC_ACCESS);
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ATTRIBUTE);
        addSupportedService(GattService.UUID_SERVICE_DEVICE_INFORMATION);
        addSupportedService(GattService.UUID_SERVICE_BATTERY_SERVICE);
        addSupportedService(DaFitConstants.UUID_SERVICE_DAFIT);

        deviceInfoProfile = new DeviceInfoProfile<>(this);
        deviceInfoProfile.addListener(mListener);
        batteryInfoProfile = new BatteryInfoProfile<>(this);
        batteryInfoProfile.addListener(mListener);
        addSupportedProfile(deviceInfoProfile);
        addSupportedProfile(batteryInfoProfile);
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));
        builder.notify(getCharacteristic(DaFitConstants.UUID_CHARACTERISTIC_DATA_IN), true);
        deviceInfoProfile.requestDeviceInfo(builder);
        batteryInfoProfile.requestBatteryInfo(builder);
        batteryInfoProfile.enableNotify(builder);
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()));

        return builder;
    }

    private BluetoothGattCharacteristic getTargetCharacteristicForPacketType(byte packetType)
    {
        if (packetType == 1)
            return getCharacteristic(DaFitConstants.UUID_CHARACTERISTIC_DATA_SPECIAL_1);
        else if (packetType == 2)
            return getCharacteristic(DaFitConstants.UUID_CHARACTERISTIC_DATA_SPECIAL_2);
        else
            return getCharacteristic(DaFitConstants.UUID_CHARACTERISTIC_DATA_OUT);
    }

    public void sendPacket(TransactionBuilder builder, byte[] packet)
    {
        DaFitPacketOut packetOut = new DaFitPacketOut(packet);

        byte[] fragment = new byte[MTU];
        while(packetOut.getFragment(fragment))
        {
            builder.write(getTargetCharacteristicForPacketType(packet[4]), fragment.clone());
        }
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        UUID charUuid = characteristic.getUuid();
        if (charUuid.equals(DaFitConstants.UUID_CHARACTERISTIC_DATA_IN))
        {
            if (packetIn.putFragment(characteristic.getValue())) {
                Pair<Byte, byte[]> packet = DaFitPacketIn.parsePacket(packetIn.getPacket());
                packetIn = new DaFitPacketIn();
                if (packet != null) {
                    byte packetType = packet.first;
                    byte[] payload = packet.second;

                    Log.i("AAAAAAAAAAAAAAAA", "Response for: " + packetType);

                    if (handlePacket(packetType, payload))
                        return true;
                }
            }
        }

        return super.onCharacteristicChanged(gatt, characteristic);
    }

    private boolean handlePacket(byte packetType, byte[] payload)
    {
        LOG.warn("Got packet " + packetType + ": " + Logging.formatBytes(payload));
        return false;
    }

    private void handleDeviceInfo(DeviceInfo info) {
        LOG.warn("Device info: " + info);
        versionCmd.hwVersion = info.getHardwareRevision();
        versionCmd.fwVersion = info.getSoftwareRevision();
        handleGBDeviceEvent(versionCmd);
    }

    private void handleBatteryInfo(BatteryInfo info) {
        LOG.warn("Battery info: " + info);
        batteryCmd.level = (short) info.getPercentCharged();
        handleGBDeviceEvent(batteryCmd);
    }

    @Override
    public boolean useAutoConnect() {
        return true;
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        // TODO
    }

    @Override
    public void onDeleteNotification(int id) {
        // not supported :(
    }

    @Override
    public void onSetCallState(CallSpec callSpec) {
        // TODO
    }

    @Override
    public void onSetCannedMessages(CannedMessagesSpec cannedMessagesSpec) {
        // not supported :(
    }

    @Override
    public void onSetTime() {
        // TODO
    }

    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {
        // TODO: set alarms
    }

    @Override
    public void onSetMusicState(MusicStateSpec stateSpec) {
        // not supported :(
    }

    @Override
    public void onSetMusicInfo(MusicSpec musicSpec) {
        // not supported :(
    }

    @Override
    public void onInstallApp(Uri uri) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onAppInfoReq() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onAppStart(UUID uuid, boolean start) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onAppDelete(UUID uuid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onAppConfiguration(UUID appUuid, String config, Integer id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onAppReorder(UUID[] uuids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onFetchRecordedData(int dataTypes) {
        // TODO
    }

    @Override
    public void onReset(int flags) {
        // TODO: this shuts down the watch, rather than rebooting it - perhaps add a new operation type?
        // (reboot is not supported, btw)

        try {
            TransactionBuilder builder = performInitialized("shutdown");
            sendPacket(builder, DaFitPacketOut.buildPacket(DaFitConstants.CMD_SHUTDOWN, new byte[] { -1 }));
            builder.queue(getQueue());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onHeartRateTest() {
        // TODO
    }

    // TODO: starting other tests

    @Override
    public void onEnableRealtimeSteps(boolean enable) {
        // TODO
    }

    @Override
    public void onEnableRealtimeHeartRateMeasurement(boolean enable) {
        // TODO
    }

    @Override
    public void onSetHeartRateMeasurementInterval(int seconds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onEnableHeartRateSleepSupport(boolean enable) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onFindDevice(boolean start) {
        if (start)
        {
            try {
                TransactionBuilder builder = performInitialized("onFindDevice");
                sendPacket(builder, DaFitPacketOut.buildPacket(DaFitConstants.CMD_FIND_MY_WATCH, new byte[0]));
                builder.queue(getQueue());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else
        {
            // Not supported - the device vibrates three times and then stops automatically
        }
    }

    @Override
    public void onSetConstantVibration(int integer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onScreenshotReq() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onAddCalendarEvent(CalendarEventSpec calendarEventSpec) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onDeleteCalendarEvent(byte type, long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onSendConfiguration(String config) {
        // TODO
    }

    @Override
    public void onReadConfiguration(String config) {
        // TODO
    }

    @Override
    public void onTestNewFunction() {
    }

    @Override
    public void onSendWeather(WeatherSpec weatherSpec) {
        // TODO
    }

    @Override
    public void onSetFmFrequency(float frequency) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onSetLedColor(int color) {
        throw new UnsupportedOperationException();
    }
}
