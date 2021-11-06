package nodomain.freeyourgadget.gadgetbridge.service.devices.galaxy_buds;

import static nodomain.freeyourgadget.gadgetbridge.util.CheckSums.crc16_ccitt;
import static nodomain.freeyourgadget.gadgetbridge.util.GB.hexdump;

import android.content.SharedPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;

public class GalaxyBudsProtocol extends GBDeviceProtocol {
    private static final Logger LOG = LoggerFactory.getLogger(GalaxyBudsProtocol.class);

    final UUID UUID_DEVICE_CTRL = UUID.fromString("00001102-0000-1000-8000-00805f9b34fd");
    private static final byte SOM = (byte) 0xFE;
    private static final byte EOM = (byte) 0xEE;
    private boolean isFirstExchange = true;

    //incoming
    private static final byte battery_status = (byte) 0x60;
    private static final byte battery_status2 = (byte) 0x61;

    //outgoing
    private static final byte find_device_start = (byte) 0xa0;
    private static final byte find_device_stop = (byte) 0xa1;

    private static final byte set_ambient_mode = (byte) 0x80; //0x0/0x1
    private static final byte set_ambient_volume = (byte) 0x84; // 0x1-0x5
    private static final byte set_ambient_voice_focus = (byte) 0x85; // 0x0/0x1

    private static final byte set_lock_touch = (byte) 0x90; // 0x0/0x1
    private static final byte set_game_mode = (byte) 0x87; // 0x0/0x2 no idea if this is doing anything
    private static final byte set_equalizer = (byte) 0x86; // 0x0/0x1

    private static final byte set_reset = (byte) 0x50;

    private static final byte set_touchpad_options = (byte) 0x92;

    private static final byte get_debug_build_info = (byte) 0x28;
    private static final byte get_serial_number = (byte) 0x29;
    private static final byte get_debug_get_all_data = (byte) 0x26;
    private static final byte get_debug_get_version = (byte) 0x24;

    @Override
    public GBDeviceEvent[] decodeResponse(byte[] responseData) {
        List<GBDeviceEvent> devEvts = new ArrayList<>();
        LOG.debug("received data: " + hexdump(responseData));
        LOG.debug("received data length: " + responseData.length);

        if (isFirstExchange) {
            isFirstExchange = false;
            devEvts.add(new GBDeviceEventVersionInfo()); //TODO: this is a weird hack to make the DBHelper happy. Replace with proper + detection
        }

        ByteBuffer incoming = ByteBuffer.wrap(responseData);
        incoming.order(ByteOrder.LITTLE_ENDIAN);

        byte sof = incoming.get();
        if (sof != SOM) {
            LOG.error("Error in message, wrong start of frame: " + hexdump(responseData));
            return null;
        }
        byte type = incoming.get();
        int length = (int) (incoming.get() & 0xff);
        byte message_id = incoming.get();
        byte[] payload;
        try {
            payload = Arrays.copyOfRange(responseData, incoming.position(), incoming.position() + length);
        } catch (Exception e) {
            LOG.error("Error getting payload data: " + length + " , " + e);
            return null;
        }

        switch (message_id) {
            case battery_status:
                devEvts.addAll(handleBatteryInfo(Arrays.copyOfRange(payload, 1, 3)));
                break;
            case battery_status2:
                devEvts.addAll(handleBatteryInfo(Arrays.copyOfRange(payload, 2, 4)));
                break;
            default:
                LOG.debug("Unhandled: " + hexdump(responseData));

        }
        return devEvts.toArray(new GBDeviceEvent[devEvts.size()]);
    }


    byte[] encodeMessage(byte command) {
        ByteBuffer msgBuf = ByteBuffer.allocate(7);
        msgBuf.order(ByteOrder.LITTLE_ENDIAN);
        msgBuf.put(SOM);
        msgBuf.put((byte) 0x0); //0x0 for sending
        msgBuf.put((byte) 0x3); //size
        msgBuf.put((byte) command); //command id
        msgBuf.putShort((short) crc16_ccitt(new byte[]{command}));
        msgBuf.put(EOM);
        LOG.debug("DEBUG: " + hexdump(msgBuf.array()));
        return msgBuf.array();
    }

    byte[] encodeMessage(byte command, byte parameter) {
        ByteBuffer msgBuf = ByteBuffer.allocate(8);
        msgBuf.order(ByteOrder.LITTLE_ENDIAN);
        msgBuf.put(SOM);
        msgBuf.put((byte) 0x0); //0x0 for sending
        msgBuf.put((byte) 0x4); //size
        msgBuf.put((byte) command); //command id
        msgBuf.put((byte) parameter);
        msgBuf.putShort((short) crc16_ccitt(new byte[]{command, parameter}));
        msgBuf.put(EOM);
        LOG.debug("DEBUG: " + hexdump(msgBuf.array()));
        return msgBuf.array();
    }

