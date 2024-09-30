/*  Copyright (C) 2024 Arjan Schrijver

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
package nodomain.freeyourgadget.gadgetbridge.devices.colmi;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettings;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsScreen;
import nodomain.freeyourgadget.gadgetbridge.capabilities.HeartRateCapability;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.colmi.samples.ColmiActivitySampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.colmi.samples.ColmiHrvValueSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.colmi.samples.ColmiSpo2SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.colmi.samples.ColmiStressSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.ColmiActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.ColmiHeartRateSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.ColmiHrvValueSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.ColmiSleepSessionSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.ColmiSleepStageSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.ColmiSpo2SampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.ColmiStressSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.HrvValueSample;
import nodomain.freeyourgadget.gadgetbridge.model.Spo2Sample;
import nodomain.freeyourgadget.gadgetbridge.model.StressSample;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.colmi.ColmiR0xDeviceSupport;

public abstract class AbstractColmiR0xCoordinator extends AbstractBLEDeviceCoordinator {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractColmiR0xCoordinator.class);

    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws GBException {
        final Long deviceId = device.getId();

        final Map<AbstractDao<?, ?>, Property> daoMap = new HashMap<AbstractDao<?, ?>, Property>() {{
            put(session.getColmiActivitySampleDao(), ColmiActivitySampleDao.Properties.DeviceId);
            put(session.getColmiHeartRateSampleDao(), ColmiHeartRateSampleDao.Properties.DeviceId);
            put(session.getColmiSpo2SampleDao(), ColmiSpo2SampleDao.Properties.DeviceId);
            put(session.getColmiStressSampleDao(), ColmiStressSampleDao.Properties.DeviceId);
            put(session.getColmiSleepSessionSampleDao(), ColmiSleepSessionSampleDao.Properties.DeviceId);
            put(session.getColmiSleepStageSampleDao(), ColmiSleepStageSampleDao.Properties.DeviceId);
            put(session.getColmiHrvValueSampleDao(), ColmiHrvValueSampleDao.Properties.DeviceId);
        }};

        for (final Map.Entry<AbstractDao<?, ?>, Property> e : daoMap.entrySet()) {
            e.getKey().queryBuilder()
                    .where(e.getValue().eq(deviceId))
                    .buildDelete().executeDeleteWithoutDetachingEntities();
        }
    }

    @Override
    public String getManufacturer() {
        return "Colmi";
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass() {
        return ColmiR0xDeviceSupport.class;
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_smartring;
    }

    @Override
    public int getDisabledIconResource() {
        return R.drawable.ic_device_smartring_disabled;
    }

    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_NONE;
    }

    @Override
    public boolean supportsPowerOff() {
        return true;
    }

    @Override
    public boolean supportsFindDevice() {
        return true;
    }

    @Override
    public boolean supportsActivityTracking() {
        return true;
    }

    @Override
    public boolean supportsActivityDataFetching() {
        return true;
    }

    @Override
    public boolean supportsRealtimeData() {
        return true;
    }

    @Override
    public boolean supportsStressMeasurement() {
        return true;
    }

    @Override
    public boolean supportsSpo2(GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsHeartRateStats() {
        return true;
    }

    @Override
    public boolean supportsHeartRateMeasurement(GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsManualHeartRateMeasurement(GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsRemSleep() {
        return true;
    }

    @Override
    public boolean supportsAwakeSleep() {
        return true;
    }

    @Override
    public boolean supportsHrvMeasurement() {
        return true;
    }

    @Override
    public SampleProvider<? extends ActivitySample> getSampleProvider(GBDevice device, DaoSession session) {
        return new ColmiActivitySampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends Spo2Sample> getSpo2SampleProvider(GBDevice device, DaoSession session) {
        return new ColmiSpo2SampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends StressSample> getStressSampleProvider(GBDevice device, DaoSession session) {
        return new ColmiStressSampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends HrvValueSample> getHrvValueSampleProvider(final GBDevice device, final DaoSession session) {
        return new ColmiHrvValueSampleProvider(device, session);
    }

    @Override
    public List<HeartRateCapability.MeasurementInterval> getHeartRateMeasurementIntervals() {
        return Arrays.asList(
                HeartRateCapability.MeasurementInterval.OFF,
                HeartRateCapability.MeasurementInterval.MINUTES_5,
                HeartRateCapability.MeasurementInterval.MINUTES_10,
                HeartRateCapability.MeasurementInterval.MINUTES_15,
                HeartRateCapability.MeasurementInterval.MINUTES_30,
                HeartRateCapability.MeasurementInterval.MINUTES_45,
                HeartRateCapability.MeasurementInterval.HOUR_1
        );
    }

    @Override
    public int[] getStressRanges() {
        // 1-29 = relaxed
        // 30-59 = normal
        // 60-79 = medium
        // 80-99 = high
        return new int[]{1, 30, 60, 80};
    }

    @Override
    public DeviceSpecificSettings getDeviceSpecificSettings(final GBDevice device) {
        final DeviceSpecificSettings deviceSpecificSettings = new DeviceSpecificSettings();
        final List<Integer> health = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.HEALTH);
        health.add(R.xml.devicesettings_colmi_r0x);
        return deviceSpecificSettings;
    }
}
