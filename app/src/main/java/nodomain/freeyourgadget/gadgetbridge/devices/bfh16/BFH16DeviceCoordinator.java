package nodomain.freeyourgadget.gadgetbridge.devices.bfh16;

import android.app.Activity;
import android.bluetooth.le.ScanFilter;
import android.content.Context;
import android.net.Uri;
import android.os.ParcelUuid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;

import androidx.annotation.NonNull;
import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;

public class BFH16DeviceCoordinator extends AbstractDeviceCoordinator
{
    protected static final Logger LOG = LoggerFactory.getLogger(BFH16DeviceCoordinator.class);


    @Override
    public DeviceType getDeviceType() {
        return DeviceType.BFH16;
    }

    @Override
    public String getManufacturer() {
        return "Denver";
    }

    @Override
    public Collection<? extends ScanFilter> createBLEScanFilters() {

        ParcelUuid bfhService = new ParcelUuid(BFH16Constants.BFH16_IDENTIFICATION_SERVICE1);

        ScanFilter filter = new ScanFilter.Builder()
                .setServiceUuid(bfhService)
                .build();

        return Collections.singletonList(filter);
    }

    @Override
    public DeviceType getSupportedType(GBDeviceCandidate candidate) {

        String name = candidate.getDevice().getName();
        if (name != null) {
            if (name.startsWith("BFH-16")) {
                return DeviceType.BFH16;
            }
        }

        return DeviceType.UNKNOWN;

    }

    @Override
    public int getBondingStyle(GBDevice deviceCandidate){
        return BONDING_STYLE_NONE;  //Might be wrong?
    }

    @Override
    public Class<? extends Activity> getPairingActivity(){
        return null;
    }

    //Additional required functions

    /**
     * Hook for subclasses to perform device-specific deletion logic, e.g. db cleanup.
     * @param gbDevice the GBDevice
     * @param device the corresponding database Device
     * @param session the session to use
     * @throws GBException
     */
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws GBException
    {

    }

    /**
     * Returns the sample provider for the device being supported.
     *
     * @return
     */
    public SampleProvider<? extends ActivitySample> getSampleProvider(GBDevice device, DaoSession session)
    {
        return null; //FIXME
    }

    /**
     * Finds an install handler for the given uri that can install the given
     * uri on the device being managed.
     *
     * @param uri
     * @param context
     * @return the install handler or null if that uri cannot be installed on the device
     */
    public InstallHandler findInstallHandler(Uri uri, Context context)
    {
        return null; //FIXME
    }


    //SupportsXXX

    /**
     * Returns true if activity data fetching is supported by the device
     * (with this coordinator).
     *
     * @return
     */
    public boolean supportsActivityDataFetching(){
        return false;
    }

    /**
     * Returns true if activity tracking is supported by the device
     * (with this coordinator).
     *
     * @return
     */
    public boolean supportsActivityTracking()
    {
        return true;
    }

    /**
     * Returns true if this device/coordinator supports taking screenshots.
     *
     * @return
     */
    public boolean supportsScreenshots()
    {
        return false;
    }

    /**
     * Returns the number of alarms this device/coordinator supports
     * Shall return 0 also if it is not possible to set alarms via
     * protocol, but only on the smart device itself.
     *
     * @return
     */
    public int getAlarmSlotCount()
    {
        return 0; //FIXME
    }

    /**
     * Returns true if this device/coordinator supports alarms with smart wakeup
     * @return
     */
    public boolean supportsSmartWakeup(GBDevice device)
    {
        return false;
    }

    /**
     * Returns true if the given device supports heart rate measurements.
     * @return
     */
    public boolean supportsHeartRateMeasurement(GBDevice device)
    {
        return true;
    }

    /**
     * Returns true if this device/coordinator supports managing device apps.
     *
     * @return
     */
    public boolean supportsAppsManagement()
    {
        return false;
    }

    /**
     * Returns the Activity class that will be used to manage device apps.
     *
     * @return
     */
    public Class<? extends Activity> getAppsManagementActivity()
    {
        return null; //FIXME
    }

    /**
     * Indicates whether the device has some kind of calender we can sync to.
     * Also used for generated sunrise/sunset events
     */
    public boolean supportsCalendarEvents()
    {
        return false;
    }

    /**
     * Indicates whether the device supports getting a stream of live data.
     * This can be live HR, steps etc.
     */
    public boolean supportsRealtimeData()
    {
        return false;
    }

    /**
     * Indicates whether the device supports current weather and/or weather
     * forecast display.
     */
    public boolean supportsWeather()
    {
        return false;
    }

    /**
     * Indicates whether the device supports being found by vibrating,
     * making some sound or lighting up
     */
    public boolean supportsFindDevice()
    {
        return false;
    }

    /**
     * Indicates whether the device supports displaying music information
     * like artist, title, album, play state etc.
     */
    public boolean supportsMusicInfo()
    {
        return false;
    }

    /**
     * Indicates whether the device has an led which supports custom colors
     */
    public boolean supportsLedColor()
    {
        return false;
    }

    /**
     * Indicates whether the device's led supports any RGB color,
     * or only preset colors
     */
    public boolean supportsRgbLedColor()
    {
        return false;
    }

}
