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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import static nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.impl.SonyTestUtils.assertPrefs;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.impl.SonyTestUtils.assertRequest;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.impl.SonyTestUtils.assertRequests;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.impl.SonyTestUtils.handleMessage;

import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.SonyHeadphonesCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.coordinators.SonyWF1000XM4Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.AmbientSoundControlButtonMode;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.AudioUpsampling;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.AutomaticPowerOff;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.EqualizerCustomBands;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.EqualizerPreset;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.PauseWhenTakenOff;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.QuickAccess;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.SpeakToChatConfig;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.SpeakToChatEnabled;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.VoiceNotifications;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.Request;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.impl.v1.params.BatteryType;

public class SonyProtocolImplV2Test {
    private final SonyProtocolImplV2 protocol = new SonyProtocolImplV2(null) {
        @Override
        protected SonyHeadphonesCoordinator getCoordinator() {
            return new SonyWF1000XM4Coordinator();
        }
    };

    @Test
    public void getAmbientSoundControl() {
        // TODO
    }

    @Test
    public void setAmbientSoundControl() {
        // TODO
        //final Request request = protocol.setAmbientSoundControl(new AmbientSoundControl(
        //        AmbientSoundControl.Mode.WIND_NOISE_REDUCTION,
        //        true,
        //        15
        //));
        //assertRequest(request, "3e:0c:00:00:00:00:08:68:15:01:01:00:03:01:0f:a6:3c");

        //final Map<AmbientSoundControl, String> commands = new LinkedHashMap<AmbientSoundControl, String>() {{
        //    put(new AmbientSoundControl(AmbientSoundControl.Mode.AMBIENT_SOUND, false, 20), "68:17:01:01:01:00:14");
        //    put(new AmbientSoundControl(AmbientSoundControl.Mode.OFF, false, 20), "68:17:01:00:00:00:14");
        //    put(new AmbientSoundControl(AmbientSoundControl.Mode.AMBIENT_SOUND, false, 10), "68:17:01:01:01:00:0a");
        //    put(new AmbientSoundControl(AmbientSoundControl.Mode.AMBIENT_SOUND, true, 20), "68:17:01:01:01:01:14");
        //    put(new AmbientSoundControl(AmbientSoundControl.Mode.NOISE_CANCELLING, false, 20), "68:17:01:01:00:00:14");
        //}};
//
        //for (Map.Entry<AmbientSoundControl, String> entry : commands.entrySet()) {
        //    final Request request2 = protocol.setAmbientSoundControl(entry.getKey());
        //    assertRequest(request2, 0x0c, entry.getValue());
        //}
    }

    @Test
    public void getNoiseCancellingOptimizerState() {
        // TODO
    }

    @Test
    public void getAudioCodec() {
        final Request requestEnabled = protocol.getAudioCodec();
        assertRequest(requestEnabled, "3e:0c:01:00:00:00:02:12:02:23:3c");
    }

    @Test
    public void getBattery() {
        final Map<BatteryType, String> commands = new LinkedHashMap<BatteryType, String>() {{
            put(BatteryType.SINGLE, "22:00");
            put(BatteryType.DUAL, "22:09");
            put(BatteryType.DUAL2, "22:01");
            put(BatteryType.CASE, "22:0a");
        }};

        for (Map.Entry<BatteryType, String> entry : commands.entrySet()) {
            final Request request = protocol.getBattery(entry.getKey());
            assertRequest(request, 0x0c, entry.getValue());
        }
    }

    @Test
    public void getFirmwareVersion() {
        // TODO
    }

    @Test
    public void setSpeakToChatEnabled() {
        assertRequests(protocol::setSpeakToChatEnabled, new LinkedHashMap<SpeakToChatEnabled, String>() {{
            put(new SpeakToChatEnabled(false), "f8:0c:01:01");
            put(new SpeakToChatEnabled(true), "f8:0c:00:01");
        }});
    }

