/*  Copyright (C) 2022 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiFWHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.AbstractMiBandFWInstallHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceApp;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;

public abstract class AbstractHuami2021FWInstallHandler extends AbstractMiBandFWInstallHandler {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractHuami2021FWInstallHandler.class);

    public AbstractHuami2021FWInstallHandler(final Uri uri, final Context context) {
        super(uri, context);
    }

    @Override
    public void onStartInstall(final GBDevice device) {
        final AbstractHuamiFirmwareInfo firmwareInfo = getHelper().getFirmwareInfo();
        final boolean shouldCache = firmwareInfo.getFirmwareType().isApp() || firmwareInfo.getFirmwareType().isWatchface();
        if (shouldCache) {
            if (firmwareInfo instanceof Huami2021FirmwareInfo) {
                saveToCache((Huami2021FirmwareInfo) firmwareInfo, device);
            } else {
                LOG.warn("firmwareInfo is {} - this should never happen", firmwareInfo.getClass());
            }
        }

        // Unset the firmware bytes
        // Huami2021 firmwares are large (> 130MB). With the current architecture, the update operation
        // will re-read them to memory, and we run out-of-memory.
        helper.unsetFwBytes();
    }

    protected abstract HuamiFWHelper createHelper(Uri uri, Context context) throws IOException;

    public HuamiFWHelper getHelper() {
        return (HuamiFWHelper) helper;
    }

    private void saveToCache(final Huami2021FirmwareInfo firmwareInfo, final GBDevice device) {
        final DeviceCoordinator coordinator = device.getDeviceCoordinator();

        final File appCacheDir;
        try {
            appCacheDir = coordinator.getAppCacheDir();
        } catch (final IOException e) {
            LOG.error("Failed to get app cache dir", e);
            return ;
        }

        final GBDeviceApp app = firmwareInfo.getAppInfo();

        // write app zip
        final File appOutputFile = new File(appCacheDir, app.getUUID().toString() + coordinator.getAppFileExtension());
        try {
            appCacheDir.mkdirs();
            FileUtils.copyURItoFile(getContext(), getUri(), appOutputFile);
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
