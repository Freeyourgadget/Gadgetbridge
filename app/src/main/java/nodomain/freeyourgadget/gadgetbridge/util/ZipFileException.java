package nodomain.freeyourgadget.gadgetbridge.util;

public class ZipFileException extends Exception {
    public ZipFileException(final String message) {
        super(String.format("Error while reading ZIP file: %s", message));
    }

    public ZipFileException(final String message, final Throwable cause) {
        super(String.format("Error while reading ZIP file: %s", message), cause);
    }
}
