package nodomain.freeyourgadget.gadgetbridge.service.devices.miband;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandFWHelper;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;

// while read i; do ./gradlew --daemon -a --offline  -DMiFirmwareDir=$i  test; if test $? != 0; then echo "Failure in $i" && break; fi; done < fw.dirs
@Ignore("Disabled for travis -- needs vm parameter -DMiFirmwareDir=/path/to/firmware/directory/")
public class FirmwareTest {

    private static final long MAX_FILE_SIZE_BYTES = 1024 * 1024; // 1MB
    private static final int MI_FW_VERSION = 0; // FIXME
    private static final int MI1A_FW_VERSION = 0; // FIXME
    private static final int MI1S_FW1_VERSION = 0;
    private static final int MI1S_FW2_VERSION = 0;

    private static final int SINGLE = 1;
    private static final int DOUBLE = 2;

    @BeforeClass
    public static void setupSuite() {
        getFirmwareDir(); // throws if firmware directory not available
    }

    @Test
    public void testFirmwareMi1() throws Exception {
        byte[] wholeFw = getFirmwareMi();
        Assert.assertNotNull(wholeFw);

        AbstractMiFirmwareInfo info = getFirmwareInfo(wholeFw, SINGLE);
        info.checkValid();

        int calculatedVersion = info.getFirmwareVersion();
        String version = MiBandFWHelper.formatFirmwareVersion(calculatedVersion);
        Assert.assertTrue(version.startsWith("1."));
        Assert.assertArrayEquals(wholeFw, info.getFirmwareBytes());
//        Assert.assertEquals("Unexpected firmware version: " + calculatedVersion, MI_FW_VERSION, calculatedVersion);
    }

    @Test
    public void testFirmwareMi1A() throws Exception {
        byte[] wholeFw = getFirmwareMi1A();
        Assert.assertNotNull(wholeFw);

        AbstractMiFirmwareInfo info = getFirmwareInfo(wholeFw, SINGLE);
        info.checkValid();

        int calculatedVersion = info.getFirmwareVersion();
        String version = MiBandFWHelper.formatFirmwareVersion(calculatedVersion);
        Assert.assertTrue(version.startsWith("5."));
        Assert.assertArrayEquals(wholeFw, info.getFirmwareBytes());
//        Assert.assertEquals("Unexpected firmware version: " + calculatedVersion, MI1A_FW_VERSION, calculatedVersion);
    }

    @Test
    public void testFirmwareMi1S() throws Exception {
        byte[] wholeFw = getFirmwareMi1S();
        Assert.assertNotNull(wholeFw);

        AbstractMiFirmwareInfo info = getFirmwareInfo(wholeFw, DOUBLE);
        info.checkValid();

        // Mi Band version
        int calculatedVersionFw1 = info.getFirst().getFirmwareVersion();
//        Assert.assertEquals("Unexpected firmware 1 version: " + calculatedVersionFw1, MI1S_FW1_VERSION, calculatedVersionFw1);
        String version1 = MiBandFWHelper.formatFirmwareVersion(calculatedVersionFw1);
        Assert.assertTrue(version1.startsWith("4."));

        // HR version
        int calculatedVersionFw2 = info.getSecond().getFirmwareVersion();
//        Assert.assertEquals("Unexpected firmware 2 version: " + calculatedVersionFw2, MI1S_FW2_VERSION, calculatedVersionFw2);
        String version2 = MiBandFWHelper.formatFirmwareVersion(calculatedVersionFw2);
        Assert.assertTrue(version2.startsWith("1."));

        try {
            info.getFirmwareVersion();
            Assert.fail("should not get fw version from AbstractMi1SFirmwareInfo");
        } catch (UnsupportedOperationException expected) {
        }

        Assert.assertFalse(Arrays.equals(info.getFirst().getFirmwareBytes(), info.getSecond().getFirmwareBytes()));
    }

