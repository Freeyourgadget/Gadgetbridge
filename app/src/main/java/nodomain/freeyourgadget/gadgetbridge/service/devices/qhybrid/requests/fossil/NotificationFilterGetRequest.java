package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil;

import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.CRC32C;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;

public class NotificationFilterGetRequest extends FileGetRequest {
    public NotificationFilterGetRequest(FossilWatchAdapter adapter) {
        super((short) 0x0C00, adapter);
    }

    @Override
    void handleFileData(byte[] fileData) {
        log("handleFileData");
        ByteBuffer buffer = ByteBuffer.wrap(fileData);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        byte[] data = new byte[fileData.length - 12 - 4];

        System.arraycopy(fileData, 12, data, 0, data.length);

        CRC32C crc32c = new CRC32C();
        crc32c.update(data);

        if((int) crc32c.getValue() != buffer.getInt(fileData.length - 4)){
            throw new RuntimeException("CRC invalid");
        }
    }
}
