package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.communicator;

import org.junit.Assert;
import org.junit.Test;

import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class CobsCoDecTest {
    final CobsCoDec cobsCoDec = new CobsCoDec();

    @Test
    public void testCobsDecoder() {
        cobsCoDec.receivedBytes(GB.hexStringToByteArray("00022C04A0139623310F684C1BCA840508020B"));
        Assert.assertNull(cobsCoDec.retrieveMessage());
        cobsCoDec.receivedBytes(GB.hexStringToByteArray("496E7374696E637420325308496E7374696E63"));
        Assert.assertNull(cobsCoDec.retrieveMessage());
        cobsCoDec.receivedBytes(GB.hexStringToByteArray("74023253010304B800"));
        Assert.assertArrayEquals(
                GB.hexStringToByteArray("2C00A0139600310F684C1BCA840508020B496E7374696E637420325308496E7374696E6374023253000004B8"),
                cobsCoDec.retrieveMessage()
        );
    }

    @Test
    public void testCobsDecoder2() {
        cobsCoDec.receivedBytes(GB.hexStringToByteArray("00022b058813a013029623ffffffffffffa71fffff046c61726a07756e6b6e6f776e0758512d4343373201f9cf00"));
        Assert.assertArrayEquals(
                new byte[]{0x2b, 0x00, (byte) 0x88, 0x13, (byte) 0xa0, 0x13, 0x00, (byte) 0x96, 0x00, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xa7, 0x1f, (byte) 0xff, (byte) 0xff, 0x04, 0x6c, 0x61, 0x72, 0x6a, 0x07, 0x75, 0x6e, 0x6b, 0x6e, 0x6f, 0x77, 0x6e, 0x07, 0x58, 0x51, 0x2d, 0x43, 0x43, 0x37, 0x32, 0x01, (byte) 0xf9, (byte) 0xcf},
                cobsCoDec.retrieveMessage()
        );
    }

    @Test
    public void testCobsEncoder2() {
        byte[] result = cobsCoDec.encode(GB.hexStringToByteArray("022b058813a013029623ffffffffffffa71fffff046c61726a07756e6b6e6f776e0758512d4343373201f9cf00"));
        Assert.assertArrayEquals(
                new byte[]{0x00, 0x2d, (byte) 0x02, (byte) 0x2b, (byte) 0x05, (byte) 0x88, (byte) 0x13, (byte) 0xa0, (byte) 0x13, (byte) 0x02, (byte) 0x96, (byte) 0x23, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xa7, (byte) 0x1f, (byte) 0xff, (byte) 0xff, (byte) 0x04, (byte) 0x6c, (byte) 0x61, (byte) 0x72, (byte) 0x6a, (byte) 0x07, (byte) 0x75, (byte) 0x6e, (byte) 0x6b, (byte) 0x6e, (byte) 0x6f, (byte) 0x77, (byte) 0x6e, (byte) 0x07, (byte) 0x58, (byte) 0x51, (byte) 0x2d, (byte) 0x43, (byte) 0x43, (byte) 0x37, (byte) 0x32, (byte) 0x01, (byte) 0xf9, (byte) 0xcf, (byte) 0x01, (byte) 0x00},
                result
        );
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
        Assert.assertArrayEquals(
                GB.hexStringToByteArray("2C00A0139600310F684C1BCA840508020B496E7374696E637420325308496E7374696E6374023253000004B8"),
                cobsCoDec.retrieveMessage()
        );
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
        Assert.assertArrayEquals(
                GB.hexStringToByteArray("2C00A0139600310F684C1BCA840508020B496E7374696E637420325308496E7374696E6374023253000004B8"),
                cobsCoDec.retrieveMessage()
        );
    }

    @Test
    public void testCobsEncoder() {
        Assert.assertArrayEquals(
                GB.hexStringToByteArray("00022C04A0139623310F684C1BCA840508020B496E7374696E637420325308496E7374696E6374023253010304B800"),
                cobsCoDec.encode(GB.hexStringToByteArray("2C00A0139600310F684C1BCA840508020B496E7374696E637420325308496E7374696E6374023253000004B8"))
        );
    }

    @Test
    public void testLongPayload() {
        //test strings from https://github.com/themarpe/cobs-java/blob/master/tests-java/Tests.java
        final byte[] test_string_0 = new byte[]{0, 0, 0, 0};
        final byte[] test_string_1 = new byte[]{0, '1', '2', '3', '4', '5'};
        final byte[] test_string_2 = new byte[]{0, '1', '2', '3', '4', '5', 0};
        final byte[] test_string_3 = new byte[]{'1', '2', '3', '4', '5', 0, '6', '7', '8', '9'};
        final byte[] test_string_4 = new byte[]{0, '1', '2', '3', '4', '5', 0, '6', '7', '8', '9', 0};
        final byte[] test_string_5 = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127, -128, -127, -126, -125, -124, -123, -122, -121, -120, -119, -118, -117, -116, -115, -114, -113, -112, -111, -110, -109, -108, -107, -106, -105, -104, -103, -102, -101, -100, -99, -98, -97, -96, -95, -94, -93, -92, -91, -90, -89, -88, -87, -86, -85, -84, -83, -82, -81, -80, -79, -78, -77, -76, -75, -74, -73, -72, -71, -70, -69, -68, -67, -66, -65, -64, -63, -62, -61, -60, -59, -58, -57, -56, -55, -54, -53, -52, -51, -50, -49, -48, -47, -46, -45, -44, -43, -42, -41, -40, -39, -38, -37, -36, -35, -34, -33, -32, -31, -30, -29, -28, -27, -26, -25, -24, -23, -22, -21, -20, -19, -18, -17, -16, -15, -14, -13, -12, -11, -10, -9, -8, -7, -6, -5, -4, -3, -2, -1, 0};
        final byte[] test_string_6 = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127, -128, -127, -126, -125, -124, -123, -122, -121, -120, -119, -118, -117, -116, -115, -114, -113, -112, -111, -110, -109, -108, -107, -106, -105, -104, -103, -102, -101, -100, -99, -98, -97, -96, -95, -94, -93, -92, -91, -90, -89, -88, -87, -86, -85, -84, -83, -82, -81, -80, -79, -78, -77, -76, -75, -74, -73, -72, -71, -70, -69, -68, -67, -66, -65, -64, -63, -62, -61, -60, -59, -58, -57, -56, -55, -54, -53, -52, -51, -50, -49, -48, -47, -46, -45, -44, -43, -42, -41, -40, -39, -38, -37, -36, -35, -34, -33, -32, -31, -30, -29, -28, -27, -26, -25, -24, -23, -22, -21, -20, -19, -18, -17, -16, -15, -14, -13, -12, -11, -10, -9, -8, -7, -6, -5, -4, -3, -2, -1, 0};
        final byte[] test_string_7 = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127, -128, -127, -126, -125, -124, -123, -122, -121, -120, -119, -118, -117, -116, -115, -114, -113, -112, -111, -110, -109, -108, -107, -106, -105, -104, -103, -102, -101, -100, -99, -98, -97, -96, -95, -94, -93, -92, -91, -90, -89, -88, -87, -86, -85, -84, -83, -82, -81, -80, -79, -78, -77, -76, -75, -74, -73, -72, -71, -70, -69, -68, -67, -66, -65, -64, -63, -62, -61, -60, -59, -58, -57, -56, -55, -54, -53, -52, -51, -50, -49, -48, -47, -46, -45, -44, -43, -42, -41, -40, -39, -38, -37, -36, -35, -34, -33, -32, -31, -30, -29, -28, -27, -26, -25, -24, -23, -22, -21, -20, -19, -18, -17, -16, -15, -14, -13, -12, -11, -10, -9, -8, -7, -6, -5, -4, -3, -2, -1};
        final byte[] test_string_8 = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127, -128, -127, -126, -125, -124, -123, -122, -121, -120, -119, -118, -117, -116, -115, -114, -113, -112, -111, -110, -109, -108, -107, -106, -105, -104, -103, -102, -101, -100, -99, -98, -97, -96, -95, -94, -93, -92, -91, -90, -89, -88, -87, -86, -85, -84, -83, -82, -81, -80, -79, -78, -77, -76, -75, -74, -73, -72, -71, -70, -69, -68, -67, -66, -65, -64, -63, -62, -61, -60, -59, -58, -57, -56, -55, -54, -53, -52, -51, -50, -49, -48, -47, -46, -45, -44, -43, -42, -41, -40, -39, -38, -37, -36, -35, -34, -33, -32, -31, -30, -29, -28, -27, -26, -25, -24, -23, -22, -21, -20, -19, -18, -17, -16, -15, -14, -13, -12, -11, -10, -9, -8, -7, -6, -5, -4, -3, -2, -1, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127, -128, -127, -126, -125, -124, -123, -122, -121, -120, -119, -118, -117, -116, -115, -114, -113, -112, -111, -110, -109, -108, -107, -106, -105, -104, -103, -102, -101, -100, -99, -98, -97, -96, -95, -94, -93, -92, -91, -90, -89, -88, -87, -86, -85, -84, -83, -82, -81, -80, -79, -78, -77, -76, -75, -74, -73, -72, -71, -70, -69, -68, -67, -66, -65, -64, -63, -62, -61, -60, -59, -58, -57, -56, -55, -54, -53, -52, -51, -50, -49, -48, -47, -46, -45, -44, -43, -42, -41, -40, -39, -38, -37, -36, -35, -34, -33, -32, -31, -30, -29, -28, -27, -26, -25, -24, -23, -22, -21, -20, -19, -18, -17, -16, -15, -14, -13, -12, -11, -10, -9, -8, -7, -6, 0, -5, -4, -3, -2, -1};
        final byte[] test_string_long_nonzero = GB.hexStringToByteArray("49535a73394d73483159456f515352443069546e466c4b32394d3479336f396a6936543263544954794e576b456f4e365769704734444a6b4c6439657a6256386b30676f4e544e587954326e596e617567334f546449376c535471506e724e4444326664774d7a3142725653596c4838794b524b50327a5a3438704961545457384b483571744a6c726948704552364b7a466c54315776337a373954524942467442784c3062486f6c386b786a48377750726f5277766546757a596876533731726e6972344e644f70475a6c6a4c65753371554545396a4f556974703655774b426b34575970754e4f484b6349364f425468334c753532324b66abababab000B");
        final byte[][] allTests = new byte[][]{
                test_string_1,
                test_string_2,
                test_string_3,
                test_string_4,
                test_string_5,
                test_string_6,
                test_string_7,
                test_string_8,
                test_string_long_nonzero,
        };

        for (byte[] payload : allTests) {
            byte[] encodedData = cobsCoDec.encode(payload);
            cobsCoDec.receivedBytes(encodedData);
            byte[] decodedData = cobsCoDec.retrieveMessage();

            Assert.assertArrayEquals(payload, decodedData);
        }
    }
}
