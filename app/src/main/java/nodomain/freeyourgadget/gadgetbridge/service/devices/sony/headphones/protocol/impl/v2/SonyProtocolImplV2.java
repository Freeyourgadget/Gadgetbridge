/*  Copyright (C) 2022-2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.impl.v2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdateDeviceInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.SonyHeadphonesCapabilities;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.AdaptiveVolumeControl;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.AmbientSoundControl;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.AmbientSoundControlButtonMode;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.AudioUpsampling;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.AutomaticPowerOff;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.ButtonModes;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.EqualizerCustomBands;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.EqualizerPreset;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.PauseWhenTakenOff;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.QuickAccess;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.SoundPosition;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.SpeakToChatConfig;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.SpeakToChatEnabled;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.SurroundMode;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.TouchSensor;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.VoiceNotifications;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.WideAreaTap;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
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
                        (byte) (supportsWindNoiseCancelling() || getCoordinator().supports(SonyHeadphonesCapabilities.AmbientSoundControl2) ? 0x17 : 0x15)
                }
        );
    }

    @Override
    public Request setAmbientSoundControl(final AmbientSoundControl ambientSoundControl) {
        final ByteBuffer buf = ByteBuffer.allocate(supportsWindNoiseCancelling() ? 8 : 7);

        buf.put(PayloadTypeV1.AMBIENT_SOUND_CONTROL_SET.getCode());
        buf.put((byte) (supportsWindNoiseCancelling() || getCoordinator().supports(SonyHeadphonesCapabilities.AmbientSoundControl2) ? 0x17 : 0x15));
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

        if (supportsWindNoiseCancelling()) {
            if (AmbientSoundControl.Mode.WIND_NOISE_REDUCTION.equals(ambientSoundControl.getMode())) {
                buf.put((byte) 0x03);
            } else {
                buf.put((byte) 0x02);
            }
        }

        buf.put((byte) (ambientSoundControl.isFocusOnVoice() ? 0x01 : 0x00));
        buf.put((byte) (ambientSoundControl.getAmbientSound()));

        return new Request(PayloadTypeV1.AMBIENT_SOUND_CONTROL_SET.getMessageType(), buf.array());
    }

    @Override
    public Request setAdaptiveVolumeControl(final AdaptiveVolumeControl config) {
        return new Request(
                PayloadTypeV1.AUTOMATIC_POWER_OFF_BUTTON_MODE_SET.getMessageType(),
                new byte[]{
                        PayloadTypeV1.AUTOMATIC_POWER_OFF_BUTTON_MODE_SET.getCode(),
                        (byte) 0x0a,
                        (byte) (config.isEnabled() ? 0x00 : 0x01) // this is reversed on V2...?
                }
        );
    }

    @Override
    public Request getAdaptiveVolumeControl() {
        return new Request(
                PayloadTypeV1.AUTOMATIC_POWER_OFF_BUTTON_MODE_GET.getMessageType(),
                new byte[]{
                        PayloadTypeV1.AUTOMATIC_POWER_OFF_BUTTON_MODE_GET.getCode(),
                        (byte) 0x0a
                }
        );
    }

    @Override
    public Request setSpeakToChatEnabled(SpeakToChatEnabled config) {
        return new Request(
                PayloadTypeV1.AUTOMATIC_POWER_OFF_BUTTON_MODE_SET.getMessageType(),
                new byte[]{
                        PayloadTypeV1.AUTOMATIC_POWER_OFF_BUTTON_MODE_SET.getCode(),
                        (byte) 0x0c,
                        (byte) (config.isEnabled() ? 0x00 : 0x01), // TODO it's reversed?
                        (byte) 0x01
                }
        );
    }

    @Override
    public Request getSpeakToChatEnabled() {
        return new Request(
                PayloadTypeV1.AUTOMATIC_POWER_OFF_BUTTON_MODE_GET.getMessageType(),
                new byte[]{
                        PayloadTypeV1.AUTOMATIC_POWER_OFF_BUTTON_MODE_GET.getCode(),
                        (byte) 0x0c
                }
        );
    }

    @Override
    public Request setSpeakToChatConfig(SpeakToChatConfig config) {
        return new Request(
                PayloadTypeV1.SPEAK_TO_CHAT_CONFIG_SET.getMessageType(),
                new byte[]{
                        PayloadTypeV1.SPEAK_TO_CHAT_CONFIG_SET.getCode(),
                        (byte) 0x0c,
                        config.getSensitivity().getCode(),
                        config.getTimeout().getCode()
                }
        );
    }

    @Override
    public Request getSpeakToChatConfig() {
        return new Request(
                PayloadTypeV1.SPEAK_TO_CHAT_CONFIG_GET.getMessageType(),
                new byte[]{
                        PayloadTypeV1.SPEAK_TO_CHAT_CONFIG_GET.getCode(),
                        (byte) 0x0c
                }
        );
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
                        (byte) 0x02
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
    public Request setWideAreaTap(final WideAreaTap config) {
        return new Request(
                PayloadTypeV1.TOUCH_SENSOR_SET.getMessageType(),
                new byte[]{
                        PayloadTypeV1.TOUCH_SENSOR_SET.getCode(),
                        (byte) 0xd1,
                        (byte) 0x00,
                        (byte) (config.isEnabled() ? 0x00 : 0x01) // this is reversed on V2...?
                }
        );
    }

    @Override
    public Request getWideAreaTap() {
        return new Request(
                PayloadTypeV1.TOUCH_SENSOR_GET.getMessageType(),
                new byte[]{
                        PayloadTypeV1.TOUCH_SENSOR_GET.getCode(),
                        (byte) 0xd1
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
                        encodeButtonMode(config.getModeLeft()),
                        encodeButtonMode(config.getModeRight())
                }
        );
    }

    @Override
    public Request getQuickAccess() {
        return new Request(
                PayloadTypeV1.AUTOMATIC_POWER_OFF_BUTTON_MODE_GET.getMessageType(),
                new byte[]{
                        PayloadTypeV1.AUTOMATIC_POWER_OFF_BUTTON_MODE_GET.getCode(),
                        (byte) 0x0d
                }
        );
    }

    @Override
    public Request setQuickAccess(final QuickAccess quickAccess) {
        return new Request(
                PayloadTypeV1.AUTOMATIC_POWER_OFF_BUTTON_MODE_SET.getMessageType(),
                new byte[]{
                        PayloadTypeV1.AUTOMATIC_POWER_OFF_BUTTON_MODE_SET.getCode(),
                        (byte) 0x0d,
                        (byte) 0x02,
                        quickAccess.getModeDoubleTap().getCode(),
                        quickAccess.getModeTripleTap().getCode()
                }
        );
    }

    @Override
    public Request getAmbientSoundControlButtonMode() {
        return new Request(
                PayloadTypeV2.AMBIENT_SOUND_CONTROL_BUTTON_MODE_GET.getMessageType(),
                new byte[]{
                        PayloadTypeV2.AMBIENT_SOUND_CONTROL_BUTTON_MODE_GET.getCode(),
                        (byte) 0x03
                }
        );
    }

    @Override
    public Request setAmbientSoundControlButtonMode(final AmbientSoundControlButtonMode ambientSoundControlButtonMode) {
        return new Request(
                PayloadTypeV2.AMBIENT_SOUND_CONTROL_BUTTON_MODE_SET.getMessageType(),
                new byte[]{
                        PayloadTypeV2.AMBIENT_SOUND_CONTROL_BUTTON_MODE_SET.getCode(),
                        (byte) 0x03,
                        (byte) 0x01,
                        (byte) (getCoordinator().supports(SonyHeadphonesCapabilities.AmbientSoundControl2) ? 0x00 : 0x35),
                        (byte) 0x01,
                        (byte) 0x00,
                        ambientSoundControlButtonMode.getCode()
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
        final ByteBuffer buf = ByteBuffer.allocate(10);

        buf.put(PayloadTypeV1.EQUALIZER_SET.getCode());
        buf.put((byte) 0x00);
        buf.put((byte) 0xa0);
        buf.put((byte) 0x06);

        buf.put((byte) (config.getBass() + 10));
        for (final Integer band : config.getBands()) {
            buf.put((byte) (band + 10));
        }

        return new Request(
                PayloadTypeV1.EQUALIZER_SET.getMessageType(),
                buf.array()
        );
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
        return new Request(
                PayloadTypeV1.VOICE_NOTIFICATIONS_GET.getMessageType(),
                new byte[]{
                        PayloadTypeV1.VOICE_NOTIFICATIONS_GET.getCode(),
                        (byte) 0x01
                }
        );
    }

    @Override
    public Request setVoiceNotifications(final VoiceNotifications config) {
        return new Request(
                PayloadTypeV1.VOICE_NOTIFICATIONS_SET.getMessageType(),
                new byte[]{
                        PayloadTypeV1.VOICE_NOTIFICATIONS_SET.getCode(),
                        (byte) 0x01,
                        (byte) (config.isEnabled() ? 0x00 : 0x01)  // reversed?
                }
        );
    }

    @Override
    public Request startNoiseCancellingOptimizer(final boolean start) {
        LOG.warn("Noise cancelling optimizer not implemented for V2");
        return null;
    }

    @Override
    public Request powerOff() {
        return new Request(
                PayloadTypeV2.POWER_SET.getMessageType(),
                new byte[]{
                        PayloadTypeV2.POWER_SET.getCode(),
                        (byte) 0x03,
                        (byte) 0x01
                }
        );
    }

    @Override
    public Request getVolume() {
        LOG.warn("Volume not implemented for V2");
        return null;
    }

    @Override
    public Request setVolume(final int volume) {
        LOG.warn("Volume not implemented for V2");
        return null;
    }

    @Override
    public List<? extends GBDeviceEvent> handlePayload(final MessageType messageType, final byte[] payload) {
        final PayloadTypeV2 payloadType = PayloadTypeV2.fromCode(messageType, payload[0]);

        switch (payloadType) {
            case AUDIO_CODEC_REPLY:
            case AUDIO_CODEC_NOTIFY:
                return handleAudioCodec(payload);
            case BATTERY_LEVEL_NOTIFY:
            case BATTERY_LEVEL_REPLY:
                return handleBattery(payload);
            case AUTOMATIC_POWER_OFF_RET:
            case AUTOMATIC_POWER_OFF_NOTIFY:
                return handleAutomaticPowerOff(payload);
            case AMBIENT_SOUND_CONTROL_BUTTON_MODE_RET:
            case AMBIENT_SOUND_CONTROL_BUTTON_MODE_NOTIFY:
                return handleAmbientSoundControlButtonMode(payload);
        }

        return super.handlePayload(messageType, payload);
    }

    @Override
    public List<? extends GBDeviceEvent> handleInitResponse(final byte[] payload) {
        return super.handleInitResponse(payload);
    }

    @Override
    public List<? extends GBDeviceEvent> handleAmbientSoundControl(final byte[] payload) {
        if (payload.length != 8 && payload.length != 7) {
            LOG.warn("Unexpected payload length {}", payload.length);
            return Collections.emptyList();
        }

        if (payload[1] != 0x15 && payload[1] != 0x17) {
            LOG.warn("Not ambient sound control, ignoring {}", payload[1]);
            return Collections.emptyList();
        }

        final boolean includesWindNoiseReduction = payload[1] == 0x17 && payload.length > 7;

        AmbientSoundControl.Mode mode = null;

        if (payload[3] == (byte) 0x00) {
            mode = AmbientSoundControl.Mode.OFF;
        } else if (payload[3] == (byte) 0x01) {
            // Enabled, determine mode

            if (includesWindNoiseReduction) {
                if (payload[5] == 0x03 || payload[5] == 0x05) {
                    mode = AmbientSoundControl.Mode.WIND_NOISE_REDUCTION;
                } else if (payload[5] == 0x02) {
                    if (payload[4] == (byte) 0x00) {
                        mode = AmbientSoundControl.Mode.NOISE_CANCELLING;
                    } else if (payload[4] == (byte) 0x01) {
                        mode = AmbientSoundControl.Mode.AMBIENT_SOUND;
                    }
                }
            } else {
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

        int i = includesWindNoiseReduction ? 6 : 5;
        final Boolean focusOnVoice = booleanFromByte(payload[i]);
        if (focusOnVoice == null) {
            LOG.warn("Unknown focus on voice mode {}", String.format("%02x", payload[i]));
            return Collections.emptyList();
        }

        i++;
        int ambientSound = payload[i];
        if (ambientSound < 0 || ambientSound > 20) {
            LOG.warn("Ambient sound level {} is out of range", String.format("%02x", payload[i]));
            return Collections.emptyList();
        }

        final AmbientSoundControl ambientSoundControl = new AmbientSoundControl(mode, focusOnVoice, ambientSound);

        LOG.debug("Ambient sound control: {}", ambientSoundControl);

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

    public List<? extends GBDeviceEvent> handleAdaptiveVolumeControl(final byte[] payload) {
        if (payload.length != 3) {
            LOG.warn("Unexpected payload length {}", payload.length);
            return Collections.emptyList();
        }

        if (payload[1] != 0x0a) {
            LOG.warn("Not adaptive volume control, ignoring");
            return Collections.emptyList();
        }

        final Boolean disabled = booleanFromByte(payload[2]);
        if (disabled == null) {
            LOG.warn("Unknown adaptive volume control code {}", String.format("%02x", payload[2]));
            return Collections.emptyList();
        }

        LOG.debug("Adaptive volume control: {}", !disabled);

        final GBDeviceEventUpdatePreferences event = new GBDeviceEventUpdatePreferences()
                .withPreferences(new AdaptiveVolumeControl(!disabled).toPreferences());

        return Collections.singletonList(event);
    }

    @Override
    public List<? extends GBDeviceEvent> handleSpeakToChatEnabled(final byte[] payload) {
        if (payload.length != 4) {
            LOG.warn("Unexpected payload length {}", payload.length);
            return Collections.emptyList();
        }

        if (payload[1] != 0x0c) {
            LOG.warn("Not speak to chat enabled, ignoring");
            return Collections.emptyList();
        }

        final Boolean disabled = booleanFromByte(payload[2]);
        if (disabled == null) {
            LOG.warn("Unknown speak to chat enabled code {}", String.format("%02x", payload[2]));
            return Collections.emptyList();
        }

        LOG.debug("Speak to chat: {}", !disabled);

        final GBDeviceEventUpdatePreferences event = new GBDeviceEventUpdatePreferences()
                .withPreferences(new SpeakToChatEnabled(!disabled).toPreferences());

        return Collections.singletonList(event);
    }

    @Override
    public List<? extends GBDeviceEvent> handleSpeakToChatConfig(final byte[] payload) {
        if (payload.length != 4) {
            LOG.warn("Unexpected payload length {}", payload.length);
            return Collections.emptyList();
        }

        if (payload[1] != 0x0c) {
            LOG.warn("Not speak to chat config, ignoring");
            return Collections.emptyList();
        }

        final SpeakToChatConfig.Sensitivity sensitivity = SpeakToChatConfig.Sensitivity.fromCode(payload[2]);
        if (sensitivity == null) {
            LOG.warn("Unknown sensitivity code {}", String.format("%02x", payload[2]));
            return Collections.emptyList();
        }

        final SpeakToChatConfig.Timeout timeout = SpeakToChatConfig.Timeout.fromCode(payload[3]);
        if (timeout == null) {
            LOG.warn("Unknown timeout code {}", String.format("%02x", payload[3]));
            return Collections.emptyList();
        }

        final SpeakToChatConfig speakToChatConfig = new SpeakToChatConfig(false, sensitivity, timeout);

        LOG.debug("Speak to chat config: {}", speakToChatConfig);

        final GBDeviceEventUpdatePreferences event = new GBDeviceEventUpdatePreferences()
                .withPreferences(speakToChatConfig.toPreferences());

        return Collections.singletonList(event);
    }

    public List<? extends GBDeviceEvent> handleQuickAccess(final byte[] payload) {
        if (payload.length != 5) {
            LOG.warn("Unexpected payload length {}", payload.length);
            return Collections.emptyList();
        }

        if (payload[1] != 0x0d || payload[2] != 0x02) {
            LOG.warn("Unexpected quick access payload bytes {}", String.format("%02x %02x", payload[1], payload[2]));
            return Collections.emptyList();
        }

        final QuickAccess.Mode modeDouble = QuickAccess.Mode.fromCode(payload[3]);
        final QuickAccess.Mode modeTriple = QuickAccess.Mode.fromCode(payload[4]);
        if (modeDouble == null || modeTriple == null) {
            LOG.warn("Unknown quick access codes {}", String.format("%02x %02x", payload[3], payload[4]));
            return Collections.emptyList();
        }

        LOG.debug("Quick Access: Double Tap: {}, Triple Tap: {}", modeDouble, modeTriple);

        final GBDeviceEventUpdatePreferences event = new GBDeviceEventUpdatePreferences()
                .withPreferences(new QuickAccess(modeDouble, modeTriple).toPreferences());

        return Collections.singletonList(event);
    }

    public List<? extends GBDeviceEvent> handleAmbientSoundControlButtonMode(final byte[] payload) {
        if (payload.length == 4 && payload[1] == 0x0c) {
            // FIXME split this
            return handleSpeakToChatConfig(payload);
        }

        if (payload.length != 7) {
            LOG.warn("Unexpected payload length {}", payload.length);
            return Collections.emptyList();
        }

        if (payload[1] != 0x03 || payload[2] != 0x01 || (payload[3] != 0x00 && payload[3] != 0x35) || payload[4] != 0x01 || payload[5] != 0x00) {
            LOG.warn(
                    "Unexpected ambient sound control button mode payload bytes {}",
                    String.format("%02x %02x %02x %02x %02x", payload[1], payload[2], payload[3], payload[4], payload[5])
            );
            return Collections.emptyList();
        }

        final AmbientSoundControlButtonMode mode = AmbientSoundControlButtonMode.fromCode(payload[6]);
        if (mode == null) {
            LOG.warn("Unknown ambient sound control button mode code {}", String.format("%02x", payload[6]));
            return Collections.emptyList();
        }

        LOG.debug("Ambient Sound Control Button Mode: {}", mode);

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

        final ButtonModes.Mode modeLeft = decodeButtonMode(payload[3]);
        final ButtonModes.Mode modeRight = decodeButtonMode(payload[4]);

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

        if (payload[1] != 0x02) {
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
            case 0x0a:
                return handleAdaptiveVolumeControl(payload);
            case 0x0c:
                return handleSpeakToChatEnabled(payload);
            case 0x0d:
                return handleQuickAccess(payload);
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
        if (payload.length != 4) {
            LOG.warn("Unexpected payload length {}", payload.length);
            return Collections.emptyList();
        }

        if (payload[1] != (byte) 0xd1) {
            LOG.warn("Not wide area tap");
            return Collections.emptyList();
        }

        boolean enabled;

        // reversed?
        switch (payload[3]) {
            case 0x00:
                enabled = true;
                break;
            case 0x01:
                enabled = false;
                break;
            default:
                LOG.warn("Unknown wide area tap code {}", String.format("%02x", payload[3]));
                return Collections.emptyList();
        }

        LOG.debug("Wide Area Tap: {}", enabled);

        final GBDeviceEventUpdatePreferences event = new GBDeviceEventUpdatePreferences()
                .withPreferences(new WideAreaTap(enabled).toPreferences());

        return Collections.singletonList(event);
    }

    @Override
    public List<? extends GBDeviceEvent> handleVoiceNotifications(final byte[] payload) {
        if (payload.length != 4) {
            LOG.warn("Unexpected payload length {}", payload.length);
            return Collections.emptyList();
        }

        boolean enabled;

        // reversed?
        switch (payload[2]) {
            case 0x00:
                enabled = true;
                break;
            case 0x01:
                enabled = false;
                break;
            default:
                LOG.warn("Unknown voice notifications code {}", String.format("%02x", payload[3]));
                return Collections.emptyList();
        }

        LOG.debug("Voice Notifications: {}", enabled);

        final GBDeviceEventUpdatePreferences event = new GBDeviceEventUpdatePreferences()
                .withPreferences(new VoiceNotifications(enabled).toPreferences());

        return Collections.singletonList(event);
    }

    @Override
    protected BatteryType decodeBatteryType(final byte b) {
        switch (b) {
            case 0x00:
                return BatteryType.SINGLE;
            case 0x01:
                return BatteryType.DUAL2;
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
            case SINGLE:
                return 0x00;
            case DUAL2:
                return 0x01;
            case DUAL:
                return 0x09;
            case CASE:
                return 0x0a;
        }

        throw new IllegalArgumentException("Unknown battery type " + batteryType);
    }

    @Override
    protected ButtonModes.Mode decodeButtonMode(final byte b) {
        switch (b) {
            case (byte) 0xff:
                return ButtonModes.Mode.OFF;
            case (byte) 0x00:
            case (byte) 0x35:  // Seems to be the only one that differs?
                return ButtonModes.Mode.AMBIENT_SOUND_CONTROL;
            case (byte) 0x20:
                return ButtonModes.Mode.PLAYBACK_CONTROL;
            case (byte) 0x10:
                return ButtonModes.Mode.VOLUME_CONTROL;
        }

        return null;
    }

    @Override
    protected byte encodeButtonMode(final ButtonModes.Mode buttonMode) {
        switch (buttonMode) {
            case OFF:
                return (byte) 0xff;
            case AMBIENT_SOUND_CONTROL:
                return (byte) (supportsWindNoiseCancelling() ? 0x35 : 0x00); // Seems to be the only one that differs?
            case PLAYBACK_CONTROL:
                return (byte) 0x20;
            case VOLUME_CONTROL:
                return (byte) 0x10;
        }

        throw new IllegalArgumentException("Unknown button mode " + buttonMode);
    }
}
