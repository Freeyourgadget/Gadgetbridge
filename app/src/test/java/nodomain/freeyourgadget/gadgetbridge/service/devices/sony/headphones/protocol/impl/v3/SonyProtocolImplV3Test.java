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
package nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.impl.v3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.impl.SonyTestUtils.assertPrefs;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.impl.SonyTestUtils.assertRequest;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.impl.SonyTestUtils.assertRequests;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.impl.SonyTestUtils.handleMessage;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.SonyHeadphonesCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.AmbientSoundControl;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.AmbientSoundControlButtonMode;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.AutomaticPowerOff;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.EqualizerCustomBands;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.PauseWhenTakenOff;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.QuickAccess;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.SpeakToChatConfig;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.SpeakToChatEnabled;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.VoiceNotifications;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.Request;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.impl.MockSonyCoordinator;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.impl.v1.params.BatteryType;

public class SonyProtocolImplV3Test {
    private final MockSonyCoordinator coordinator = new MockSonyCoordinator();
    private final SonyProtocolImplV3 protocol = new SonyProtocolImplV3(null) {
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
    public void setAmbientSoundControl() {
        final Map<AmbientSoundControl, String> commands = new LinkedHashMap<AmbientSoundControl, String>() {{
            put(new AmbientSoundControl(AmbientSoundControl.Mode.AMBIENT_SOUND, false, 20), "68:17:01:01:01:00:14");
            put(new AmbientSoundControl(AmbientSoundControl.Mode.OFF, false, 20), "68:17:01:00:00:00:14");
            put(new AmbientSoundControl(AmbientSoundControl.Mode.AMBIENT_SOUND, false, 10), "68:17:01:01:01:00:0a");
            put(new AmbientSoundControl(AmbientSoundControl.Mode.AMBIENT_SOUND, true, 20), "68:17:01:01:01:01:14");
            put(new AmbientSoundControl(AmbientSoundControl.Mode.NOISE_CANCELLING, false, 20), "68:17:01:01:00:00:14");
        }};

        for (Map.Entry<AmbientSoundControl, String> entry : commands.entrySet()) {
            final Request request = protocol.setAmbientSoundControl(entry.getKey());
            assertRequest(request, 0x0c, entry.getValue());
        }
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
    public void getBattery() {
        final Map<BatteryType, String> commands = new LinkedHashMap<BatteryType, String>() {{
            put(BatteryType.SINGLE, "22:00");
            put(BatteryType.DUAL, "22:09");
            put(BatteryType.CASE, "22:0a");
        }};

        for (Map.Entry<BatteryType, String> entry : commands.entrySet()) {
            final Request request = protocol.getBattery(entry.getKey());
            assertRequest(request, 0x0c, entry.getValue());
        }
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
    public void setPauseWhenTakenOff() {
        assertRequests(protocol::setPauseWhenTakenOff, new LinkedHashMap<PauseWhenTakenOff, String>() {{
            put(new PauseWhenTakenOff(false), "f8:01:01");
            put(new PauseWhenTakenOff(true), "f8:01:00");
        }});
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
    public void setAutomaticPowerOff() {
        assertRequests(protocol::setAutomaticPowerOff, new LinkedHashMap<AutomaticPowerOff, String>() {{
            put(AutomaticPowerOff.OFF, "28:05:11:00");
            put(AutomaticPowerOff.WHEN_TAKEN_OFF, "28:05:10:00");
        }});
    }

    @Test
    public void setVoiceNotifications() {
        assertRequests(protocol::setVoiceNotifications, 0x0e, new LinkedHashMap<VoiceNotifications, String>() {{
            put(new VoiceNotifications(false), "48:01:01");
            put(new VoiceNotifications(true), "48:01:00");
        }});
    }

    @Test
    public void powerOff() {
        final Request request = protocol.powerOff();
        assertRequest(request, 0x0c, "24:03:01");
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
