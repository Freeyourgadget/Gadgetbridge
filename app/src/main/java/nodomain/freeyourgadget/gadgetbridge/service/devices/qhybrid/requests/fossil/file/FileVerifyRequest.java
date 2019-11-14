package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file;

import android.bluetooth.BluetoothGattCharacteristic;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.FossilRequest;

public class FileVerifyRequest extends FossilRequest {
    private boolean isFinished = false;
    private short handle;

    public FileVerifyRequest(short fileHandle) {
        this.handle = fileHandle;
        ByteBuffer buffer = this.createBuffer();
        buffer.putShort(fileHandle);

        this.data = buffer.array();
    }

    public short getHandle() {
        return handle;
    }

    @Override
    public void handleResponse(BluetoothGattCharacteristic characteristic) {
        super.handleResponse(characteristic);

        if(!characteristic.getUuid().toString().equals(this.getRequestUUID().toString())){
            throw new RuntimeException("wrong response UUID");
        }

        byte[] value = characteristic.getValue();

        byte type = (byte)(value[0] & 0x0F);

        if(type == 0x0A) return;

        if(type != 4) throw new RuntimeException("wrong response type");

        if(value.length != 4) throw new RuntimeException("wrong response length");

        ByteBuffer buffer = ByteBuffer.wrap(value);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        if(this.handle != buffer.getShort(1)) throw new RuntimeException("wrong response handle");

        byte status = buffer.get(3);

        if(status != 0) throw new RuntimeException("wrong response status");

        this.isFinished = true;

        this.onPrepare();
    }

    public void onPrepare(){}

    @Override
    public byte[] getStartSequence() {
        return new byte[]{4};
    }

    @Override
    public int getPayloadLength() {
        return 3;
    }

    @Override
    public boolean isFinished(){
        return this.isFinished;
    }

}
