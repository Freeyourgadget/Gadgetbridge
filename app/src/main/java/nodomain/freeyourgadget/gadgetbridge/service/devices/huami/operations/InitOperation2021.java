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
import java.util.Random;
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
    private final byte[] privateEC = new byte[24];
    private byte[] publicEC;
    private byte[] remotePublicEC = new byte[48];
    private byte[] remoteRandom = new byte[16];
    private byte[] sharedEC;
    private byte[] finalSharedSessionAES = new byte[16];

    private final byte[] reassembleBuffer = new byte[512];
    private int lastSequenceNumber = 0;
    private int reassembleBuffer_pointer = 0;
    private int reassembleBuffer_expectedBytes = 0;

    static {
        System.loadLibrary("tiny-edhc");
    }


    private static final Logger LOG = LoggerFactory.getLogger(InitOperation2021.class);

    public InitOperation2021(boolean needsAuth, byte authFlags, byte cryptFlags, HuamiSupport support, TransactionBuilder builder) {
        super(needsAuth, authFlags, cryptFlags, support, builder);
    }

    @Override
    protected void doPerform() {
        huamiSupport.enableNotifications(builder, true);
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));
        // get random auth number
        generateKeyPair();
        byte[] sendPubkeyCommand = new byte[48 + 4];
        sendPubkeyCommand[0] = 0x04;
        sendPubkeyCommand[1] = 0x02;
        sendPubkeyCommand[2] = 0x00;
        sendPubkeyCommand[3] = 0x02;
        System.arraycopy(publicEC, 0, sendPubkeyCommand, 4, 48);
        huamiSupport.writeToChunked2021(builder, (short) 0x82, (byte) 0x66, sendPubkeyCommand);
    }

    private native byte[] ecdh_generate_public(byte[] privateEC);

    private native byte[] ecdh_generate_shared(byte[] privateEC, byte[] remotePublicEC);


    private void generateKeyPair() {
        Random r = new Random();
        r.nextBytes(privateEC);
        publicEC = ecdh_generate_public(privateEC);
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
//            0x03 0x01 0x00 0x64 0x00 0x43 0x00 0x00 0x00 0x82 0x00 0x10 0x04 0x01 0x36 0x41 0xf2 0x5a 0x8f 0xb3
            if (value.length > 1 && value[0] == 0x03) {
                int sequenceNumber = value[4];
                int headerSize;
                if (sequenceNumber == 0 && value[9] == (byte) 0x82 && value[10] == 0x00 && value[11] == 0x10 && value[12] == 0x04 && value[13] == 0x01) {
                    reassembleBuffer_pointer = 0;
                    headerSize = 14;
                    reassembleBuffer_expectedBytes = value[5] - 3;

                } else if (sequenceNumber > 0) {
                    if (sequenceNumber != lastSequenceNumber + 1) {
                        LOG.warn("unexpected sequence number");
                        return false;
                    }
                    headerSize = 5;
                } else {
                    LOG.info("Unhandled characteristic changed: " + characteristicUUID);
                    return super.onCharacteristicChanged(gatt, characteristic);
                }

                int bytesToCopy = value.length - headerSize;
                System.arraycopy(value, headerSize, reassembleBuffer, reassembleBuffer_pointer, bytesToCopy);
                reassembleBuffer_pointer += bytesToCopy;

                lastSequenceNumber = sequenceNumber;
                if (reassembleBuffer_pointer == reassembleBuffer_expectedBytes) {
                    System.arraycopy(reassembleBuffer, 0, remoteRandom, 0, 16);
                    System.arraycopy(reassembleBuffer, 16, remotePublicEC, 0, 48);
                    sharedEC = ecdh_generate_shared(privateEC, remotePublicEC);
                    byte[] secretKey = getSecretKey();
                    for (int i = 0; i < 16; i++) {
                        finalSharedSessionAES[i] = (byte) (sharedEC[i + 8] ^ secretKey[i]);
                    }
                    try {
                        byte[] encryptedRandom1 = encryptAES(remoteRandom, secretKey);
                        byte[] encryptedRandom2 = encryptAES(remoteRandom, finalSharedSessionAES);
                        if (encryptedRandom1.length == 16 && encryptedRandom2.length == 16) {
                            byte[] command = new byte[33];
                            command[0] = 0x05;
                            System.arraycopy(encryptedRandom1, 0, command, 1, 16);
                            System.arraycopy(encryptedRandom2, 0, command, 17, 16);
                            TransactionBuilder builder = createTransactionBuilder("Sending double encryted random to device");
                            huamiSupport.writeToChunked2021(builder, (short) 0x82, (byte) 0x67, command);
                            huamiSupport.performImmediately(builder);
                        }
                    } catch (Exception e) {
                        LOG.error("AES encryption failed", e);
                    }
                }
                return true;
            }

            huamiSupport.logMessageContent(value);
            return super.onCharacteristicChanged(gatt, characteristic);
        } else {
            LOG.info("Unhandled characteristic changed: " + characteristicUUID);
            return super.onCharacteristicChanged(gatt, characteristic);
        }

    }

    private byte[] encryptAES(byte[] value, byte[] secretKey) throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException {
        byte[] mValue = Arrays.copyOfRange(value, 0, 16);
        @SuppressLint("GetInstance") Cipher ecipher = Cipher.getInstance("AES/ECB/NoPadding");
        SecretKeySpec newKey = new SecretKeySpec(secretKey, "AES");
        ecipher.init(Cipher.ENCRYPT_MODE, newKey);
        return ecipher.doFinal(mValue);
    }
}
