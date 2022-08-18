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
package nodomain.freeyourgadget.gadgetbridge.devices.huami;

import android.content.Context;
import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.capabilities.password.PasswordCapabilityImpl;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;

public abstract class Huami2021Coordinator extends HuamiCoordinator {
    private static final Logger LOG = LoggerFactory.getLogger(Huami2021Coordinator.class);

    @Override
    public boolean supportsHeartRateMeasurement(final GBDevice device) {
        // TODO: One-shot HR measures are not working, so let's disable this for now
        return false;
    }

    @Override
    public boolean supportsRealtimeData() {
        return true;
    }

    @Override
    public boolean supportsWeather() {
        // TODO: It's supported by the devices, but not yet implemented
        return false;
    }

    @Override
    public boolean supportsUnicodeEmojis() {
        return true;
    }

    @Override
    public boolean supportsActivityTracking() {
        // TODO: It's supported by the devices, but not yet implemented
        return false;
    }

    @Override
    public boolean supportsActivityDataFetching() {
        // TODO: It's supported by the devices, but not yet implemented
        return false;
    }

    @Override
    public boolean supportsActivityTracks() {
        // TODO: It's supported by the devices, but not yet implemented
        return false;
    }

    @Override
    public boolean supportsMusicInfo() {
        return true;
    }

    @Override
    public int getWorldClocksSlotCount() {
        // TODO: It's supported, but not implemented - even in the official app
        return 0;
    }

    @Override
    public boolean supportsCalendarEvents() {
        return true;
    }

    @Override
    public SampleProvider<? extends AbstractActivitySample> getSampleProvider(final GBDevice device, final DaoSession session) {
        // TODO: It's supported by the devices, but not yet implemented
        return null;
    }

    @Override
    public boolean supportsAlarmSnoozing() {
        // All alarms snooze by default, there doesn't seem to be a flag that disables it
        return false;
    }

    @Override
    public int getReminderSlotCount() {
        return 50;
    }

    @Override
    public int[] getSupportedDeviceSpecificAuthenticationSettings() {
        return new int[]{
                R.xml.devicesettings_pairingkey
        };
    }

    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_REQUIRE_KEY;
    }
}
