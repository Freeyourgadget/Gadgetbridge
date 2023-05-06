/*  Copyright (C) 2023 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.impl.v1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.impl.SonyTestUtils.assertRequest;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.impl.SonyTestUtils.assertRequests;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.SonyHeadphonesCapabilities;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.SonyHeadphonesCoordinator;
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
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.MessageType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.Request;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.impl.MockSonyCoordinator;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.impl.v1.params.BatteryType;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class SonyProtocolImplV1Test {
    private final MockSonyCoordinator coordinator = new MockSonyCoordinator();
    private final SonyProtocolImplV1 protocol = new SonyProtocolImplV1(null) {
        @Override
        protected SonyHeadphonesCoordinator getCoordinator() {
            return coordinator;
        }
    };

    @Before
    public void before() {
        coordinator.getCapabilities().clear();
    }

    @Test
    public void getAmbientSoundControl() {
        final Request request = protocol.getAmbientSoundControl();
        assertRequest(request, 0x0c, "66:02");
    }

    @Test
    public void setAmbientSoundControl() {
        final Map<AmbientSoundControl, String> commands = new LinkedHashMap<AmbientSoundControl, String>() {{
            put(new AmbientSoundControl(AmbientSoundControl.Mode.OFF, false, 0), "68:02:00:00:00:01:00:00");
            put(new AmbientSoundControl(AmbientSoundControl.Mode.OFF, false, 5), "68:02:00:00:00:01:00:05");
            put(new AmbientSoundControl(AmbientSoundControl.Mode.OFF, true, 5), "68:02:00:00:00:01:01:05");
            put(new AmbientSoundControl(AmbientSoundControl.Mode.OFF, true, 10), "68:02:00:00:00:01:01:0A");
            put(new AmbientSoundControl(AmbientSoundControl.Mode.AMBIENT_SOUND, false, 0), "68:02:11:00:00:01:00:00");
            put(new AmbientSoundControl(AmbientSoundControl.Mode.AMBIENT_SOUND, false, 10), "68:02:11:00:00:01:00:0A");
            put(new AmbientSoundControl(AmbientSoundControl.Mode.AMBIENT_SOUND, true, 10), "68:02:11:00:00:01:01:0A");
            put(new AmbientSoundControl(AmbientSoundControl.Mode.AMBIENT_SOUND, false, 15), "68:02:11:00:00:01:00:0F");
            put(new AmbientSoundControl(AmbientSoundControl.Mode.AMBIENT_SOUND, true, 15), "68:02:11:00:00:01:01:0F");
            put(new AmbientSoundControl(AmbientSoundControl.Mode.NOISE_CANCELLING, false, 0), "68:02:11:00:01:01:00:00");
            put(new AmbientSoundControl(AmbientSoundControl.Mode.NOISE_CANCELLING, true, 15), "68:02:11:00:01:01:01:00");
            put(new AmbientSoundControl(AmbientSoundControl.Mode.NOISE_CANCELLING, false, 15), "68:02:11:00:01:01:00:00");
        }};

        for (Map.Entry<AmbientSoundControl, String> entry : commands.entrySet()) {
            final Request request = protocol.setAmbientSoundControl(entry.getKey());
            assertRequest(request, 0x0c, entry.getValue());
        }

        coordinator.addCapability(SonyHeadphonesCapabilities.WindNoiseReduction);
        final Map<AmbientSoundControl, String> commandsWindNoiseReduction = new LinkedHashMap<AmbientSoundControl, String>() {{
            put(new AmbientSoundControl(AmbientSoundControl.Mode.OFF, false, 0), "68:02:00:02:00:01:00:00");
            put(new AmbientSoundControl(AmbientSoundControl.Mode.OFF, false, 5), "68:02:00:02:00:01:00:05");
            put(new AmbientSoundControl(AmbientSoundControl.Mode.OFF, true, 5), "68:02:00:02:00:01:01:05");
            put(new AmbientSoundControl(AmbientSoundControl.Mode.OFF, true, 10), "68:02:00:02:00:01:01:0A");
            put(new AmbientSoundControl(AmbientSoundControl.Mode.AMBIENT_SOUND, false, 0), "68:02:11:02:00:01:00:00");
            put(new AmbientSoundControl(AmbientSoundControl.Mode.AMBIENT_SOUND, false, 10), "68:02:11:02:00:01:00:0A");
            put(new AmbientSoundControl(AmbientSoundControl.Mode.AMBIENT_SOUND, true, 10), "68:02:11:02:00:01:01:0A");
            put(new AmbientSoundControl(AmbientSoundControl.Mode.AMBIENT_SOUND, false, 15), "68:02:11:02:00:01:00:0F");
            put(new AmbientSoundControl(AmbientSoundControl.Mode.AMBIENT_SOUND, true, 15), "68:02:11:02:00:01:01:0F");
            put(new AmbientSoundControl(AmbientSoundControl.Mode.NOISE_CANCELLING, false, 0), "68:02:11:02:02:01:00:00");
            put(new AmbientSoundControl(AmbientSoundControl.Mode.NOISE_CANCELLING, true, 15), "68:02:11:02:02:01:01:00");
            put(new AmbientSoundControl(AmbientSoundControl.Mode.NOISE_CANCELLING, false, 15), "68:02:11:02:02:01:00:00");
            put(new AmbientSoundControl(AmbientSoundControl.Mode.WIND_NOISE_REDUCTION, false, 5), "68:02:11:02:01:01:00:00");
            put(new AmbientSoundControl(AmbientSoundControl.Mode.WIND_NOISE_REDUCTION, false, 15), "68:02:11:02:01:01:00:00");
            put(new AmbientSoundControl(AmbientSoundControl.Mode.WIND_NOISE_REDUCTION, true, 15), "68:02:11:02:01:01:01:00");
        }};

        for (Map.Entry<AmbientSoundControl, String> entry : commandsWindNoiseReduction.entrySet()) {
            final Request request = protocol.setAmbientSoundControl(entry.getKey());
            assertRequest(request, 0x0c, entry.getValue());
        }
    }

    @Test
    public void setSpeakToChatEnabled() {
        assertRequests(protocol::setSpeakToChatEnabled, new LinkedHashMap<SpeakToChatEnabled, String>() {{
            put(new SpeakToChatEnabled(false), "f8:05:01:00");
            put(new SpeakToChatEnabled(true), "f8:05:01:01");
        }});
    }

    @Test
    public void getSpeakToChatEnabled() {
        final Request request = protocol.getSpeakToChatEnabled();
        assertRequest(request, 0x0c, "f6:05");
    }

    @Test
    public void setSpeakToChatConfig() {
        assertRequests(protocol::setSpeakToChatConfig, new LinkedHashMap<SpeakToChatConfig, String>() {{
            put(new SpeakToChatConfig(false, SpeakToChatConfig.Sensitivity.AUTO, SpeakToChatConfig.Timeout.SHORT), "fc:05:00:00:00:00");
            put(new SpeakToChatConfig(false, SpeakToChatConfig.Sensitivity.AUTO, SpeakToChatConfig.Timeout.STANDARD), "fc:05:00:00:00:01");
            put(new SpeakToChatConfig(false, SpeakToChatConfig.Sensitivity.AUTO, SpeakToChatConfig.Timeout.LONG), "fc:05:00:00:00:02");
            put(new SpeakToChatConfig(false, SpeakToChatConfig.Sensitivity.AUTO, SpeakToChatConfig.Timeout.OFF), "fc:05:00:00:00:03");

            put(new SpeakToChatConfig(false, SpeakToChatConfig.Sensitivity.HIGH, SpeakToChatConfig.Timeout.SHORT), "fc:05:00:01:00:00");
            put(new SpeakToChatConfig(false, SpeakToChatConfig.Sensitivity.HIGH, SpeakToChatConfig.Timeout.STANDARD), "fc:05:00:01:00:01");
            put(new SpeakToChatConfig(false, SpeakToChatConfig.Sensitivity.HIGH, SpeakToChatConfig.Timeout.LONG), "fc:05:00:01:00:02");
            put(new SpeakToChatConfig(false, SpeakToChatConfig.Sensitivity.HIGH, SpeakToChatConfig.Timeout.OFF), "fc:05:00:01:00:03");

            put(new SpeakToChatConfig(false, SpeakToChatConfig.Sensitivity.LOW, SpeakToChatConfig.Timeout.SHORT), "fc:05:00:02:00:00");
            put(new SpeakToChatConfig(false, SpeakToChatConfig.Sensitivity.LOW, SpeakToChatConfig.Timeout.STANDARD), "fc:05:00:02:00:01");
            put(new SpeakToChatConfig(false, SpeakToChatConfig.Sensitivity.LOW, SpeakToChatConfig.Timeout.LONG), "fc:05:00:02:00:02");
            put(new SpeakToChatConfig(false, SpeakToChatConfig.Sensitivity.LOW, SpeakToChatConfig.Timeout.OFF), "fc:05:00:02:00:03");

            put(new SpeakToChatConfig(true, SpeakToChatConfig.Sensitivity.AUTO, SpeakToChatConfig.Timeout.SHORT), "fc:05:00:00:01:00");
            put(new SpeakToChatConfig(true, SpeakToChatConfig.Sensitivity.AUTO, SpeakToChatConfig.Timeout.STANDARD), "fc:05:00:00:01:01");
            put(new SpeakToChatConfig(true, SpeakToChatConfig.Sensitivity.AUTO, SpeakToChatConfig.Timeout.LONG), "fc:05:00:00:01:02");
            put(new SpeakToChatConfig(true, SpeakToChatConfig.Sensitivity.AUTO, SpeakToChatConfig.Timeout.OFF), "fc:05:00:00:01:03");

            put(new SpeakToChatConfig(true, SpeakToChatConfig.Sensitivity.HIGH, SpeakToChatConfig.Timeout.SHORT), "fc:05:00:01:01:00");
            put(new SpeakToChatConfig(true, SpeakToChatConfig.Sensitivity.HIGH, SpeakToChatConfig.Timeout.STANDARD), "fc:05:00:01:01:01");
            put(new SpeakToChatConfig(true, SpeakToChatConfig.Sensitivity.HIGH, SpeakToChatConfig.Timeout.LONG), "fc:05:00:01:01:02");
            put(new SpeakToChatConfig(true, SpeakToChatConfig.Sensitivity.HIGH, SpeakToChatConfig.Timeout.OFF), "fc:05:00:01:01:03");

            put(new SpeakToChatConfig(true, SpeakToChatConfig.Sensitivity.LOW, SpeakToChatConfig.Timeout.SHORT), "fc:05:00:02:01:00");
            put(new SpeakToChatConfig(true, SpeakToChatConfig.Sensitivity.LOW, SpeakToChatConfig.Timeout.STANDARD), "fc:05:00:02:01:01");
            put(new SpeakToChatConfig(true, SpeakToChatConfig.Sensitivity.LOW, SpeakToChatConfig.Timeout.LONG), "fc:05:00:02:01:02");
            put(new SpeakToChatConfig(true, SpeakToChatConfig.Sensitivity.LOW, SpeakToChatConfig.Timeout.OFF), "fc:05:00:02:01:03");
        }});
    }

    @Test
    public void getSpeakToChatConfig() {
        final Request request = protocol.getSpeakToChatConfig();
        assertRequest(request, 0x0c, "fa:05");
    }

    @Test
    public void getNoiseCancellingOptimizerState() {
        final Request request = protocol.getNoiseCancellingOptimizerState();
        assertRequest(request, 0x0c, "86:01");
    }

    @Test
    public void getAudioCodec() {
        final Request request = protocol.getAudioCodec();
        assertRequest(request, 0x0c, "18:00");
    }

    @Test
    public void getBattery() {
        final Map<BatteryType, String> commands = new LinkedHashMap<BatteryType, String>() {{
            put(BatteryType.SINGLE, "10:00");
            put(BatteryType.DUAL, "10:01");
            put(BatteryType.CASE, "10:02");
        }};

        for (Map.Entry<BatteryType, String> entry : commands.entrySet()) {
            final Request request = protocol.getBattery(entry.getKey());
            assertRequest(request, 0x0c, entry.getValue());
        }
    }

    @Test
    public void getFirmwareVersion() {
        final Request request = protocol.getFirmwareVersion();
        assertRequest(request, 0x0c, "04:02");
    }

    @Test
    public void getAudioUpsampling() {
        final Request request = protocol.getAudioUpsampling();
        assertRequest(request, 0x0c, "e6:02");
    }

    @Test
    public void setAudioUpsampling() {
        assertRequests(protocol::setAudioUpsampling, new LinkedHashMap<AudioUpsampling, String>() {{
            put(new AudioUpsampling(false), "e8:02:00:00");
            put(new AudioUpsampling(true), "e8:02:00:01");
        }});
    }

    @Test
    public void getAutomaticPowerOff() {
        final Request request = protocol.getAutomaticPowerOff();
        assertRequest(request, 0x0c, "f6:04");
    }

    @Test
    public void setAutomaticPowerOff() {
        assertRequests(protocol::setAutomaticPowerOff, new LinkedHashMap<AutomaticPowerOff, String>() {{
            put(AutomaticPowerOff.OFF, "f8:04:01:11:00");
            put(AutomaticPowerOff.AFTER_5_MIN, "f8:04:01:00:00");
            put(AutomaticPowerOff.AFTER_30_MIN, "f8:04:01:01:01");
            put(AutomaticPowerOff.AFTER_1_HOUR, "f8:04:01:02:02");
            put(AutomaticPowerOff.AFTER_3_HOUR, "f8:04:01:03:03");
            put(AutomaticPowerOff.WHEN_TAKEN_OFF, "f8:04:01:10:00");
        }});
    }

    @Test
    public void getButtonModes() {
        final Request request = protocol.getButtonModes();
        assertRequest(request, 0x0c, "f6:06");
    }

    @Test
    public void setButtonModes() {
        assertRequests(protocol::setButtonModes, new LinkedHashMap<ButtonModes, String>() {{
            put(new ButtonModes(ButtonModes.Mode.OFF, ButtonModes.Mode.OFF), "f8:06:02:ff:ff");
            put(new ButtonModes(ButtonModes.Mode.OFF, ButtonModes.Mode.AMBIENT_SOUND_CONTROL), "f8:06:02:ff:00");
            put(new ButtonModes(ButtonModes.Mode.OFF, ButtonModes.Mode.PLAYBACK_CONTROL), "f8:06:02:ff:20");
            put(new ButtonModes(ButtonModes.Mode.OFF, ButtonModes.Mode.VOLUME_CONTROL), "f8:06:02:ff:10");
            put(new ButtonModes(ButtonModes.Mode.OFF, ButtonModes.Mode.OFF), "f8:06:02:ff:ff");
            put(new ButtonModes(ButtonModes.Mode.AMBIENT_SOUND_CONTROL, ButtonModes.Mode.OFF), "f8:06:02:00:ff");
            put(new ButtonModes(ButtonModes.Mode.PLAYBACK_CONTROL, ButtonModes.Mode.OFF), "f8:06:02:20:ff");
            put(new ButtonModes(ButtonModes.Mode.VOLUME_CONTROL, ButtonModes.Mode.OFF), "f8:06:02:10:ff");
        }});
    }

    @Test
    public void getQuickAccess() {
        final Request request = protocol.getQuickAccess();
        assertNull(request);
    }

    @Test
    public void setQuickAccess() {
        assertRequests(protocol::setQuickAccess, new LinkedHashMap<QuickAccess, String>() {{
            put(new QuickAccess(QuickAccess.Mode.OFF, QuickAccess.Mode.OFF), null);
            put(new QuickAccess(QuickAccess.Mode.OFF, QuickAccess.Mode.SPOTIFY), null);
            put(new QuickAccess(QuickAccess.Mode.SPOTIFY, QuickAccess.Mode.OFF), null);
            put(new QuickAccess(QuickAccess.Mode.SPOTIFY, QuickAccess.Mode.SPOTIFY), null);
        }});
    }

    @Test
    public void getAmbientSoundControlButtonMode() {
        final Request request = protocol.getAmbientSoundControlButtonMode();
        assertNull(request);
    }

    @Test
    public void setAmbientSoundControlButtonMode() {
        assertRequests(protocol::setAmbientSoundControlButtonMode, new LinkedHashMap<AmbientSoundControlButtonMode, String>() {{
            put(AmbientSoundControlButtonMode.NC_AS_OFF, null);
            put(AmbientSoundControlButtonMode.NC_AS, null);
            put(AmbientSoundControlButtonMode.NC_OFF, null);
            put(AmbientSoundControlButtonMode.AS_OFF, null);
        }});
    }

    @Test
    public void getPauseWhenTakenOff() {
        final Request request = protocol.getPauseWhenTakenOff();
        assertRequest(request, 0x0c, "f6:03");
    }

    @Test
    public void setPauseWhenTakenOff() {
        assertRequests(protocol::setPauseWhenTakenOff, new LinkedHashMap<PauseWhenTakenOff, String>() {{
            put(new PauseWhenTakenOff(false), "f8:03:00:00");
            put(new PauseWhenTakenOff(true), "f8:03:00:01");
        }});
    }

    @Test
    public void getEqualizer() {
        final Request request = protocol.getEqualizer();
        assertRequest(request, 0x0c, "56:01");
    }

    @Test
    public void setEqualizerPreset() {
        assertRequests(protocol::setEqualizerPreset, new LinkedHashMap<EqualizerPreset, String>() {{
            put(EqualizerPreset.OFF, "58:01:00:00");
            put(EqualizerPreset.BRIGHT, "58:01:10:00");
            put(EqualizerPreset.EXCITED, "58:01:11:00");
            put(EqualizerPreset.MELLOW, "58:01:12:00");
            put(EqualizerPreset.RELAXED, "58:01:13:00");
            put(EqualizerPreset.VOCAL, "58:01:14:00");
            put(EqualizerPreset.TREBLE_BOOST, "58:01:15:00");
            put(EqualizerPreset.BASS_BOOST, "58:01:16:00");
            put(EqualizerPreset.SPEECH, "58:01:17:00");
            put(EqualizerPreset.MANUAL, "58:01:a0:00");
            put(EqualizerPreset.CUSTOM_1, "58:01:a1:00");
        }});
    }

    @Test
    public void setEqualizerCustomBands() {
        assertRequests(protocol::setEqualizerCustomBands, new LinkedHashMap<EqualizerCustomBands, String>() {{
            put(new EqualizerCustomBands(Arrays.asList(0, 0, 0, 0, 0), 0), "58:01:ff:06:0a:0a:0a:0a:0a:0a");
            put(new EqualizerCustomBands(Arrays.asList(5, 1, 0, 4, 0), 0), "58:01:ff:06:0a:0f:0b:0a:0e:0a");
            put(new EqualizerCustomBands(Arrays.asList(10, 0, 3, 0, 5), -2), "58:01:ff:06:08:14:0a:0d:0a:0f");
            put(new EqualizerCustomBands(Arrays.asList(-3, -7, 0, 2, 9), 0), "58:01:ff:06:0a:07:03:0a:0c:13");
            put(new EqualizerCustomBands(Arrays.asList(-3, -7, 0, 2, 9), 7), "58:01:ff:06:11:07:03:0a:0c:13");
        }});
    }

    @Test
    public void getSoundPosition() {
        final Request request = protocol.getSoundPosition();
        assertRequest(request, 0x0c, "46:02");
    }

    @Test
    public void setSoundPosition() {
        assertRequests(protocol::setSoundPosition, new LinkedHashMap<SoundPosition, String>() {{
            put(SoundPosition.OFF, "48:02:00");
            put(SoundPosition.FRONT, "48:02:03");
            put(SoundPosition.FRONT_LEFT, "48:02:01");
            put(SoundPosition.FRONT_RIGHT, "48:02:02");
            put(SoundPosition.REAR_LEFT, "48:02:11");
            put(SoundPosition.REAR_RIGHT, "48:02:12");
        }});
    }

    @Test
    public void getSurroundMode() {
        final Request request = protocol.getSurroundMode();
        assertRequest(request, 0x0c, "46:01");
    }

    @Test
    public void setSurroundMode() {
        assertRequests(protocol::setSurroundMode, new LinkedHashMap<SurroundMode, String>() {{
            put(SurroundMode.OFF, "48:01:00");
            put(SurroundMode.ARENA, "48:01:02");
            put(SurroundMode.CLUB, "48:01:04");
            put(SurroundMode.OUTDOOR_STAGE, "48:01:01");
            put(SurroundMode.CONCERT_HALL, "48:01:03");
        }});
    }

    @Test
    public void getTouchSensor() {
        final Request request = protocol.getTouchSensor();
        assertRequest(request, 0x0c, "d6:d2");
    }

    @Test
    public void setTouchSensor() {
        assertRequests(protocol::setTouchSensor, new LinkedHashMap<TouchSensor, String>() {{
            put(new TouchSensor(false), "d8:d2:01:00");
            put(new TouchSensor(true), "d8:d2:01:01");
        }});
    }

    @Test
    public void getVoiceNotifications() {
        final Request request = protocol.getVoiceNotifications();
        assertRequest(request, 0x0e, "46:01:01");
    }

    @Test
    public void setVoiceNotifications() {
        assertRequests(protocol::setVoiceNotifications, 0x0e, new LinkedHashMap<VoiceNotifications, String>() {{
            put(new VoiceNotifications(false), "48:01:01:00");
            put(new VoiceNotifications(true), "48:01:01:01");
        }});
    }

    @Test
    public void startNoiseCancellingOptimizer() {
        assertRequests(protocol::startNoiseCancellingOptimizer, new LinkedHashMap<Boolean, String>() {{
            put(Boolean.TRUE, "84:01:00:01");
            put(Boolean.FALSE, "84:01:00:00");
        }});
    }

    @Test
    public void powerOff() {
        final Request request = protocol.powerOff();
        assertRequest(request, 0x0c, "22:00:01");
    }

    @Test
    public void handlePayload() {
        // TODO
    }

    @Test
    public void validInitPayload() {
        // TODO
    }

    @Test
    public void handleInitResponse() {
        // TODO
    }

    @Test
    public void handleAmbientSoundControl() {
        // TODO
    }

    @Test
    public void handleNoiseCancellingOptimizerStatus() {
        // TODO
    }

    @Test
    public void handleNoiseCancellingOptimizerState() {
        // TODO
    }

    @Test
    public void handleAudioUpsampling() {
        // TODO
    }

    @Test
    public void handleAutomaticPowerOff() {
        // TODO
    }

    @Test
    public void handleSpeakToChatEnabled() {
        // TODO
    }

    @Test
    public void handleButtonModes() {
        // TODO
    }

    @Test
    public void handlePauseWhenTakenOff() {
        // TODO
    }

    @Test
    public void handleBattery() {
        // TODO
    }

    @Test
    public void handleAudioCodec() {
        final List<? extends GBDeviceEvent> event = protocol.handlePayload(
                MessageType.fromCode((byte) 0x0c),
                GB.hexStringToByteArray("1b:00:01".replace(":", ""))
        );

        assertEquals("Expect 2 events", 2, event.size());
    }

    @Test
    public void handleEqualizer() {
        // TODO
    }

    @Test
    public void handleFirmwareVersion() {
        // TODO
    }

    @Test
    public void handleJson() {
        // TODO
    }

    @Test
    public void handleAutomaticPowerOffButtonMode() {
        // TODO
    }

    @Test
    public void handleVirtualSound() {
        // TODO
    }

    @Test
    public void handleSoundPosition() {
        // TODO
    }

    @Test
    public void handleSurroundMode() {
        // TODO
    }

    @Test
    public void handleTouchSensor() {
        // TODO
    }

    @Test
    public void handleSpeakToChatConfig() {
        // TODO
    }

    @Test
    public void handleVoiceNotifications() {
        // TODO
    }
}
