/*  Copyright (C) 2024 José Rebelo

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.cmfwatchpro;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.FwAppInstallerActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.InstallActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.GenericItem;

public class CmfInstallHandler implements InstallHandler {
    protected final Uri mUri;
    protected final Context mContext;
    protected final CmfFwHelper helper;

    public CmfInstallHandler(final Uri uri, final Context context) {
        this.mUri = uri;
        this.mContext = context;
        this.helper = new CmfFwHelper(uri, context);
    }

    @Override
    public Class<? extends Activity> getInstallActivity() {
        return FwAppInstallerActivity.class;
    }

    @Override
    public boolean isValid() {
        return helper.isValid();
    }

    @Override
    public void validateInstallation(final InstallActivity installActivity, final GBDevice device) {
        if (device.isBusy()) {
            installActivity.setInfoText(device.getBusyTask());
            installActivity.setInstallEnabled(false);
            return;
        }

        if (!device.isInitialized()) {
            installActivity.setInfoText(mContext.getString(R.string.fwapp_install_device_not_ready));
            installActivity.setInstallEnabled(false);
            return;
        }

        if (!helper.isValid()) {
            installActivity.setInfoText(mContext.getString(R.string.fwapp_install_device_not_supported));
            installActivity.setInstallEnabled(false);
            return;
        }

        final GenericItem installItem = new GenericItem();
        if (helper.isWatchface()) {
            installItem.setIcon(R.drawable.ic_watchface);
            installItem.setName(mContext.getString(R.string.kind_watchface));
        } else if (helper.isFirmware()) {
            installItem.setIcon(R.drawable.ic_firmware);
            installItem.setName(mContext.getString(R.string.kind_firmware));
        } else if (helper.isAgps()) {
            installItem.setIcon(R.drawable.ic_firmware);
            installItem.setName(mContext.getString(R.string.kind_agps_bundle));
        } else {
            installItem.setIcon(R.drawable.ic_device_unknown);
            installItem.setName(mContext.getString(R.string.kind_invalid));
        }

        installItem.setDetails(helper.getDetails());

        installActivity.setInfoText(mContext.getString(R.string.firmware_install_warning, "(unknown)"));
        installActivity.setInstallItem(installItem);
        installActivity.setInstallEnabled(true);
    }

    @Override
    public void onStartInstall(final GBDevice device) {
        helper.unsetFwBytes(); // free up memory
    }
}
