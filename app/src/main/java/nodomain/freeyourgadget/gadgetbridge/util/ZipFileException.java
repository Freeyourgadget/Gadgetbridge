package nodomain.freeyourgadget.gadgetbridge.util;

public class ZipFileException extends Exception {
    public ZipFileException(String message) {
        super(String.format("Error while reading ZIP file: %s", message));
    }
}
