package nodomain.freeyourgadget.gadgetbridge.service.devices.fitpro;

import static junit.framework.TestCase.assertEquals;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.CMD_FIND_BAND;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.CMD_GET_HW_INFO;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.CMD_GROUP_GENERAL;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.CMD_GROUP_REQUEST_DATA;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.CMD_SET_DEVICE_VIBRATIONS;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.VALUE_ON;
import static nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils.arrayToString;

import org.junit.Test;

// invoke individual test this way:
//./gradlew :app:testMainDebugUnitTest --tests nodomain.freeyourgadget.gadgetbridge.service.devices.fitpro.FitProDeviceSupportTest

public class FitProDeviceSupportTest {

    @Test
    public void testCraftDataFromCommandAndSingleValue() {
        byte[] data = FitProDeviceSupport.craftData(CMD_GROUP_REQUEST_DATA, CMD_GET_HW_INFO);
        String result = arrayToString(data);
        String expected = arrayToString(new byte[]{(byte) 0xCD, 0x00, 0x05, 0x1A, 0x01, 0x10, 0x00, 0x00});
        System.out.println("fitpro test, data 1: " + result);
        assertEquals(expected, result);
    }

    @Test
    public void testCraftDataFromCommandSingleValueAndParameter() {
        byte[] data = FitProDeviceSupport.craftData(CMD_GROUP_GENERAL, CMD_FIND_BAND, VALUE_ON);
        String result = arrayToString(data);
        String expected = arrayToString(new byte[]{(byte) 0xCD, 0x00, 0x06, 0x12, 0x01, 0x0B, 0x00, 0x01, 0x01});
        System.out.println("fitpro test, data 2: " + result);
        assertEquals(expected, result);
    }

    @Test
    public void testCraftDataFromCommandAndByteArray() {
        byte[] data = FitProDeviceSupport.craftData(CMD_GROUP_GENERAL, CMD_SET_DEVICE_VIBRATIONS, new byte[]{0, 0, 0, 0});
        String result = arrayToString(data);
        String expected = arrayToString(new byte[]{(byte) 0xCD, 0x00, 0x09, 0x12, 0x01, 0x08, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00});
        System.out.println("fitpro test, data 3: " + result);
        assertEquals(expected, result);
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