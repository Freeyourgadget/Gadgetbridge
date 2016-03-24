package nodomain.freeyourgadget.gadgetbridge.devices.miband;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.AbstractMiFirmwareInfo;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;

/**
 * Also see Mi1SFirmwareInfo.
 */
public class MiBandFWHelper {
    private static final Logger LOG = LoggerFactory.getLogger(MiBandFWHelper.class);

    /**
     * The backing firmware info instance, which in general supports the provided
     * given firmware. You must call AbstractMiFirmwareInfo#checkValid() before
     * attempting to flash it.
     */
    @NonNull
    private final AbstractMiFirmwareInfo firmwareInfo;
    @NonNull
    private final byte[] fw;

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
        String pebblePattern = ".*\\.(pbw|pbz|pbl)";
        if (uri.getPath().matches(pebblePattern)) {
            throw new IOException("Firmware has a filename that looks like a Pebble app/firmware.");
        }

        try (InputStream in = new BufferedInputStream(context.getContentResolver().openInputStream(uri))) {
            this.fw = FileUtils.readAll(in, 1024 * 1024); // 1 MB
            this.firmwareInfo = determineFirmwareInfoFor(fw);
        } catch (IOException ex) {
            throw ex; // pass through
        } catch (IllegalArgumentException ex) {
            throw new IOException("This doesn't seem to be a Mi Band firmware: " + ex.getLocalizedMessage(), ex);
        } catch (Exception e) {
            throw new IOException("Error reading firmware file: " + uri.toString(), e);
        }
    }

    public int getFirmwareVersion() {
        // FIXME: UnsupportedOperationException!
        return firmwareInfo.getFirst().getFirmwareVersion();
    }

    public int getFirmware2Version() {
        return firmwareInfo.getFirst().getFirmwareVersion();
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
        return format(getFirmwareVersion());
    }

    public String getHumanFirmwareVersion2() {
        return format(firmwareInfo.getSecond().getFirmwareVersion());
    }

    public String format(int version) {
        return formatFirmwareVersion(version);
    }

    @NonNull
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
        return firmwareInfo.isGenerallyCompatibleWith(device);
    }

    public boolean isSingleFirmware() {
        return firmwareInfo.isSingleMiBandFirmware();
    }

    /**
     * @param wholeFirmwareBytes
     * @return
     * @throws IllegalArgumentException when the data is not recognized as firmware data
     */
    public static
    @NonNull
    AbstractMiFirmwareInfo determineFirmwareInfoFor(byte[] wholeFirmwareBytes) {
        return AbstractMiFirmwareInfo.determineFirmwareInfoFor(wholeFirmwareBytes);
    }

    /**
     * The backing firmware info instance, which in general supports the provided
     * given firmware. You MUST call AbstractMiFirmwareInfo#checkValid() AND
     * isGenerallyCompatibleWithDevice() before attempting to flash it.
     */
    @NonNull
    public AbstractMiFirmwareInfo getFirmwareInfo() {
        return firmwareInfo;
    }
}