    @Test
    public void setSpeakToChatConfig() {
        assertRequests(protocol::setSpeakToChatConfig, new LinkedHashMap<SpeakToChatConfig, String>() {{
            put(new SpeakToChatConfig(false, SpeakToChatConfig.Sensitivity.HIGH, SpeakToChatConfig.Timeout.STANDARD), "fc:0c:01:01");
            put(new SpeakToChatConfig(false, SpeakToChatConfig.Sensitivity.LOW, SpeakToChatConfig.Timeout.STANDARD), "fc:0c:02:01");
            put(new SpeakToChatConfig(false, SpeakToChatConfig.Sensitivity.AUTO, SpeakToChatConfig.Timeout.STANDARD), "fc:0c:00:01");
            put(new SpeakToChatConfig(false, SpeakToChatConfig.Sensitivity.AUTO, SpeakToChatConfig.Timeout.SHORT), "fc:0c:00:00");
            put(new SpeakToChatConfig(false, SpeakToChatConfig.Sensitivity.AUTO, SpeakToChatConfig.Timeout.LONG), "fc:0c:00:02");
            put(new SpeakToChatConfig(false, SpeakToChatConfig.Sensitivity.AUTO, SpeakToChatConfig.Timeout.OFF), "fc:0c:00:03");
            put(new SpeakToChatConfig(false, SpeakToChatConfig.Sensitivity.AUTO, SpeakToChatConfig.Timeout.STANDARD), "fc:0c:00:01");
        }});
    }

    @Test
    public void getAudioUpsampling() {
        // TODO
    }

    @Test
    public void setAudioUpsampling() {
        final Request requestEnabled = protocol.setAudioUpsampling(new AudioUpsampling(true));
        assertRequest(requestEnabled, "3e:0c:00:00:00:00:03:e8:01:01:f9:3c");

        final Request requestDisabled = protocol.setAudioUpsampling(new AudioUpsampling(false));
        assertRequest(requestDisabled, "3e:0c:01:00:00:00:03:e8:01:00:f9:3c");
    }

    @Test
    public void getAutomaticPowerOff() {
        // TODO
    }

    @Test
    public void setAutomaticPowerOff() {
        assertRequests(protocol::setAutomaticPowerOff, new LinkedHashMap<AutomaticPowerOff, String>() {{
            put(AutomaticPowerOff.OFF, "28:05:11:00");
            put(AutomaticPowerOff.WHEN_TAKEN_OFF, "28:05:10:00");
        }});
    }

    @Test
    public void getButtonModes() {
        // TODO
    }

    @Test
    public void setButtonModes() {
        // TODO
    }

    @Test
    public void getQuickAccess() {
        final Request request = protocol.getQuickAccess();
        assertRequest(request, "3e:0c:00:00:00:00:02:f6:0d:11:3c");
    }

    @Test
    public void setQuickAccess() {
        final Map<QuickAccess, String> commands = new LinkedHashMap<QuickAccess, String>() {{
            put(new QuickAccess(QuickAccess.Mode.OFF, QuickAccess.Mode.OFF), "3e:0c:01:00:00:00:05:f8:0d:02:00:00:19:3c");
            put(new QuickAccess(QuickAccess.Mode.OFF, QuickAccess.Mode.SPOTIFY), "3e:0c:00:00:00:00:05:f8:0d:02:00:01:19:3c");
            put(new QuickAccess(QuickAccess.Mode.SPOTIFY, QuickAccess.Mode.OFF), "3e:0c:00:00:00:00:05:f8:0d:02:01:00:19:3c");
        }};

        for (Map.Entry<QuickAccess, String> entry : commands.entrySet()) {
            final Request request = protocol.setQuickAccess(entry.getKey());
            assertRequest(request, entry.getValue());
        }
    }

    @Test
    public void getAmbientSoundControlButtonMode() {
        final Request request = protocol.getAmbientSoundControlButtonMode();
        assertRequest(request, "3e:0c:00:00:00:00:02:fa:03:0b:3c");
    }

