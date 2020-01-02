package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.image;

import java.io.ByteArrayOutputStream;

public class ImageRLEEncoder {
    public static byte[] RLEEncode(byte[] data) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length * 2);

        int lastByte = data[0];
        int count = 1;
        byte currentByte = -1;

        for (int i = 1; i < data.length; i++) {
            currentByte = data[i];

            if (currentByte != lastByte || count >= 255) {
                bos.write(data[i - 1]);
                bos.write(count);

                count = 1;
                lastByte = data[i];
            } else {
                count++;
            }
        }

        bos.write(currentByte);
        bos.write(count);

        byte[] result = bos.toByteArray();

        return result;
    }
}
