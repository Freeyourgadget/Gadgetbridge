package nodomain.freeyourgadget.gadgetbridge.service.devices.miband;

import android.support.annotation.NonNull;

import java.lang.reflect.Array;
import java.util.Arrays;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils;

public abstract class AbstractMiFirmwareInfo {

    /**
     * @param wholeFirmwareBytes
     * @return
     * @throws IllegalArgumentException when the data is not recognized as firmware data
     */
    public static
    @NonNull
    AbstractMiFirmwareInfo determineFirmwareInfoFor(byte[] wholeFirmwareBytes) {
        AbstractMiFirmwareInfo[] candidates = getFirmwareInfoCandidatesFor(wholeFirmwareBytes);
        if (candidates.length == 0) {
            throw new IllegalArgumentException("Unsupported data (maybe not even a firmware?).");
        }
        if (candidates.length == 1) {
            return candidates[0];
        }
        throw new IllegalArgumentException("don't know for which device the firmware is, matches multiple devices");
    }

    private static AbstractMiFirmwareInfo[] getFirmwareInfoCandidatesFor(byte[] wholeFirmwareBytes) {
        AbstractMiFirmwareInfo[] candidates = new AbstractMiFirmwareInfo[3];
        int i = 0;
        Mi1FirmwareInfo mi1Info = Mi1FirmwareInfo.getInstance(wholeFirmwareBytes);
        if (mi1Info != null) {
            candidates[i++] = mi1Info;
        }
        Mi1AFirmwareInfo mi1aInfo = Mi1AFirmwareInfo.getInstance(wholeFirmwareBytes);
        if (mi1aInfo != null) {
            candidates[i++] = mi1aInfo;
        }
        Mi1SFirmwareInfo mi1sInfo = Mi1SFirmwareInfo.getInstance(wholeFirmwareBytes);
        if (mi1sInfo != null) {
            candidates[i++] = mi1sInfo;
        }
        return Arrays.copyOfRange(candidates, 0, i);
    }

    @NonNull
    protected byte[] wholeFirmwareBytes;

    public AbstractMiFirmwareInfo(@NonNull byte[] wholeFirmwareBytes) {
        this.wholeFirmwareBytes = wholeFirmwareBytes;
    }

    public abstract int getFirmwareOffset();

    public abstract int getFirmwareLength();

    public abstract int getFirmwareVersion();

    /**
     * Returns true if the firmware data is recognized as such and can be
     * handled by this instance. No further sanity checks are done at this point.
     */
    protected abstract boolean isGenerallySupportedFirmware();

    /**
     * This method checks whether the firmware data is recognized as such and can be handled
     * by this instance. It will be called by #isGenerallySupportedFirmware() in order to check
     * whether this instance can be used at all or shall be thrown away.
     */
    protected abstract boolean isHeaderValid();

    /**
     * Checks whether this instance, with the provided firmware data is compatible with the
     * given device. Must be called to avoid installing Mi1 firmware on Mi1A, for example.
     * @param device
     */
    public abstract boolean isGenerallyCompatibleWith(GBDevice device);

    public @NonNull byte[] getFirmwareBytes() {
        return Arrays.copyOfRange(wholeFirmwareBytes, getFirmwareOffset(), getFirmwareOffset() + getFirmwareLength());
    }

    public int getFirmwareVersionMajor() {
        int version = getFirmwareVersion();
        if (version > 0) {
            return (version >> 24);
        }
        throw new IllegalArgumentException("bad firmware version: " + version);
    }

    public abstract boolean isSingleMiBandFirmware();

    /**
     * Performs a thorough sanity check of the firmware data and throws IllegalArgumentException
     * if there's any problem with it.
     * @throws IllegalArgumentException
     */
    public void checkValid() throws IllegalArgumentException {
    }

    public AbstractMiFirmwareInfo getFirst() {
        if (isSingleMiBandFirmware()) {
            return this;
        }
        throw new UnsupportedOperationException(getClass().getName() + " must override getFirst() and getSecond()");
    }

    public AbstractMiFirmwareInfo getSecond() {
        if (isSingleMiBandFirmware()) {
            throw new UnsupportedOperationException(getClass().getName() + " only supports on firmware");
        }
        throw new UnsupportedOperationException(getClass().getName() + " must override getFirst() and getSecond()");
    }
}
