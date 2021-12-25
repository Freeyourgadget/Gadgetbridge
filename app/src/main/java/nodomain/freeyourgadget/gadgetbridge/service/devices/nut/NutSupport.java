/*  Copyright (C) 2020-2021 Taavi Eomäe

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.nut;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.nut.NutConstants;
import nodomain.freeyourgadget.gadgetbridge.devices.nut.NutKey;
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
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.IntentListener;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.battery.BatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.battery.BatteryInfoProfile;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfo;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfoProfile;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class NutSupport extends AbstractBTLEDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(NutSupport.class);

    private final GBDeviceEventBatteryInfo batteryCmd = new GBDeviceEventBatteryInfo();

    private final DeviceInfoProfile<NutSupport> deviceInfoProfile;
    private final BatteryInfoProfile<NutSupport> batteryInfoProfile;
    private final IntentListener listener = new IntentListener() {
        @Override
        public void notify(Intent intent) {
            String action = intent.getAction();
            if (action.equals(DeviceInfoProfile.ACTION_DEVICE_INFO)) {
                handleDeviceInfo((DeviceInfo) intent.getParcelableExtra(DeviceInfoProfile.EXTRA_DEVICE_INFO));
            } else if (action.equals(BatteryInfoProfile.ACTION_BATTERY_INFO)) {
                handleBatteryInfo((BatteryInfo) intent.getParcelableExtra(BatteryInfoProfile.EXTRA_BATTERY_INFO));
            } else {
                LOG.warn("Unhandled intent given to listener");
            }
        }
    };
    private SharedPreferences prefs = null;
    /**
     * It uses the proprietary Nut interface.
     */
    private boolean proprietary = false;
    /**
     * Proprietary Nut interface needs authentication.
     * <p>
     * Don't write characteristics until authenticated.
     * <p>
     * Will disconnect in a minute if you don't authenticate.
     */
    private boolean authenticated = true;
    /**
     * The two keys used for authentication
     */
    private BigInteger key1;
    private BigInteger key2;


    public NutSupport() {
        super(LOG);
        addSupportedService(NutConstants.SERVICE_BATTERY);
        addSupportedService(NutConstants.SERVICE_DEVICE_INFO);
        addSupportedService(NutConstants.SERVICE_IMMEDIATE_ALERT);
        addSupportedService(NutConstants.SERVICE_LINK_LOSS);
        addSupportedService(NutConstants.SERVICE_PROPRIETARY_NUT);
        addSupportedService(NutConstants.SERVICE_UNKNOWN_2);

        deviceInfoProfile = new DeviceInfoProfile<>(this);
        deviceInfoProfile.addListener(listener);
        addSupportedProfile(deviceInfoProfile);

        batteryInfoProfile = new BatteryInfoProfile<>(this);
        batteryInfoProfile.addListener(listener);
        addSupportedProfile(batteryInfoProfile);
    }

    private void handleBatteryInfo(BatteryInfo info) {
        LOG.info("Received Nut battery info");
        batteryCmd.level = (short) info.getPercentCharged();
        handleGBDeviceEvent(batteryCmd);
    }

    private void handleDeviceInfo(DeviceInfo info) {
        LOG.info("Received Nut device info");
        LOG.info(String.valueOf(info));
        GBDeviceEventVersionInfo versionInfo = new GBDeviceEventVersionInfo();
        if (info.getHardwareRevision() != null) {
            versionInfo.hwVersion = info.getHardwareRevision();
        }
        if (info.getFirmwareRevision() != null) {
            versionInfo.fwVersion = info.getFirmwareRevision();
        }

        handleGBDeviceEvent(versionInfo);
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()));

        // Init prefs
        prefs = GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress());
        loadKeysFromPrefs();

        LOG.debug("Requesting device info!");
        deviceInfoProfile.requestDeviceInfo(builder);
        batteryInfoProfile.requestBatteryInfo(builder);

        // If this characteristic exists, it has proprietary Nut interface
        this.proprietary = (getCharacteristic(NutConstants.CHARAC_AUTH_STATUS) != null);

        if (proprietary) {
            this.authenticated = false;
            /**
             * Part of {@link NutConstants.SERVICE_PROPRIETARY_NUT}
             * Enables proprietary notification
             */
            builder.notify(getCharacteristic(NutConstants.CHARAC_AUTH_STATUS), true);
            LOG.info("Enabled authentication status notify");

            /**
             * Part of {@link NutConstants.SERVICE_UNKNOWN_2}
             * Enables button-press notify
             */
            builder.notify(getCharacteristic(NutConstants.CHARAC_UNKNOWN_2), true);
        } else {
            /**
             * Part of {@link NutConstants.SERVICE_UNKNOWN_1_WEIRDNESS}
             * Enables button-press notify
             */
            builder.notify(getCharacteristic(NutConstants.CHARAC_CHANGE_POWER), true);
        }

        readDeviceInfo();
        return builder;
    }

    @Override
    public boolean useAutoConnect() {
        return true;
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {

    }

    @Override
    public void onDeleteNotification(int id) {

    }

    @Override
    public void onSetTime() {

    }

    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {

    }

    @Override
    public void onSetCallState(CallSpec callSpec) {

    }

    @Override
    public void onSetCannedMessages(CannedMessagesSpec cannedMessagesSpec) {

    }

    @Override
    public void onSetMusicState(MusicStateSpec stateSpec) {

    }

    @Override
    public void onSetMusicInfo(MusicSpec musicSpec) {

    }

    @Override
    public void onEnableRealtimeSteps(boolean enable) {

    }

    @Override
    public void onInstallApp(Uri uri) {

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
    public void onFindDevice(boolean enable) {
        deviceImmediateAlert(enable);
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
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic) {
        if (super.onCharacteristicChanged(gatt, characteristic)) {
            return true;
        }

        UUID characteristicUUID = characteristic.getUuid();
        if (characteristicUUID.equals(NutConstants.CHARAC_AUTH_STATUS)) {
            handleAuthResult(characteristic.getValue());
            return true;
        }
        LOG.info("Unhandled characteristic changed: " + characteristicUUID);
        return false;
    }

    @Override
    public boolean onCharacteristicRead(BluetoothGatt gatt,
                                        BluetoothGattCharacteristic characteristic,
                                        int status) {
        if (super.onCharacteristicRead(gatt, characteristic, status)) {
            return true;
        }
        UUID characteristicUUID = characteristic.getUuid();

        if (characteristicUUID.equals(NutConstants.CHARAC_SYSTEM_ID)) {
            // TODO: Handle System ID read
            return true;
        }
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
    public void onSendWeather(WeatherSpec weatherSpec) {

    }

    /**
     * Enables or disables link loss alert
     */
    private void deviceLinkLossAlert(boolean enable) {
        UUID charac;
        if (this.proprietary) {
            /** Part of {@link NutConstants.SERVICE_PROPRIETARY_NUT} */
            charac = NutConstants.CHARAC_CHANGE_POWER;
        } else {
            /** Part of {@link NutConstants.SERVICE_IMMEDIATE_ALERT} */
            charac = NutConstants.CHARAC_LINK_LOSS_ALERT_LEVEL;
        }

        byte[] payload = new byte[]{(byte) (enable ? 0x00 : 0x01)};
        if (enable) {
            writeCharacteristic("Enable link loss alert", charac, payload);
        } else {
            writeCharacteristic("Disable link loss alert", charac, payload);
        }
    }

    /**
     * Should trigger an immediate alert
     *
     * @param enable turn on or not
     */
    private void deviceImmediateAlert(boolean enable) {
        UUID charac;
        if (this.proprietary) {
            /** Part of {@link NutConstants.SERVICE_IMMEDIATE_ALERT} */
            charac = NutConstants.CHARAC_LINK_LOSS_ALERT_LEVEL;
            if (!authenticated) {
                LOG.warn("Not authenticated, can't alert");
                return;
            }
        } else {
            /** Part of {@link NutConstants.SERVICE_PROPRIETARY_NUT} */
            charac = NutConstants.CHARAC_CHANGE_POWER;
        }

        if (enable) {
            writeCharacteristic("Start alert", charac, new byte[]{(byte) 0x04});
        } else {
            writeCharacteristic("Stop alert", charac, new byte[]{(byte) 0x03});
        }
    }

    /**
     * This will write a new key to the device
     * <p>
     * However, <b>it is irreversible</b>,
     * if you can't generate the right packets,
     * the device is basically bricked!
     * <p>
     * If you can generate the correct packets,
     * it can be reset... somehow
     *
     * @param key key
     */
    private void deviceWriteNewKey(byte[] key) {
        // TODO: Determine each nuance of how this
        //  works before using it!
        byte[] result_payload = new byte[key.length + 1];
        result_payload[0] = (byte) 0x04;
        System.arraycopy(key, 0, result_payload, 1, key.length);

        writeCharacteristic("Write new key",
                NutConstants.CHARAC_DFU_PW,
                result_payload);
    }

    /**
     * Turns the device off
     */
    private void deviceShutdown() {
        writeCharacteristic("Shutdown", NutConstants.CHARAC_CHANGE_POWER, new byte[]{0x06});
    }

    /**
     * Switches the device to Nordic's DFU mode
     */
    private void deviceDFU() {
        writeCharacteristic("Enable DFU mode", NutConstants.CHARAC_DFU_PW, new byte[]{0x14});
    }

    /**
     * Specifies how long the alert lasts
     *
     * @param duration in seconds (I think)
     */
    private void deviceWriteAlertDuration(int duration) {
        if (duration == 0) {
            duration = 15;
        }

        UUID charac;
        if (this.proprietary) {
            charac = NutConstants.CHARAC_DFU_PW;
        } else {
            charac = NutConstants.CHARAC_CHANGE_POWER;
        }

        writeCharacteristic("Write alert duration",
                charac,
                new byte[]{37, (byte) duration});
    }

    private void readHardwareInfo() {
        BluetoothGattCharacteristic characteristic = getCharacteristic(NutConstants.CHARAC_HARDWARE_VERSION);
        if (characteristic != null &&
                ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) > 0)) {
            readCharacteristic("Device read hardware",
                    NutConstants.CHARAC_HARDWARE_VERSION);
        }

        BluetoothGattCharacteristic characteristic1 = getCharacteristic(NutConstants.CHARAC_MANUFACTURER_NAME);
        if (characteristic1 != null &&
                (characteristic1.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
            readCharacteristic("Read manufacturer",
                    NutConstants.CHARAC_MANUFACTURER_NAME);
        }
    }

    private void readFirmwareInfo() {
        BluetoothGattCharacteristic characteristic = getCharacteristic(NutConstants.CHARAC_FIRMWARE_VERSION);
        if (characteristic != null &&
                (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
            readCharacteristic("Read firmware version",
                    NutConstants.CHARAC_FIRMWARE_VERSION);
        }
    }

    private void readBatteryInfo() {
        BluetoothGattCharacteristic characteristic = getCharacteristic(NutConstants.CHARAC_BATTERY_INFO);
        if (characteristic != null &&
                (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
            readCharacteristic("Read battery info",
                    NutConstants.CHARAC_BATTERY_INFO);
        }
    }

    /**
     * Loads the three keys from device-specific shared preferences
     */
    private void loadKeysFromPrefs() {
        if (prefs != null) {
            LOG.info("Reading keys");
            key1 = new BigInteger(prefs.getString("nut_packet_key_1", "0"));
            key2 = new BigInteger(prefs.getString("nut_packet_key_2", "0"));
            if (key1.equals(BigInteger.ZERO) || key2.equals(BigInteger.ZERO)) {
                byte[] challenge = NutKey.hexStringToByteArrayNut(prefs.getString("nut_packet_challenge", "00"));
                byte[] response = NutKey.hexStringToByteArrayNut(prefs.getString("nut_response_challenge", "00"));
                if (Arrays.equals(challenge, new byte[]{0x00}) ||
                        Arrays.equals(response, new byte[]{0x00})) {
                    GB.toast("No key available for the device", Toast.LENGTH_LONG, GB.ERROR);
                    return;
                }
                Map.Entry<BigInteger, BigInteger> key = NutKey.reversePasswordGeneration(
                        challenge,
                        response,
                        gbDevice.getAddress()
                );
                if (key == null) {
                    GB.toast("No correct key available for the device", Toast.LENGTH_LONG, GB.ERROR);
                    return;
                }
                key1 = key.getKey();
                key2 = key.getValue();
                LOG.debug("Key was extracted from challenge-response packets");
            } else {
                LOG.debug("Key was preset");
            }
        }
    }

    /**
     * Processes the authentication flow of the proprietary Nut protocol
     * See more: {@link NutConstants#SERVICE_PROPRIETARY_NUT}
     *
     * @param received the notify characteristic's content
     */
    public final void handleAuthResult(byte[] received) {
        if (received != null && received.length != 0) {
            if (received[0] == 0x01) {
                // Password is needed
                // Preamble, counter, rotating key, static key
                byte[] payload = new byte[1 + 1 + 3 + 12];

                // This is a response to the challenge
                payload[0] = 0x02;

                // Modify the challenge
                byte[] response = NutKey.passwordGeneration(gbDevice.getAddress(), received, key1, key2);
                System.arraycopy(response, 0, payload, 1, response.length);

                writeCharacteristic("Authentication",
                        NutConstants.CHARAC_DFU_PW,
                        payload
                );
                LOG.debug("Successfully sent auth");
            } else if (received[0] == 0x03) {
                if (received[1] == 0x55) {
                    LOG.debug("Successful password attempt or uninitialized");
                    authenticated = true;
                    initChara();
                } else {
                    LOG.debug("Error authenticating");
                    // TODO: Disconnect
                }
            } else if (received[0] == 0x05) {
                LOG.debug("Password has been set");
            } else {
                LOG.debug("Invalid packet");
                // TODO: Disconnect
            }
        }
    }

    /**
     * Initializes required characteristics
     */
    private void initChara() {
        if (proprietary) {
            writeCharacteristic("Init alert 1", NutConstants.CHARAC_LINK_LOSS_ALERT_LEVEL, new byte[]{(byte) 0x00});
            writeCharacteristic("Init alert 2", NutConstants.CHARAC_LINK_LOSS_ALERT_LEVEL, new byte[]{(byte) 0x00});
            writeCharacteristic("Init alert 3", NutConstants.CHARAC_LINK_LOSS_ALERT_LEVEL, new byte[]{(byte) 0x00});
        }
    }

    /**
     * Initiates a read of all the device information characteristics
     */
    private void readDeviceInfo() {
        readBatteryInfo();
        readHardwareInfo();
        readFirmwareInfo();
    }

    /**
     * Just wraps writing into a neat little function
     *
     * @param taskName something that describes the task a bit
     * @param charac   the characteristic to write
     * @param data     the data to write
     */
    private void writeCharacteristic(String taskName, UUID charac, byte[] data) {
        BluetoothGattCharacteristic characteristic = getCharacteristic(charac);

        TransactionBuilder builder = new TransactionBuilder(taskName);
        builder.write(characteristic, data);
        builder.queue(getQueue());
    }

    /**
     * Just wraps reading into a neat little function
     *
     * @param taskName something that describes the task a bit
     * @param charac   the characteristic to read
     */
    private void readCharacteristic(String taskName, UUID charac) {
        BluetoothGattCharacteristic characteristic = getCharacteristic(charac);

        TransactionBuilder builder = new TransactionBuilder(taskName);
        builder.read(characteristic);
        builder.queue(getQueue());
    }
}
