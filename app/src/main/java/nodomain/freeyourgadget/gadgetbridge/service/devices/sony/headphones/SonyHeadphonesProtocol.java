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
package nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones;

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
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventSendBytes;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.AmbientSoundControl;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.AutomaticPowerOff;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.EqualizerCustomBands;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.EqualizerPreset;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.SoundPosition;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.SurroundMode;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public abstract class SonyHeadphonesProtocol extends GBDeviceProtocol {
    private static final Logger LOG = LoggerFactory.getLogger(SonyHeadphonesProtocol.class);

    /**
     * Packet format:
     * <p>
     * - PACKET_HEADER
     * - Command type? - almost always 0x0c or 0x0e?
     * - Sequence Number - needs to be updated with the one sent in the ACK responses
     * - 4-byte big endian int with number of bytes that will follow
     * - N bytes of data
     * - Checksum (1-byte sum, excluding header)
     * - PACKET_TRAILER
     * <p>
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

    private byte sequenceNumber = 0;

    public SonyHeadphonesProtocol(GBDevice device) {
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

        int payloadLength = ((message[2] << 24) & 0xFF000000) | ((message[3] << 16) & 0xFF0000) | ((message[4] << 8) & 0xFF00) | (message[5] & 0xFF);
        if (payloadLength != message.length - 7) {
            LOG.error("Unexpected payload length {}, expected {}", message.length - 7, payloadLength);
            return null;
        }

        if (message[0] == MSG_TYPE_ACK) {
            LOG.info("Received ACK: {}", hexdump);
            sequenceNumber = message[1];
            return new GBDeviceEvent[]{new GBDeviceEventSendBytes(encodeAck())};
        }

        LOG.warn("Unknown message: {}", hexdump);

        return null;
    }

    @Override
    public byte[] encodeSendConfiguration(String config) {
        final SharedPreferences prefs = GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress());
        EqualizerPreset equalizerPreset = EqualizerPreset.valueOf(prefs.getString(DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_MODE, "off").toUpperCase());

        switch (config) {
            case DeviceSettingsPreferenceConst.PREF_SONY_AMBIENT_SOUND_CONTROL:
            case DeviceSettingsPreferenceConst.PREF_SONY_FOCUS_VOICE:
            case DeviceSettingsPreferenceConst.PREF_SONY_AMBIENT_SOUND_LEVEL:
                String soundControl = prefs.getString(DeviceSettingsPreferenceConst.PREF_SONY_AMBIENT_SOUND_CONTROL, "noise_cancelling");
                boolean focusVoice = prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_SONY_FOCUS_VOICE, false);
                int level = prefs.getInt(DeviceSettingsPreferenceConst.PREF_SONY_AMBIENT_SOUND_LEVEL, 0);
                return encodeSoundControl(AmbientSoundControl.valueOf(soundControl.toUpperCase()), focusVoice, level);

            case DeviceSettingsPreferenceConst.PREF_SONY_SOUND_POSITION:
                return encodeSoundPosition(
                        SoundPosition.valueOf(prefs.getString(DeviceSettingsPreferenceConst.PREF_SONY_SOUND_POSITION, "off").toUpperCase())
                );

            case DeviceSettingsPreferenceConst.PREF_SONY_SURROUND_MODE:
                return encodeSurroundMode(
                        SurroundMode.valueOf(prefs.getString(DeviceSettingsPreferenceConst.PREF_SONY_SURROUND_MODE, "off").toUpperCase())
                );

            case DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_MODE:
                return encodeEqualizerPreset(equalizerPreset);

            case DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_MANUAL_BAND_400:
            case DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_MANUAL_BAND_1000:
            case DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_MANUAL_BAND_2500:
            case DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_MANUAL_BAND_6300:
            case DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_MANUAL_BAND_16000:
            case DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_MANUAL_CLEAR_BASS:
                int m_band1 = prefs.getInt(DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_MANUAL_BAND_400, 10) - 10;
                int m_band2 = prefs.getInt(DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_MANUAL_BAND_1000, 10) - 10;
                int m_band3 = prefs.getInt(DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_MANUAL_BAND_2500, 10) - 10;
                int m_band4 = prefs.getInt(DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_MANUAL_BAND_6300, 10) - 10;
                int m_band5 = prefs.getInt(DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_MANUAL_BAND_16000, 10) - 10;
                int m_bass = prefs.getInt(DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_MANUAL_CLEAR_BASS, 10) - 10;

                return encodeEqualizerCustomBands(new EqualizerCustomBands(Arrays.asList(m_band1, m_band2, m_band3, m_band4, m_band5), m_bass));

            case DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_CUSTOM_1_BAND_400:
            case DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_CUSTOM_1_BAND_1000:
            case DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_CUSTOM_1_BAND_2500:
            case DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_CUSTOM_1_BAND_6300:
            case DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_CUSTOM_1_BAND_16000:
            case DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_CUSTOM_1_CLEAR_BASS:
                int c1_band1 = prefs.getInt(DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_CUSTOM_1_BAND_400, 10) - 10;
                int c1_band2 = prefs.getInt(DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_CUSTOM_1_BAND_1000, 10) - 10;
                int c1_band3 = prefs.getInt(DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_CUSTOM_1_BAND_2500, 10) - 10;
                int c1_band4 = prefs.getInt(DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_CUSTOM_1_BAND_6300, 10) - 10;
                int c1_band5 = prefs.getInt(DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_CUSTOM_1_BAND_16000, 10) - 10;
                int c1_bass = prefs.getInt(DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_CUSTOM_1_CLEAR_BASS, 10) - 10;

                return encodeEqualizerCustomBands(new EqualizerCustomBands(Arrays.asList(c1_band1, c1_band2, c1_band3, c1_band4, c1_band5), c1_bass));

            case DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_CUSTOM_2_BAND_400:
            case DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_CUSTOM_2_BAND_1000:
            case DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_CUSTOM_2_BAND_2500:
            case DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_CUSTOM_2_BAND_6300:
            case DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_CUSTOM_2_BAND_16000:
            case DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_CUSTOM_2_CLEAR_BASS:
                int c2_band1 = prefs.getInt(DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_CUSTOM_2_BAND_400, 10) - 10;
                int c2_band2 = prefs.getInt(DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_CUSTOM_2_BAND_1000, 10) - 10;
                int c2_band3 = prefs.getInt(DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_CUSTOM_2_BAND_2500, 10) - 10;
                int c2_band4 = prefs.getInt(DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_CUSTOM_2_BAND_6300, 10) - 10;
                int c2_band5 = prefs.getInt(DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_CUSTOM_2_BAND_16000, 10) - 10;
                int c2_bass = prefs.getInt(DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_CUSTOM_2_CLEAR_BASS, 10) - 10;

                return encodeEqualizerCustomBands(new EqualizerCustomBands(Arrays.asList(c2_band1, c2_band2, c2_band3, c2_band4, c2_band5), c2_bass));

            case DeviceSettingsPreferenceConst.PREF_SONY_DSEE_HX:
                return encodeDSEEHX(prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_SONY_DSEE_HX, false));

            case DeviceSettingsPreferenceConst.PREF_SONY_TOUCH_SENSOR:
                return encodeTouchSensor(prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_SONY_TOUCH_SENSOR, true));

            case DeviceSettingsPreferenceConst.PREF_SONY_AUTOMATIC_POWER_OFF:
                return encodeAutomaticPowerOff(
                        AutomaticPowerOff.valueOf(prefs.getString(DeviceSettingsPreferenceConst.PREF_SONY_AUTOMATIC_POWER_OFF, "off").toUpperCase())
                );

            case DeviceSettingsPreferenceConst.PREF_SONY_NOTIFICATION_VOICE_GUIDE:
                return encodeVoiceNotifications(prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_SONY_NOTIFICATION_VOICE_GUIDE, true));

            default:
                LOG.warn("Unknown config '{}'", config);
        }

        return super.encodeSendConfiguration(config);
    }

    public byte[] encodeAck() {
        return encodeMessage(
                MSG_TYPE_ACK,
                new byte[]{}
        );
    }

    public byte[] encodeTriggerNoiseCancellingOptimizer() {
        // This successfully triggers the noise cancelling optimizer. However, we don't get the
        // optimization progress messages.

        return encodeMessage(
                (byte) 0x0c,
                new byte[]{(byte) 0x84, (byte) 0x01, (byte) 0x00, (byte) 0x01}
        );
    }

    private byte[] encodeSoundControl(AmbientSoundControl ambientSoundControl, boolean focusOnVoice, int ambientSound) {
        if (ambientSound < 0 || ambientSound > 19) {
            throw new IllegalArgumentException("Level must be between 0 and 19");
        }

        ByteBuffer buf = ByteBuffer.allocate(14);
        buf.order(ByteOrder.BIG_ENDIAN);

        buf.put((byte) 0x0c);
        buf.put(sequenceNumber);
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
        return encodeMessage(
                (byte) 0x0c,
                new byte[]{(byte) 0x48, CMD_SOUND_POSITION, position.code}
        );
    }

    private byte[] encodeSurroundMode(SurroundMode mode) {
        return encodeMessage(
                (byte) 0x0c,
                new byte[]{(byte) 0x48, CMD_SOUND_SURROUND, mode.code}
        );
    }

    private byte[] encodeEqualizerPreset(EqualizerPreset preset) {
        return encodeMessage(
                (byte) 0x0c,
                new byte[]{(byte) 0x58, (byte) 0x01, preset.code[0], preset.code[1]}
        );
    }

    private byte[] encodeEqualizerCustomBands(EqualizerCustomBands equalizer) {
        final ByteBuffer buf = ByteBuffer.allocate(10);

        buf.put((byte) 0x58);
        buf.put((byte) 0x01);
        buf.put((byte) 0xff);
        buf.put((byte) 0x06);

        buf.put((byte) (equalizer.getClearBass() + 10));
        for (final Integer band : equalizer.getBands()) {
            buf.put((byte) (band + 10));
        }

        return encodeMessage(
                (byte) 0x0c,
                buf.array()
        );
    }

    private byte[] encodeDSEEHX(boolean enabled) {
        return encodeMessage(
                (byte) 0x0c,
                new byte[]{(byte) 0xe8, (byte) 0x02, (byte) 0x00, (byte) (enabled ? 0x01 : 0x00)}
        );
    }

    private byte[] encodeTouchSensor(boolean enabled) {
        return encodeMessage(
                (byte) 0x0c,
                new byte[]{(byte) 0xd8, (byte) 0xd2, (byte) 0x01, (byte) (enabled ? 0x01 : 0x00)}
        );
    }

    private byte[] encodeAutomaticPowerOff(AutomaticPowerOff automaticPowerOff) {
        return encodeMessage(
                (byte) 0x0c,
                new byte[]{(byte) 0xf8, (byte) 0x04, (byte) 0x01, automaticPowerOff.code[0], automaticPowerOff.code[1]}
        );
    }

    private byte[] encodeVoiceNotifications(boolean enabled) {
        return encodeMessage(
                (byte) 0x0e,
                new byte[]{(byte) 0x48, (byte) 0x01, (byte) 0x01, (byte) (enabled ? 0x01 : 0x00)}
        );
    }

    private byte[] encodeMessage(byte type, byte[] content) {
        ByteBuffer buf = ByteBuffer.allocate(content.length + 6);
        buf.order(ByteOrder.BIG_ENDIAN);

        buf.put(type);
        buf.put(sequenceNumber);
        buf.putInt(content.length);
        buf.put(content);

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
