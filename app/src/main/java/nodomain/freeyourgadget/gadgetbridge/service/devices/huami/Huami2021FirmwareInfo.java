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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceApp;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils;
import nodomain.freeyourgadget.gadgetbridge.util.BitmapUtil;
import nodomain.freeyourgadget.gadgetbridge.util.ZipFile;
import nodomain.freeyourgadget.gadgetbridge.util.ZipFileException;


public abstract class Huami2021FirmwareInfo extends AbstractHuamiFirmwareInfo {
    private static final Logger LOG = LoggerFactory.getLogger(Huami2021FirmwareInfo.class);

    private final String preComputedVersion;
    private GBDeviceApp gbDeviceApp;

    public Huami2021FirmwareInfo(final byte[] bytes) {
        super(bytes);
        this.preComputedVersion = preComputeVersion();
    }

    /**
     * The device name, to search on firmware.bin in order to determine compatibility.
     */
    public abstract String deviceName();

    /**
     * The device sources, to match compatible packages.
     * As per: https://docs.zepp.com/docs/reference/related-resources/device-list/
     */
    public abstract Set<Integer> deviceSources();

    @Override
    protected HuamiFirmwareType determineFirmwareType(final byte[] bytes) {
        if (ArrayUtils.equals(bytes, UIHHContainer.UIHH_HEADER, 0)) {
            return handleUihhPackage(bytes);
        } else if (ZipFile.isZipFile(bytes)) {
            return handleZipPackage(bytes);
        } else {
            return HuamiFirmwareType.INVALID;
        }
    }

    private HuamiFirmwareType handleUihhPackage(byte[] bytes) {
        final UIHHContainer uihh = UIHHContainer.fromRawBytes(bytes);
        if (uihh == null) {
            LOG.warn("Invalid UIHH file");
            return HuamiFirmwareType.INVALID;
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
            final ZipFile zipFile = new ZipFile(uihhFirmwareZipFile.getContent());
            final byte[] firmwareBin;
            try {
                firmwareBin = zipFile.getFileFromZip("META/firmware.bin");
            } catch (final ZipFileException e) {
                LOG.error("Failed to read zip from UIHH", e);
                return HuamiFirmwareType.INVALID;
            }

            if (isCompatibleFirmwareBin(firmwareBin)) {
                return HuamiFirmwareType.FIRMWARE_UIHH_2021_ZIP_WITH_CHANGELOG;
            }
        }

        if (agpsEpoTypes.size() == 3) {
            // AGPS EPO update
            return HuamiFirmwareType.AGPS_UIHH;
        }

        return HuamiFirmwareType.INVALID;
    }

    private HuamiFirmwareType handleZipPackage(byte[] bytes) {
        final ZipFile zipFile = new ZipFile(bytes);

        // Attempt to handle as a firmware
        try {
            final byte[] firmwareBin = zipFile.getFileFromZip("META/firmware.bin");
            if (isCompatibleFirmwareBin(firmwareBin)) {
                return HuamiFirmwareType.FIRMWARE;
            } else {
                return HuamiFirmwareType.INVALID;
            }
        } catch (final ZipFileException e) {
            LOG.warn("Failed to get firmware.bin from zip file", e);
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
                return HuamiFirmwareType.INVALID;
            }

            final HuamiFirmwareType huamiFirmwareType;
            final GBDeviceApp.Type gbDeviceAppType;
            switch (appType) {
                case "watchface":
                    huamiFirmwareType = HuamiFirmwareType.WATCHFACE;
                    gbDeviceAppType = GBDeviceApp.Type.WATCHFACE;
                    break;
                case "app":
                    huamiFirmwareType = HuamiFirmwareType.APP;
                    gbDeviceAppType = GBDeviceApp.Type.APP_GENERIC;
                    break;
                default:
                    LOG.warn("Unknown app type {}", appType);
                    return HuamiFirmwareType.INVALID;
            }

            Bitmap icon = null;
            try {
                final byte[] iconBytes = zipFile.getFileFromZip("assets/" + appIconPath);
                if (BitmapUtil.isPng(iconBytes)) {
                    icon = BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.length);
                } else {
                    icon = BitmapUtil.decodeTga(iconBytes);
                }
            } catch (final ZipFileException e) {
                LOG.error("Failed to get app icon from zip", e);
            }

            gbDeviceApp = new GBDeviceApp(
                    UUID.fromString(String.format("%08x-0000-0000-0000-000000000000", appId)),
                    appName,
                    appCreator,
                    appVersion,
                    gbDeviceAppType,
                    icon
            );

