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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FilePutRequest;
import nodomain.freeyourgadget.gadgetbridge.util.Version;

public class AlarmsSetRequest extends FilePutRequest {
    public AlarmsSetRequest(Alarm[] alarms, FossilWatchAdapter adapter) {
        super((short) 0x0A00, createFileFromAlarms(alarms, isNewFormat(adapter)), isNewFormat(adapter) ? (short) 3 : (short) 2, adapter); // TODO version 3
    }

    static private boolean isNewFormat(FossilWatchAdapter adapter) {
        GBDevice device = adapter.getDeviceSupport().getDevice();
        String firmware = device.getFirmwareVersion();

        Version newFormatVersion = new Version("1.0.2.17");
        Pattern versionPattern = Pattern.compile("([0-9]+)\\.([0-9]+)\\.([0-9]+)\\.([0-9]+)");
        Matcher matcher = versionPattern.matcher(firmware);
        matcher.find();
        String thisVersion = matcher.group(0);

        int result = newFormatVersion.compareTo(new Version(thisVersion));

        return result != 1;
    }

    static public byte[] createFileFromAlarms(Alarm[] alarms, boolean newFormat) {
        ByteBuffer buffer;
        if (!newFormat) {
            buffer = ByteBuffer.allocate(alarms.length * 3);
            for (Alarm alarm : alarms) buffer.put(alarm.getData());
        } else {
            String label = "Brr Brr";
            String message = "I am an alarm";

            label = label.substring(0, Math.min(label.length(), 15));
            message = message.substring(0, Math.min(message.length(), 50));

            int SIZE_ALARM = 17 + label.length() + message.length();
            int sizeAll = alarms.length * SIZE_ALARM;
            buffer = ByteBuffer.allocate(sizeAll); // 4 for overall length
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            for (Alarm alarm : alarms) {
                buffer.put((byte) 0x00); // dunno why
                buffer.putShort((short) (SIZE_ALARM - 3)); // alarm size, 0 above does not count
                buffer.put((byte) 0x00); // prolly entry id time data
                buffer.putShort((short) 3); // prolly entry length
                buffer.put(alarm.getData());

                buffer.put((byte) 0x01); // another entry id label
                buffer.putShort((short) (label.length() + 1));  // entry length
                buffer.put(label.getBytes());
                buffer.put((byte) 0x00); // null terminator

                buffer.put((byte) 0x02); // entry id subtext
                buffer.putShort((short) (message.length() + 1)); // entry length
                buffer.put(message.getBytes());
                buffer.put((byte) 0x00); // null terminator
            }
        }

        return buffer.array();
    }
}
