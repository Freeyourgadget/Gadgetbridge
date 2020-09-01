package nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.entities.control;

import nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.util.ByteArrayWriter;

public class ControlPointWithValue extends ControlPoint {
    protected final int value;

    public ControlPointWithValue(final CommandCode commandCode, final int value) {
        super(commandCode);
        if (value < 0 || value > 65535) {
            throw new IllegalArgumentException("command value out of range " + value);
        }
        this.value = value;
    }

    public final byte[] toByteArray() {
        final ByteArrayWriter byteArrayWriter = this.getValueWriter();
        byteArrayWriter.appendUint16(this.value);
        return byteArrayWriter.getByteArray();
    }
}