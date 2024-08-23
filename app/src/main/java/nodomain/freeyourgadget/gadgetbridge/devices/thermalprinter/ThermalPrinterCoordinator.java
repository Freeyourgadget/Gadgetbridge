package nodomain.freeyourgadget.gadgetbridge.devices.thermalprinter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.thermalprinter.GenericThermalPrinterSupport;

public class ThermalPrinterCoordinator extends AbstractDeviceCoordinator {
    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws GBException {

    }

    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_LAZY;
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_bluetooth_printer;
    }

    @Override
    public int getDisabledIconResource() {
        return R.drawable.ic_device_bluetooth_printer_disabled;
    }

    @Override
    public String getManufacturer() {
        return "";
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass() {
        return GenericThermalPrinterSupport.class;
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_generic_thermal_printer;
    }

    @Override
    public boolean supports(GBDeviceCandidate candidate) {
        return candidate.supportsService(GenericThermalPrinterSupport.discoveryService);
    }

    @Override
    public boolean supportsFlashing() {
        return true;
    }

    @Override
    public boolean addBatteryPollingSettings() {
        return true;
    }

    @Nullable
    @Override
    public InstallHandler findInstallHandler(Uri uri, Context context) {
        //TODO: maybe there is another way to implement opening/receiving pictures
        final ImageFilePrinterHandler imageFilePrinterHandler = new ImageFilePrinterHandler(uri, context);
        if (imageFilePrinterHandler.isValid()) {
            Intent instentStartPrintActivity = new Intent(context, SendToPrinterActivity.class);
            instentStartPrintActivity.putExtra(GenericThermalPrinterSupport.INTENT_EXTRA_URI, uri);
            context.startActivity(instentStartPrintActivity);
        }
        return null;
    }

}
