/*  Copyright (C) 2016-2019 Andreas Shimokawa, Carsten Pfeiffer

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiService;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEOperation;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiSupport;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class InitOperation extends AbstractBTLEOperation<HuamiSupport> {
    private static final Logger LOG = LoggerFactory.getLogger(InitOperation.class);

    private final TransactionBuilder builder;
    private final boolean needsAuth;
    private final byte authFlags;

    public InitOperation(boolean needsAuth, byte authFlags, HuamiSupport support, TransactionBuilder builder) {
        super(support);
        this.needsAuth = needsAuth;
        this.authFlags = authFlags;
        this.builder = builder;
        builder.setGattCallback(this);
    }

    @Override
    protected void doPerform() throws IOException {
        getSupport().enableNotifications(builder, true);
        if (needsAuth) {
            builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.AUTHENTICATING, getContext()));
            // write key to miband2
            byte[] sendKey = org.apache.commons.lang3.ArrayUtils.addAll(new byte[]{HuamiService.AUTH_SEND_KEY, authFlags}, getSecretKey());
            builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_AUTH), sendKey);
        } else {
            builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));
            // get random auth number
            builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_AUTH), requestAuthNumber());
        }
    }

    private byte[] requestAuthNumber() {
        return new byte[]{HuamiService.AUTH_REQUEST_RANDOM_AUTH_NUMBER, authFlags};
    }

    private byte[] getSecretKey() {
        byte[] authKeyBytes = new byte[]{0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x40, 0x41, 0x42, 0x43, 0x44, 0x45};

        SharedPreferences preferences = getContext().getSharedPreferences("devicesettings_" + getDevice().getAddress(),Context.MODE_PRIVATE);
        String authKey = preferences.getString("authkey", null);
        if (authKey != null && !authKey.isEmpty()) {
            byte[] srcBytes = authKey.getBytes();
            System.arraycopy(srcBytes, 0, authKeyBytes, 0, Math.min(srcBytes.length,16));
        }
        return authKeyBytes;
    }

    @Override
    public TransactionBuilder performInitialized(String taskName) throws IOException {
        throw new UnsupportedOperationException("This IS the initialization class, you cannot call this method");
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic) {
        UUID characteristicUUID = characteristic.getUuid();
        if (HuamiService.UUID_CHARACTERISTIC_AUTH.equals(characteristicUUID)) {
            try {
                byte[] value = characteristic.getValue();
                getSupport().logMessageContent(value);
                if (value[0] == HuamiService.AUTH_RESPONSE &&
                        value[1] == HuamiService.AUTH_SEND_KEY &&
                        value[2] == HuamiService.AUTH_SUCCESS) {
                    TransactionBuilder builder = createTransactionBuilder("Sending the secret key to the band");
                    builder.write(characteristic, requestAuthNumber());
                    getSupport().performImmediately(builder);
                } else if (value[0] == HuamiService.AUTH_RESPONSE &&
                        value[1] == HuamiService.AUTH_REQUEST_RANDOM_AUTH_NUMBER &&
                        value[2] == HuamiService.AUTH_SUCCESS) {
                    // md5??
                    byte[] eValue = handleAESAuth(value, getSecretKey());
                    byte[] responseValue = org.apache.commons.lang3.ArrayUtils.addAll(
                            new byte[]{HuamiService.AUTH_SEND_ENCRYPTED_AUTH_NUMBER, authFlags}, eValue);

                    TransactionBuilder builder = createTransactionBuilder("Sending the encrypted random key to the band");
                    builder.write(characteristic, responseValue);
                    getSupport().setCurrentTimeWithService(builder);
                    getSupport().performImmediately(builder);
                } else if (value[0] == HuamiService.AUTH_RESPONSE &&
                        value[1] == HuamiService.AUTH_SEND_ENCRYPTED_AUTH_NUMBER &&
                        value[2] == HuamiService.AUTH_SUCCESS) {
                    TransactionBuilder builder = createTransactionBuilder("Authenticated, now initialize phase 2");
                    builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));
                    getSupport().requestDeviceInfo(builder);
                    getSupport().enableFurtherNotifications(builder, true);
                    getSupport().phase2Initialize(builder);
                    getSupport().phase3Initialize(builder);
                    getSupport().setInitialized(builder);
                    getSupport().performImmediately(builder);
                } else {
                    return super.onCharacteristicChanged(gatt, characteristic);
                }
            } catch (Exception e) {
                GB.toast(getContext(), "Error authenticating Mi Band 2", Toast.LENGTH_LONG, GB.ERROR, e);
            }
            return true;
        } else {
            LOG.info("Unhandled characteristic changed: " + characteristicUUID);
            return super.onCharacteristicChanged(gatt, characteristic);
        }
    }

    private byte[] getMD5(byte[] message) throws NoSuchAlgorithmException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        return md5.digest(message);
    }

    private byte[] handleAESAuth(byte[] value, byte[] secretKey) throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException {
        byte[] mValue = Arrays.copyOfRange(value, 3, 19);
        Cipher ecipher = Cipher.getInstance("AES/ECB/NoPadding");
        SecretKeySpec newKey = new SecretKeySpec(secretKey, "AES");
        ecipher.init(Cipher.ENCRYPT_MODE, newKey);
        byte[] enc = ecipher.doFinal(mValue);
        return enc;
    }
}
