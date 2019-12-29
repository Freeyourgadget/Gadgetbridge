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
package nodomain.freeyourgadget.gadgetbridge.devices.dafit;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.le.ScanFilter;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelUuid;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;

import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.dafit.settings.DaFitEnumDeviceVersion;
import nodomain.freeyourgadget.gadgetbridge.devices.dafit.settings.DaFitEnumMetricSystem;
import nodomain.freeyourgadget.gadgetbridge.devices.dafit.settings.DaFitEnumTimeSystem;
import nodomain.freeyourgadget.gadgetbridge.devices.dafit.settings.DaFitSetting;
import nodomain.freeyourgadget.gadgetbridge.devices.dafit.settings.DaFitSettingBool;
import nodomain.freeyourgadget.gadgetbridge.devices.dafit.settings.DaFitSettingByte;
import nodomain.freeyourgadget.gadgetbridge.devices.dafit.settings.DaFitSettingEnum;
import nodomain.freeyourgadget.gadgetbridge.devices.dafit.settings.DaFitSettingInt;
import nodomain.freeyourgadget.gadgetbridge.devices.dafit.settings.DaFitSettingLanguage;
import nodomain.freeyourgadget.gadgetbridge.devices.dafit.settings.DaFitSettingRemindersToMove;
import nodomain.freeyourgadget.gadgetbridge.devices.dafit.settings.DaFitSettingTimeRange;
import nodomain.freeyourgadget.gadgetbridge.devices.dafit.settings.DaFitSettingUserInfo;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;

