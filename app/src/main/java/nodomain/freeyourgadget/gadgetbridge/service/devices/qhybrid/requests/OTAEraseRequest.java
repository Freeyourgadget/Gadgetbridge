package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests;

import android.bluetooth.BluetoothGattCharacteristic;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

public class OTAEraseRequest extends Request {

    public OTAEraseRequest(int pageOffset) {
        ByteBuffer buffer = createBuffer();
        buffer.putShort((short) 23131);
        buffer.putInt(pageOffset);

        this.data = buffer.array();
    }

    @Override
    public byte[] getStartSequence() {
        return new byte[]{18};
    }

    @Override
    public int getPayloadLength() {
        return 7;
    }


    public UUID getRequestUUID(){
        return UUID.fromString("3dda0003-957f-7d4a-34a6-74696673696d");
    }

    @Override
    public void handleResponse(BluetoothGattCharacteristic characteristic) {
        byte[] bytes = characteristic.getValue();
        final ByteBuffer wrap = ByteBuffer.wrap(bytes);
        wrap.order(ByteOrder.LITTLE_ENDIAN);
        short fileHandle = wrap.getShort(1);
        byte status = wrap.get(3);
        int sizeWritten = wrap.getInt(4);
    }
}
