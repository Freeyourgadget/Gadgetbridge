/*  Copyright (C) 2016-2017 Carsten Pfeiffer, José Rebelo

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

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.le.ScanFilter;
import android.content.Context;
import android.os.Build;
import android.os.ParcelUuid;
import android.support.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.SettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.DateTimeDisplay;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.DoNotDisturb;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBand2SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandPairingActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandService;
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.MiBandActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public abstract class HuamiCoordinator extends AbstractDeviceCoordinator {
    private static final Logger LOG = LoggerFactory.getLogger(HuamiCoordinator.class);

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.MIBAND2;
    }

    @Override
    public Class<? extends Activity> getPairingActivity() {
        return MiBandPairingActivity.class;
    }

    @NonNull
    @Override
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public Collection<? extends ScanFilter> createBLEScanFilters() {
        ParcelUuid mi2Service = new ParcelUuid(MiBandService.UUID_SERVICE_MIBAND2_SERVICE);
        ScanFilter filter = new ScanFilter.Builder().setServiceUuid(mi2Service).build();
        return Collections.singletonList(filter);
    }

    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws GBException {
        Long deviceId = device.getId();
        QueryBuilder<?> qb = session.getMiBandActivitySampleDao().queryBuilder();
        qb.where(MiBandActivitySampleDao.Properties.DeviceId.eq(deviceId)).buildDelete().executeDeleteWithoutDetachingEntities();
    }

    @Override
    public String getManufacturer() {
        return "Huami";
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
    public boolean supportsAlarmConfiguration() {
        return true;
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
    public SampleProvider<? extends AbstractActivitySample> getSampleProvider(GBDevice device, DaoSession session) {
        return new MiBand2SampleProvider(device, session);
    }

    public static DateTimeDisplay getDateDisplay(Context context) throws IllegalArgumentException {
        Prefs prefs = GBApplication.getPrefs();
        String dateFormatTime = context.getString(R.string.p_dateformat_time);
        if (dateFormatTime.equals(prefs.getString(MiBandConst.PREF_MI2_DATEFORMAT, dateFormatTime))) {
            return DateTimeDisplay.TIME;
        }
        return DateTimeDisplay.DATE_TIME;
    }

    public static boolean getActivateDisplayOnLiftWrist() {
        Prefs prefs = GBApplication.getPrefs();
        return prefs.getBoolean(MiBandConst.PREF_MI2_ACTIVATE_DISPLAY_ON_LIFT, true);
    }

    public static Set<String> getDisplayItems() {
        Prefs prefs = GBApplication.getPrefs();
        return prefs.getStringSet(MiBandConst.PREF_MI2_DISPLAY_ITEMS, null);
    }

    public static boolean getGoalNotification() {
        Prefs prefs = GBApplication.getPrefs();
        return prefs.getBoolean(MiBandConst.PREF_MI2_GOAL_NOTIFICATION, false);
    }

    public static boolean getRotateWristToSwitchInfo() {
        Prefs prefs = GBApplication.getPrefs();
        return prefs.getBoolean(MiBandConst.PREF_MI2_ROTATE_WRIST_TO_SWITCH_INFO, false);
    }

    public static boolean getInactivityWarnings() {
        Prefs prefs = GBApplication.getPrefs();
        return prefs.getBoolean(MiBandConst.PREF_MI2_INACTIVITY_WARNINGS, false);
    }

    public static int getInactivityWarningsThreshold() {
        Prefs prefs = GBApplication.getPrefs();
        return prefs.getInt(MiBandConst.PREF_MI2_INACTIVITY_WARNINGS_THRESHOLD, 60);
    }

    public static boolean getInactivityWarningsDnd() {
        Prefs prefs = GBApplication.getPrefs();
        return prefs.getBoolean(MiBandConst.PREF_MI2_INACTIVITY_WARNINGS_DND, false);
    }

    public static Date getInactivityWarningsStart() {
        return getTimePreference(MiBandConst.PREF_MI2_INACTIVITY_WARNINGS_START, "06:00");
    }

    public static Date getInactivityWarningsEnd() {
        return getTimePreference(MiBandConst.PREF_MI2_INACTIVITY_WARNINGS_END, "22:00");
    }

    public static Date getInactivityWarningsDndStart() {
        return getTimePreference(MiBandConst.PREF_MI2_INACTIVITY_WARNINGS_DND_START, "12:00");
    }

    public static Date getInactivityWarningsDndEnd() {
        return getTimePreference(MiBandConst.PREF_MI2_INACTIVITY_WARNINGS_DND_END, "14:00");
    }

    public static Date getDoNotDisturbStart() {
        return getTimePreference(MiBandConst.PREF_MI2_DO_NOT_DISTURB_START, "01:00");
    }

    public static Date getDoNotDisturbEnd() {
        return getTimePreference(MiBandConst.PREF_MI2_DO_NOT_DISTURB_END, "06:00");
    }

    public static Date getTimePreference(String key, String defaultValue) {
        Prefs prefs = GBApplication.getPrefs();
        String time = prefs.getString(key, defaultValue);

        DateFormat df = new SimpleDateFormat("HH:mm");
        try {
            return df.parse(time);
        } catch(Exception e) {
            LOG.error("Unexpected exception in MiBand2Coordinator.getTime: " + e.getMessage());
        }

        return new Date();
    }

    public static MiBandConst.DistanceUnit getDistanceUnit() {
        Prefs prefs = GBApplication.getPrefs();
        String unit = prefs.getString(SettingsActivity.PREF_MEASUREMENT_SYSTEM, GBApplication.getContext().getString(R.string.p_unit_metric));
        if (unit.equals(GBApplication.getContext().getString(R.string.p_unit_metric))) {
            return MiBandConst.DistanceUnit.METRIC;
        } else {
            return MiBandConst.DistanceUnit.IMPERIAL;
        }
    }

    public static DoNotDisturb getDoNotDisturb(Context context) {
        Prefs prefs = GBApplication.getPrefs();

        String dndOff = context.getString(R.string.p_off);
        String dndAutomatic = context.getString(R.string.p_automatic);
        String dndScheduled = context.getString(R.string.p_scheduled);

        String pref = prefs.getString(MiBandConst.PREF_MI2_DO_NOT_DISTURB, dndOff);

        if (dndAutomatic.equals(pref)) {
            return DoNotDisturb.AUTOMATIC;
        } else if (dndScheduled.equals(pref)) {
            return DoNotDisturb.SCHEDULED;
        }

        return DoNotDisturb.OFF;
    }

    @Override
    public boolean supportsScreenshots() {
        return false;
    }

    @Override
    public boolean supportsSmartWakeup(GBDevice device) {
        return false;
    }
}
