/*  Copyright (C) 2023-2024 Daniel Dakhno, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.test;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.ArrayUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.appmanager.AppManagerActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.capabilities.HeartRateCapability;
import nodomain.freeyourgadget.gadgetbridge.capabilities.password.PasswordCapabilityImpl;
import nodomain.freeyourgadget.gadgetbridge.capabilities.widgets.WidgetManager;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.roidmi.RoidmiConst;
import nodomain.freeyourgadget.gadgetbridge.devices.test.activity.TestActivitySummaryParser;
import nodomain.freeyourgadget.gadgetbridge.devices.test.samples.TestPaiSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.test.samples.TestSpo2SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.test.samples.TestStressSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.test.samples.TestSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.test.samples.TestTemperatureSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.AbstractNotificationPattern;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryParser;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryConfig;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.model.HeartRateSample;
import nodomain.freeyourgadget.gadgetbridge.model.PaiSample;
import nodomain.freeyourgadget.gadgetbridge.model.SleepRespiratoryRateSample;
import nodomain.freeyourgadget.gadgetbridge.model.Spo2Sample;
import nodomain.freeyourgadget.gadgetbridge.model.StressSample;
import nodomain.freeyourgadget.gadgetbridge.model.TemperatureSample;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.ServiceDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.test.TestDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class TestDeviceCoordinator extends AbstractDeviceCoordinator {
    @Override
    protected void deleteDevice(@NonNull final GBDevice gbDevice,
                                @NonNull final Device device,
                                @NonNull final DaoSession session) throws GBException {
        // Nothing to do
    }

    @Override
    public boolean supports(final GBDeviceCandidate candidate) {
        return false;
    }

    @Override
    public String getManufacturer() {
        return "Gadgetbridge";
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass() {
        return TestDeviceSupport.class;
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_test;
    }

    @Override
    public SampleProvider<? extends ActivitySample> getSampleProvider(final GBDevice device, final DaoSession session) {
        return supportsActivityTracking() ? new TestSampleProvider(device, session) : super.getSampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends StressSample> getStressSampleProvider(final GBDevice device, final DaoSession session) {
        return supportsStressMeasurement() ? new TestStressSampleProvider() : super.getStressSampleProvider(device, session);
    }

    @Override
    public int[] getStressRanges() {
        // TODO getStressRanges
        return super.getStressRanges();
    }

    @Override
    public TimeSampleProvider<? extends TemperatureSample> getTemperatureSampleProvider(final GBDevice device, final DaoSession session) {
        return new TestTemperatureSampleProvider(); // TODO supportsTemperature
    }

    @Override
    public TimeSampleProvider<? extends Spo2Sample> getSpo2SampleProvider(final GBDevice device, final DaoSession session) {
        return supportsSpo2(device) ? new TestSpo2SampleProvider() : super.getSpo2SampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends HeartRateSample> getHeartRateMaxSampleProvider(final GBDevice device, final DaoSession session) {
        // TODO getHeartRateMaxSampleProvider
        return super.getHeartRateMaxSampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends HeartRateSample> getHeartRateRestingSampleProvider(final GBDevice device, final DaoSession session) {
        // TODO getHeartRateRestingSampleProvider
        return super.getHeartRateRestingSampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends HeartRateSample> getHeartRateManualSampleProvider(final GBDevice device, final DaoSession session) {
        // TODO getHeartRateManualSampleProvider
        return super.getHeartRateManualSampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends PaiSample> getPaiSampleProvider(final GBDevice device, final DaoSession session) {
        return supportsPai() ? new TestPaiSampleProvider() : super.getPaiSampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends SleepRespiratoryRateSample> getSleepRespiratoryRateSampleProvider(final GBDevice device, final DaoSession session) {
        // TODO getHeartRateManualSampleProvider
        return super.getSleepRespiratoryRateSampleProvider(device, session);
    }

    @Nullable
    @Override
    public ActivitySummaryParser getActivitySummaryParser(final GBDevice device) {
        return supportsActivityTracks() ? new TestActivitySummaryParser() : super.getActivitySummaryParser(device);
    }

    @Override
    public File getAppCacheDir() throws IOException {
        return new File(FileUtils.getExternalFilesDir(), "test-app-cache");
    }

    @Override
    public String getAppCacheSortFilename() {
        return "test-app-cache-order.txt";
    }

    @Override
    public String getAppFileExtension() {
        return ".txt";
    }

    @Override
    public boolean supportsAppListFetching() {
        return supports(getTestDevice(), TestFeature.APP_LIST_FETCHING);
    }

    @Override
    public boolean supportsFlashing() {
        return supports(getTestDevice(), TestFeature.FLASHING);
    }

    @Nullable
    @Override
    public InstallHandler findInstallHandler(final Uri uri, final Context context) {
        // TODO findInstallHandler?
        return super.findInstallHandler(uri, context);
    }

    @Override
    public boolean supportsScreenshots(final GBDevice device) {
        return supports(getTestDevice(), TestFeature.SCREENSHOTS);
    }

    @Override
    public int getAlarmSlotCount(final GBDevice device) {
        /*
          0: Forced smart, no interval
          1: Forced smart, with interval
          2: Unforced smart, no interval
          3: Unforced smart, with interval
          4: Not smart
         */
        return 5;
    }

    @Override
    public boolean supportsSmartWakeup(final GBDevice device, int position) {
        return supports(getTestDevice(), TestFeature.SMART_WAKEUP) && position <= 3;
    }

    @Override
    public boolean supportsSmartWakeupInterval(GBDevice device, int alarmPosition) {
        return supports(getTestDevice(), TestFeature.SMART_WAKEUP_INTERVAL) && (alarmPosition == 1 || alarmPosition == 3);
    }

    @Override
    public boolean forcedSmartWakeup(final GBDevice device, final int alarmPosition) {
        return supports(getTestDevice(), TestFeature.SMART_WAKEUP_FORCED_SLOT) && alarmPosition <= 1;
    }

    @Override
    public boolean supportsAppReordering() {
        return supports(getTestDevice(), TestFeature.APP_REORDERING);
    }

    @Override
    public boolean supportsAppsManagement(final GBDevice device) {
        return supports(getTestDevice(), TestFeature.APPS_MANAGEMENT);
    }

    @Override
    public boolean supportsCachedAppManagement(final GBDevice device) {
        return supports(getTestDevice(), TestFeature.CACHED_APP_MANAGEMENT);
    }

    @Override
    public boolean supportsInstalledAppManagement(final GBDevice device) {
        return supports(getTestDevice(), TestFeature.INSTALLED_APP_MANAGEMENT);
    }

    @Override
    public boolean supportsWatchfaceManagement(final GBDevice device) {
        return supports(getTestDevice(), TestFeature.WATCHFACE_MANAGEMENT);
    }

    @Nullable
    @Override
    public Class<? extends Activity> getAppsManagementActivity() {
        return AppManagerActivity.class;
    }

    @Nullable
    @Override
    public Class<? extends Activity> getWatchfaceDesignerActivity() {
        // TODO getWatchfaceDesignerActivity
        return super.getWatchfaceDesignerActivity();
    }

    @Override
    public boolean supportsCalendarEvents() {
        return supports(getTestDevice(), TestFeature.CALENDAR_EVENTS);
    }

    @Override
    public boolean supportsActivityDataFetching() {
        return supports(getTestDevice(), TestFeature.ACTIVITY_DATA_FETCHING);
    }

    @Override
    public boolean supportsActivityTracking() {
        return supports(getTestDevice(), TestFeature.ACTIVITY_TRACKING);
    }

    @Override
    public boolean supportsSleepMeasurement() {
        return supports(getTestDevice(), TestFeature.SLEEP_MEASUREMENT);
    }

    @Override
    public boolean supportsStepCounter() {
        return supports(getTestDevice(), TestFeature.STEP_COUNTER);
    }

    @Override
    public boolean supportsSpeedzones() {
        return supports(getTestDevice(), TestFeature.SPEEDZONES);
    }

    @Override
    public boolean supportsActivityTabs() {
        return supports(getTestDevice(), TestFeature.ACTIVITY_TABS);
    }

    @Override
    public boolean supportsTemperatureMeasurement() {
        return supports(getTestDevice(), TestFeature.TEMPERATURE_MEASUREMENT);
    }

    @Override
    public boolean supportsActivityTracks() {
        return supports(getTestDevice(), TestFeature.ACTIVITY_TRACKS);
    }

    @Override
    public boolean supportsStressMeasurement() {
        return supports(getTestDevice(), TestFeature.STRESS_MEASUREMENT);
    }

    @Override
    public boolean supportsSpo2(GBDevice device) {
        return supports(getTestDevice(), TestFeature.SPO2);
    }

    @Override
    public boolean supportsHeartRateStats() {
        return supports(getTestDevice(), TestFeature.HEART_RATE_STATS);
    }

    @Override
    public boolean supportsPai() {
        return supports(getTestDevice(), TestFeature.PAI);
    }

    @Override
    public int getPaiName() {
        return R.string.menuitem_pai;
    }

    @Override
    public boolean supportsPaiTime() {
        return supports(getTestDevice(), TestFeature.PAI_TIME);
    }

    @Override
    public boolean supportsSleepRespiratoryRate() {
        return supports(getTestDevice(), TestFeature.SLEEP_RESPIRATORY_RATE);
    }

    @Override
    public boolean supportsAlarmSnoozing() {
        return supports(getTestDevice(), TestFeature.ALARM_SNOOZING);
    }

    @Override
    public boolean supportsAlarmTitle(final GBDevice device) {
        return supports(getTestDevice(), TestFeature.ALARM_TITLE);
    }

    @Override
    public int getAlarmTitleLimit(final GBDevice device) {
        return supports(getTestDevice(), TestFeature.ALARM_TITLE_LIMIT) ? 10 : -1;
    }

    @Override
    public boolean supportsAlarmDescription(final GBDevice device) {
        return supports(getTestDevice(), TestFeature.ALARM_DESCRIPTION);
    }

    @Override
    public boolean supportsMusicInfo() {
        return supports(getTestDevice(), TestFeature.MUSIC_INFO);
    }

    @Override
    public boolean supportsLedColor() {
        return supports(getTestDevice(), TestFeature.LED_COLOR);
    }

    @Override
    public int getMaximumReminderMessageLength() {
        // TODO getMaximumReminderMessageLength
        return 16;
    }

    @Override
    public int getReminderSlotCount(final GBDevice device) {
        return supports(getTestDevice(), TestFeature.REMINDERS) ? 3 : 0;
    }

    @Override
    public int getCannedRepliesSlotCount(final GBDevice device) {
        return supports(getTestDevice(), TestFeature.CANNED_REPLIES) ? 3 : 0;
    }

    @Override
    public int getWorldClocksSlotCount() {
        return supports(getTestDevice(), TestFeature.WORLD_CLOCKS) ? 3 : 0;
    }

    @Override
    public int getWorldClocksLabelLength() {
        // TODO getWorldClocksLabelLength
        return 10;
    }

    @Override
    public boolean supportsDisabledWorldClocks() {
        return supports(getTestDevice(), TestFeature.DISABLED_WORLD_CLOCKS);
    }

    @Override
    public int getContactsSlotCount(final GBDevice device) {
        return supports(getTestDevice(), TestFeature.CONTACTS) ? 3 : 0;
    }

    @Override
    public boolean supportsRgbLedColor() {
        return supports(getTestDevice(), TestFeature.RGB_LED_COLOR);
    }

    @NonNull
    @Override
    public int[] getColorPresets() {
        return RoidmiConst.COLOR_PRESETS;
    }

    @Override
    public boolean supportsHeartRateMeasurement(final GBDevice device) {
        return supports(getTestDevice(), TestFeature.HEART_RATE_MEASUREMENT);
    }

    @Override
    public boolean supportsManualHeartRateMeasurement(final GBDevice device) {
        return supports(getTestDevice(), TestFeature.MANUAL_HEART_RATE_MEASUREMENT);
    }

    @Override
    public boolean supportsRealtimeData() {
        return supports(getTestDevice(), TestFeature.REALTIME_DATA);
    }

    @Override
    public boolean supportsRemSleep() {
        return supports(getTestDevice(), TestFeature.REM_SLEEP);
    }

    @Override
    public boolean supportsWeather() {
        return supports(getTestDevice(), TestFeature.WEATHER);
    }

    @Override
    public boolean supportsFindDevice() {
        return supports(getTestDevice(), TestFeature.FIND_DEVICE);
    }

    @Override
    public boolean supportsUnicodeEmojis() {
        return supports(getTestDevice(), TestFeature.UNICODE_EMOJIS);
    }

    @Override
    public int[] getSupportedDeviceSpecificConnectionSettings() {
        return super.getSupportedDeviceSpecificConnectionSettings();
    }

    @Override
    public int[] getSupportedDeviceSpecificSettings(final GBDevice device) {
        final List<Integer> settings = new ArrayList<>();

        settings.add(R.xml.devicesettings_header_apps);
        settings.add(R.xml.devicesettings_loyalty_cards);

        if (getWorldClocksSlotCount() > 0) {
            settings.add(R.xml.devicesettings_header_time);
            settings.add(R.xml.devicesettings_world_clocks);
        }

        if (getContactsSlotCount(device) > 0) {
            settings.add(R.xml.devicesettings_header_other);
            settings.add(R.xml.devicesettings_contacts);
        }

        settings.add(R.xml.devicesettings_header_developer);
        settings.add(R.xml.devicesettings_test_features);

        return ArrayUtils.toPrimitive(settings.toArray(new Integer[0]));
    }

    @Override
    public int[] getSupportedDeviceSpecificAuthenticationSettings() {
        return new int[]{R.xml.devicesettings_pairingkey};
    }

    @Override
    public DeviceSpecificSettingsCustomizer getDeviceSpecificSettingsCustomizer(final GBDevice device) {
        return new TestDeviceSpecificSettingsCustomizer();
    }

    @Override
    public String[] getSupportedLanguageSettings(final GBDevice device) {
        return new String[]{
                "auto",
                "en_US",
        };
    }

    @Nullable
    @Override
    public Class<? extends Activity> getCalibrationActivity() {
        // TODO getCalibrationActivity
        return super.getCalibrationActivity();
    }

    @Override
    public int getBatteryCount() {
        return supports(getTestDevice(), TestFeature.BATTERIES_MULTIPLE) ? 3 : 1;
    }

    @Override
    public BatteryConfig[] getBatteryConfig(final GBDevice device) {
        if (getBatteryCount() == 1) {
            return super.getBatteryConfig(device);
        }

        final BatteryConfig[] ret = new BatteryConfig[getBatteryCount()];

        for (int i = 0; i < getBatteryCount(); i++) {
            ret[i] = new BatteryConfig(i, R.drawable.ic_battery_full, R.string.battery);
        }

        return ret;
    }

    @Override
    public boolean supportsPowerOff() {
        return supports(getTestDevice(), TestFeature.POWER_OFF);
    }

    @Override
    public PasswordCapabilityImpl.Mode getPasswordCapability() {
        return super.getPasswordCapability();
    }

    @Override
    public List<HeartRateCapability.MeasurementInterval> getHeartRateMeasurementIntervals() {
        return super.getHeartRateMeasurementIntervals();
    }

    @Override
    public boolean supportsWidgets(final GBDevice device) {
        return supports(getTestDevice(), TestFeature.WIDGETS);
    }

    @Nullable
    @Override
    public WidgetManager getWidgetManager(final GBDevice device) {
        return super.getWidgetManager(device);
    }

    @Override
    public boolean supportsNavigation() {
        return supports(getTestDevice(), TestFeature.NAVIGATION);
    }

    @Override
    public EnumSet<ServiceDeviceSupport.Flags> getInitialFlags() {
        return EnumSet.noneOf(ServiceDeviceSupport.Flags.class);
    }

    @Override
    public int getDefaultIconResource() {
        // TODO getDefaultIconResource
        return super.getDefaultIconResource();
    }

    @Override
    public int getDisabledIconResource() {
        // TODO getDisabledIconResource
        return super.getDisabledIconResource();
    }

    @Override
    public boolean supportsNotificationVibrationPatterns() {
        return supports(getTestDevice(), TestFeature.NOTIFICATION_VIBRATION_PATTERNS);
    }

    @Override
    public boolean supportsNotificationVibrationRepetitionPatterns() {
        return supports(getTestDevice(), TestFeature.NOTIFICATION_VIBRATION_REPETITION_PATTERNS);
    }

    @Override
    public boolean supportsNotificationLedPatterns() {
        return supports(getTestDevice(), TestFeature.NOTIFICATION_LED_PATTERNS);
    }

    @Override
    public boolean addBatteryPollingSettings() {
        return supports(getTestDevice(), TestFeature.BATTERY_POLLING_SETTINGS);
    }

    @Override
    public AbstractNotificationPattern[] getNotificationVibrationPatterns() {
        // TODO getNotificationVibrationPatterns
        return new AbstractNotificationPattern[0];
    }

    @Override
    public AbstractNotificationPattern[] getNotificationVibrationRepetitionPatterns() {
        // TODO getNotificationVibrationRepetitionPatterns
        return new AbstractNotificationPattern[0];
    }

    @Override
    public AbstractNotificationPattern[] getNotificationLedPatterns() {
        // TODO getNotificationLedPatterns
        return new AbstractNotificationPattern[0];
    }

    @Override
    public boolean validateAuthKey(final String authKey) {
        return authKey.equals("hunter2");
    }

    public boolean supports(final GBDevice device, final TestFeature feature) {
        if (device == null) {
            return false;
        }

        final Set<String> features = new HashSet<>(getPrefs(device).getStringSet(TestDeviceConst.PREF_TEST_FEATURES, Collections.emptySet()));
        // if nothing enable, enable everything
        return features.isEmpty() || features.contains(feature.name());
    }

    @Nullable
    private GBDevice getTestDevice() {
        // HACK: Avoid the refactor of adding a device as a parameter on all functions
        // This means that it's only possible to have a single test device

        if (GBApplication.app() == null || GBApplication.app().getDeviceManager() == null) {
            return null;
        }

        final List<GBDevice> devices = GBApplication.app().getDeviceManager().getDevices();
        for (GBDevice device : devices) {
            if (DeviceType.TEST == device.getType()) {
                return device;
            }
        }

        return null;
    }

    protected static Prefs getPrefs(final GBDevice device) {
        return new Prefs(GBApplication.getDeviceSpecificSharedPrefs(device.getAddress()));
    }
}
