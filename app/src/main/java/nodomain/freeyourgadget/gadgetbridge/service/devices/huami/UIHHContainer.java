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

import androidx.annotation.Nullable;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.util.CheckSums;

public class UIHHContainer {
    private static final Logger LOG = LoggerFactory.getLogger(UIHHContainer.class);

    public static final byte[] UIHH_HEADER = new byte[]{
            'U', 'I', 'H', 'H'
    };

    public List<FileEntry> files = new ArrayList<>();

    public List<FileEntry> getFiles() {
        return files;
    }

    public byte[] getFile(final FileType fileType) {
        for (final FileEntry file : files) {
            if (file.getType() == fileType) {
                return file.getContent();
            }
        }

        return null;
    }

    public void addFile(final FileType type, final byte[] bytes) {
        files.add(new FileEntry(type, bytes));
    }

    @Nullable
    public byte[] toRawBytes() {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        for (final FileEntry file : files) {
            try {
                baos.write(file.buildHeader());
                baos.write(file.getContent());
            } catch (final IOException e) {
                LOG.error("Failed to generate UIHH bytes", e);
                return null;
            }
        }

        final byte[] contentBytes = baos.toByteArray();
        final byte[] headerBytes = buildHeader(contentBytes);

        return ArrayUtils.addAll(headerBytes, contentBytes);
    }

    @Nullable
    public static UIHHContainer fromRawBytes(final byte[] bytes) {
        if (bytes.length < 32) {
            LOG.error("bytes array too small {}", bytes.length);
            return null;
        }

        if (!nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils.startsWith(bytes, UIHH_HEADER)) {
            LOG.error("UIHH header not found");
            return null;
        }

        final int crc32 = BLETypeConversions.toUint32(ArrayUtils.subarray(bytes, 12, 12 + 4));
        final int length = BLETypeConversions.toUint32(ArrayUtils.subarray(bytes, 22, 22 + 4));

        if (length + 32 != bytes.length) {
            LOG.error("Length mismatch between header and bytes: {}/{}", length, bytes.length);
            return null;
        }

        if (crc32 != CheckSums.getCRC32(ArrayUtils.subarray(bytes, 32, bytes.length))) {
            LOG.error("CRC mismatch for content");
            return null;
        }

        int i = 32;

        final UIHHContainer ret = new UIHHContainer();

        while (i < bytes.length) {
            if (i + 10 >= bytes.length) {
                LOG.error("Not enough bytes remaining");
                return null;
            }

            if (bytes[i] != 1) {
                LOG.error("Expected 1 at position {}", i);
                return null;
            }
            i++;

            final FileType type = FileType.fromValue(bytes[i]);
            if (type == null) {
                LOG.error("Unknown type byte {} at position {}", String.format("0x%x", bytes[i], i));
                return null;
            }
            i++;

            final int fileLength = BLETypeConversions.toUint32(ArrayUtils.subarray(bytes, i, i + 4));
            i += 4;
            final int fileCrc32 = BLETypeConversions.toUint32(ArrayUtils.subarray(bytes, i, i + 4));
            i += 4;

            if (i + fileLength > bytes.length) {
                LOG.error("Not enough bytes remaining to read a {} byte file", fileLength);
                return null;
            }

            final byte[] fileContent = ArrayUtils.subarray(bytes, i, i + fileLength);
            if (fileCrc32 != CheckSums.getCRC32(fileContent)) {
                LOG.error("CRC mismatch for {}", type);
                return null;
            }
            i += fileLength;

            ret.getFiles().add(new FileEntry(type, fileContent));
        }

        return ret;
    }

    private static byte[] buildHeader(final byte[] content) {
        final ByteBuffer buf = ByteBuffer.allocate(32);
        buf.order(ByteOrder.LITTLE_ENDIAN);

        buf.put(UIHH_HEADER);
        buf.put(new byte[]{0x04, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01});
        buf.putInt(CheckSums.getCRC32(content));
        buf.put(new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
        buf.putInt(content.length);
        buf.put(new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00});

        return buf.array();
    }

    public enum FileType {
        FIRMWARE_ZIP(HuamiFirmwareType.FIRMWARE.getValue(), "firmware.zip"),
        FIRMWARE_CHANGELOG(0x10, "changelog.txt"),
        GPS_ALM_BIN(HuamiFirmwareType.GPS_ALMANAC.getValue(), "gps_alm.bin"),
        GLN_ALM_BIN(0x0f, "gln_alm.bin"),
        LLE_BDS_LLE(0x86, "lle_bds.lle"),
        LLE_GPS_LLE(0x87, "lle_gps.lle"),
        LLE_GLO_LLE(0x88, "lle_glo.lle"),
        LLE_GAL_LLE(0x89, "lle_gal.lle"),
        LLE_QZSS_LLE(0x8a, "lle_qzss.lle"),
        ;

        private final byte value;
        private final String name;

        FileType(final int value, final String name) {
            this.value = (byte) value;
            this.name = name;
        }

        public byte getValue() {
            return value;
        }

        public static FileType fromValue(final byte value) {
            for (final FileType fileType : values()) {
                if (fileType.getValue() == value) {
                    return fileType;
                }
            }

            return null;
        }
    }

    public static class FileEntry {
        private final FileType type;
        private final byte[] content;

        public FileEntry(final FileType type, final byte[] content) {
            this.type = type;
            this.content = content;
        }

        public FileType getType() {
            return type;
        }

        public byte[] getContent() {
            return content;
        }

        public byte[] buildHeader() {
            final ByteBuffer buf = ByteBuffer.allocate(10);
            buf.order(ByteOrder.LITTLE_ENDIAN);

            buf.put((byte) 0x01);
            buf.put(type.getValue());
            buf.putInt(content.length);
            buf.putInt(CheckSums.getCRC32(content));

            return buf.array();
        }
    }
}
