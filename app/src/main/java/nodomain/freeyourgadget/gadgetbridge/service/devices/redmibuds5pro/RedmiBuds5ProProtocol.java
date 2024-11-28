/*  Copyright (C) 2024 Jonathan Gobbo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.redmibuds5pro;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.*;
import static nodomain.freeyourgadget.gadgetbridge.util.GB.hexdump;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventSendBytes;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdateDeviceState;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.redmibuds5pro.prefs.Configuration.Config;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.redmibuds5pro.prefs.Configuration.StrengthTarget;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.redmibuds5pro.prefs.Gestures.InteractionType;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.redmibuds5pro.prefs.Gestures.Position;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice.State;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.service.devices.redmibuds5pro.protocol.Authentication;
import nodomain.freeyourgadget.gadgetbridge.service.devices.redmibuds5pro.protocol.Message;
import nodomain.freeyourgadget.gadgetbridge.service.devices.redmibuds5pro.protocol.MessageType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.redmibuds5pro.protocol.Opcode;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class RedmiBuds5ProProtocol extends GBDeviceProtocol {

    private static final Logger LOG = LoggerFactory.getLogger(RedmiBuds5ProProtocol.class);
    final UUID UUID_DEVICE_CTRL = UUID.fromString("0000fd2d-0000-1000-8000-00805f9b34fb");

    private byte sequenceNumber = 0;

    protected RedmiBuds5ProProtocol(GBDevice device) {
        super(device);
    }

    public byte[] encodeStartAuthentication() {
        byte[] authRnd = Authentication.getRandomChallenge();
        LOG.debug("[AUTH] Sending challenge: {}", hexdump(authRnd));

        byte[] payload = new byte[17];
        payload[0] = 0x01;
        System.arraycopy(authRnd, 0, payload, 1, 16);
        return new Message(MessageType.PHONE_REQUEST, Opcode.AUTH_CHALLENGE, sequenceNumber++, payload).encode();
    }

    @Override
    public byte[] encodeSendConfiguration(String config) {
        switch (config) {
            case PREF_REDMI_BUDS_5_PRO_AMBIENT_SOUND_CONTROL:
                return encodeSetAmbientSoundControl();
            case PREF_REDMI_BUDS_5_PRO_NOISE_CANCELLING_STRENGTH:
                return encodeSetEffectStrength(config, StrengthTarget.ANC);
            case PREF_REDMI_BUDS_5_PRO_TRANSPARENCY_STRENGTH:
                return encodeSetEffectStrength(config, StrengthTarget.TRANSPARENCY);
            case PREF_REDMI_BUDS_5_PRO_ADAPTIVE_NOISE_CANCELLING:
                return encodeSetBooleanConfig(config, Config.ADAPTIVE_ANC);
//            case PREF_REDMI_BUDS_5_PRO_PERSONALIZED_NOISE_CANCELLING:
//                return encodeSetBooleanConfig(config, Config.CUSTOMIZED_ANC);

            case PREF_REDMI_BUDS_5_PRO_CONTROL_SINGLE_TAP_LEFT:
                return encodeSetGesture(config, InteractionType.SINGLE, Position.LEFT);
            case PREF_REDMI_BUDS_5_PRO_CONTROL_SINGLE_TAP_RIGHT:
                return encodeSetGesture(config, InteractionType.SINGLE, Position.RIGHT);
            case PREF_REDMI_BUDS_5_PRO_CONTROL_DOUBLE_TAP_LEFT:
                return encodeSetGesture(config, InteractionType.DOUBLE, Position.LEFT);
            case PREF_REDMI_BUDS_5_PRO_CONTROL_DOUBLE_TAP_RIGHT:
                return encodeSetGesture(config, InteractionType.DOUBLE, Position.RIGHT);
            case PREF_REDMI_BUDS_5_PRO_CONTROL_TRIPLE_TAP_LEFT:
                return encodeSetGesture(config, InteractionType.TRIPLE, Position.LEFT);
            case PREF_REDMI_BUDS_5_PRO_CONTROL_TRIPLE_TAP_RIGHT:
                return encodeSetGesture(config, InteractionType.TRIPLE, Position.RIGHT);
            case PREF_REDMI_BUDS_5_PRO_CONTROL_LONG_TAP_MODE_LEFT:
                return encodeSetGesture(config, InteractionType.LONG, Position.LEFT);
            case PREF_REDMI_BUDS_5_PRO_CONTROL_LONG_TAP_MODE_RIGHT:
                return encodeSetGesture(config, InteractionType.LONG, Position.RIGHT);
            case PREF_REDMI_BUDS_5_PRO_CONTROL_LONG_TAP_SETTINGS_LEFT:
                return encodeSetLongGestureMode(config, Position.LEFT);
            case PREF_REDMI_BUDS_5_PRO_CONTROL_LONG_TAP_SETTINGS_RIGHT:
                return encodeSetLongGestureMode(config, Position.RIGHT);

            case PREF_REDMI_BUDS_5_PRO_WEARING_DETECTION:
                return encodeSetEarDetection();
            case PREF_REDMI_BUDS_5_PRO_AUTO_REPLY_PHONECALL:
                return encodeSetBooleanConfig(config, Config.AUTO_ANSWER);
            case PREF_REDMI_BUDS_5_PRO_DOUBLE_CONNECTION:
                return encodeSetBooleanConfig(config, Config.DOUBLE_CONNECTION);
            case PREF_REDMI_BUDS_5_PRO_ADAPTIVE_SOUND:
                return encodeSetBooleanConfig(config, Config.ADAPTIVE_SOUND);

            case PREF_REDMI_BUDS_5_PRO_EQUALIZER_PRESET:
                return encodeSetIntegerConfig(config, Config.EQ_PRESET);
            case PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_62:
            case PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_125:
            case PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_250:
            case PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_500:
            case PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_1k:
            case PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_2k:
            case PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_4k:
            case PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_8k:
            case PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_12k:
            case PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_16k:
                return encodeSetCustomEqualizer();

            default:
                LOG.debug("Unsupported config: {}", config);
        }

        return super.encodeSendConfiguration(config);
    }

    public byte[] encodeSetCustomEqualizer() {
        Prefs prefs = getDevicePrefs();

        List<String> bands = List.of(PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_62, PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_125,
                PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_250, PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_500, PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_1k,
                PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_2k, PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_4k, PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_8k,
                PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_12k, PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_16k);

        byte[] eqCurve = new byte[10];
        for (int i = 0; i < 10; i++) {
            eqCurve[i] = (byte) Integer.parseInt(prefs.getString(bands.get(i), "0"));
        }
        return new Message(MessageType.PHONE_REQUEST, Opcode.SET_CONFIG, sequenceNumber++, new byte[]{
                0x24, 0x00, 0x37, 0x05, 0x01, 0x01, 0x0A,
                0x00, 0x3E, eqCurve[0], 0x00, 0x7D, eqCurve[1],
                0x00, (byte) 0xFA, eqCurve[2], 0x01, (byte) 0xF4, eqCurve[3],
                0x03, (byte) 0xE8, eqCurve[4], 0x07, (byte) 0xE0, eqCurve[5],
                0x0F, (byte) 0xA0, eqCurve[6], 0x1F, 0x40, eqCurve[7],
                0x2E, (byte) 0xE0, eqCurve[8], 0x3E, (byte) 0x80, eqCurve[9]
        }).encode();
    }

    public byte[] encodeSetEarDetection() {
        Prefs prefs = getDevicePrefs();
        byte value = (byte) (prefs.getBoolean(PREF_REDMI_BUDS_5_PRO_WEARING_DETECTION, false) ? 0x00 : 0x01);
        return new Message(MessageType.PHONE_REQUEST, Opcode.ANC, sequenceNumber++, new byte[]{0x02, 0x06, value}).encode();
    }

    public byte[] encodeSetLongGestureMode(String config, Position position) {
        Prefs prefs = getDevicePrefs();
        byte value = (byte) Integer.parseInt(prefs.getString(config, "7"));
        byte[] payload = new byte[]{0x04, 0x00, 0x0a, (byte) 0xFF, (byte) 0xFF};
        if (position == Position.LEFT) {
            payload[3] = value;
        } else {
            payload[4] = value;
        }
        return new Message(MessageType.PHONE_REQUEST, Opcode.SET_CONFIG, sequenceNumber++, payload).encode();
    }

    public byte[] encodeSetGesture(String config, InteractionType interactionType, Position position) {
        Prefs prefs = getDevicePrefs();
        byte value = (byte) Integer.parseInt(prefs.getString(config, "1"));
        byte[] payload = new byte[]{0x05, 0x00, 0x02, interactionType.value, (byte) 0xFF, (byte) 0xFF};
        if (position == Position.LEFT) {
            payload[4] = value;
        } else {
            payload[5] = value;
        }
        return new Message(MessageType.PHONE_REQUEST, Opcode.SET_CONFIG, sequenceNumber++, payload).encode();
    }

    public byte[] encodeSetEffectStrength(String pref, StrengthTarget effect) {
        Prefs prefs = getDevicePrefs();
        byte mode = (byte) Integer.parseInt(prefs.getString(pref, "0"));
        return new Message(MessageType.PHONE_REQUEST, Opcode.SET_CONFIG, sequenceNumber++, new byte[]{0x04, 0x00, 0x0b, effect.value, mode}).encode();
    }

    public byte[] encodeSetIntegerConfig(String pref, Config config) {
        Prefs prefs = getDevicePrefs();
        byte value = (byte) Integer.parseInt(prefs.getString(pref, "0"));
        return new Message(MessageType.PHONE_REQUEST, Opcode.SET_CONFIG, sequenceNumber++, new byte[]{0x03, 0x00, config.value, value}).encode();
    }

    public byte[] encodeSetBooleanConfig(String pref, Config config) {
        Prefs prefs = getDevicePrefs();
        byte value = (byte) (prefs.getBoolean(pref, false) ? 0x01 : 0x00);
        return new Message(MessageType.PHONE_REQUEST, Opcode.SET_CONFIG, sequenceNumber++, new byte[]{0x03, 0x00, config.value, value}).encode();
    }

    public byte[] encodeGetConfig() {
        List<Config> configs = List.of(Config.EFFECT_STRENGTH, Config.ADAPTIVE_ANC, // Config.CUSTOMIZED_ANC,
                Config.GESTURES, Config.LONG_GESTURES, Config.EAR_DETECTION, Config.DOUBLE_CONNECTION,
                Config.AUTO_ANSWER, Config.ADAPTIVE_SOUND, Config.EQ_PRESET, Config.EQ_CURVE);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            for (Config config : configs) {
                Message message = new Message(MessageType.PHONE_REQUEST, Opcode.GET_CONFIG, sequenceNumber++, new byte[]{0x00, config.value});
                outputStream.write(message.encode());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return outputStream.toByteArray();
    }

    public byte[] encodeSetAmbientSoundControl() {
        Prefs prefs = getDevicePrefs();
        byte mode = (byte) Integer.parseInt(prefs.getString(PREF_REDMI_BUDS_5_PRO_AMBIENT_SOUND_CONTROL, "0"));
        return new Message(MessageType.PHONE_REQUEST, Opcode.ANC, sequenceNumber++, new byte[]{0x02, 0x04, mode}).encode();
    }

    public void decodeGetConfig(byte[] configPayload) {

        SharedPreferences preferences = getDevicePrefs().getPreferences();
        Editor editor = preferences.edit();
        Config config = Config.fromCode(configPayload[2]);
        switch (config) {
            case GESTURES:
                editor.putString(PREF_REDMI_BUDS_5_PRO_CONTROL_SINGLE_TAP_LEFT, Integer.toString(configPayload[4]));
                editor.putString(PREF_REDMI_BUDS_5_PRO_CONTROL_SINGLE_TAP_RIGHT, Integer.toString(configPayload[5]));

                editor.putString(PREF_REDMI_BUDS_5_PRO_CONTROL_DOUBLE_TAP_LEFT, Integer.toString(configPayload[7]));
                editor.putString(PREF_REDMI_BUDS_5_PRO_CONTROL_DOUBLE_TAP_RIGHT, Integer.toString(configPayload[8]));

                editor.putString(PREF_REDMI_BUDS_5_PRO_CONTROL_TRIPLE_TAP_LEFT, Integer.toString(configPayload[10]));
                editor.putString(PREF_REDMI_BUDS_5_PRO_CONTROL_TRIPLE_TAP_RIGHT, Integer.toString(configPayload[11]));

                editor.putString(PREF_REDMI_BUDS_5_PRO_CONTROL_LONG_TAP_MODE_LEFT, Integer.toString(configPayload[13]));
                editor.putString(PREF_REDMI_BUDS_5_PRO_CONTROL_LONG_TAP_MODE_RIGHT, Integer.toString(configPayload[14]));
                break;
            case AUTO_ANSWER:
                editor.putBoolean(PREF_REDMI_BUDS_5_PRO_AUTO_REPLY_PHONECALL, configPayload[3] == 0x01);
                break;
            case DOUBLE_CONNECTION:
                editor.putBoolean(PREF_REDMI_BUDS_5_PRO_DOUBLE_CONNECTION, configPayload[3] == 0x01);
                break;
            case EQ_PRESET:
                editor.putString(PREF_REDMI_BUDS_5_PRO_EQUALIZER_PRESET, Integer.toString(configPayload[3]));
                break;
            case LONG_GESTURES:
                editor.putString(PREF_REDMI_BUDS_5_PRO_CONTROL_LONG_TAP_SETTINGS_LEFT, Integer.toString(configPayload[3]));
                editor.putString(PREF_REDMI_BUDS_5_PRO_CONTROL_LONG_TAP_SETTINGS_RIGHT, Integer.toString(configPayload[4]));
                break;
            case EFFECT_STRENGTH:
                byte mode = configPayload[4];
                if (configPayload[3] == StrengthTarget.ANC.value) {
                    editor.putString(PREF_REDMI_BUDS_5_PRO_NOISE_CANCELLING_STRENGTH, Integer.toString(mode));
                } else if (configPayload[3] == StrengthTarget.TRANSPARENCY.value) {
                    editor.putString(PREF_REDMI_BUDS_5_PRO_TRANSPARENCY_STRENGTH, Integer.toString(mode));
                }
                break;
            case ADAPTIVE_ANC:
                editor.putBoolean(PREF_REDMI_BUDS_5_PRO_ADAPTIVE_NOISE_CANCELLING, configPayload[3] == 0x01);
                break;
            case ADAPTIVE_SOUND:
                editor.putBoolean(PREF_REDMI_BUDS_5_PRO_ADAPTIVE_SOUND, configPayload[3] == 0x01);
                break;
            case EQ_CURVE:
                editor.putString(PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_62, Integer.toString(configPayload[12] & 0xFF));
                editor.putString(PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_125, Integer.toString(configPayload[15] & 0xFF));
                editor.putString(PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_250, Integer.toString(configPayload[18] & 0xFF));
                editor.putString(PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_500, Integer.toString(configPayload[21] & 0xFF));
                editor.putString(PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_1k, Integer.toString(configPayload[24] & 0xFF));
                editor.putString(PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_2k, Integer.toString(configPayload[27] & 0xFF));
                editor.putString(PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_4k, Integer.toString(configPayload[30] & 0xFF));
                editor.putString(PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_8k, Integer.toString(configPayload[33] & 0xFF));
                editor.putString(PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_12k, Integer.toString(configPayload[36] & 0xFF));
                editor.putString(PREF_REDMI_BUDS_5_PRO_EQUALIZER_BAND_16k, Integer.toString(configPayload[39] & 0xFF));
                break;
//            case CUSTOMIZED_ANC:
//                editor.putBoolean(PREF_REDMI_BUDS_5_PRO_PERSONALIZED_NOISE_CANCELLING, configPayload[3] == 0x01);
//                break;
            default:
                LOG.debug("Unhandled device update: {}", hexdump(configPayload));
        }
        editor.apply();
    }

    private GBDeviceEventBatteryInfo parseBatteryInfo(byte batteryInfo, int index) {

        if (batteryInfo == (byte) 0xff) {
            return null;
        }
        GBDeviceEventBatteryInfo batteryEvent = new GBDeviceEventBatteryInfo();
        batteryEvent.state = (batteryInfo & 128) != 0 ? BatteryState.BATTERY_CHARGING : BatteryState.BATTERY_NORMAL;
        batteryEvent.batteryIndex = index;
        batteryEvent.level = (batteryInfo & 127);
        LOG.debug("Battery {}: {}", index, batteryEvent.level);
        return batteryEvent;
    }

    private GBDeviceEvent[] decodeDeviceInfo(byte[] deviceInfoPayload) {

        List<GBDeviceEvent> events = new ArrayList<>();

        GBDeviceEventVersionInfo info = new GBDeviceEventVersionInfo();
        byte[] fw = new byte[4];
        byte[] vidPid = new byte[4];
        byte[] batteryData = new byte[3];
        int i = 0;
        while (i < deviceInfoPayload.length) {
            byte len = deviceInfoPayload[i];
            byte index = deviceInfoPayload[i + 1];
            switch (index) {
                case 0x01:
                    System.arraycopy(deviceInfoPayload, i + 2, fw, 0, 4);
                    break;
                case 0x03:
                    System.arraycopy(deviceInfoPayload, i + 2, vidPid, 0, 4);
                    break;
                case 0x07:
                    System.arraycopy(deviceInfoPayload, i + 2, batteryData, 0, 3);
                    break;
            }
            i += len + 1;
        }

        String fwVersion1 = ((fw[0] >> 4) & 0xF) + "." + (fw[0] & 0xF) + "." + ((fw[1] >> 4) & 0xF) + "." + (fw[1] & 0xF);
        String fwVersion2 = ((fw[2] >> 4) & 0xF) + "." + (fw[2] & 0xF) + "." + ((fw[3] >> 4) & 0xF) + "." + (fw[3] & 0xF);
        String hwVersion = String.format("VID: 0x%02X%02X, PID: 0x%02X%02X", vidPid[0], vidPid[1], vidPid[2], vidPid[3]);

        info.fwVersion = fwVersion1;
        info.fwVersion2 = fwVersion2;
        info.hwVersion = hwVersion;

        events.add(parseBatteryInfo(batteryData[0], 1));
        events.add(parseBatteryInfo(batteryData[1], 2));
        events.add(parseBatteryInfo(batteryData[2], 0));
        events.add(info);

        return events.toArray(new GBDeviceEvent[0]);
    }

    private void decodeDeviceRunInfo(byte[] deviceRunInfoPayload) {
        int i = 0;
        while (i < deviceRunInfoPayload.length) {
            byte len = deviceRunInfoPayload[i];
            byte index = deviceRunInfoPayload[i + 1];
            SharedPreferences preferences = getDevicePrefs().getPreferences();
            Editor editor = preferences.edit();
            switch (index) {
                case 0x09:
                    byte mode = deviceRunInfoPayload[i + 2];
                    editor.putString(PREF_REDMI_BUDS_5_PRO_AMBIENT_SOUND_CONTROL, Integer.toString(mode));
                    break;
                case 0x0A:
                    editor.putBoolean(PREF_REDMI_BUDS_5_PRO_WEARING_DETECTION, deviceRunInfoPayload[i + 2] == 0x00);
            }
            editor.apply();
            i += len + 1;
        }
    }

    private GBDeviceEvent[] decodeDeviceUpdate(Message updateMessage) {
        byte[] updatePayload = updateMessage.getPayload();
        List<GBDeviceEvent> events = new ArrayList<>();

        int i = 0;
        while (i < updatePayload.length) {
            byte len = updatePayload[i];
            byte index = updatePayload[i + 1];
            switch (index) {
                case 0x00:
                    events.add(parseBatteryInfo(updatePayload[i + 2], 1));
                    events.add(parseBatteryInfo(updatePayload[i + 3], 2));
                    events.add(parseBatteryInfo(updatePayload[i + 4], 0));
                    break;
                case 0x04:
                    SharedPreferences preferences = getDevicePrefs().getPreferences();
                    Editor editor = preferences.edit();

                    byte mode = updatePayload[i + 2];
                    editor.putString(PREF_REDMI_BUDS_5_PRO_AMBIENT_SOUND_CONTROL, Integer.toString(mode));
                    editor.apply();
                    break;
                default:
                    LOG.debug("Unimplemented device update: {}", hexdump(updatePayload));
            }
            i += len + 1;
        }
        events.add(new GBDeviceEventSendBytes(new Message(MessageType.RESPONSE, Opcode.REPORT_STATUS, updateMessage.getSequenceNumber(), new byte[]{}).encode()));
        return events.toArray(new GBDeviceEvent[0]);
    }

    private GBDeviceEvent[] decodeNotifyConfig(Message notifyMessage) {

        byte[] notifyPayload = notifyMessage.getPayload();
        List<GBDeviceEvent> events = new ArrayList<>();

        int i = 0;
        while (i < notifyPayload.length) {
            byte len = notifyPayload[i];
            byte index = notifyPayload[i + 2];
            switch (index) {
                case 0x0C:
                    LOG.debug("Received earbuds position info");
                    /*
                    e.g. 0C 03
                            0011
                            wearing left, wearing right, left in case, right in case
                     */
                    break;
                case 0x0B:
                    SharedPreferences preferences = getDevicePrefs().getPreferences();
                    Editor editor = preferences.edit();

                    byte soundCtrlMode = notifyPayload[i + 3];
                    editor.putString(PREF_REDMI_BUDS_5_PRO_AMBIENT_SOUND_CONTROL, Integer.toString(soundCtrlMode));

                    byte mode = notifyPayload[i + 4];
                    if (notifyPayload[i + 3] == 0x01) {
                        editor.putString(PREF_REDMI_BUDS_5_PRO_NOISE_CANCELLING_STRENGTH, Integer.toString(mode));
                    } else {
                        editor.putString(PREF_REDMI_BUDS_5_PRO_TRANSPARENCY_STRENGTH, Integer.toString(mode));
                    }

                    editor.apply();
                    break;
            }

            i += len + 1;
        }
        events.add(new GBDeviceEventSendBytes(new Message(MessageType.RESPONSE, Opcode.NOTIFY_CONFIG, notifyMessage.getSequenceNumber(), new byte[]{}).encode()));
        return events.toArray(new GBDeviceEvent[0]);
    }

    private GBDeviceEvent[] handleAuthentication(Message authMessage) {
        List<GBDeviceEvent> events = new ArrayList<>();
        switch (authMessage.getOpcode()) {
            case AUTH_CHALLENGE:
                if (authMessage.getType() == MessageType.RESPONSE) {
                    LOG.debug("[AUTH] Received Challenge Response");
                    /*
                        Should check if equal, but does not really matter
                     */
                    LOG.debug("[AUTH] Sending authentication confirmation");
                    events.add(new GBDeviceEventSendBytes(new Message(MessageType.PHONE_REQUEST, Opcode.AUTH_CONFIRM, sequenceNumber++, new byte[]{0x01, 0x00}).encode()));
                } else {
                    byte[] responsePayload = authMessage.getPayload();
                    byte[] challenge = new byte[16];
                    System.arraycopy(responsePayload, 1, challenge, 0, 16);

                    LOG.info("[AUTH] Received Challenge: {}", hexdump(challenge));
                    Authentication auth = new Authentication();
                    byte[] challengeResponse = auth.computeChallengeResponse(challenge);
                    LOG.info("[AUTH] Sending Challenge Response: {}", hexdump(challengeResponse));

                    byte[] payload = new byte[17];
                    payload[0] = 0x01;
                    System.arraycopy(challengeResponse, 0, payload, 1, 16);
                    Message res = new Message(MessageType.RESPONSE, Opcode.AUTH_CHALLENGE, authMessage.getSequenceNumber(), payload);
                    events.add(new GBDeviceEventSendBytes(res.encode()));
                }
                break;
            case AUTH_CONFIRM:
                if (authMessage.getType() == MessageType.RESPONSE) {
                    LOG.debug("[AUTH] Confirmed first authentication step");
                } else {
                    LOG.debug("[AUTH] Received authentication confirmation");
                    Message res = new Message(MessageType.RESPONSE, Opcode.AUTH_CONFIRM, authMessage.getSequenceNumber(), new byte[]{0x01});
                    LOG.debug("[AUTH] Sending final authentication confirmation");
                    events.add(new GBDeviceEventSendBytes(res.encode()));

                    LOG.debug("[INIT] Sending device info request");
                    Message info = new Message(MessageType.PHONE_REQUEST, Opcode.GET_DEVICE_INFO, sequenceNumber++, new byte[]{(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff});
                    events.add(new GBDeviceEventSendBytes(info.encode()));

                    LOG.debug("[INIT] Sending device run info request");
                    Message runInfo = new Message(MessageType.PHONE_REQUEST, Opcode.GET_DEVICE_RUN_INFO, sequenceNumber++, new byte[]{(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff});
                    events.add(new GBDeviceEventSendBytes(runInfo.encode()));

                    LOG.debug("[INIT] Sending configuration request");
                    events.add(new GBDeviceEventSendBytes(encodeGetConfig()));
                }
                break;
        }
        return events.toArray(new GBDeviceEvent[0]);
    }

    @Override
    public GBDeviceEvent[] decodeResponse(byte[] responseData) {

        LOG.debug("Incoming message: {}", hexdump(responseData));

        List<GBDeviceEvent> events = new ArrayList<>();

        List<Message> incomingMessages = Message.splitPiggybackedMessages(responseData);

        for (Message message : incomingMessages) {

            LOG.debug("Parsed message: {}", message);

            switch (message.getOpcode()) {
                case AUTH_CHALLENGE:
                case AUTH_CONFIRM:
                    events.addAll(Arrays.asList(handleAuthentication(message)));
                    break;
                case GET_DEVICE_INFO:
                    LOG.debug("[INIT] Received device info");
                    if (getDevice().getState() != State.INITIALIZED) {
                        events.addAll(Arrays.asList(decodeDeviceInfo(message.getPayload())));
                        LOG.debug("[INIT] Device Initialized");
                        events.add(new GBDeviceEventUpdateDeviceState(State.INITIALIZED));
                    }
                    break;
                case GET_DEVICE_RUN_INFO:
                    LOG.debug("[INIT] Received device run info");
                    decodeDeviceRunInfo(message.getPayload());
                    break;
                case REPORT_STATUS:
                    events.addAll(Arrays.asList(decodeDeviceUpdate(message)));
                    break;
                case GET_CONFIG:
                    decodeGetConfig(message.getPayload());
                    break;
                case NOTIFY_CONFIG:
                    events.addAll(Arrays.asList(decodeNotifyConfig(message)));
                    break;
                default:
                    LOG.debug("Unhandled message: {}", message);
                    break;
            }
        }
        return events.toArray(new GBDeviceEvent[0]);
    }

}
