package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.notification;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.stream.Stream;
import java.util.zip.CRC32;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.file.AssetFilePutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.utils.StringUtils;

public class NotificationImagePutRequest extends AssetFilePutRequest {
    private NotificationImagePutRequest(String packageName, byte[] file, FossilWatchAdapter adapter) {
        super(prepareFileCrc(packageName), file, adapter);
    }

    private NotificationImagePutRequest(NotificationImage image, FossilWatchAdapter adapter) {
        super(prepareFileCrc(image.getPackageName()), image.getImageData(), adapter);
    }

    public NotificationImagePutRequest(String[] fileNames, byte[][] files, FossilWatchAdapter adapter) throws IOException {
        super(prepareFileCrc(fileNames), files, adapter);
    }


    private static byte[][] prepareFileCrc(String[] packageNames){
        byte[][] names = new byte[packageNames.length][];
        for (int i = 0; i < packageNames.length; i++){
            names[i] = prepareFileCrc(packageNames[i]);
        }
        return names;
    }

    private static byte[] prepareFileCrc(String packageName){
        CRC32 crc = new CRC32();
        crc.update(packageName.getBytes());

        String crcString = StringUtils.bytesToHex(
                ByteBuffer
                        .allocate(4)
                        .order(ByteOrder.LITTLE_ENDIAN)
                        .putInt((int) crc.getValue())
                        .array()
        );

        ByteBuffer buffer = ByteBuffer.allocate(crcString.length() + 1)
                .put(crcString.getBytes())
                .put((byte) 0x00);

        return buffer.array();
    }
}
