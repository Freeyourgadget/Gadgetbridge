package nodomain.freeyourgadget.gadgetbridge.service.devices.miband2;

import java.util.HashMap;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils;
import nodomain.freeyourgadget.gadgetbridge.util.CheckSums;

public class Mi2FirmwareInfo {
    private static final byte[] FW_HEADER = new byte[]{
            (byte) 0xa3,
            (byte) 0x68,
            (byte) 0x04,
            (byte) 0x3b,
            (byte) 0x02,
            (byte) 0xdb,
            (byte) 0xc8,
            (byte) 0x58,
            (byte) 0xd0,
            (byte) 0x50,
            (byte) 0xfa,
            (byte) 0xe7,
            (byte) 0x0c,
            (byte) 0x34,
            (byte) 0xf3,
            (byte) 0xe7,
    };
    private static final int FW_HEADER_OFFSET = 0x150;

    private static Map<Integer,String> crcToVersion = new HashMap<>();
    static {
        crcToVersion.put(41899, "1.0.0.39");
    }

    public static String toVersion(int crc16) {
        return crcToVersion.get(crc16);
    }

    public static int[] getWhitelistedVersions() {
        return ArrayUtils.toIntArray(crcToVersion.keySet());
    }

    private final int crc16;

    private byte[] bytes;
    private String firmwareVersion;

    public Mi2FirmwareInfo(byte[] bytes) {
        this.bytes = bytes;
        crc16 = CheckSums.getCRC16(bytes);
        firmwareVersion = crcToVersion.get(crc16);
    }

    public boolean isGenerallyCompatibleWith(GBDevice device) {
        return isHeaderValid() && device.getType() == DeviceType.MIBAND2;
    }

    public boolean isHeaderValid() {
        // TODO: this is certainly not a correct validation, but it works for now
        return ArrayUtils.equals(bytes, FW_HEADER, FW_HEADER_OFFSET);
    }

    public void checkValid() throws IllegalArgumentException {
    }

    /**
     * Returns the size of the firmware in number of bytes.
     * @return
     */
    public int getSize() {
        return bytes.length;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public int getCrc16() {
        return crc16;
    }

    public int getFirmwareVersion() {
        return getCrc16(); // HACK until we know how to determine the version from the fw bytes
    }
}
