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

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiService;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiSupport;
import nodomain.freeyourgadget.gadgetbridge.util.CryptoUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class InitOperation2021 extends InitOperation {
    private byte[] privateEC = new byte[24];
    private byte[] publicEC;
    private byte[] remotePublicEC = new byte[48];
    private final byte[] remoteRandom = new byte[16];
    private byte[] sharedEC;
    private final byte[] finalSharedSessionAES = new byte[16];

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

    private void testAuth() {
        byte[] secretKey = getSecretKey();
        privateEC = new byte[]{0x0b, 0x42, (byte) 0xb9, (byte) 0xe6, 0x1c, 0x23, 0x34, 0x0e, 0x35, (byte) 0xc1, 0x6e, 0x2e, 0x7d, (byte) 0xe4, 0x33, (byte) 0xf4, (byte) 0xb5, (byte) 0x85, (byte) 0x9a, 0x72, (byte) 0xec, 0x11, 0x40, 0x27};
        remotePublicEC = new byte[]{(byte) 0xe6, 0x01, 0x6a, (byte) 0xba, 0x1d, (byte) 0xe7, (byte) 0xac, 0x0f, 0x0c, 0x7f, 0x0f, (byte) 0xf7, (byte) 0xe2, 0x24, 0x3e, 0x66, 0x62, (byte) 0xb5, (byte) 0xe0, 0x3b, 0x01, 0x00, 0x00, 0x00, (byte) 0xad, (byte) 0x8a, 0x4b, (byte) 0xed, (byte) 0xc7, 0x6a, 0x1e, (byte) 0xfd, (byte) 0xe7, 0x72, 0x5c, (byte) 0xc6, 0x62, (byte) 0xb5, 0x48, 0x35, 0x51, 0x3e, 0x3d, 0x57, 0x05, 0x00, 0x00, 0x00};

        publicEC = ecdh_generate_public(privateEC);
        sharedEC = ecdh_generate_shared(privateEC, remotePublicEC);
        LOG.warn("publicEC: " + GB.hexdump(publicEC));
        LOG.warn("privateEC: " + GB.hexdump(privateEC));
        LOG.warn("remotepubEC: " + GB.hexdump(remotePublicEC));
        LOG.warn("sharedEC: " + GB.hexdump(sharedEC));
        for (int i = 0; i < 16; i++) {
            finalSharedSessionAES[i] = (byte) (sharedEC[i + 8] ^ secretKey[i]);
        }
        LOG.warn("finalSharedAES: " + GB.hexdump(finalSharedSessionAES));
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
        //testAuth();
        huamiSupport.writeToChunked2021(builder, HuamiService.CHUNKED2021_ENDPOINT_AUTH, huamiSupport.getNextHandle(), sendPubkeyCommand, false);
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
            if (value.length > 1 && value[0] == 0x03) {
                int sequenceNumber = value[4];
                int headerSize;
                if (sequenceNumber == 0 && value[9] == (byte) HuamiService.CHUNKED2021_ENDPOINT_AUTH && value[10] == 0x00 && value[11] == 0x10 && value[12] == 0x04 && value[13] == 0x01) {
                    reassembleBuffer_pointer = 0;
                    headerSize = 14;
                    reassembleBuffer_expectedBytes = value[5] - 3;

                } else if (sequenceNumber > 0) {
                    if (sequenceNumber != lastSequenceNumber + 1) {
                        LOG.warn("unexpected sequence number");
                        return false;
                    }
                    headerSize = 5;
                } else if (value[9] == (byte) HuamiService.CHUNKED2021_ENDPOINT_AUTH && value[10] == 0x00 && value[11] == 0x10 && value[12] == 0x05 && value[13] == 0x01) {
                    try {
                        TransactionBuilder builder = createTransactionBuilder("Authenticated, now initialize phase 2");
                        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));
                        huamiSupport.enableFurtherNotifications(builder, true);
                        huamiSupport.requestDeviceInfo(builder);
                        huamiSupport.phase2Initialize(builder);
                        huamiSupport.phase3Initialize(builder);
                        huamiSupport.setInitialized(builder);
                        huamiSupport.performImmediately(builder);
                    } catch (Exception e) {
                        LOG.error("failed initializing device", e);
                    }
                    return true;
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
                    huamiSupport.encryptedSequenceNr = ((sharedEC[0] & 0xff) | ((sharedEC[1] & 0xff) << 8) | ((sharedEC[2] & 0xff) << 16) | ((sharedEC[3] & 0xff) << 24));

                    byte[] secretKey = getSecretKey();
                    for (int i = 0; i < 16; i++) {
                        finalSharedSessionAES[i] = (byte) (sharedEC[i + 8] ^ secretKey[i]);
                    }
                    huamiSupport.sharedSessionKey =  finalSharedSessionAES;
                    try {
                        byte[] encryptedRandom1 = CryptoUtils.encryptAES(remoteRandom, secretKey);
                        byte[] encryptedRandom2 = CryptoUtils.encryptAES(remoteRandom, finalSharedSessionAES);
                        if (encryptedRandom1.length == 16 && encryptedRandom2.length == 16) {
                            byte[] command = new byte[33];
                            command[0] = 0x05;
                            System.arraycopy(encryptedRandom1, 0, command, 1, 16);
                            System.arraycopy(encryptedRandom2, 0, command, 17, 16);
                            TransactionBuilder builder = createTransactionBuilder("Sending double encryted random to device");
                            huamiSupport.writeToChunked2021(builder, HuamiService.CHUNKED2021_ENDPOINT_AUTH, huamiSupport.getNextHandle(), command, false);
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
}
