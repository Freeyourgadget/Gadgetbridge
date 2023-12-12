/*  Copyright (C) 2015-2021 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, Dmitry Markin, JohnnySun, José Rebelo, Matthieu Baerts, Nephiel,
    Uwe Hermann

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
package nodomain.freeyourgadget.gadgetbridge.devices;

import android.app.Activity;
import android.bluetooth.le.ScanFilter;
import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.capabilities.HeartRateCapability;
import nodomain.freeyourgadget.gadgetbridge.capabilities.password.PasswordCapabilityImpl;
import nodomain.freeyourgadget.gadgetbridge.capabilities.widgets.WidgetManager;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
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

/**
 * This interface is implemented at least once for every supported gadget device.
 * It allows Gadgetbridge to generically deal with different kinds of devices
 * without actually knowing the details of any device.
 * <p/>
 * Instances will be created as needed and asked whether they support a given
 * device. If a coordinator answers true, it will be used to assist in handling
 * the given device.
 */
public interface DeviceCoordinator {
    String EXTRA_DEVICE_CANDIDATE = "nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate.EXTRA_DEVICE_CANDIDATE";
    /**
     * Do not attempt to bond after discovery.
     */
    int BONDING_STYLE_NONE = 0;
    /**
     * Bond after discovery.
     * This is not recommended, as there are mobile devices on which bonding does not work.
     * Prefer to use #BONDING_STYLE_ASK instead.
     */
    int BONDING_STYLE_BOND = 1;
    /**
     * Let the user decide whether to bond or not after discovery.
     * Prefer this over #BONDING_STYLE_BOND
     */
    int BONDING_STYLE_ASK = 2;

    /**
     * A secret key has to be entered before connecting
     */
    int BONDING_STYLE_REQUIRE_KEY = 3;

    /**
     * Lazy pairing, i.e. device initiated pairing is requested
     */
    int BONDING_STYLE_LAZY = 4;

    enum ConnectionType{
        BLE(false, true),
        BL_CLASSIC(true, false),
        BOTH(true, true)
        ;
        boolean usesBluetoothClassic, usesBluetoothLE;

        ConnectionType(boolean usesBluetoothClassic, boolean usesBluetoothLE) {
            this.usesBluetoothClassic = usesBluetoothClassic;
            this.usesBluetoothLE = usesBluetoothLE;
        }

        public boolean usesBluetoothLE(){
            return usesBluetoothLE;
        }

        public boolean usesBluetoothClassic(){
            return usesBluetoothClassic;
        }
    }

    /**
     * Returns the type of connection, Classic of BLE
     *
     * @return ConnectionType
     */
    ConnectionType getConnectionType();

    /**
     * Checks whether this coordinator handles the given candidate.
     *
     * @param candidate
     * @return true if this coordinator handles the given candidate.
     */
    boolean supports(GBDeviceCandidate candidate);

    /**
     * Returns a list of scan filters that shall be used to discover devices supported
     * by this coordinator.
     * @return the list of scan filters, may be empty
     */
    @NonNull
    Collection<? extends ScanFilter> createBLEScanFilters();

    GBDevice createDevice(GBDeviceCandidate candidate, DeviceType type);

    /**
     * Deletes all information, including all related database content about the
     * given device.
     * @throws GBException
     */
    void deleteDevice(GBDevice device) throws GBException;

    /**
     * Returns the Activity class to be started in order to perform a pairing of a
     * given device after its discovery.
     *
     * @return the activity class for pairing/initial authentication, or null if none
     */
    @Nullable
    Class<? extends Activity> getPairingActivity();

    @Nullable
    Class<? extends Activity> getCalibrationActivity();

    /**
     * Returns true if activity data fetching is supported by the device
     * (with this coordinator).
     * This enables the sync button in control center and the device can thus be asked to send the data
     * (as opposed the device pushing the data to us by itself)
     *
     * @return
     */
    boolean supportsActivityDataFetching();

    /**
     * Returns true if activity tracking is supported by the device
     * (with this coordinator).
     * This enables the ActivityChartsActivity.
     *
     * @return
     */
    boolean supportsActivityTracking();

