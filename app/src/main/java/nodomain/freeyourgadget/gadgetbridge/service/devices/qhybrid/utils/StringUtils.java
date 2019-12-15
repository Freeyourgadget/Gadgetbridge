package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.utils;

public class StringUtils extends nodomain.freeyourgadget.gadgetbridge.util.StringUtils {
    public static String terminateNull(String input){
        if(input.length() == 0){
            return new String(new byte[]{(byte) 0});
        }
        char lastChar = input.charAt(input.length() - 1);
        if(lastChar == 0) return input;

        byte[] newArray = new byte[input.length() + 1];
        System.arraycopy(input.getBytes(), 0, newArray, 0, input.length());

        newArray[newArray.length - 1] = 0;

        return new String(newArray);
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = bytes.length - 1; j >= 0; j--) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
}