public class DaFitDeviceCoordinator extends AbstractDeviceCoordinator {

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.DAFIT;
    }

    @Override
    public String getManufacturer() {
        return "Media-Tech";
    }

    @NonNull
    @Override
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public Collection<? extends ScanFilter> createBLEScanFilters() {
        ParcelUuid service = new ParcelUuid(DaFitConstants.UUID_SERVICE_DAFIT);
        ScanFilter filter = new ScanFilter.Builder().setServiceUuid(service).build();
        return Collections.singletonList(filter);
    }

    @NonNull
    @Override
    public DeviceType getSupportedType(GBDeviceCandidate candidate) {
        // TODO: It would be nice to also filter on "manufacturer" (which is used as a protocol version) being MOYOUNG-V2 or MOYOUNG but I have no idea if it's possible to do that at this point
        if (candidate.supportsService(DaFitConstants.UUID_SERVICE_DAFIT)) {
            return DeviceType.DAFIT;
        }
        return DeviceType.UNKNOWN;
    }

    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_NONE;
    }

    @Nullable
    @Override
    public Class<? extends Activity> getPairingActivity() {
        return null;
    }

    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws GBException {

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
        return new DaFitSampleProvider(device, session);
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
        return false;
    }

    @Override
    public boolean supportsLedColor() {
        return false;
    }

    @Override
    public boolean supportsRgbLedColor() {
        return false;
    }

    @NonNull
    @Override
    public int[] getColorPresets() {
        return new int[0];
    }

    @Override
    public boolean supportsUnicodeEmojis() { return false; }

    private static final DaFitSetting[] DAFIT_SETTINGS = new DaFitSetting[] {
        new DaFitSettingUserInfo("USER_INFO", DaFitConstants.CMD_SET_USER_INFO),
        new DaFitSettingByte("STEP_LENGTH", (byte)-1, DaFitConstants.CMD_SET_STEP_LENGTH),
        // (*) new DaFitSettingEnum<>("DOMINANT_HAND", DaFitConstants.CMD_QUERY_DOMINANT_HAND, DaFitConstants.CMD_SET_DOMINANT_HAND, DaFitEnumDominantHand.class),
        new DaFitSettingInt("GOAL_STEP", DaFitConstants.CMD_QUERY_GOAL_STEP, DaFitConstants.CMD_SET_GOAL_STEP),

        new DaFitSettingEnum<>("DEVICE_VERSION", DaFitConstants.CMD_QUERY_DEVICE_VERSION, DaFitConstants.CMD_SET_DEVICE_VERSION, DaFitEnumDeviceVersion.class),
        new DaFitSettingLanguage("DEVICE_LANGUAGE", DaFitConstants.CMD_QUERY_DEVICE_LANGUAGE, DaFitConstants.CMD_SET_DEVICE_LANGUAGE),
        new DaFitSettingEnum<>("TIME_SYSTEM", DaFitConstants.CMD_QUERY_TIME_SYSTEM, DaFitConstants.CMD_SET_TIME_SYSTEM, DaFitEnumTimeSystem.class),
        new DaFitSettingEnum<>("METRIC_SYSTEM", DaFitConstants.CMD_QUERY_METRIC_SYSTEM, DaFitConstants.CMD_SET_METRIC_SYSTEM, DaFitEnumMetricSystem.class),

        // (*) new DaFitSetting("DISPLAY_DEVICE_FUNCTION", DaFitConstants.CMD_QUERY_DISPLAY_DEVICE_FUNCTION, DaFitConstants.CMD_SET_DISPLAY_DEVICE_FUNCTION),
        // (*) new DaFitSetting("SUPPORT_WATCH_FACE", DaFitConstants.CMD_QUERY_SUPPORT_WATCH_FACE, (byte)-1),
        // (*) new DaFitSetting("WATCH_FACE_LAYOUT", DaFitConstants.CMD_QUERY_WATCH_FACE_LAYOUT, DaFitConstants.CMD_SET_WATCH_FACE_LAYOUT),
        new DaFitSettingByte("DISPLAY_WATCH_FACE", DaFitConstants.CMD_QUERY_DISPLAY_WATCH_FACE, DaFitConstants.CMD_SET_DISPLAY_WATCH_FACE),
        new DaFitSettingBool("OTHER_MESSAGE_STATE", DaFitConstants.CMD_QUERY_OTHER_MESSAGE_STATE, DaFitConstants.CMD_SET_OTHER_MESSAGE_STATE),

        new DaFitSettingBool("QUICK_VIEW", DaFitConstants.CMD_QUERY_QUICK_VIEW, DaFitConstants.CMD_SET_QUICK_VIEW),
        new DaFitSettingTimeRange("QUICK_VIEW_TIME", DaFitConstants.CMD_QUERY_QUICK_VIEW_TIME, DaFitConstants.CMD_SET_QUICK_VIEW_TIME),
        new DaFitSettingBool("SEDENTARY_REMINDER", DaFitConstants.CMD_QUERY_SEDENTARY_REMINDER, DaFitConstants.CMD_SET_SEDENTARY_REMINDER),
        new DaFitSettingRemindersToMove("REMINDERS_TO_MOVE_PERIOD", DaFitConstants.CMD_QUERY_REMINDERS_TO_MOVE_PERIOD, DaFitConstants.CMD_SET_REMINDERS_TO_MOVE_PERIOD),
        new DaFitSettingTimeRange("DO_NOT_DISTURB_TIME", DaFitConstants.CMD_QUERY_DO_NOT_DISTURB_TIME, DaFitConstants.CMD_SET_DO_NOT_DISTURB_TIME),
        // (*) new DaFitSetting("PSYCHOLOGICAL_PERIOD", DaFitConstants.CMD_QUERY_PSYCHOLOGICAL_PERIOD, DaFitConstants.CMD_SET_PSYCHOLOGICAL_PERIOD),

        new DaFitSettingBool("BREATHING_LIGHT", DaFitConstants.CMD_QUERY_BREATHING_LIGHT, DaFitConstants.CMD_SET_BREATHING_LIGHT)
    };

    @Override
    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        return new int[]{
            R.xml.devicesettings_personalinfo,
            //R.xml.devicesettings_steplength, // TODO is this needed? does it work? write-only so hard to tell
            R.xml.devicesettings_dafit_device_version,
            R.xml.devicesettings_dafit_language,
            R.xml.devicesettings_timeformat,
            R.xml.devicesettings_measurementsystem,
            R.xml.devicesettings_dafit_watchface,
            //R.xml.devicesettings_dafit_othermessage, // not implemented because this doesn't really do anything on the watch side, only enables/disables sending of "other" notifications in the app (no idea why they store the setting on the watch)
            R.xml.devicesettings_liftwrist_display,
            R.xml.devicesettings_dafit_sedentary_reminder,
            R.xml.devicesettings_donotdisturb_no_auto_v2,
            //R.xml.devicesettings_dafit_breathinglight, // No idea what this does but it doesn't seem to change anything
        };
    }

    public DaFitSetting[] getSupportedSettings() {
        return DAFIT_SETTINGS;
    }
}