    /**
     * Indicates whether the device supports recording dedicated activity tracks, like
     * walking, hiking, running, swimming, etc. and retrieving the recorded
     * data. This is different from the constant activity tracking since the tracks are
     * usually recorded with additional features, like e.g. GPS.
     */
    boolean supportsActivityTracks();

    /**
     * Returns true if stress measurement and fetching is supported by the device
     * (with this coordinator).
     */
    boolean supportsStressMeasurement();

    /**
     * Returns true if SpO2 measurement and fetching is supported by the device
     * (with this coordinator).
     */
    boolean supportsSpo2();

    /**
     * Returns true if heart rate stats (max, resting, manual) measurement and fetching is supported
     * by the device (with this coordinator).
     */
    boolean supportsHeartRateStats();

    /**
     * Returns true if PAI (Personal Activity Intelligence) measurement and fetching is supported by
     * the device (with this coordinator).
     */
    boolean supportsPai();

    /**
     * Returns the device-specific name for PAI (eg. Vitality Score).
     */
    @StringRes
    int getPaiName();

    /**
     * Returns true if the device is capable of providing the time contribution for each PAI type
     * (light, moderate, high).
     */
    boolean supportsPaiTime();

    /**
     * Returns true if sleep respiratory rate measurement and fetching is supported by
     * the device (with this coordinator).
     */
    boolean supportsSleepRespiratoryRate();

    /**
     * Returns true if activity data fetching is supported AND possible at this
     * very moment. This will consider the device state (being connected/disconnected/busy...)
     * etc.
     *
     * @param device
     * @return
     */
    boolean allowFetchActivityData(GBDevice device);

    /**
     * Returns the sample provider for the device being supported.
     *
     * @return
     */
    SampleProvider<? extends ActivitySample> getSampleProvider(GBDevice device, DaoSession session);

    /**
     * Returns the sample provider for stress data, for the device being supported.
     */
    TimeSampleProvider<? extends StressSample> getStressSampleProvider(GBDevice device, DaoSession session);

    /**
     * Returns the stress ranges (relaxed, mild, moderate, high), so that stress can be categorized.
     */
    int[] getStressRanges();

    /**
     * Returns the sample provider for temperature data, for the device being supported.
     */
    TimeSampleProvider<? extends TemperatureSample> getTemperatureSampleProvider(GBDevice device, DaoSession session);

    /**
     * Returns the sample provider for SpO2 data, for the device being supported.
     */
    TimeSampleProvider<? extends Spo2Sample> getSpo2SampleProvider(GBDevice device, DaoSession session);

    /**
     * Returns the sample provider for max HR data, for the device being supported.
     */
    TimeSampleProvider<? extends HeartRateSample> getHeartRateMaxSampleProvider(GBDevice device, DaoSession session);

    /**
     * Returns the sample provider for resting HR data, for the device being supported.
     */
    TimeSampleProvider<? extends HeartRateSample> getHeartRateRestingSampleProvider(GBDevice device, DaoSession session);

    /**
     * Returns the sample provider for manual HR data, for the device being supported.
     */
    TimeSampleProvider<? extends HeartRateSample> getHeartRateManualSampleProvider(GBDevice device, DaoSession session);

    /**
     * Returns the sample provider for PAI data, for the device being supported.
     */
    TimeSampleProvider<? extends PaiSample> getPaiSampleProvider(GBDevice device, DaoSession session);

    /**
     * Returns the sample provider for sleep respiratory rate data, for the device being supported.
     */
    TimeSampleProvider<? extends SleepRespiratoryRateSample> getSleepRespiratoryRateSampleProvider(GBDevice device, DaoSession session);

    /**
     * Returns the {@link ActivitySummaryParser} for the device being supported.
     *
     * @return
     */
    ActivitySummaryParser getActivitySummaryParser(final GBDevice device);

    /**
     * Returns true if this device/coordinator supports installing files like firmware,
     * watchfaces, gps, resources, fonts...
     *
     * @return
     */
    boolean supportsFlashing();

