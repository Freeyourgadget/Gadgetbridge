/*  Copyright (C) 2023 Daniele Gobbetti, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.nothing;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLClassicDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryConfig;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.nothing.Ear1Support;

public abstract class AbstractEarCoordinator extends AbstractBLClassicDeviceCoordinator {
    @Override
    public InstallHandler findInstallHandler(Uri uri, Context context) {
        return null;
    }

    @Override
    public String getManufacturer() {
        return "Nothing";
    }

    @Override
    public boolean supportsFindDevice() {
        return true;
    }

    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws GBException {

    }

    @Override
    public int getBatteryCount() {
        return 3;
    }

    @Override
    public BatteryConfig[] getBatteryConfig() {
        BatteryConfig battery1 = new BatteryConfig(0, R.drawable.ic_tws_case, R.string.battery_case);
        BatteryConfig battery2 = new BatteryConfig(1, R.drawable.ic_nothing_ear_l, R.string.left_earbud);
        BatteryConfig battery3 = new BatteryConfig(2, R.drawable.ic_nothing_ear_r, R.string.right_earbud);
        return new BatteryConfig[]{battery1, battery2, battery3};
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass() {
        return Ear1Support.class;
    }

    @Override
    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        return new int[]{
                R.xml.devicesettings_nothing_ear1
        };
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_nothingear;
    }

    @Override
    public int getDisabledIconResource() {
        return R.drawable.ic_device_nothingear_disabled;
    }

    @Override
    public DeviceSpecificSettingsCustomizer getDeviceSpecificSettingsCustomizer(final GBDevice device) {
        return new EarSettingsCustomizer();
    }

    public abstract boolean incrementCounter();

    public abstract boolean supportsLightAncAndTransparency();
}
