/*  Copyright (C) 2023 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi;

import android.content.SharedPreferences;
import android.os.Build;

import androidx.annotation.Nullable;

import com.google.protobuf.ByteString;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.shaded.crypto.CryptoException;
import org.bouncycastle.shaded.crypto.engines.AESEngine;
import org.bouncycastle.shaded.crypto.modes.CCMBlockCipher;
import org.bouncycastle.shaded.crypto.params.AEADParameters;
import org.bouncycastle.shaded.crypto.params.KeyParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.proto.xiaomi.XiaomiProto;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services.AbstractXiaomiService;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class XiaomiAuthService extends AbstractXiaomiService {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiAuthService.class);

    public static final byte[] PAYLOAD_HEADER_AUTH = new byte[]{0, 0, 2, 2};

    public static final int COMMAND_TYPE = 1;

    public static final int CMD_SEND_USERID = 5;

    public static final int CMD_NONCE = 26;
    public static final int CMD_AUTH = 27;

    private boolean encryptionInitialized = false;

    private final byte[] secretKey = new byte[16];
    private final byte[] nonce = new byte[16];
    private final byte[] encryptionKey = new byte[16];
    private final byte[] decryptionKey = new byte[16];
    private final byte[] encryptionNonce = new byte[4];
    private final byte[] decryptionNonce = new byte[4];

    public XiaomiAuthService(final XiaomiSupport support) {
        super(support);
    }

    public boolean isEncryptionInitialized() {
        return encryptionInitialized;
    }

    protected void startEncryptedHandshake(final TransactionBuilder builder) {
        encryptionInitialized = false;

        builder.add(new SetDeviceStateAction(getSupport().getDevice(), GBDevice.State.AUTHENTICATING, getSupport().getContext()));

        System.arraycopy(getSecretKey(getSupport().getDevice()), 0, secretKey, 0, 16);
        new SecureRandom().nextBytes(nonce);

        getSupport().sendCommand(builder, buildNonceCommand(nonce));
    }

    protected void startClearTextHandshake(final TransactionBuilder builder) {
        builder.add(new SetDeviceStateAction(getSupport().getDevice(), GBDevice.State.AUTHENTICATING, getSupport().getContext()));

        final XiaomiProto.Auth auth = XiaomiProto.Auth.newBuilder()
                .setUserId(getUserId(getSupport().getDevice()))
                .build();

        final XiaomiProto.Command command = XiaomiProto.Command.newBuilder()
                .setType(XiaomiAuthService.COMMAND_TYPE)
                .setSubtype(XiaomiAuthService.CMD_SEND_USERID)
                .setAuth(auth)
                .build();

        getSupport().sendCommand(builder, command);
    }

    @Override
    public void handleCommand(final XiaomiProto.Command cmd) {
        if (cmd.getType() != COMMAND_TYPE) {
            throw new IllegalArgumentException("Not an auth command");
        }

        switch (cmd.getSubtype()) {
            case CMD_NONCE: {
                LOG.debug("Got watch nonce");

                // Watch nonce
                final XiaomiProto.Command reply = handleWatchNonce(cmd.getAuth().getWatchNonce());
                if (reply == null) {
                    getSupport().disconnect();
                    return;
                }

                final TransactionBuilder builder = getSupport().createTransactionBuilder("auth step 2");
                // TODO use sendCommand
                builder.write(
                        getSupport().getCharacteristic(getSupport().characteristicCommandWrite.getCharacteristicUUID()),
                        ArrayUtils.addAll(PAYLOAD_HEADER_AUTH, reply.toByteArray())
                );
                builder.queue(getSupport().getQueue());
                break;
            }

            case CMD_AUTH:
            case CMD_SEND_USERID: {
                if (cmd.getSubtype() == CMD_AUTH || cmd.getAuth().getStatus() == 1) {
                    LOG.info("Authenticated!");

                    encryptionInitialized = cmd.getSubtype() == CMD_AUTH;

                    final TransactionBuilder builder = getSupport().createTransactionBuilder("phase 2 initialize");
                    builder.add(new SetDeviceStateAction(getSupport().getDevice(), GBDevice.State.INITIALIZED, getSupport().getContext()));
                    getSupport().phase2Initialize();
                    builder.queue(getSupport().getQueue());
                } else {
                    LOG.warn("could not authenticate");
                }
                break;
            }
            default:
                LOG.warn("Unknown auth payload subtype {}", cmd.getSubtype());
        }
    }

    public byte[] encrypt(final byte[] arr, final short i) {
        final ByteBuffer packetNonce = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN)
                .put(encryptionNonce)
                .putInt(0)
                .putShort(i) // TODO what happens once this overflows?
                .putShort((short) 0);

        try {
            return encrypt(encryptionKey, packetNonce.array(), arr);
        } catch (final CryptoException e) {
            throw new RuntimeException("failed to encrypt", e);
        }
    }

    public byte[] decrypt(final byte[] arr) {
        final ByteBuffer packetNonce = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN);
        packetNonce.put(decryptionNonce);
        packetNonce.putInt(0);
        packetNonce.putInt(0);

        try {
            return decrypt(decryptionKey, packetNonce.array(), arr);
        } catch (final CryptoException e) {
            throw new RuntimeException("failed to decrypt", e);
        }
    }

    @Nullable
    private XiaomiProto.Command handleWatchNonce(final XiaomiProto.WatchNonce watchNonce) {
        final byte[] step2hmac = computeAuthStep3Hmac(secretKey, nonce, watchNonce.getNonce().toByteArray());

        System.arraycopy(step2hmac, 0, decryptionKey, 0, 16);
        System.arraycopy(step2hmac, 16, encryptionKey, 0, 16);
        System.arraycopy(step2hmac, 32, decryptionNonce, 0, 4);
        System.arraycopy(step2hmac, 36, encryptionNonce, 0, 4);

        if (BuildConfig.DEBUG) {
            LOG.debug("decryptionKey: {}", GB.hexdump(decryptionKey));
            LOG.debug("encryptionKey: {}", GB.hexdump(encryptionKey));
            LOG.debug("decryptionNonce: {}", GB.hexdump(decryptionNonce));
            LOG.debug("encryptionNonce: {}", GB.hexdump(encryptionNonce));
        }

        final byte[] decryptionConfirmation = hmacSHA256(decryptionKey, ArrayUtils.addAll(watchNonce.getNonce().toByteArray(), nonce));
        if (!Arrays.equals(decryptionConfirmation, watchNonce.getHmac().toByteArray())) {
            LOG.warn("Watch hmac mismatch");
            return null;
        }

        final XiaomiProto.AuthDeviceInfo authDeviceInfo = XiaomiProto.AuthDeviceInfo.newBuilder()
                .setUnknown1(0) // TODO ?
                .setPhoneApiLevel(Build.VERSION.SDK_INT)
                .setPhoneName(Build.MODEL)
                .setUnknown3(224) // TODO ?
                // TODO region should be actual device region?
                .setRegion(Locale.getDefault().getLanguage().substring(0, 2).toUpperCase(Locale.ROOT))
                .build();

        final byte[] encryptedNonces = hmacSHA256(encryptionKey, ArrayUtils.addAll(nonce, watchNonce.getNonce().toByteArray()));
        final byte[] encryptedDeviceInfo = encrypt(authDeviceInfo.toByteArray(), (short) 0);
        final XiaomiProto.AuthStep3 authStep3 = XiaomiProto.AuthStep3.newBuilder()
                .setEncryptedNonces(ByteString.copyFrom(encryptedNonces))
                .setEncryptedDeviceInfo(ByteString.copyFrom(encryptedDeviceInfo))
                .build();

        final XiaomiProto.Command.Builder cmd = XiaomiProto.Command.newBuilder();
        cmd.setType(COMMAND_TYPE);
        cmd.setSubtype(CMD_AUTH);

        final XiaomiProto.Auth.Builder auth = XiaomiProto.Auth.newBuilder();
        auth.setAuthStep3(authStep3);

        return cmd.setAuth(auth.build()).build();
    }

    public static XiaomiProto.Command buildNonceCommand(final byte[] nonce) {
        final XiaomiProto.PhoneNonce.Builder phoneNonce = XiaomiProto.PhoneNonce.newBuilder();
        phoneNonce.setNonce(ByteString.copyFrom(nonce));

        final XiaomiProto.Auth.Builder auth = XiaomiProto.Auth.newBuilder();
        auth.setPhoneNonce(phoneNonce.build());

        final XiaomiProto.Command.Builder command = XiaomiProto.Command.newBuilder();
        command.setType(COMMAND_TYPE);
        command.setSubtype(CMD_NONCE);
        command.setAuth(auth.build());
        return command.build();
    }

    public static byte[] computeAuthStep3Hmac(final byte[] secretKey,
                                              final byte[] phoneNonce,
                                              final byte[] watchNonce) {
        final byte[] miwearAuthBytes = "miwear-auth".getBytes();

        final Mac mac;
        try {
            mac = Mac.getInstance("HmacSHA256");
            // Compute the actual key and re-initialize the mac
            mac.init(new SecretKeySpec(ArrayUtils.addAll(phoneNonce, watchNonce), "HmacSHA256"));
            final byte[] hmacKeyBytes = mac.doFinal(secretKey);
            final SecretKeySpec key = new SecretKeySpec(hmacKeyBytes, "HmacSHA256");
            mac.init(key);
        } catch (final NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to initialize hmac for auth step 2", e);
        }

        final byte[] output = new byte[64];
        byte[] tmp = new byte[0];
        byte b = 1;
        int i = 0;
        while (i < output.length) {
            mac.update(tmp);
            mac.update(miwearAuthBytes);
            mac.update(b);
            tmp = mac.doFinal();
            for (int j = 0; j < tmp.length && i < output.length; j++, i++) {
                output[i] = tmp[j];
            }
            b++;
        }
        return output;
    }

    protected static byte[] getSecretKey(final GBDevice device) {
        final byte[] authKeyBytes = new byte[16];

        final SharedPreferences sharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(device.getAddress());

        final String authKey = sharedPrefs.getString("authkey", "").trim();
        if (StringUtils.isNotBlank(authKey)) {
            final byte[] srcBytes;
            // Allow both with and without 0x, to avoid user mistakes
            if (authKey.length() == 34 && authKey.startsWith("0x")) {
                srcBytes = GB.hexStringToByteArray(authKey.trim().substring(2));
            } else {
                srcBytes = GB.hexStringToByteArray(authKey.trim());
            }
            System.arraycopy(srcBytes, 0, authKeyBytes, 0, Math.min(srcBytes.length, 16));
        }

        return authKeyBytes;
    }

    protected static String getUserId(final GBDevice device) {
        final SharedPreferences sharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(device.getAddress());

        final String authKey = sharedPrefs.getString("authkey", null);
        if (StringUtils.isNotBlank(authKey)) {
            return authKey;
        }

        return "0000000000";
    }

    protected static byte[] hmacSHA256(final byte[] key, final byte[] input) {
        try {
            final Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(input);
        } catch (final Exception e) {
            throw new RuntimeException("Failed to hmac", e);
        }
    }

    public static byte[] encrypt(final byte[] key, final byte[] nonce, final byte[] payload) throws
            CryptoException {
        final CCMBlockCipher cipher = createBlockCipher(true, new SecretKeySpec(key, "AES"), nonce);
        final byte[] out = new byte[cipher.getOutputSize(payload.length)];
        final int outBytes = cipher.processBytes(payload, 0, payload.length, out, 0);
        cipher.doFinal(out, outBytes);
        return out;
    }

    public static byte[] decrypt(final byte[] key,
                                 final byte[] nonce,
                                 final byte[] encryptedPayload) throws CryptoException {
        final CCMBlockCipher cipher = createBlockCipher(false, new SecretKeySpec(key, "AES"), nonce);
        final byte[] decrypted = new byte[cipher.getOutputSize(encryptedPayload.length)];
        cipher.doFinal(decrypted, cipher.processBytes(encryptedPayload, 0, encryptedPayload.length, decrypted, 0));
        return decrypted;
    }

    public static CCMBlockCipher createBlockCipher(final boolean forEncrypt,
                                                   final SecretKey secretKey,
                                                   final byte[] nonce) {
        final AESEngine aesFastEngine = new AESEngine();
        aesFastEngine.init(forEncrypt, new KeyParameter(secretKey.getEncoded()));
        final CCMBlockCipher blockCipher = new CCMBlockCipher(aesFastEngine);
        blockCipher.init(forEncrypt, new AEADParameters(new KeyParameter(secretKey.getEncoded()), 32, nonce, null));
        return blockCipher;
    }
}