    /**
     * Finds an install handler for the given uri that can install the given
     * uri on the device being managed.
     *
     * @param uri
     * @param context
     * @return the install handler or null if that uri cannot be installed on the device
     */
    InstallHandler findInstallHandler(Uri uri, Context context);

    /**
     * Returns true if this device/coordinator supports taking screenshots.
     *
     * @return
     */
    boolean supportsScreenshots();

    /**
     * Returns the number of alarms this device/coordinator supports
     * Shall return 0 also if it is not possible to set alarms via
     * protocol, but only on the smart device itself.
     *
     * @return
     */
    int getAlarmSlotCount(GBDevice device);

    /**
     * Returns true if this device/coordinator supports alarms with smart wakeup
     * @return
     */
    boolean supportsSmartWakeup(GBDevice device);

    /**
     * Returns true if this device/coordinator supports alarm snoozing
     * @return
     */
    boolean supportsAlarmSnoozing();

    /**
     * Returns true if this device/coordinator supports alarm descriptions
     * @return
     */
    boolean supportsAlarmDescription(GBDevice device);

    /**
     * Returns true if the given device supports heart rate measurements.
     * @return
     */
    boolean supportsHeartRateMeasurement(GBDevice device);

    /**
     * Returns true if the device supports triggering manual one-shot heart rate measurements.
     */
    boolean supportsManualHeartRateMeasurement(GBDevice device);

    /**
     * Returns the readable name of the manufacturer.
     */
    String getManufacturer();

    /**
     * Returns true if this device/coordinator supports managing device apps.
     *
     * @return
     */
    boolean supportsAppsManagement(GBDevice device);

    boolean supportsCachedAppManagement(GBDevice device);
    boolean supportsInstalledAppManagement(GBDevice device);
    boolean supportsWatchfaceManagement(GBDevice device);

    /**
     * Returns the Activity class that will be used to manage device apps.
     *
     * @return
     */
    Class<? extends Activity> getAppsManagementActivity();

    /**
     * Returns the Activity class that will be used to design watchfaces.
     *
     * @return
     */
    Class<? extends Activity> getWatchfaceDesignerActivity();

    /**
     * Returns the device app cache directory.
     */
    File getAppCacheDir() throws IOException;

    /**
     * Returns a String containing the device app sort order filename.
     */
    String getAppCacheSortFilename();

    /**
     * Returns a String containing the file extension for watch apps.
     */
    String getAppFileExtension();

    /**
     * Indicated whether the device supports fetching a list of its apps.
     */
    boolean supportsAppListFetching();

    /**
     * Indicates whether the device supports reordering of apps.
     */
    boolean supportsAppReordering();

    /**
     * Returns how/if the given device should be bonded before connecting to it.
     */
    int getBondingStyle();

    /**
     * Returns true if this device is in an experimental state / not tested.
     */
    boolean isExperimental();

    /**
     * Indicates whether the device has some kind of calender we can sync to.
     * Also used for generated sunrise/sunset events
     */
    boolean supportsCalendarEvents();

    /**
     * Indicates whether the device supports getting a stream of live data.
     * This can be live HR, steps etc.
     */
    boolean supportsRealtimeData();

    /**
     * Indicates whether the device supports REM sleep tracking.
     */
    boolean supportsRemSleep();

    /**
     * Indicates whether the device supports current weather and/or weather
     * forecast display.
     */
    boolean supportsWeather();

    /**
     * Indicates whether the device supports being found by vibrating, 
     * making some sound or lighting up
     */
    boolean supportsFindDevice();

    /**
     * Indicates whether the device supports displaying music information
     * like artist, title, album, play state etc.
     */
    boolean supportsMusicInfo();

    /**
     * Indicates the maximum reminder message length.
     */
    int getMaximumReminderMessageLength();

    /**
     * Indicates the maximum number of reminder slots available in the device.
     */
    int getReminderSlotCount(GBDevice device);

    /**
     * Indicates the maximum number of canned replies available in the device.
     */
    int getCannedRepliesSlotCount(GBDevice device);

    /**
     * Indicates the maximum number of slots available for world clocks in the device.
     */
    int getWorldClocksSlotCount();

