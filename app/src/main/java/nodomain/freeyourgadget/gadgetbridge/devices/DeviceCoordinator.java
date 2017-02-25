package nodomain.freeyourgadget.gadgetbridge.devices;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.le.ScanFilter;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;

import java.util.Collection;

import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;

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
     * Checks whether this coordinator handles the given candidate.
     * Returns the supported device type for the given candidate or
     * DeviceType.UNKNOWN
     *
     * @param candidate
     * @return the supported device type for the given candidate.
     */
    @NonNull
    DeviceType getSupportedType(GBDeviceCandidate candidate);

    /**
     * Checks whether this coordinator handles the given candidate.
     *
     * @param candidate
     * @return true if this coordinator handles the given candidate.
     */
    boolean supports(GBDeviceCandidate candidate);

    /**
     * Checks whether this candidate handles the given device.
     *
     * @param device
     * @return true if this coordinator handles the given device.
     */
    boolean supports(GBDevice device);

    /**
     * Returns a list of scan filters that shall be used to discover devices supported
     * by this coordinator.
     * @return the list of scan filters, may be empty
     */
    @NonNull
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    Collection<? extends ScanFilter> createBLEScanFilters();

    GBDevice createDevice(GBDeviceCandidate candidate);

    /**
     * Deletes all information, including all related database content about the
     * given device.
     * @throws GBException
     */
    void deleteDevice(GBDevice device) throws GBException;

    /**
     * Returns the kind of device type this coordinator supports.
     *
     * @return
     */
    DeviceType getDeviceType();

    /**
     * Returns the Activity class to be started in order to perform a pairing of a
     * given device.
     *
     * @return
     */
    Class<? extends Activity> getPairingActivity();

    /**
     * Returns the Activity class that will be used as the primary activity
     * for the given device.
     *
     * @return
     */
    Class<? extends Activity> getPrimaryActivity();

    /**
     * Returns true if activity data fetching is supported by the device
     * (with this coordinator).
     *
     * @return
     */
    boolean supportsActivityDataFetching();

    /**
     * Returns true if activity tracking is supported by the device
     * (with this coordinator).
     *
     * @return
     */
    boolean supportsActivityTracking();

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
     * Returns true if this device/coordinator supports setting alarms.
     *
     * @return
     */
    boolean supportsAlarmConfiguration();

    /**
     * Returns true if this device/coordinator supports alarms with smart wakeup
     * @return
     */
    boolean supportsSmartWakeup(GBDevice device);

    /**
     * Returns true if the given device supports heart rate measurements.
     * @return
     */
    boolean supportsHeartRateMeasurement(GBDevice device);

    int getTapString();

    /**
     * Returns the readable name of the manufacturer.
     */
    String getManufacturer();

    /**
     * Returns true if this device/coordinator supports managing device apps.
     *
     * @return
     */
    boolean supportsAppsManagement();

    /**
     * Returns the Activity class that will be used to manage device apps.
     *
     * @return
     */
    Class<? extends Activity> getAppsManagementActivity();

    /**
     * Returns true if the given device needs a background webview for
     * executing javascript or configuration, for example.
     *
     * @param device
     */
    boolean needsBackgroundWebView(GBDevice device);
}
