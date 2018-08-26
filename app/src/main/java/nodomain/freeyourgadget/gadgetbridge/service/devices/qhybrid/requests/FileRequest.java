package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests;

import java.util.UUID;

public class FileRequest extends Request {
    public boolean completed = false;
    public int status;
    @Override
    public UUID getRequestUUID() {
        return UUID.fromString("3dda0003-957f-7d4a-34a6-74696673696d");
    }

    @Override
    public byte[] getStartSequence() {
        return null;
    }
}
