package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class MusicControlEntityUpdateMessage extends GFDIMessage {

    private final Map<MusicEntity, String> attributes;

    public MusicControlEntityUpdateMessage(Map<MusicEntity, String> attributes) {

        this.attributes = attributes;
        this.garminMessage = GarminMessage.MUSIC_CONTROL_ENTITY_UPDATE;

    }

    @Override
    protected boolean generateOutgoing() {
        final MessageWriter writer = new MessageWriter(response);
        writer.writeShort(0); // packet size will be filled below
        writer.writeShort(this.garminMessage.getId());

        for (Map.Entry<MusicEntity, String> entry : attributes.entrySet()) {
            MusicEntity a = entry.getKey();
            String value = entry.getValue();
            if (null == value)
                value = "";
            byte[] v = value.getBytes(StandardCharsets.UTF_8);
            if (v.length > 252) throw new IllegalArgumentException("Too long value");

            writer.writeByte((v.length + 3) & 0xff); //the three following bytes
            writer.writeByte(a.getEntityId());
            writer.writeByte(a.ordinal());
            writer.writeByte(0);//TODO what is this?
            writer.writeBytes(v);
        }
        return true;
    }

    public enum PLAYER implements MusicEntity {
        NAME,
        PLAYBACK_INFO,
        VOLUME;

        @Override
        public int getEntityId() {
            return 0;
        }
    }


    public enum QUEUE implements MusicEntity {
        INDEX,
        COUNT,
        SHUFFLE,
        REPEAT;

        @Override
        public int getEntityId() {
            return 1;
        }
    }

    public enum TRACK implements MusicEntity {
        ARTIST,
        ALBUM,
        TITLE,
        DURATION;

        @Override
        public int getEntityId() {
            return 2;
        }
    }

    public interface MusicEntity {
        int getEntityId();

        int ordinal();
    }
}
