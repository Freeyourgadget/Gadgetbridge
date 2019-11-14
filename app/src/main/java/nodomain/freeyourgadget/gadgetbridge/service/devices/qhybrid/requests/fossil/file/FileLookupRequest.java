package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file;

import android.bluetooth.BluetoothGattCharacteristic;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.UUID;
import java.util.zip.CRC32;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.Request;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.FossilRequest;

public class FileLookupRequest extends FossilRequest {
    private short handle = -1;
    private byte fileType;

    private FossilWatchAdapter adapter;

    private ByteBuffer fileBuffer;

    private byte[] fileData;

    protected boolean finished = false;

    public FileLookupRequest(byte fileType, FossilWatchAdapter adapter) {
        this.fileType = fileType;
        this.adapter = adapter;

        this.data =
                createBuffer()
                        .put(fileType)
                        .array();
    }

    protected FossilWatchAdapter getAdapter() {
        return adapter;
    }

    public short getHandle() {
        if(!finished){
            throw new UnsupportedOperationException("File lookup not finished");
        }
        return handle;
    }

    @Override
    public boolean isFinished(){
        return finished;
    }

    @Override
    public void handleResponse(BluetoothGattCharacteristic characteristic) {
        byte[] value = characteristic.getValue();
        byte first = value[0];
        if(characteristic.getUuid().toString().equals("3dda0003-957f-7d4a-34a6-74696673696d")){
            if((first & 0x0F) == 2){
                ByteBuffer buffer = ByteBuffer.wrap(value);
                buffer.order(ByteOrder.LITTLE_ENDIAN);

                short handle = buffer.getShort(1);
                int size = buffer.getInt(4);

                byte status = buffer.get(3);

                if(status != 0){
                    throw new RuntimeException("file lookup error: " + status);
                }

                if(this.handle != handle){
                    // throw new RuntimeException("handle: " + handle + "   expected: " + this.handle);
                }
                log("file size: " + size);
                fileBuffer = ByteBuffer.allocate(size);
            }else if((first & 0x0F) == 8){
                this.finished = true;

                ByteBuffer buffer = ByteBuffer.wrap(value);
                buffer.order(ByteOrder.LITTLE_ENDIAN);

                CRC32 crc = new CRC32();
                crc.update(this.fileData);

                int crcExpected = buffer.getInt(8);

                if((int) crc.getValue() != crcExpected){
                    throw new RuntimeException("handle: " + handle + "   expected: " + this.handle);
                }

                ByteBuffer dataBuffer = ByteBuffer.wrap(fileData);
                dataBuffer.order(ByteOrder.LITTLE_ENDIAN);

                this.handle = dataBuffer.getShort(0);

                this.handleFileLookup(this.handle);
            }
        }else if(characteristic.getUuid().toString().equals("3dda0004-957f-7d4a-34a6-74696673696d")){
            fileBuffer.put(value, 1, value.length - 1);
            if((first & 0x80) == 0x80){
                this.fileData = fileBuffer.array();
            }
        }
    }

    public void handleFileLookup(short fileHandle){}

    @Override
    public UUID getRequestUUID() {
        return UUID.fromString("3dda0003-957f-7d4a-34a6-74696673696d");
    }

    @Override
    public byte[] getStartSequence() {
        return new byte[]{2, (byte) 0xFF};
    }

    @Override
    public int getPayloadLength() {
        return 3;
    }
}
