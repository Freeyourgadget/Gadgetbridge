package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.music;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FilePutRequest;

public class MusicInfoSetRequest extends FilePutRequest {
    public MusicInfoSetRequest(String artist, String album, String title, FossilWatchAdapter adapter) {
        super((short) 0x0400, createFile(artist, album, title), adapter);
    }

    private static byte[] createFile(String artist, String album, String title){
        int length = artist.length() + album.length() + title.length()
                + 3 // null terminators
                + 8; // length and header

        ByteBuffer buffer = ByteBuffer.allocate(length);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        buffer.putShort((short) length);
        buffer.put((byte) 0x01); // dunno
        buffer.put((byte) (title.length() + 1));
        buffer.put((byte) (artist.length() + 1));
        buffer.put((byte) (album.length() + 1));
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
