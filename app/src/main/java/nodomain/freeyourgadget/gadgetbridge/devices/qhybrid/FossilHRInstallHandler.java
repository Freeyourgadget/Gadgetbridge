/*  Copyright (C) 2020-2021 Andreas Shimokawa

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
package nodomain.freeyourgadget.gadgetbridge.devices.qhybrid;

import android.content.Context;
import android.net.Uri;

import java.io.IOException;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.InstallActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.model.GenericItem;

public class FossilHRInstallHandler implements InstallHandler {
    private final Uri mUri;
    private final Context mContext;
    private FossilFileReader fossilFile;

    FossilHRInstallHandler(Uri uri, Context context) {
        mUri = uri;
        mContext = context;
        try {
            fossilFile = new FossilFileReader(uri, mContext);
        } catch (IOException ignored) {
        }
    }

    @Override
    public void validateInstallation(InstallActivity installActivity, GBDevice device) {
        if (device.isBusy()) {
            installActivity.setInfoText(device.getBusyTask());
            installActivity.setInstallEnabled(false);
            return;
        }
        if (device.getType() != DeviceType.FOSSILQHYBRID || !device.isConnected() || !fossilFile.isValid()) {
            installActivity.setInfoText("Element cannot be installed");
            installActivity.setInstallEnabled(false);
            return;
        }
        GenericItem installItem = new GenericItem();
        if (fossilFile.isFirmware()) {
            installItem.setIcon(R.drawable.ic_firmware);
            installItem.setName(fossilFile.getName());
            installItem.setDetails(fossilFile.getVersion());
            installActivity.setInfoText(mContext.getString(R.string.firmware_install_warning, "(unknown)"));
        } else if (fossilFile.isApp()) {
            installItem.setName(fossilFile.getName());
            installItem.setDetails(fossilFile.getVersion());
            installItem.setIcon(R.drawable.ic_watchapp);
            installActivity.setInfoText(mContext.getString(R.string.app_install_info, installItem.getName(), fossilFile.getVersion(), "(unknown)"));
        } else if (fossilFile.isWatchface()) {
            installItem.setName(fossilFile.getName());
            installItem.setDetails(fossilFile.getVersion());
            installItem.setIcon(R.drawable.ic_watchface);
            installActivity.setInfoText(mContext.getString(R.string.watchface_install_info, installItem.getName(), fossilFile.getVersion(), "(unknown)"));
        } else {
            installActivity.setInfoText("Element cannot be installed");
            installActivity.setInstallEnabled(false);
            return;
        }
        installActivity.setInstallEnabled(true);
        installActivity.setInstallItem(installItem);
    }


    @Override
    public void onStartInstall(GBDevice device) {
    }

    @Override
    public boolean isValid() {
        return fossilFile.isValid();
    }
}
