package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.alarm;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FileGetRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FileLookupAndGetRequest;

public class AlarmsGetRequest extends FileLookupAndGetRequest {
    public AlarmsGetRequest(FossilWatchAdapter adapter) {
        super((byte) 0x0A, adapter);
    }

    @Override
    public void handleFileData(byte[] fileData) {
        ByteBuffer buffer = ByteBuffer.wrap(fileData);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        short handle = buffer.getShort(0);
        if(handle != (short) 0x0A00) throw new RuntimeException("wrong alarm handle");

        int length = buffer.getInt(8) / 3;
        Alarm[] alarms = new Alarm[length];

        for (int i = 0; i < length; i++){
            buffer.position(12 + i * 3);
            byte[] alarmBytes = new byte[]{
                    buffer.get(),
                    buffer.get(),
                    buffer.get()
            };
            alarms[i] = Alarm.fromBytes(alarmBytes);
        }

        this.handleAlarms(alarms);
    }

    public void handleAlarms(Alarm[] alarms){
        Alarm[] alarms2 = new Alarm[alarms.length];

        for(int i = 0; i < alarms.length; i++){
            alarms2[i] = Alarm.fromBytes(alarms[i].getData());
        }
    }
}
