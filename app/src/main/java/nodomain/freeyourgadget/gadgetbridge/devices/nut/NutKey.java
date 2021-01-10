/*  Copyright (C) 2020-2021 Taavi Eomäe

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
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.devices.nut;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.AbstractMap;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class NutKey {
    private static final Logger LOG = LoggerFactory.getLogger(NutKey.class);

    /**
     * Different from {@link GB#hexStringToByteArray} because
     * it returns an array of zero bytes when it's given zero bytes
     * <p>
     * https://stackoverflow.com/a/140430/4636860
     *
     * @param encoded hexadecimal string like "0xcafebabe", "DEADBEEF" or "feeddead"
     * @return resulting byte array
     */
    public static byte[] hexStringToByteArrayNut(String encoded) {
        if (encoded.startsWith("0x")) {
            encoded = encoded.substring(2);
        }

        if ((encoded.length() % 2) != 0) {
            throw new IllegalArgumentException("Input string must contain an even number of characters");
        }

        final byte[] result = new byte[encoded.length() / 2];
        final char[] enc = encoded.toCharArray();
        for (int i = 0; i < enc.length; i += 2) {
            result[i / 2] = (byte) Integer.parseInt(String.valueOf(enc[i]) + enc[i + 1], 16);
        }
        return result;
    }

    /**
     * Returns the array as hexadecimal string space delimited
     *
     * @param bytes bytes to return
     * @return returns
     */
    public static String bytesToHex2(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 3];
        for (int j = 0; j < bytes.length; j++) {
            hexChars[j * 3] = GB.HEX_CHARS[(bytes[j] & 0xFF) >>> 4];
            hexChars[j * 3 + 1] = GB.HEX_CHARS[(bytes[j] & 0xFF) & 0x0F];
            hexChars[j * 3 + 2] = " ".toCharArray()[0];
        }
        return new String(hexChars).toLowerCase();
    }

    /**
     * Generates an assembled packet based on inputs
     *
     * @param mac       the mac of the target device (AA:BB:CC:DD:EE:FF)
     * @param challenge the received challenge, THE ENTIRE PAYLOAD WITH PREAMBLE!
     * @param key1      first key
     * @param key2      second key
     * @return assembled packet (without the preamble!)
     */
    public static byte[] passwordGeneration(String mac, byte[] challenge, BigInteger key1, BigInteger key2) {
        if (challenge[0] != 0x01) {
            throw new IllegalArgumentException("Challenge must be given with the preamble");
        }

        byte[] mac_as_bytes = macAsByteArray(mac);
        byte[] correct_challenge = new byte[challenge.length - 1];
        System.arraycopy(challenge, 1, correct_challenge, 0, challenge.length - 1);
        ArrayUtils.reverse(correct_challenge);
        BigInteger c = new BigInteger(1, mac_as_bytes).add(new BigInteger(1, correct_challenge));

        BigInteger max64 = BigInteger.ONE.add(BigInteger.ONE).pow(64).subtract(BigInteger.ONE);
        BigInteger tmp1 = key2.xor(max64);
        BigInteger result1;
        if (c.compareTo(tmp1) > 0) {
            result1 = c.add(key1).subtract(tmp1);
        } else {
            result1 = key1;
        }

        BigInteger result2;
        if (key2.remainder(BigInteger.ONE.add(BigInteger.ONE)).compareTo(BigInteger.ONE) == 0) {
            result2 = key2.add(c);
        } else {
            result2 = key2.multiply(BigInteger.ONE.add(BigInteger.ONE)).add(c);
        }

        return byteArraysConcatReverseWithPad(result1.toByteArray(), result2.toByteArray());
    }

    /**
     * Reverses the password generation into keys
     * <p>
     * This assumes you have:
     * The MAC of the device, the challenge and response payload
     * <p>
     * See also {@link NutKey#passwordGeneration}
     *
     * @param challenge the RECEIVED and COMPLETE payload (truncated accordingly)
     * @param response  the SENT and COMPLETE payload
     * @param deviceMac colon-separated MAC address of the Nut as a string
     */
    public static Map.Entry<BigInteger, BigInteger> reversePasswordGeneration(byte[] challenge,
                                                                              byte[] response,
                                                                              String deviceMac) {
        // The two arrays that were concat. with byteArraysConcatReverseWithPad(orig1, orig2)
        byte[] original1 = new byte[8];
        byte[] original2 = new byte[8];

        // The response without preamble
        if (response[0] != 0x02) {
            throw new IllegalArgumentException("Response always begins with 0x02");
        }
        byte[] cleanResponse = new byte[16];
        System.arraycopy(response, 1, cleanResponse, 0, cleanResponse.length);

        // The challenge without preamble
        byte[] cleanChall = new byte[4];
        System.arraycopy(challenge, 1, cleanChall, 0, cleanChall.length);

        // Reverse the two arrays sent as a response
        byteArraysDeConcatReverseWithPad(cleanResponse, original1, original2);

        // Two common components in the equation
        BigInteger a = new BigInteger(1, macAsByteArray(deviceMac));
        byte[] cleanChallTmp = cleanChall.clone();
        ArrayUtils.reverse(cleanChallTmp);
        BigInteger b = new BigInteger(1, cleanChallTmp);
        BigInteger c = a.add(b);
        BigInteger max64 = BigInteger.ONE.add(BigInteger.ONE).pow(64).subtract(BigInteger.ONE);

        // We don't know actual keys used yet
        // There's two possibilities,
        // either it's directly what's in the packet
        // orig1 = key1
        BigInteger key1a = new BigInteger(1, original1);
        // Or it's derived from key2 using this formula :S
        // orig1 = c + key1 - (key2 XOR (2^64 - 1)
        // see below when it might be needed

        // It's either just
        // orig2 = key2 + c
        BigInteger key2a = new BigInteger(1, original2).subtract(c);
        // alternatively
        // orig2 = 2 * key2 + c
        BigInteger key2b = new BigInteger(1, original2).multiply(BigInteger.ONE.add(BigInteger.ONE)).subtract(c);

        // Now we have key2a, key2b, key1a,
        // trying to determine if we can do with just those,
        // or need to continue
        byte[] key1a2aresult = passwordGeneration(deviceMac, challenge, key1a, key2a);
        LOG.debug("Result1a2a:02 %s\n", bytesToHex2(key1a2aresult));

        if (java.util.Arrays.equals(key1a2aresult, cleanResponse)) {
            LOG.debug("Found key1a & key2a are correct, DONE!");
            return new AbstractMap.SimpleEntry<>(key1a, key2a);
        }
        // Unsuccessful, let's try key1a with key2b

        byte[] key1a2bresult = passwordGeneration(deviceMac, challenge, key1a, key2b);
        LOG.debug("Result1a2b:02 %s\n", bytesToHex2(key1a2bresult));

        if (java.util.Arrays.equals(key1a2bresult, cleanResponse)) {
            LOG.debug("Found key1a & key2b are correct, DONE!");
            return new AbstractMap.SimpleEntry<>(key1a, key2b);
        }

        // If we're still not done, we have to calculate two possible key1b-s
        // one for key2a, other for key2b
        // key1 = c + (key2 XOR (2^64 - 1) + orig1
        BigInteger key1b2a = c.add(key2a.xor(max64)).add(new BigInteger(1, original1));
        byte[] key1b2aresult = passwordGeneration(deviceMac, challenge, key1b2a, key2a);
        LOG.debug("Result1b2a:02 %s\n", bytesToHex2(key1b2aresult));

        if (java.util.Arrays.equals(key1b2aresult, cleanResponse)) {
            LOG.debug("Found key1b2a & key2b are correct, DONE!");
            return new AbstractMap.SimpleEntry<>(key1b2a, key2b);
        }

        BigInteger key1b2b = c.add(key2b.xor(max64)).add(new BigInteger(1, original1));
        byte[] key1b2bresult = passwordGeneration(deviceMac, challenge, key1b2b, key2b);
        LOG.debug("Result1b2b:02 %s\n", bytesToHex2(key1b2bresult));

        if (java.util.Arrays.equals(key1b2bresult, cleanResponse)) {
            LOG.debug("Found key1b2b & key2b are correct, DONE!");
            return new AbstractMap.SimpleEntry<>(key1b2b, key2b);
        }

        LOG.warn("Input might be incorrect, a correct key was not found");
        return null;
    }

    /**
     * Turns the MAC address into an array of bytes
     *
     * @param address MAC address
     * @return byte[] containing the MAC address bytes
     */
    public static byte[] macAsByteArray(String address) {
        //noinspection DynamicRegexReplaceableByCompiledPattern
        return hexStringToByteArrayNut(address.replace(":", ""));
    }

    /**
     * Concatenates two byte arrays in reverse and pads with zeros
     *
     * @param arr1 first array to concatenate
     * @param arr2 second array to concatenate
     * @return 16 bytes that contain the array in reverse, zeros if any parameter is empty
     */
    public static byte[] byteArraysConcatReverseWithPad(byte[] arr1, byte[] arr2) {
        byte[] result = new byte[16];
        for (int i = 0; i < Math.min(arr2.length, 8); i++) {
            // Reverse the array - 0-indexed - start shorter arrays from "0" - byte index to handle
            result[8 - 1 - (8 - Math.min(arr2.length, 8)) - i] = arr2[i + Math.max((arr2.length - 8), 0)];
        }
        for (int i = 0; i < Math.min(arr1.length, 8); i++) {
            result[16 - 1 - (8 - Math.min(arr1.length, 8)) - i] = arr1[i + Math.max((arr1.length - 8), 0)];
        }
        return result;
    }

    /**
     * De-concatenates two byte arrays in reverse,
     * places them in specified destinations,
     * <p>
     * 16 bytes that contain the array in reverse,
     * zeros if any parameter is empty
     *
     * @param input array to de-concatenate, 16 bytes
     */
    public static void byteArraysDeConcatReverseWithPad(byte[] input, byte[] dest1, byte[] dest2) {
        if (input.length != 16) {
            throw new IllegalArgumentException("Input must be 16 bytes!");
        }
        for (int i = 0; i < 8; i++) {
            dest2[8 - 1 - i] = input[i];
        }
        for (int i = 8; i < 16; i++) {
            dest1[16 - 1 - i] = input[i];
        }
    }
}
