/*  Copyright (C) 2020 Taavi Eomäe

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
package nodomain.freeyourgadget.gadgetbridge.devices.pinetime;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.InstallActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.model.GenericItem;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.UriHelper;

public class PineTimeInstallHandler implements InstallHandler {
    private static final Logger LOG = LoggerFactory.getLogger(PineTimeInstallHandler.class);

    private final Context context;
    private boolean valid = false;
    private String version = "(Unknown version)";

    public PineTimeInstallHandler(Uri uri, Context context) {
        this.context = context;
        UriHelper uriHelper;
        try {
            uriHelper = UriHelper.get(uri, this.context);
        } catch (IOException e) {
            valid = false;
            return;
        }

        try (InputStream in = new BufferedInputStream(uriHelper.openInputStream())) {
            byte[] bytes = new byte[32];
            int read = in.read(bytes);
            if (read < 32) {
                valid = false;
                return;
            }
        } catch (Exception e) {
            valid = false;
            return;
        }
        valid = true;
    }

    @Override
    public void validateInstallation(InstallActivity installActivity, GBDevice device) {
        if (device.isBusy()) {
            installActivity.setInfoText(device.getBusyTask());
            installActivity.setInstallEnabled(false);
            return;
        }

        if (device.getType() != DeviceType.PINETIME_JF || !device.isConnected()) {
            installActivity.setInfoText("Firmware cannot be installed");
            installActivity.setInstallEnabled(false);
            return;
        }

        GenericItem installItem = new GenericItem();
        installItem.setIcon(R.drawable.ic_firmware);
        installItem.setName("PineTime firmware");
        installItem.setDetails(version);

        installActivity.setInfoText(context.getString(R.string.firmware_install_warning, "(unknown)"));
        installActivity.setInstallEnabled(true);
        installActivity.setInstallItem(installItem);
        LOG.debug("Initialized PineTimeInstallHandler");
    }


    @Override
    public void onStartInstall(GBDevice device) {
    }

    @Override
    public boolean isValid() {
        return valid;
    }
}
