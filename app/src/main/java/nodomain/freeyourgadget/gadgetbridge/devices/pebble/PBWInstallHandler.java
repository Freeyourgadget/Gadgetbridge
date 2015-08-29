package nodomain.freeyourgadget.gadgetbridge.devices.pebble;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.IOException;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.InstallActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceApp;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.model.GenericItem;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;

public class PBWInstallHandler implements InstallHandler {

    private final Context mContext;
    private PBWReader mPBWReader;
    private final Uri mUri;

    public PBWInstallHandler(Uri uri, Context context) {
        mContext = context;
        mUri = uri;
    }

    @Override
    public void validateInstallation(InstallActivity installActivity, GBDevice device) {
        if (device.isBusy()) {
            installActivity.setInfoText(device.getBusyTask());
            installActivity.setInstallEnabled(false);
            return;
        }
        if (device.getType() != DeviceType.PEBBLE || !device.isConnected()) {
            installActivity.setInfoText("Element cannot be installed");
            installActivity.setInstallEnabled(false);
            return;
        }

        mPBWReader = new PBWReader(mUri, mContext, device.getHardwareVersion().equals("dvt") ? "basalt" : "aplite");
        if (!mPBWReader.isValid()) {
            installActivity.setInfoText("pbw/pbz is broken or incompatible with your Hardware or Firmware.");
            installActivity.setInstallEnabled(false);
            return;
        }

        GenericItem installItem = new GenericItem();
        installItem.setIcon(R.drawable.ic_device_pebble);

        if (mPBWReader.isFirmware()) {
            String hwRevision = mPBWReader.getHWRevision();
            if (hwRevision != null && hwRevision.equals(device.getHardwareVersion())) {
                installItem.setName(mContext.getString(R.string.pbw_installhandler_pebble_firmware, ""));
                installItem.setDetails(mContext.getString(R.string.pbwinstallhandler_correct_hw_revision));

                installActivity.setInfoText(mContext.getString(R.string.firmware_install_warning, hwRevision));
                installActivity.setInstallEnabled(true);
            } else {
                if (hwRevision != null) {
                    installItem.setName(mContext.getString(R.string.pbw_installhandler_pebble_firmware, hwRevision));
                    installItem.setDetails(mContext.getString(R.string.pbwinstallhandler_incorrect_hw_revision));
                }
                installActivity.setInfoText(mContext.getString(R.string.pbw_install_handler_hw_revision_mismatch));
                installActivity.setInstallEnabled(false);
            }
        } else {
            GBDeviceApp app = mPBWReader.getGBDeviceApp();
            if (app != null) {
                installItem.setName(app.getName());
                installItem.setDetails(mContext.getString(R.string.pbwinstallhandler_app_item, app.getCreator(), app.getVersion()));
                installActivity.setInfoText(mContext.getString(R.string.app_install_info, app.getName(), app.getVersion(), app.getCreator()));
                installActivity.setInstallEnabled(true);
            } else {
                installActivity.setInfoText(mContext.getString(R.string.pbw_install_handler_unable_to_install, mUri.getPath()));
                installActivity.setInstallEnabled(false);
            }
        }

        if (installItem.getName() != null) {
            installActivity.setInstallItem(installItem);
        }
    }

    @Override
    public void onStartInstall(GBDevice device) {
        if (mPBWReader.isFirmware()) {
            return;
        }

        if (!device.getFirmwareVersion().startsWith("v3")) {
            return;
        }

        GBDeviceApp app = mPBWReader.getGBDeviceApp();
        File pbwFile = new File(mUri.getPath());
        try {
            File destDir = new File(FileUtils.getExternalFilesDir() + "/pbw-cache");
            destDir.mkdirs();
            FileUtils.copyFile(pbwFile, new File(destDir + "/" + app.getUUID().toString() + ".pbw"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isValid() {
        // always pretend it is valid, as we cant know yet about hw/fw version
        return true;
    }

}