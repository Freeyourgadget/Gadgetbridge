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

import static nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.impl.SonyTestUtils.assertRequest;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.impl.SonyTestUtils.handleMessage;

import org.junit.Ignore;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.SonyHeadphonesCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.coordinators.SonyWF1000XM4Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.AmbientSoundControl;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.AudioUpsampling;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.EqualizerPreset;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.PauseWhenTakenOff;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.Request;

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
        final Request request = protocol.setAmbientSoundControl(new AmbientSoundControl(
                AmbientSoundControl.Mode.WIND_NOISE_REDUCTION,
                true,
                15
        ));
        assertRequest(request, "3e:0c:00:00:00:00:08:68:15:01:01:00:03:01:0f:a6:3c");
    }

    @Test
    public void getNoiseCancellingOptimizerState() {
        // TODO
    }

    @Test
    public void getAudioCodec() {
        // TODO
    }

    @Test
    public void getBattery() {
        // TODO
    }

    @Test
    public void getFirmwareVersion() {
        // TODO
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
        // TODO
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
    public void getPauseWhenTakenOff() {
        // TODO
    }

    @Test
    public void setPauseWhenTakenOff() {
        final Request requestEnabled = protocol.setPauseWhenTakenOff(new PauseWhenTakenOff(true));
        assertRequest(requestEnabled, "3e:0c:01:00:00:00:03:f8:01:00:09:3c");

        final Request requestDisabled = protocol.setPauseWhenTakenOff(new PauseWhenTakenOff(false));
        assertRequest(requestDisabled, "3e:0c:00:00:00:00:03:f8:01:01:09:3c");
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
        // TODO
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
    @Ignore("Not implemented on V2")
    public void setVoiceNotifications() {
    }

    @Test
    @Ignore("Not implemented on V2")
    public void startNoiseCancellingOptimizer() {
    }

    @Test
    @Ignore("Not implemented on V2")
    public void powerOff() {
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
}
