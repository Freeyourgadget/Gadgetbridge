/*  Copyright (C) 2019-2020 Daniel Dakhno

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.alarm;

import android.widget.Toast;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FileLookupAndGetRequest;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

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
        // TODO: This does nothing currently!
    }

    @Override
    public void handleFileLookupError(FILE_LOOKUP_ERROR error) {
        if(error == FILE_LOOKUP_ERROR.FILE_EMPTY){
            GB.toast("alarm file empty yet", Toast.LENGTH_LONG,  GB.ERROR);
        }else{
            throw new RuntimeException("strange lookup stuff");
        }
    }
}
