/*  Copyright (C) 2015-2024 akasaka / Genjitsu Labs, Alicia Hormann, Andreas
    Shimokawa, Arjan Schrijver, Carsten Pfeiffer, Daniel Dakhno, Daniele Gobbetti,
    Davis Mosenkovs, Dmitry Markin, José Rebelo, Matthieu Baerts, Nephiel,
    Petr Vaněk, Taavi Eomäe, Johannes Krude

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
package nodomain.freeyourgadget.gadgetbridge.devices;

import static nodomain.freeyourgadget.gadgetbridge.GBApplication.getPrefs;

import android.app.Activity;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanFilter;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettings;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.capabilities.HeartRateCapability;
import nodomain.freeyourgadget.gadgetbridge.capabilities.password.PasswordCapabilityImpl;
import nodomain.freeyourgadget.gadgetbridge.capabilities.widgets.WidgetManager;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.entities.AlarmDao;
import nodomain.freeyourgadget.gadgetbridge.entities.BatteryLevelDao;
import nodomain.freeyourgadget.gadgetbridge.entities.CyclingSample;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.DeviceAttributesDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.AbstractNotificationPattern;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryParser;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryConfig;
import nodomain.freeyourgadget.gadgetbridge.model.BodyEnergySample;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.model.HeartRateSample;
import nodomain.freeyourgadget.gadgetbridge.model.HrvSummarySample;
import nodomain.freeyourgadget.gadgetbridge.model.HrvValueSample;
import nodomain.freeyourgadget.gadgetbridge.model.PaiSample;
import nodomain.freeyourgadget.gadgetbridge.model.RespiratoryRateSample;
import nodomain.freeyourgadget.gadgetbridge.model.RestingMetabolicRateSample;
import nodomain.freeyourgadget.gadgetbridge.model.SleepScoreSample;
import nodomain.freeyourgadget.gadgetbridge.model.Spo2Sample;
import nodomain.freeyourgadget.gadgetbridge.model.StressSample;
import nodomain.freeyourgadget.gadgetbridge.model.TemperatureSample;
import nodomain.freeyourgadget.gadgetbridge.model.Vo2MaxSample;
import nodomain.freeyourgadget.gadgetbridge.model.WeightSample;
import nodomain.freeyourgadget.gadgetbridge.service.ServiceDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public abstract class AbstractDeviceCoordinator implements DeviceCoordinator {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractDeviceCoordinator.class);

    private Pattern supportedDeviceName = null;

    /**
     * This method should return a Regexp pattern that will matched against a found device
     * to check whether this coordinator supports that device.
     * If more sophisticated logic is needed to determine device support, the supports(GBDeviceCandidate)
     * should be overridden.
     *
     * @return Pattern
     */
    protected Pattern getSupportedDeviceName() {
        return null;
    }

    @Override
    public boolean supports(GBDeviceCandidate candidate) {
        if (supportedDeviceName == null) {
            supportedDeviceName = getSupportedDeviceName();
        }
        if (supportedDeviceName == null) {
            LOG.error("{} should either override getSupportedDeviceName or supports(GBDeviceCandidate)", getClass());
            return false;
        }

        return supportedDeviceName.matcher(candidate.getName()).matches();
    }

    @Override
    public ConnectionType getConnectionType() {
        return ConnectionType.BOTH;
    }

    @Override
    public boolean isConnectable(){
        return true;
    }

    @NonNull
    @Override
    public Collection<? extends ScanFilter> createBLEScanFilters() {
        return Collections.emptyList();
    }

    @Override
    public GBDevice createDevice(GBDeviceCandidate candidate, DeviceType deviceType) {
        GBDevice gbDevice = new GBDevice(candidate.getDevice().getAddress(), candidate.getName(), null, null, deviceType);
        for (BatteryConfig batteryConfig : getBatteryConfig(gbDevice)) {
            gbDevice.setBatteryIcon(batteryConfig.getBatteryIcon(), batteryConfig.getBatteryIndex());
            gbDevice.setBatteryLabel(batteryConfig.getBatteryLabel(), batteryConfig.getBatteryIndex());
        }
        return gbDevice;
    }

    @Override
    public final void deleteDevice(final GBDevice gbDevice) throws GBException {
        LOG.info("will try to delete device: {}", gbDevice.getName());
        if (gbDevice.isConnected() || gbDevice.isConnecting()) {
            GBApplication.deviceService(gbDevice).disconnect();
        }
        Prefs prefs = getPrefs();

        Set<String> lastDeviceAddresses = prefs.getStringSet(GBPrefs.LAST_DEVICE_ADDRESSES, Collections.emptySet());
        if (lastDeviceAddresses.contains(gbDevice.getAddress())) {
            LOG.debug("#1605 removing last device (one of last devices)");
            lastDeviceAddresses = new HashSet<String>(lastDeviceAddresses);
            lastDeviceAddresses.remove(gbDevice.getAddress());
            prefs.getPreferences().edit().putStringSet(GBPrefs.LAST_DEVICE_ADDRESSES, lastDeviceAddresses).apply();
        }

        GBApplication.deleteDeviceSpecificSharedPrefs(gbDevice.getAddress());

        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            DaoSession session = dbHandler.getDaoSession();
            Device device = DBHelper.findDevice(gbDevice, session);
            if (device != null) {
                deleteDevice(gbDevice, device, session);
                QueryBuilder<?> qb = session.getDeviceAttributesDao().queryBuilder();
                qb.where(DeviceAttributesDao.Properties.DeviceId.eq(device.getId())).buildDelete().executeDeleteWithoutDetachingEntities();
                QueryBuilder<?> batteryLevelQueryBuilder = session.getBatteryLevelDao().queryBuilder();
                batteryLevelQueryBuilder.where(BatteryLevelDao.Properties.DeviceId.eq(device.getId())).buildDelete().executeDeleteWithoutDetachingEntities();
                QueryBuilder<?> alarmDeviceQueryBuilder = session.getAlarmDao().queryBuilder();
                alarmDeviceQueryBuilder.where(AlarmDao.Properties.DeviceId.eq(device.getId())).buildDelete().executeDeleteWithoutDetachingEntities();
                session.getDeviceDao().delete(device);
            } else {
                LOG.info("device to delete not found in db: {}", gbDevice);
            }
        } catch (Exception e) {
            throw new GBException("Error deleting device: " + e.getMessage(), e);
        }
    }

    /**
     * Hook for subclasses to perform device-specific deletion logic, e.g. db cleanup.
     *
     * @param gbDevice the GBDevice
     * @param device   the corresponding database Device
     * @param session  the session to use
     * @throws GBException if there was an error deleting device-specific resources
     */
    protected abstract void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws GBException;

    @Override
    public boolean allowFetchActivityData(GBDevice device) {
        return device.isInitialized() && !device.isBusy() && supportsActivityDataFetching();
    }

    @Override
    public SampleProvider<? extends ActivitySample> getSampleProvider(final GBDevice device, final DaoSession session) {
        return null;
    }

    @Override
    public TimeSampleProvider<? extends StressSample> getStressSampleProvider(GBDevice device, DaoSession session) {
        return null;
    }

    @Override
    public TimeSampleProvider<? extends BodyEnergySample> getBodyEnergySampleProvider(final GBDevice device, final DaoSession session) {
        return null;
    }

    @Override
    public TimeSampleProvider<? extends HrvSummarySample> getHrvSummarySampleProvider(GBDevice device, DaoSession session) {
        return null;
    }

    @Override
    public TimeSampleProvider<? extends HrvValueSample> getHrvValueSampleProvider(GBDevice device, DaoSession session) {
        return null;
    }

    @Override
    public Vo2MaxSampleProvider<? extends Vo2MaxSample> getVo2MaxSampleProvider(GBDevice device, DaoSession session) {
        return null;
    }

    @Override
    public int[] getStressRanges() {
        // 0-39 = relaxed
        // 40-59 = mild
        // 60-79 = moderate
        // 80-100 = high
        return new int[]{0, 40, 60, 80};
    }

    @Override
    public TimeSampleProvider<? extends TemperatureSample> getTemperatureSampleProvider(GBDevice device, DaoSession session) {
        return null;
    }

    @Override
    public TimeSampleProvider<? extends Spo2Sample> getSpo2SampleProvider(GBDevice device, DaoSession session) {
        return null;
    }

    @Override
    public TimeSampleProvider<CyclingSample> getCyclingSampleProvider(GBDevice device, DaoSession session) {
        return null;
    }

    @Override
    public TimeSampleProvider<? extends HeartRateSample> getHeartRateMaxSampleProvider(GBDevice device, DaoSession session) {
        return null;
    }

    @Override
    public TimeSampleProvider<? extends HeartRateSample> getHeartRateRestingSampleProvider(GBDevice device, DaoSession session) {
        return null;
    }

    @Override
    public TimeSampleProvider<? extends HeartRateSample> getHeartRateManualSampleProvider(GBDevice device, DaoSession session) {
        return null;
    }

    @Override
    public TimeSampleProvider<? extends PaiSample> getPaiSampleProvider(GBDevice device, DaoSession session) {
        return null;
    }

    @Override
    public TimeSampleProvider<? extends RespiratoryRateSample> getRespiratoryRateSampleProvider(GBDevice device, DaoSession session) {
        return null;
    }

    @Override
    public TimeSampleProvider<? extends WeightSample> getWeightSampleProvider(GBDevice device, DaoSession session) {
        return null;
    }

    @Override
    public TimeSampleProvider<? extends RestingMetabolicRateSample> getRestingMetabolicRateProvider(final GBDevice device, final DaoSession session) {
        return new DefaultRestingMetabolicRateProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends SleepScoreSample> getSleepScoreProvider(final GBDevice device, final DaoSession session) {
        return null;
    }

    @Override
    @Nullable
    public ActivitySummaryParser getActivitySummaryParser(final GBDevice device, final Context context) {
        return null;
    }

    public boolean isHealthWearable(BluetoothDevice device) {
        BluetoothClass bluetoothClass;
        try {
            bluetoothClass = device.getBluetoothClass();
        } catch (SecurityException se) {
            LOG.warn("missing bluetooth permission: ", se);
            return false;
        }
        if (bluetoothClass == null) {
            LOG.warn("unable to determine bluetooth device class of {}", device);
            return false;
        }
        if (bluetoothClass.getMajorDeviceClass() == BluetoothClass.Device.Major.WEARABLE
                || bluetoothClass.getMajorDeviceClass() == BluetoothClass.Device.Major.UNCATEGORIZED) {
            int deviceClasses =
                    BluetoothClass.Device.HEALTH_BLOOD_PRESSURE
                            | BluetoothClass.Device.HEALTH_DATA_DISPLAY
                            | BluetoothClass.Device.HEALTH_PULSE_RATE
                            | BluetoothClass.Device.HEALTH_WEIGHING
                            | BluetoothClass.Device.HEALTH_UNCATEGORIZED
                            | BluetoothClass.Device.HEALTH_PULSE_OXIMETER
                            | BluetoothClass.Device.HEALTH_GLUCOSE;

            return (bluetoothClass.getDeviceClass() & deviceClasses) != 0;
        }
        return false;
    }

    @Override
    public File getAppCacheDir() throws IOException {
        return null;
    }

    @Override
    public File getWritableExportDirectory(final GBDevice device) throws IOException {
        File dir;
        dir = new File(FileUtils.getExternalFilesDir() + File.separator + device.getAddress());
        if (!dir.isDirectory()) {
            if (!dir.mkdir()) {
                throw new IOException("Cannot create device specific directory for " + device.getName());
            }
        }
        return dir;
    }

    @Override
    public String getAppCacheSortFilename() {
        return null;
    }

    @Override
    public String getAppFileExtension() {
        return null;
    }

    @Override
    public boolean supportsAppListFetching() {
        return false;
    }

    @Override
    public boolean supportsFlashing() {
        return false;
    }

    @Nullable
    @Override
    public InstallHandler findInstallHandler(final Uri uri, final Context context) {
        return null;
    }

    @Override
    public boolean supportsScreenshots(final GBDevice device) {
        return false;
    }

    @Override
    public int getAlarmSlotCount(final GBDevice device) {
        return 0;
    }

    @Override
    public boolean supportsSmartWakeup(GBDevice device, int alarmPosition) {
        return false;
    }

    @Override
    public boolean supportsSmartWakeupInterval(GBDevice device, int alarmPosition) {
        return false;
    }

    @Override
    public boolean forcedSmartWakeup(GBDevice device, int alarmPosition) {
        return false;
    }

    @Override
    public boolean supportsAppReordering() {
        return false;
    }

    @Override
    public boolean supportsAppsManagement(final GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsCachedAppManagement(final GBDevice device) {
        try {
            return supportsAppsManagement(device) && getAppCacheDir() != null;
        } catch (final Exception e) {
            // we failed, but still tried, so it's supported..
            LOG.error("Failed to get app cache dir", e);
            return true;
        }
    }

    @Override
    public boolean supportsInstalledAppManagement(final GBDevice device) {
        return supportsAppsManagement(device);
    }

    @Override
    public boolean supportsWatchfaceManagement(final GBDevice device) {
        return supportsAppsManagement(device);
    }

    @Nullable
    @Override
    public Class<? extends Activity> getAppsManagementActivity() {
        return null;
    }

    @Nullable
    @Override
    public Class<? extends Activity> getWatchfaceDesignerActivity() {
        return null;
    }

    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_ASK;
    }

    @Override
    public boolean suggestUnbindBeforePair() {
        return true;
    }

    @Override
    public boolean isExperimental() {
        return false;
    }

    @Override
    public boolean supportsCalendarEvents() {
        return false;
    }

    @Override
    public boolean supportsActivityDataFetching() {
        return false;
    }

    @Override
    public boolean supportsActivityTracking() {
        return false;
    }

    @Override
    public boolean supportsActivityTracks() {
        return false;
    }

    @Override
    public boolean supportsStressMeasurement() {
        return false;
    }

    @Override
    public boolean supportsBodyEnergy() {
        return false;
    }

    @Override
    public boolean supportsHrvMeasurement() {
        return false;
    }

    @Override
    public boolean supportsVO2Max() {
        return false;
    }

    @Override
    public boolean supportsVO2MaxCycling() {
        return false;
    }

    @Override
    public boolean supportsVO2MaxRunning() {
        return false;
    }

    @Override
    public boolean supportsActiveCalories() {
        return false;
    }

    @Override
    public boolean supportsActivityTabs() {
        return supportsActivityTracking();
    }
    @Override
    public boolean supportsSleepMeasurement() {
        return supportsActivityTracking();
    }
    @Override
    public boolean supportsStepCounter() {
        return supportsActivityTracking();
    }
    @Override
    public boolean supportsSpeedzones() {
        return supportsActivityTracking();
    }

    @Override
    public boolean supportsTemperatureMeasurement() {
        return false;
    }

    @Override
    public boolean supportsContinuousTemperature() {
        return false;
    }

    @Override
    public boolean supportsSpo2(final GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsHeartRateStats() {
        return false;
    }

    @Override
    public boolean supportsPai() {
        return false;
    }

    @Override
    public int getPaiName() {
        return R.string.menuitem_pai;
    }

    @Override
    public boolean supportsPaiTime() {
        return supportsPai();
    }

    @Override
    public boolean supportsPaiLow() {
        return supportsPai();
    }

    @Override
    public int getPaiTarget() {
        return 100;
    }

    @Override
    public boolean supportsRespiratoryRate() {
        return false;
    }

    @Override
    public boolean supportsDayRespiratoryRate() {
        return false;
    }

    @Override
    public boolean supportsSleepRespiratoryRate() {
        return supportsRespiratoryRate();
    }

    @Override
    public boolean supportsWeightMeasurement() {
        return false;
    }

    @Override
    public boolean supportsAlarmSnoozing() {
        return false;
    }

    @Override
    public boolean supportsAlarmTitle(GBDevice device) {
        return false;
    }

    @Override
    public int getAlarmTitleLimit(GBDevice device) {
        return -1;
    }

    @Override
    public boolean supportsAlarmDescription(GBDevice device) {
        return false;
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
    public int getMaximumReminderMessageLength() {
        return 0;
    }

    @Override
    public int getReminderSlotCount(final GBDevice device) {
        return 0;
    }

    @Override
    public boolean getRemindersHaveTime() {
        return true;
    }

    @Override
    public boolean getReserveReminderSlotsForCalendar() {
        return false;
    }

    @Override
    public int getCannedRepliesSlotCount(final GBDevice device) {
        return 0;
    }

    @Override
    public int getWorldClocksSlotCount() {
        return 0;
    }

    @Override
    public int getWorldClocksLabelLength() {
        return 10;
    }

    @Override
    public boolean supportsDisabledWorldClocks() {
        return false;
    }

    @Override
    public int getContactsSlotCount(final GBDevice device) {
        return 0;
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
    public boolean supportsHeartRateMeasurement(final GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsHeartRateRestingMeasurement(final GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsManualHeartRateMeasurement(final GBDevice device) {
        return supportsHeartRateMeasurement(device);
    }

    @Override
    public boolean supportsRealtimeData() {
        return false;
    }

    @Override
    public boolean supportsCyclingData() {
        return false;
    }

    @Override
    public boolean supportsRemSleep() {
        return false;
    }

    @Override
    public boolean supportsAwakeSleep() {
        return false;
    }

    @Override
    public boolean supportsSleepScore() {
        return false;
    }

    @Override
    public boolean supportsWeather() {
        return false;
    }

    @Override
    public boolean supportsFindDevice() {
        return false;
    }

    @Override
    public boolean supportsUnicodeEmojis() {
        return false;
    }

    @Override
    public boolean supportsSleepAsAndroid() {
        return false;
    }

    @Override
    public Set<SleepAsAndroidFeature> getSleepAsAndroidFeatures() {
        return Collections.emptySet();
    }

    @Override
    public int[] getSupportedDeviceSpecificConnectionSettings() {
        int[] settings = new int[0];
        ConnectionType connectionType = getConnectionType();

        if (connectionType.usesBluetoothLE()) {
            settings = ArrayUtils.insert(0, settings, R.xml.devicesettings_reconnect_ble);
        }
        if (connectionType.usesBluetoothClassic()) {
            settings = ArrayUtils.insert(0, settings, R.xml.devicesettings_reconnect_bl_classic);
        }

        return settings;
    }

    @Override
    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        return new int[0];
    }

    @Override
    public DeviceSpecificSettings getDeviceSpecificSettings(GBDevice device) {
        final int[] settings = getSupportedDeviceSpecificSettings(device);
        if (settings == null || settings.length == 0) {
            return null;
        }

        return new DeviceSpecificSettings(settings);
    }

    @Override
    public int[] getSupportedDeviceSpecificAuthenticationSettings() {
        return new int[0];
    }

    @Override
    public DeviceSpecificSettingsCustomizer getDeviceSpecificSettingsCustomizer(GBDevice device) {
        return null;
    }

    @Override
    public String[] getSupportedLanguageSettings(GBDevice device) {
        return null;
    }

    @Nullable
    @Override
    public Class<? extends Activity> getPairingActivity() {
        return null;
    }

    @Nullable
    @Override
    public Class<? extends Activity> getCalibrationActivity() {
        return null;
    }

    @Override
    public int getBatteryCount() {
        return 1;
    } //multiple battery support, default is 1, maximum is 3, 0 will disable the battery in UI

    @Override
    public BatteryConfig[] getBatteryConfig(final GBDevice device) {
        final BatteryConfig[] batteryConfigs = new BatteryConfig[getBatteryCount()];
        for (int i = 0; i < getBatteryCount(); i++) {
            batteryConfigs[i] = new BatteryConfig(i);
        }
        return batteryConfigs;
    }

    @Override
    public boolean addBatteryPollingSettings() {
        return false;
    }

    @Override
    public boolean supportsPowerOff() {
        return false;
    }

    @Override
    public PasswordCapabilityImpl.Mode getPasswordCapability() {
        return PasswordCapabilityImpl.Mode.NONE;
    }

    @Override
    public List<HeartRateCapability.MeasurementInterval> getHeartRateMeasurementIntervals() {
        return Arrays.asList(
                HeartRateCapability.MeasurementInterval.OFF,
                HeartRateCapability.MeasurementInterval.MINUTES_1,
                HeartRateCapability.MeasurementInterval.MINUTES_5,
                HeartRateCapability.MeasurementInterval.MINUTES_10,
                HeartRateCapability.MeasurementInterval.MINUTES_30,
                HeartRateCapability.MeasurementInterval.HOUR_1
        );
    }

    @Override
    public boolean supportsWidgets(final GBDevice device) {
        return false;
    }

    @Nullable
    @Override
    public WidgetManager getWidgetManager(final GBDevice device) {
        return null;
    }

    public boolean supportsNavigation() {
        return false;
    }

    @Override
    public int getOrderPriority() {
        return 0;
    }

    @Override
    public EnumSet<ServiceDeviceSupport.Flags> getInitialFlags() {
        return EnumSet.of(ServiceDeviceSupport.Flags.BUSY_CHECKING);
    }

    @Override
    @DrawableRes
    public int getDefaultIconResource() {
        return R.drawable.ic_device_default;
    }

    @Override
    @DrawableRes
    public int getDisabledIconResource() {
        return R.drawable.ic_device_default_disabled;
    }

    @Override
    public boolean supportsNotificationVibrationPatterns() {
        return false;
    }

    @Override
    public boolean supportsNotificationVibrationRepetitionPatterns() {
        return false;
    }

    @Override
    public boolean supportsNotificationLedPatterns() {
        return false;
    }

    @Override
    public AbstractNotificationPattern[] getNotificationVibrationPatterns() {
        return new AbstractNotificationPattern[0];
    }

    @Override
    public AbstractNotificationPattern[] getNotificationVibrationRepetitionPatterns() {
        return new AbstractNotificationPattern[0];
    }

    @Override
    public AbstractNotificationPattern[] getNotificationLedPatterns() {
        return new AbstractNotificationPattern[0];
    }

    @Override
    public boolean validateAuthKey(final String authKey) {
        return !(authKey.getBytes().length < 34 || !authKey.startsWith("0x"));
    }
}
