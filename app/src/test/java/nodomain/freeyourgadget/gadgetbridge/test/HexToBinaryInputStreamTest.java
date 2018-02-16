package nodomain.freeyourgadget.gadgetbridge.test;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;

import static org.junit.Assert.assertTrue;

public class HexToBinaryInputStreamTest extends TestBase {

    @Test
    public void testConversion() throws IOException {
        byte[] hexString;
        byte[] binString;

        try (InputStream in = ActivityDetailsParserTest.class.getClassLoader().getResourceAsStream("ActivityDetailsDump1.txt")) {
            hexString = FileUtils.readAll(in, 1024 * 1024);
            assertTrue(hexString.length > 1);
            try (InputStream in2 = getContents(ActivityDetailsParserTest.class.getClassLoader().getResource("ActivityDetailsDump1.txt"))) {
                binString = FileUtils.readAll(in2, 1024 * 1024);
                assertTrue(binString.length > 1);
            }
        }
        Assert.assertTrue(hexString.length > binString.length);
        ByteArrayOutputStream binToHexOut = new ByteArrayOutputStream(hexString.length);
        for (int i = 0; i < binString.length; i++) {
            String hexed = String.format("0x%x", binString[i]);
            binToHexOut.write(hexed.getBytes("US-ASCII"));
            if ((i + 1) % 17 == 0) {
                binToHexOut.write('\n');
            } else {
                binToHexOut.write(' ');
            }
        }

        byte[] hexedBytes = binToHexOut.toByteArray();
        Assert.assertArrayEquals(hexString, hexedBytes);
    }

    private InputStream getContents(URL hexFile) throws IOException {
        return new HexToBinaryInputStream(hexFile.openStream());
    }

}
