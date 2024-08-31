/*  Copyright (C) 2022-2024 Daniel Dakhno, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos;

import static nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiFirmwareType.AGPS_UIHH;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import androidx.annotation.Nullable;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.InstallActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.ZeppOsFwHelper;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceApp;
import nodomain.freeyourgadget.gadgetbridge.model.GenericItem;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;

public class ZeppOsFwInstallHandler implements InstallHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsFwInstallHandler.class);

    private final Uri mUri;
    private final Context mContext;

    private final ZeppOsFwHelper mHelper;

    public ZeppOsFwInstallHandler(final Uri uri, final Context context, final String deviceName, final Set<Integer> deviceSources) {
        mUri = uri;
        mContext = context;
        mHelper = new ZeppOsFwHelper(uri, context, deviceName, deviceSources);
    }

    @Nullable
    @Override
    public Class<? extends Activity> getInstallActivity() {
        return null;
    }

    @Override
    public boolean isValid() {
        return mHelper.isValid();
    }

    @Override
    public void validateInstallation(InstallActivity installActivity, GBDevice device) {
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

        if (!mHelper.isValid()) {
            installActivity.setInfoText(mContext.getString(R.string.fwapp_install_device_not_supported));
            installActivity.setInstallEnabled(false);
            return;
        }

        final DeviceCoordinator coordinator = device.getDeviceCoordinator();

        final GenericItem fwItem = new GenericItem(mContext.getString(
                R.string.installhandler_firmware_name,
                device.getAliasOrName(),
                mHelper.getFirmwareType(), // TODO human name
                mHelper.getFirmwareVersion()
        ));
        fwItem.setIcon(coordinator.getDefaultIconResource());

        final StringBuilder sb = new StringBuilder();
        if (!mHelper.getFirmwareType().isWatchface() && !mHelper.getFirmwareType().isApp() && mHelper.getFirmwareType() != AGPS_UIHH) {
            sb.append(mContext.getString(
                    R.string.fw_upgrade_notice,
                    mContext.getString(R.string.fw_upgrade_notice_zepp_os, mHelper.getFirmwareVersion(), device.getAliasOrName())
            ));

            // TODO whitelisted firmware
            //if (mHelper.isFirmwareWhitelisted()) {
            //    sb.append(" ").append(mContext.getString(R.string.miband_firmware_known));
            //    fwItem.setDetails(mContext.getString(R.string.miband_fwinstaller_compatible_version));
            //    // TODO: set a CHECK (OKAY) button
            //} else {
                sb.append("  ").append(mContext.getString(R.string.miband_firmware_unknown_warning)).append(" \n\n")
                        .append(mContext.getString(R.string.miband_firmware_suggest_whitelist, String.valueOf(mHelper.getFirmwareVersion())));
                fwItem.setDetails(mContext.getString(R.string.miband_fwinstaller_untested_version));
                // TODO: set a UNKNOWN (question mark) button
            //}
        }

        installActivity.setPreview(mHelper.getPreview());

        installActivity.setInfoText(sb.toString());
        installActivity.setInstallItem(fwItem);
        installActivity.setInstallEnabled(true);
    }

    @Override
    public void onStartInstall(final GBDevice device) {
        final boolean shouldCache = mHelper.getFirmwareType().isApp() || mHelper.getFirmwareType().isWatchface();
        if (shouldCache) {
            saveToCache(device);
        }
    }

    private void saveToCache(final GBDevice device) {
        final DeviceCoordinator coordinator = device.getDeviceCoordinator();

        final File appCacheDir;
        try {
            appCacheDir = coordinator.getAppCacheDir();
        } catch (final IOException e) {
            LOG.error("Failed to get app cache dir", e);
            return;
        }

        final GBDeviceApp app = mHelper.getAppInfo();
        if (app == null) {
            LOG.warn("Unable to cache app, appInfo is null");
            return;
        }

        // write app zip
        final File appOutputFile = new File(appCacheDir, app.getUUID().toString() + coordinator.getAppFileExtension());
        try {
            appCacheDir.mkdirs();
            FileUtils.copyURItoFile(mContext, mUri, appOutputFile);
        } catch (final IOException e) {
            LOG.error("Failed to save app to cache", e);
            return;
        }

        // write app metadata
        final File metadataOutputFile = new File(appCacheDir, app.getUUID().toString() + ".json");
        try (Writer writer = new BufferedWriter(new FileWriter(metadataOutputFile))) {
            final JSONObject appJSON = app.getJSON();
            writer.write(appJSON.toString());
        } catch (final IOException e) {
            LOG.error("Failed to write app metadata", e);
            return;
        }

        if (app.getPreviewImage() != null) {
            final File previewOutputFile = new File(appCacheDir, app.getUUID().toString() + "_preview.png");
            try (FileOutputStream fos = new FileOutputStream(previewOutputFile)) {
                app.getPreviewImage().compress(Bitmap.CompressFormat.PNG, 9, fos);
            } catch (final IOException e) {
                LOG.error("Failed to write app preview", e);
            }
        }
    }
}
