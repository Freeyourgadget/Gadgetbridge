/*  Copyright (C) 2016-2017 Andreas Shimokawa, Carsten Pfeiffer, João
    Paulo Barraca

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
package nodomain.freeyourgadget.gadgetbridge.devices.hplus;

/*
* @author João Paulo Barraca &lt;jpbarraca@gmail.com&gt;
*/

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.le.ScanFilter;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelUuid;
import android.support.annotation.NonNull;

import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.ChartsActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.HPlusHealthActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

import static nodomain.freeyourgadget.gadgetbridge.GBApplication.getContext;

public class HPlusCoordinator extends AbstractDeviceCoordinator {
    protected static final Logger LOG = LoggerFactory.getLogger(HPlusCoordinator.class);
    protected static Prefs prefs  = GBApplication.getPrefs();

    @NonNull
    @Override
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public Collection<? extends ScanFilter> createBLEScanFilters() {
        ParcelUuid hpService = new ParcelUuid(HPlusConstants.UUID_SERVICE_HP);
        ScanFilter filter = new ScanFilter.Builder().setServiceUuid(hpService).build();
        return Collections.singletonList(filter);
    }

    @NonNull
    @Override
    public DeviceType getSupportedType(GBDeviceCandidate candidate) {
        String name = candidate.getDevice().getName();
        if (name != null && name.startsWith("HPLUS")) {
            return DeviceType.HPLUS;
        }

        return DeviceType.UNKNOWN;
    }

    @Override
    public int getBondingStyle(GBDevice deviceCandidate){
        return BONDING_STYLE_NONE;
    }

    @Override
    public boolean supportsCalendarEvents() {
        return false;
    }

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.HPLUS;
    }

    @Override
    public Class<? extends Activity> getPairingActivity() {
        return null;
    }

    @Override
    public Class<? extends Activity> getPrimaryActivity() {
        return ChartsActivity.class;
    }

    @Override
    public InstallHandler findInstallHandler(Uri uri, Context context) {
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
        return new HPlusHealthSampleProvider(device, session);
    }

    @Override
    public boolean supportsScreenshots() {
        return false;
    }

    @Override
    public boolean supportsAlarmConfiguration() {
        return true;
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
        return "Zeblaze";
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
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws GBException {
        Long deviceId = device.getId();
        QueryBuilder<?> qb = session.getHPlusHealthActivitySampleDao().queryBuilder();
        qb.where(HPlusHealthActivitySampleDao.Properties.DeviceId.eq(deviceId)).buildDelete().executeDeleteWithoutDetachingEntities();
    }

    public static byte getLanguage(String address) {
        String language = prefs.getString("language", "default");
        Locale locale;

        if (language.equals("default")) {
            locale = Locale.getDefault();
        } else {
            locale = new Locale(language);
        }

        if (locale.getLanguage().equals(new Locale("cn").getLanguage())){
            return HPlusConstants.ARG_LANGUAGE_CN;
        }else{
            return HPlusConstants.ARG_LANGUAGE_EN;
        }
    }

    public static byte getTimeMode(String address) {
        String tmode = prefs.getString(HPlusConstants.PREF_HPLUS_TIMEFORMAT, getContext().getString(R.string.p_timeformat_24h));

        if(tmode.equals(getContext().getString(R.string.p_timeformat_24h))) {
            return HPlusConstants.ARG_TIMEMODE_24H;
        }else{
            return HPlusConstants.ARG_TIMEMODE_12H;
        }
    }

    public static byte getUnit(String address) {
        String units = prefs.getString(HPlusConstants.PREF_HPLUS_UNIT, getContext().getString(R.string.p_unit_metric));

        if(units.equals(getContext().getString(R.string.p_unit_metric))){
            return HPlusConstants.ARG_UNIT_METRIC;
        }else{
            return HPlusConstants.ARG_UNIT_IMPERIAL;
        }
    }

    public static byte getUserWeight() {
        ActivityUser activityUser = new ActivityUser();

        return (byte) (activityUser.getWeightKg() & 0xFF);
    }

    public static byte getUserHeight() {
        ActivityUser activityUser = new ActivityUser();

        return (byte) (activityUser.getHeightCm() & 0xFF);
    }

    public static byte getUserAge() {
        ActivityUser activityUser = new ActivityUser();

        return (byte) (activityUser.getAge() & 0xFF);
    }

    public static byte getUserGender() {
        ActivityUser activityUser = new ActivityUser();

        if (activityUser.getGender() == ActivityUser.GENDER_MALE)
            return HPlusConstants.ARG_GENDER_MALE;

        return HPlusConstants.ARG_GENDER_FEMALE;
    }

    public static int getGoal() {
        ActivityUser activityUser = new ActivityUser();

        return activityUser.getStepsGoal();
    }

    public static byte getScreenTime(String address) {
        return (byte) (prefs.getInt(HPlusConstants.PREF_HPLUS_SCREENTIME, 5) & 0xFF);
    }

    public static byte getAllDayHR(String address) {
        Boolean value = (prefs.getBoolean(HPlusConstants.PREF_HPLUS_ALLDAYHR, true));

        if(value){
            return HPlusConstants.ARG_HEARTRATE_ALLDAY_ON;
        }else{
            return HPlusConstants.ARG_HEARTRATE_ALLDAY_OFF;
        }
    }

    public static byte getSocial(String address) {
        //TODO: Figure what this is. Returning the default value

        return (byte) 255;
    }

    public static byte getUserWrist(String address) {
        String value = prefs.getString(HPlusConstants.PREF_HPLUS_WRIST, getContext().getString(R.string.left));

        if(value.equals(getContext().getString(R.string.left))){
            return HPlusConstants.ARG_WRIST_LEFT;
        }else{
            return HPlusConstants.ARG_WRIST_RIGHT;
        }
    }

    public static int getSITStartTime(String address) {
        return prefs.getInt(HPlusConstants.PREF_HPLUS_SIT_START_TIME, 0);
    }

    public static int getSITEndTime(String address) {
        return prefs.getInt(HPlusConstants.PREF_HPLUS_SIT_END_TIME, 0);
    }

    public static void setUnicodeSupport(String address, boolean state){
        SharedPreferences.Editor editor = prefs.getPreferences().edit();
        editor.putBoolean(HPlusConstants.PREF_HPLUS_UNICODE + "_" + address, state);
        editor.commit();
    }

    public static boolean getUnicodeSupport(String address){
        return (prefs.getBoolean(HPlusConstants.PREF_HPLUS_UNICODE, false));
    }
}
