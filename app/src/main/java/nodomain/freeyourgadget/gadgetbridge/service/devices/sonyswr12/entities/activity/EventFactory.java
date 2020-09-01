package nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.entities.activity;

import nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.util.IntFormat;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.util.ByteArrayReader;

public class EventFactory {

    public static EventBase readEventFromByteArray(byte[] array) {
        try {
            ByteArrayReader byteArrayReader = new ByteArrayReader(array);
            EventCode eventCode = EventCode.fromInt(byteArrayReader.readUint8());
            switch (eventCode) {
                case HEART_RATE: {
                    long value = byteArrayReader.readInt(IntFormat.UINT32);
                    return new EventWithValue(eventCode, value);
                }
                case ACTIVITY_DATA: {
                    return EventWithActivity.fromByteArray(byteArrayReader);
                }
                default: return null;
            }
        } catch (Exception ex) {
            return null;
        }
    }
}
