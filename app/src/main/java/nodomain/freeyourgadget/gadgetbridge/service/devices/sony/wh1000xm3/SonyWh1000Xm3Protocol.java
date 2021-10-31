/*  Copyright (C) 2021 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wh1000xm3;

import android.content.SharedPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.wh1000xm3.AmbientSoundControl;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.wh1000xm3.AutomaticPowerOff;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.wh1000xm3.EqualizerCustomBands;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.wh1000xm3.EqualizerPreset;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.wh1000xm3.SoundPosition;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.wh1000xm3.SurroundMode;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class SonyWh1000Xm3Protocol extends GBDeviceProtocol {
    private static final Logger LOG = LoggerFactory.getLogger(SonyWh1000Xm3Protocol.class);

    /**
     * Packet format:
     *
     * - PACKET_HEADER
     * - Command type? - almost always 0x0c?
     * - Sequence Number - needs to be updated with the one sent in the responses
     * - 4-byte big endian int with number of bytes that will follow
     * - N bytes of data
     * - Checksum (1-byte sum, excluding header)
     * - PACKET_TRAILER
     *
     * Data between PACKET_HEADER and PACKET_TRAILER is escaped with PACKET_ESCAPE, and the
     * following byte masked with PACKET_ESCAPE_MASK.
     */

    public static final byte PACKET_HEADER = 0x3e;
    public static final byte PACKET_TRAILER = 0x3c;
    public static final byte PACKET_ESCAPE = 0x3d;
    public static final byte PACKET_ESCAPE_MASK = (byte) 0b11101111;

    private static final byte MSG_TYPE_ACK = 0x01;

    private static final byte CMD_SOUND_SURROUND = 0x01;
    private static final byte CMD_SOUND_POSITION = 0x02;

    private int sequenceNumber = 0;

    public SonyWh1000Xm3Protocol(GBDevice device) {
        super(device);
    }

    @Override
    public GBDeviceEvent[] decodeResponse(byte[] res) {
        byte[] message = unescape(res);
        String hexdump = GB.hexdump(message, 0, message.length);

        LOG.debug("Received {}", hexdump);

        byte messageChecksum = message[message.length - 1];
        byte expectedChecksum = calcChecksum(message, true);

        if (messageChecksum != expectedChecksum) {
            LOG.error("Invalid checksum for {}", hexdump);
            return null;
        }

        int payloadLength = ((message[2] << 24) & 0xFF000000) | ((message[3] << 16) & 0xFF0000) | ((message[4] << 8) & 0xFF00) | ((message[5] << 24) & 0xFF000000);
        if (payloadLength != message.length - 7) {
            LOG.error("Unexpected payload length {}, expected {}", message.length - 6, payloadLength);
            return null;
        }

        sequenceNumber = message[1];

        if (message[0] == MSG_TYPE_ACK) {
            LOG.info("Received ACK: {}", hexdump);
            return new GBDeviceEvent[]{};
        }

        LOG.warn("Unknown message: {}", hexdump);

        return null;
    }

    @Override
    public byte[] encodeSendConfiguration(String config) {
        final SharedPreferences prefs = GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress());
        EqualizerPreset equalizerPreset = EqualizerPreset.valueOf(prefs.getString(DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_EQUALIZER_MODE, "off").toUpperCase());

        switch (config) {
            case DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_AMBIENT_SOUND_CONTROL:
            case DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_FOCUS_VOICE:
            case DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_AMBIENT_SOUND_LEVEL:
                String soundControl = prefs.getString(DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_AMBIENT_SOUND_CONTROL, "noise_cancelling");
                boolean focusVoice = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_FOCUS_VOICE, false);
                int level = prefs.getInt(DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_AMBIENT_SOUND_LEVEL, 0);
                return encodeSoundControl(AmbientSoundControl.valueOf(soundControl.toUpperCase()), focusVoice, level);

            case DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_SOUND_POSITION:
                return encodeSoundPosition(
                        SoundPosition.valueOf(prefs.getString(DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_SOUND_POSITION, "off").toUpperCase())
                );

            case DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_SURROUND_MODE:
                return encodeSurroundMode(
                        SurroundMode.valueOf(prefs.getString(DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_SURROUND_MODE, "off").toUpperCase())
                );

            case DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_EQUALIZER_MODE:
                return encodeEqualizerPreset(equalizerPreset);

            case DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_EQUALIZER_MANUAL_BAND_400:
            case DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_EQUALIZER_MANUAL_BAND_1000:
            case DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_EQUALIZER_MANUAL_BAND_2500:
            case DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_EQUALIZER_MANUAL_BAND_6300:
            case DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_EQUALIZER_MANUAL_BAND_16000:
            case DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_EQUALIZER_MANUAL_CLEAR_BASS:
                int m_band1 = prefs.getInt(DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_EQUALIZER_MANUAL_BAND_400, 10) - 10;
                int m_band2 = prefs.getInt(DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_EQUALIZER_MANUAL_BAND_1000, 10) - 10;
                int m_band3 = prefs.getInt(DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_EQUALIZER_MANUAL_BAND_2500, 10) - 10;
                int m_band4 = prefs.getInt(DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_EQUALIZER_MANUAL_BAND_6300, 10) - 10;
                int m_band5 = prefs.getInt(DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_EQUALIZER_MANUAL_BAND_16000, 10) - 10;
                int m_bass = prefs.getInt(DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_EQUALIZER_MANUAL_CLEAR_BASS, 10) - 10;

                // Set the equalizer preset, since changing the bands will switch it
                // TODO: This is not updating the UI once the user returns to the previous screen
                prefs.edit().putString(DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_EQUALIZER_MODE, EqualizerPreset.MANUAL.toString().toLowerCase()).apply();

                return encodeEqualizerCustomBands(EqualizerPreset.MANUAL, equalizerPreset, new EqualizerCustomBands(Arrays.asList(m_band1, m_band2, m_band3, m_band4, m_band5), m_bass));

            case DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_EQUALIZER_CUSTOM_1_BAND_400:
            case DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_EQUALIZER_CUSTOM_1_BAND_1000:
            case DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_EQUALIZER_CUSTOM_1_BAND_2500:
            case DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_EQUALIZER_CUSTOM_1_BAND_6300:
            case DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_EQUALIZER_CUSTOM_1_BAND_16000:
            case DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_EQUALIZER_CUSTOM_1_CLEAR_BASS:
                int c1_band1 = prefs.getInt(DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_EQUALIZER_CUSTOM_1_BAND_400, 10) - 10;
                int c1_band2 = prefs.getInt(DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_EQUALIZER_CUSTOM_1_BAND_1000, 10) - 10;
                int c1_band3 = prefs.getInt(DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_EQUALIZER_CUSTOM_1_BAND_2500, 10) - 10;
                int c1_band4 = prefs.getInt(DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_EQUALIZER_CUSTOM_1_BAND_6300, 10) - 10;
                int c1_band5 = prefs.getInt(DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_EQUALIZER_CUSTOM_1_BAND_16000, 10) - 10;
                int c1_bass = prefs.getInt(DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_EQUALIZER_CUSTOM_1_CLEAR_BASS, 10) - 10;

                // Set the equalizer preset, since changing the bands will switch it
                // TODO: This is not updating the UI once the user returns to the previous screen
                prefs.edit().putString(DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_EQUALIZER_MODE, EqualizerPreset.CUSTOM_1.toString().toLowerCase()).apply();

                return encodeEqualizerCustomBands(EqualizerPreset.CUSTOM_1, equalizerPreset, new EqualizerCustomBands(Arrays.asList(c1_band1, c1_band2, c1_band3, c1_band4, c1_band5), c1_bass));

            case DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_EQUALIZER_CUSTOM_2_BAND_400:
            case DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_EQUALIZER_CUSTOM_2_BAND_1000:
            case DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_EQUALIZER_CUSTOM_2_BAND_2500:
            case DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_EQUALIZER_CUSTOM_2_BAND_6300:
            case DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_EQUALIZER_CUSTOM_2_BAND_16000:
            case DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_EQUALIZER_CUSTOM_2_CLEAR_BASS:
                int c2_band1 = prefs.getInt(DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_EQUALIZER_CUSTOM_2_BAND_400, 10) - 10;
                int c2_band2 = prefs.getInt(DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_EQUALIZER_CUSTOM_2_BAND_1000, 10) - 10;
                int c2_band3 = prefs.getInt(DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_EQUALIZER_CUSTOM_2_BAND_2500, 10) - 10;
                int c2_band4 = prefs.getInt(DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_EQUALIZER_CUSTOM_2_BAND_6300, 10) - 10;
                int c2_band5 = prefs.getInt(DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_EQUALIZER_CUSTOM_2_BAND_16000, 10) - 10;
                int c2_bass = prefs.getInt(DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_EQUALIZER_CUSTOM_2_CLEAR_BASS, 10) - 10;

                // Set the equalizer preset, since changing the bands will switch it
                // TODO: This is not updating the UI once the user returns to the previous screen
                prefs.edit().putString(DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_EQUALIZER_MODE, EqualizerPreset.CUSTOM_2.toString().toLowerCase()).apply();

                return encodeEqualizerCustomBands(EqualizerPreset.CUSTOM_2, equalizerPreset, new EqualizerCustomBands(Arrays.asList(c2_band1, c2_band2, c2_band3, c2_band4, c2_band5), c2_bass));

            case DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_DSEE_HX:
                return encodeDSEEHX(prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_DSEE_HX, false));

            case DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_TOUCH_SENSOR:
                return encodeTouchSensor(prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_TOUCH_SENSOR, true));

            case DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_AUTOMATIC_POWER_OFF:
                return encodeAutomaticPowerOff(
                        AutomaticPowerOff.valueOf(prefs.getString(DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_AUTOMATIC_POWER_OFF, "off").toUpperCase())
                );

            case DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_NOTIFICATION_VOICE_GUIDE:
                return encodeVoiceNotifications(prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_SONY_WH1000XM3_NOTIFICATION_VOICE_GUIDE, true));

            default:
                LOG.warn("Unknown config '{}'", config);
        }

        return super.encodeSendConfiguration(config);
    }

    public byte[] encodeTriggerNoiseCancellingOptimizer() {
        // This successfully triggers the noise cancelling optimizer. However, we don't get the
        // optimization progress messages.

        ByteBuffer buf = ByteBuffer.allocate(10);
        buf.order(ByteOrder.BIG_ENDIAN);

        buf.put((byte) 0x0c);
        buf.put((byte) sequenceNumber++);
        buf.putInt(4);

        buf.put((byte) 0x84);
        buf.put((byte) 0x01);
        buf.put((byte) 0x00);
        buf.put((byte) 0x01);

        return encodeMessage(buf.array());
    }

    private byte[] encodeSoundControl(AmbientSoundControl ambientSoundControl, boolean focusOnVoice, int ambientSound) {
        if (ambientSound < 0 || ambientSound > 19) {
            throw new IllegalArgumentException("Level must be between 0 and 19");
        }

        ByteBuffer buf = ByteBuffer.allocate(14);
        buf.order(ByteOrder.BIG_ENDIAN);

        buf.put((byte) 0x0c);
        buf.put((byte) sequenceNumber++);
        buf.putInt(8);

        buf.put((byte) 0x68);
        buf.put((byte) 0x02);
        if (AmbientSoundControl.OFF.equals(ambientSoundControl)) {
            buf.put((byte) 0x00);
        } else {
            buf.put((byte) 0x11);
        }
        buf.put((byte) 0x01);

        switch (ambientSoundControl) {
            case NOISE_CANCELLING:
                buf.put((byte) 2);
                break;
            case WIND_NOISE_REDUCTION:
                buf.put((byte) 1);
                break;
            case OFF:
            case AMBIENT_SOUND:
            default:
                buf.put((byte) 0);
                break;
        }

        buf.put((byte) 0x01);
        buf.put((byte) (focusOnVoice ? 0x01 : 0x00));
        buf.put((byte) (ambientSound + 1));

        return encodeMessage(buf.array());
    }

    private byte[] encodeSoundPosition(SoundPosition position) {
        final ByteBuffer buf = ByteBuffer.allocate(9);
        buf.order(ByteOrder.BIG_ENDIAN);

        buf.put((byte) 0x0c);
        buf.put((byte) sequenceNumber++);
        buf.putInt(3);

        buf.put((byte) 0x48);
        buf.put(CMD_SOUND_POSITION);

        switch (position) {
            case OFF:
                buf.put((byte) 0x00);
                break;
            case FRONT:
                buf.put((byte) 0x03);
                break;
            case FRONT_LEFT:
                buf.put((byte) 0x01);
                break;
            case FRONT_RIGHT:
                buf.put((byte) 0x02);
                break;
            case REAR_LEFT:
                buf.put((byte) 0x11);
                break;
            case REAR_RIGHT:
                buf.put((byte) 0x12);
                break;
        }

        return encodeMessage(buf.array());
    }

    private byte[] encodeSurroundMode(SurroundMode mode) {
        final ByteBuffer buf = ByteBuffer.allocate(9);
        buf.order(ByteOrder.BIG_ENDIAN);

        buf.put((byte) 0x0c);
        buf.put((byte) sequenceNumber++);
        buf.putInt(3);

        buf.put((byte) 0x48);
        buf.put(CMD_SOUND_SURROUND);

        switch (mode) {
            case OFF:
                buf.put((byte) 0x00);
                break;
            case ARENA:
                buf.put((byte) 0x02);
                break;
            case CLUB:
                buf.put((byte) 0x04);
                break;
            case OUTDOOR_STAGE:
                buf.put((byte) 0x01);
                break;
            case CONCERT_HALL:
                buf.put((byte) 0x03);
                break;
        }

        return encodeMessage(buf.array());
    }

    private byte[] encodeEqualizerPreset(EqualizerPreset preset) {
        final ByteBuffer buf = ByteBuffer.allocate(10);
        buf.order(ByteOrder.BIG_ENDIAN);

        buf.put((byte) 0x0c);
        buf.put((byte) sequenceNumber++);
        buf.putInt(4);

        buf.put((byte) 0x58);
        buf.put((byte) 0x01);

        switch (preset) {
            case OFF:
                buf.put((byte) 0x00).put((byte) 0x00);
                break;
            case BRIGHT:
                buf.put((byte) 0x10).put((byte) 0x00);
                break;
            case EXCITED:
                buf.put((byte) 0x11).put((byte) 0x00);
                break;
            case MELLOW:
                buf.put((byte) 0x12).put((byte) 0x00);
                break;
            case RELAXED:
                buf.put((byte) 0x13).put((byte) 0x00);
                break;
            case VOCAL:
                buf.put((byte) 0x14).put((byte) 0x00);
                break;
            case TREBLE_BOOST:
                buf.put((byte) 0x15).put((byte) 0x00);
                break;
            case BASS_BOOST:
                buf.put((byte) 0x16).put((byte) 0x00);
                break;
            case SPEECH:
                buf.put((byte) 0x17).put((byte) 0x00);
                break;
            case MANUAL:
                buf.put((byte) 0xa0).put((byte) 0x00);
                break;
            case CUSTOM_1:
                buf.put((byte) 0xa1).put((byte) 0x00);
                break;
            case CUSTOM_2:
                buf.put((byte) 0xa2).put((byte) 0x00);
                break;
        }

        return encodeMessage(buf.array());
    }

    private byte[] encodeEqualizerCustomBands(EqualizerPreset preset, EqualizerPreset previousPreset, EqualizerCustomBands equalizer) {
        ByteArrayOutputStream cmdStream = new ByteArrayOutputStream(16);

        try {
            if (preset != previousPreset) {
                // If we're not on the preset that is being changed, we need to swap to it
                cmdStream.write(encodeEqualizerPreset(preset));
            }

            cmdStream.write(encodeEqualizerCustomBands(equalizer));

            if (preset != previousPreset) {
                // And then we swap back to the previous preset
                // FIXME: this is not working, the new preset stays
                //cmdStream.write(encodeEqualizerPreset(previousPreset));
            }
        } catch (IOException e) {
            LOG.error("This should never happen", e);
        }

        return cmdStream.toByteArray();
    }

    private byte[] encodeEqualizerCustomBands(EqualizerCustomBands equalizer) {
        final ByteBuffer buf = ByteBuffer.allocate(16);
        buf.order(ByteOrder.BIG_ENDIAN);

        buf.put((byte) 0x0c);
        buf.put((byte) sequenceNumber++);
        buf.putInt(10);

        buf.put((byte) 0x58);
        buf.put((byte) 0x01);
        buf.put((byte) 0xff);
        buf.put((byte) 0x06);

        buf.put((byte) (equalizer.getClearBass() + 10));
        for (final Integer band : equalizer.getBands()) {
            buf.put((byte) (band + 10));
        }

        return encodeMessage(buf.array());
    }

    private byte[] encodeDSEEHX(boolean enabled) {
        ByteBuffer buf = ByteBuffer.allocate(10);
        buf.order(ByteOrder.BIG_ENDIAN);

        buf.put((byte) 0x0c);
        buf.put((byte) sequenceNumber++);
        buf.putInt(4);

        buf.put((byte) 0xe8);
        buf.put((byte) 0x02);
        buf.put((byte) 0x00);
        buf.put((byte) (enabled ? 0x01 : 0x00));

        return encodeMessage(buf.array());
    }

    private byte[] encodeTouchSensor(boolean enabled) {
        ByteBuffer buf = ByteBuffer.allocate(10);
        buf.order(ByteOrder.BIG_ENDIAN);

        buf.put((byte) 0x0c);
        buf.put((byte) sequenceNumber++);
        buf.putInt(4);

        buf.put((byte) 0xd8);
        buf.put((byte) 0xd2);
        buf.put((byte) 0x01);
        buf.put((byte) (enabled ? 0x01 : 0x00));

        return encodeMessage(buf.array());
    }

    private byte[] encodeAutomaticPowerOff(AutomaticPowerOff automaticPowerOff) {
        ByteBuffer buf = ByteBuffer.allocate(11);
        buf.order(ByteOrder.BIG_ENDIAN);

        buf.put((byte) 0x0c);
        buf.put((byte) sequenceNumber++);
        buf.putInt(5);

        buf.put((byte) 0xf8);
        buf.put((byte) 0x04);
        buf.put((byte) 0x01);

        switch (automaticPowerOff) {
            case OFF:
                buf.put((byte) 0x11).put((byte) 0x00);
                break;
            case AFTER_5_MIN:
                buf.put((byte) 0x00).put((byte) 0x00);
                break;
            case AFTER_30_MIN:
                buf.put((byte) 0x01).put((byte) 0x01);
                break;
            case AFTER_1_HOUR:
                buf.put((byte) 0x02).put((byte) 0x02);
                break;
            case AFTER_3_HOUR:
                buf.put((byte) 0x03).put((byte) 0x03);
                break;
        }

        return encodeMessage(buf.array());
    }

    private byte[] encodeVoiceNotifications(boolean enabled) {
        ByteBuffer buf = ByteBuffer.allocate(10);
        buf.order(ByteOrder.BIG_ENDIAN);

        buf.put((byte) 0x0e);
        buf.put((byte) sequenceNumber++);
        buf.putInt(4);

        buf.put((byte) 0x48);
        buf.put((byte) 0x01);
        buf.put((byte) 0x01);
        buf.put((byte) (enabled ? 0x01 : 0x00));

        return encodeMessage(buf.array());
    }

    private byte[] encodeMessage(byte[] message) {
        ByteArrayOutputStream cmdStream = new ByteArrayOutputStream(message.length + 2);

        cmdStream.write(PACKET_HEADER);

        byte checksum = calcChecksum(message, false);

        try {
            cmdStream.write(escape(message));
            cmdStream.write(escape(new byte[]{checksum}));
        } catch (IOException e) {
            LOG.error("This should never happen", e);
        }

        cmdStream.write(PACKET_TRAILER);

        return cmdStream.toByteArray();
    }

    private byte[] escape(byte[] bytes) {
        ByteArrayOutputStream escapedStream = new ByteArrayOutputStream(bytes.length);

        for (byte b : bytes) {
            switch (b) {
                case PACKET_HEADER:
                case PACKET_TRAILER:
                case PACKET_ESCAPE:
                    escapedStream.write(PACKET_ESCAPE);
                    escapedStream.write(b & PACKET_ESCAPE_MASK);
                    break;
                default:
                    escapedStream.write(b);
                    break;
            }
        }

        return escapedStream.toByteArray();
    }

    private byte[] unescape(byte[] bytes) {
        ByteArrayOutputStream unescapedStream = new ByteArrayOutputStream(bytes.length);

        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            if (b == PACKET_ESCAPE) {
                if (++i >= bytes.length) {
                    throw new IllegalArgumentException("Invalid escape character at end of array");
                }
                unescapedStream.write(b & ~PACKET_ESCAPE_MASK);
            } else {
                unescapedStream.write(b);
            }
        }

        return unescapedStream.toByteArray();
    }

    public byte calcChecksum(byte[] message, boolean ignoreLastByte) {
        int chk = 0;
        for (int i = 0; i < message.length - (ignoreLastByte ? 1 : 0); i++) {
            chk += message[i] & 255;
        }
        return (byte) chk;
    }
}