    @Test
    public void setAmbientSoundControlButtonMode() {
        final Map<AmbientSoundControlButtonMode, String> commands = new LinkedHashMap<AmbientSoundControlButtonMode, String>() {{
            put(AmbientSoundControlButtonMode.NC_AS_OFF, "3e:0c:00:00:00:00:07:fc:03:01:35:01:00:01:4a:3c");
            put(AmbientSoundControlButtonMode.NC_AS, "3e:0c:01:00:00:00:07:fc:03:01:35:01:00:02:4c:3c");
            put(AmbientSoundControlButtonMode.NC_OFF, "3e:0c:01:00:00:00:07:fc:03:01:35:01:00:03:4d:3c");
            put(AmbientSoundControlButtonMode.AS_OFF, "3e:0c:01:00:00:00:07:fc:03:01:35:01:00:04:4e:3c");
        }};

        for (Map.Entry<AmbientSoundControlButtonMode, String> entry : commands.entrySet()) {
            final Request request = protocol.setAmbientSoundControlButtonMode(entry.getKey());
            assertRequest(request, entry.getValue());
        }
    }

    @Test
    public void getPauseWhenTakenOff() {
        // TODO
    }

    @Test
    public void setPauseWhenTakenOff() {
        assertRequests(protocol::setPauseWhenTakenOff, new LinkedHashMap<PauseWhenTakenOff, String>() {{
            put(new PauseWhenTakenOff(false), "f8:01:01");
            put(new PauseWhenTakenOff(true), "f8:01:00");
        }});
    }

    @Test
    public void getEqualizer() {
        final Request requestDisabled = protocol.getEqualizer();
        assertRequest(requestDisabled, "3e:0c:00:00:00:00:02:56:00:64:3c");
    }

    @Test
    public void setEqualizerPreset() {
        final Map<EqualizerPreset, String> commands = new LinkedHashMap<EqualizerPreset, String>() {{
            put(EqualizerPreset.OFF, "3e:0c:01:00:00:00:04:58:00:00:00:69:3c");
            put(EqualizerPreset.BRIGHT, "3e:0c:01:00:00:00:04:58:00:10:00:79:3c");
            put(EqualizerPreset.EXCITED, "3e:0c:01:00:00:00:04:58:00:11:00:7a:3c");
            put(EqualizerPreset.MELLOW, "3e:0c:01:00:00:00:04:58:00:12:00:7b:3c");
            put(EqualizerPreset.RELAXED, "3e:0c:01:00:00:00:04:58:00:13:00:7c:3c");
            put(EqualizerPreset.VOCAL, "3e:0c:01:00:00:00:04:58:00:14:00:7d:3c");
            put(EqualizerPreset.TREBLE_BOOST, "3e:0c:01:00:00:00:04:58:00:15:00:7e:3c");
            put(EqualizerPreset.BASS_BOOST, "3e:0c:01:00:00:00:04:58:00:16:00:7f:3c");
            put(EqualizerPreset.SPEECH, "3e:0c:01:00:00:00:04:58:00:17:00:80:3c");
            put(EqualizerPreset.MANUAL, "3e:0c:01:00:00:00:04:58:00:a0:00:09:3c");
            put(EqualizerPreset.CUSTOM_1, "3e:0c:01:00:00:00:04:58:00:a1:00:0a:3c");
            put(EqualizerPreset.CUSTOM_2, "3e:0c:01:00:00:00:04:58:00:a2:00:0b:3c");
        }};

        for (Map.Entry<EqualizerPreset, String> entry : commands.entrySet()) {
            final Request request = protocol.setEqualizerPreset(entry.getKey());
            assertRequest(request, entry.getValue());
        }
    }

