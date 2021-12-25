package nodomain.freeyourgadget.gadgetbridge.test;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import androidx.annotation.NonNull;

class HexToBinaryInputStream extends FilterInputStream {
    HexToBinaryInputStream(InputStream in) {
        super(in);
    }

    @Override
    public int read() throws IOException {
        int value;
        StringBuilder buffer = new StringBuilder(4);

        loop:
        while (true) {
            value = super.read();
            switch (value) {
                case -1:
                case ' ':
                case '\r':
                case '\n':
                    break loop;
                default:
                    buffer.append((char) value);
            }
        }
        if (buffer.length() > 0) {
            return Integer.decode(buffer.toString());
        }
        return -1;
    }

    @Override
    public int read(@NonNull byte[] b, int off, int len) throws IOException {
        if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        }
        for (int i = 0; i < len; i++) {
            int value = read();
            if (value == -1) {
                return i;
            }
            b[off + i] = (byte) value;
        }
        return len;
    }
}
