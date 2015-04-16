package nodomain.freeyourgadget.gadgetbridge.pebble;

public class PebbleInstallable {
    final private byte type;
    final private int crc;
    final private String fileName;
    final private int fileSize;

    public PebbleInstallable(String fileName, int fileSize, int crc, byte type) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.crc = crc;
        this.type = type;
    }

    public String getFileName() {
        return fileName;
    }

    public int getFileSize() {
        return fileSize;
    }

    public byte getType() {
        return type;
    }

    public int getCRC() {
        return crc;
    }
}
