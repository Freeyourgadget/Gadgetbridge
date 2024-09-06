/*  Copyright (C) 2024 Damien Gaignon, Martin.JM

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

package nodomain.freeyourgadget.gadgetbridge.devices.huawei;

import android.app.Activity;
import android.bluetooth.le.ScanFilter;
import android.content.Context;
import android.net.Uri;
import android.os.ParcelUuid;

import androidx.annotation.NonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettings;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummaryDao;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutDataSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutSummarySample;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiWorkoutSummarySampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.Spo2Sample;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiLESupport;

public abstract class HuaweiLECoordinator extends AbstractBLEDeviceCoordinator implements HuaweiCoordinatorSupplier {

    private final HuaweiCoordinator huaweiCoordinator = new HuaweiCoordinator(this);
    private GBDevice device;

    @Override
    public HuaweiCoordinator getHuaweiCoordinator() {
        return huaweiCoordinator;
    }

    @NonNull
    @Override
    public Collection<? extends ScanFilter> createBLEScanFilters() {
        ParcelUuid huaweiService = new ParcelUuid(HuaweiConstants.UUID_SERVICE_HUAWEI_SERVICE);
        ScanFilter filter = new ScanFilter.Builder().setServiceUuid(huaweiService).build();
        return Collections.singletonList(filter);
    }

    @Override
    public DeviceSpecificSettingsCustomizer getDeviceSpecificSettingsCustomizer(final GBDevice device) {
        return new HuaweiSettingsCustomizer(device);
    }

    @Override
    public String[] getSupportedLanguageSettings(GBDevice device) {
        return huaweiCoordinator.getSupportedLanguageSettings(device);
    }

    @Override
    public int[] getSupportedDeviceSpecificAuthenticationSettings() {
        return new int[]{
                R.xml.devicesettings_huawei_account,
                R.xml.devicesettings_miband6_new_protocol
        };
    }

    @Override
    public int getBondingStyle(){
        return BONDING_STYLE_NONE;
    }

    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws GBException {
        long deviceId = device.getId();
        QueryBuilder<?> qb = session.getHuaweiActivitySampleDao().queryBuilder();
        qb.where(HuaweiActivitySampleDao.Properties.DeviceId.eq(deviceId)).buildDelete().executeDeleteWithoutDetachingEntities();

        QueryBuilder<HuaweiWorkoutSummarySample> qb2 = session.getHuaweiWorkoutSummarySampleDao().queryBuilder();
        List<HuaweiWorkoutSummarySample> workouts = qb2.where(HuaweiWorkoutSummarySampleDao.Properties.DeviceId.eq(deviceId)).build().list();
        for (HuaweiWorkoutSummarySample sample : workouts) {
            session.getHuaweiWorkoutDataSampleDao().queryBuilder().where(
                    HuaweiWorkoutDataSampleDao.Properties.WorkoutId.eq(sample.getWorkoutId())
            ).buildDelete().executeDeleteWithoutDetachingEntities();
        }

        session.getHuaweiWorkoutSummarySampleDao().queryBuilder().where(HuaweiWorkoutSummarySampleDao.Properties.DeviceId.eq(deviceId)).buildDelete().executeDeleteWithoutDetachingEntities();

        session.getBaseActivitySummaryDao().queryBuilder().where(BaseActivitySummaryDao.Properties.DeviceId.eq(deviceId)).buildDelete().executeDeleteWithoutDetachingEntities();
    }

    @Override
    public String getManufacturer() {
        return "Huawei";
    }

    @Override
    public boolean supportsSmartWakeup(GBDevice device, int position) {
        return huaweiCoordinator.supportsSmartAlarm(device, position);
    }

    @Override
    public boolean supportsSmartWakeupInterval(GBDevice device, int alarmPosition) {
        return supportsSmartWakeup(device, alarmPosition);
    }

    @Override
    public boolean forcedSmartWakeup(GBDevice device, int alarmPosition) {
        return huaweiCoordinator.forcedSmartWakeup(device, alarmPosition);
    }

    @Override
    public boolean supportsAlarmTitle(GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsWeather() {
        return huaweiCoordinator.supportsWeather();
    }

    @Override
    public Class<? extends Activity> getAppsManagementActivity() {
        return huaweiCoordinator.getAppManagerActivity();
    }

    @Override
    public boolean supportsAppListFetching() {
        return huaweiCoordinator.getSupportsAppListFetching();
    }
    @Override
    public boolean supportsAppsManagement(GBDevice device) {
        return huaweiCoordinator.getSupportsAppsManagement(device);
    }

    @Override
    public boolean supportsWatchfaceManagement(GBDevice device) {
        return supportsAppsManagement(device);
    }

    @Override
    public boolean supportsInstalledAppManagement(GBDevice device) {
        return huaweiCoordinator.getSupportsInstalledAppManagement(device);
    }

    @Override
    public boolean supportsCachedAppManagement(GBDevice device) {
        return huaweiCoordinator.getSupportsCachedAppManagement(device);
    }

    @Override
    public int getAlarmSlotCount(GBDevice device) {
        return huaweiCoordinator.getAlarmSlotCount(device);
    }

    @Override
    public int getContactsSlotCount(GBDevice device) {
        return huaweiCoordinator.getContactsSlotCount(device);
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
    public boolean supportsActivityTracks() {
        return true;
    }

    @Override
    public boolean supportsHeartRateMeasurement(GBDevice device) {
        return huaweiCoordinator.supportsHeartRate(device);
    }

    @Override
    public boolean supportsSpo2(GBDevice device) {
        return huaweiCoordinator.supportsSPo2(device);
    }

    @Override
    public boolean supportsMusicInfo() {
        return huaweiCoordinator.supportsMusic();
    }

    @Override
    public InstallHandler findInstallHandler(Uri uri, Context context) {
        return huaweiCoordinator.getInstallHandler(uri, context);
    }

    @Override
    public SampleProvider<? extends AbstractActivitySample> getSampleProvider(GBDevice device, DaoSession session) {
        return new HuaweiSampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends Spo2Sample> getSpo2SampleProvider(GBDevice device, DaoSession session) {
        return new HuaweiSpo2SampleProvider(device, session);
    }

    public DeviceSpecificSettings getDeviceSpecificSettings(final GBDevice device) {
        return huaweiCoordinator.getDeviceSpecificSettings(device);
    }

    @Override
    public HuaweiDeviceType getHuaweiType() {
        return HuaweiDeviceType.BLE;
    }

    @Override
    public void setDevice(GBDevice device) {
        this.device = device;
    }

    @Override
    public GBDevice getDevice() {
        return this.device;
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass() {
        return HuaweiLESupport.class;
    }

    @Override
    public boolean addBatteryPollingSettings() {
        return true;
    }
}
