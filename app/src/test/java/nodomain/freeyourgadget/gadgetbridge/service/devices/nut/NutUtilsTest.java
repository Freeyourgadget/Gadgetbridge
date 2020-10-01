/*  Copyright (C) 2020 Taavi Eomäe

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
    */
package nodomain.freeyourgadget.gadgetbridge.service.devices.nut;

import org.junit.Test;

import java.math.BigInteger;
import java.util.AbstractMap;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.fail;
import static nodomain.freeyourgadget.gadgetbridge.devices.nut.NutKey.byteArraysConcatReverseWithPad;
import static nodomain.freeyourgadget.gadgetbridge.devices.nut.NutKey.byteArraysDeConcatReverseWithPad;
import static nodomain.freeyourgadget.gadgetbridge.devices.nut.NutKey.bytesToHex2;
import static nodomain.freeyourgadget.gadgetbridge.devices.nut.NutKey.hexStringToByteArrayNut;
import static nodomain.freeyourgadget.gadgetbridge.devices.nut.NutKey.macAsByteArray;
import static nodomain.freeyourgadget.gadgetbridge.devices.nut.NutKey.passwordGeneration;
import static nodomain.freeyourgadget.gadgetbridge.devices.nut.NutKey.reversePasswordGeneration;

public class NutUtilsTest {
    @Test
    public void testPasswordGen() {
        String result = bytesToHex2(passwordGeneration(
                "00:00:00:00:00:00",
                new byte[]{1, 0, 0, 0, 0, 0, 0},
                BigInteger.ZERO,
                BigInteger.ZERO)
        );
        String expected = bytesToHex2(new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
        assertEquals(expected, result);
    }

    @Test
    public void testPasswordGen2() {
        String result = bytesToHex2(passwordGeneration(
                "00:00:00:00:00:00",
                new byte[]{1, 0, 0, 0, 0, 0, 0},
                BigInteger.ZERO,
                BigInteger.ZERO)
        );
        String expected = bytesToHex2(new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
        assertEquals(expected, result);
    }

    @Test
    public void testPasswordGen3() {
        String result = bytesToHex2(passwordGeneration(
                "00:00:00:00:00:00",
                new byte[]{1, 1, 0, 0, 0, 0, 0},
                BigInteger.ZERO,
                BigInteger.ZERO)
        );
        String expected = bytesToHex2(new byte[]{0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
        assertEquals(expected, result);
    }

    @Test
    public void testPasswordGen4() {
        String result = bytesToHex2(passwordGeneration(
                "00:00:00:00:00:00",
                new byte[]{1, 0, 0, 0, 0, 0, 0},
                new BigInteger(1, new byte[]{1, 0, 0, 0, 0, 0}),
                BigInteger.ZERO)
        );
        String expected = bytesToHex2(new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00});
        assertEquals(expected, result);
    }

    @Test
    public void testPasswordGen5() {
        String result = bytesToHex2(passwordGeneration(
                "00:00:00:00:00:00",
                new byte[]{1, 0, 0, 0, 0, 0, 0, 0, 0},
                BigInteger.ZERO,
                new BigInteger(1, new byte[]{1, 1, 1, 1, 1, 1, 0, 0}))
        );
        String expected = bytesToHex2(new byte[]{0x00, 0x00, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
        assertEquals(expected, result);
    }

    @Test
    public void testPasswordGen6() {
        String result = bytesToHex2(passwordGeneration(
                "01:02:03:04:05:06",
                new byte[]{1, 1, 2, 3, 4, 5, 6, 7, 8},
                BigInteger.ZERO,
                BigInteger.ZERO)
        );
        String expected = bytesToHex2(new byte[]{0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
        assertEquals(expected, result);
    }

    @Test
    public void testPasswordGen7() {
        String result = bytesToHex2(passwordGeneration(
                "01:02:03:04:05:06",
                new byte[]{1, 1, 2, 3, 4, 5, 6, 7, 8},
                new BigInteger(1, new byte[]{1, 2, 3, 4, 5, 6, 7, 8}),
                new BigInteger(1, new byte[]{1, 2, 3, 4, 5, 6, 7, 8}))
        );
        String expected = bytesToHex2(new byte[]{0x17, 0x15, 0x13, 0x11, 0x0f, 0x0d, 0x0b, 0x0a, 0x08, 0x07, 0x06, 0x05, 0x04, 0x03, 0x02, 0x01});
        assertEquals(expected, result);
    }

    @Test
    public void testReversePassword() {
        String challenge = "01e8f0340d000000000000000000000000";
        String response = "029bbd0fa25aed0000dcfd0c0000000000";
        String device_mac = "ED:5A:94:CB:98:E4";

        Map.Entry<BigInteger, BigInteger> key = reversePasswordGeneration(
                hexStringToByteArrayNut(challenge),
                hexStringToByteArrayNut(response),
                device_mac
        );

        assertNotNull(key);
        assertEquals(new AbstractMap.SimpleEntry<>(new BigInteger("851420"), new BigInteger("996303")), key);
    }

    @Test
    public void testPassword() {
        String challenge = "011a9b826c000000000000000000000000";
        String response = "02cd675d015bed0000dcfd0c0000000000";
        String device_mac = "ED:5A:94:CB:98:E4";

        Map.Entry<BigInteger, BigInteger> key = new AbstractMap.SimpleEntry<>(new BigInteger("851420"), new BigInteger("996303"));

        byte[] result = new byte[17];
        result[0] = 0x02;
        System.arraycopy(passwordGeneration(device_mac, hexStringToByteArrayNut(challenge), key.getKey(), key.getValue()), 0, result, 1, 16);

        assertEquals(bytesToHex2(hexStringToByteArrayNut(response)), bytesToHex2(result));
    }

    @Test
    public void testInvalidResponse() {
        String challenge = "00";
        String response = "00";
        String device_mac = "0:00:00:00:00:00";
        try {
            reversePasswordGeneration(
                    hexStringToByteArrayNut(challenge),
                    hexStringToByteArrayNut(response),
                    device_mac
            );
        } catch (IllegalArgumentException e) {
            // This is intended behaviour
            assertNotNull(e);
            return;
        }
        fail();
    }

    @Test
    public void testHexToByteArray() {
        byte[] arr = hexStringToByteArrayNut("0x0000000000");
        assertEquals(bytesToHex2(new byte[]{0x00, 0x00, 0x00, 0x00, 0x00}), bytesToHex2(arr));
    }

    @Test
    public void testHexToByteArray2() {
        byte[] arr = hexStringToByteArrayNut("cafebabe");
        assertEquals(bytesToHex2(new byte[]{(byte) 0xca, (byte) 0xfe, (byte) 0xba, (byte) 0xbe}), bytesToHex2(arr));
    }

    @Test
    public void testMACToByteArray() {
        byte[] arr = macAsByteArray("AA:BB:CC:DD:EE:FF");
        assertEquals(bytesToHex2(
                new byte[]{
                        (byte) 0xaa,
                        (byte) 0xbb,
                        (byte) 0xcc,
                        (byte) 0xdd,
                        (byte) 0xee,
                        (byte) 0xff
                }
        ), bytesToHex2(arr));
    }

    @Test
    public void testConcat() {
        byte[] src1 = new byte[]{1, 2, 3, 4, 5, 6, 7, 8};
        byte[] src2 = new byte[]{9, 10, 11, 12, 13, 14, 15, 16};
        assertEquals(
                bytesToHex2(new byte[]{0x10, 0x0f, 0x0e, 0x0d, 0x0c, 0x0b, 0x0a, 0x09, 0x08, 0x07, 0x06, 0x05, 0x04, 0x03, 0x02, 0x01}),
                bytesToHex2(byteArraysConcatReverseWithPad(
                        src1,
                        src2)
                )
        );
    }

    @Test
    public void testConcatDeConcat() {
        byte[] src1 = new byte[]{0, 0, 3, 4, 5, 6, 0, 0};
        byte[] src2 = new byte[]{0, 0, 7, 8, 9, 1, 0, 0};
        byte[] dst1 = new byte[8];
        byte[] dst2 = new byte[8];
        byteArraysDeConcatReverseWithPad(
                byteArraysConcatReverseWithPad(
                        src1,
                        src2
                ),
                dst1,
                dst2
        );
        assertEquals(bytesToHex2(src1), bytesToHex2(dst1));
        assertEquals(bytesToHex2(src2), bytesToHex2(dst2));
    }

    @Test
    public void testConcatDeConcat2() {
        byte[] src1 = new byte[]{1, 2, 3, 4, 5, 6, 7, 8};
        byte[] src2 = new byte[]{9, 10, 11, 12, 13, 14, 15, 16};
        byte[] dst1 = new byte[8];
        byte[] dst2 = new byte[8];
        byteArraysDeConcatReverseWithPad(
                byteArraysConcatReverseWithPad(
                        src1,
                        src2
                ),
                dst1,
                dst2
        );
        assertEquals(bytesToHex2(src1), bytesToHex2(dst1));
        assertEquals(bytesToHex2(src2), bytesToHex2(dst2));
    }
}
