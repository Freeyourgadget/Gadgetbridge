package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.alarm;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FilePutRequest;

public class AlarmsSetRequest extends FilePutRequest {
    public AlarmsSetRequest(Alarm[] alarms, FossilWatchAdapter adapter) {
        super((short) 0x0A00, createFileFromAlarms(alarms), adapter);
    }

    static byte[] createFileFromAlarms(Alarm[] alarms){
        ByteBuffer buffer = ByteBuffer.allocate(alarms.length * 3);
        for(Alarm alarm : alarms) buffer.put(alarm.getData());

        return buffer.array();
    }
}
