package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr;

public final class ChecksumCalculator {
    private static final int[] CONSTANTS = {0x0000, 0xCC01, 0xD801, 0x1400, 0xF001, 0x3C00, 0x2800, 0xE401, 0xA001, 0x6C00, 0x7800, 0xB401, 0x5000, 0x9C01, 0x8801, 0x4400};

    private ChecksumCalculator() {
    }

    public static int computeCrc(byte[] data, int offset, int length) {
        return computeCrc(0, data, offset, length);
    }

    public static int computeCrc(int initialCrc, byte[] data, int offset, int length) {
        int crc = initialCrc;
        for (int i = offset; i < offset + length; ++i) {
            int b = data[i];
            crc = (((crc >> 4) & 4095) ^ CONSTANTS[crc & 15]) ^ CONSTANTS[b & 15];
            crc = (((crc >> 4) & 4095) ^ CONSTANTS[crc & 15]) ^ CONSTANTS[(b >> 4) & 15];
        }
        return crc;
    }
}
