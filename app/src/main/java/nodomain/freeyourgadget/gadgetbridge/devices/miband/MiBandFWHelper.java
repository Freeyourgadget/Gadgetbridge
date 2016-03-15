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

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.Mi1SInfo;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;

/**
 * Also see Mi1SInfo.
 */
public class MiBandFWHelper {
    private static final Logger LOG = LoggerFactory.getLogger(MiBandFWHelper.class);
    private static final int MI_FW_BASE_OFFSET = 1056;
    private static final int MI1S_FW_BASE_OFFSET = 1092;

    private final Uri uri;
    private final ContentResolver cr;
    private byte[] fw;

    private int baseOffset = -1;

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
            16779782, //1.0.10.6 reported on the wiki
            16779787, //1.0.10.11 tested by developer
            //FW_16779790, //1.0.10.14 reported on the wiki (vibration does not work currently)
            84870926, // 5.15.7.14 tested by developer
    };

    public MiBandFWHelper(Uri uri, Context context) throws IOException {
        this.uri = uri;
        cr = context.getContentResolver();
        if (cr == null) {
            throw new IOException("No content resolver");
        }

        baseOffset = determineBaseOffset(uri);
        String pebblePattern = ".*\\.(pbw|pbz|pbl)";

        if (uri.getPath().matches(pebblePattern)) {
            throw new IOException("Firmware has a filename that looks like a Pebble app/firmware.");
        }

        try (InputStream in = new BufferedInputStream(cr.openInputStream(uri))) {
            this.fw = FileUtils.readAll(in, 1024 * 1024); // 1 MB
            if (fw.length <= getOffsetFirmwareVersionMajor()) {
                throw new IOException("This doesn't seem to be a Mi Band firmware, file size too small.");
            }
            byte firmwareVersionMajor = fw[getOffsetFirmwareVersionMajor()];
            if (!isSupportedFirmwareVersionMajor(firmwareVersionMajor)) {
                throw new IOException("Firmware major version not supported, either too new or this isn't a Mi Band firmware: " + firmwareVersionMajor);
            }
        } catch (IOException ex) {
            throw ex; // pass through
        } catch (Exception e) {
            throw new IOException("Error reading firmware file: " + uri.toString(), e);
        }
    }

    private int getOffsetFirmwareVersionMajor() {
        return baseOffset + 3;
    }

    private int getOffsetFirmwareVersionMinor() {
        return baseOffset + 2;
    }

    private int getOffsetFirmwareVersionRevision() {
        return baseOffset + 1;
    }

    private int getOffsetFirmwareVersionBuild() {
        return baseOffset;
    }

    private int determineBaseOffset(Uri uri) throws IOException {
        String name = uri.getLastPathSegment().toLowerCase();
        if (name.startsWith("mili")) {
            if (name.contains("_hr")) {
                return MI1S_FW_BASE_OFFSET;
            }
            return MI_FW_BASE_OFFSET;
        } else {
            throw new IOException("Unknown file name " + name + "; cannot recognize firmware by it.");
        }
    }

    private byte getFirmwareVersionMajor() {
        return fw[getOffsetFirmwareVersionMajor()];
    }

    private byte getFirmwareVersionMinor() {
        return fw[getOffsetFirmwareVersionMinor()];
    }

    private boolean isSupportedFirmwareVersionMajor(byte firmwareVersionMajor) {
        return firmwareVersionMajor == 1 || firmwareVersionMajor == 4 || firmwareVersionMajor == 5;
    }

    public int getFirmwareVersion() {
        return (fw[getOffsetFirmwareVersionMajor()] << 24) | (fw[getOffsetFirmwareVersionMinor()] << 16) | (fw[getOffsetFirmwareVersionRevision()] << 8) | fw[getOffsetFirmwareVersionBuild()];
    }

    public static String formatFirmwareVersion(int version) {
        if (version == -1)
            return GBApplication.getContext().getString(R.string._unknown_);

        return String.format("%d.%d.%d.%d",
                version >> 24 & 255,
                version >> 16 & 255,
                version >> 8 & 255,
                version & 255);
    }

    public String getHumanFirmwareVersion() {
        return String.format(Locale.US, "%d.%d.%d.%d", fw[getOffsetFirmwareVersionMajor()], fw[getOffsetFirmwareVersionMinor()], fw[getOffsetFirmwareVersionRevision()], fw[getOffsetFirmwareVersionBuild()]);
    }

    public String getHumanFirmwareVersion2() {
        return format(Mi1SInfo.getFirmware2VersionFrom(getFw()));
    }

    public String format(int version) {
        return formatFirmwareVersion(version);
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
        if (MiBandConst.MI_1S.equals(deviceHW)) {
            return getFirmwareVersionMajor() == 4;
        }
        return false;
    }

    public boolean isSingleFirmware() {
        return Mi1SInfo.isSingleMiBandFirmware(getFw());
    }
}
