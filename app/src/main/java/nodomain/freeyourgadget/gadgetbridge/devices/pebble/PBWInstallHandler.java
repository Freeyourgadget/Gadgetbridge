package nodomain.freeyourgadget.gadgetbridge.devices.pebble;

import android.content.Context;
import android.net.Uri;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.InstallActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceApp;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;

public class PBWInstallHandler implements InstallHandler {

    private final Context mContext;
    private final PBWReader mPBWReader;
    private final Uri mUri;

    public PBWInstallHandler(Uri uri, Context context) {
        mContext = context;
        mPBWReader = new PBWReader(uri, context);
        mUri = uri;
    }

    @Override
    public void validateInstallation(InstallActivity installActivity, GBDevice device) {
        if (device.isBusy() || device.getType() != DeviceType.PEBBLE || !device.isConnected()) {
            installActivity.setInfoText("Element cannot be installed");
            installActivity.setInstallEnabled(false);
            return;
        }

        if (mPBWReader.isFirmware()) {
            String hwRevision = mPBWReader.getHWRevision();
            if (hwRevision != null && hwRevision.equals(device.getHardwareVersion())) {
                installActivity.setInfoText(mContext.getString(R.string.firmware_install_warning, hwRevision));
                installActivity.setInstallEnabled(true);
            } else {
                installActivity.setInfoText(mContext.getString(R.string.pbw_install_handler_hw_revision_mismatch));
                installActivity.setInstallEnabled(false);
            }
        } else {
            GBDeviceApp app = mPBWReader.getGBDeviceApp();
            if (app != null) {
                installActivity.setInfoText(mContext.getString(R.string.app_install_info, app.getName(), app.getVersion(), app.getCreator()));
                installActivity.setInstallEnabled(true);
            } else {
                installActivity.setInfoText(mContext.getString(R.string.pbw_install_handler_unable_to_install, mUri.getPath()));
                installActivity.setInstallEnabled(false);
            }
        }
    }

    public boolean isValid() {
        return mPBWReader.isValid();
    }
}