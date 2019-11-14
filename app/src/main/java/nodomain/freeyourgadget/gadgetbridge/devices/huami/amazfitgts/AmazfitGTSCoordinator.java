package nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitgts;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;

public class AmazfitGTSCoordinator extends HuamiCoordinator {
    private static final Logger LOG = LoggerFactory.getLogger(AmazfitGTSCoordinator.class);

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.AMAZFITGTS;
    }

    @NonNull
    @Override
    public DeviceType getSupportedType(GBDeviceCandidate candidate) {
        try {
            BluetoothDevice device = candidate.getDevice();
            String name = device.getName();
            if (name != null && name.equalsIgnoreCase("Amazfit GTS")) {
                return DeviceType.AMAZFITGTS;
            }
        } catch (Exception ex) {
            LOG.error("unable to check device support", ex);
        }
        return DeviceType.UNKNOWN;
    }

    @Override
    public InstallHandler findInstallHandler(Uri uri, Context context) {
        AmazfitGTSFWInstallHandler handler = new AmazfitGTSFWInstallHandler(uri, context);
        return handler.isValid() ? handler : null;
    }

    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_REQUIRE_KEY;
    }

    @Override
    public boolean supportsHeartRateMeasurement(GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsWeather() {
        return true;
    }

    @Override
    public boolean supportsActivityTracks() {
        return true;
    }

    @Override
    public boolean supportsMusicInfo() {
        return true;
    }

    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        return new int[]{
                R.xml.devicesettings_amazfitgtr,
                R.xml.devicesettings_wearlocation,
                R.xml.devicesettings_liftwrist_display,
                R.xml.devicesettings_disconnectnotification,
                R.xml.devicesettings_expose_hr_thirdparty,
                R.xml.devicesettings_pairingkey
        };
    }
}
