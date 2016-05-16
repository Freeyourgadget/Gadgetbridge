package nodomain.freeyourgadget.gadgetbridge.devices;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;

import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractActivitySample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
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
    String EXTRA_DEVICE_MAC_ADDRESS = "nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate.EXTRA_MAC_ADDRESS";

    /**
     * Checks whether this candidate handles the given candidate.
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
    SampleProvider<AbstractActivitySample> getSampleProvider(DBHandler db);

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
     * Returns true if this device/coordinator supports settig alarms.
     *
     * @return
     */
    boolean supportsAlarmConfiguration();

    int getTapString();

    /**
     * Returns the readable name of the manufacturer.
     */
    String getManufacturer();
}
