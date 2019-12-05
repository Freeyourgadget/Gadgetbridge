package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil;

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.Request;

public abstract class FossilRequest extends Request {
    public abstract boolean isFinished();

    public byte getType(){
        return getStartSequence()[0];
    }

    @Override
    public UUID getRequestUUID() {
        return UUID.fromString("3dda0003-957f-7d4a-34a6-74696673696d");
    }
}