    @Test
    public void setEqualizerCustomBands() {
        assertRequests(protocol::setEqualizerCustomBands, new LinkedHashMap<EqualizerCustomBands, String>() {{
            put(new EqualizerCustomBands(Arrays.asList(0, 1, 2, 3, 1), 0), "58:00:a0:06:0a:0a:0b:0c:0d:0b");
            put(new EqualizerCustomBands(Arrays.asList(0, 1, 2, 3, 5), 0), "58:00:a0:06:0a:0a:0b:0c:0d:0f");
            put(new EqualizerCustomBands(Arrays.asList(0, 1, 2, 4, 5), 0), "58:00:a0:06:0a:0a:0b:0c:0e:0f");
            put(new EqualizerCustomBands(Arrays.asList(5, 1, 2, 3, 5), 0), "58:00:a0:06:0a:0f:0b:0c:0d:0f");
            put(new EqualizerCustomBands(Arrays.asList(0, 1, 2, 3, 5), -6), "58:00:a0:06:04:0a:0b:0c:0d:0f");
            put(new EqualizerCustomBands(Arrays.asList(0, 1, 2, 3, 5), 10), "58:00:a0:06:14:0a:0b:0c:0d:0f");
        }});
    }

    @Test
    @Ignore("Not implemented on V2")
    public void getSoundPosition() {
    }

    @Test
    @Ignore("Not implemented on V2")
    public void setSoundPosition() {
    }

    @Test
    @Ignore("Not implemented on V2")
    public void getSurroundMode() {
    }

    @Test
    @Ignore("Not implemented on V2")
    public void setSurroundMode() {
    }

    @Test
    @Ignore("Not implemented on V2")
    public void getTouchSensor() {
    }

    @Test
    @Ignore("Not implemented on V2")
    public void setTouchSensor() {
    }

    @Test
    @Ignore("Not implemented on V2")
    public void getVoiceNotifications() {
    }

    @Test
    public void setVoiceNotifications() {
        assertRequests(protocol::setVoiceNotifications, 0x0e, new LinkedHashMap<VoiceNotifications, String>() {{
            put(new VoiceNotifications(false), "48:01:01");
            put(new VoiceNotifications(true), "48:01:00");
        }});
    }

    @Test
    @Ignore("Not implemented on V2")
    public void startNoiseCancellingOptimizer() {
    }

