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

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils;
import nodomain.freeyourgadget.gadgetbridge.util.ZipFile;
import nodomain.freeyourgadget.gadgetbridge.util.ZipFileException;


public abstract class Huami2021FirmwareInfo extends AbstractHuamiFirmwareInfo {
    private static final Logger LOG = LoggerFactory.getLogger(Huami2021FirmwareInfo.class);

    private final String preComputedVersion;

    public Huami2021FirmwareInfo(final byte[] bytes) {
        super(bytes);
        this.preComputedVersion = preComputeVersion();
    }

    /**
     * The device name, to search on firmware.bin in order to determine compatibility.
     */
    public abstract String deviceName();

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
                default:
                    LOG.warn("Unexpected file for {}", file.getType());
            }
        }

        if (uihhFirmwareZipFile != null && hasChangelog) {
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
        final JSONObject appJson = getAppJson(zipFile);
        if (appJson == null) {
            return HuamiFirmwareType.INVALID;
        }

        final String appType;
        try {
            appType = appJson.getJSONObject("app").getString("appType");
        } catch (final Exception e) {
            LOG.error("Failed to get appType from app.json", e);
            return HuamiFirmwareType.INVALID;
        }

        switch (appType) {
            case "watchface":
                return HuamiFirmwareType.WATCHFACE;
            case "app":
                return HuamiFirmwareType.APP;
            default:
                LOG.warn("Unknown app type {}", appType);
        }

        return HuamiFirmwareType.INVALID;
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

        final JSONObject appJson = getAppJson(zipFile);
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

    private static JSONObject getAppJson(final ZipFile zipFile) {
        final byte[] appJsonBin;
        try {
            appJsonBin = zipFile.getFileFromZip("app.json");
        } catch (final ZipFileException e) {
            LOG.error("Failed to read app.json", e);
            return null;
        }

        try {
            final String appJsonString = new String(appJsonBin, StandardCharsets.UTF_8)
                    // Remove UTF-8 BOM if present
                    .replace("\uFEFF", "");
            return new JSONObject(appJsonString);
        } catch (final Exception e) {
            LOG.error("Failed to parse app.json", e);
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
