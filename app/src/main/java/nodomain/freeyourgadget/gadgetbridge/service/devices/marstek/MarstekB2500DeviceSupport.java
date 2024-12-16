package nodomain.freeyourgadget.gadgetbridge.service.devices.marstek;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_BATTERY_ALLOW_PASS_THOUGH;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_BATTERY_DISCHARGE_INTERVALS_SET;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_BATTERY_DISCHARGE_MANAUAL;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_BATTERY_MINIMUM_CHARGE;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.SharedPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.SimpleTimeZone;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;


public class MarstekB2500DeviceSupport extends AbstractBTLEDeviceSupport {
    public static final UUID UUID_CHARACTERISTIC_MAIN = UUID.fromString("0000ff02-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_SERVICE_MAIN = UUID.fromString("0000ff00-0000-1000-8000-00805f9b34fb");

    private static final byte COMMAND_PREFIX = 0x73;
    private static final byte COMMAND = 0x23;
    private static final byte OPCODE_REBOOT = 0x25;
    private static final byte OPCODE_INFO1 = 0x03;
    private static final byte OPCODE_INFO2 = 0x13;

    // the following already have checksums precalculated (last byte)
    private static final byte[] COMMAND_GET_INFOS1 = new byte[]{COMMAND_PREFIX, 0x06, COMMAND, OPCODE_INFO1, 0x01, 0x54};
    private static final byte[] COMMAND_GET_INFOS2 = new byte[]{COMMAND_PREFIX, 0x06, COMMAND, OPCODE_INFO2, 0x00, 0x45};
    private static final byte[] COMMAND_REBOOT = new byte[]{COMMAND_PREFIX, 0x06, COMMAND, OPCODE_REBOOT, 0x01, 0x72};
    private static final byte[] COMMAND_SET_AUTO_DISCHARGE = new byte[]{COMMAND_PREFIX, 0x06, COMMAND, 0x11, 0x00, 0x47};
    private static final byte[] COMMAND_SET_POWERMETER_CHANNEL1 = new byte[]{COMMAND_PREFIX, 0x06, COMMAND, 0x2a, 0x00, 0x7c};
    private static final byte[] COMMAND_SET_BATTERY_ALLOW_PASS_THOUGH = new byte[]{COMMAND_PREFIX, 0x06, COMMAND, 0x0d, 0x00, 0x5b};
    private static final byte[] COMMAND_SET_BATTERY_DISALLOW_PASS_THOUGH = new byte[]{COMMAND_PREFIX, 0x06, COMMAND, 0x0d, 0x01, 0x5a};


    private static final Logger LOG = LoggerFactory.getLogger(MarstekB2500DeviceSupport.class);
    private int firmwareVersion;

    public MarstekB2500DeviceSupport() {
        super(LOG);
        addSupportedService(UUID_SERVICE_MAIN);
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);

        UUID characteristicUUID = characteristic.getUuid();
        byte[] value = characteristic.getValue();

        LOG.info("Characteristic changed UUID: {}", characteristicUUID);
        LOG.info("Characteristic changed value: {}", StringUtils.bytesToHex(value));

        if (value[0] == COMMAND_PREFIX) {
            if ((value[1] == 0x10) && (value[2] == COMMAND) && (value[3] == OPCODE_INFO1)) {
                decodeInfos(value);
                return true;
            } else if ((value[1] == 0x3a || value[1] == 0x22) && value[2] == COMMAND && value[3] == OPCODE_INFO2) {
                decodeDischargeIntervalsToPreferences(value);
                return true;
            }
        }
        return false;
    }


    @Override
    public void onTestNewFunction() {
        sendCommand("get infos 1", COMMAND_GET_INFOS1);
        sendCommand("get infos 2", COMMAND_GET_INFOS2);
    }