    byte[] encodeMessage(byte command, byte parameter, byte value) {
        ByteBuffer msgBuf = ByteBuffer.allocate(9);
        msgBuf.order(ByteOrder.LITTLE_ENDIAN);
        msgBuf.put(SOM);
        msgBuf.put((byte) 0x0); //0x0 for sending
        msgBuf.put((byte) 0x5); //size
        msgBuf.put((byte) command);
        msgBuf.put((byte) parameter);
        msgBuf.put((byte) value);
        msgBuf.putShort((short) crc16_ccitt(new byte[]{command, parameter, value}));
        msgBuf.put(EOM);
        LOG.debug("DEBUG: " + hexdump(msgBuf.array()));
        return msgBuf.array();
    }

    @Override
    public byte[] encodeFindDevice(boolean start) {
        byte command = (byte) (start ? find_device_start : find_device_stop);
        return encodeMessage(command);
    }

    @Override
    public byte[] encodeReset(int reset) {
        if (reset == RESET_FLAGS_FACTORY_RESET) {
            return encodeMessage(set_reset);
        }
        return null;
    }

    @Override
    public byte[] encodeTestNewFunction() {
        //return encodeMessage(get_debug_build_info);
        return null;
    }

    @Override
    public byte[] encodeSendConfiguration(String config) {

        SharedPreferences prefs = GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress());

        switch (config) {
            case DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_AMBIENT_MODE:
                byte enable_ambient = (byte) (prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_AMBIENT_MODE, false) ? 0x01 : 0x00);
                return encodeMessage(set_ambient_mode, enable_ambient);
            case DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_AMBIENT_VOICE_FOCUS:
                byte enable_voice = (byte) (prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_AMBIENT_VOICE_FOCUS, false) ? 0x01 : 0x00);
                return encodeMessage(set_ambient_voice_focus, enable_voice);
            case DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_AMBIENT_VOLUME:
                int ambient_volume = prefs.getInt(DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_AMBIENT_VOLUME, 0);
                byte ambient_volume_byte = (byte) (ambient_volume + 1); //seek bar is 0-4, we need 1-5
                return encodeMessage(set_ambient_volume, ambient_volume_byte);
            case DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_LOCK_TOUCH:
                byte set_lock = (byte) (prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_LOCK_TOUCH, false) ? 0x01 : 0x00);
                return encodeMessage(set_lock_touch, set_lock);
            case DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_GAME_MODE:
                byte game_mode = (byte) (prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_GAME_MODE, false) ? 0x2 : 0x00);
                return encodeMessage(set_game_mode, game_mode);
            case DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_EQUALIZER:
            case DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_EQUALIZER_DOLBY:
            case DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_EQUALIZER_MODE:
                byte equalizer = (byte) (prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_EQUALIZER, false) ? 0x1 : 0x00);
                boolean equalizer_dolby = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_EQUALIZER_DOLBY, false);
                int dolby = 0;
                if (equalizer_dolby) {
                    dolby = 5;
                }
                String equalizer_mode = prefs.getString(DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_EQUALIZER_MODE, "0");
                byte mode = (byte) (Integer.parseInt(equalizer_mode) + dolby);
                return encodeMessage(set_equalizer, equalizer, mode);

            case DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_TOUCH_LEFT:
            case DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_TOUCH_RIGHT:
                String touch_left = prefs.getString(DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_TOUCH_LEFT, "1");
                String touch_right = prefs.getString(DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_TOUCH_RIGHT, "1");
                byte touchmode_left = (byte) Integer.parseInt(touch_left);
                byte touchmode_right = (byte) Integer.parseInt(touch_right);
                return encodeMessage(set_touchpad_options, touchmode_left, touchmode_right);

            default:
                LOG.debug("CONFIG: " + config);
        }
        return super.encodeSendConfiguration(config);
    }


    private List<GBDeviceEvent> handleBatteryInfo(byte[] payload) {
        List<GBDeviceEvent> deviceEvents = new ArrayList<>();
        LOG.debug("Battery payload: " + hexdump(payload));
        LOG.debug("pl: " + payload.length);
        LOG.debug("p0: " + payload[0]);
        LOG.debug("p1: " + payload[1]);

        int batteryLevel1 = payload[0];
        int batteryLevel2 = payload[1];

        GBDeviceEventBatteryInfo evBattery1 = new GBDeviceEventBatteryInfo();
        evBattery1.batteryIndex = 0;
        evBattery1.level = GBDevice.BATTERY_UNKNOWN;
        evBattery1.level = (batteryLevel1 > 0) ? batteryLevel1 : GBDevice.BATTERY_UNKNOWN;
        evBattery1.state = (batteryLevel1 > 0) ? BatteryState.BATTERY_NORMAL : BatteryState.UNKNOWN;
        deviceEvents.add(evBattery1);

        GBDeviceEventBatteryInfo evBattery2 = new GBDeviceEventBatteryInfo();
        evBattery2.batteryIndex = 1;
        evBattery2.level = GBDevice.BATTERY_UNKNOWN;
        evBattery2.level = (batteryLevel2 > 0) ? batteryLevel2 : GBDevice.BATTERY_UNKNOWN;
        evBattery2.state = (batteryLevel2 > 0) ? BatteryState.BATTERY_NORMAL : BatteryState.UNKNOWN;
        deviceEvents.add(evBattery2);

        return deviceEvents;
    }

    protected GalaxyBudsProtocol(GBDevice device) {
        super(device);

    }
}
