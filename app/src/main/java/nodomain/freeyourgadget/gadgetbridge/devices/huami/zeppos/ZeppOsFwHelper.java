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
package nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceApp;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiFirmwareType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.UIHHContainer;
import nodomain.freeyourgadget.gadgetbridge.util.BitmapUtil;
import nodomain.freeyourgadget.gadgetbridge.util.CheckSums;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GBZipFile;
import nodomain.freeyourgadget.gadgetbridge.util.UriHelper;
import nodomain.freeyourgadget.gadgetbridge.util.ZipFileException;

public class ZeppOsFwHelper {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsFwHelper.class);

    private final Uri uri;
    private final Context context;
    private final String deviceName;
    private final Set<Integer> deviceSources;

    private HuamiFirmwareType firmwareType = HuamiFirmwareType.INVALID;
    private File file = null;
    private int crc32;
    private String version = "Unknown";
    private GBDeviceApp gbDeviceApp = null;

    public ZeppOsFwHelper(final Uri uri, final Context context, final String deviceName, final Set<Integer> deviceSources) {
        this.uri = uri;
        this.context = context;
        this.deviceName = deviceName;
        this.deviceSources = deviceSources;

        processUri();
    }

    public HuamiFirmwareType getFirmwareType() {
        return firmwareType;
    }

    public String getFirmwareVersion() {
        return version;
    }

    public File getFile() {
        if (file == null) {
            throw new IllegalStateException("file is null");
        }

        return file;
    }

    public int getSize() {
        if (file == null) {
            throw new IllegalStateException("file is null");
        }

        return (int) file.length();
    }

    public int getCrc32() {
        return crc32;
    }

    private void processUri() {
        // Copy file to cache first
        final File cacheDir = context.getCacheDir();
        final File zpkCacheDir = new File(cacheDir, "zeppos");
        zpkCacheDir.mkdir();

        try {
            file = File.createTempFile("fwhelper", "bin", context.getCacheDir());
            file.deleteOnExit();
        } catch (final IOException e) {
            LOG.error("Failed to create temp file for zpk", e);
            return;
        }

        final UriHelper uriHelper;
        try {
            uriHelper = UriHelper.get(uri, context);
        } catch (final IOException e) {
            LOG.error("Failed to get uri helper", e);
            return;
        }

        final CRC32 crc = new CRC32();
        try (FileOutputStream outputStream = new FileOutputStream(file);
             InputStream inputStream = uriHelper.openInputStream()) {
            final byte[] buffer = new byte[64 * 1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
                crc.update(buffer, 0, len);
            }
            crc32 = (int) crc.getValue();
        } catch (final IOException e) {
            LOG.error("Failed to write bytes to temporary file", e);
            return;
        }

        final byte[] header = getHeader(file, 4);
        if (header == null) {
            return;
        }

        if (Arrays.equals(header, GBZipFile.ZIP_HEADER)) {
            try (ZipFile zipFile = new ZipFile(file, java.util.zip.ZipFile.OPEN_READ)) {
                processZipFile(zipFile);
            } catch (final ZipException e) {
                LOG.warn("{} is not a valid zip file", uri, e);
            } catch (final IOException e) {
                LOG.warn("Error while processing {}", uri, e);
            }
        } else if (Arrays.equals(header, UIHHContainer.UIHH_HEADER)) {
            // FIXME: This should be refactored to avoid pulling the entire file to memory
            // However, it's currently only used for agps updates, which are usually just ~140KB
            try (InputStream in = new BufferedInputStream(uriHelper.openInputStream())) {
                final byte[] fullFile = FileUtils.readAll(in, 32 * 1024 * 1024); // 32MB
                processAsUihh(fullFile);
            } catch (final IOException e) {
                LOG.error("Failed to read full uihh from file", e);
            }
        }
    }

    private void processAsUihh(byte[] bytes) {
        final UIHHContainer uihh = UIHHContainer.fromRawBytes(bytes);
        if (uihh == null) {
            LOG.warn("Invalid UIHH file");
            return;
        }

        final Set<UIHHContainer.FileType> agpsEpoTypes = new HashSet<>();
        UIHHContainer.FileEntry uihhFirmwareZipFile = null;
        boolean hasChangelog = false;
        for (final UIHHContainer.FileEntry file : uihh.getFiles()) {
            switch (file.getType()) {
                case FIRMWARE_ZIP:
                    uihhFirmwareZipFile = file;
                    continue;
                case FIRMWARE_CHANGELOG:
                    hasChangelog = true;
                    continue;
                case AGPS_EPO_GR_3:
                case AGPS_EPO_GAL_7:
                case AGPS_EPO_BDS_3:
                    agpsEpoTypes.add(file.getType());
                    continue;
                default:
                    LOG.warn("Unexpected file for {}", file.getType());
            }
        }

        if (uihhFirmwareZipFile != null && hasChangelog) {
            // UIHH firmware update
            final GBZipFile zipFile = new GBZipFile(uihhFirmwareZipFile.getContent());
            final byte[] firmwareBin;
            try {
                firmwareBin = zipFile.getFileFromZip("META/firmware.bin");
            } catch (final ZipFileException e) {
                LOG.error("Failed to read zip from UIHH", e);
                return;
            }

            if (isCompatibleFirmwareBin(firmwareBin)) {
                firmwareType = HuamiFirmwareType.FIRMWARE_UIHH_2021_ZIP_WITH_CHANGELOG;
            }
        } else if (agpsEpoTypes.size() == 3) {
            // AGPS EPO update
            firmwareType = HuamiFirmwareType.AGPS_UIHH;
        }
    }

    private void processZipFile(final ZipFile zipFile) {
        // Attempt to handle as a firmware
        final byte[] firmwareBin = getFileFromZip(zipFile, "META/firmware.bin");
        if (firmwareBin != null) {
            if (isCompatibleFirmwareBin(firmwareBin)) {
                firmwareType = HuamiFirmwareType.FIRMWARE;
                final JSONObject fwInfoRoot = getJson(zipFile, "META/fw_info");
                if (fwInfoRoot != null) {
                    final JSONArray fwInfoArr = fwInfoRoot.optJSONArray("fw_info");
                    if (fwInfoArr != null) {
                        for (int i = 0; i < fwInfoArr.length(); i++) {
                            final JSONObject fwInfo = fwInfoArr.optJSONObject(i);
                            if (fwInfo == null) {
                                continue;
                            }

                            if ("firmware".equals(fwInfo.optString("name"))) {
                                version = fwInfo.optString("version");
                                break;
                            }
                        }
                    }
                } else {
                    version = getFirmwareVersion(firmwareBin);
                }
            } else {
                firmwareType = HuamiFirmwareType.INVALID;
            }

            return;
        }

        // Attempt to handle as an app / watchface
        final JSONObject appJson = getJson(zipFile, "app.json");
        if (appJson != null) {
            final int appId;
            final String appName;
            final String appVersion;
            final String appType;
            final String appCreator;
            final String appIconPath;
            final JSONObject appJsonApp;
            try {
                appJsonApp = appJson.getJSONObject("app");
                appId = appJsonApp.getInt("appId");
                appName = appJsonApp.getString("appName");
                appVersion = appJsonApp.getJSONObject("version").getString("name");
                appType = appJsonApp.getString("appType");
                appCreator = appJsonApp.getString("vender");
                appIconPath = appJsonApp.getString("icon");
            } catch (final Exception e) {
                LOG.error("Failed to get appType from app.json", e);
                firmwareType = HuamiFirmwareType.INVALID;
                return;
            }

            final GBDeviceApp.Type gbDeviceAppType;
            switch (appType) {
                case "watchface":
                    firmwareType = HuamiFirmwareType.WATCHFACE;
                    gbDeviceAppType = GBDeviceApp.Type.WATCHFACE;
                    version = String.format("%s (watchface)", appName);
                    break;
                case "app":
                    firmwareType = HuamiFirmwareType.APP;
                    gbDeviceAppType = GBDeviceApp.Type.APP_GENERIC;
                    version = String.format("%s (app)", appName);
                    break;
                default:
                    LOG.warn("Unknown app type {}", appType);
                    firmwareType = HuamiFirmwareType.INVALID;
                    return;
            }

            Bitmap icon = null;
            final byte[] iconBytes = getFileFromZip(zipFile, "assets/" + appIconPath);
            if (iconBytes != null) {
                if (BitmapUtil.isPng(iconBytes)) {
                    icon = BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.length);
                } else {
                    icon = BitmapUtil.decodeTga(iconBytes);
                }
            }

            gbDeviceApp = new GBDeviceApp(
                    UUID.fromString(String.format("%08x-0000-0000-0000-000000000000", appId)),
                    appName,
                    appCreator,
                    appVersion,
                    gbDeviceAppType,
                    icon
            );

            return;
        }

        // Attempt to handle as a zab file
        final byte[] zpkBytes = handleZabPackage(zipFile);
        if (zpkBytes != null) {
            final File cacheDir = context.getCacheDir();
            final File zpkCacheDir = new File(cacheDir, "zpk");
            zpkCacheDir.mkdir();

            final File zpkFile;
            try {
                zpkFile = File.createTempFile("zpk", "zip", context.getCacheDir());
                zpkFile.deleteOnExit();
            } catch (final IOException e) {
                LOG.error("Failed to create temp file for zpk", e);
                return;
            }

            try (FileOutputStream outputStream = new FileOutputStream(zpkFile)) {
                outputStream.write(zpkBytes);
            } catch (final IOException e) {
                LOG.error("Failed to write zpk bytes to temporary file", e);
                return;
            }

            try (ZipFile zpkZpkFile = new ZipFile(zpkFile, java.util.zip.ZipFile.OPEN_READ)) {
                processZipFile(zpkZpkFile);
            } catch (final ZipException e) {
                LOG.warn("{} is not a valid zip file", uri, e);
            } catch (final IOException e) {
                LOG.warn("Error while processing {}", uri, e);
            }

            if (firmwareType != HuamiFirmwareType.INVALID) {
                file = zpkFile;
                crc32 = CheckSums.getCRC32(zpkBytes);
            }
        }
    }

    /**
     * A zab package is a zip file with:
     * - manifest.json
     * - .sc (source code)
     * - One or more zpk files
     * <p>
     * Right now, we only handle the first compatible zpk file that is supported by the connected device.
     */
    private byte[] handleZabPackage(final ZipFile zipFile) {
        final JSONObject manifest = getJson(zipFile, "manifest.json");
        if (manifest == null) {
            return null;
        }

        final JSONArray zpks;
        try {
            zpks = manifest.getJSONArray("zpks");
        } catch (final Exception e) {
            LOG.error("Failed to get zpks from manifest.json", e);
            return null;
        }

        // Iterate all zpks until a compatible one is found
        for (int i = 0; i < zpks.length(); i++) {
            try {
                final JSONObject zpkEntry = zpks.getJSONObject(i);
                final JSONArray platforms = zpkEntry.getJSONArray("platforms");

                // Check if this zpk is compatible with the current device
                for (int j = 0; j < platforms.length(); j++) {
                    final JSONObject platform = platforms.getJSONObject(j);

                    if (deviceSources.contains(platform.getInt("deviceSource"))) {
                        // It's compatible with the device, fetch device.zip
                        final String name = zpkEntry.getString("name");
                        final byte[] zpkBytes = getFileFromZip(zipFile, name);
                        if (!GBZipFile.isZipFile(zpkBytes)) {
                            LOG.warn("bytes for {} not a zip file", name);
                            continue;
                        }
                        final GBZipFile zpkFile = new GBZipFile(zpkBytes);
                        final byte[] deviceZip = zpkFile.getFileFromZip("device.zip");
                        if (!GBZipFile.isZipFile(zpkBytes)) {
                            LOG.warn("bytes for device.zip of zpk {} not a zip file", name);
                            continue;
                        }

                        return deviceZip;
                    }
                }
            } catch (final Exception e) {
                LOG.warn("Failed to parse zpk", e);
            }
        }

        LOG.warn("No compatible zpk found in zab file");

        return null;
    }

    @Nullable
    public GBDeviceApp getAppInfo() {
        return gbDeviceApp;
    }

    public boolean isValid() {
        return firmwareType != HuamiFirmwareType.INVALID;
    }

    @Nullable
    public Bitmap getPreview() {
        if (gbDeviceApp != null) {
            return gbDeviceApp.getPreviewImage();
        }

        return null;
    }

    private boolean isCompatibleFirmwareBin(final byte[] firmwareBin) {
        if (firmwareBin == null) {
            LOG.error("firmware bin is null");
            return false;
        }

        if (!searchString(firmwareBin, deviceName)) {
            LOG.warn("Failed to find {} in fwBytes", deviceName);
            return false;
        }

        return true;
    }

    public static String getFirmwareVersion(final byte[] firmwareBin) {
        int startIdx = 10;
        int endIdx = -1;

        for (int i = startIdx; i < startIdx + 20; i++) {
            byte c = firmwareBin[i];

            if (c == 0) {
                endIdx = i;
                break;
            }

            if (c != '.' && (c < '0' || c > '9')) {
                // not a valid version character
                break;
            }
        }

        if (endIdx == -1) {
            LOG.warn("Failed to find firmware version in expected offset");
            return null;
        }

        return new String(Arrays.copyOfRange(firmwareBin, startIdx, endIdx));
    }

    @Nullable
    private static JSONObject getJson(final ZipFile zipFile, final String path) {
        final byte[] appJsonBin = getFileFromZip(zipFile, path);
        if (appJsonBin == null) {
            return null;
        }

        try {
            final String appJsonString = new String(appJsonBin, StandardCharsets.UTF_8)
                    // Remove UTF-8 BOM if present
                    .replace("\uFEFF", "");
            return new JSONObject(appJsonString);
        } catch (final Exception e) {
            LOG.error("Failed to parse " + path, e);
        }

        return null;
    }

    @Nullable
    private static byte[] getFileFromZip(final ZipFile zipFile, final String path) {
        try {
            final ZipEntry entry = zipFile.getEntry(path);
            if (entry == null) {
                return null;
            }
            return GBZipFile.readAllBytes(zipFile.getInputStream(entry));
        } catch (final IOException e) {
            LOG.error("Failed to read " + path, e);
            return null;
        }
    }

    @Nullable
    public static byte[] getHeader(final File file, final int bytes) {
        final byte[] header = new byte[bytes];

        try (InputStream is = new FileInputStream(file)) {
            if (is.read(header) != header.length) {
                LOG.warn("Read unexpected number of header bytes");
                return null;
            }
        } catch (final IOException e) {
            LOG.error("Error while reading header bytes", e);
            return null;
        }

        return header;
    }

    public static boolean searchString(final byte[] fwBytes, final String str) {
        final byte[] strBytes = (str + "\0").getBytes(StandardCharsets.UTF_8);

        for (int i = 0; i < fwBytes.length - strBytes.length + 1; i++) {
            boolean found = true;
            for (int j = 0; j < strBytes.length; j++) {
                if (fwBytes[i + j] != strBytes[j]) {
                    found = false;
                    break;
                }
            }
            if (found) {
                return true;
            }
        }

        return false;
    }
}
