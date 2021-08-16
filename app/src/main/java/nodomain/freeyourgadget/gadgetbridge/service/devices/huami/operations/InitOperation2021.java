/*  Copyright (C) 2016-2021 Andreas Shimokawa, Carsten Pfeiffer

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
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiSupport;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class InitOperation2021 extends InitOperation {

    private static final Logger LOG = LoggerFactory.getLogger(InitOperation2021.class);

    public InitOperation2021(boolean needsAuth, byte authFlags, byte cryptFlags, HuamiSupport support, TransactionBuilder builder) {
        super(needsAuth, authFlags, cryptFlags, support, builder);
    }

    @Override
    protected void doPerform() {
        huamiSupport.enableNotifications(builder, true);
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));
        // get random auth number
        huamiSupport.writeToChunked2021(builder, (short) 0x82, (byte) 0x66, requestAuthNumber());
    }

    private byte[] requestAuthNumber() {
        return new byte[]{0x04,0x02,0x00,0x02, (byte) 0xe3,0x16, (byte) 0xde,0x05, (byte) 0xe4, (byte) 0xa7,0x05,0x18,0x4b, (byte) 0xc9, (byte) 0x99, (byte) 0xd7, (byte) 0x90, (byte) 0x90,0x71, (byte) 0xdc,0x1c,0x58,0x55,0x1b,0x02,0x00,0x00,0x00, (byte) 0x96, (byte) 0xf1, (byte) 0x82,0x01, (byte) 0x98,0x6a, (byte) 0xd7, (byte) 0xe6,0x52,0x2e, (byte) 0xfd, (byte) 0xdc,0x4d, (byte) 0xe9,0x23, (byte) 0x81, (byte) 0x82,0x7d,0x76,0x6c,0x01,0x00,0x00,0x00};
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
        if (HuamiService.UUID_CHARACTERISTIC_CHUNKEDTRANSFER_2021_READ.equals(characteristicUUID)) {
            byte[] value = characteristic.getValue();
            huamiSupport.logMessageContent(value);
            return super.onCharacteristicChanged(gatt, characteristic);
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
