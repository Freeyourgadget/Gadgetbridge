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

//@Ignore("Disabled for travis -- needs vm parameter -DMiFirmwareDir=/path/to/firmware/directory/")
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

        // Mi Band version
        int calculatedLengthFw1 = info.getFirst().getFirmwareLength();
        int calculatedOffsetFw1 = info.getFirst().getFirmwareOffset();
        int endIndexFw1 = calculatedOffsetFw1 + calculatedLengthFw1;

        int calculatedLengthFw2 = info.getSecond().getFirmwareLength();
        int calculatedOffsetFw2 = info.getSecond().getFirmwareOffset();
        int endIndexFw2 = calculatedOffsetFw2 + calculatedLengthFw2;

        Assert.assertTrue(endIndexFw1 <= wholeFw.length - calculatedLengthFw2);
        Assert.assertTrue(endIndexFw2 <= wholeFw.length);

        Assert.assertTrue(endIndexFw1 <= calculatedOffsetFw2);
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

        Assert.assertNotEquals(info.getFirst().getFirmwareOffset(), info.getSecond().getFirmwareOffset());
        Assert.assertFalse(Arrays.equals(info.getFirst().getFirmwareBytes(), info.getSecond().getFirmwareBytes()));
    }

    private AbstractMiFirmwareInfo getFirmwareInfo(byte[] wholeFw, int numFirmwares) {
        AbstractMiFirmwareInfo info = AbstractMiFirmwareInfo.determineFirmwareInfoFor(wholeFw);
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
        Assert.assertTrue("System property MiFirmwareDir should point to a directory continaing the Mi Band firmware files", dir.isDirectory());
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
