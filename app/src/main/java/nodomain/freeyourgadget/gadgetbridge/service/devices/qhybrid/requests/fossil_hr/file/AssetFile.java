package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.file;

import java.nio.ByteBuffer;
import java.util.zip.CRC32;

import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;


public class AssetFile {
    private String fileName;
    private byte[] fileData;

    protected AssetFile(String fileName, byte[] fileData) {
        this.fileName = fileName;
        this.fileData = fileData;
    }

    public AssetFile(byte[] fileData) {
        CRC32 crc = new CRC32();
        crc.update(fileData);

        this.fileName = StringUtils.bytesToHex(
                ByteBuffer.allocate(4)
                .putInt((int) crc.getValue())
                .array()
        );

        this.fileData = fileData;
    }

    public String getFileName() {
        return fileName;
    }

    public byte[] getFileData() {
        return fileData;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
