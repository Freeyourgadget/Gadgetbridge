package nodomain.freeyourgadget.gadgetbridge.model;

/**
 * Created by steffen on 07.06.16.
 */
public class MusicStateSpec {
    public static final int STATE_PLAYING = 0;
    public static final int STATE_PAUSED  = 1;
    public static final int STATE_STOPPED = 2;
    public static final int STATE_UNKNOWN = 3;

    public byte state;
    public int position;
    public int playRate;
    public byte shuffle;
    public byte repeat;
}