    @Override
    public void onReset(int flags) {
        if ((flags & GBDeviceProtocol.RESET_FLAGS_REBOOT) != 0) {
            sendCommand("reboot", COMMAND_REBOOT);
        }
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));
        getDevice().setFirmwareVersion("N/A");
        getDevice().setFirmwareVersion2("N/A");
        builder.requestMtu(512);
        builder.notify(getCharacteristic(UUID_CHARACTERISTIC_MAIN), true);
        builder.wait(3500);
        builder.write(getCharacteristic(UUID_CHARACTERISTIC_MAIN), COMMAND_GET_INFOS1);
        builder.wait(750);
        builder.write(getCharacteristic(UUID_CHARACTERISTIC_MAIN), COMMAND_GET_INFOS2);
        builder.wait(750);
        builder.write(getCharacteristic(UUID_CHARACTERISTIC_MAIN), encodeSetCurrentTime());
        builder.wait(750);
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()));
        return builder;
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    private void sendCommand(String taskName, byte[] contents) {
        TransactionBuilder builder = new TransactionBuilder(taskName);
        BluetoothGattCharacteristic characteristic = getCharacteristic(UUID_CHARACTERISTIC_MAIN);
        if (characteristic != null && contents != null) {
            builder.write(characteristic, contents);
            builder.wait(750);
            builder.queue(getQueue());
        }
    }

    @Override
    public void onSetTime() {
        sendCommand("set time", encodeSetCurrentTime());
    }

    @Override
    public void onSendConfiguration(final String config) {
        Prefs devicePrefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress()));
        switch (config) {
            case PREF_BATTERY_DISCHARGE_INTERVALS_SET:
                if (devicePrefs.getBoolean(PREF_BATTERY_DISCHARGE_MANAUAL, true)) {
                    sendCommand("set discharge intervals", encodeDischargeIntervalsFromPreferences());
                } else {
                    sendCommand("set dynamic discharge", COMMAND_SET_AUTO_DISCHARGE);
                    sendCommand("set channel auto", COMMAND_SET_POWERMETER_CHANNEL1);
                }
                return;
            case PREF_BATTERY_MINIMUM_CHARGE:
                sendCommand("set minimum charge", encodeMinimumChargeFromPreferences());
                return;
            case PREF_BATTERY_ALLOW_PASS_THOUGH:
                if (devicePrefs.getBoolean(PREF_BATTERY_ALLOW_PASS_THOUGH, true)) {
                    sendCommand("set allow pass-though", COMMAND_SET_BATTERY_ALLOW_PASS_THOUGH);
                } else {
                    sendCommand("set disallow pass-though", COMMAND_SET_BATTERY_DISALLOW_PASS_THOUGH);
                }
                return;
        }

        LOG.warn("Unknown config changed: {}", config);
    }

    private void decodeInfos(byte[] value) {
        Prefs devicePrefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress()));
        SharedPreferences.Editor devicePrefsEdit = devicePrefs.getPreferences().edit();
        ByteBuffer buf = ByteBuffer.wrap(value);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.position(12); // skip header and unknown
        firmwareVersion = buf.get();
        boolean battery_allow_passthough = buf.get() != 0x01;
        boolean manual_discharge_intervals = buf.get() != 0x01;
        buf.position(buf.position() + 3); // skip unknown
        byte battery_max_use = buf.get();
        short battery_current_discharge = buf.getShort();
        buf.position(buf.position() + 1); // skip unknown
        short battery_charge = buf.getShort();

        getDevice().setFirmwareVersion("V" + (firmwareVersion & 0xff));
        getDevice().sendDeviceUpdateIntent(getContext());

        int battery_minimum_charge = 100 - battery_max_use;

        devicePrefsEdit.putString(PREF_BATTERY_MINIMUM_CHARGE, String.valueOf(battery_minimum_charge));
        devicePrefsEdit.putBoolean(PREF_BATTERY_DISCHARGE_MANAUAL, manual_discharge_intervals);
        devicePrefsEdit.putBoolean(PREF_BATTERY_ALLOW_PASS_THOUGH, battery_allow_passthough);
        devicePrefsEdit.apply();
        devicePrefsEdit.commit();

        int battery_percentage = (int) Math.ceil((battery_charge / 2240.0f) * 100);
        getDevice().setBatteryLevel(battery_percentage);
        getDevice().sendDeviceUpdateIntent(getContext());
    }

    private void decodeDischargeIntervalsToPreferences(byte[] value) {
        Prefs devicePrefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress()));
        SharedPreferences.Editor devicePrefsEdit = devicePrefs.getPreferences().edit();
        ByteBuffer buf = ByteBuffer.wrap(value);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.position(5); // skip
        for (int i = 1; i <= 5; i++) {
            boolean enabled = buf.get() != 0x00;
            int startHour = buf.get();
            int startMinute = buf.get();
            int endHour = buf.get();
            int endMinute = buf.get();
            int watt = buf.getShort();
            devicePrefsEdit.putBoolean("battery_discharge_interval" + i + "_enabled", enabled);
            devicePrefsEdit.putString("battery_discharge_interval" + i + "_start", DateTimeUtils.formatTime(startHour, startMinute));
            devicePrefsEdit.putString("battery_discharge_interval" + i + "_end", DateTimeUtils.formatTime(endHour, endMinute));
            devicePrefsEdit.putString("battery_discharge_interval" + i + "_watt", String.valueOf(watt));

            if (i == 3) {
                if (value.length == 0x22) // old fw only seems to return 3 settings and has 7 trailing bytes
                    break;
                buf.position(buf.position() + 17); // skip 17 bytes, there is a hole with unknown data
            }
        }
        devicePrefsEdit.apply();
        devicePrefsEdit.commit();
    }

    private byte[] encodeMinimumChargeFromPreferences() {
        Prefs devicePrefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress()));
        int minimum_charge = devicePrefs.getInt(PREF_BATTERY_MINIMUM_CHARGE, 10);
        int maximum_use = 100 - minimum_charge;

        byte length = 6;
        ByteBuffer buf = ByteBuffer.allocate(length);
        buf.order(ByteOrder.LITTLE_ENDIAN);

        buf.put(COMMAND_PREFIX);
        buf.put(length);
        buf.put(COMMAND);
        buf.put((byte) 0x0b);
        buf.put((byte) maximum_use);
        buf.put(getXORChecksum(buf.array()));

        return buf.array();
    }

    private byte[] encodeSetCurrentTime() {
        long ts = System.currentTimeMillis();
        long ts_offset = (SimpleTimeZone.getDefault().getOffset(ts));

        byte length = 13;
        ByteBuffer buf = ByteBuffer.allocate(length);
        buf.order(ByteOrder.LITTLE_ENDIAN);

        buf.put(COMMAND_PREFIX);
        buf.put(length);
        buf.put(COMMAND);
        buf.put((byte) 0x14);

        final Calendar calendar = DateTimeUtils.getCalendarUTC();
        buf.put((byte) ((calendar.get(Calendar.YEAR) - 1900) & 0xff));
        buf.put((byte) calendar.get(Calendar.MONTH));
        buf.put((byte) calendar.get(Calendar.DAY_OF_MONTH));
        buf.put((byte) calendar.get(Calendar.HOUR_OF_DAY));
        buf.put((byte) calendar.get(Calendar.MINUTE));
        buf.put((byte) calendar.get(Calendar.SECOND));
        buf.putShort((short) (ts_offset / 60000));

        buf.put(getXORChecksum(buf.array()));
        return buf.array();
    }

    private byte[] encodeDischargeIntervalsFromPreferences() {
        Prefs devicePrefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress()));
        if (devicePrefs.getBoolean(PREF_BATTERY_DISCHARGE_MANAUAL, true)) {
            int nr_invervals = (firmwareVersion >= 220) ? 5 : 3; // old firmware V210 only had 3 intervals it seems, so set only 3
            int length = 5 + nr_invervals * 7;

            ByteBuffer buf = ByteBuffer.allocate(length);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            buf.put(COMMAND_PREFIX);
            buf.put((byte) length);
            buf.put(COMMAND); // set power parameters ?
            buf.put((byte) 0x12); // set discharge power timers ?
            for (int i = 1; i <= nr_invervals; i++) {
                boolean enabled = devicePrefs.getBoolean("battery_discharge_interval" + i + "_enabled", false);
                LocalTime startTime = devicePrefs.getLocalTime("battery_discharge_interval" + i + "_start", "00:00");
                LocalTime endTime = devicePrefs.getLocalTime("battery_discharge_interval" + i + "_end", "00:00");
                short watt = (short) devicePrefs.getInt("battery_discharge_interval" + i + "_watt", 80);
                buf.put((byte) (enabled ? 0x01 : 0x00));
                buf.put((byte) startTime.getHour());
                buf.put((byte) startTime.getMinute());
                buf.put((byte) endTime.getHour());
                buf.put((byte) endTime.getMinute());
                buf.putShort(watt);
            }
            buf.put(getXORChecksum(buf.array()));

            return buf.array();
        }
        return null;
    }

    private byte getXORChecksum(byte[] command) {
        byte checksum = 0;
        for (byte b : command) {
            checksum ^= b;
        }
        return checksum;
    }

}
