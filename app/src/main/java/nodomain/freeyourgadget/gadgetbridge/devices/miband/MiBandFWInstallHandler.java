package nodomain.freeyourgadget.gadgetbridge.devices.miband;

import android.content.Context;
import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.InstallActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;

public class MiBandFWInstallHandler implements InstallHandler {
    private static final Logger LOG = LoggerFactory.getLogger(MiBandFWInstallHandler.class);

    private final Context mContext;
    private MiBandFWHelper helper;
    private String errorMessage;

    public MiBandFWInstallHandler(Uri uri, Context context) {
        mContext = context;

        try {
            helper = new MiBandFWHelper(uri, mContext);
        } catch (IOException e) {
            errorMessage = e.getMessage();
            LOG.warn(errorMessage, e);
        }
    }

    @Override
    public void validateInstallation(InstallActivity installActivity, GBDevice device) {
        if (device.isBusy() || device.getType() != DeviceType.MIBAND || !device.isInitialized()) {
            installActivity.setInfoText("Element cannot be installed");
            installActivity.setInstallEnabled(false);
            return;
        }

        StringBuilder builder = new StringBuilder(mContext.getString(R.string.fw_upgrade_notice, helper.getHumanFirmwareVersion()));

        if (helper.isFirmwareWhitelisted()) {
            builder.append(" ").append(mContext.getString(R.string.miband_firmware_known));
        } else {
            builder.append("  ").append(mContext.getString(R.string.miband_firmware_unknown_warning)).append(" ")
                    .append(mContext.getString(R.string.miband_firmware_suggest_whitelist, helper.getFirmwareVersion()));
        }
        installActivity.setInfoText(builder.toString());
        installActivity.setInstallEnabled(true);
    }

    @Override
    public void onStartInstall() {

    }

    public boolean isValid() {
        return helper != null;
    }
}
