/*  Copyright (C) 2020-2021 Andreas Shimokawa, Taavi Eomäe

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

import static java.nio.charset.StandardCharsets.UTF_8;

import android.content.Context;
import android.net.Uri;
import android.os.Build;

import com.google.gson.Gson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.InstallActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.model.GenericItem;
import nodomain.freeyourgadget.gadgetbridge.util.UriHelper;

public class PineTimeInstallHandler implements InstallHandler {
    private static final Logger LOG = LoggerFactory.getLogger(PineTimeInstallHandler.class);

    private final Context context;
    private boolean valid = false;
    private String version = "(Unknown version)";

    public PineTimeInstallHandler(Uri uri, Context context) {
        this.context = context;

        UriHelper uriHelper;
        InputStream inputStream;
        ZipInputStream zipInputStream;

        InfiniTimeDFUPackage metadata = null;
        try {
            uriHelper = UriHelper.get(uri, this.context);
            inputStream = new BufferedInputStream(uriHelper.openInputStream());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                zipInputStream = new ZipInputStream(inputStream, UTF_8);
            } else {
                zipInputStream = new ZipInputStream(inputStream);
            }

            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                if (entry.getName().equals("manifest.json")) {
                    LOG.debug("Found manifest.json in DFU zip");
                    StringBuilder json = new StringBuilder();

                    final byte[] buffer = new byte[1024];

                    while (zipInputStream.read(buffer, 0, buffer.length) != -1) {
                        json.append(new String(buffer));
                    }

                    Gson gson = new Gson();
                    metadata = gson.fromJson(json.toString().trim(), InfiniTimeDFUPackage.class);
                    continue;
                }
            }

            zipInputStream.close();
            inputStream.close();
        } catch (Exception e) {
            valid = false;
            return;
        }

        if (metadata != null &&
                metadata.manifest != null &&
                metadata.manifest.application != null &&
                metadata.manifest.application.bin_file != null) {
            valid = true;
            version = metadata.manifest.application.bin_file;
        } else {
            valid = false;
            LOG.error("Somehow metadata was found, but some data was missing");
        }
    }

    @Override
    public void validateInstallation(InstallActivity installActivity, GBDevice device) {
        installActivity.setInstallEnabled(true);

        if (device.isBusy()) {
            LOG.error("Firmware cannot be installed (device busy)");
            installActivity.setInfoText("Firmware cannot be installed (device busy)");
            installActivity.setInfoText(device.getBusyTask());
            installActivity.setInstallEnabled(false);
            return;
        }

        if (device.getType() != DeviceType.PINETIME_JF || !device.isConnected()) {
            LOG.error("Firmware cannot be installed (not connected or wrong device)");
            installActivity.setInfoText("Firmware cannot be installed (not connected or wrong device)");
            installActivity.setInstallEnabled(false);
            return;
        }

        if (!valid) {
            LOG.error("Firmware cannot be installed (not valid)");
            installActivity.setInfoText("Firmware cannot be installed (not valid)");
            installActivity.setInstallEnabled(false);
        }

        GenericItem installItem = new GenericItem();
        installItem.setIcon(R.drawable.ic_firmware);
        installItem.setName("PineTime firmware");
        installItem.setDetails(version);

        installActivity.setInfoText(context.getString(R.string.firmware_install_warning, "(unknown)"));
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
