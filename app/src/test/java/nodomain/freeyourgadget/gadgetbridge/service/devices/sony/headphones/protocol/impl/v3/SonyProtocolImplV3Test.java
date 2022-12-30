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
import static nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.impl.SonyTestUtils.assertRequest;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.impl.SonyTestUtils.handleMessage;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.SonyHeadphonesCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.coordinators.SonyLinkBudsSCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.AmbientSoundControlButtonMode;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.prefs.QuickAccess;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.Request;

public class SonyProtocolImplV3Test {
    private final SonyProtocolImplV3 protocol = new SonyProtocolImplV3(null) {
        @Override
        protected SonyHeadphonesCoordinator getCoordinator() {
            return new SonyLinkBudsSCoordinator();
        }
    };

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
    public void handleQuickAccess() {
        final Map<QuickAccess, String> commands = new LinkedHashMap<QuickAccess, String>() {{
            // Notify
            put(new QuickAccess(QuickAccess.Mode.OFF, QuickAccess.Mode.OFF), "3e:0c:00:00:00:00:05:f9:0d:02:00:00:19:3c");
            put(new QuickAccess(QuickAccess.Mode.OFF, QuickAccess.Mode.SPOTIFY), "3e:0c:01:00:00:00:05:f9:0d:02:00:01:1b:3c");
            put(new QuickAccess(QuickAccess.Mode.SPOTIFY, QuickAccess.Mode.OFF), "3e:0c:01:00:00:00:05:f9:0d:02:01:00:1b:3c");
        }};

        for (Map.Entry<QuickAccess, String> entry : commands.entrySet()) {
            final List<? extends GBDeviceEvent> events = handleMessage(protocol, entry.getValue());
            assertEquals("Expect 1 events", 1, events.size());
            final GBDeviceEventUpdatePreferences event = (GBDeviceEventUpdatePreferences) events.get(0);
            final Map<String, Object> expectedPrefs = entry.getKey().toPreferences();
            assertEquals("Expect 2 prefs", 2, expectedPrefs.size());
            final Object prefDoubleTap = expectedPrefs.get(DeviceSettingsPreferenceConst.PREF_SONY_QUICK_ACCESS_DOUBLE_TAP);
            assertNotNull(prefDoubleTap);
            assertEquals(prefDoubleTap, event.preferences.get(DeviceSettingsPreferenceConst.PREF_SONY_QUICK_ACCESS_DOUBLE_TAP));
            final Object prefTripleTap = expectedPrefs.get(DeviceSettingsPreferenceConst.PREF_SONY_QUICK_ACCESS_TRIPLE_TAP);
            assertNotNull(prefTripleTap);
            assertEquals(prefTripleTap, event.preferences.get(DeviceSettingsPreferenceConst.PREF_SONY_QUICK_ACCESS_TRIPLE_TAP));
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
