package nodomain.freeyourgadget.gadgetbridge.devices.miband;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.AbstractMiFirmwareInfo;

/**
 * Also see Mi1SFirmwareInfo.
 */
public class MiBandFWHelper extends AbstractMiBandFWHelper {
    private static final Logger LOG = LoggerFactory.getLogger(MiBandFWHelper.class);

    /**
     * The backing firmware info instance, which in general supports the provided
     * given firmware. You must call AbstractMiFirmwareInfo#checkValid() before
     * attempting to flash it.
     */
    @NonNull
    private AbstractMiFirmwareInfo firmwareInfo;

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
            68094986, // 4.15.12.10 tested by developer
            68158215, // 4.16.3.7 tested by developer
            68158486, // 4.16.4.22 tested by developer and user
            84870926, // 5.15.7.14 tested by developer
    };

    public MiBandFWHelper(Uri uri, Context context) throws IOException {
        super(uri, context);
    }

    @Override
    public int getFirmwareVersion() {
        // FIXME: UnsupportedOperationException!
        return firmwareInfo.getFirst().getFirmwareVersion();
    }

    @Override
    public int getFirmware2Version() {
        return firmwareInfo.getFirst().getFirmwareVersion();
    }

    @Override
    public String getHumanFirmwareVersion2() {
        return format(firmwareInfo.getSecond().getFirmwareVersion());
    }

    @Override
    protected int[] getWhitelistedFirmwareVersions() {
        return whitelistedFirmwareVersion;
    }

    @Override
    public boolean isFirmwareGenerallyCompatibleWith(GBDevice device) {
        return firmwareInfo.isGenerallyCompatibleWith(device);
    }

    @Override
    public boolean isSingleFirmware() {
        return firmwareInfo.isSingleMiBandFirmware();
    }

    /**
     * @param wholeFirmwareBytes
     * @return
     * @throws IllegalArgumentException when the data is not recognized as firmware data
     */
    @Override
    protected void determineFirmwareInfo(byte[] wholeFirmwareBytes) {
        firmwareInfo = AbstractMiFirmwareInfo.determineFirmwareInfoFor(wholeFirmwareBytes);
    }

    @Override
    public void checkValid() throws IllegalArgumentException {
        firmwareInfo.checkValid();
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
