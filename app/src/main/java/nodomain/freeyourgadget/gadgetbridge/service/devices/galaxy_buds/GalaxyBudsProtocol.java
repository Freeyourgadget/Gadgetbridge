/*  Copyright (C) 2021-2024 narektor, Petr Vaněk

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.galaxy_buds;

import static nodomain.freeyourgadget.gadgetbridge.util.CheckSums.crc16_ccitt;
import static nodomain.freeyourgadget.gadgetbridge.util.GB.hexdump;

import android.content.SharedPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;

public class GalaxyBudsProtocol extends GBDeviceProtocol {
    private static final Logger LOG = LoggerFactory.getLogger(GalaxyBudsProtocol.class);

    final UUID UUID_GALAXY_BUDS_DEVICE_CTRL = UUID.fromString("00001102-0000-1000-8000-00805f9b34fd");
    final UUID UUID_GALAXY_BUDS_LIVE_DEVICE_CTRL = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static final byte SOM_BUDS = (byte) 0xFE;
    private static final byte EOM_BUDS = (byte) 0xEE;
    private static final byte SOM_BUDS_PLUS = (byte) 0xFD;
    private static final byte EOM_BUDS_PLUS = (byte) 0xDD;

    private byte StartOfMessage = SOM_BUDS;
    private byte EndOfMessage = EOM_BUDS;

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
    private static final byte set_game_mode = (byte) 0x87; // 0x0/0x2 no idea if this is doing anything...
    // this is sent dynamically based on whether the current running app is a game or not

    private static final byte set_equalizer = (byte) 0x86;

    private static final byte set_reset = (byte) 0x50;

    private static final byte set_touchpad_options = (byte) 0x92;

    private static final byte get_debug_build_info = (byte) 0x28;
    private static final byte get_serial_number = (byte) 0x29;
    private static final byte get_debug_get_all_data = (byte) 0x26;
    private static final byte get_debug_get_version = (byte) 0x24;

    //Live
    private static final byte set_automatic_noise_cancelling = (byte) 0x98; //0x0/0x1
    private static final byte set_pressure_relief = (byte) 0x9f; //0x0/0x1

    //Live and Pro
    private static final byte set_live_pro_game_mode = (byte) 0x85; // 0x0/0x1 no idea if this is doing anything

    //Pro
    // Comments thanks to phh as per https://codeberg.org/Freeyourgadget/Gadgetbridge/issues/2642#issuecomment-445962

    private static final byte set_spatial_audio_control = (byte) 0xc3; //0x0/0x1
    // takes a boolean '1' or '0' to enable or disable 360 Audio (probably useless until someone
    // manage to send Dolby Atmos to the headset). 360 audio is a feature where (Shown only on Samsung rom)

    private static final byte set_outside_double_tap = (byte) 0x95; //0x0/0x1
    // This is an option in "Labs", to detect double tap even when not taped on touch pad (so I presume it's using accelerometer)

    private static final byte set_adjust_sound_sync = (byte) 0x85; //0x0/0x1
    // This is an option in "Labs" available only on Samsung ROM, exposed to the user as "automatic game mode".
    // This is used in confunction with GAME_MODE. My guess is that this says to the ear buds
    // "yes, I know that the reported latency might change on the fly, I'm fine with that".

    private static final byte set_detect_conversations = (byte) 0x7a; //0x0/0x1
    // This is called "Voice detect -- Noise controls and sound settings go back to the previous state
    // when your voice isn't detected for 10 seconds". This triggers the feature that when you talk,
    // the ear buds automatically go into "ambient sound" mode, and enhance the sound of your interlocutor

    private static final byte set_detect_conversations_duration = (byte) 0x7b; //0x0/0x1/0x2
    // Takes {0,1,2}: 0 means 5 seconds, 1 means 10 seconds, 2 means 15s. This is the duration after
    // which the ear buds go back to ANC after switching to ambient sound when the user talks.

    private static final byte set_ambient_mode_during_calls = (byte) 0x8b; //0x0/0x1
    // "Use ambient sound during calls"

    private static final byte set_noise_controls_with_one_earbud = (byte) 0x6f; //0x0/0x1
    // "Noise controls with one earbud" in Accessibility menu.
    // It allows ANC and ambient sound even if only one earbud is in-ear.

    private static final byte set_balance = (byte) 0x8f;
    // takes value in 0-32 range, it is used to change left/right balance

    private static final byte extra_high_ambient = (byte) 0x96; //0x0/0x1
    //  "Maximize ambient sound volume"/"Amplify sounds from your surroundings so you can stay aware of what's going on around you.

    private static final byte set_seamless_connection = (byte) 0xaf; //0x0/0x1
    // It is used to allow the ear buds to roam across devices, like devices being allowed to take ear buds "focus".

    private static final byte set_voice_wake_up = (byte) 0x97; //0x0/0x1
    // Enables "Hey Bixby" wake up word. Shown only on Samsung ROM

    private static final byte set_speak_seamlessly = (byte) 0x7d; //0x0/0x1
    //  "After Voice wake up, you can say the command you want right away without waiting for sound feedback."

    private static final byte voice_wake_up_language = (byte) 0x99;
    // Language for "Hey bixby" wakeup - 1 = "de-DE", 2 = "en-GB", 3 = "en-US", 4 = "es-ES", 5 = "fr-FR", 6 = "it-IT", 7 = "ko-KR", 8 = "pt-BR", 9 = "zh-CN"

    private static final byte set_voice_noti_status = (byte) 0xa4; //0x0/0x1
    // I have no idea why it does that. It sends "1" when it starts reading notification aloud, and "0" when it finished.

    private static final byte set_noise_controls = (byte) 0x78; //Takes 0/1/2.
    // 0 is Ambient Sound and ANC OFF, 1 is ANC on, 2 is Ambient sound ON

    private static final byte set_mute_earbud = (byte) 0xa2;
    // Takes two booleans (left then right). This is used in conjuction with FIND_MY_EARBUDS_START to alternate between tweeting the left and the right earbud

    private static final byte set_customize_ambient_sound = (byte) 0x82;
    //one byte for left volume 0-4, one byte for right volume 0-4, and one byte 0-4 for ambient sound tone from "soft" to "clear"

    private static final byte set_noise_reduction_level = (byte) 0x83;
    // 1 means High noise reduction, 0 low noise reduction.

    private static final byte set_touch_and_hold_noise_controls = (byte) 0x79;
    // Takes either 3 booleans or 6 booleans depending on earbuds revision.
    // It is used when long press on touchpad is set in SET_TOUCHPAD_OPTION to "Switch noise control",
    // to control whether the long press switches between ANC on <=> Ambient sound on, or to anc+ambient off,
    // or ambient <=> off or anc <=> off, and when it's 6 bytes, it can have different behavior between right and left earbud.

    private static final byte voice_wake_up_event = (byte) 0x9a;
    // Just a reponse. It's a ACK for when received "Hey bixby" command ?

    private static final byte in_ear_detection = (byte) 0x6e;

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

        int length = 9;
        byte type = 0;

        byte sof = incoming.get();
        if (sof != StartOfMessage) {
            LOG.error("Error in message, wrong start of frame: " + hexdump(responseData));
            return null;
        }

        if (StartOfMessage == SOM_BUDS_PLUS) {
            length = (int) (incoming.get() & 0xff);
            type = incoming.get();
        } else {
            type = incoming.get();
            length = (int) (incoming.get() & 0xff);
        }

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
                devEvts.addAll(handleBatteryInfo(Arrays.copyOfRange(payload, 1, 11))); //11
                break;
            case battery_status2:
                devEvts.addAll(handleBatteryInfo(Arrays.copyOfRange(payload, 2, 12))); //12
                break;
            default:
                LOG.debug("Unhandled: " + hexdump(responseData));

        }
        return devEvts.toArray(new GBDeviceEvent[devEvts.size()]);
    }


    byte[] encodeMessage(byte command) {
        ByteBuffer msgBuf = ByteBuffer.allocate(7);
        msgBuf.order(ByteOrder.LITTLE_ENDIAN);
        msgBuf.put(StartOfMessage);
        byte size = 0x3;
        if (StartOfMessage == SOM_BUDS_PLUS) {
            msgBuf.put((byte) size);
            msgBuf.put((byte) 0x0); //0x0 for sending
        } else {
            msgBuf.put((byte) 0x0); //0x0 for sending
            msgBuf.put((byte) size); //size
        }
        msgBuf.put((byte) command); //command id
        msgBuf.putShort((short) crc16_ccitt(new byte[]{command}));
        msgBuf.put(EndOfMessage);
        LOG.debug("DEBUG: " + hexdump(msgBuf.array()));
        return msgBuf.array();
    }

    byte[] encodeMessage(byte command, byte parameter) {
        ByteBuffer msgBuf = ByteBuffer.allocate(8);
        msgBuf.order(ByteOrder.LITTLE_ENDIAN);
        msgBuf.put(StartOfMessage);
        byte size = 0x4;
        if (StartOfMessage == SOM_BUDS_PLUS) {
            msgBuf.put((byte) size);
            msgBuf.put((byte) 0x0); //0x0 for sending
        } else {
            msgBuf.put((byte) 0x0); //0x0 for sending
            msgBuf.put((byte) size); //size
        }
        msgBuf.put((byte) command); //command id
        msgBuf.put((byte) parameter);
        msgBuf.putShort((short) crc16_ccitt(new byte[]{command, parameter}));
        msgBuf.put(EndOfMessage);
        LOG.debug("DEBUG: " + hexdump(msgBuf.array()));
        return msgBuf.array();
    }

    byte[] encodeMessage(byte command, byte parameter, byte value) {
        ByteBuffer msgBuf = ByteBuffer.allocate(9);
        msgBuf.order(ByteOrder.LITTLE_ENDIAN);
        msgBuf.put(StartOfMessage);
        byte size = 0x5;
        if (StartOfMessage == SOM_BUDS_PLUS) {
            msgBuf.put((byte) size);
            msgBuf.put((byte) 0x0); //0x0 for sending
        } else {
            msgBuf.put((byte) 0x0); //0x0 for sending
            msgBuf.put((byte) size); //size
        }
        msgBuf.put((byte) command);
        msgBuf.put((byte) parameter);
        msgBuf.put((byte) value);
        msgBuf.putShort((short) crc16_ccitt(new byte[]{command, parameter, value}));
        msgBuf.put(EndOfMessage);
        LOG.debug("DEBUG: " + hexdump(msgBuf.array()));
        return msgBuf.array();
    }

    byte[] encodeMessage(byte command, byte[] payload) {
        byte payload_size = (byte) (3 + payload.length);
        ByteBuffer msgBuf = ByteBuffer.allocate(4 + payload_size);
        msgBuf.order(ByteOrder.LITTLE_ENDIAN);
        msgBuf.put(StartOfMessage);

        if (StartOfMessage == SOM_BUDS_PLUS) {
            msgBuf.put((byte) payload_size);
            msgBuf.put((byte) 0x0); //0x0 for sending
        } else {
            msgBuf.put((byte) 0x0); //0x0 for sending
            msgBuf.put((byte) payload_size); //size
        }
        msgBuf.put((byte) command);
        msgBuf.put(payload);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            outputStream.write(command);
            outputStream.write(payload);
        } catch (IOException e) {
            LOG.warn("Assembling message failed: " + e.getMessage());
        }

        msgBuf.putShort((short) crc16_ccitt(outputStream.toByteArray()));
        msgBuf.put(EndOfMessage);
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
            case DeviceSettingsPreferenceConst.PREF_GALAXY_PRO_DOUBLE_TAP_EDGE:
                byte outside_double_tap = (byte) (prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_GALAXY_PRO_DOUBLE_TAP_EDGE, false) ? 0x01 : 0x00);
                return encodeMessage(set_outside_double_tap, outside_double_tap);

            case DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_PRO_VOICE_DETECT:
                byte detect_conversations = (byte) (prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_PRO_VOICE_DETECT, false) ? 0x01 : 0x00);
                return encodeMessage(set_detect_conversations, detect_conversations);

            case DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_PRO_VOICE_DETECT_DURATION:
                String voice_detect_duration = prefs.getString(DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_PRO_VOICE_DETECT_DURATION, "1");
                byte voice_detect_duration_b = (byte) Integer.parseInt(voice_detect_duration);
                return encodeMessage(set_detect_conversations_duration, voice_detect_duration_b);


            case DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_AMBIENT_MODE_DURING_CALL:
                byte ambient_mode_during_calls = (byte) (prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_AMBIENT_MODE_DURING_CALL, false) ? 0x01 : 0x00);
                return encodeMessage(set_ambient_mode_during_calls, ambient_mode_during_calls);

            case DeviceSettingsPreferenceConst.PREFS_NOISE_CONTROL_WITH_ONE_EARBUD:
                byte noise_controls_with_one_earbud = (byte) (prefs.getBoolean(DeviceSettingsPreferenceConst.PREFS_NOISE_CONTROL_WITH_ONE_EARBUD, false) ? 0x01 : 0x00);
                return encodeMessage(set_noise_controls_with_one_earbud, noise_controls_with_one_earbud);

            case DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_PRO_IN_EAR_DETECTION:
                byte ear_detection = (byte) (prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_PRO_IN_EAR_DETECTION, false) ? 0x01 : 0x00);
                return encodeMessage(in_ear_detection, ear_detection);

            case DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_PRO_BALANCE:
                int hearing_enhancements = prefs.getInt(DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_PRO_BALANCE, 16);
                return encodeMessage(set_balance, (byte) hearing_enhancements);

            case DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_PRO_NOISE_CONTROL:
                int noise_controls = Integer.parseInt(prefs.getString(DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_PRO_NOISE_CONTROL, "0"));
                return encodeMessage(set_noise_controls, (byte) noise_controls);

            case DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_2_NOISE_CONTROL:
                int b2_noise_controls = Integer.parseInt(prefs.getString(DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_2_NOISE_CONTROL, "0"));
                return encodeMessage(set_noise_controls, (byte) b2_noise_controls);

            case DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_PRO_ANC_LEVEL:
                int anc_level = Integer.parseInt(prefs.getString(DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_PRO_ANC_LEVEL, "0"));
                return encodeMessage(set_noise_reduction_level, (byte) anc_level);

            case DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_AMBIENT_SOUND:
            case DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_PRO_AMBIENT_VOLUME_RIGHT:
            case DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_PRO_AMBIENT_VOLUME_LEFT:
            case DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_PRO_AMBIENT_SOUND_TONE:
                byte ambient_sound = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_AMBIENT_SOUND, true) ? (byte) 1 : (byte) 0;
                int ambient_right = prefs.getInt(DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_PRO_AMBIENT_VOLUME_RIGHT, 1);
                int ambient_left = prefs.getInt(DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_PRO_AMBIENT_VOLUME_LEFT, 1);
                int sound_tone = prefs.getInt(DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_PRO_AMBIENT_SOUND_TONE, 1);
                return encodeMessage(set_customize_ambient_sound, new byte[]{ambient_sound, (byte) ambient_left, (byte) ambient_right, (byte) sound_tone});

            case DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_AMBIENT_MODE:
                byte enable_ambient = (byte) (prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_AMBIENT_MODE, false) ? 0x01 : 0x00);
                return encodeMessage(set_ambient_mode, enable_ambient);

            case DeviceSettingsPreferenceConst.PREFS_GALAXY_BUDS_SEAMLESS_CONNECTION:
                byte seamless_switch = (byte) (prefs.getBoolean(DeviceSettingsPreferenceConst.PREFS_GALAXY_BUDS_SEAMLESS_CONNECTION, false) ? 0x00 : 0x01);
                return encodeMessage(set_seamless_connection, seamless_switch);

            case DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_AMBIENT_VOICE_FOCUS:
                byte enable_voice = (byte) (prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_AMBIENT_VOICE_FOCUS, false) ? 0x01 : 0x00);
                return encodeMessage(set_ambient_voice_focus, enable_voice);
            case DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_AMBIENT_VOLUME:
                int ambient_volume = prefs.getInt(DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_AMBIENT_VOLUME, 0);
                byte ambient_volume_byte = (byte) (ambient_volume);
                return encodeMessage(set_ambient_volume, ambient_volume_byte);
            case DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_LOCK_TOUCH:
                byte set_lock = (byte) (prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_LOCK_TOUCH, false) ? 0x01 : 0x00);
                return encodeMessage(set_lock_touch, set_lock);
            case DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_GAME_MODE:
                if (StartOfMessage == SOM_BUDS_PLUS) {
                    byte game_mode = (byte) (prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_GAME_MODE, false) ? 0x1 : 0x00);
                    return encodeMessage(set_live_pro_game_mode, game_mode);
                } else {
                    byte game_mode = (byte) (prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_GAME_MODE, false) ? 0x2 : 0x00);
                    return encodeMessage(set_game_mode, game_mode);
                }

            case DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_EQUALIZER:
            case DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_EQUALIZER_DOLBY:
            case DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_EQUALIZER_MODE:
                byte equalizer = (byte) (prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_EQUALIZER, false) ? 0x1 : 0x00);
                boolean equalizer_dolby = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_EQUALIZER_DOLBY, false);
                int dolby = 0;
                if (!equalizer_dolby) {
                    dolby = 5;
                }
                String equalizer_mode = prefs.getString(DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_EQUALIZER_MODE, "0");

                if (StartOfMessage == SOM_BUDS_PLUS) {
                    return encodeMessage(set_equalizer, (byte) (Integer.parseInt(equalizer_mode)));
                } else {
                    byte mode = (byte) (Integer.parseInt(equalizer_mode) + dolby);
                    return encodeMessage(set_equalizer, equalizer, mode);
                }

            case DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_TOUCH_LEFT:
            case DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_TOUCH_RIGHT:
                String touch_left = prefs.getString(DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_TOUCH_LEFT, "1");
                String touch_right = prefs.getString(DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_TOUCH_RIGHT, "1");
                byte touchmode_left = (byte) Integer.parseInt(touch_left);
                byte touchmode_right = (byte) Integer.parseInt(touch_right);
                return encodeMessage(set_touchpad_options, touchmode_left, touchmode_right);

            case DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_TOUCH_RIGHT_SWITCH:
            case DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_TOUCH_LEFT_SWITCH:
                String touch_right_switch = prefs.getString(DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_TOUCH_RIGHT_SWITCH, "1");
                String touch_left_switch = prefs.getString(DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_TOUCH_LEFT_SWITCH, "1");
                byte[] touch_right_switch_b = encode_touch_switch(touch_right_switch);
                byte[] touch_left_switch_b = encode_touch_switch(touch_left_switch);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                try {
                    outputStream.write(touch_left_switch_b);
                    outputStream.write(touch_right_switch_b);
                } catch (IOException e) {
                    LOG.warn("Assembling message failed: " + e.getMessage());
                }
                return encodeMessage(set_touch_and_hold_noise_controls, outputStream.toByteArray());


            case DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_LIVE_ANC:
                byte enable_anc = (byte) (prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_LIVE_ANC, false) ? 0x1 : 0x00);
                return encodeMessage(set_automatic_noise_cancelling, enable_anc);

            case DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_PRESSURE_RELIEF:
                byte enable_pressure_relief = (byte) (prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_GALAXY_BUDS_PRESSURE_RELIEF, false) ? 0x1 : 0x00);
                return encodeMessage(set_pressure_relief, enable_pressure_relief);

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
        LOG.debug("p2: " + payload[5]);

        int batteryLevel1 = payload[0];
        int batteryLevel2 = payload[1];
        int batteryLevel3 = payload[5];

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


        if (StartOfMessage == SOM_BUDS_PLUS) {
            GBDeviceEventBatteryInfo evBattery3 = new GBDeviceEventBatteryInfo();
            // reorder for the non OG version
            evBattery1.batteryIndex = 1; //left
            evBattery2.batteryIndex = 2; //right
            evBattery3.batteryIndex = 0; //case

            evBattery3.level = GBDevice.BATTERY_UNKNOWN;
            evBattery3.level = (batteryLevel3 > 0) ? batteryLevel3 : GBDevice.BATTERY_UNKNOWN;
            evBattery3.state = (batteryLevel3 > 0) ? BatteryState.BATTERY_NORMAL : BatteryState.UNKNOWN;
            deviceEvents.add(evBattery3);
        }

        return deviceEvents;
    }

    private byte[] encode_touch_switch(String input) {
        switch (input) {
            case "1":
                return new byte[]{0x1, 0x0, 0x1};
            case "2":
                return new byte[]{0x0, 0x1, 0x1};
            default:
                return new byte[]{0x1, 0x1, 0x0};
        }
    }

    protected GalaxyBudsProtocol(GBDevice device) {
        super(device);
        DeviceType type = device.getType();
        if (type.equals(DeviceType.GALAXY_BUDS_LIVE)
                || type.equals(DeviceType.GALAXY_BUDS_PRO)
                || type.equals(DeviceType.GALAXY_BUDS2)
                || type.equals(DeviceType.GALAXY_BUDS2_PRO)) {
            StartOfMessage = SOM_BUDS_PLUS;
            EndOfMessage = EOM_BUDS_PLUS;
        }
    }
}
