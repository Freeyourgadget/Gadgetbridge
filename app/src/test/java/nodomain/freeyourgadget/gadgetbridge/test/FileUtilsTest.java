package nodomain.freeyourgadget.gadgetbridge.test;

import org.junit.Test;

import java.io.File;
import java.io.IOException;

import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FileUtilsTest extends TestBase {

    @Test
    public void testValidFileName() {
        String tempName = "foo:bar";
        assertEquals("foo_bar", FileUtils.makeValidFileName(tempName));

        tempName = "fo\no::bar";
        assertEquals("fo_o__bar", FileUtils.makeValidFileName(tempName));
    }

    @Test
    public void testExternalFilesDir() throws IOException {
        File dir = FileUtils.getExternalFilesDir();
        assertTrue(dir.exists());
    }

    @Test
    public void testExternalFile() throws IOException {
        File extDir = FileUtils.getExternalFilesDir();
        File dir = FileUtils.getExternalFile("qfiles");
        assertFalse(dir.exists());

        File file = FileUtils.getExternalFile("qfiles/test");
        assertTrue(file.getParentFile().exists());
        assertFalse(file.exists());

        File expectedFile = new File(extDir + "/qfiles/test");
        assertEquals(expectedFile.getPath(), file.getPath());
    }
}
