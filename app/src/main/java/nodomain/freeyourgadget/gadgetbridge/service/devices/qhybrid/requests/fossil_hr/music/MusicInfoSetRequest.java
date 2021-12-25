/*  Copyright (C) 2019-2021 Daniel Dakhno

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.music;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.file.FileHandle;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FilePutRequest;

public class MusicInfoSetRequest extends FilePutRequest {
    public MusicInfoSetRequest(String artist, String album, String title, FossilWatchAdapter adapter) {
        super(FileHandle.MUSIC_INFO, createFile(artist, album, title), adapter);
    }

    private static byte[] createFile(String artist, String album, String title) {
        //counting byte array length because of utf chars, they may take up two bytes
        int titleLength = title.getBytes().length + 1; // +1 = null terminator
        int albumLength = album.getBytes().length + 1;
        int artistLength = artist.getBytes().length + 1;

        int length = artistLength + albumLength + titleLength
                + 8; // length and header

        ByteBuffer buffer = ByteBuffer.allocate(length);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        buffer.putShort((short) length);
        buffer.put((byte) 0x01); // dunno
        buffer.put((byte) (titleLength));
        buffer.put((byte) (artistLength));
        buffer.put((byte) (albumLength));
        buffer.put((byte) 0x0C); // dunno
        buffer.put((byte) 0x00); // dunno

        buffer.put(title.getBytes())
                .put((byte) 0x00); // null terminator

        buffer.put(artist.getBytes())
                .put((byte) 0x00); // null terminator

        buffer.put(album.getBytes())
                .put((byte) 0x00); // null terminator

        return buffer.array();
    }
}
