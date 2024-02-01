/*  Copyright (C) 2021-2024 Andreas Shimokawa, Damien Gaignon, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.util;

import android.annotation.SuppressLint;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptoUtils {
    public static byte[] encryptAES(byte[] value, byte[] secretKey) throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException {
        @SuppressLint("GetInstance") Cipher ecipher = Cipher.getInstance("AES/ECB/NoPadding");
        SecretKeySpec newKey = new SecretKeySpec(secretKey, "AES");
        ecipher.init(Cipher.ENCRYPT_MODE, newKey);
        return ecipher.doFinal(value);
    }

    public static byte[] decryptAES(byte[] value, byte[] secretKey) throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException {
        @SuppressLint("GetInstance") Cipher ecipher = Cipher.getInstance("AES/ECB/NoPadding");
        SecretKeySpec newKey = new SecretKeySpec(secretKey, "AES");
        ecipher.init(Cipher.DECRYPT_MODE, newKey);
        return ecipher.doFinal(value);
    }

    public static byte[] encryptAES_CBC_Pad(byte[] data, byte[] key, byte[] iv) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
        AlgorithmParameterSpec paramSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, paramSpec);
        return cipher.doFinal(data);
    }

    public static byte[] decryptAES_CBC_Pad(byte[] data, byte[] key, byte[] iv) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
        AlgorithmParameterSpec paramSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, paramSpec);
        return cipher.doFinal(data);
    }

    public static byte[] encryptAES_GCM_NoPad(byte[] data, byte[] key, byte[] iv, byte[] aad) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
        GCMParameterSpec paramSpec = new GCMParameterSpec(16 * 8, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, paramSpec);
        if (aad != null) {
            cipher.updateAAD(aad);
        }
        return cipher.doFinal(data);
    }

    public static byte[] decryptAES_GCM_NoPad(byte[] data, byte[] key, byte[] iv, byte[] aad) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
        GCMParameterSpec paramSpec = new GCMParameterSpec(16 * 8, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, paramSpec);
        if (aad != null) {
            cipher.updateAAD(aad);
        }
        return cipher.doFinal(data);
    }

    public static byte[] calcHmacSha256(byte[] secretKey, byte[] message) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey, "HmacSHA256");
        mac.init(secretKeySpec);
        return mac.doFinal(message);
    }

    public static byte[] digest(byte[] message) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(message);
    }

    // Thanks to https://www.javatips.net/api/keywhiz-master/hkdf/src/main/java/keywhiz/hkdf/Hkdf.java for light code
    public static byte[] hkdfSha256(byte[] secretKey, byte[] salt, byte[] info, int outputLength) throws InvalidKeyException, NoSuchAlgorithmException { // return 32 byte len session key - outputLength=32 ?
        //extract start
        byte[] pseudoRandomKey = calcHmacSha256(salt, secretKey);
        SecretKey pseudoSecretKey = new SecretKeySpec(pseudoRandomKey, "HmacSHA256");
        //extract end
        int hashLen = 32;
        int n = (outputLength % hashLen == 0) ? outputLength / hashLen : (outputLength / hashLen) + 1;
        byte[] hashRound = new byte[0];

        ByteBuffer generatedBytes = ByteBuffer.allocate(n * hashLen);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(pseudoSecretKey);
        for (int roundNum = 1; roundNum <= n; roundNum++) {
          mac.reset();
          ByteBuffer t = ByteBuffer.allocate(hashRound.length + info.length + 1);
          t.put(hashRound);
          t.put(info);
          t.put((byte)roundNum);
          hashRound = mac.doFinal(t.array());
          generatedBytes.put(hashRound);
        }

        byte[] result = new byte[outputLength];
        generatedBytes.rewind();
        generatedBytes.get(result, 0, outputLength);
        return result;
    }

    public static byte[] pbkdf2Sha256(String key, String iv, int count, int length) throws InvalidKeySpecException, NoSuchAlgorithmException, UnsupportedEncodingException {
        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        PBEKeySpec keySpec = new PBEKeySpec(key.toCharArray(), iv.getBytes("utf-8"), count, length);
        return secretKeyFactory.generateSecret(keySpec).getEncoded();
    }
}
