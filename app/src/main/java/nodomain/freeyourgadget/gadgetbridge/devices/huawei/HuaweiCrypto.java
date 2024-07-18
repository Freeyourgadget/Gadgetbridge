/*  Copyright (C) 2024 Damien Gaignon, Martin.JM

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
package nodomain.freeyourgadget.gadgetbridge.devices.huawei;

import nodomain.freeyourgadget.gadgetbridge.util.CryptoUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HuaweiCrypto {
    private static final Logger LOG = LoggerFactory.getLogger(HuaweiCrypto.class);

    public static class CryptoException extends Exception {
        CryptoException(Exception e) {
            super(e);
        }
    }

    public static final byte[] SECRET_KEY_1_v1 = new byte[]{ 0x6F, 0x75, 0x6A, 0x79,
                                                            0x6D, 0x77, 0x71, 0x34,
                                                            0x63, 0x6C, 0x76, 0x39,
                                                            0x33, 0x37, 0x38, 0x79};
    public static final byte[] SECRET_KEY_2_v1 = new byte[]{ 0x62, 0x31, 0x30, 0x6A,
                                                            0x67, 0x66, 0x64, 0x39,
                                                            0x79, 0x37, 0x76, 0x73,
                                                            0x75, 0x64, 0x61, 0x39};
    public static final byte[] SECRET_KEY_1_v23 = new byte[]{ 0x55, 0x53, (byte)0x86, (byte)0xFC,
                                                            0x63, 0x20, 0x07, (byte)0xAA,
                                                            (byte)0x86, 0x49, 0x35, 0x22,
                                                            (byte)0xB8, 0x6A, (byte)0xE2, 0x5C};
    public static final byte[] SECRET_KEY_2_v23 = new byte[]{ 0x33, 0x07, (byte)0x9B, (byte)0xC5,
                                                            0x7A, (byte)0x88, 0x6D, 0x3C,
                                                            (byte)0xF5, 0x61, 0x37, 0x09,
                                                            0x6F, 0x22, (byte)0x80, 0x00};

    public static final byte[] DIGEST_SECRET_v1 = new byte[]{ 0x70, (byte)0xFB, 0x6C, 0x24,
                                                            0x03, 0x5F, (byte)0xDB, 0x55,
                                                            0x2F, 0x38, (byte)0x89, (byte)0x8A,
                                                            (byte) 0xEE, (byte)0xDE, 0x3F, 0x69};
    public static final byte[] DIGEST_SECRET_v2 = new byte[]{ (byte)0x93, (byte)0xAC, (byte)0xDE, (byte)0xF7,
                                                            0x6A, (byte)0xCB, 0x09, (byte)0x85,
                                                            0x7D, (byte)0xBF, (byte)0xE5, 0x26,
                                                            0x1A, (byte)0xAB, (byte)0xCD, 0x78};
    public static final byte[] DIGEST_SECRET_v3 = new byte[]{ (byte)0x9C, 0x27, 0x63, (byte)0xA9,
                                                            (byte)0xCC, (byte)0xE1, 0x34, 0x76,
                                                            0x6D, (byte)0xE3, (byte)0xFF, 0x61,
                                                            0x18, 0x20, 0x05, 0x53};

    public static final byte[] MESSAGE_RESPONSE = new byte[]{0x01, 0x10};
    public static final byte[] MESSAGE_CHALLENGE = new byte[]{0x01, 0x00};

    public static final long ENCRYPTION_COUNTER_MAX = 0xFFFFFFFF;

    protected int authVersion;
    protected int deviceSupportType;
    protected byte authAlgo;
    protected  byte authMode;

    public HuaweiCrypto(int authVersion) {
        this.authVersion = authVersion;
    }

    public HuaweiCrypto(int authVersion, byte authAlgo, int deviceSupportType, byte authMode) {
        this(authVersion);
        this.deviceSupportType = deviceSupportType;
        this.authAlgo = authAlgo;
        this.authMode = authMode;
    }

    public static byte[] generateNonce() {
        // While technically not a nonce, we need it to be random and rely on the length for the chance of repitition to be small
        byte[] returnValue = new byte[16];
        (new SecureRandom()).nextBytes(returnValue);
        return returnValue;
    }

    private byte[] getDigestSecret() {
        byte[] digest;
        if (authVersion == 1) {
            digest = DIGEST_SECRET_v1.clone();
        } else if (authVersion == 2) {
            digest = DIGEST_SECRET_v2.clone();
        } else {
            digest = DIGEST_SECRET_v3.clone();
        }
        return digest;
    }
    public byte[] computeDigest(byte[] message, byte[] nonce) throws NoSuchAlgorithmException, InvalidKeyException {
        byte[] digestSecret = getDigestSecret();
        byte[] msgToDigest = ByteBuffer.allocate(16 + message.length)
                                                .put(digestSecret)
                                                .put(message)
                                                .array();
        byte[] digestStep1 = CryptoUtils.calcHmacSha256(msgToDigest, nonce);
        byte[] challenge = ByteBuffer.allocate(0x40)
                .put(CryptoUtils.calcHmacSha256(digestStep1, nonce))
                .put(digestStep1)
                .array();
        return challenge;
    }

    public byte[] computeDigestHiChainLite(byte[] message, byte[] key, byte[] nonce) throws NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException, UnsupportedEncodingException {
        byte[] digestStep1;
        byte[] hashKey = CryptoUtils.digest(GB.hexdump(key).getBytes("UTF-8"));
        byte[] digestSecret = getDigestSecret();
        for (int i = 0; i < digestSecret.length; i++) {
            digestSecret[i] = (byte) (((0xFF & hashKey[i]) ^ (digestSecret[i] & 0xFF)) & 0xFF);
        }
        byte[] msgToDigest = ByteBuffer.allocate(18)
                .put(digestSecret)
                .put(message)
                .array();
        if (authAlgo == 0x01) {
            digestStep1 = CryptoUtils.pbkdf2Sha256(GB.hexdump(msgToDigest), GB.hexdump(nonce), 0x3e8, 0x100);
        } else {
            digestStep1 = CryptoUtils.calcHmacSha256(msgToDigest, nonce);
        }
        byte[] challenge = ByteBuffer.allocate(0x40)
                .put(CryptoUtils.calcHmacSha256(digestStep1, nonce))
                .put(digestStep1)
                .array();
        return challenge;
    }

    public byte[] digestChallenge(byte[] secretKey, byte[] nonce) throws NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException, UnsupportedEncodingException {
        if (authMode == 0x02) {
            if (secretKey == null)
                return null;
            if (authVersion == 0x02) {
                byte[] key = ByteBuffer.allocate(18)
                        .put(secretKey)
                        .put(MESSAGE_CHALLENGE)
                        .array();
                byte[] challenge = ByteBuffer.allocate(0x40)
                        .put(CryptoUtils.calcHmacSha256(key, nonce))
                        .put(key)
                        .array();
                return challenge;
            }
            return computeDigestHiChainLite(MESSAGE_CHALLENGE, secretKey, nonce);
        }
        return computeDigest(MESSAGE_CHALLENGE, nonce);
    }

    public byte[] digestResponse(byte[] secretKey, byte[] nonce) throws NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException, UnsupportedEncodingException {
        if (authMode == 0x02) {
            if (secretKey == null)
                return null;
            if (authVersion == 0x02) {
                byte[] key = ByteBuffer.allocate(18)
                        .put(secretKey)
                        .put(MESSAGE_RESPONSE)
                        .array();
                byte[] challenge = ByteBuffer.allocate(0x40)
                        .put(CryptoUtils.calcHmacSha256(key, nonce))
                        .put(key)
                        .array();
                return challenge;
            }
            return computeDigestHiChainLite(MESSAGE_RESPONSE, secretKey, nonce);
        }
        return computeDigest(MESSAGE_RESPONSE, nonce);
    }

    public static ByteBuffer initializationVector(long counter) {
        if (counter == ENCRYPTION_COUNTER_MAX) {
            counter = 1;
        } else {
            counter += 1;
        }
        ByteBuffer ivCounter = ByteBuffer.allocate(16);
        ivCounter.put(generateNonce(), 0, 12);
        ivCounter.put(ByteBuffer.allocate(8).putLong(counter).array(), 4, 4);
        ivCounter.rewind();
        return ivCounter;
    }

    public byte[] createSecretKey(String macAddress) throws NoSuchAlgorithmException {
        byte[] secret_key_1 = SECRET_KEY_1_v23;
        byte[] secret_key_2 = SECRET_KEY_2_v23;
        if (authVersion == 1) {
            secret_key_1 = SECRET_KEY_1_v1;
            secret_key_2 = SECRET_KEY_2_v1;
        }

        byte[] macAddressKey = (macAddress.replace(":", "") + "0000").getBytes(StandardCharsets.UTF_8);

        byte[] mixedSecretKey = new byte[16];
        for (int i = 0; i < 16; i++) {
            mixedSecretKey[i] = (byte)((((0xFF & secret_key_1[i]) << 4) ^ (0xFF & secret_key_2[i])) & 0xFF);
        }

        byte[] mixedSecretKeyHash = CryptoUtils.digest(mixedSecretKey);
        byte[] finalMixedKey = new byte[16];
        for (int i = 0; i < 16; i++) {
            finalMixedKey[i] = (byte)((((0xFF & mixedSecretKeyHash[i]) >> 6) ^ (0xFF & macAddressKey[i])) & 0xFF);
        }
        byte[] finalMixedKeyHash = CryptoUtils.digest(finalMixedKey);
        return Arrays.copyOfRange(finalMixedKeyHash, 0, 16);
    }

    public byte[] encryptBondingKey(byte encryptMethod, byte[] data, byte[] encryptionKey, byte[] iv) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, IllegalArgumentException {
        if (encryptMethod == 0x01)
            return CryptoUtils.encryptAES_GCM_NoPad(data, encryptionKey, iv, null);
        return CryptoUtils.encryptAES_CBC_Pad(data, encryptionKey, iv);
    }

    public byte[] decryptBondingKey(byte[] data, String mac, byte[] iv) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, IllegalArgumentException {
        byte[] encryptionKey = createSecretKey(mac);
        return CryptoUtils.decryptAES_CBC_Pad(data, encryptionKey, iv);
    }

    public byte[] decryptPinCode(byte encryptMethod, byte[] message, byte[] iv) throws CryptoException {
        byte[] secretKey = getDigestSecret();
        try {
            if (encryptMethod == 0x1)
                return CryptoUtils.decryptAES_GCM_NoPad(message, secretKey, iv, null);
            return CryptoUtils.decryptAES_CBC_Pad(message, secretKey, iv);
        } catch (Exception e) {
            throw new CryptoException(e);
        }
    }

    public static byte[] encrypt(boolean useGCM, byte[] message, byte[] key, byte[] iv) throws CryptoException {
        try {
            if (useGCM)
                return CryptoUtils.encryptAES_GCM_NoPad(message, key, iv, null);
            return CryptoUtils.encryptAES_CBC_Pad(message, key, iv);
        } catch (InvalidAlgorithmParameterException | NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException | BadPaddingException | InvalidKeyException | IllegalArgumentException e) {
            throw new CryptoException(e);
        }
    }

    public static byte[] decrypt(boolean useGCM, byte[] message, byte[] key, byte[] iv) throws CryptoException {
        try {
            if (useGCM)
                return CryptoUtils.decryptAES_GCM_NoPad(message, key, iv, null);
            return CryptoUtils.decryptAES_CBC_Pad(message, key, iv);
        } catch (InvalidAlgorithmParameterException | NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException | BadPaddingException | InvalidKeyException | IllegalArgumentException e) {
            throw new CryptoException(e);
        }
    }
}
