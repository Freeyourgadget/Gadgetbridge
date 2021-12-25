/*  Copyright (C) 2020-2021 opavlov

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
