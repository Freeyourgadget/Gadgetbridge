package nodomain.freeyourgadget.gadgetbridge.util;

import org.bouncycastle.shaded.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


public class GBTarFile {
    private static final Logger LOG = LoggerFactory.getLogger(GBTarFile.class);
    private final byte[] tarBytes;
    public static final int TAR_MAGIC_BYTES_OFFSET = 257;
    public static final byte[] TAR_MAGIC_BYTES = new byte[]{
            'u', 's', 't', 'a', 'r', '\0'
    };
    public static final int TAR_BLOCK_SIZE = 512;
    public static final int TAR_HEADER_FILE_NAME_OFFSET = 0;
    public static final int TAR_HEADER_FILE_NAME_LENGTH = 100;
    public static final int TAR_HEADER_FILE_SIZE_OFFSET = 124;
    public static final int TAR_HEADER_FILE_SIZE_LENGTH = 12;


    public GBTarFile(byte[] tarBytes) {
        this.tarBytes = tarBytes;
    }

    public static boolean isTarFile(byte[] data) {
        return ArrayUtils.equals(data, TAR_MAGIC_BYTES, TAR_MAGIC_BYTES_OFFSET);
    }

    public List<String> listFileNames() {
        final List<String> fileNames = new ArrayList<>();
        for (TarHeader header: listHeaders()) {
            fileNames.add(header.fileName);
        }
        return fileNames;
    }

    public boolean containsFile(String fileName) {
        for (TarHeader header: listHeaders()) {
            if (fileName.equals(header.fileName)) {
                return true;
            }
        }
        return false;
    }

    private List<TarHeader> listHeaders() {
        final List<TarHeader> headers = new ArrayList<>();
        int offset = 0;
        while (ArrayUtils.equals(tarBytes, TAR_MAGIC_BYTES, offset + TAR_MAGIC_BYTES_OFFSET)) {
            final TarHeader tarHeader = new TarHeader(Arrays.copyOfRange(tarBytes, offset, offset + TAR_BLOCK_SIZE));
            headers.add(tarHeader);
            offset += (((tarHeader.fileSize + TAR_BLOCK_SIZE - 1) / TAR_BLOCK_SIZE) + 1) * TAR_BLOCK_SIZE;
        }
        return headers;
    }

    private static class TarHeader {
        final String fileName;
        final int fileSize;

        public TarHeader(byte[] header) {
            fileName = parseString(header, TAR_HEADER_FILE_NAME_OFFSET, TAR_HEADER_FILE_NAME_LENGTH);
            fileSize = Integer.parseInt(parseString(header, TAR_HEADER_FILE_SIZE_OFFSET, TAR_HEADER_FILE_SIZE_LENGTH).trim(), 8);
        }

        private static String parseString(final byte[] data, final int offset, final int maxLength) {
            int length = 0;
            while (length < maxLength && offset + length < data.length && data[offset + length] != 0) {
                length++;
            }
            return new String(data, offset, length, StandardCharsets.US_ASCII);
        }
    }
}
