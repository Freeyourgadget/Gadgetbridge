package nodomain.freeyourgadget.gadgetbridge.miband;

public class LEDColors {

    public static final int toInt(byte r, byte g, byte b) {
        int result = ((int) r << 16);
        result |= ((int) g << 8);
        result |= ((int) b);
        return result;
    }

    public static final byte[] toBytes(int rgb) {
        byte r = (byte) ((rgb >> 16) & 0x0000ff);
        byte g = (byte) ((rgb >> 8) & 0x0000ff);
        byte b = (byte) (rgb & 0x0000ff);
        return new byte[] { r, g, b };
    }

    public static final int RED = toInt((byte) 6, (byte) 0, (byte) 0);
    public static final int GREEN = toInt((byte) 0, (byte) 6, (byte) 6);
    public static final int BLUE = toInt((byte) 0, (byte) 0, (byte) 6);
    public static final int CYAN = toInt((byte) 0, (byte) 6, (byte) 6);
    public static final int YELLOW = toInt((byte) 6, (byte) 6, (byte) 0);
    public static final int MAGENTA = toInt((byte) 6, (byte) 0, (byte) 6);
    public static final int OFF = toInt((byte) 0, (byte) 0, (byte) 0);

}
