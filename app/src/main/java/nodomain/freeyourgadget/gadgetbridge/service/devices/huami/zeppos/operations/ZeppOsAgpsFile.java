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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.operations;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.UIHHContainer;
import nodomain.freeyourgadget.gadgetbridge.util.ZipFile;
import nodomain.freeyourgadget.gadgetbridge.util.ZipFileException;

public class ZeppOsAgpsFile {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsAgpsFile.class);

    private final byte[] zipBytes;

    public ZeppOsAgpsFile(final byte[] zipBytes) {
        this.zipBytes = zipBytes;
    }

    public boolean isValid() {
        if (!ZipFile.isZipFile(zipBytes)) {
            return false;
        }

        final ZipFile zipFile = new ZipFile(zipBytes);

        try {
            final byte[] manifestBin = zipFile.getFileFromZip("META-INF/MANIFEST.MF");
            if (manifestBin == null) {
                LOG.warn("Failed to get MANIFEST from zip");
                return false;
            }

            final String appJsonString = new String(manifestBin, StandardCharsets.UTF_8)
                    // Remove UTF-8 BOM if present
                    .replace("\uFEFF", "");
            final JSONObject jsonObject = new JSONObject(appJsonString);
            return jsonObject.getString("manifestVersion").equals("2.0") &&
                    zipFile.fileExists("EPO_BDS_3.DAT") &&
                    zipFile.fileExists("EPO_GAL_7.DAT") &&
                    zipFile.fileExists("EPO_GR_3.DAT");
        } catch (final Exception e) {
            LOG.error("Failed to parse read MANIFEST or check file", e);
        }

        return false;
    }

    public byte[] getUihhBytes() {
        final UIHHContainer uihh = new UIHHContainer();

        final ZipFile zipFile = new ZipFile(zipBytes);

        try {
            uihh.addFile(UIHHContainer.FileType.AGPS_EPO_GR_3, zipFile.getFileFromZip("EPO_GR_3.DAT"));
            uihh.addFile(UIHHContainer.FileType.AGPS_EPO_GAL_7, zipFile.getFileFromZip("EPO_GAL_7.DAT"));
            uihh.addFile(UIHHContainer.FileType.AGPS_EPO_BDS_3, zipFile.getFileFromZip("EPO_BDS_3.DAT"));
        } catch (final ZipFileException e) {
            throw new IllegalStateException("Failed to read file from zip", e);
        }

        return uihh.toRawBytes();
    }
}
