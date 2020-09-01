package nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.entities.activity;

import nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.util.ByteArrayWriter;

public abstract class EventBase {
    protected final EventCode eventCode;

    protected EventBase(EventCode eventCode) {
        this.eventCode = eventCode;
    }

    public EventCode getCode() {
        return this.eventCode;
    }

    protected ByteArrayWriter getValueWriter() {
        ByteArrayWriter byteArrayWriter = new ByteArrayWriter();
        byteArrayWriter.appendUint8(this.eventCode.value);
        return byteArrayWriter;
    }
}
