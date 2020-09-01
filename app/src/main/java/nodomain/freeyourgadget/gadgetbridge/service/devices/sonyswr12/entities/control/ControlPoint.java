package nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.entities.control;

import nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.util.ByteArrayWriter;

public abstract class ControlPoint {
    protected final CommandCode code;

    public ControlPoint(CommandCode code) {
        this.code = code;
    }

    protected final ByteArrayWriter getValueWriter() {
        final ByteArrayWriter byteArrayWriter = new ByteArrayWriter();
        byteArrayWriter.appendUint8(this.code.value);
        return byteArrayWriter;
    }
}
