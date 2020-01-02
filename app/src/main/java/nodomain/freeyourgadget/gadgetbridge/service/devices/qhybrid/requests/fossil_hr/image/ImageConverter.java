package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.image;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.encoder.RLEEncoder;

public class ImageConverter {
    public static void encodeToTwoBitImage(byte monochromeImage){

    }

    public static byte[] encodeToRLEImage(byte[] monochromeImage, int height, int width) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(monochromeImage.length * 2);

        bos.write((byte) height);
        bos.write((byte) width);

        bos.write(RLEEncoder.RLEEncode(monochromeImage));

        bos.write((byte) 0x0FF);
        bos.write((byte) 0x0FF);

        return bos.toByteArray();
    }
}
