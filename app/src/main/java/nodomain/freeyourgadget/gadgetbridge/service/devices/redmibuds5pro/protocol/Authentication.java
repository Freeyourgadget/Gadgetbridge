/*  Copyright (C) 2024 Jonathan Gobbo

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.redmibuds5pro.protocol;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/*
    Authentication based on the custom Bluetooth version of the SAFER+ encryption algorithm with:
    - 128 bit key size
    - 8 rounds
 */
public class Authentication {

    private final static int PATTERN = 0x9999;
    private final static int BLOCK_SIZE = 16;

    byte[][] biasMatrix;
    byte[] expTab;
    byte[] logTab;

    public Authentication() {
        generateBiasMatrix();
        generateExpTab();
        generateLogTab();
    }

    private void generateBiasMatrix() {
        biasMatrix = new byte[16][];

        for (int i = 0; i < 16; i++) {
            byte[] biasVec = new byte[16];
            for (int j = 0; j < 16; j++) {
                int exponent = (17 * (i + 2) + (j + 1));
                BigInteger base = BigInteger.valueOf(45);
                BigInteger modExp = base.pow(base.pow(exponent).mod(BigInteger.valueOf(257)).intValue())
                        .mod(BigInteger.valueOf(257));
                byte val = (byte) (modExp.intValue() == 256 ? 0 : modExp.intValue());
                biasVec[j] = val;
            }
            biasMatrix[i] = biasVec;
        }
    }

    private void generateExpTab() {
        expTab = new byte[256];

        for (int i = 0; i < 256; i++) {
            BigInteger base = BigInteger.valueOf(45);
            BigInteger exp = base.pow(i).mod(BigInteger.valueOf(257));
            expTab[i] = (byte) (i == 128 ? 0 : exp.intValue());
        }
    }

    private void generateLogTab() {
        logTab = new byte[256];

        for (int i = 0; i < 256; i++) {
            if (i == 0) {
                logTab[i] = (byte) 128;
            } else {
                BigInteger base = BigInteger.valueOf(45);
                BigInteger modExp = base.pow(i).mod(BigInteger.valueOf(257));
                if (modExp.intValue() != 256) {
                    logTab[modExp.intValue()] = (byte) i;
                }
            }
        }
    }

    private byte[][] keySchedule(byte[] keyInit) {

        keyInit[15] ^= 6;

        byte[][] keys = new byte[17][];
        keys[0] = keyInit;

        List<Byte> register = new ArrayList<>();
        for (byte b : keyInit) {
            register.add(b);
        }
        byte xor = register.stream().reduce((byte) 0x0, (cSum, e) -> (byte) (cSum ^ e));
        register.add(xor);

        for (int keyIdx = 1; keyIdx < keys.length; keyIdx++) {
            byte[] keyI = new byte[16];
            for (int i = 0; i < 17; i++) {
                byte rot = (byte) (((register.get(i) & 0xff) >>> 5) | ((register.get(i) & 0xff) << (8 - 5)));
                register.set(i, rot);
            }
            for (int i = 0; i < 16; i++) {
                keyI[i] = (byte) (register.get((keyIdx + i) % 17) + biasMatrix[keyIdx - 1][i]);
            }
            keys[keyIdx] = keyI;
        }

        return keys;
    }

    private byte[] encrypt(byte[] plaintext, byte[][] keys) {

        byte[] ciphertext = plaintext.clone();
        for (int round = 0; round < 8; round++) {
            if (round == 2) {
                for (int i = 0; i < BLOCK_SIZE; i++) {
                    if ((1 << i & PATTERN) != 0) {
                        ciphertext[i] ^= plaintext[i];
                    } else {
                        ciphertext[i] += plaintext[i];
                    }
                }
            }
            for (int i = 0; i < BLOCK_SIZE; i++) {
                if ((1 << i & PATTERN) != 0) {
                    ciphertext[i] ^= keys[round * 2][i];
                } else {
                    ciphertext[i] += keys[round * 2][i];
                }
            }
            for (int i = 0; i < BLOCK_SIZE; i++) {
                if ((1 << i & PATTERN) != 0) {
                    ciphertext[i] = expTab[ciphertext[i] & 0xff];
                } else {
                    ciphertext[i] = logTab[ciphertext[i] & 0xff];
                }
            }
            for (int i = 0; i < BLOCK_SIZE; i++) {
                if ((1 << i & PATTERN) != 0) {
                    ciphertext[i] = (byte) (keys[round * 2 + 1][i] + ciphertext[i]);
                } else {
                    ciphertext[i] = (byte) (keys[round * 2 + 1][i] ^ ciphertext[i]);
                }
            }
            byte[] ciphertextCopy = ciphertext.clone();
            for (int i = 0; i < BLOCK_SIZE; i++) {
                byte cSum = 0;
                for (int j = 0; j < BLOCK_SIZE; j++) {
                    cSum += (byte) (AuthData.COEFFICIENTS[i][j] * ciphertextCopy[j]);
                }
                ciphertext[i] = cSum;
            }
        }

        for (int i = 0; i < BLOCK_SIZE; i++) {
            if ((1 << i & PATTERN) != 0) {
                ciphertext[i] = (byte) (keys[16][i] ^ ciphertext[i]);
            } else {
                ciphertext[i] = (byte) (keys[16][i] + ciphertext[i]);
            }
        }
        return ciphertext;
    }

    public static byte[] getRandomChallenge() {
        byte[] res = new byte[BLOCK_SIZE];
        SecureRandom rnd = new SecureRandom();
        rnd.nextBytes(res);
        return res;
    }

    public byte[] computeChallengeResponse(byte[] challenge) {
        byte[][] keys = keySchedule(challenge);
        return encrypt(AuthData.SEQ, keys);
    }

}
