package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr;

public final class BinaryUtils {
    private BinaryUtils() {
    }

    public static int readByte(byte[] array, int offset) {
        return array[offset] & 0xFF;
    }

    public static int readShort(byte[] array, int offset) {
        return (array[offset] & 0xFF) | ((array[offset + 1] & 0xFF) << 8);
    }

    public static int readInt(byte[] array, int offset) {
        return (array[offset] & 0xFF) | ((array[offset + 1] & 0xFF) << 8) | ((array[offset + 2] & 0xFF) << 16) | ((array[offset + 3] & 0xFF) << 24);
    }

    public static long readLong(byte[] array, int offset) {
        return (array[offset] & 0xFFL) | ((array[offset + 1] & 0xFFL) << 8) | ((array[offset + 2] & 0xFFL) << 16) | ((array[offset + 3] & 0xFFL) << 24) |
                ((array[offset + 4] & 0xFFL) << 32) | ((array[offset + 5] & 0xFFL) << 40) | ((array[offset + 6] & 0xFFL) << 48) | ((array[offset + 7] & 0xFFL) << 56);
    }

    public static void writeByte(byte[] array, int offset, int value) {
        array[offset] = (byte) value;
    }

    public static void writeShort(byte[] array, int offset, int value) {
        array[offset] = (byte) value;
        array[offset + 1] = (byte) (value >> 8);
    }

    public static void writeInt(byte[] array, int offset, int value) {
        array[offset] = (byte) value;
        array[offset + 1] = (byte) (value >> 8);
        array[offset + 2] = (byte) (value >> 16);
        array[offset + 3] = (byte) (value >> 24);
    }

    public static void writeLong(byte[] array, int offset, long value) {
        array[offset] = (byte) value;
        array[offset + 1] = (byte) (value >> 8);
        array[offset + 2] = (byte) (value >> 16);
        array[offset + 3] = (byte) (value >> 24);
        array[offset + 4] = (byte) (value >> 32);
        array[offset + 5] = (byte) (value >> 40);
        array[offset + 6] = (byte) (value >> 48);
        array[offset + 7] = (byte) (value >> 56);
    }
}