    @Test
    public void powerOff() {
        final Request request = protocol.powerOff();
        assertRequest(request, 0x0c, "24:03:01");
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
    @Ignore("Not implemented on V2")
    public void handleNoiseCancellingOptimizerStatus() {
    }

    @Test
    @Ignore("Not implemented on V2")
    public void handleNoiseCancellingOptimizerState() {
    }

    @Test
    public void handleAudioUpsampling() {
        final Map<AudioUpsampling, String> commands = new LinkedHashMap<AudioUpsampling, String>() {{
            put(new AudioUpsampling(false), "3e:0c:00:00:00:00:03:e9:01:00:f9:3c");
            put(new AudioUpsampling(true), "3e:0c:01:00:00:00:03:e9:01:01:fb:3c");
        }};

        for (Map.Entry<AudioUpsampling, String> entry : commands.entrySet()) {
            final List<? extends GBDeviceEvent> events = handleMessage(protocol, entry.getValue());
            assertEquals("Expect 1 events", 1, events.size());
            final GBDeviceEventUpdatePreferences event = (GBDeviceEventUpdatePreferences) events.get(0);
            final Object modePrefValue = entry.getKey()
                    .toPreferences()
                    .get(DeviceSettingsPreferenceConst.PREF_SONY_AUDIO_UPSAMPLING);
            assertEquals(modePrefValue, event.preferences.get(DeviceSettingsPreferenceConst.PREF_SONY_AUDIO_UPSAMPLING));
        }
    }

    @Test
    public void handleAutomaticPowerOff() {
        // TODO
    }

    @Test
    public void handleButtonModes() {
        // TODO
    }

    @Test
    public void handlePauseWhenTakenOff() {
        final Map<PauseWhenTakenOff, String> commands = new LinkedHashMap<PauseWhenTakenOff, String>() {{
            put(new PauseWhenTakenOff(false), "3e:0c:00:00:00:00:03:f9:01:01:0a:3c");
            put(new PauseWhenTakenOff(true), "3e:0c:01:00:00:00:03:f9:01:00:0a:3c");
        }};

        for (Map.Entry<PauseWhenTakenOff, String> entry : commands.entrySet()) {
            final List<? extends GBDeviceEvent> events = handleMessage(protocol, entry.getValue());
            assertEquals("Expect 1 events", 1, events.size());
            final GBDeviceEventUpdatePreferences event = (GBDeviceEventUpdatePreferences) events.get(0);
            final Object modePrefValue = entry.getKey()
                    .toPreferences()
                    .get(DeviceSettingsPreferenceConst.PREF_SONY_PAUSE_WHEN_TAKEN_OFF);
            assertEquals(modePrefValue, event.preferences.get(DeviceSettingsPreferenceConst.PREF_SONY_PAUSE_WHEN_TAKEN_OFF));
        }
    }

    @Test
    public void handleBattery() {
        // TODO
    }

    @Test
    public void handleAudioCodec() {
        // TODO
    }

    @Test
    public void handleEqualizer() {
        final Map<EqualizerPreset, String> commands = new LinkedHashMap<EqualizerPreset, String>() {{
            put(EqualizerPreset.OFF, "3e:0c:01:00:00:00:0a:59:00:00:06:0a:0a:0a:0a:0a:0a:b2:3c");
            put(EqualizerPreset.BRIGHT, "3e:0c:01:00:00:00:0a:59:00:10:06:09:0a:0f:11:11:13:dd:3c");
            put(EqualizerPreset.EXCITED, "3e:0c:01:00:00:00:0a:59:00:11:06:12:09:0b:0a:0d:0f:d3:3c");
            put(EqualizerPreset.MELLOW, "3e:0c:01:00:00:00:0a:59:00:12:06:07:09:08:07:06:04:b1:3c");
            put(EqualizerPreset.RELAXED, "3e:0c:01:00:00:00:0a:59:00:13:06:01:07:09:07:05:02:a8:3c");
            put(EqualizerPreset.VOCAL, "3e:0c:01:00:00:00:0a:59:00:14:06:0a:10:0e:0c:0d:09:d4:3c");
            put(EqualizerPreset.TREBLE_BOOST, "3e:0c:01:00:00:00:0a:59:00:15:06:0a:0a:0a:0c:10:14:d9:3c");
            put(EqualizerPreset.BASS_BOOST, "3e:0c:01:00:00:00:0a:59:00:16:06:11:0a:0a:0a:0a:0a:cf:3c");
            put(EqualizerPreset.SPEECH, "3e:0c:01:00:00:00:0a:59:00:17:06:00:0e:0d:0b:0c:00:bf:3c");
            put(EqualizerPreset.MANUAL, "3e:0c:01:00:00:00:0a:59:00:a0:06:0a:0a:0a:0a:0a:0a:52:3c");
            put(EqualizerPreset.CUSTOM_1, "3e:0c:01:00:00:00:0a:59:00:a1:06:0a:0a:0a:0a:0a:0a:53:3c");
            put(EqualizerPreset.CUSTOM_2, "3e:0c:01:00:00:00:0a:59:00:a2:06:0a:0a:0a:0a:0a:0a:54:3c");
        }};

        for (Map.Entry<EqualizerPreset, String> entry : commands.entrySet()) {
            final List<? extends GBDeviceEvent> events = handleMessage(protocol, entry.getValue());
            assertEquals("Expect 1 events", 1, events.size());
            final GBDeviceEventUpdatePreferences event = (GBDeviceEventUpdatePreferences) events.get(0);
            final Object modePrefValue = entry.getKey()
                    .toPreferences()
                    .get(DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_MODE);
            assertNotNull(modePrefValue);
            assertEquals(modePrefValue, event.preferences.get(DeviceSettingsPreferenceConst.PREF_SONY_EQUALIZER_MODE));
        }
    }

    @Test
    public void handleFirmwareVersion() {
        // TODO
    }

    @Test
    @Ignore("Not implemented on V2")
    public void handleJson() {
    }

    @Test
    public void handleAutomaticPowerOffButtonMode() {
        // TODO
    }

    @Test
    @Ignore("Not implemented on V2")
    public void handleVirtualSound() {
    }

    @Test
    @Ignore("Not implemented on V2")
    public void handleSoundPosition() {
    }

    @Test
    @Ignore("Not implemented on V2")
    public void handleSurroundMode() {
    }

    @Test
    @Ignore("Not implemented on V2")
    public void handleTouchSensor() {
    }

    @Test
    @Ignore("Not implemented on V2")
    public void handleVoiceNotifications() {
    }

    @Test
    public void handleQuickAccess() {
        final Map<String, QuickAccess> commands = new LinkedHashMap<String, QuickAccess>() {{
            // Ret
            put("3e:0c:00:00:00:00:05:f7:0d:02:00:00:17:3c", new QuickAccess(QuickAccess.Mode.OFF, QuickAccess.Mode.OFF));
            put("3e:0c:01:00:00:00:05:f7:0d:02:00:01:19:3c", new QuickAccess(QuickAccess.Mode.OFF, QuickAccess.Mode.SPOTIFY));
            put("3e:0c:01:00:00:00:05:f7:0d:02:01:00:19:3c", new QuickAccess(QuickAccess.Mode.SPOTIFY, QuickAccess.Mode.OFF));

            // Notify
            put("3e:0c:00:00:00:00:05:f9:0d:02:00:00:19:3c", new QuickAccess(QuickAccess.Mode.OFF, QuickAccess.Mode.OFF));
            put("3e:0c:01:00:00:00:05:f9:0d:02:00:01:1b:3c", new QuickAccess(QuickAccess.Mode.OFF, QuickAccess.Mode.SPOTIFY));
            put("3e:0c:01:00:00:00:05:f9:0d:02:01:00:1b:3c", new QuickAccess(QuickAccess.Mode.SPOTIFY, QuickAccess.Mode.OFF));
        }};

        for (Map.Entry<String, QuickAccess> entry : commands.entrySet()) {
            final List<? extends GBDeviceEvent> events = handleMessage(protocol, entry.getKey());
            assertPrefs(events, entry.getValue().toPreferences());
        }
    }

    @Test
    public void handleAmbientSoundControlButtonMode() {
        final Map<AmbientSoundControlButtonMode, String> commands = new LinkedHashMap<AmbientSoundControlButtonMode, String>() {{
            // Notify
            put(AmbientSoundControlButtonMode.NC_AS_OFF, "3e:0c:01:00:00:00:07:fd:03:01:35:01:00:01:4c:3c");
            put(AmbientSoundControlButtonMode.NC_AS, "3e:0c:00:00:00:00:07:fd:03:01:35:01:00:02:4c:3c");
            put(AmbientSoundControlButtonMode.NC_OFF, "3e:0c:00:00:00:00:07:fd:03:01:35:01:00:03:4d:3c");
            put(AmbientSoundControlButtonMode.AS_OFF, "3e:0c:01:00:00:00:07:fd:03:01:35:01:00:04:4f:3c");
        }};

        for (Map.Entry<AmbientSoundControlButtonMode, String> entry : commands.entrySet()) {
            final List<? extends GBDeviceEvent> events = handleMessage(protocol, entry.getValue());
            assertEquals("Expect 1 events", 1, events.size());
            final GBDeviceEventUpdatePreferences event = (GBDeviceEventUpdatePreferences) events.get(0);
            final Map<String, Object> expectedPrefs = entry.getKey().toPreferences();
            assertEquals("Expect 1 prefs", 1, expectedPrefs.size());
            final Object modePrefValue = expectedPrefs
                    .get(DeviceSettingsPreferenceConst.PREF_SONY_AMBIENT_SOUND_CONTROL_BUTTON_MODE);
            assertNotNull(modePrefValue);
            assertEquals(modePrefValue, event.preferences.get(DeviceSettingsPreferenceConst.PREF_SONY_AMBIENT_SOUND_CONTROL_BUTTON_MODE));
        }
    }
}
