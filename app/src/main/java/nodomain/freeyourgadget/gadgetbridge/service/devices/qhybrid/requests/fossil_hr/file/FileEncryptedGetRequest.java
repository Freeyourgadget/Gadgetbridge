/*  Copyright (C) 2019-2021 Daniel Dakhno

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.file;

import android.bluetooth.BluetoothGattCharacteristic;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.zip.CRC32;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil_hr.FossilHRWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.file.FileHandle;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.FossilRequest;
import nodomain.freeyourgadget.gadgetbridge.util.CRC32C;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public abstract class FileEncryptedGetRequest extends FossilRequest implements FileEncryptedInterface{
    private short handle;
    private FossilHRWatchAdapter adapter;

    private ByteBuffer fileBuffer;

    private byte[] fileData;

    private boolean finished = false;

    private Cipher cipher;
    private SecretKeySpec keySpec;
    private byte[] originalIv;

    int fileSize;

    private int packetCount = 0;
    private int ivIncrementor = 0x1f;

    public FileEncryptedGetRequest(short handle, FossilHRWatchAdapter adapter) {
        this.handle = handle;
        this.adapter = adapter;

        this.data =
                createBuffer()
                        .putShort(handle)
                        .putInt(0)
                        .putInt(0xFFFFFFFF)
                        .array();
    }

    public FileEncryptedGetRequest(FileHandle handle, FossilHRWatchAdapter adapter) {
        this(handle.getHandle(), adapter);
    }

    private void initDecryption() {
        try {
            cipher = Cipher.getInstance("AES/CTR/NoPadding");
            try {
                keySpec = new SecretKeySpec(this.adapter.getSecretKey(), "AES");
            } catch (IllegalAccessException e) {
                GB.toast("error getting key: " + e.getMessage(), Toast.LENGTH_LONG, GB.ERROR, e);
                return;
            }


            originalIv = new byte[16];


            byte[] phoneRandomNumber = adapter.getPhoneRandomNumber();
            byte[] watchRandomNumber = adapter.getWatchRandomNumber();

            System.arraycopy(phoneRandomNumber, 0, originalIv, 2, 6);
            System.arraycopy(watchRandomNumber, 0, originalIv, 9, 7);

            originalIv[7]++;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
        }
    }

    public FossilWatchAdapter getAdapter() {
        return adapter;
    }

    private byte[] incrementIV(byte[] iv, int amount){
        byte[] incrementedIv = new byte[iv.length];
        System.arraycopy(iv, 0, incrementedIv, 0, iv.length);
        ByteBuffer buffer = ByteBuffer.wrap(incrementedIv);
        int number = buffer.getInt(12);
        number += amount;
        buffer.position(12);
        buffer.putInt(number);
        return buffer.array();
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public void handleResponse(BluetoothGattCharacteristic characteristic) {
        byte[] value = characteristic.getValue();
        byte first = value[0];
        if (characteristic.getUuid().toString().equals("3dda0003-957f-7d4a-34a6-74696673696d")) {
            if ((first & 0x0F) == 1) {
                ByteBuffer buffer = ByteBuffer.wrap(value);
                buffer.order(ByteOrder.LITTLE_ENDIAN);

                this.initDecryption();

                short handle = buffer.getShort(1);
                fileSize = buffer.getInt(4);

                byte status = buffer.get(3);

                ResultCode code = ResultCode.fromCode(status);
                if (!code.inidicatesSuccess()) {
                    throw new RuntimeException("FileGet error: " + code + "   (" + status + ")");
                }

                if (this.handle != handle) {
                    throw new RuntimeException("handle: " + handle + "   expected: " + this.handle);
                }
                log("file size: " + fileSize);
                fileBuffer = ByteBuffer.allocate(fileSize);
            } else if ((first & 0x0F) == 8) {
                this.finished = true;

                ByteBuffer buffer = ByteBuffer.wrap(value);
                buffer.order(ByteOrder.LITTLE_ENDIAN);

                short handle = buffer.getShort(1);
                if (this.handle != handle) {
                    throw new RuntimeException("handle: " + handle + "   expected: " + this.handle);
                }

                CRC32 crc = new CRC32();
                crc.update(this.fileData);

                CRC32C c = new CRC32C();
                c.update(this.fileData, 0, fileData.length);

                int crcExpected = buffer.getInt(8);

                if ((int) crc.getValue() != crcExpected) {
                    throw new RuntimeException("crc: " + crc.getValue() + "   expected: " + crcExpected);
                }

                this.handleFileData(this.fileData);
            }
        } else if (characteristic.getUuid().toString().equals("3dda0004-957f-7d4a-34a6-74696673696d")) {
            try {
                byte[] result = null;
                if(packetCount == 1) {
                    for(int testIvSummand = 0x1e; testIvSummand < 0x30; testIvSummand++){
                        byte[] iv = incrementIV(originalIv, testIvSummand);
                        cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(iv));
                        result = cipher.doFinal(value);

                        int currentLength = fileBuffer.position() + result.length - 1;

                        byte expectedByte = (currentLength == fileSize) ? (byte)0x81 : (byte)0x01; // 0x81 indicated the last payload

                        if(result[0] == expectedByte){
                            this.ivIncrementor = testIvSummand;
                            log("iv summand: " + testIvSummand);
                            break;
                        }
                        log("no iv summand found");
                    }
                }else{
                    byte[] iv = incrementIV(originalIv, ivIncrementor * packetCount);

                    cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(iv));
                    result = cipher.doFinal(value);
                }

                log("decryption result: " + StringUtils.bytesToHex(result));
                packetCount++;

                fileBuffer.put(result, 1, result.length - 1);
                if ((result[0] & 0x80) == 0x80) {
                    this.fileData = fileBuffer.array();
                }
            } catch (BadPaddingException | IllegalBlockSizeException | InvalidAlgorithmParameterException | InvalidKeyException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public UUID getRequestUUID() {
        return UUID.fromString("3dda0003-957f-7d4a-34a6-74696673696d");
    }

    @Override
    public byte[] getStartSequence() {
        return new byte[]{1};
    }

    @Override
    public int getPayloadLength() {
        return 11;
    }

    abstract public void handleFileData(byte[] fileData);
}
