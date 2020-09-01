package nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.entities.alarm;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.util.UIntBitWriter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.util.ByteArrayWriter;

public class BandAlarms {
    public final List<BandAlarm> alarms;

    public BandAlarms(List<BandAlarm> alarms) {
        this.alarms = alarms;
    }

    public byte[] toByteArray() {
        ByteArrayWriter byteArrayWriter = new ByteArrayWriter();
        if (this.alarms.size() == 0) {
            byteArrayWriter.appendUint32(1073741824L);
        } else {
            for (BandAlarm bandAlarm : this.alarms) {
                UIntBitWriter uIntBitWriter = new UIntBitWriter(32);
                uIntBitWriter.append(2, 0);
                uIntBitWriter.append(4, bandAlarm.index);
                uIntBitWriter.append(2, bandAlarm.state.value);
                uIntBitWriter.append(4, bandAlarm.interval);
                uIntBitWriter.append(6, bandAlarm.hour);
                uIntBitWriter.append(6, bandAlarm.minute);
                uIntBitWriter.append(1, 0);
                uIntBitWriter.append(7, bandAlarm.repeat.toInt());
                byteArrayWriter.appendUint32(uIntBitWriter.getValue());
            }
        }
        return byteArrayWriter.getByteArray();
    }
}
