package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.notification;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.zip.CRC32;

import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.NotificationConfiguration;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FileCloseAndPutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FilePutRequest;

public class NotificationFilterPutRequest extends FilePutRequest {
    public NotificationFilterPutRequest(NotificationConfiguration[] configs, FossilWatchAdapter adapter) {
        super((short) 0x0C00, createFile(configs), adapter);
    }


    public NotificationFilterPutRequest(ArrayList<NotificationConfiguration> configs, FossilWatchAdapter adapter) {
        super((short) 0x0C00, createFile(configs.toArray(new NotificationConfiguration[0])), adapter);
    }

    private static byte[] createFile(NotificationConfiguration[] configs){
        ByteBuffer buffer = ByteBuffer.allocate(configs.length * 27);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        for(NotificationConfiguration config : configs){
            buffer.putShort((short) 25); //packet length

            CRC32 crc = new CRC32();
            crc.update(config.getPackageName().getBytes());

            buffer.put(PacketID.PACKAGE_NAME_CRC.id);
            buffer.put((byte) 4);
            buffer.putInt((int) crc.getValue());

            buffer.put(PacketID.GROUP_ID.id);
            buffer.put((byte) 1);
            buffer.put((byte) 2);

            buffer.put(PacketID.PRIORITY.id);
            buffer.put((byte) 1);
            buffer.put((byte) 0xFF);

            buffer.put(PacketID.MOVEMENT.id);
            buffer.put((byte) 8);
            buffer.putShort(config.getHour())
                    .putShort(config.getMin())
                    .putShort(config.getSubEye())
                    .putShort((short) 5000);

            buffer.put(PacketID.VIBRATION.id);
            buffer.put((byte) 1);
            buffer.put(config.getVibration().getValue());
        }

        return buffer.array();
    }

    enum PacketID{
        PACKAGE_NAME((byte) 1),
        SENDER_NAME((byte) 2),
        PACKAGE_NAME_CRC((byte) 4),
        GROUP_ID((byte) 128),
        APP_DISPLAY_NAME((byte) 129),
        PRIORITY((byte) 0xC1),
        MOVEMENT((byte) 0xC2),
        VIBRATION((byte) 0xC3);

        byte id;

        PacketID(byte id){
            this.id = id;
        }
    }

    enum VibrationType{
        SINGLE_SHORT((byte) 5),
        DOUBLE_SHORT((byte) 6),
        TRIPLE_SHORT((byte) 7),
        SINGLE_LONG((byte) 8),
        SILENT((byte) 9);

        byte id;
        VibrationType(byte id){
            this.id = id;
        }
    }
}
