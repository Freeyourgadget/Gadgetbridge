/*  Copyright (C) 2023 akasaka / Genjitsu Labs

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

package nodomain.freeyourgadget.gadgetbridge.devices.sony.wena3;

import android.app.Activity;
import android.bluetooth.le.ScanFilter;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;

import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.Wena3ActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.Wena3BehaviorSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.Wena3CaloriesSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.Wena3EnergySampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.Wena3HeartRateSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.Wena3StressSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.Wena3Vo2SampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.AbstractNotificationPattern;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.model.HeartRateSample;
import nodomain.freeyourgadget.gadgetbridge.model.StressSample;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.SonyWena3DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.notification.defines.LedColor;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.notification.defines.VibrationCount;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.notification.defines.VibrationKind;

public class SonyWena3Coordinator extends AbstractBLEDeviceCoordinator {
    @Nullable
    @Override
    public Class<? extends Activity> getPairingActivity() {
        return null;
    }

    @Override
    public String getManufacturer() {
        return "Sony";
    }

    @Override
    public boolean supportsAppsManagement(GBDevice device) {
        return false;
    }

    @Override
    public Class<? extends Activity> getAppsManagementActivity() {
        return null;
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass() {
        return SonyWena3DeviceSupport.class;
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_sony_wena3;
    }

    @Override
    public DeviceSpecificSettingsCustomizer getDeviceSpecificSettingsCustomizer(GBDevice device) {
        return new SonyWena3SettingsCustomizer();
    }

    @NonNull
    @Override
    public Collection<? extends ScanFilter> createBLEScanFilters() {
        ScanFilter.Builder builder = new ScanFilter.Builder();
        builder.setDeviceName(SonyWena3Constants.BT_DEVICE_NAME);
        ArrayList<ScanFilter> result = new ArrayList<>();
        result.add(builder.build());
        return result;
    }

    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) {
        Long deviceId = device.getId();
        QueryBuilder<?> qb;
        
        qb = session.getWena3HeartRateSampleDao().queryBuilder();
        qb.where(Wena3HeartRateSampleDao.Properties.DeviceId.eq(deviceId)).buildDelete().executeDeleteWithoutDetachingEntities();
        qb = session.getWena3BehaviorSampleDao().queryBuilder();
        qb.where(Wena3BehaviorSampleDao.Properties.DeviceId.eq(deviceId)).buildDelete().executeDeleteWithoutDetachingEntities();
        qb = session.getWena3CaloriesSampleDao().queryBuilder();
        qb.where(Wena3CaloriesSampleDao.Properties.DeviceId.eq(deviceId)).buildDelete().executeDeleteWithoutDetachingEntities();
        qb = session.getWena3EnergySampleDao().queryBuilder();
        qb.where(Wena3EnergySampleDao.Properties.DeviceId.eq(deviceId)).buildDelete().executeDeleteWithoutDetachingEntities();
        qb = session.getWena3ActivitySampleDao().queryBuilder();
        qb.where(Wena3ActivitySampleDao.Properties.DeviceId.eq(deviceId)).buildDelete().executeDeleteWithoutDetachingEntities();
        qb = session.getWena3Vo2SampleDao().queryBuilder();
        qb.where(Wena3Vo2SampleDao.Properties.DeviceId.eq(deviceId)).buildDelete().executeDeleteWithoutDetachingEntities();
        qb = session.getWena3StressSampleDao().queryBuilder();
        qb.where(Wena3StressSampleDao.Properties.DeviceId.eq(deviceId)).buildDelete().executeDeleteWithoutDetachingEntities();
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile(SonyWena3Constants.BT_DEVICE_NAME);
    }
    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_BOND;
    }

    @Override
    public boolean supportsCalendarEvents() {
        return true;
    }

    @Override
    public boolean supportsRealtimeData() {
        return false;
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
    public boolean supportsAppReordering() {
        return false;
    }

    @Override
    public boolean supportsStressMeasurement() {
        return true;
    }

    @Override
    public TimeSampleProvider<? extends StressSample> getStressSampleProvider(GBDevice device, DaoSession session) {
        return new SonyWena3StressSampleProvider(device, session);
    }

    @Override
    public boolean supportsSpo2() {
        return false;
    }

    @Override
    public boolean supportsHeartRateMeasurement(GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsHeartRateStats() {
        return true;
    }

    @Override
    public SampleProvider<? extends ActivitySample> getSampleProvider(GBDevice device, DaoSession session) {
        return new SonyWena3ActivitySampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends HeartRateSample> getHeartRateRestingSampleProvider(GBDevice device, DaoSession session) {
        return new SonyWena3HeartRateSampleProvider(device, session);
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
    public int getAlarmSlotCount(GBDevice device) {
        return SonyWena3Constants.ALARM_SLOTS;
    }

    @Override
    public boolean supportsSmartWakeup(GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsMusicInfo() {
        return true;
    }

    @Override
    public boolean supportsFindDevice() {
        return false;
    }

    @Override
    public boolean supportsWeather() {
        return true;
    }

    @Override
    public boolean isExperimental() {
        return false;
    }

    @Override
    public String[] getSupportedLanguageSettings(GBDevice device) {
        return new String[]{
                "auto",
                "en_US",
                "ja_JP"
        };
    }

    @Override
    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        return new int[]{
                R.xml.devicesettings_notifications_enable,
                R.xml.devicesettings_sync_calendar,
                R.xml.devicesettings_wearlocation,
                R.xml.devicesettings_donotdisturb_no_auto,
                R.xml.devicesettings_wena3_auto_power_off,
                R.xml.devicesettings_goal_notification,
                R.xml.devicesettings_wena3,
        };
    }


    public boolean supportsNotificationVibrationPatterns() {
        return true;
    }

    public boolean supportsNotificationVibrationRepetitionPatterns() {
        return true;
    }

    public boolean supportsNotificationLedPatterns() {
        return true;
    }

    public AbstractNotificationPattern[] getNotificationVibrationPatterns() {
        return new AbstractNotificationPattern[] {
                VibrationKind.NONE, VibrationKind.BASIC,
                VibrationKind.CONTINUOUS, VibrationKind.RAPID,
                VibrationKind.TRIPLE, VibrationKind.STEP_UP, VibrationKind.STEP_DOWN,
                VibrationKind.WARNING, VibrationKind.SIREN, VibrationKind.SHORT
        };
    }

    public AbstractNotificationPattern[] getNotificationVibrationRepetitionPatterns() {
        return new AbstractNotificationPattern[] {
                VibrationCount.ONCE, VibrationCount.TWICE, VibrationCount.THREE, VibrationCount.FOUR,
                VibrationCount.INDEFINITE
        };
    }

    public AbstractNotificationPattern[] getNotificationLedPatterns() {
        return new AbstractNotificationPattern[] {
                LedColor.NONE, LedColor.RED, LedColor.YELLOW, LedColor.GREEN,
                LedColor.CYAN, LedColor.BLUE, LedColor.PURPLE, LedColor.WHITE
        };
    }
}
