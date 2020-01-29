package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.notification;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.zip.CRC32;

import nodomain.freeyourgadget.gadgetbridge.devices.qhybrid.NotificationHRConfiguration;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FilePutRequest;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class NotificationFilterPutHRRequest extends FilePutRequest {
    public NotificationFilterPutHRRequest(NotificationHRConfiguration[] configs, FossilWatchAdapter adapter) {
        super((short) 0x0C00, createFile(configs), adapter);
    }


    public NotificationFilterPutHRRequest(ArrayList<NotificationHRConfiguration> configs, FossilWatchAdapter adapter) {
        super((short) 0x0C00, createFile(configs.toArray(new NotificationHRConfiguration[0])), adapter);
    }

    private static byte[] createFile(NotificationHRConfiguration[] configs) {
        ByteBuffer buffer = ByteBuffer.allocate(configs.length * 28);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        for (NotificationHRConfiguration config : configs) {
            buffer.putShort((short) 28); //packet length

            CRC32 crc = new CRC32();
            crc.update(config.getPackageName().getBytes());

            byte[] crcBytes = ByteBuffer
                    .allocate(4)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .putInt((int) crc.getValue())
                    .array();

            // 6 bytes
            buffer.put(PacketID.PACKAGE_NAME_CRC.id)
                    .put((byte) 4)
                    .put(crcBytes);

            // 3 bytes
            buffer.put(PacketID.GROUP_ID.id)
                    .put((byte) 1)
                    .put((byte) 2);

            // 3 bytes
            buffer.put(PacketID.PRIORITY.id)
                    .put((byte) 1)
                    .put((byte) 0xFF);

            // 14 bytes
            buffer.put(PacketID.ICON.id)
                    .put((byte) 0x0C)
                    .put((byte) 0xFF)
                    .put((byte) 0x00)
                    .put((byte) 0x09)
                    .put(StringUtils.bytesToHex(crcBytes).getBytes())
                    .put((byte) 0x00);

        }

        return buffer.array();
    }

    enum PacketID {
        PACKAGE_NAME((byte) 0x01),
        SENDER_NAME((byte) 0x02),
        PACKAGE_NAME_CRC((byte) 0x04),
        GROUP_ID((byte) 0x80),
        APP_DISPLAY_NAME((byte) 0x81),
        ICON((byte) 0x82),
        PRIORITY((byte) 0xC1),
        MOVEMENT((byte) 0xC2),
        VIBRATION((byte) 0xC3);

        byte id;

        PacketID(byte id) {
            this.id = id;
        }
    }

    enum VibrationType {
        SINGLE_SHORT((byte) 5),
        DOUBLE_SHORT((byte) 6),
        TRIPLE_SHORT((byte) 7),
        SINGLE_LONG((byte) 8),
        SILENT((byte) 9);

        byte id;

        VibrationType(byte id) {
            this.id = id;
        }
    }
}
