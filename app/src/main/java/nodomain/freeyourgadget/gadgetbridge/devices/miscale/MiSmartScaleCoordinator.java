/*  Copyright (C) 2024 Severin von Wnuck-Lipinski

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
package nodomain.freeyourgadget.gadgetbridge.devices.miscale;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettings;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.MiScaleWeightSampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.WeightSample;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miscale.MiSmartScaleDeviceSupport;

public class MiSmartScaleCoordinator extends AbstractBLEDeviceCoordinator {
    @Override
    public String getManufacturer() {
        return "Huami";
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("MI SCALE2");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_mismartscale;
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_miscale;
    }

    @Override
    public int getDisabledIconResource() {
        return R.drawable.ic_device_miscale_disabled;
    }

    @Override
    public int getBatteryCount() {
        return 0;
    }

    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_NONE;
    }

    @Override
    protected void deleteDevice(@NonNull final GBDevice gbDevice, @NonNull final Device device, @NonNull final DaoSession session) throws GBException {
        final Long deviceId = device.getId();
        final QueryBuilder<?> qb = session.getMiScaleWeightSampleDao().queryBuilder();

        qb.where(MiScaleWeightSampleDao.Properties.DeviceId.eq(deviceId)).buildDelete().executeDeleteWithoutDetachingEntities();
    }

    @Override
    public DeviceSpecificSettings getDeviceSpecificSettings(final GBDevice device) {
        final DeviceSpecificSettings settings = new DeviceSpecificSettings();

        settings.addRootScreen(R.xml.devicesettings_mismartscale);

        return settings;
    }

    @Override
    public TimeSampleProvider<? extends WeightSample> getWeightSampleProvider(final GBDevice device, final DaoSession session) {
        return new MiScaleSampleProvider(device, session);
    }

    @Override
    public boolean supportsWeightMeasurement() {
        return true;
    }

    @Override
    public boolean supportsActivityTracking() {
        return true;
    }

    @Override
    public boolean supportsActivityTabs() {
        return false;
    }

    @Override
    public boolean supportsSleepMeasurement() {
        return false;
    }

    @Override
    public boolean supportsStepCounter() {
        return false;
    }

    @Override
    public boolean supportsSpeedzones() {
        return false;
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass() {
        return MiSmartScaleDeviceSupport.class;
    }
}
