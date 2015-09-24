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

import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;

public class MiBandFWHelper {
    private static final Logger LOG = LoggerFactory.getLogger(MiBandFWHelper.class);

    private final Uri uri;
    private final ContentResolver cr;
    private byte[] fw;

    private final int firmwareVersionBuild = 1056;
    private final int firmwareVersionRevision = 1057;
    private final int firmwareVersionMinor = 1058;
    private final int firmwareVersionMajor = 1059;

    private final int[] whitelistedFirmwareVersion = {
            16779534, // 1.0.9.14 tested by developer
            16779547,  //1.0.9.27 tested by developer
            16779568, //1.0.9.48 tested by developer
            16779585, //1.0.9.65 tested by developer
            16779779, //1.0.10.3 reported on the wiki
            16779782, //1.0.10.6 reported on the wikiew
            16779787, //1.0.10.11 tested by developer
            //16779790, //1.0.10.14 reported on the wiki (vibration does not work currently)
    };

    public MiBandFWHelper(Uri uri, Context context) throws IOException {
        this.uri = uri;
        cr = context.getContentResolver();

        String pebblePattern = ".*\\.(pbw|pbz)";

        if (uri.getPath().matches(pebblePattern)) {
            throw new IOException("Firmware has a filename that looks like a Pebble app/firmware.");
        }

        try (InputStream in = new BufferedInputStream(cr.openInputStream(uri))) {
            this.fw = FileUtils.readAll(in, 1024 * 1024); // 1 MB
            if (fw.length <= firmwareVersionMajor || fw[firmwareVersionMajor] != 1) {
                throw new IOException("Firmware major version should be 1, probably this isn't a MiBand firmware.");
            }
        } catch (Exception e) {
            throw new IOException("Error reading firmware file: " + uri.toString(), e);
        }
    }

    public int getFirmwareVersion() {
        return (fw[firmwareVersionMajor] << 24) | (fw[firmwareVersionMinor] << 16) | (fw[firmwareVersionRevision] << 8) | fw[firmwareVersionBuild];
    }

    public String getHumanFirmwareVersion() {
        return String.format(Locale.US, "%d.%d.%d.%d", fw[firmwareVersionMajor], fw[firmwareVersionMinor], fw[firmwareVersionRevision], fw[firmwareVersionBuild]);
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
}
