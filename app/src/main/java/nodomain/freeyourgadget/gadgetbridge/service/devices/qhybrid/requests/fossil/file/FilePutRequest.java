/*  Copyright (C) 2019-2020 Andreas Shimokawa, Daniel Dakhno

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file;

import android.bluetooth.BluetoothGattCharacteristic;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.UUID;
import java.util.zip.CRC32;

import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.FossilRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.file.ResultCode;
import nodomain.freeyourgadget.gadgetbridge.util.CRC32C;

public class FilePutRequest extends FossilRequest {
    public enum UploadState {INITIALIZED, UPLOADING, CLOSING, UPLOADED}

    public UploadState state;

    private ArrayList<byte[]> packets = new ArrayList<>();

    private short handle, fileVersion;

    private FossilWatchAdapter adapter;

    private byte[] file;

    private int fullCRC;

    public FilePutRequest(short handle, byte[] file, short fileVersion, FossilWatchAdapter adapter) {
        this.handle = handle;
        this.adapter = adapter;
        this.fileVersion = fileVersion;

        int fileLength = file.length + 16;
        ByteBuffer buffer = this.createBuffer();
        buffer.putShort(1, handle);
        buffer.putInt(3, 0);
        buffer.putInt(7, fileLength);
        buffer.putInt(11, fileLength);

        this.data = buffer.array();

        this.file = file;

        state = UploadState.INITIALIZED;
    }

    public FilePutRequest(short handle, byte[] file, FossilWatchAdapter adapter) {
        this(handle, file, (short) 2, adapter);
    }

    public short getHandle() {
        return handle;
    }

    @Override
    public void handleResponse(BluetoothGattCharacteristic characteristic) {
        byte[] value = characteristic.getValue();
        if (characteristic.getUuid().toString().equals("3dda0003-957f-7d4a-34a6-74696673696d")) {
            int responseType = value[0] & 0x0F;
            log("response: " + responseType);
            switch (responseType) {
                case 3: {
                    if (value.length != 5 || (value[0] & 0x0F) != 3) {
                        throw new RuntimeException("wrong answer header");
                    }
                    state = UploadState.UPLOADING;

                    TransactionBuilder transactionBuilder = new TransactionBuilder("file upload");
                    BluetoothGattCharacteristic uploadCharacteristic = adapter.getDeviceSupport().getCharacteristic(UUID.fromString("3dda0004-957f-7d4a-34a6-74696673696d"));

                    this.prepareFilePackets(this.file, this.fileVersion);

                    for (byte[] packet : packets) {
                        transactionBuilder.write(uploadCharacteristic, packet);
                    }

                    transactionBuilder.queue(adapter.getDeviceSupport().getQueue());
                    break;
                }
                case 8: {
                    if (value.length == 4) return;
                    ByteBuffer buffer = ByteBuffer.wrap(value);
                    buffer.order(ByteOrder.LITTLE_ENDIAN);
                    short handle = buffer.getShort(1);
                    int crc = buffer.getInt(8);
                    byte status = value[3];

                    ResultCode code = ResultCode.fromCode(status);
                    if(!code.inidicatesSuccess()){
                        throw new RuntimeException("upload status: " + code + "   (" + status + ")");
                    }

                    if (handle != this.handle) {
                        throw new RuntimeException("wrong response handle");
                    }

                    if (crc != this.fullCRC) {
                        throw new RuntimeException("file upload exception: wrong crc");
                    }


                    ByteBuffer buffer2 = ByteBuffer.allocate(3);
                    buffer2.order(ByteOrder.LITTLE_ENDIAN);
                    buffer2.put((byte) 4);
                    buffer2.putShort(this.handle);

                    new TransactionBuilder("file close")
                            .write(
                                    adapter.getDeviceSupport().getCharacteristic(UUID.fromString("3dda0003-957f-7d4a-34a6-74696673696d")),
                                    buffer2.array()
                            )
                            .queue(adapter.getDeviceSupport().getQueue());

                    this.state = UploadState.CLOSING;
                    break;
                }
                case 4: {
                    if (value.length == 9) return;
                    if (value.length != 4 || (value[0] & 0x0F) != 4) {
                        throw new RuntimeException("wrong file closing header");
                    }
                    ByteBuffer buffer = ByteBuffer.wrap(value);
                    buffer.order(ByteOrder.LITTLE_ENDIAN);

                    short handle = buffer.getShort(1);

                    if (handle != this.handle) {
                        onFilePut(false);
                        throw new RuntimeException("wrong file closing handle");
                    }

                    byte status = buffer.get(3);

                    ResultCode code = ResultCode.fromCode(status);
                    if(!code.inidicatesSuccess()){
                        onFilePut(false);
                        throw new RuntimeException("wrong closing status: " + code + "   (" + status + ")");
                    }

                    this.state = UploadState.UPLOADED;

                    onFilePut(true);

                    log("uploaded file");

                    break;
                }
                case 9: {
                    this.onFilePut(false);
                    throw new RuntimeException("file put timeout");
                    /*timeout = true;
                    ByteBuffer buffer2 = ByteBuffer.allocate(3);
                    buffer2.order(ByteOrder.LITTLE_ENDIAN);
                    buffer2.put((byte) 4);
                    buffer2.putShort(this.handle);

                    new TransactionBuilder("file close")
                            .write(
                                    adapter.getDeviceSupport().getCharacteristic(UUID.fromString("3dda0003-957f-7d4a-34a6-74696673696d")),
                                    buffer2.array()
                            )
                            .queue(adapter.getDeviceSupport().getQueue());

                    this.state = UploadState.CLOSING;
                    break;*/
                }
            }
        }
    }

    @Override
    public boolean isFinished() {
        return this.state == UploadState.UPLOADED;
    }

    private void prepareFilePackets(byte[] file, short fileVersion) {
        int maxPacketSize = adapter.getMTU() - 4;

        ByteBuffer buffer = ByteBuffer.allocate(file.length + 12 + 4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        buffer.putShort(handle);
        buffer.putShort(fileVersion);
        buffer.putInt(0);
        buffer.putInt(file.length);

        buffer.put(file);

        CRC32C crc = new CRC32C();

        crc.update(file,0,file.length);
        buffer.putInt((int) crc.getValue());

        byte[] data = buffer.array();

        CRC32 fullCRC = new CRC32();

        fullCRC.update(data);
        this.fullCRC = (int) fullCRC.getValue();

        int packetCount = (int) Math.ceil(data.length / (float) maxPacketSize);

        for (int i = 0; i < packetCount; i++) {
            int currentPacketLength = Math.min(maxPacketSize, data.length - i * maxPacketSize);
            byte[] packet = new byte[currentPacketLength + 1];
            packet[0] = (byte) i;
            System.arraycopy(data, i * maxPacketSize, packet, 1, currentPacketLength);

            packets.add(packet);
        }
    }

    public void onFilePut(boolean success) {
    }

    @Override
    public byte[] getStartSequence() {
        return new byte[]{0x03};
    }

    @Override
    public int getPayloadLength() {
        return 15;
    }

    @Override
    public UUID getRequestUUID() {
        return UUID.fromString("3dda0003-957f-7d4a-34a6-74696673696d");
    }
}
