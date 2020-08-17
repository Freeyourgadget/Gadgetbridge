/*  Copyright (C) 2016-2020 Andreas Shimokawa, Carsten Pfeiffer

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

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.SharedPreferences;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
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
    private final byte cryptFlags;
    private final HuamiSupport huamiSupport;

    public InitOperation(boolean needsAuth, byte authFlags, byte cryptFlags, HuamiSupport support, TransactionBuilder builder) {
        super(support);
        this.huamiSupport = support;
        this.needsAuth = needsAuth;
        this.authFlags = authFlags;
        this.cryptFlags = cryptFlags;
        this.builder = builder;
        builder.setGattCallback(this);
    }

    @Override
    protected void doPerform() {
        huamiSupport.enableNotifications(builder, true);
        if (needsAuth) {
            builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.AUTHENTICATING, getContext()));
            // write key to device
            byte[] sendKey = org.apache.commons.lang3.ArrayUtils.addAll(new byte[]{HuamiService.AUTH_SEND_KEY, authFlags}, getSecretKey());
            builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_AUTH), sendKey);
        } else {
            builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));
            // get random auth number
            builder.write(getCharacteristic(HuamiService.UUID_CHARACTERISTIC_AUTH), requestAuthNumber());
        }
    }

    private byte[] requestAuthNumber() {
        if (cryptFlags == 0x00) {
            return new byte[]{HuamiService.AUTH_REQUEST_RANDOM_AUTH_NUMBER, authFlags};
        } else {
            return new byte[]{(byte) (cryptFlags | HuamiService.AUTH_REQUEST_RANDOM_AUTH_NUMBER), authFlags, 0x02, 0x01, 0x00};
        }
    }

    private byte[] getSecretKey() {
        byte[] authKeyBytes = new byte[]{0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x40, 0x41, 0x42, 0x43, 0x44, 0x45};

        SharedPreferences sharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress());

        String authKey = sharedPrefs.getString("authkey", null);
        if (authKey != null && !authKey.isEmpty()) {
            byte[] srcBytes = authKey.trim().getBytes();
            if (authKey.length() == 34 && authKey.substring(0, 2).equals("0x")) {
                srcBytes = GB.hexStringToByteArray(authKey.substring(2));
            }
            System.arraycopy(srcBytes, 0, authKeyBytes, 0, Math.min(srcBytes.length, 16));
        }

        return authKeyBytes;
    }

    @Override
    public TransactionBuilder performInitialized(String taskName) {
        throw new UnsupportedOperationException("This IS the initialization class, you cannot call this method");
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic) {
        UUID characteristicUUID = characteristic.getUuid();
        if (HuamiService.UUID_CHARACTERISTIC_AUTH.equals(characteristicUUID)) {
            try {
                byte[] value = characteristic.getValue();
                huamiSupport.logMessageContent(value);
                if (value[0] == HuamiService.AUTH_RESPONSE &&
                        value[1] == HuamiService.AUTH_SEND_KEY &&
                        value[2] == HuamiService.AUTH_SUCCESS) {
                    TransactionBuilder builder = createTransactionBuilder("Sending the secret key to the device");
                    builder.write(characteristic, requestAuthNumber());
                    huamiSupport.performImmediately(builder);
                } else if (value[0] == HuamiService.AUTH_RESPONSE &&
                        (value[1] & 0x0f) == HuamiService.AUTH_REQUEST_RANDOM_AUTH_NUMBER &&
                        value[2] == HuamiService.AUTH_SUCCESS) {
                    byte[] eValue = handleAESAuth(value, getSecretKey());
                    byte[] responseValue = org.apache.commons.lang3.ArrayUtils.addAll(
                            new byte[]{(byte) (HuamiService.AUTH_SEND_ENCRYPTED_AUTH_NUMBER | cryptFlags), authFlags}, eValue);

                    TransactionBuilder builder = createTransactionBuilder("Sending the encrypted random key to the device");
                    builder.write(characteristic, responseValue);
                    huamiSupport.setCurrentTimeWithService(builder);
                    huamiSupport.performImmediately(builder);
                } else if (value[0] == HuamiService.AUTH_RESPONSE &&
                        (value[1] & 0x0f) == HuamiService.AUTH_SEND_ENCRYPTED_AUTH_NUMBER &&
                        value[2] == HuamiService.AUTH_SUCCESS) {
                    TransactionBuilder builder = createTransactionBuilder("Authenticated, now initialize phase 2");
                    builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));
                    huamiSupport.enableFurtherNotifications(builder, true);
                    huamiSupport.requestDeviceInfo(builder);
                    huamiSupport.phase2Initialize(builder);
                    huamiSupport.phase3Initialize(builder);
                    huamiSupport.setInitialized(builder);
                    huamiSupport.performImmediately(builder);
                } else {
                    return super.onCharacteristicChanged(gatt, characteristic);
                }
            } catch (Exception e) {
                GB.toast(getContext(), "Error authenticating Huami device", Toast.LENGTH_LONG, GB.ERROR, e);
            }
            return true;
        } else {
            LOG.info("Unhandled characteristic changed: " + characteristicUUID);
            return super.onCharacteristicChanged(gatt, characteristic);
        }
    }

    private byte[] handleAESAuth(byte[] value, byte[] secretKey) throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException {
        byte[] mValue = Arrays.copyOfRange(value, 3, 19);
        @SuppressLint("GetInstance") Cipher ecipher = Cipher.getInstance("AES/ECB/NoPadding");
        SecretKeySpec newKey = new SecretKeySpec(secretKey, "AES");
        ecipher.init(Cipher.ENCRYPT_MODE, newKey);
        return ecipher.doFinal(mValue);
    }
}
