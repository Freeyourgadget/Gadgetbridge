/*  Copyright (C) 2020-2024 MPeter, Taavi Eomäe

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
package nodomain.freeyourgadget.gadgetbridge.devices.pinetime;

import android.content.Context;
import android.net.Uri;

import com.google.gson.Gson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.InstallActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.model.GenericItem;
import nodomain.freeyourgadget.gadgetbridge.util.UriHelper;
import nodomain.freeyourgadget.gadgetbridge.util.GBZipFile;
import nodomain.freeyourgadget.gadgetbridge.util.ZipFileException;

public class PineTimeInstallHandler implements InstallHandler {
    private static final Logger LOG = LoggerFactory.getLogger(PineTimeInstallHandler.class);
    private static final Pattern binNameVersionPattern = Pattern.compile(".*-((?:\\d+\\.){2}\\d+).bin$");

    private final Context context;

    public enum InfiniTimeUpdateType {
        UNKNOWN,
        DFU,
        RESOURCES
    }
    public InfiniTimeUpdateType updateType;

    private InfiniTimeDFUPackage dfuPackageManifest;

    public PineTimeInstallHandler(Uri uri, Context context) {
        this.context = context;

        updateType = InfiniTimeUpdateType.UNKNOWN;
        UriHelper uriHelper;

        try {
            uriHelper = UriHelper.get(uri, this.context);

            GBZipFile zipPackage = new GBZipFile(uriHelper.openInputStream());
            if (zipPackage.fileExists("manifest.json")) {
                updateType = InfiniTimeUpdateType.DFU;
                String manifest = new String(zipPackage.getFileFromZip("manifest.json"));

                if (!manifest.trim().isEmpty()) {
                    dfuPackageManifest = new Gson().fromJson(manifest.trim(), InfiniTimeDFUPackage.class);
                }
            } else if (zipPackage.fileExists("resources.json")) {
                updateType = InfiniTimeUpdateType.RESOURCES;
            } else {
                LOG.error("Unable to determine update type, no manifest.json or resources.json file found.");
            }
        } catch (ZipFileException e) {
            LOG.error("Unable to read the zip file.", e);
        } catch (FileNotFoundException e) {
            LOG.error("The update file was not found.", e);
        } catch (IOException e) {
            LOG.error("General IO error occurred.", e);
        } catch (Exception e) {
            LOG.error("Unknown error occurred.", e);
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

        if (!isValid()) {
            LOG.error("Firmware cannot be installed (not valid)");
            installActivity.setInfoText("Firmware cannot be installed (not valid)");
            installActivity.setInstallEnabled(false);
        }

        GenericItem installItem = new GenericItem();
        installItem.setIcon(R.drawable.ic_firmware);
        if (updateType == InfiniTimeUpdateType.DFU) {
            installItem.setName("PineTime firmware");
        } else {
            installItem.setName("PineTime resources");
        }
        installItem.setDetails(getVersion());

        installActivity.setInfoText(context.getString(R.string.firmware_install_warning, "(unknown)"));
        installActivity.setInstallItem(installItem);
        LOG.debug("Initialized PineTimeInstallHandler");
    }

    @Override
    public void onStartInstall(GBDevice device) {
    }

    @Override
    public boolean isValid() {
        if (updateType == InfiniTimeUpdateType.DFU) {
            return dfuPackageManifest != null &&
                dfuPackageManifest.manifest != null &&
                dfuPackageManifest.manifest.application != null &&
                dfuPackageManifest.manifest.application.bin_file != null;
        } else if (updateType == InfiniTimeUpdateType.RESOURCES) {
            return true; // TODO What counts as valid for a resource update?
        } else { // updateType == UNKNOWN
            return false;
        }
    }

    // TODO: obtain version information from manifest file instead
    private String getVersion() {
        if (updateType == InfiniTimeUpdateType.DFU) {
            String binFileName = dfuPackageManifest.manifest.application.bin_file;
            Matcher regexMatcher = binNameVersionPattern.matcher(binFileName);

            if (regexMatcher.matches())
                return regexMatcher.group(1);
        }
        // TODO Get version of a resources.package
        return "(Unknown version)";
    }
}
