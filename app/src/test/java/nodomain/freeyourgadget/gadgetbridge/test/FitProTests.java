package nodomain.freeyourgadget.gadgetbridge.test;

import org.junit.Test;

public class FitProTests extends TestBase {
    // since unit tests seem to be broken in Android Studio these are not complete
    // but but at least this provides some help for the future
    // can be used this way:

    @Test
    public void testSleepPacket() {
        String sleepData = "cd002915010300242b1f0008000f0003001e0002002d0001003c0001004b0001005a00010069000200780002";

        //handleSleepData(stringToByteArray(sleepData));
    }

    @Test
    public void testStepPacket() {
        //note different format of data and thus different stringWith0xToByteArray method
        String stepData = "0xcd 0x00 0x31 0x15 0x01 0x02 0x00 0x2c 0x2b 0x34 0x00 0x05 0x80 0x45 0x07 0x1a 0x09 0xc8 0xbb 0x06 0x79 0x55 0x06 0x26 0x09 0xe8 0xb0 0xe4 0x71 0x15 0x05 0x9b 0x0a 0x08 0xa4 0xdc 0x7b 0x85 0x06 0x67 0x0a 0x28 0xb4 0x16 0x76 0x35 0x06 0x16 0x0a 0x48 0xac 0x57";

        //handleStepData(stringWith0xToByteArray(stepData));
    }

    public byte[] stringToByteArray(String s) {
        byte[] byteArray = new byte[s.length() / 2];
        String[] strBytes = new String[s.length() / 2];
        int k = 0;
        for (int i = 0; i < s.length(); i = i + 2) {
            int j = i + 2;
            strBytes[k] = s.substring(i, j);
            byteArray[k] = (byte) Integer.parseInt(strBytes[k], 16);
            k++;
        }
        return byteArray;
    }


    public static byte[] stringWith0xToByteArray(String s) {
        String[] split = s.split(" ");
        int k = 0;
        byte[] byteArray = new byte[split.length];
        for (String ch : split) {
            byteArray[k] = (byte) Integer.parseInt(ch.split("x")[1], 16);
            k++;
        }
        return byteArray;
    }

}
