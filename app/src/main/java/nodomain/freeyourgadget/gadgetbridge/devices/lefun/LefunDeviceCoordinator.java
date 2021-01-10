/*  Copyright (C) 2016-2021 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, José Rebelo, Petr Kadlec, protomors, Yukai Li

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
package nodomain.freeyourgadget.gadgetbridge.devices.lefun;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;

import static nodomain.freeyourgadget.gadgetbridge.devices.lefun.LefunConstants.ADVERTISEMENT_NAME;
import static nodomain.freeyourgadget.gadgetbridge.devices.lefun.LefunConstants.MANUFACTURER_NAME;
import static nodomain.freeyourgadget.gadgetbridge.devices.lefun.LefunConstants.NUM_ALARM_SLOTS;

/**
 * Device coordinator for Lefun band
 */
public class LefunDeviceCoordinator extends AbstractDeviceCoordinator {
    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws GBException {

    }

    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_NONE;
    }

    @NonNull
    @Override
    public DeviceType getSupportedType(GBDeviceCandidate candidate) {
        // There's a bunch of other names other than "Lefun", but let's just focus on one for now.
        if (ADVERTISEMENT_NAME.equals(candidate.getName())) {
            // The device does not advertise service UUIDs, so can't check whether it supports
            // the proper service. We can check that it doesn't advertise any services, though.
            // We're actually supposed to check for presence of the string "TJDR" within the
            // manufacturer specific data, which consists of the device's MAC address and said
            // string. But we're not being given it, so *shrug*.
            if (candidate.getServiceUuids().length == 0) {
                return DeviceType.LEFUN;
            }
        }

        return DeviceType.UNKNOWN;
    }

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.LEFUN;
    }

    @Nullable
    @Override
    public Class<? extends Activity> getPairingActivity() {
        return null;
    }

    @Override
    public boolean supportsActivityDataFetching() {
        return true;
    }

    @Override
    public boolean supportsActivityTracking() {
        return true;
    }

    @Override
    public SampleProvider<? extends ActivitySample> getSampleProvider(GBDevice device, DaoSession session) {
        return new LefunSampleProvider(device, session);
    }

    @Override
    public InstallHandler findInstallHandler(Uri uri, Context context) {
        return null;
    }

    @Override
    public boolean supportsScreenshots() {
        return false;
    }

    @Override
    public int getAlarmSlotCount() {
        return NUM_ALARM_SLOTS;
    }

    @Override
    public boolean supportsSmartWakeup(GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsHeartRateMeasurement(GBDevice device) {
        return true;
    }

    @Override
    public String getManufacturer() {
        return MANUFACTURER_NAME;
    }

    @Override
    public boolean supportsAppsManagement() {
        return false;
    }

    @Override
    public Class<? extends Activity> getAppsManagementActivity() {
        return null;
    }

    @Override
    public boolean supportsCalendarEvents() {
        return false;
    }

    @Override
    public boolean supportsRealtimeData() {
        return true;
    }

    @Override
    public boolean supportsWeather() {
        return false;
    }

    @Override
    public boolean supportsFindDevice() {
        return true;
    }

    @Override
    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        return new int[]{
                R.xml.devicesettings_liftwrist_display_noshed,
                R.xml.devicesettings_timeformat,
                R.xml.devicesettings_antilost,
                R.xml.devicesettings_longsit,
                R.xml.devicesettings_hydration_reminder,
                R.xml.devicesettings_lefun_interface_language,
        };
    }
}
