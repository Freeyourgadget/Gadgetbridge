package nodomain.freeyourgadget.gadgetbridge.devices.miband;

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
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.UriHelper;

/**
 * Also see Mi1SFirmwareInfo.
 */
public abstract class AbstractMiBandFWHelper {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractMiBandFWHelper.class);

    @NonNull
    private final byte[] fw;

    public AbstractMiBandFWHelper(Uri uri, Context context) throws IOException {
        UriHelper uriHelper = UriHelper.get(uri, context);
        String pebblePattern = ".*\\.(pbw|pbz|pbl)";
        if (uriHelper.getFileName().matches(pebblePattern)) {
            throw new IOException("Firmware has a filename that looks like a Pebble app/firmware.");
        }

        try (InputStream in = new BufferedInputStream(uriHelper.openInputStream())) {
            this.fw = FileUtils.readAll(in, 1024 * 1024); // 1 MB
            determineFirmwareInfo(fw);
        } catch (IOException ex) {
            throw ex; // pass through
        } catch (IllegalArgumentException ex) {
            throw new IOException("This doesn't seem to be a Mi Band firmware: " + ex.getLocalizedMessage(), ex);
        } catch (Exception e) {
            throw new IOException("Error reading firmware file: " + uri.toString(), e);
        }
    }

    public abstract int getFirmwareVersion();

    public abstract int getFirmware2Version();

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

    public abstract String getHumanFirmwareVersion2();

    public String format(int version) {
        return formatFirmwareVersion(version);
    }

    @NonNull
    public byte[] getFw() {
        return fw;
    }

    public boolean isFirmwareWhitelisted() {
        for (int wlf : getWhitelistedFirmwareVersions()) {
            if (wlf == getFirmwareVersion()) {
                return true;
            }
        }
        return false;
    }

    protected abstract int[] getWhitelistedFirmwareVersions();

    public abstract boolean isFirmwareGenerallyCompatibleWith(GBDevice device);

    public abstract boolean isSingleFirmware();

    /**
     * @param wholeFirmwareBytes
     * @return
     * @throws IllegalArgumentException when the data is not recognized as firmware data
     */
    @NonNull
    protected abstract void determineFirmwareInfo(byte[] wholeFirmwareBytes);

    public abstract void checkValid() throws IllegalArgumentException;
}
