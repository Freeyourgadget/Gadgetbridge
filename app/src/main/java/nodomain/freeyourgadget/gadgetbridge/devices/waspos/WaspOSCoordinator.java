/*  Copyright (C) 2016-2020 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, Gordon Williams, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.waspos;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.le.ScanFilter;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelUuid;

import androidx.annotation.NonNull;

import java.util.Collection;
import java.util.Collections;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.waspos.WaspOSConstants;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;

public class WaspOSCoordinator extends AbstractDeviceCoordinator {

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.WASPOS;
    }

    @Override
    public String getManufacturer() {
        return "MicroPython";
    }

    @NonNull
    @Override
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public Collection<? extends ScanFilter> createBLEScanFilters() {
        // TODO: filter on name beginning Bangle.js? Doesn't appear to be built-in :(
        // https://developer.android.com/reference/android/bluetooth/le/ScanFilter.Builder.html#setDeviceName(java.lang.String)
        ParcelUuid hpService = new ParcelUuid(WaspOSConstants.UUID_SERVICE_NORDIC_UART);
        ScanFilter filter = new ScanFilter.Builder().setServiceUuid(hpService).build();
        return Collections.singletonList(filter);
    }

    @NonNull
    @Override
    public DeviceType getSupportedType(GBDeviceCandidate candidate) {
        String name = candidate.getDevice().getName();
        /* Filter by Espruino devices to avoid getting
        the device chooser full of spam devices. */
        if (name != null && (
              name.startsWith("DS-D6") ||
              name.startsWith("K9") ||
              name.startsWith("PineTime") ||
              name.startsWith("P8")))
            return DeviceType.WASPOS;

        return DeviceType.UNKNOWN;
    }

    @Override
    public int getBondingStyle(){
        return BONDING_STYLE_NONE;
    }

    @Override
    public boolean supportsCalendarEvents() {
        return false;
    }

    @Override
    public boolean supportsRealtimeData() {
        return false;
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
    public boolean supportsActivityDataFetching() {
        return false;
    }

    @Override
    public boolean supportsActivityTracking() {
        return false;
    }

    @Override
    public boolean supportsScreenshots() {
        return false;
    }

    @Override
    public boolean supportsSmartWakeup(GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsHeartRateMeasurement(GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsAppsManagement() {
        return false;
    }

    @Override
    public int getAlarmSlotCount() {
        return 0;
    }

    @Override
    public Class<? extends Activity> getAppsManagementActivity() {
        return null;
    }


    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) {
    }

    @Override
    public Class<? extends Activity> getPairingActivity() {
        return null;
    }

    @Override
    public SampleProvider<? extends ActivitySample> getSampleProvider(GBDevice device, DaoSession session) {
        return null;//new WaspOSSampleProvider(device, session);
    }

    @Override
    public InstallHandler findInstallHandler(Uri uri, Context context) {
        return null;
    }
    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        return new int[]{
                R.xml.devicesettings_transliteration
        };
    }

}
