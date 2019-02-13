/*  Copyright (C) 2015-2019 Andreas Shimokawa, Carsten Pfeiffer

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
import nodomain.freeyourgadget.gadgetbridge.model.GenericItem;

public abstract class AbstractMiBandFWInstallHandler implements InstallHandler {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractMiBandFWInstallHandler.class);

    protected final Context mContext;
    protected AbstractMiBandFWHelper helper;
    private String errorMessage;

    public AbstractMiBandFWInstallHandler(Uri uri, Context context) {
        mContext = context;

        try {
            helper = createHelper(uri, context);
        } catch (IOException e) {
            errorMessage = e.getMessage();
            LOG.warn(errorMessage, e);
        }
    }

    public Context getContext() {
        return mContext;
    }

    public AbstractMiBandFWHelper getHelper() {
        return helper;
    }

    protected abstract AbstractMiBandFWHelper createHelper(Uri uri, Context context) throws IOException;

    protected GenericItem createInstallItem(GBDevice device) {
        return new GenericItem(mContext.getString(R.string.installhandler_firmware_name, mContext.getString(device.getType().getName()), helper.getFirmwareKind(), helper.getHumanFirmwareVersion()));
    }

    protected String getFwUpgradeNotice() {
        return mContext.getString(R.string.fw_upgrade_notice, helper.getHumanFirmwareVersion());
    }

    @Override
    public void validateInstallation(InstallActivity installActivity, GBDevice device) {
        if (device.isBusy()) {
            installActivity.setInfoText(device.getBusyTask());
            installActivity.setInstallEnabled(false);
            return;
        }

        if (!isSupportedDeviceType(device) || !device.isInitialized()) {
            installActivity.setInfoText(mContext.getString(R.string.fwapp_install_device_not_ready));
            installActivity.setInstallEnabled(false);
            return;
        }

        try {
            helper.checkValid();
        } catch (IllegalArgumentException ex) {
            installActivity.setInfoText(ex.getLocalizedMessage());
            installActivity.setInstallEnabled(false);
            return;
        }

        GenericItem fwItem = createInstallItem(device);
        fwItem.setIcon(device.getType().getIcon());

        if (!helper.isFirmwareGenerallyCompatibleWith(device)) {
            fwItem.setDetails(mContext.getString(R.string.miband_fwinstaller_incompatible_version));
            installActivity.setInfoText(mContext.getString(R.string.fwinstaller_firmware_not_compatible_to_device));
            installActivity.setInstallEnabled(false);
            return;
        }
        StringBuilder builder = new StringBuilder();
        if (helper.isSingleFirmware()) {
            getFwUpgradeNotice();
            builder.append(getFwUpgradeNotice());
        } else {
            builder.append(mContext.getString(R.string.fw_multi_upgrade_notice, helper.getHumanFirmwareVersion(), helper.getHumanFirmwareVersion2()));
        }


        if (helper.isFirmwareWhitelisted()) {
            builder.append(" ").append(mContext.getString(R.string.miband_firmware_known));
            fwItem.setDetails(mContext.getString(R.string.miband_fwinstaller_compatible_version));
            // TODO: set a CHECK (OKAY) button
        } else {
            builder.append("  ").append(mContext.getString(R.string.miband_firmware_unknown_warning)).append(" \n\n")
                    .append(mContext.getString(R.string.miband_firmware_suggest_whitelist, String.valueOf(helper.getFirmwareVersion())));
            fwItem.setDetails(mContext.getString(R.string.miband_fwinstaller_untested_version));
            // TODO: set a UNKNOWN (question mark) button
        }
        installActivity.setInfoText(builder.toString());
        installActivity.setInstallItem(fwItem);
        installActivity.setInstallEnabled(true);
    }

    protected abstract boolean isSupportedDeviceType(GBDevice device);

    @Override
    public void onStartInstall(GBDevice device) {

    }

    @Override
    public boolean isValid() {
        return helper != null;
    }
}
