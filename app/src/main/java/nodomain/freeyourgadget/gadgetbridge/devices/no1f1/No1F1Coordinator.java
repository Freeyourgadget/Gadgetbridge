/*  Copyright (C) 2016-2019 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, José Rebelo, Petr Kadlec, protomors

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
package nodomain.freeyourgadget.gadgetbridge.devices.no1f1;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.le.ScanFilter;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelUuid;

import java.util.Collection;
import java.util.Collections;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.No1F1ActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;

public class No1F1Coordinator extends AbstractDeviceCoordinator {

    @NonNull
    @Override
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public Collection<? extends ScanFilter> createBLEScanFilters() {
        ParcelUuid hpService = new ParcelUuid(No1F1Constants.UUID_SERVICE_NO1);
        ScanFilter filter = new ScanFilter.Builder().setServiceUuid(hpService).build();
        return Collections.singletonList(filter);
    }

    @NonNull
    @Override
    public DeviceType getSupportedType(GBDeviceCandidate candidate) {
        String name = candidate.getDevice().getName();
        if (name != null && (name.startsWith("X-RUN") || name.startsWith("MH30"))) {
            return DeviceType.NO1F1;
        }

        return DeviceType.UNKNOWN;
    }

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.NO1F1;
    }

    @Override
    public int getBondingStyle(GBDevice deviceCandidate) {
        return BONDING_STYLE_NONE;
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
        return new No1F1SampleProvider(device, session);
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
        return 3;
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
        return "NO1";
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
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws GBException {
        Long deviceId = device.getId();
        QueryBuilder<?> qb = session.getNo1F1ActivitySampleDao().queryBuilder();
        qb.where(No1F1ActivitySampleDao.Properties.DeviceId.eq(deviceId)).buildDelete().executeDeleteWithoutDetachingEntities();
    }
}
