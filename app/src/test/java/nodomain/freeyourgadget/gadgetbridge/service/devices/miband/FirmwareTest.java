package nodomain.freeyourgadget.gadgetbridge.service.devices.miband;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandFWHelper;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;

public class FirmwareTest {

    private static final long MAX_FILE_SIZE_BYTES = 1024 * 1024; // 1MB
    private static final int MI_FW_VERSION = 0; // FIXME
    private static final int MI1A_FW_VERSION = 0; // FIXME
    private static final int MI1S_FW1_VERSION = 0;
    private static final int MI1S_FW2_VERSION = 0;

    @Test
    public void testFirmwareMi() throws Exception{
        byte[] wholeFw = getFirmwareMi();
        Assert.assertNotNull(wholeFw);

        Assert.assertTrue(Mi1SInfo.isSingleMiBandFirmware(wholeFw));
        int calculatedLength = Mi1SInfo.getFirmware1LengthIn(wholeFw);
        Assert.assertTrue("Unexpected firmware length: " + wholeFw.length, calculatedLength < wholeFw.length);
        int calculatedVersion = Mi1SInfo.getFirmware1VersionFrom(wholeFw);
//        Assert.assertEquals("Unexpected firmware version: " + calculatedVersion, MI_FW_VERSION, calculatedVersion);

        String version = MiBandFWHelper.formatFirmwareVersion(calculatedVersion);
        Assert.assertTrue(version.startsWith("1."));
    }

    @Test
    public void testFirmwareMi1A() throws Exception{
        byte[] wholeFw = getFirmwareMi1A();
        Assert.assertNotNull(wholeFw);

        Assert.assertTrue(Mi1SInfo.isSingleMiBandFirmware(wholeFw));
        int calculatedLength = Mi1SInfo.getFirmware1LengthIn(wholeFw);
        Assert.assertTrue("Unexpected firmware length: " + wholeFw.length, calculatedLength < wholeFw.length);
        int calculatedVersion = Mi1SInfo.getFirmware1VersionFrom(wholeFw);
//        Assert.assertEquals("Unexpected firmware version: " + calculatedVersion, MI1A_FW_VERSION, calculatedVersion);

        String version = MiBandFWHelper.formatFirmwareVersion(calculatedVersion);
        Assert.assertTrue(version.startsWith("5."));
    }

    @Test
    public void testFirmwareMi1S() throws Exception{
        byte[] wholeFw = getFirmwareMi1S();
        Assert.assertNotNull(wholeFw);

        Assert.assertFalse(Mi1SInfo.isSingleMiBandFirmware(wholeFw));

        // Mi Band version
        int calculatedLengthFw1 = Mi1SInfo.getFirmware1LengthIn(wholeFw);
        int calculatedOffsetFw1 = Mi1SInfo.getFirmware1OffsetIn(wholeFw);
        int endIndexFw1 = calculatedOffsetFw1 + calculatedLengthFw1;

        int calculatedLengthFw2 = Mi1SInfo.getFirmware2LengthIn(wholeFw);
        int calculatedOffsetFw2 = Mi1SInfo.getFirmware2OffsetIn(wholeFw);
        int endIndexFw2 = calculatedOffsetFw2 + calculatedLengthFw2;

        Assert.assertTrue(endIndexFw1 <= wholeFw.length - calculatedLengthFw2);
        Assert.assertTrue(endIndexFw2 <= wholeFw.length);

        Assert.assertTrue(endIndexFw1 <= calculatedOffsetFw2);
        int calculatedVersionFw1 = Mi1SInfo.getFirmware1VersionFrom(wholeFw);
//        Assert.assertEquals("Unexpected firmware 1 version: " + calculatedVersionFw1, MI1S_FW1_VERSION, calculatedVersionFw1);
        String version1 = MiBandFWHelper.formatFirmwareVersion(calculatedVersionFw1);
        Assert.assertTrue(version1.startsWith("4."));

        // HR version
        int calculatedVersionFw2 = Mi1SInfo.getFirmware2VersionFrom(wholeFw);
//        Assert.assertEquals("Unexpected firmware 2 version: " + calculatedVersionFw2, MI1S_FW2_VERSION, calculatedVersionFw2);
        String version2 = MiBandFWHelper.formatFirmwareVersion(calculatedVersionFw2);
        Assert.assertTrue(version2.startsWith("1."));
    }

    private File getFirmwareDir() {
        String path = System.getProperty("MiFirmwareDir");
        Assert.assertNotNull(path);
        File dir = new File(path);
        Assert.assertTrue(dir.isDirectory());
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
