/*  Copyright (C) 2019 krzys_h

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
package nodomain.freeyourgadget.gadgetbridge.devices.moyoung;

import android.annotation.TargetApi;
import android.bluetooth.le.ScanFilter;
import android.os.Build;
import android.os.ParcelUuid;

import androidx.annotation.NonNull;

import java.util.Collection;
import java.util.Collections;

import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.samples.MoyoungActivitySampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.samples.MoyoungSpo2SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.settings.MoyoungEnumDeviceVersion;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.settings.MoyoungEnumMetricSystem;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.settings.MoyoungEnumTimeSystem;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.settings.MoyoungSetting;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.settings.MoyoungSettingBool;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.settings.MoyoungSettingByte;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.settings.MoyoungSettingEnum;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.settings.MoyoungSettingInt;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.settings.MoyoungSettingLanguage;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.settings.MoyoungSettingRemindersToMove;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.settings.MoyoungSettingTimeRange;
import nodomain.freeyourgadget.gadgetbridge.devices.moyoung.settings.MoyoungSettingUserInfo;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.MoyoungActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.MoyoungBloodPressureSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.MoyoungHeartRateSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.MoyoungSpo2SampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.Spo2Sample;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.moyoung.MoyoungDeviceSupport;

public abstract class AbstractMoyoungDeviceCoordinator extends AbstractDeviceCoordinator {

    @NonNull
    @Override
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public Collection<? extends ScanFilter> createBLEScanFilters() {
        ParcelUuid service = new ParcelUuid(MoyoungConstants.UUID_SERVICE_MOYOUNG);
        ScanFilter filter = new ScanFilter.Builder().setServiceUuid(service).build();
        return Collections.singletonList(filter);
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass() {
        return MoyoungDeviceSupport.class;
    }

    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_NONE;
    }

    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws GBException {
        Long deviceId = device.getId();
        QueryBuilder<?> qb;

        qb = session.getMoyoungActivitySampleDao().queryBuilder();
        qb.where(MoyoungActivitySampleDao.Properties.DeviceId.eq(deviceId)).buildDelete().executeDeleteWithoutDetachingEntities();
        qb = session.getMoyoungHeartRateSampleDao().queryBuilder();
        qb.where(MoyoungHeartRateSampleDao.Properties.DeviceId.eq(deviceId)).buildDelete().executeDeleteWithoutDetachingEntities();
        qb = session.getMoyoungSpo2SampleDao().queryBuilder();
        qb.where(MoyoungSpo2SampleDao.Properties.DeviceId.eq(deviceId)).buildDelete().executeDeleteWithoutDetachingEntities();
        qb = session.getMoyoungBloodPressureSampleDao().queryBuilder();
        qb.where(MoyoungBloodPressureSampleDao.Properties.DeviceId.eq(deviceId)).buildDelete().executeDeleteWithoutDetachingEntities();
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
    public boolean supportsSpo2(GBDevice device) {
        return true;
    }

    @Override
    public SampleProvider<? extends ActivitySample> getSampleProvider(GBDevice device, DaoSession session) {
        return new MoyoungActivitySampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends Spo2Sample> getSpo2SampleProvider(GBDevice device, DaoSession session) {
        return new MoyoungSpo2SampleProvider(device, session);
    }

    @Override
    public int getAlarmSlotCount(GBDevice device) {
        return 3;
    }

    @Override
    public boolean supportsHeartRateMeasurement(GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsCalendarEvents() {
        return true;
    }

    @Override
    public boolean supportsRealtimeData() {
        return true;
    }

    @Override
    public boolean supportsWeather() {
        return true;
    }

    @Override
    public boolean supportsFindDevice() {
        return true;
    }

    @Override
    public boolean supportsActivityTracks() {
        return true;
    }

    @Override
    public boolean supportsMusicInfo() {
        return true;
    }

    private static final MoyoungSetting[] MOYOUNG_SETTINGS = new MoyoungSetting[] {
        new MoyoungSettingUserInfo("USER_INFO", MoyoungConstants.CMD_SET_USER_INFO),
        new MoyoungSettingByte("STEP_LENGTH", (byte)-1, MoyoungConstants.CMD_SET_STEP_LENGTH),
        // (*) new MoyoungSettingEnum<>("DOMINANT_HAND", MoyoungConstants.CMD_QUERY_DOMINANT_HAND, MoyoungConstants.CMD_SET_DOMINANT_HAND, MoyoungEnumDominantHand.class),
        new MoyoungSettingInt("GOAL_STEP", MoyoungConstants.CMD_QUERY_GOAL_STEP, MoyoungConstants.CMD_SET_GOAL_STEP),

        new MoyoungSettingEnum<>("DEVICE_VERSION", MoyoungConstants.CMD_QUERY_DEVICE_VERSION, MoyoungConstants.CMD_SET_DEVICE_VERSION, MoyoungEnumDeviceVersion.class),
        new MoyoungSettingLanguage("DEVICE_LANGUAGE", MoyoungConstants.CMD_QUERY_DEVICE_LANGUAGE, MoyoungConstants.CMD_SET_DEVICE_LANGUAGE),
        new MoyoungSettingEnum<>("TIME_SYSTEM", MoyoungConstants.CMD_QUERY_TIME_SYSTEM, MoyoungConstants.CMD_SET_TIME_SYSTEM, MoyoungEnumTimeSystem.class),
        new MoyoungSettingEnum<>("METRIC_SYSTEM", MoyoungConstants.CMD_QUERY_METRIC_SYSTEM, MoyoungConstants.CMD_SET_METRIC_SYSTEM, MoyoungEnumMetricSystem.class),

        // (*) new MoyoungSetting("DISPLAY_DEVICE_FUNCTION", MoyoungConstants.CMD_QUERY_DISPLAY_DEVICE_FUNCTION, MoyoungConstants.CMD_SET_DISPLAY_DEVICE_FUNCTION),
        // (*) new MoyoungSetting("SUPPORT_WATCH_FACE", MoyoungConstants.CMD_QUERY_SUPPORT_WATCH_FACE, (byte)-1),
        // (*) new MoyoungSetting("WATCH_FACE_LAYOUT", MoyoungConstants.CMD_QUERY_WATCH_FACE_LAYOUT, MoyoungConstants.CMD_SET_WATCH_FACE_LAYOUT),
        new MoyoungSettingByte("DISPLAY_WATCH_FACE", MoyoungConstants.CMD_QUERY_DISPLAY_WATCH_FACE, MoyoungConstants.CMD_SET_DISPLAY_WATCH_FACE),
        new MoyoungSettingBool("OTHER_MESSAGE_STATE", MoyoungConstants.CMD_QUERY_OTHER_MESSAGE_STATE, MoyoungConstants.CMD_SET_OTHER_MESSAGE_STATE),

        new MoyoungSettingBool("QUICK_VIEW", MoyoungConstants.CMD_QUERY_QUICK_VIEW, MoyoungConstants.CMD_SET_QUICK_VIEW),
        new MoyoungSettingTimeRange("QUICK_VIEW_TIME", MoyoungConstants.CMD_QUERY_QUICK_VIEW_TIME, MoyoungConstants.CMD_SET_QUICK_VIEW_TIME),
        new MoyoungSettingBool("SEDENTARY_REMINDER", MoyoungConstants.CMD_QUERY_SEDENTARY_REMINDER, MoyoungConstants.CMD_SET_SEDENTARY_REMINDER),
        new MoyoungSettingRemindersToMove("REMINDERS_TO_MOVE_PERIOD", MoyoungConstants.CMD_QUERY_REMINDERS_TO_MOVE_PERIOD, MoyoungConstants.CMD_SET_REMINDERS_TO_MOVE_PERIOD),
        new MoyoungSettingTimeRange("DO_NOT_DISTURB_TIME", MoyoungConstants.CMD_QUERY_DO_NOT_DISTURB_TIME, MoyoungConstants.CMD_SET_DO_NOT_DISTURB_TIME),
        // (*) new MoyoungSetting("PSYCHOLOGICAL_PERIOD", MoyoungConstants.CMD_QUERY_PSYCHOLOGICAL_PERIOD, MoyoungConstants.CMD_SET_PSYCHOLOGICAL_PERIOD),

        new MoyoungSettingBool("BREATHING_LIGHT", MoyoungConstants.CMD_QUERY_BREATHING_LIGHT, MoyoungConstants.CMD_SET_BREATHING_LIGHT)
    };

    @Override
    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        return new int[]{
            //R.xml.devicesettings_steplength, // TODO is this needed? does it work? write-only so hard to tell
            R.xml.devicesettings_moyoung_device_version,
            R.xml.devicesettings_moyoung_language,
            R.xml.devicesettings_timeformat,
            R.xml.devicesettings_measurementsystem,
            R.xml.devicesettings_moyoung_watchface,
            //R.xml.devicesettings_moyoung_othermessage, // not implemented because this doesn't really do anything on the watch side, only enables/disables sending of "other" notifications in the app (no idea why they store the setting on the watch)
            R.xml.devicesettings_liftwrist_display,
            R.xml.devicesettings_moyoung_sedentary_reminder,
            R.xml.devicesettings_donotdisturb_no_auto,
            //R.xml.devicesettings_moyoung_breathinglight, // No idea what this does but it doesn't seem to change anything
            R.xml.devicesettings_world_clocks,
        };
    }

    public MoyoungSetting[] getSupportedSettings() {
        return MOYOUNG_SETTINGS;
    }
}
