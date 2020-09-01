package nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.entities.activity;

import nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.util.ByteArrayWriter;

public class EventWithValue extends EventBase {
    public final long value;

    public EventWithValue(EventCode eventCode, long value) {
        super(eventCode);
        this.value = value;
    }

    public byte[] toByteArray() {
        ByteArrayWriter byteArrayWriter = this.getValueWriter();
        byteArrayWriter.appendUint32(this.value);
        return byteArrayWriter.getByteArray();
    }
}
