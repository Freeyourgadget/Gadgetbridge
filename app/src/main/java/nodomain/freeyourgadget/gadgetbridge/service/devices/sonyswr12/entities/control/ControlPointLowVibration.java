package nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.entities.control;

import nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.util.UIntBitWriter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.util.ByteArrayWriter;

public final class ControlPointLowVibration extends ControlPoint {
    public boolean smartWakeUp;
    public boolean incomingCall;
    public boolean notification;

    public ControlPointLowVibration(boolean isEnabled){
        super(CommandCode.LOW_VIBRATION);
        this.smartWakeUp = isEnabled;
        this.incomingCall = isEnabled;
        this.notification = isEnabled;
    }

    public final byte[] toByteArray() {
        final UIntBitWriter uIntBitWriter = new UIntBitWriter(16);
        uIntBitWriter.append(13, 0);
        uIntBitWriter.appendBoolean(this.smartWakeUp);
        uIntBitWriter.appendBoolean(this.incomingCall);
        uIntBitWriter.appendBoolean(this.notification);
        final ByteArrayWriter byteArrayWriter = this.getValueWriter();
        byteArrayWriter.appendUint16((int) uIntBitWriter.getValue());
        return byteArrayWriter.getByteArray();
    }
}
