package nodomain.freeyourgadget.gadgetbridge.service.devices.soundcore.liberty4_nc;

import static nodomain.freeyourgadget.gadgetbridge.util.GB.hexdump;

import android.content.SharedPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.AmbientSoundControlButtonMode;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.soundcore.SoundcorePacket;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class SoundcoreLibertyProtocol extends GBDeviceProtocol {

    private static final Logger LOG = LoggerFactory.getLogger(SoundcoreLibertyProtocol.class);

    private static final int battery_case = 0;
    private static final int battery_earphone_left = 1;
    private static final int battery_earphone_right = 2;

    final UUID UUID_DEVICE_CTRL = UUID.fromString("0cf12d31-fac3-4553-bd80-d6832e7b3947");
    protected SoundcoreLibertyProtocol(GBDevice device) {
        super(device);
    }

    private GBDeviceEventBatteryInfo buildBatteryInfo(int batteryIndex, int level) {
        GBDeviceEventBatteryInfo info = new GBDeviceEventBatteryInfo();
        info.batteryIndex = batteryIndex;
        info.level = level;
        return info;
    }

    private GBDeviceEventVersionInfo buildVersionInfo(String firmware1, String firmware2, String serialNumber) {
        GBDeviceEventVersionInfo info = new GBDeviceEventVersionInfo();
        info.hwVersion = serialNumber;
        info.fwVersion = firmware1;
        info.fwVersion2 = firmware2;
        return info;
    }

    private String readString(byte[] data, int position, int size) {
        if (position + size > data.length) throw new IllegalStateException();
        return new String(data, position, size, StandardCharsets.UTF_8);
    }
    @Override
    public GBDeviceEvent[] decodeResponse(byte[] responseData) {
        ByteBuffer buf = ByteBuffer.wrap(responseData);
        SoundcorePacket packet = SoundcorePacket.decode(buf);

        if (packet == null)
            return null;

        List<GBDeviceEvent> devEvts = new ArrayList<>();
        short cmd = packet.getCommand();
        byte[] payload = packet.getPayload();

        if (cmd == (short) 0x0101) {
            // a lot of other data is in here, anything interesting?
            String firmware1 = readString(payload, 6, 5);
            String firmware2 = readString(payload, 11, 5);
            String serialNumber = readString(payload, 16, 16);
            devEvts.add(buildVersionInfo(firmware1, firmware2, serialNumber));
        } else if (cmd == (short) 0x8d01) {
            LOG.debug("Unknown incoming message - command: " + cmd + ", dump: " + hexdump(responseData));
        } else if (cmd == (short) 0x8205) {
            LOG.debug("Unknown incoming message - command: " + cmd + ", dump: " + hexdump(responseData));
        } else if (cmd == (short) 0x0105) {
            LOG.debug("Unknown incoming message - command: " + cmd + ", dump: " + hexdump(responseData));
        } else if (cmd == (short) 0x0106) { //Sound Mode Update
            decodeAudioMode(payload);
        } else if (cmd == (short) 0x0301) { // Battery Update
            int batteryLeft = payload[0] * 20;
            int batteryRight = payload[1] * 20;
            int batteryCase = payload[2] * 20;

            devEvts.add(buildBatteryInfo(battery_case, batteryCase));
            devEvts.add(buildBatteryInfo(battery_earphone_left, batteryLeft));
            devEvts.add(buildBatteryInfo(battery_earphone_right, batteryRight));
        } else {
            // see https://github.com/gmallios/SoundcoreManager/blob/master/soundcore-lib/src/models/packet_kind.rs
            // for a mapping for other soundcore devices (similar protocol?)
            LOG.debug("Unknown incoming message - command: " + cmd + ", dump: " + hexdump(responseData));
        }
        return devEvts.toArray(new GBDeviceEvent[devEvts.size()]);
    }

    private void decodeAudioMode(byte[] payload) {
        SharedPreferences prefs = getDevicePrefs().getPreferences();
        SharedPreferences.Editor editor = prefs.edit();
        String soundmode = "off";
        int anc_strength = 0;

        if (payload[0] == 0x00) {
            soundmode = "noise_cancelling";
        } else if (payload[0] == 0x01) {
            soundmode = "ambient_sound";
        } else if (payload[0] == 0x02) {
            soundmode = "off";
        }

        if (payload[1] == 0x10) {
            anc_strength = 0;
        } else if (payload[1] == 0x20) {
            anc_strength = 1;
        } else if (payload[1] == 0x30) {
            anc_strength = 2;
        }

        boolean vocal_mode = (payload[2] == 0x01);
        boolean adaptive_anc = (payload[3] == 0x01);
        boolean windnoiseReduction = (payload[4] == 0x01);

        editor.putString(DeviceSettingsPreferenceConst.PREF_SOUNDCORE_AMBIENT_SOUND_CONTROL, soundmode);
        editor.putInt(DeviceSettingsPreferenceConst.PREF_SONY_AMBIENT_SOUND_LEVEL, anc_strength);
        editor.putBoolean(DeviceSettingsPreferenceConst.PREF_SOUNDCORE_TRANSPARENCY_VOCAL_MODE, vocal_mode);
        editor.putBoolean(DeviceSettingsPreferenceConst.PREF_SOUNDCORE_ADAPTIVE_NOISE_CANCELLING, adaptive_anc);
        editor.putBoolean(DeviceSettingsPreferenceConst.PREF_SOUNDCORE_WIND_NOISE_REDUCTION, windnoiseReduction);
        editor.apply();
    }

    @Override
    public byte[] encodeSendConfiguration(String config) {
        Prefs prefs = getDevicePrefs();
        String pref_string;

        switch (config) {
            // Ambient Sound Modes
            case DeviceSettingsPreferenceConst.PREF_SOUNDCORE_AMBIENT_SOUND_CONTROL:
            case DeviceSettingsPreferenceConst.PREF_SOUNDCORE_WIND_NOISE_REDUCTION:
            case DeviceSettingsPreferenceConst.PREF_SOUNDCORE_TRANSPARENCY_VOCAL_MODE:
            case DeviceSettingsPreferenceConst.PREF_SOUNDCORE_ADAPTIVE_NOISE_CANCELLING:
            case DeviceSettingsPreferenceConst.PREF_SONY_AMBIENT_SOUND_LEVEL:
                return encodeAudioMode();

            // Control
            case DeviceSettingsPreferenceConst.PREF_SOUNDCORE_CONTROL_SINGLE_TAP_DISABLED:
                return encodeControlTouchLockMessage(TapAction.SINGLE_TAP, prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_SOUNDCORE_CONTROL_SINGLE_TAP_DISABLED, false));
            case DeviceSettingsPreferenceConst.PREF_SOUNDCORE_CONTROL_DOUBLE_TAP_DISABLED:
                return encodeControlTouchLockMessage(TapAction.DOUBLE_TAP, prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_SOUNDCORE_CONTROL_DOUBLE_TAP_DISABLED, false));
            case DeviceSettingsPreferenceConst.PREF_SOUNDCORE_CONTROL_TRIPLE_TAP_DISABLED:
                return encodeControlTouchLockMessage(TapAction.TRIPLE_TAP, prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_SOUNDCORE_CONTROL_TRIPLE_TAP_DISABLED, false));
            case DeviceSettingsPreferenceConst.PREF_SOUNDCORE_CONTROL_LONG_PRESS_DISABLED:
                return encodeControlTouchLockMessage(TapAction.LONG_PRESS, prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_SOUNDCORE_CONTROL_LONG_PRESS_DISABLED, false));

            case DeviceSettingsPreferenceConst.PREF_SOUNDCORE_CONTROL_SINGLE_TAP_ACTION_LEFT:
                pref_string = prefs.getString(DeviceSettingsPreferenceConst.PREF_SOUNDCORE_CONTROL_SINGLE_TAP_ACTION_LEFT, "");
                return encodeControlFunctionMessage(TapAction.SINGLE_TAP, false, TapFunction.valueOf(pref_string));
            case DeviceSettingsPreferenceConst.PREF_SOUNDCORE_CONTROL_SINGLE_TAP_ACTION_RIGHT:
                pref_string = prefs.getString(DeviceSettingsPreferenceConst.PREF_SOUNDCORE_CONTROL_SINGLE_TAP_ACTION_RIGHT, "");
                return encodeControlFunctionMessage(TapAction.SINGLE_TAP, true, TapFunction.valueOf(pref_string));
            case DeviceSettingsPreferenceConst.PREF_SOUNDCORE_CONTROL_DOUBLE_TAP_ACTION_LEFT:
                pref_string = prefs.getString(DeviceSettingsPreferenceConst.PREF_SOUNDCORE_CONTROL_DOUBLE_TAP_ACTION_LEFT, "");
                return encodeControlFunctionMessage(TapAction.DOUBLE_TAP, false, TapFunction.valueOf(pref_string));
            case DeviceSettingsPreferenceConst.PREF_SOUNDCORE_CONTROL_DOUBLE_TAP_ACTION_RIGHT:
                pref_string = prefs.getString(DeviceSettingsPreferenceConst.PREF_SOUNDCORE_CONTROL_DOUBLE_TAP_ACTION_RIGHT, "");
                return encodeControlFunctionMessage(TapAction.DOUBLE_TAP, true, TapFunction.valueOf(pref_string));
            case DeviceSettingsPreferenceConst.PREF_SOUNDCORE_CONTROL_TRIPLE_TAP_ACTION_LEFT:
                pref_string = prefs.getString(DeviceSettingsPreferenceConst.PREF_SOUNDCORE_CONTROL_TRIPLE_TAP_ACTION_LEFT, "");
                return encodeControlFunctionMessage(TapAction.TRIPLE_TAP, false, TapFunction.valueOf(pref_string));
            case DeviceSettingsPreferenceConst.PREF_SOUNDCORE_CONTROL_TRIPLE_TAP_ACTION_RIGHT:
                pref_string = prefs.getString(DeviceSettingsPreferenceConst.PREF_SOUNDCORE_CONTROL_TRIPLE_TAP_ACTION_RIGHT, "");
                return encodeControlFunctionMessage(TapAction.TRIPLE_TAP, true, TapFunction.valueOf(pref_string));
            case DeviceSettingsPreferenceConst.PREF_SOUNDCORE_CONTROL_LONG_PRESS_ACTION_LEFT:
                pref_string = prefs.getString(DeviceSettingsPreferenceConst.PREF_SOUNDCORE_CONTROL_LONG_PRESS_ACTION_LEFT, "");
                return encodeControlFunctionMessage(TapAction.LONG_PRESS, false, TapFunction.valueOf(pref_string));
            case DeviceSettingsPreferenceConst.PREF_SOUNDCORE_CONTROL_LONG_PRESS_ACTION_RIGHT:
                pref_string = prefs.getString(DeviceSettingsPreferenceConst.PREF_SOUNDCORE_CONTROL_LONG_PRESS_ACTION_RIGHT, "");
                return encodeControlFunctionMessage(TapAction.LONG_PRESS, true, TapFunction.valueOf(pref_string));

            case DeviceSettingsPreferenceConst.PREF_SONY_AMBIENT_SOUND_CONTROL_BUTTON_MODE:
                AmbientSoundControlButtonMode modes = AmbientSoundControlButtonMode.fromPreferences(prefs.getPreferences());
                switch (modes) {
                    case NC_AS_OFF:
                        return encodeControlAmbientModeMessage(true, true, true);
                    case NC_AS:
                        return encodeControlAmbientModeMessage(true, true, false);
                    case NC_OFF:
                        return encodeControlAmbientModeMessage(true, false, true);
                    case AS_OFF:
                        return encodeControlAmbientModeMessage(false, true, true);
                }

            // Miscellaneous Settings
            case DeviceSettingsPreferenceConst.PREF_SOUNDCORE_WEARING_DETECTION:
                boolean wearingDetection = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_SOUNDCORE_WEARING_DETECTION, false);
                return new SoundcorePacket((short) 0x8101, new byte[]{encodeBoolean(wearingDetection)}).encode();
            case DeviceSettingsPreferenceConst.PREF_SOUNDCORE_WEARING_TONE:
                boolean wearingTone = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_SOUNDCORE_WEARING_TONE, false);
                return new SoundcorePacket((short) 0x8c01, new byte[]{encodeBoolean(wearingTone)}).encode();
            case DeviceSettingsPreferenceConst.PREF_SOUNDCORE_TOUCH_TONE:
                boolean touchTone = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_SOUNDCORE_TOUCH_TONE, false);
                return new SoundcorePacket((short) 0x8301, new byte[]{encodeBoolean(touchTone)}).encode();
            default:
                LOG.debug("Unsupported CONFIG: " + config);
        }

        return super.encodeSendConfiguration(config);
    }

    byte[] encodeDeviceInfoRequest() {
        return new SoundcorePacket((short) 0x0101).encode();
    }

    byte[] encodeMysteryDataRequest1() {
        byte[] payload = new byte[]{0x00};
        return new SoundcorePacket((short) 0x8d01, payload).encode();
    }
    byte[] encodeMysteryDataRequest2() {
        return new SoundcorePacket((short) 0x0105).encode();
    }
    byte[] encodeMysteryDataRequest3() {
        byte[] payload = new byte[]{0x00};
        return new SoundcorePacket((short) 0x8205, payload).encode();
    }

    /**
     * Encodes the following settings to a payload to set the audio-mode on the headphones:
     * PREF_SOUNDCORE_AMBIENT_SOUND_CONTROL If ANC, Transparent or neither should be active
     * PREF_SOUNDCORE_ADAPTIVE_NOISE_CANCELLING If the strenght of the ANC should be set manual or adaptively according to ambient noise
     * PREF_SONY_AMBIENT_SOUND_LEVEL How strong the ANC should be in manual mode
     * PREF_SOUNDCORE_TRANSPARENCY_VOCAL_MODE If the Transparency should focus on vocals or should be fully transparent
     * PREF_SOUNDCORE_WIND_NOISE_REDUCTION If Transparency or ANC should reduce Wind Noise
     * @return The payload
     */
    private byte[] encodeAudioMode() {
        Prefs prefs = getDevicePrefs();

        byte anc_mode;
        switch (prefs.getString(DeviceSettingsPreferenceConst.PREF_SOUNDCORE_AMBIENT_SOUND_CONTROL, "off")) {
            case "noise_cancelling":
                anc_mode = 0x00;
                break;
            case "ambient_sound":
                anc_mode = 0x01;
                break;
            case "off":
                anc_mode = 0x02;
                break;
            default:
                LOG.error("Invalid Audio Mode selected");
                return null;
        }

        byte anc_strength;
        switch (prefs.getInt(DeviceSettingsPreferenceConst.PREF_SONY_AMBIENT_SOUND_LEVEL, 0)) {
            case 0:
                anc_strength = 0x10;
                break;
            case 1:
                anc_strength = 0x20;
                break;
            case 2:
                anc_strength = 0x30;
                break;
            default:
                LOG.error("Invalid ANC Strength selected");
                return null;
        }

        byte adaptive_anc = encodeBoolean(prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_SOUNDCORE_ADAPTIVE_NOISE_CANCELLING, true));
        byte vocal_mode = encodeBoolean(prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_SOUNDCORE_TRANSPARENCY_VOCAL_MODE, false));
        byte windnoise_reduction = encodeBoolean(prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_SOUNDCORE_WIND_NOISE_REDUCTION, false));

        byte[] payload = new byte[]{anc_mode, anc_strength, vocal_mode, adaptive_anc, windnoise_reduction, 0x01};
        return new SoundcorePacket((short) 0x8106, payload).encode();
    }

    /**
     * Enables or disables a tap-action
     * @param action The byte that encodes the action (single/double/triple or long tap)
     * @param disabled If the action should be enabled or disabled
     * @return
     */
    private byte[] encodeControlTouchLockMessage(TapAction action, boolean disabled) {
        boolean enabled = !disabled;
        byte enabled_byte;
        byte[] payload;
        switch (action) {
            case SINGLE_TAP:
            case TRIPLE_TAP:
                enabled_byte = encodeBoolean(enabled);
                break;
            case DOUBLE_TAP:
            case LONG_PRESS:
                enabled_byte = enabled?(byte) 0x11: (byte) 0x10;
                break;
            default:
                LOG.error("Invalid Tap action");
                return null;
        }
        payload = new byte[]{0x00, action.getCode(), enabled_byte};
        return new SoundcorePacket((short) 0x8304, payload).encode();
    }

    /**
     * Assigns a function (eg play/pause) to an action (eg single tap on right bud)
     * @param action The byte that encodes the action (single/double/triple or long tap)
     * @param right  If the right or left earbud is meant
     * @param function The byte that encodes the triggered function (eg play/pause)
     * @return The encoded message
     */
    private byte[] encodeControlFunctionMessage(TapAction action, boolean right, TapFunction function) {
        byte function_byte;
        switch (action) {
            case SINGLE_TAP:
            case DOUBLE_TAP:
                function_byte = (byte) (16*6 + function.getCode());
                break;
            case TRIPLE_TAP:
                function_byte = (byte) (16*4 + function.getCode());
                break;
            case LONG_PRESS:
                function_byte = (byte) (16*5 + function.getCode());
                break;
            default:
                LOG.error("Invalid Tap action");
                return null;
        }
        byte[] payload = new byte[] {encodeBoolean(right), action.getCode(), function_byte};
        return new SoundcorePacket((short) 0x8104, payload).encode();
    }

    /**
     * Encodes between which Audio Modes a tap should switch, if it is set to switch the Audio Mode.
     * Zb ANC -> -> Transparency -> Normal -> ANC -> ....
     */
    private byte[] encodeControlAmbientModeMessage(boolean anc, boolean transparency, boolean normal) {
        // Original app does not allow only one true flag. Unsure if Earbuds accept this state.
        byte ambientModes = (byte) (4 * (normal?1:0) + 2 * (transparency?1:0) + (anc?1:0));
        return new SoundcorePacket((short) 0x8206, new byte[] {ambientModes}).encode();
    }

    private byte encodeBoolean(boolean bool) {
        if (bool) return 0x01;
        else return 0x00;
    }
}