            return huamiFirmwareType;
        }

        // Attempt to handle as a zab file
        final byte[] zpk = handleZabPackage(zipFile);
        if (zpk != null) {
            setBytes(zpk);
            return handleZipPackage(zpk);
        }

        return HuamiFirmwareType.INVALID;
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

                    if (deviceSources().contains(platform.getInt("deviceSource"))) {
                        // It's compatible with the device, fetch device.zip
                        final String name = zpkEntry.getString("name");
                        final byte[] zpkBytes = zipFile.getFileFromZip(name);
                        if (!ZipFile.isZipFile(zpkBytes)) {
                            LOG.warn("bytes for {} not a zip file", name);
                            continue;
                        }
                        final ZipFile zpkFile = new ZipFile(zpkBytes);
                        final byte[] deviceZip = zpkFile.getFileFromZip("device.zip");
                        if (!ZipFile.isZipFile(zpkBytes)) {
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

    @Override
    public String toVersion(int crc16) {
        final String crcMapVersion = getCrcMap().get(crc16);
        if (crcMapVersion != null) {
            return crcMapVersion;
        }

        return preComputedVersion;
    }

    public String preComputeVersion() {
        try {
            switch (firmwareType) {
                case FIRMWARE_UIHH_2021_ZIP_WITH_CHANGELOG:
                    final UIHHContainer uihh = UIHHContainer.fromRawBytes(getBytes());
                    if (uihh == null) {
                        return null;
                    }
                    return getFirmwareVersion(uihh.getFile(UIHHContainer.FileType.FIRMWARE_ZIP));
                case FIRMWARE:
                    return getFirmwareVersion(getBytes());
                case WATCHFACE:
                    final String watchfaceName = getAppName(new ZipFile(getBytes()));
                    if (watchfaceName == null) {
                        return "(unknown watchface)";
                    }
                    return String.format("%s (watchface)", watchfaceName);
                case APP:
                    final String appName = getAppName(new ZipFile(getBytes()));
                    if (appName == null) {
                        return "(unknown app)";
                    }
                    return String.format("%s (app)", appName);
            }
        } catch (final Exception e) {
            LOG.error("Failed to pre compute version", e);
        }

        return null;
    }

    public GBDeviceApp getAppInfo() {
        return gbDeviceApp;
    }

    @Nullable
    @Override
    public Bitmap getPreview() {
        if (gbDeviceApp != null) {
            return gbDeviceApp.getPreviewImage();
        }

        return null;
    }

    public Huami2021FirmwareInfo repackFirmwareInUIHH() throws IOException {
        if (!firmwareType.equals(HuamiFirmwareType.FIRMWARE)) {
            throw new IllegalStateException("Can only repack FIRMWARE");
        }

        final UIHHContainer uihh = packFirmwareInUIHH(getBytes());

        try {
            final Constructor<? extends Huami2021FirmwareInfo> constructor = this.getClass().getConstructor(byte[].class);
            return constructor.newInstance((Object) uihh.toRawBytes());
        } catch (final Exception e) {
            throw new IOException("Failed to construct new " + getClass().getName(), e);
        }
    }

    private static UIHHContainer packFirmwareInUIHH(final byte[] zipBytes) {
        final UIHHContainer uihh = new UIHHContainer();
        final byte[] timestampBytes = BLETypeConversions.fromUint32((int) (System.currentTimeMillis() / 1000L));
        final String changelogText = "Unknown changelog";
        final byte[] changelogBytes = BLETypeConversions.join(
                timestampBytes,
                changelogText.getBytes(StandardCharsets.UTF_8)
        );
        uihh.addFile(UIHHContainer.FileType.FIRMWARE_ZIP, zipBytes);
        uihh.addFile(UIHHContainer.FileType.FIRMWARE_CHANGELOG, changelogBytes);
        return uihh;
    }

    private boolean isCompatibleFirmwareBin(final byte[] firmwareBin) {
        if (firmwareBin == null) {
            LOG.error("firmware bin is null");
            return false;
        }

        if (!searchString(firmwareBin, deviceName())) {
            LOG.warn("Failed to find {} in fwBytes", deviceName());
            return false;
        }

        return true;
    }

    public static String getFirmwareVersion(final byte[] fwBytes) {
        final ZipFile zipFile = new ZipFile(fwBytes);
        final byte[] firmwareBin;
        try {
            firmwareBin = zipFile.getFileFromZip("META/firmware.bin");
        } catch (final ZipFileException e) {
            LOG.error("Failed to get firmware.bin from zip", e);
            return null;
        }

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

    public String getAppName(final ZipFile zipFile) {
        // TODO check i18n section?
        // TODO Show preview icon?

        final JSONObject appJson = getJson(zipFile, "app.json");
        if (appJson == null) {
            return null;
        }

        try {
            return appJson.getJSONObject("app").getString("appName");
        } catch (final Exception e) {
            LOG.error("Failed to get appName from app.json", e);
        }

        return null;
    }

    private static JSONObject getJson(final ZipFile zipFile, final String path) {
        final byte[] appJsonBin;
        try {
            appJsonBin = zipFile.getFileFromZip(path);
        } catch (final ZipFileException e) {
            LOG.error("Failed to read " + path, e);
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
