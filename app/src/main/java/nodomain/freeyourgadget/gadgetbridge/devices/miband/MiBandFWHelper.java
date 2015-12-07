package nodomain.freeyourgadget.gadgetbridge.devices.miband;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;

public class MiBandFWHelper {
    private static final Logger LOG = LoggerFactory.getLogger(MiBandFWHelper.class);

    private final Uri uri;
    private final ContentResolver cr;
    private byte[] fw;

    private final int offsetFirmwareVersionBuild = 1056;
    private final int offsetFirmwareVersionRevision = 1057;
    private final int offsetFirmwareVersionMinor = 1058;
    private final int offsetFirmwareVersionMajor = 1059;

    /**
     * Provides a different notification API which is also used on Mi1A devices.
     */
    public static final int FW_16779790 = 16779790;

    private final int[] whitelistedFirmwareVersion = {
            16779534, // 1.0.9.14 tested by developer
            16779547,  //1.0.9.27 tested by developer
            16779568, //1.0.9.48 tested by developer
            16779585, //1.0.9.65 tested by developer
            16779779, //1.0.10.3 reported on the wiki
            16779782, //1.0.10.6 reported on the wikiew
            16779787, //1.0.10.11 tested by developer
            //FW_16779790, //1.0.10.14 reported on the wiki (vibration does not work currently)
    };

    public MiBandFWHelper(Uri uri, Context context) throws IOException {
        this.uri = uri;
        cr = context.getContentResolver();
        if (cr == null) {
            throw new IOException("No content resolver");
        }

        String pebblePattern = ".*\\.(pbw|pbz|pbl)";

        if (uri.getPath().matches(pebblePattern)) {
            throw new IOException("Firmware has a filename that looks like a Pebble app/firmware.");
        }

        try (InputStream in = new BufferedInputStream(cr.openInputStream(uri))) {
            this.fw = FileUtils.readAll(in, 1024 * 1024); // 1 MB
            if (fw.length <= offsetFirmwareVersionMajor) {
                throw new IOException("This doesn't seem to be a Mi Band firmware, file size too small.");
            }
            byte firmwareVersionMajor = fw[offsetFirmwareVersionMajor];
            if (!isSupportedFirmwareVersionMajor(firmwareVersionMajor)) {
                throw new IOException("Firmware major version not supported, either too new or this isn't a Mi Band firmware: " + firmwareVersionMajor);
            }
        } catch (IOException ex) {
            throw ex; // pass through
        } catch (Exception e) {
            throw new IOException("Error reading firmware file: " + uri.toString(), e);
        }
    }

    private byte getFirmwareVersionMajor() {
        return fw[offsetFirmwareVersionMajor];
    }

    private byte getFirmwareVersionMinor() {
        return fw[offsetFirmwareVersionMinor];
    }

    private boolean isSupportedFirmwareVersionMajor(byte firmwareVersionMajor) {
        return firmwareVersionMajor == 1 || firmwareVersionMajor == 5;
    }

    public int getFirmwareVersion() {
        return (fw[offsetFirmwareVersionMajor] << 24) | (fw[offsetFirmwareVersionMinor] << 16) | (fw[offsetFirmwareVersionRevision] << 8) | fw[offsetFirmwareVersionBuild];
    }

    public String getHumanFirmwareVersion() {
        return String.format(Locale.US, "%d.%d.%d.%d", fw[offsetFirmwareVersionMajor], fw[offsetFirmwareVersionMinor], fw[offsetFirmwareVersionRevision], fw[offsetFirmwareVersionBuild]);
    }

    public byte[] getFw() {
        return fw;
    }

    public boolean isFirmwareWhitelisted() {
        for (int wlf : whitelistedFirmwareVersion) {
            if (wlf == getFirmwareVersion()) {
                return true;
            }
        }
        return false;
    }

    public boolean isFirmwareGenerallyCompatibleWith(GBDevice device) {
        String deviceHW = device.getHardwareVersion();
        if (MiBandConst.MI_1.equals(deviceHW)) {
            return getFirmwareVersionMajor() == 1;
        }
        if (MiBandConst.MI_1A.equals(deviceHW)) {
            return getFirmwareVersionMajor() == 5;
        }
        return false;
    }
}