    /**
     * Indicates the maximum label length for a world clock in the device.
     */
    int getWorldClocksLabelLength();

    /**
     * Indicates whether the device supports disabled world clocks that can be enabled through
     * a menu on the device.
     */
    boolean supportsDisabledWorldClocks();

    /**
     * Indicates the maximum number of slots available for contacts in the device.
     */
    int getContactsSlotCount(GBDevice device);

    /**
     * Indicates whether the device has an led which supports custom colors
     */
    boolean supportsLedColor();

    /**
     * Indicates whether the device's led supports any RGB color,
     * or only preset colors
     */
    boolean supportsRgbLedColor();

    /**
     * Returns the preset colors supported by the device, if any, in ARGB, with alpha = 255
     */
    @NonNull
    int[] getColorPresets();

    /**
     * Indicates whether the device supports unicode emojis.
     */
    boolean supportsUnicodeEmojis();

    /**
     * Returns device specific settings related to connection
     *
     * @return int[]
     */
    int[] getSupportedDeviceSpecificConnectionSettings();

    /**
     * Returns device specific settings related to the application itself
     * charts settings and so on
     * @return int[]
     */
    int[] getSupportedDeviceSpecificApplicationSettings();

    /**
     * Returns device specific settings related to the Auth key
     * @return int[]
     */
    int[] getSupportedDeviceSpecificAuthenticationSettings();

    /**
     * Indicates which device specific settings the device supports (not per device type or family, but unique per device).
     */
    int[] getSupportedDeviceSpecificSettings(GBDevice device);

    /**
     * Returns the {@link DeviceSpecificSettingsCustomizer}, allowing for the customization of the devices specific settings screen.
     */
    DeviceSpecificSettingsCustomizer getDeviceSpecificSettingsCustomizer(GBDevice device);

    /**
     * Indicates which device specific language the device supports
     */
    String[] getSupportedLanguageSettings(GBDevice device);

    /**
     *
     * Multiple battery support: Indicates how many batteries the device has.
     * 1 is default, 3 is maximum at the moment (as per UI layout)
     * 0 will disable the battery from the UI
     */
    int getBatteryCount();

    BatteryConfig[] getBatteryConfig();

    boolean supportsPowerOff();

    PasswordCapabilityImpl.Mode getPasswordCapability();

    List<HeartRateCapability.MeasurementInterval> getHeartRateMeasurementIntervals();

    /**
     * Whether the device supports screens with configurable widgets.
     */
    boolean supportsWidgets(GBDevice device);

    /**
     * Gets the {@link WidgetManager} for this device. Must not be null if supportsWidgets is true.
     */
    @Nullable
    WidgetManager getWidgetManager(GBDevice device);

    boolean supportsNavigation();

    int getOrderPriority();

    @NonNull
    Class<? extends DeviceSupport> getDeviceSupportClass();

    EnumSet<ServiceDeviceSupport.Flags> getInitialFlags();

    @StringRes
    int getDeviceNameResource();

    @DrawableRes
    int getDefaultIconResource();

    @DrawableRes
    int getDisabledIconResource();

    /**
     * Whether the device supports a variety of vibration patterns for notifications.
     */
    boolean supportsNotificationVibrationPatterns();
    /**
     * Whether the device supports a variety of vibration pattern repetitions for notifications.
     */
    boolean supportsNotificationVibrationRepetitionPatterns();

    /**
     * Whether the device supports a variety of LED patterns for notifications.
     */
    boolean supportsNotificationLedPatterns();
    /**
     * What vibration pattern repetitions for notifications are supported by the device.
     */
     AbstractNotificationPattern[] getNotificationVibrationPatterns();
    /**
     * What vibration pattern repetitions for notifications are supported by the device.
     * Technote: this is not an int or a range because some devices (e.g. Wena 3) only allow
     * a very specific set of value combinations here.
     */
    AbstractNotificationPattern[] getNotificationVibrationRepetitionPatterns();
    /**
     * What LED patterns for notifications are supported by the device.
     */
    AbstractNotificationPattern[] getNotificationLedPatterns();

    boolean validateAuthKey(String authKey);
}
