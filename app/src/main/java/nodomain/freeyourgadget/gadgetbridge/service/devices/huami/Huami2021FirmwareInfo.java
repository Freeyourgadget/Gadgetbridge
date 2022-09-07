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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;


public abstract class Huami2021FirmwareInfo extends AbstractHuamiFirmwareInfo {
    private static final Logger LOG = LoggerFactory.getLogger(Huami2021FirmwareInfo.class);

    public static final byte[] ZIP_HEADER = new byte[]{
            0x50, 0x4B, 0x03, 0x04
    };

    public Huami2021FirmwareInfo(final byte[] bytes) {
        super(bytes);
    }

    /**
     * The device name, to search on firmware.bin in order to determine compatibility.
     */
    public abstract String deviceName();

    /**
     * The expected firmware header bytes, to search on firmware.bin in order to determine compatibility.
     */
    public abstract byte[] getExpectedFirmwareHeader();

    @Override
    protected HuamiFirmwareType determineFirmwareType(final byte[] bytes) {
        if (ArrayUtils.equals(bytes, UIHHContainer.UIHH_HEADER, 0)) {
            final UIHHContainer uihh = UIHHContainer.fromRawBytes(bytes);
            if (uihh == null) {
                LOG.warn("Invalid UIHH file");
                return HuamiFirmwareType.INVALID;
            }

            UIHHContainer.FileEntry uihhFirmwareZipFile = null;
            boolean hasChangelog = false;
            for (final UIHHContainer.FileEntry file : uihh.getFiles()) {
                switch(file.getType()) {
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
                final byte[] firmwareBin = getFileFromZip(uihhFirmwareZipFile.getContent(), "META/firmware.bin");

                if (isCompatibleFirmwareBin(firmwareBin)) {
                    // TODO: Firmware upgrades with UIHH files are untested, so they are disabled
                    return HuamiFirmwareType.INVALID;
                    //return HuamiFirmwareType.FIRMWARE_UIHH_2021_ZIP_WITH_CHANGELOG;
                }
            }

            return HuamiFirmwareType.INVALID;
        }

        if (!ArrayUtils.equals(bytes, ZIP_HEADER, 0)) {
            return HuamiFirmwareType.INVALID;
        }

        final byte[] firmwareBin = getFileFromZip(bytes, "META/firmware.bin");
        if (isCompatibleFirmwareBin(firmwareBin)) {
            return HuamiFirmwareType.FIRMWARE;
        }

        final String appType = getAppType();
        switch(appType) {
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
                final String watchfaceName = getAppName();
                if (watchfaceName == null) {
                    return "(unknown watchface)";
                }
                return String.format("%s (watchface)", watchfaceName);
            case APP:
                final String appName = getAppName();
                if (appName == null) {
                    return "(unknown app)";
                }
                return String.format("%s (app)", appName);
        }

        return null;
    }

    private boolean isCompatibleFirmwareBin(final byte[] firmwareBin) {
        if (firmwareBin == null) {
            return false;
        }

        if (!ArrayUtils.equals(firmwareBin, getExpectedFirmwareHeader(), 0)) {
            LOG.warn("Unexpected firmware header: {}", GB.hexdump(Arrays.copyOfRange(firmwareBin, 0, getExpectedFirmwareHeader().length + 3)));
            return false;
        }

        // On the MB7, this only works for firmwares > 1.8.5.1, not for any older firmware
        if (!searchString32BitAligned(firmwareBin, deviceName())) {
            LOG.warn("Failed to find {} in fwBytes", deviceName());
            return false;
        }

        return true;
    }

    public String getFirmwareVersion(final byte[] fwbytes) {
        final byte[] firmwareBin = getFileFromZip(fwbytes, "META/firmware.bin");

        if (firmwareBin == null) {
            LOG.warn("Failed to read firmware.bin");
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

    public String getAppName() {
        final byte[] appJsonBin = getFileFromZip(getBytes(), "app.json");
        if (appJsonBin == null) {
            LOG.warn("Failed to get app.json from zip");
            return null;
        }

        try {
            final String appJsonString = new String(appJsonBin, StandardCharsets.UTF_8)
                    // Remove UTF-8 BOM if present
                    .replace("\uFEFF", "");
            final JSONObject jsonObject = new JSONObject(appJsonString);
            // TODO check i18n section?
            // TODO Show preview icon?
            final String appName = jsonObject.getJSONObject("app").getString("appName");

            return String.format("%s", appName);
        } catch (final Exception e) {
            LOG.error("Failed to parse app.json", e);
        }

        return null;
    }

    public String getAppType() {
        final byte[] appJsonBin = getFileFromZip(getBytes(), "app.json");
        if (appJsonBin == null) {
            LOG.warn("Failed to get app.json from zip");
            return null;
        }

        try {
            final String appJsonString = new String(appJsonBin, StandardCharsets.UTF_8)
                    // Remove UTF-8 BOM if present
                    .replace("\uFEFF", "");
            final JSONObject jsonObject = new JSONObject(appJsonString);
            return jsonObject.getJSONObject("app").getString("appType");
        } catch (final Exception e) {
            LOG.error("Failed to parse app.json", e);
        }

        return null;
    }

    public static byte[] getFileFromZip(final byte[] zipBytes, final String path) {
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (zipEntry.getName().equals(path)) {
                    return readAllBytes(zipInputStream);
                }
            }
        } catch (final IOException e) {
            LOG.error(String.format("Failed to read %s from zip", path), e);
            return null;
        }

        LOG.debug("{} not found in zip", path);

        return null;
    }

    public static byte[] readAllBytes(final InputStream is) throws IOException {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int n;
        byte[] buf = new byte[16384];

        while ((n = is.read(buf, 0, buf.length)) != -1) {
            buffer.write(buf, 0, n);
        }

        return buffer.toByteArray();
    }
}