    @Test
    public void testDoubleFirmwareMi1A() throws Exception {
        byte[] wholeFw = getFirmwareMi1A();
        Assert.assertNotNull(wholeFw);

        AbstractMiFirmwareInfo info = TestMi1AFirmwareInfo.getInstance(wholeFw);
        Assert.assertNotNull(info);
        info.checkValid();

        // Mi Band version
        int calculatedVersionFw1 = info.getFirst().getFirmwareVersion();
//        Assert.assertEquals("Unexpected firmware 1 version: " + calculatedVersionFw1, MI1S_FW1_VERSION, calculatedVersionFw1);
        String version1 = MiBandFWHelper.formatFirmwareVersion(calculatedVersionFw1);
        Assert.assertTrue(version1.startsWith("5."));

        // Same Mi Band version
        int calculatedVersionFw2 = info.getSecond().getFirmwareVersion();
//        Assert.assertEquals("Unexpected firmware 2 version: " + calculatedVersionFw2, MI1S_FW2_VERSION, calculatedVersionFw2);
        String version2 = MiBandFWHelper.formatFirmwareVersion(calculatedVersionFw2);
        Assert.assertTrue(version2.startsWith("5."));

        try {
            info.getFirmwareVersion();
            Assert.fail("should not get fw version from AbstractMi1SFirmwareInfo");
        } catch (UnsupportedOperationException expected) {
        }

        // these are actually the same with this test info!
        Assert.assertEquals(info.getFirst().getFirmwareOffset(), info.getSecond().getFirmwareOffset());
        Assert.assertTrue(Arrays.equals(info.getFirst().getFirmwareBytes(), info.getSecond().getFirmwareBytes()));
    }

    private AbstractMiFirmwareInfo getFirmwareInfo(byte[] wholeFw, int numFirmwares) {
        AbstractMiFirmwareInfo info = AbstractMiFirmwareInfo.determineFirmwareInfoFor(wholeFw);
        assertFirmwareInfo(info, wholeFw, numFirmwares);
        return info;
    }

    private AbstractMiFirmwareInfo assertFirmwareInfo(AbstractMiFirmwareInfo info, byte[] wholeFw, int numFirmwares) {
        switch (numFirmwares) {
            case SINGLE: {
                Assert.assertTrue("should be single miband firmware", info.isSingleMiBandFirmware());
                Assert.assertSame(info, info.getFirst());
                try {
                    info.getSecond();
                    Assert.fail("should throw UnsuportedOperationException");
                } catch (UnsupportedOperationException expected) {
                }
                int calculatedLength = info.getFirmwareLength();
                Assert.assertTrue("Unexpected firmware length: " + wholeFw.length, calculatedLength <= wholeFw.length);
                break;
            }
            case DOUBLE: {
                Assert.assertFalse("should not be single miband firmware", info.isSingleMiBandFirmware());
                Assert.assertNotSame(info, info.getFirst());
                Assert.assertNotSame(info, info.getSecond());
                Assert.assertNotSame(info.getFirst(), info.getSecond());
                int calculatedLength = info.getFirst().getFirmwareLength();
                Assert.assertTrue("Unexpected firmware length: " + wholeFw.length, calculatedLength <= wholeFw.length);
                calculatedLength = info.getSecond().getFirmwareLength();
                Assert.assertTrue("Unexpected firmware length: " + wholeFw.length, calculatedLength <= wholeFw.length);
                break;
            }
            default:
                Assert.fail("unexpected numFirmwares: " + numFirmwares);
        }
        Assert.assertTrue(info.isGenerallySupportedFirmware());
        return info;
    }

    private static File getFirmwareDir() {
        String path = System.getProperty("MiFirmwareDir");
        Assert.assertNotNull("You must run this test with -DMiFirmwareDir=/path/to/directory/with/miband/firmwarefiles/", path);
        File dir = new File(path);
        Assert.assertTrue("System property MiFirmwareDir should point to a directory containing the Mi Band firmware files", dir.isDirectory());
        return dir;
    }

    private byte[] getFirmwareMi() throws IOException {
        return getFirmware(new File(getFirmwareDir(), "Mili.fw"));
    }

    private byte[] getFirmwareMi1A() throws IOException {
        return getFirmware(new File(getFirmwareDir(), "Mili_1a.fw"));
    }

    private byte[] getFirmwareMi1S() throws IOException {
        return getFirmware(new File(getFirmwareDir(), "Mili_hr.fw"));
    }

    private byte[] getFirmware(File aFile) throws IOException {
        Assert.assertNotNull(aFile);
        try (FileInputStream stream = new FileInputStream(aFile)) {
            return FileUtils.readAll(stream, MAX_FILE_SIZE_BYTES);
        }
    }

}
