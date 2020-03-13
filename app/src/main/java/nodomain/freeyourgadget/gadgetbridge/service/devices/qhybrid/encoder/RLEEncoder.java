package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.encoder;

import java.io.ByteArrayOutputStream;

public class RLEEncoder {
    public static byte[] RLEEncode(byte[] data) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length * 2);

        int lastByte = data[0];
        int count = 1;
        byte currentByte = -1;

        for (int i = 1; i < data.length; i++) {
            currentByte = data[i];

            if (currentByte != lastByte || count >= 255) {
                bos.write(count);
                bos.write(data[i - 1]);

                count = 1;
                lastByte = data[i];
            } else {
                count++;
            }
        }

        bos.write(count);
        bos.write(currentByte);

        byte[] result = bos.toByteArray();

        return result;
    }
}
