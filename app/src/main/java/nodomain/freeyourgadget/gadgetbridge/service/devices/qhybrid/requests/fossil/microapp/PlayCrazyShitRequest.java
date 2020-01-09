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
package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.microapp;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FilePutRequest;

public class PlayCrazyShitRequest extends FilePutRequest {
    public PlayCrazyShitRequest(byte[] appData, FossilWatchAdapter adapter) {
        super((short) 0x0600, createPayload(appData), adapter);
    }

    private static byte[] createPayload(byte[] appData) {
        List<MicroAppCommand> commands = new ArrayList<>();

        commands.add(new StartCriticalCommand());
        // commands.add(new RepeatStartCommand((byte) 10));
        commands.add(new VibrateCommand(VibrationType.NORMAL));
        commands.add(new DelayCommand(1));
        // commands.add(new RepeatStopCommand());
        // commands.add(new StreamCommand((byte) 0b11111111));
        // commands.add(new AnimationCommand((short) 300, (short) 60));
        // commands.add(new DelayCommand(2));
        commands.add(new CloseCommand());

        int length = 0;

        for (MicroAppCommand command : commands) length += command.getData().length;

        ByteBuffer buffer = ByteBuffer.allocate(
                3 /* magic bytes */
                        + 8 /* button header copy */
                        + 1 /* 0xFF */
                        + 2 /* payload length */
                        + length
                        + 4 /* crc */
        );
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put((byte) 0x01);
        buffer.put((byte) 0x00);
        buffer.put((byte) 0x08);
        buffer.put(appData, 3, 8);
        buffer.put((byte) 0xFF);
        buffer.putShort((short)(length + 3));

        for(MicroAppCommand command : commands) buffer.put(command.getData());

        CRC32 crc = new CRC32();
        crc.update(buffer.array(), 0, buffer.position());

        buffer.putInt((int) crc.getValue());

        return buffer.array();
    }
}
