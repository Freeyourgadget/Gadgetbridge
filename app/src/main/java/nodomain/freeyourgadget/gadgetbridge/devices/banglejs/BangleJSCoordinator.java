package nodomain.freeyourgadget.gadgetbridge.devices.banglejs;

import java.util.Collection;
import java.util.Collections;


import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.bluetooth.le.ScanFilter;
import android.os.ParcelUuid;
import androidx.annotation.NonNull;

import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.hplus.HPlusConstants;
import nodomain.freeyourgadget.gadgetbridge.devices.id115.ID115SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.HPlusHealthActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;

public class BangleJSCoordinator extends AbstractDeviceCoordinator {

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.BANGLEJS;
    }

    @Override
    public String getManufacturer() {
        return "Espruino";
    }

    @Override
    public Collection<? extends ScanFilter> createBLEScanFilters() {
        // TODO: filter on name beginning Bangle.js? Doesn't appear to be built-in :(
        // https://developer.android.com/reference/android/bluetooth/le/ScanFilter.Builder.html#setDeviceName(java.lang.String)
        ParcelUuid hpService = new ParcelUuid(BangleJSConstants.UUID_SERVICE_NORDIC_UART);
        ScanFilter filter = new ScanFilter.Builder().setServiceUuid(hpService).build();
        return Collections.singletonList(filter);
    }

    @Override
    public DeviceType getSupportedType(GBDeviceCandidate candidate) {
        String name = candidate.getDevice().getName();
        /* Filter by Espruino devices to avoid getting
        the device chooser full of spam devices. */
        if (name != null && (
              name.startsWith("Bangle.js") ||
              name.startsWith("Pixl.js") ||
              name.startsWith("Puck.js") ||
              name.startsWith("MDBT42Q") ||
              name.startsWith("Espruino"))) 
            return DeviceType.BANGLEJS;

        return DeviceType.UNKNOWN;
    }

    @Override
    public int getBondingStyle(){
        // Let the user decide whether to bond or not after discovery.
        return BONDING_STYLE_ASK;
    }

    @Override
    public boolean supportsCalendarEvents() {
        return false;
    }

    @Override
    public boolean supportsRealtimeData() {
        return false;
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
    public boolean supportsActivityDataFetching() {
        return false;
    }

    @Override
    public boolean supportsActivityTracking() {
        return false;
    }

    @Override
    public boolean supportsScreenshots() {
        return false;
    }

    @Override
    public boolean supportsSmartWakeup(GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsHeartRateMeasurement(GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsAppsManagement() {
        return false;
    }

    @Override
    public int getAlarmSlotCount() {
        return 10;
    }

    @Override
    public Class<? extends Activity> getAppsManagementActivity() {
        return null;
    }


    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws GBException {
    }

    @Override
    public Class<? extends Activity> getPairingActivity() {
        return null;
    }

    @Override
    public SampleProvider<? extends ActivitySample> getSampleProvider(GBDevice device, DaoSession session) {
        return null;//new BangleJSSampleProvider(device, session);
    }

    @Override
    public InstallHandler findInstallHandler(Uri uri, Context context) {
        return null;
    }
}
