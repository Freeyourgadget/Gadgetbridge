package nodomain.freeyourgadget.gadgetbridge.miband;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.Locale;

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
            16779547  //1.0.9.27 testd by developer
    };

    public MiBandFWHelper(Uri uri, Context context) {
        this.uri = uri;
        cr = context.getContentResolver();

        InputStream fin;

        try {
            fin = new BufferedInputStream(cr.openInputStream(uri));
            this.fw = new byte[fin.available()];
            fin.read(fw);
            fin.close();

        } catch (Exception e) {
            e.printStackTrace();
            this.fw = null;
        }

        if (fw[firmwareVersionMajor] != 1) {
            LOG.error("Firmware major version should be 1, probably this isn't a MiBand firmware.");
            this.fw = null;
        }

    }

    public int getFirmwareVersion() {
        if (fw == null) {
            return -1;
        }
        return (fw[firmwareVersionMajor] << 24) | (fw[firmwareVersionMinor] << 16) | (fw[firmwareVersionRevision] << 8) | fw[firmwareVersionBuild];
    }

    public String getHumanFirmwareVersion() {
        if (fw == null) {
            return "UNK";
        }
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

    //thanks http://stackoverflow.com/questions/13209364/convert-c-crc16-to-java-crc16
    public int getCRC16(byte[] seq) {
        int crc = 0xFFFF;

        for (int j = 0; j < seq.length; j++) {
            crc = ((crc >>> 8) | (crc << 8)) & 0xffff;
            crc ^= (seq[j] & 0xff);//byte to int, trunc sign
            crc ^= ((crc & 0xff) >> 4);
            crc ^= (crc << 12) & 0xffff;
            crc ^= ((crc & 0xFF) << 5) & 0xffff;
        }
        crc &= 0xffff;
        return crc;
    }

}
