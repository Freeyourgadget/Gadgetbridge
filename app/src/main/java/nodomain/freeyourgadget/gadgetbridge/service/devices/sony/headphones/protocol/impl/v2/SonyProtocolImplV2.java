/*  Copyright (C) 2022 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.impl.v2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdateDeviceInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.AmbientSoundControl;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.AudioUpsampling;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.AutomaticPowerOff;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.ButtonModes;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.EqualizerCustomBands;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.EqualizerPreset;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.PauseWhenTakenOff;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.SoundPosition;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.SurroundMode;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.TouchSensor;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.VoiceNotifications;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.MessageType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.Request;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.impl.v1.PayloadTypeV1;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.impl.v1.SonyProtocolImplV1;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.impl.v1.params.AudioCodec;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.impl.v1.params.BatteryType;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class SonyProtocolImplV2 extends SonyProtocolImplV1 {
    private static final Logger LOG = LoggerFactory.getLogger(SonyProtocolImplV2.class);

    public SonyProtocolImplV2(final GBDevice device) {
        super(device);
    }

    @Override
    public Request getAmbientSoundControl() {
        return new Request(
                PayloadTypeV1.AMBIENT_SOUND_CONTROL_GET.getMessageType(),
                new byte[]{
                        PayloadTypeV1.AMBIENT_SOUND_CONTROL_GET.getCode(),
                        (byte) 0x15
                }
        );
    }

    @Override
    public Request setAmbientSoundControl(final AmbientSoundControl ambientSoundControl) {
        final ByteBuffer buf = ByteBuffer.allocate(8);

        buf.put(PayloadTypeV1.AMBIENT_SOUND_CONTROL_SET.getCode());
        buf.put((byte) 0x15);
        buf.put((byte) 0x01); // 0x00 while dragging the slider?

        if (AmbientSoundControl.Mode.OFF.equals(ambientSoundControl.getMode())) {
            buf.put((byte) 0x00);
        } else {
            buf.put((byte) 0x01);
        }

        if (AmbientSoundControl.Mode.AMBIENT_SOUND.equals(ambientSoundControl.getMode())) {
            buf.put((byte) 0x01);
        } else {
            buf.put((byte) 0x00);
        }

        if (AmbientSoundControl.Mode.WIND_NOISE_REDUCTION.equals(ambientSoundControl.getMode())) {
            buf.put((byte) 0x03);
        } else {
            buf.put((byte) 0x02);
        }

        buf.put((byte) (ambientSoundControl.isFocusOnVoice() ? 0x01 : 0x00));
        buf.put((byte) (ambientSoundControl.getAmbientSound()));

        return new Request(PayloadTypeV1.AMBIENT_SOUND_CONTROL_SET.getMessageType(), buf.array());
    }

    @Override
    public Request getNoiseCancellingOptimizerState() {
        LOG.warn("Noise cancelling optimizer not implemented for V2");
        return null;
    }

    @Override
    public Request getAudioCodec() {
        return new Request(
                PayloadTypeV2.AUDIO_CODEC_REQUEST.getMessageType(),
                new byte[]{
                        PayloadTypeV2.AUDIO_CODEC_REQUEST.getCode(),
                        (byte) 0x00
                }
        );
    }

    @Override
    public Request getBattery(final BatteryType batteryType) {
        return new Request(
                PayloadTypeV2.BATTERY_LEVEL_REQUEST.getMessageType(),
                new byte[]{
                        PayloadTypeV2.BATTERY_LEVEL_REQUEST.getCode(),
                        encodeBatteryType(batteryType)
                }
        );
    }

    @Override
    public Request getFirmwareVersion() {
        return super.getFirmwareVersion();
    }

    @Override
    public Request getAudioUpsampling() {
        return new Request(
                PayloadTypeV1.AUDIO_UPSAMPLING_GET.getMessageType(),
                new byte[]{
                        PayloadTypeV1.AUDIO_UPSAMPLING_GET.getCode(),
                        (byte) 0x01
                }
        );
    }

    @Override
    public Request setAudioUpsampling(final AudioUpsampling config) {
        return new Request(
                PayloadTypeV1.AUDIO_UPSAMPLING_SET.getMessageType(),
                new byte[]{
                        PayloadTypeV1.AUDIO_UPSAMPLING_SET.getCode(),
                        (byte) 0x01,
                        (byte) (config.isEnabled() ? 0x01 : 0x00)
                }
        );
    }

    @Override
    public Request getAutomaticPowerOff() {
        return new Request(
                PayloadTypeV2.AUTOMATIC_POWER_OFF_GET.getMessageType(),
                new byte[]{
                        PayloadTypeV2.AUTOMATIC_POWER_OFF_GET.getCode(),
                        (byte) 0x05
                }
        );
    }

    @Override
    public Request setAutomaticPowerOff(final AutomaticPowerOff config) {
        return new Request(
                PayloadTypeV2.AUTOMATIC_POWER_OFF_SET.getMessageType(),
                new byte[]{
                        PayloadTypeV2.AUTOMATIC_POWER_OFF_SET.getCode(),
                        (byte) 0x05,
                        config.getCode()[0],
                        config.getCode()[1]
                }
        );
    }

    @Override
    public Request getButtonModes() {
        return new Request(
                PayloadTypeV1.AUTOMATIC_POWER_OFF_BUTTON_MODE_GET.getMessageType(),
                new byte[]{
                        PayloadTypeV1.AUTOMATIC_POWER_OFF_BUTTON_MODE_GET.getCode(),
                        (byte) 0x03
                }
        );
    }

    @Override
    public Request setButtonModes(final ButtonModes config) {
        return new Request(
                PayloadTypeV1.AUTOMATIC_POWER_OFF_BUTTON_MODE_SET.getMessageType(),
                new byte[]{
                        PayloadTypeV1.AUTOMATIC_POWER_OFF_BUTTON_MODE_SET.getCode(),
                        (byte) 0x03,
                        (byte) 0x02,
                        config.getModeLeft().getCode(),
                        config.getModeRight().getCode()
                }
        );
    }

    @Override
    public Request getPauseWhenTakenOff() {
        return new Request(
                PayloadTypeV2.AUTOMATIC_POWER_OFF_GET.getMessageType(),
                new byte[]{
                        PayloadTypeV2.AUTOMATIC_POWER_OFF_GET.getCode(),
                        (byte) 0x01
                }
        );
    }

    @Override
    public Request setPauseWhenTakenOff(final PauseWhenTakenOff config) {
        return new Request(
                PayloadTypeV1.AUTOMATIC_POWER_OFF_BUTTON_MODE_SET.getMessageType(),
                new byte[]{
                        PayloadTypeV1.AUTOMATIC_POWER_OFF_BUTTON_MODE_SET.getCode(),
                        (byte) 0x01,
                        (byte) (config.isEnabled() ? 0x00 : 0x01) // this is reversed on V2...?
                }
        );
    }

    @Override
    public Request getEqualizer() {
        return new Request(
                PayloadTypeV1.EQUALIZER_GET.getMessageType(),
                new byte[]{
                        PayloadTypeV1.EQUALIZER_GET.getCode(),
                        (byte) 0x00
                }
        );
    }

    @Override
    public Request setEqualizerPreset(final EqualizerPreset config) {
        return new Request(
                PayloadTypeV1.EQUALIZER_SET.getMessageType(),
                new byte[]{
                        PayloadTypeV1.EQUALIZER_SET.getCode(),
                        (byte) 0x00,
                        config.getCode(),
                        (byte) 0x00
                }
        );
    }

    @Override
    public Request setEqualizerCustomBands(final EqualizerCustomBands config) {
        LOG.warn("Equalizer custom bands not implemented for V2");
        return null;
    }

    @Override
    public Request getSoundPosition() {
        LOG.warn("Sound position not implemented for V2");
        return null;
    }

    @Override
    public Request setSoundPosition(final SoundPosition config) {
        LOG.warn("Sound position not implemented for V2");
        return null;
    }

    @Override
    public Request getSurroundMode() {
        LOG.warn("Surround mode not implemented for V2");
        return null;
    }

    @Override
    public Request setSurroundMode(final SurroundMode config) {
        LOG.warn("Surround mode not implemented for V2");
        return null;
    }

    @Override
    public Request getTouchSensor() {
        LOG.warn("Touch sensor not implemented for V2");
        return null;
    }

    @Override
    public Request setTouchSensor(final TouchSensor config) {
        LOG.warn("Touch sensor not implemented for V2");
        return null;
    }

    @Override
    public Request getVoiceNotifications() {
        LOG.warn("Voice notifications not implemented for V2");
        return null;
    }

    @Override
    public Request setVoiceNotifications(final VoiceNotifications config) {
        LOG.warn("Voice notifications not implemented for V2");
        return null;
    }

    @Override
    public Request startNoiseCancellingOptimizer(final boolean start) {
        LOG.warn("Noise cancelling optimizer not implemented for V2");
        return null;
    }

    @Override
    public Request powerOff() {
        LOG.warn("Power off not implemented for V2");
        return null;
    }

    @Override
    public List<? extends GBDeviceEvent> handlePayload(final MessageType messageType, final byte[] payload) {
        final PayloadTypeV2 payloadType = PayloadTypeV2.fromCode(messageType, payload[0]);

        switch (payloadType) {
            case AUDIO_CODEC_NOTIFY:
                return handleAudioCodec(payload);
            case BATTERY_LEVEL_NOTIFY:
            case BATTERY_LEVEL_REPLY:
                return handleBattery(payload);
            case AUTOMATIC_POWER_OFF_RET:
            case AUTOMATIC_POWER_OFF_NOTIFY:
                return handleAutomaticPowerOff(payload);
        }

        return super.handlePayload(messageType, payload);
    }

    @Override
    public List<? extends GBDeviceEvent> handleInitResponse(final byte[] payload) {
        return super.handleInitResponse(payload);
    }

    @Override
    public List<? extends GBDeviceEvent> handleAmbientSoundControl(final byte[] payload) {
        if (payload.length != 8) {
            LOG.warn("Unexpected payload length {}", payload.length);
            return Collections.emptyList();
        }

        if (payload[1] != 0x15) {
            LOG.warn("Not ambient sound control, ignoring {}", payload[1]);
            return Collections.emptyList();
        }

        AmbientSoundControl.Mode mode = null;

        if (payload[3] == (byte) 0x00) {
            mode = AmbientSoundControl.Mode.OFF;
        } else if (payload[3] == (byte) 0x01) {
            // Enabled, determine mode

            if (payload[5] == 0x03 || payload[5] == 0x05) {
                mode = AmbientSoundControl.Mode.WIND_NOISE_REDUCTION;
            } else if (payload[5] == 0x02) {
                if (payload[4] == (byte) 0x00) {
                    mode = AmbientSoundControl.Mode.NOISE_CANCELLING;
                } else if (payload[4] == (byte) 0x01) {
                    mode = AmbientSoundControl.Mode.AMBIENT_SOUND;
                }
            }
        }

        if (mode == null) {
            LOG.warn("Unable to determine ambient sound control mode from {}", GB.hexdump(payload));
            return Collections.emptyList();
        }

        final Boolean focusOnVoice = booleanFromByte(payload[6]);
        if (focusOnVoice == null) {
            LOG.warn("Unknown focus on voice mode {}", String.format("%02x", payload[6]));
            return Collections.emptyList();
        }

        int ambientSound = payload[7];
        if (ambientSound < 0 || ambientSound > 20) {
            LOG.warn("Ambient sound level {} is out of range", String.format("%02x", payload[7]));
            return Collections.emptyList();
        }

        final AmbientSoundControl ambientSoundControl = new AmbientSoundControl(mode, focusOnVoice, ambientSound);

        LOG.warn("Ambient sound control: {}", ambientSoundControl);

        final GBDeviceEventUpdatePreferences eventUpdatePreferences = new GBDeviceEventUpdatePreferences()
                .withPreferences(ambientSoundControl.toPreferences());

        return Collections.singletonList(eventUpdatePreferences);
    }

    @Override
    public List<? extends GBDeviceEvent> handleNoiseCancellingOptimizerStatus(final byte[] payload) {
        LOG.warn("Touch sensor not implemented for V2");
        return Collections.emptyList();
    }

    @Override
    public List<? extends GBDeviceEvent> handleNoiseCancellingOptimizerState(final byte[] payload) {
        LOG.warn("Touch sensor not implemented for V2");
        return Collections.emptyList();
    }


    @Override
    public List<? extends GBDeviceEvent> handleAudioUpsampling(final byte[] payload) {
        if (payload.length != 3) {
            LOG.warn("Unexpected payload length {}", payload.length);
            return Collections.emptyList();
        }

        if (payload[1] != 0x01) {
            LOG.warn("Not audio upsampling, ignoring {}", payload[1]);
            return Collections.emptyList();
        }

        final Boolean enabled = booleanFromByte(payload[2]);
        if (enabled == null) {
            LOG.warn("Unknown audio upsampling code {}", String.format("%02x", payload[2]));
            return Collections.emptyList();
        }

        LOG.debug("Audio Upsampling: {}", enabled);

        final GBDeviceEventUpdatePreferences event = new GBDeviceEventUpdatePreferences()
                .withPreferences(new AudioUpsampling(enabled).toPreferences());

        return Collections.singletonList(event);
    }

    @Override
    public List<? extends GBDeviceEvent> handleAutomaticPowerOff(final byte[] payload) {
        if (payload.length != 4) {
            LOG.warn("Unexpected payload length {}", payload.length);
            return Collections.emptyList();
        }

        if (payload[1] != 0x05) {
            LOG.warn("Not automatic power off config, ignoring");
            return Collections.emptyList();
        }

        final AutomaticPowerOff mode = AutomaticPowerOff.fromCode(payload[2], payload[3]);
        if (mode == null) {
            LOG.warn("Unknown automatic power off codes {}", String.format("%02x %02x", payload[3], payload[4]));
            return Collections.emptyList();
        }

        LOG.debug("Automatic Power Off: {}", mode);

        final GBDeviceEventUpdatePreferences event = new GBDeviceEventUpdatePreferences()
                .withPreferences(mode.toPreferences());

        return Collections.singletonList(event);
    }

    @Override
    public List<? extends GBDeviceEvent> handleButtonModes(final byte[] payload) {
        if (payload.length != 5) {
            LOG.warn("Unexpected payload length {}", payload.length);
            return Collections.emptyList();
        }

        if (payload[1] != 0x03) {
            LOG.warn("Not button mode config, ignoring");
            return Collections.emptyList();
        }

        final ButtonModes.Mode modeLeft = ButtonModes.Mode.fromCode(payload[3]);
        final ButtonModes.Mode modeRight = ButtonModes.Mode.fromCode(payload[4]);

        if (modeLeft == null || modeRight == null) {
            LOG.warn("Unknown button mode codes {}", String.format("%02x %02x", payload[3], payload[4]));
            return Collections.emptyList();
        }

        LOG.debug("Button Modes: L: {}, R: {}", modeLeft, modeRight);

        final GBDeviceEventUpdatePreferences event = new GBDeviceEventUpdatePreferences()
                .withPreferences(new ButtonModes(modeLeft, modeRight).toPreferences());

        return Collections.singletonList(event);
    }

    @Override
    public List<? extends GBDeviceEvent> handlePauseWhenTakenOff(final byte[] payload) {
        if (payload.length != 3) {
            LOG.warn("Unexpected payload length {}", payload.length);
            return Collections.emptyList();
        }

        if (payload[1] != 0x01) {
            LOG.warn("Not pause when taken off, ignoring");
            return Collections.emptyList();
        }

        final Boolean disabled = booleanFromByte(payload[2]);
        if (disabled == null) {
            LOG.warn("Unknown pause when taken off code {}", String.format("%02x", payload[2]));
            return Collections.emptyList();
        }

        LOG.debug("Pause when taken off: {}", !disabled);

        final GBDeviceEventUpdatePreferences event = new GBDeviceEventUpdatePreferences()
                .withPreferences(new PauseWhenTakenOff(!disabled).toPreferences());

        return Collections.singletonList(event);
    }

    @Override
    public List<? extends GBDeviceEvent> handleBattery(final byte[] payload) {
        return super.handleBattery(payload);
    }

    @Override
    public List<? extends GBDeviceEvent> handleAudioCodec(final byte[] payload) {
        if (payload.length != 3) {
            LOG.warn("Unexpected payload length {}", payload.length);
            return Collections.emptyList();
        }

        if (payload[1] != 0x03) {
            LOG.warn("Not audio codec, ignoring");
            return Collections.emptyList();
        }

        final AudioCodec audioCodec = AudioCodec.fromCode(payload[2]);
        if (audioCodec == null) {
            LOG.warn("Unable to determine audio codec from {}", GB.hexdump(payload));
            return Collections.emptyList();
        }

        final GBDeviceEventUpdateDeviceInfo gbDeviceEventUpdateDeviceInfo = new GBDeviceEventUpdateDeviceInfo("AUDIO_CODEC: ", audioCodec.name());

        final GBDeviceEventUpdatePreferences gbDeviceEventUpdatePreferences = new GBDeviceEventUpdatePreferences()
                .withPreference(DeviceSettingsPreferenceConst.PREF_SONY_AUDIO_CODEC, audioCodec.name().toLowerCase(Locale.getDefault()));

        return Arrays.asList(gbDeviceEventUpdateDeviceInfo, gbDeviceEventUpdatePreferences);
    }

    @Override
    public List<? extends GBDeviceEvent> handleEqualizer(final byte[] payload) {
        return super.handleEqualizer(payload);
    }

    @Override
    public List<? extends GBDeviceEvent> handleFirmwareVersion(final byte[] payload) {
        return super.handleFirmwareVersion(payload);
    }

    @Override
    public List<? extends GBDeviceEvent> handleJson(final byte[] payload) {
        LOG.warn("JSON not implemented for V2");
        return Collections.emptyList();
    }

    @Override
    public List<? extends GBDeviceEvent> handleAutomaticPowerOffButtonMode(final byte[] payload) {
        switch (payload[1]) {
            case 0x01:
                return handlePauseWhenTakenOff(payload);
            case 0x03:
                return handleButtonModes(payload);
        }

        return Collections.emptyList();
    }

    @Override
    public List<? extends GBDeviceEvent> handleVirtualSound(final byte[] payload) {
        LOG.warn("Virtual sound not implemented for V2");
        return Collections.emptyList();
    }

    @Override
    public List<? extends GBDeviceEvent> handleSoundPosition(final byte[] payload) {
        LOG.warn("Sound position not implemented for V2");
        return Collections.emptyList();
    }

    @Override
    public List<? extends GBDeviceEvent> handleSurroundMode(final byte[] payload) {
        LOG.warn("Surround mode not implemented for V2");
        return Collections.emptyList();
    }

    @Override
    public List<? extends GBDeviceEvent> handleTouchSensor(final byte[] payload) {
        LOG.warn("Touch sensor not implemented for V2");
        return Collections.emptyList();
    }

    @Override
    public List<? extends GBDeviceEvent> handleVoiceNotifications(final byte[] payload) {
        LOG.warn("Voice notifications not implemented for V2");
        return Collections.emptyList();
    }

    @Override
    protected BatteryType decodeBatteryType(final byte b) {
        switch (b) {
            case 0x09:
                return BatteryType.DUAL;
            case 0x0a:
                return BatteryType.CASE;
        }

        return null;
    }

    @Override
    protected byte encodeBatteryType(final BatteryType batteryType) {
        switch (batteryType) {
            case DUAL:
                return 0x09;
            case CASE:
            case SINGLE: // TODO: This is not the code for single, but we need to encode something
                return 0x0a;
        }

        throw new IllegalArgumentException("Unknown battery type " + batteryType);
    }
}
