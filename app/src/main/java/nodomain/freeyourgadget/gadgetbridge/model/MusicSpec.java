package nodomain.freeyourgadget.gadgetbridge.model;

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
}
