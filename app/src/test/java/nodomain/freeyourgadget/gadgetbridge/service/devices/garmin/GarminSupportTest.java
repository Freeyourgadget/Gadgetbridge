package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.communicator.CobsCoDec;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FieldDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitFile;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.GlobalFITMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;
import nodomain.freeyourgadget.gadgetbridge.util.GB;


public class GarminSupportTest {
    //test strings from https://github.com/themarpe/cobs-java/blob/master/tests-java/Tests.java
    static final byte[] test_string_0 = new byte[]{0, 0, 0, 0};
    static final byte[] test_string_1 = new byte[]{0, '1', '2', '3', '4', '5'};
    static final byte[] test_string_2 = new byte[]{0, '1', '2', '3', '4', '5', 0};
    static final byte[] test_string_3 = new byte[]{'1', '2', '3', '4', '5', 0, '6', '7', '8', '9'};
    static final byte[] test_string_4 = new byte[]{0, '1', '2', '3', '4', '5', 0, '6', '7', '8', '9', 0};
    static final byte[] test_string_5 = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127, -128, -127, -126, -125, -124, -123, -122, -121, -120, -119, -118, -117, -116, -115, -114, -113, -112, -111, -110, -109, -108, -107, -106, -105, -104, -103, -102, -101, -100, -99, -98, -97, -96, -95, -94, -93, -92, -91, -90, -89, -88, -87, -86, -85, -84, -83, -82, -81, -80, -79, -78, -77, -76, -75, -74, -73, -72, -71, -70, -69, -68, -67, -66, -65, -64, -63, -62, -61, -60, -59, -58, -57, -56, -55, -54, -53, -52, -51, -50, -49, -48, -47, -46, -45, -44, -43, -42, -41, -40, -39, -38, -37, -36, -35, -34, -33, -32, -31, -30, -29, -28, -27, -26, -25, -24, -23, -22, -21, -20, -19, -18, -17, -16, -15, -14, -13, -12, -11, -10, -9, -8, -7, -6, -5, -4, -3, -2, -1, 0};
    static final byte[] test_string_6 = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127, -128, -127, -126, -125, -124, -123, -122, -121, -120, -119, -118, -117, -116, -115, -114, -113, -112, -111, -110, -109, -108, -107, -106, -105, -104, -103, -102, -101, -100, -99, -98, -97, -96, -95, -94, -93, -92, -91, -90, -89, -88, -87, -86, -85, -84, -83, -82, -81, -80, -79, -78, -77, -76, -75, -74, -73, -72, -71, -70, -69, -68, -67, -66, -65, -64, -63, -62, -61, -60, -59, -58, -57, -56, -55, -54, -53, -52, -51, -50, -49, -48, -47, -46, -45, -44, -43, -42, -41, -40, -39, -38, -37, -36, -35, -34, -33, -32, -31, -30, -29, -28, -27, -26, -25, -24, -23, -22, -21, -20, -19, -18, -17, -16, -15, -14, -13, -12, -11, -10, -9, -8, -7, -6, -5, -4, -3, -2, -1, 0};
    static final byte[] test_string_7 = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127, -128, -127, -126, -125, -124, -123, -122, -121, -120, -119, -118, -117, -116, -115, -114, -113, -112, -111, -110, -109, -108, -107, -106, -105, -104, -103, -102, -101, -100, -99, -98, -97, -96, -95, -94, -93, -92, -91, -90, -89, -88, -87, -86, -85, -84, -83, -82, -81, -80, -79, -78, -77, -76, -75, -74, -73, -72, -71, -70, -69, -68, -67, -66, -65, -64, -63, -62, -61, -60, -59, -58, -57, -56, -55, -54, -53, -52, -51, -50, -49, -48, -47, -46, -45, -44, -43, -42, -41, -40, -39, -38, -37, -36, -35, -34, -33, -32, -31, -30, -29, -28, -27, -26, -25, -24, -23, -22, -21, -20, -19, -18, -17, -16, -15, -14, -13, -12, -11, -10, -9, -8, -7, -6, -5, -4, -3, -2, -1};
    static final byte[] test_string_8 = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127, -128, -127, -126, -125, -124, -123, -122, -121, -120, -119, -118, -117, -116, -115, -114, -113, -112, -111, -110, -109, -108, -107, -106, -105, -104, -103, -102, -101, -100, -99, -98, -97, -96, -95, -94, -93, -92, -91, -90, -89, -88, -87, -86, -85, -84, -83, -82, -81, -80, -79, -78, -77, -76, -75, -74, -73, -72, -71, -70, -69, -68, -67, -66, -65, -64, -63, -62, -61, -60, -59, -58, -57, -56, -55, -54, -53, -52, -51, -50, -49, -48, -47, -46, -45, -44, -43, -42, -41, -40, -39, -38, -37, -36, -35, -34, -33, -32, -31, -30, -29, -28, -27, -26, -25, -24, -23, -22, -21, -20, -19, -18, -17, -16, -15, -14, -13, -12, -11, -10, -9, -8, -7, -6, -5, -4, -3, -2, -1, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127, -128, -127, -126, -125, -124, -123, -122, -121, -120, -119, -118, -117, -116, -115, -114, -113, -112, -111, -110, -109, -108, -107, -106, -105, -104, -103, -102, -101, -100, -99, -98, -97, -96, -95, -94, -93, -92, -91, -90, -89, -88, -87, -86, -85, -84, -83, -82, -81, -80, -79, -78, -77, -76, -75, -74, -73, -72, -71, -70, -69, -68, -67, -66, -65, -64, -63, -62, -61, -60, -59, -58, -57, -56, -55, -54, -53, -52, -51, -50, -49, -48, -47, -46, -45, -44, -43, -42, -41, -40, -39, -38, -37, -36, -35, -34, -33, -32, -31, -30, -29, -28, -27, -26, -25, -24, -23, -22, -21, -20, -19, -18, -17, -16, -15, -14, -13, -12, -11, -10, -9, -8, -7, -6, 0, -5, -4, -3, -2, -1};
    final CobsCoDec cobsCoDec = new CobsCoDec();
    final byte[][] allTests = new byte[][]{test_string_1, test_string_2, test_string_3, test_string_4, test_string_5, test_string_6, test_string_7, test_string_8};

    @Test
    public void testCobsDecoder() {

        cobsCoDec.receivedBytes(GB.hexStringToByteArray("00022C04A0139623310F684C1BCA840508020B"));
        Assert.assertNull(cobsCoDec.retrieveMessage());
        cobsCoDec.receivedBytes(GB.hexStringToByteArray("496E7374696E637420325308496E7374696E63"));
        Assert.assertNull(cobsCoDec.retrieveMessage());
        cobsCoDec.receivedBytes(GB.hexStringToByteArray("74023253010304B800"));
        Assert.assertArrayEquals(GB.hexStringToByteArray("2C00A0139600310F684C1BCA840508020B496E7374696E637420325308496E7374696E6374023253000004B8"),
                cobsCoDec.retrieveMessage());
    }

    @Test
    public void testCobsDecoder2() {

        cobsCoDec.receivedBytes(GB.hexStringToByteArray("00022b058813a013029623ffffffffffffa71fffff046c61726a07756e6b6e6f776e0758512d4343373201f9cf00"));
        Assert.assertArrayEquals(new byte[]{0x2b, 0x00, (byte) 0x88, 0x13, (byte) 0xa0, 0x13, 0x00, (byte) 0x96, 0x00, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xa7, 0x1f, (byte) 0xff, (byte) 0xff, 0x04, 0x6c, 0x61, 0x72, 0x6a, 0x07, 0x75, 0x6e, 0x6b, 0x6e, 0x6f, 0x77, 0x6e, 0x07, 0x58, 0x51, 0x2d, 0x43, 0x43, 0x37, 0x32, 0x01, (byte) 0xf9, (byte) 0xcf},
                cobsCoDec.retrieveMessage());
    }

    @Test
    public void testCobsEncoder2() {

        byte[] result = cobsCoDec.encode(GB.hexStringToByteArray("022b058813a013029623ffffffffffffa71fffff046c61726a07756e6b6e6f776e0758512d4343373201f9cf00"));
        Assert.assertArrayEquals(new byte[]{0x00, 0x2d, (byte) 0x02, (byte) 0x2b, (byte) 0x05, (byte) 0x88, (byte) 0x13, (byte) 0xa0, (byte) 0x13, (byte) 0x02, (byte) 0x96, (byte) 0x23, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xa7, (byte) 0x1f, (byte) 0xff, (byte) 0xff, (byte) 0x04, (byte) 0x6c, (byte) 0x61, (byte) 0x72, (byte) 0x6a, (byte) 0x07, (byte) 0x75, (byte) 0x6e, (byte) 0x6b, (byte) 0x6e, (byte) 0x6f, (byte) 0x77, (byte) 0x6e, (byte) 0x07, (byte) 0x58, (byte) 0x51, (byte) 0x2d, (byte) 0x43, (byte) 0x43, (byte) 0x37, (byte) 0x32, (byte) 0x01, (byte) 0xf9, (byte) 0xcf, (byte) 0x01, (byte) 0x00},
                result);
    }


    @Test
    public void testCobsDecoderSingleByteAtStart() {

        cobsCoDec.receivedBytes(GB.hexStringToByteArray("00"));
        Assert.assertNull(cobsCoDec.retrieveMessage());
        cobsCoDec.receivedBytes(GB.hexStringToByteArray("022C04A0139623310F684C1BCA840508020B"));
        Assert.assertNull(cobsCoDec.retrieveMessage());
        cobsCoDec.receivedBytes(GB.hexStringToByteArray("496E7374696E637420325308496E7374696E63"));
        Assert.assertNull(cobsCoDec.retrieveMessage());
        cobsCoDec.receivedBytes(GB.hexStringToByteArray("74023253010304B800"));
        Assert.assertArrayEquals(GB.hexStringToByteArray("2C00A0139600310F684C1BCA840508020B496E7374696E637420325308496E7374696E6374023253000004B8"),
                cobsCoDec.retrieveMessage());
    }

    @Test
    public void testCobsDecoderSingleByteAtEnd() {

        cobsCoDec.receivedBytes(GB.hexStringToByteArray("00022C04A0139623310F684C1BCA840508020B"));
        Assert.assertNull(cobsCoDec.retrieveMessage());
        cobsCoDec.receivedBytes(GB.hexStringToByteArray("496E7374696E637420325308496E7374696E63"));
        Assert.assertNull(cobsCoDec.retrieveMessage());
        cobsCoDec.receivedBytes(GB.hexStringToByteArray("74023253010304B8"));
        Assert.assertNull(cobsCoDec.retrieveMessage());
        cobsCoDec.receivedBytes(GB.hexStringToByteArray("00"));
        Assert.assertArrayEquals(GB.hexStringToByteArray("2C00A0139600310F684C1BCA840508020B496E7374696E637420325308496E7374696E6374023253000004B8"),
                cobsCoDec.retrieveMessage());
    }

    @Test
    public void testCobsEncoder() {
        Assert.assertArrayEquals(GB.hexStringToByteArray("00022C04A0139623310F684C1BCA840508020B496E7374696E637420325308496E7374696E6374023253010304B800"),
                cobsCoDec.encode(GB.hexStringToByteArray("2C00A0139600310F684C1BCA840508020B496E7374696E637420325308496E7374696E6374023253000004B8")));

    }

    @Test
    public void testLongPayload() {

        for (byte[] payload : allTests) {
            byte[] encodedData = cobsCoDec.encode(payload);
            cobsCoDec.receivedBytes(encodedData);
            byte[] decodedData = cobsCoDec.retrieveMessage();

            Assert.assertArrayEquals(payload, decodedData);
        }
    }

    @Test
    public void testBaseFields() {

        RecordDefinition recordDefinition = new RecordDefinition(ByteOrder.LITTLE_ENDIAN, new RecordHeader((byte) 6), GlobalFITMessage.WEATHER, null); //just some random data
        List<FieldDefinition> fieldDefinitionList = new ArrayList<>();
        for (BaseType baseType :
                BaseType.values()) {
            fieldDefinitionList.add(new FieldDefinition(baseType.getIdentifier(), baseType.getSize(), baseType, baseType.name()));

        }
        recordDefinition.setFieldDefinitions(fieldDefinitionList);

        RecordData test = new RecordData(recordDefinition);

        for (BaseType baseType :
                BaseType.values()) {
            System.out.println(baseType.getIdentifier());
            Object startVal, endVal;

            switch (baseType.name()) {
                case "ENUM":
                case "UINT8":
                case "BASE_TYPE_BYTE":
                    startVal = 0;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = (int) 0xff - 1;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = -1;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertNull(endVal);
                    break;
                case "SINT8":
                    startVal = (int) Byte.MIN_VALUE;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = (int) Byte.MAX_VALUE - 1;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = (int) Byte.MIN_VALUE - 1;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertNull(endVal);
                    break;
                case "SINT16":
                    startVal = (int) Short.MIN_VALUE;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = (int) Short.MAX_VALUE - 1;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = (int) Short.MIN_VALUE - 1;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertNull(endVal);
                    break;
                case "UINT16":
                    startVal = 0;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = (int) 0xffff - 1;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = -1;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertNull(endVal);
                    break;
                case "SINT32":
                    startVal = (long) Integer.MIN_VALUE;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = (long) Integer.MAX_VALUE - 1;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = (long) Integer.MIN_VALUE - 1;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertNull(endVal);
                    break;
                case "UINT32":
                    startVal = 0L;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = (long) 0xffffffffL - 1;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, (long) ((long) endVal & 0xffffffffL));
                    startVal = 0xffffffff;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertNull(endVal);
                    break;
                case "FLOAT32":
                    startVal = 0.0f;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = -Float.MAX_VALUE;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = Float.MAX_VALUE;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = (double) -Float.MAX_VALUE * 2;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertNull(endVal);
                    break;
                case "FLOAT64":
                    startVal = 0.0d;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = Double.MIN_VALUE;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = Double.MAX_VALUE;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = (double) -Double.MAX_VALUE * 2;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertNull(endVal);
                    break;
                case "UINT8Z":
                    startVal = 1;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = (int) 0xff;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = 0;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertNull(endVal);
                    startVal = -1;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertNull(endVal);
                    break;
                case "UINT16Z":
                    startVal = 1;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = (int) 0xffff;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = -1;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertNull(endVal);
                    startVal = 0;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertNull(endVal);
                    break;
                case "UINT32Z":
                    startVal = 1L;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = (long) 0xffffffffL;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, (long) ((long) endVal & 0xffffffffL));
                    startVal = -1;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertNull(endVal);
                    startVal = 0;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertNull(endVal);
                    break;
                case "SINT64":
                    startVal = BigInteger.valueOf(Long.MIN_VALUE);
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(((BigInteger) startVal).longValue(), endVal);
                    startVal = BigInteger.valueOf(Long.MAX_VALUE - 1);
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(((BigInteger) startVal).longValue(), endVal);
                    startVal = BigInteger.valueOf(Long.MAX_VALUE);
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertNull(endVal);
                    break;
                case "UINT64":
                    startVal = 0L;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = BigInteger.valueOf(0xFFFFFFFFFFFFFFFFL - 1);
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(((BigInteger) startVal).longValue() & 0xFFFFFFFFFFFFFFFFL, endVal);
                    startVal = BigInteger.valueOf(0xFFFFFFFFFFFFFFFFL);
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertNull(endVal);
                    break;
                case "UINT64Z":
                    startVal = 1L;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(startVal, endVal);
                    startVal = BigInteger.valueOf(0xFFFFFFFFFFFFFFFFL);
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertEquals(((BigInteger) startVal).longValue() & 0xFFFFFFFFFFFFFFFFL, endVal);
                    startVal = 0L;
                    test.setFieldByName(baseType.name(), startVal);
                    endVal = test.getFieldByName(baseType.name());
                    Assert.assertNull(endVal);
                    break;
                case "STRING":
                    //TODO
                    break;
                default:
                    System.out.println(baseType.name());
                    Assert.assertFalse(true); //we should not end up here, if it happen we forgot a case in the switch
            }

        }

    }

    @Test
    public void TestFitFileSettings2() {

        byte[] fileContents = GB.hexStringToByteArray("0e101405b90600002e464954b18b40000000000603048c04048601028402" +
                "028405028400010000ed2adce7ffffffff01001906ffff02410000310002" +
                "000284010102015401ff4200000200160104860204860001020301000401" +
                "000501010a01000b01000c01000d01020e01020f01021001001101001201" +
                "001501001601001a01001b01001d01003401003501000200000000000000" +
                "0000000000030002000032ffffff0100fe00000001430000030013000807" +
                "0402840101000201020301020501000601000701000801020a01020b0102" +
                "0c01000d01000e0100100100110100120100150100180102036564676535" +
                "3130000c030129b70000003cb9b901000001a80200ff440000040004fe02" +
                "8401028b00010203010a04000058c3010145000006002400040703048627" +
                "040a290c0afe028404028b05028b06028b07028b0802840902840a02840b" +
                "02842a028b0101000201000c01020d01020e01020f010210010211010212" +
                "010213010214010215010a16010a17010a18010a23030224010025010226" +
                "010a28010a2b010a2c01000545564f00849eb90227350000171513121110" +
                "0f0e0d0c0b00000000000000000001ba300800005000f4010000ffff0101" +
                "0000000001fe01000000050032ff04ff020b000046000006002400050703" +
                "048627040a290c0afe028404028b05028b06028b07028b0802840902840a" +
                "02840b02842a028b0101000201000c01020d01020e01020f010210010211" +
                "010212010213010214010215010a16010a17010a18010a23030224010025" +
                "010226010a28010a2b010a2c0100065032534c0000000000273500001715" +
                "131211100f0e0d0c0b000100000000000000316e300800005a00f4010000" +
                "ffff01010000000001fe01000000050076be04ff020b0000470000060024" +
                "00090703048627040a290c0afe028404028b05028b06028b07028b080284" +
                "0902840a02840b02842a028b0101000201000c01020d01020e01020f0102" +
                "10010211010212010213010214010215010a16010a17010a18010a230302" +
                "24010025010226010a28010a2b010a2c0100074c414e47535445520013cc" +
                "1200273500001715131211100f0e0d0c0b000200000000000000632a3008" +
                "00005f00f4010000ffff010100000000010001000000050032ff04ff020b" +
                "000048000006002400020703048627040a290c0afe028404028b05028b06" +
                "028b07028b0802840902840a02840b02842a028b0101000201000c01020d" +
                "01020e01020f010210010211010212010213010214010215010a16010a17" +
                "010a18010a23030224010025010226010a28010a2b010a2c0100084d0000" +
                "000000352700001715131211100f0e0d0c0b000300000000000000697a30" +
                "0800005f00f4010000ffff010100000000010001000000050032ff04ff02" +
                "0b000049000006002400070703048627040a290c0afe028404028b05028b" +
                "06028b07028b0802840902840a02840b02842a028b0101000201000c0102" +
                "0d01020e01020f010210010211010212010213010214010215010a16010a" +
                "17010a18010a23030224010025010226010a28010a2b010a2c0100094269" +
                "6b6520350000000000273500001715131211100f0e0d0c0b000400000000" +
                "0000000000300800005f00f4010000ffff01010000000000fe0000000000" +
                "0032ff04ff020b00000942696b6520360000000000273500001715131211" +
                "100f0e0d0c0b0005000000000000000000300800005f00f4010000ffff01" +
                "010000000000fe00000000000032ff04ff020b00000942696b6520370000" +
                "000000273500001715131211100f0e0d0c0b000600000000000000000030" +
                "0800005f00f4010000ffff01010000000000fe00000000000032ff04ff02" +
                "0b00000942696b6520380000000000273500001715131211100f0e0d0c0b" +
                "0007000000000000000000300800005f00f4010000ffff01010000000000" +
                "fe00000000000032ff04ff020b00000942696b6520390000000000273500" +
                "001715131211100f0e0d0c0b0008000000000000000000300800005f00f4" +
                "010000ffff01010000000000fe00000000000032ff04ff020b00004a0000" +
                "06002400080703048627040a290c0afe028404028b05028b06028b07028b" +
                "0802840902840a02840b02842a028b0101000201000c01020d01020e0102" +
                "0f010210010211010212010213010214010215010a16010a17010a18010a" +
                "23030224010025010226010a28010a2b010a2c01000a42696b6520313000" +
                "00000000273500001715131211100f0e0d0c0b0009000000000000000000" +
                "300800005f00f4010000ffff01010000000000fe00000000000032ff04ff" +
                "020b00004b00007f00090309070001000401000501000601000701000801" +
                "000901000a01000b45646765203531300000ffffffffffffff09ef");//https://github.com/polyvertex/fitdecode/blob/48b6554d8a3baf33f8b5b9b2fd079fcbe9ac8ce2/tests/files/Settings2.fit

        String expectedOutput = "{\n" +
                "Local Message: raw: 0 Global Message Number: FILE_ID=[\n" +
                "serial_number(UINT32Z/4): 3889965805 time_created(UINT32/4): null manufacturer(UINT16/2): 1 product(UINT16/2): 1561 number(UINT16/2): null type(ENUM/1): settings ], \n" +
                "Local Message: raw: 1 Global Message Number: FILE_CREATOR=[\n" +
                "software_version(UINT16/2): 340 hardware_version(UINT8/1): null ], \n" +
                "Local Message: raw: 2 Global Message Number: DEVICE_SETTINGS=[\n" +
                "utc_offset(UINT32/4): 0 time_offset(UINT32/4): 0 active_time_zone(UINT8/1): 0 unknown_3(ENUM/1): 0 time_mode(ENUM/1): 0 time_zone_offset(SINT8/1): 0 unknown_10(ENUM/1): 3 unknown_11(ENUM/1): 0 backlight_mode(ENUM/1): 2 unknown_13(UINT8/1): 0 unknown_14(UINT8/1): 0 unknown_15(UINT8/1): 50 unknown_16(ENUM/1): null unknown_17(ENUM/1): null unknown_18(ENUM/1): null unknown_21(ENUM/1): 1 unknown_22(ENUM/1): 0 unknown_26(ENUM/1): 254 unknown_27(ENUM/1): 0 unknown_29(ENUM/1): 0 unknown_52(ENUM/1): 0 unknown_53(ENUM/1): 1 ], \n" +
                "Local Message: raw: 3 Global Message Number: USER_PROFILE=[\n" +
                "friendly_name(STRING/8): edge510 weight(UINT16/2): 78 gender(ENUM/1): 1 age(UINT8/1): 41 height(UINT8/1): 183 language(ENUM/1): english elev_setting(ENUM/1): metric weight_setting(ENUM/1): metric resting_heart_rate(UINT8/1): 60 default_max_biking_heart_rate(UINT8/1): 185 default_max_heart_rate(UINT8/1): 185 hr_setting(ENUM/1): 1 speed_setting(ENUM/1): metric dist_setting(ENUM/1): metric power_setting(ENUM/1): 1 activity_class(ENUM/1): 168 position_setting(ENUM/1): 2 temperature_setting(ENUM/1): metric unknown_24(UINT8/1): null ], \n" +
                "Local Message: raw: 4 Global Message Number: UNK_4=[\n" +
                "unknown_254(UINT16/2): 0 unknown_1(UINT16Z/2): 50008 unknown_0(UINT8/1): 1 unknown_3(UINT8Z/1): 1 ], \n" +
                "Local Message: raw: 5 Global Message Number: UNK_6=[\n" +
                "unknown_0(STRING/4): EVO unknown_3(UINT32/4): 45719172 unknown_39(UINT8Z/4): [39,53,,] unknown_41(UINT8Z/12): [23,21,19,18,17,16,15,14,13,12,11,] unknown_254(UINT16/2): 0 unknown_4(UINT16Z/2): null unknown_5(UINT16Z/2): null unknown_6(UINT16Z/2): null unknown_7(UINT16Z/2): 47617 unknown_8(UINT16/2): 2096 unknown_9(UINT16/2): 0 unknown_10(UINT16/2): 80 unknown_11(UINT16/2): 500 unknown_42(UINT16Z/2): null unknown_1(ENUM/1): null unknown_2(ENUM/1): null unknown_12(UINT8/1): 1 unknown_13(UINT8/1): 1 unknown_14(UINT8/1): 0 unknown_15(UINT8/1): 0 unknown_16(UINT8/1): 0 unknown_17(UINT8/1): 0 unknown_18(UINT8/1): 1 unknown_19(UINT8/1): 254 unknown_20(UINT8/1): 1 unknown_21(UINT8Z/1): null unknown_22(UINT8Z/1): null unknown_23(UINT8Z/1): null unknown_24(UINT8Z/1): 5 unknown_35(UINT8/3): [0,50,] unknown_36(ENUM/1): 4 unknown_37(UINT8/1): null unknown_38(UINT8Z/1): 2 unknown_40(UINT8Z/1): 11 unknown_43(UINT8Z/1): null unknown_44(ENUM/1): 0 ], \n" +
                "Local Message: type: TODAY_WEATHER_CONDITIONS Global Message Number: UNK_6=[\n" +
                "unknown_0(STRING/5): P2SL unknown_3(UINT32/4): 0 unknown_39(UINT8Z/4): [39,53,,] unknown_41(UINT8Z/12): [23,21,19,18,17,16,15,14,13,12,11,] unknown_254(UINT16/2): 1 unknown_4(UINT16Z/2): null unknown_5(UINT16Z/2): null unknown_6(UINT16Z/2): null unknown_7(UINT16Z/2): 28209 unknown_8(UINT16/2): 2096 unknown_9(UINT16/2): 0 unknown_10(UINT16/2): 90 unknown_11(UINT16/2): 500 unknown_42(UINT16Z/2): null unknown_1(ENUM/1): null unknown_2(ENUM/1): null unknown_12(UINT8/1): 1 unknown_13(UINT8/1): 1 unknown_14(UINT8/1): 0 unknown_15(UINT8/1): 0 unknown_16(UINT8/1): 0 unknown_17(UINT8/1): 0 unknown_18(UINT8/1): 1 unknown_19(UINT8/1): 254 unknown_20(UINT8/1): 1 unknown_21(UINT8Z/1): null unknown_22(UINT8Z/1): null unknown_23(UINT8Z/1): null unknown_24(UINT8Z/1): 5 unknown_35(UINT8/3): [0,118,190] unknown_36(ENUM/1): 4 unknown_37(UINT8/1): null unknown_38(UINT8Z/1): 2 unknown_40(UINT8Z/1): 11 unknown_43(UINT8Z/1): null unknown_44(ENUM/1): 0 ], \n" +
                "Local Message: raw: 7 Global Message Number: UNK_6=[\n" +
                "unknown_0(STRING/9): LANGSTER unknown_3(UINT32/4): 1231891 unknown_39(UINT8Z/4): [39,53,,] unknown_41(UINT8Z/12): [23,21,19,18,17,16,15,14,13,12,11,] unknown_254(UINT16/2): 2 unknown_4(UINT16Z/2): null unknown_5(UINT16Z/2): null unknown_6(UINT16Z/2): null unknown_7(UINT16Z/2): 10851 unknown_8(UINT16/2): 2096 unknown_9(UINT16/2): 0 unknown_10(UINT16/2): 95 unknown_11(UINT16/2): 500 unknown_42(UINT16Z/2): null unknown_1(ENUM/1): null unknown_2(ENUM/1): null unknown_12(UINT8/1): 1 unknown_13(UINT8/1): 1 unknown_14(UINT8/1): 0 unknown_15(UINT8/1): 0 unknown_16(UINT8/1): 0 unknown_17(UINT8/1): 0 unknown_18(UINT8/1): 1 unknown_19(UINT8/1): 0 unknown_20(UINT8/1): 1 unknown_21(UINT8Z/1): null unknown_22(UINT8Z/1): null unknown_23(UINT8Z/1): null unknown_24(UINT8Z/1): 5 unknown_35(UINT8/3): [0,50,] unknown_36(ENUM/1): 4 unknown_37(UINT8/1): null unknown_38(UINT8Z/1): 2 unknown_40(UINT8Z/1): 11 unknown_43(UINT8Z/1): null unknown_44(ENUM/1): 0 ], \n" +
                "Local Message: raw: 8 Global Message Number: UNK_6=[\n" +
                "unknown_0(STRING/2): M unknown_3(UINT32/4): 0 unknown_39(UINT8Z/4): [53,39,,] unknown_41(UINT8Z/12): [23,21,19,18,17,16,15,14,13,12,11,] unknown_254(UINT16/2): 3 unknown_4(UINT16Z/2): null unknown_5(UINT16Z/2): null unknown_6(UINT16Z/2): null unknown_7(UINT16Z/2): 31337 unknown_8(UINT16/2): 2096 unknown_9(UINT16/2): 0 unknown_10(UINT16/2): 95 unknown_11(UINT16/2): 500 unknown_42(UINT16Z/2): null unknown_1(ENUM/1): null unknown_2(ENUM/1): null unknown_12(UINT8/1): 1 unknown_13(UINT8/1): 1 unknown_14(UINT8/1): 0 unknown_15(UINT8/1): 0 unknown_16(UINT8/1): 0 unknown_17(UINT8/1): 0 unknown_18(UINT8/1): 1 unknown_19(UINT8/1): 0 unknown_20(UINT8/1): 1 unknown_21(UINT8Z/1): null unknown_22(UINT8Z/1): null unknown_23(UINT8Z/1): null unknown_24(UINT8Z/1): 5 unknown_35(UINT8/3): [0,50,] unknown_36(ENUM/1): 4 unknown_37(UINT8/1): null unknown_38(UINT8Z/1): 2 unknown_40(UINT8Z/1): 11 unknown_43(UINT8Z/1): null unknown_44(ENUM/1): 0 ], \n" +
                "Local Message: type: HOURLY_WEATHER_FORECAST Global Message Number: UNK_6=[\n" +
                "unknown_0(STRING/7): Bike 5 unknown_3(UINT32/4): 0 unknown_39(UINT8Z/4): [39,53,,] unknown_41(UINT8Z/12): [23,21,19,18,17,16,15,14,13,12,11,] unknown_254(UINT16/2): 4 unknown_4(UINT16Z/2): null unknown_5(UINT16Z/2): null unknown_6(UINT16Z/2): null unknown_7(UINT16Z/2): null unknown_8(UINT16/2): 2096 unknown_9(UINT16/2): 0 unknown_10(UINT16/2): 95 unknown_11(UINT16/2): 500 unknown_42(UINT16Z/2): null unknown_1(ENUM/1): null unknown_2(ENUM/1): null unknown_12(UINT8/1): 1 unknown_13(UINT8/1): 1 unknown_14(UINT8/1): 0 unknown_15(UINT8/1): 0 unknown_16(UINT8/1): 0 unknown_17(UINT8/1): 0 unknown_18(UINT8/1): 0 unknown_19(UINT8/1): 254 unknown_20(UINT8/1): 0 unknown_21(UINT8Z/1): null unknown_22(UINT8Z/1): null unknown_23(UINT8Z/1): null unknown_24(UINT8Z/1): null unknown_35(UINT8/3): [0,50,] unknown_36(ENUM/1): 4 unknown_37(UINT8/1): null unknown_38(UINT8Z/1): 2 unknown_40(UINT8Z/1): 11 unknown_43(UINT8Z/1): null unknown_44(ENUM/1): 0 , \n" +
                "unknown_0(STRING/7): Bike 6 unknown_3(UINT32/4): 0 unknown_39(UINT8Z/4): [39,53,,] unknown_41(UINT8Z/12): [23,21,19,18,17,16,15,14,13,12,11,] unknown_254(UINT16/2): 5 unknown_4(UINT16Z/2): null unknown_5(UINT16Z/2): null unknown_6(UINT16Z/2): null unknown_7(UINT16Z/2): null unknown_8(UINT16/2): 2096 unknown_9(UINT16/2): 0 unknown_10(UINT16/2): 95 unknown_11(UINT16/2): 500 unknown_42(UINT16Z/2): null unknown_1(ENUM/1): null unknown_2(ENUM/1): null unknown_12(UINT8/1): 1 unknown_13(UINT8/1): 1 unknown_14(UINT8/1): 0 unknown_15(UINT8/1): 0 unknown_16(UINT8/1): 0 unknown_17(UINT8/1): 0 unknown_18(UINT8/1): 0 unknown_19(UINT8/1): 254 unknown_20(UINT8/1): 0 unknown_21(UINT8Z/1): null unknown_22(UINT8Z/1): null unknown_23(UINT8Z/1): null unknown_24(UINT8Z/1): null unknown_35(UINT8/3): [0,50,] unknown_36(ENUM/1): 4 unknown_37(UINT8/1): null unknown_38(UINT8Z/1): 2 unknown_40(UINT8Z/1): 11 unknown_43(UINT8Z/1): null unknown_44(ENUM/1): 0 , \n" +
                "unknown_0(STRING/7): Bike 7 unknown_3(UINT32/4): 0 unknown_39(UINT8Z/4): [39,53,,] unknown_41(UINT8Z/12): [23,21,19,18,17,16,15,14,13,12,11,] unknown_254(UINT16/2): 6 unknown_4(UINT16Z/2): null unknown_5(UINT16Z/2): null unknown_6(UINT16Z/2): null unknown_7(UINT16Z/2): null unknown_8(UINT16/2): 2096 unknown_9(UINT16/2): 0 unknown_10(UINT16/2): 95 unknown_11(UINT16/2): 500 unknown_42(UINT16Z/2): null unknown_1(ENUM/1): null unknown_2(ENUM/1): null unknown_12(UINT8/1): 1 unknown_13(UINT8/1): 1 unknown_14(UINT8/1): 0 unknown_15(UINT8/1): 0 unknown_16(UINT8/1): 0 unknown_17(UINT8/1): 0 unknown_18(UINT8/1): 0 unknown_19(UINT8/1): 254 unknown_20(UINT8/1): 0 unknown_21(UINT8Z/1): null unknown_22(UINT8Z/1): null unknown_23(UINT8Z/1): null unknown_24(UINT8Z/1): null unknown_35(UINT8/3): [0,50,] unknown_36(ENUM/1): 4 unknown_37(UINT8/1): null unknown_38(UINT8Z/1): 2 unknown_40(UINT8Z/1): 11 unknown_43(UINT8Z/1): null unknown_44(ENUM/1): 0 , \n" +
                "unknown_0(STRING/7): Bike 8 unknown_3(UINT32/4): 0 unknown_39(UINT8Z/4): [39,53,,] unknown_41(UINT8Z/12): [23,21,19,18,17,16,15,14,13,12,11,] unknown_254(UINT16/2): 7 unknown_4(UINT16Z/2): null unknown_5(UINT16Z/2): null unknown_6(UINT16Z/2): null unknown_7(UINT16Z/2): null unknown_8(UINT16/2): 2096 unknown_9(UINT16/2): 0 unknown_10(UINT16/2): 95 unknown_11(UINT16/2): 500 unknown_42(UINT16Z/2): null unknown_1(ENUM/1): null unknown_2(ENUM/1): null unknown_12(UINT8/1): 1 unknown_13(UINT8/1): 1 unknown_14(UINT8/1): 0 unknown_15(UINT8/1): 0 unknown_16(UINT8/1): 0 unknown_17(UINT8/1): 0 unknown_18(UINT8/1): 0 unknown_19(UINT8/1): 254 unknown_20(UINT8/1): 0 unknown_21(UINT8Z/1): null unknown_22(UINT8Z/1): null unknown_23(UINT8Z/1): null unknown_24(UINT8Z/1): null unknown_35(UINT8/3): [0,50,] unknown_36(ENUM/1): 4 unknown_37(UINT8/1): null unknown_38(UINT8Z/1): 2 unknown_40(UINT8Z/1): 11 unknown_43(UINT8Z/1): null unknown_44(ENUM/1): 0 , \n" +
                "unknown_0(STRING/7): Bike 9 unknown_3(UINT32/4): 0 unknown_39(UINT8Z/4): [39,53,,] unknown_41(UINT8Z/12): [23,21,19,18,17,16,15,14,13,12,11,] unknown_254(UINT16/2): 8 unknown_4(UINT16Z/2): null unknown_5(UINT16Z/2): null unknown_6(UINT16Z/2): null unknown_7(UINT16Z/2): null unknown_8(UINT16/2): 2096 unknown_9(UINT16/2): 0 unknown_10(UINT16/2): 95 unknown_11(UINT16/2): 500 unknown_42(UINT16Z/2): null unknown_1(ENUM/1): null unknown_2(ENUM/1): null unknown_12(UINT8/1): 1 unknown_13(UINT8/1): 1 unknown_14(UINT8/1): 0 unknown_15(UINT8/1): 0 unknown_16(UINT8/1): 0 unknown_17(UINT8/1): 0 unknown_18(UINT8/1): 0 unknown_19(UINT8/1): 254 unknown_20(UINT8/1): 0 unknown_21(UINT8Z/1): null unknown_22(UINT8Z/1): null unknown_23(UINT8Z/1): null unknown_24(UINT8Z/1): null unknown_35(UINT8/3): [0,50,] unknown_36(ENUM/1): 4 unknown_37(UINT8/1): null unknown_38(UINT8Z/1): 2 unknown_40(UINT8Z/1): 11 unknown_43(UINT8Z/1): null unknown_44(ENUM/1): 0 ], \n" +
                "Local Message: type: DAILY_WEATHER_FORECAST Global Message Number: UNK_6=[\n" +
                "unknown_0(STRING/8): Bike 10 unknown_3(UINT32/4): 0 unknown_39(UINT8Z/4): [39,53,,] unknown_41(UINT8Z/12): [23,21,19,18,17,16,15,14,13,12,11,] unknown_254(UINT16/2): 9 unknown_4(UINT16Z/2): null unknown_5(UINT16Z/2): null unknown_6(UINT16Z/2): null unknown_7(UINT16Z/2): null unknown_8(UINT16/2): 2096 unknown_9(UINT16/2): 0 unknown_10(UINT16/2): 95 unknown_11(UINT16/2): 500 unknown_42(UINT16Z/2): null unknown_1(ENUM/1): null unknown_2(ENUM/1): null unknown_12(UINT8/1): 1 unknown_13(UINT8/1): 1 unknown_14(UINT8/1): 0 unknown_15(UINT8/1): 0 unknown_16(UINT8/1): 0 unknown_17(UINT8/1): 0 unknown_18(UINT8/1): 0 unknown_19(UINT8/1): 254 unknown_20(UINT8/1): 0 unknown_21(UINT8Z/1): null unknown_22(UINT8Z/1): null unknown_23(UINT8Z/1): null unknown_24(UINT8Z/1): null unknown_35(UINT8/3): [0,50,] unknown_36(ENUM/1): 4 unknown_37(UINT8/1): null unknown_38(UINT8Z/1): 2 unknown_40(UINT8Z/1): 11 unknown_43(UINT8Z/1): null unknown_44(ENUM/1): 0 ], \n" +
                "Local Message: raw: 11 Global Message Number: CONNECTIVITY=[\n" +
                "name(STRING/9): Edge 510 bluetooth_enabled(ENUM/1): 0 live_tracking_enabled(ENUM/1): null weather_conditions_enabled(ENUM/1): null weather_alerts_enabled(ENUM/1): null auto_activity_upload_enabled(ENUM/1): null course_download_enabled(ENUM/1): null workout_download_enabled(ENUM/1): null gps_ephemeris_download_enabled(ENUM/1): null ]}";

        FitFile fitFile = FitFile.parseIncoming(fileContents);
        Assert.assertEquals(expectedOutput, fitFile.toString());

    }

    @Test
    public void TestFitFileDevelopersField() {
        byte[] fileContents = GB.hexStringToByteArray("0e206806a20000002e464954bed040000100000401028400010002028403048c00000f042329000006a540000100cf0201100d030102000101020305080d1522375990e97962db0040000100ce05000102010102020102031107080a0700000001646f7567686e7574735f6561726e656400646f7567686e7574730060000100140403010204010205048606028401000100008c580000c738b98001008f5a00032c808e400200905c0005a9388a1003d39e");//https://github.com/polyvertex/fitdecode/blob/48b6554d8a3baf33f8b5b9b2fd079fcbe9ac8ce2/tests/files/DeveloperData.fit

        String expectedOutput = "{\n" +
                "Local Message: raw: 0 Global Message Number: FILE_ID=[\n" +
                "manufacturer(UINT16/2): 15 type(ENUM/1): activity product(UINT16/2): 9001 serial_number(UINT32Z/4): 1701 ], \n" +
                "Local Message: raw: 0 Global Message Number: DEVELOPER_DATA=[\n" +
                "application_id(BASE_TYPE_BYTE/16): [1,1,2,3,5,8,13,21,34,55,89,144,233,121,98,219] developer_data_index(UINT8/1): 0 ], \n" +
                "Local Message: raw: 0 Global Message Number: FIELD_DESCRIPTION=[\n" +
                "developer_data_index(UINT8/1): 0 field_definition_number(UINT8/1): 0 fit_base_type_id(UINT8/1): 1 field_name(STRING/17): doughnuts_earned units(STRING/10): doughnuts ], \n" +
                "Local Message: raw: 0 Global Message Number: RECORD=[\n" +
                "heart_rate(UINT8/1): 140 unknown_4(UINT8/1): 88 unknown_5(UINT32/4): 51000 unknown_6(UINT16/2): 47488 doughnuts_earned(SINT8/1): 1 , \n" +
                "heart_rate(UINT8/1): 143 unknown_4(UINT8/1): 90 unknown_5(UINT32/4): 208000 unknown_6(UINT16/2): 36416 doughnuts_earned(SINT8/1): 2 , \n" +
                "heart_rate(UINT8/1): 144 unknown_4(UINT8/1): 92 unknown_5(UINT32/4): 371000 unknown_6(UINT16/2): 35344 doughnuts_earned(SINT8/1): 3 ]}";

        FitFile fitFile = FitFile.parseIncoming(fileContents);
        Assert.assertEquals(expectedOutput, fitFile.toString());
    }
}
