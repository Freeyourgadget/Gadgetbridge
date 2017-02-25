package nodomain.freeyourgadget.gadgetbridge.model;

import java.util.Objects;

public class MusicSpec {
    public static final int MUSIC_UNDEFINED = 0;
    public static final int MUSIC_PLAY = 1;
    public static final int MUSIC_PAUSE = 2;
    public static final int MUSIC_PLAYPAUSE = 3;
    public static final int MUSIC_NEXT = 4;
    public static final int MUSIC_PREVIOUS = 5;

    public String artist;
    public String album;
    public String track;
    public int duration;
    public int trackCount;
    public int trackNr;

    public MusicSpec() {

    }

    public MusicSpec(MusicSpec old) {
        this.duration = old.duration;
        this.trackCount = old.trackCount;
        this.trackNr = old.trackNr;
        this.track = old.track;
        this.album = old.album;
        this.artist = old.artist;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof MusicSpec)) {
            return false;
        }
        MusicSpec musicSpec = (MusicSpec) obj;

        return Objects.equals(this.artist, musicSpec.artist) &&
                Objects.equals(this.album, musicSpec.album) &&
                Objects.equals(this.track, musicSpec.track) &&
                this.duration == musicSpec.duration &&
                this.trackCount == musicSpec.trackCount &&
                this.trackNr == musicSpec.trackNr;

    }
}
