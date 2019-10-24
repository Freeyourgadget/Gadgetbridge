package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit;

import java.nio.ByteBuffer;
import java.util.UUID;

public class SettingsFilePutRequest extends Request {
    public int fileLength;
    public byte[] file;

    public SettingsFilePutRequest(byte[] file){
        this.fileLength = file.length;
        this.file = file;
        ByteBuffer buffer = this.createBuffer();
        buffer.putShort(1, (short)0x0800);
        buffer.putInt(3, 0);
        buffer.putInt(7, fileLength - 10);
        buffer.putInt(11, fileLength - 10);

        this.data = buffer.array();
    }

    @Override
    public int getPayloadLength() {
        return 15;
    }

    @Override
    public byte[] getStartSequence() {
        return new byte[]{17};
    }

    @Override
    public UUID getRequestUUID() {
        return UUID.fromString("3dda0007-957f-7d4a-34a6-74696673696d");
    }
}
