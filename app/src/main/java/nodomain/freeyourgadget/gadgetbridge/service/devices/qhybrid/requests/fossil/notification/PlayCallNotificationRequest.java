package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.notification;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;

public class PlayCallNotificationRequest extends PlayNotificationRequest {
    private final static int MESSAGE_ID_CALL = 1;

    public PlayCallNotificationRequest(String number, boolean callStart, FossilWatchAdapter adapter) {
        super(callStart ? 1 : 7, callStart ? 8 : 2,
                ByteBuffer.wrap(new byte[]{(byte) 0x80, (byte) 0x00, (byte) 0x59, (byte) 0xB7}).order(ByteOrder.LITTLE_ENDIAN).getInt(),
                number, "Incoming Call", MESSAGE_ID_CALL, adapter);
    }
}
