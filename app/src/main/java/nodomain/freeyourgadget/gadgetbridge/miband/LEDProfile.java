package nodomain.freeyourgadget.gadgetbridge.miband;

import android.content.Context;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;

public class LEDProfile {
    public static final Context CONTEXT = GBApplication.getContext();


    public static final String ID_STACCATO = CONTEXT.getString(R.string.p_staccato);
    public static final String ID_SHORT = CONTEXT.getString(R.string.p_short);
    public static final String ID_MEDIUM = CONTEXT.getString(R.string.p_medium);
    public static final String ID_LONG = CONTEXT.getString(R.string.p_long);
    public static final String ID_WATERDROP = CONTEXT.getString(R.string.p_waterdrop);
    public static final String ID_RING = CONTEXT.getString(R.string.p_ring);
    public static final String ID_ALARM_CLOCK = CONTEXT.getString(R.string.p_alarm_clock);

    public static LEDProfile getProfile(String id, short repeat) {
        if (ID_STACCATO.equals(id)) {
            return new LEDProfile(id, new int[]{100, 0}, repeat, new int[] { LEDColors.YELLOW, 100, 0, LEDColors.RED, 100, 0 }, repeat);
        }
        if (ID_SHORT.equals(id)) {
            return new LEDProfile(id, new int[]{200, 200}, repeat, new int[] { LEDColors.GREEN, 200, 200 }, repeat);
        }
        if (ID_LONG.equals(id)) {
            return new LEDProfile(id, new int[]{500, 1000}, repeat, new int[] { LEDColors.MAGENTA, 500, 1000 }, repeat);
        }
        if (ID_WATERDROP.equals(id)) {
            return new LEDProfile(id, new int[]{100, 1500}, repeat, new int[] { LEDColors.BLUE, 100, 1500 }, repeat);
        }
        if (ID_RING.equals(id)) {
            return new LEDProfile(id, new int[]{300, 200, 600, 2000}, repeat, new int[] { LEDColors.CYAN, 300, 200, LEDColors.MAGENTA, 600, 2000 }, repeat);
        }
        if (ID_ALARM_CLOCK.equals(id)) {
            return new LEDProfile(id, new int[]{30, 35, 30, 35, 30, 35, 30, 800}, repeat, new int[] {LEDColors.BLUE, 30, 35, LEDColors.CYAN, 30, 35, LEDColors.BLUE, 30, 35, LEDColors.CYAN, 30, 800 }, repeat);
        }
        // medium
        return new LEDProfile(id, new int[]{300, 600}, repeat, new int[]{ LEDColors.YELLOW, 300, 600 }, repeat);
    }

    private final String id;

    private final int[] vibrationOnOffSequence;
    private int[] colorOnOffSequence;
    private short vibrationRepeat;
    private short colorRepeat;
    private boolean pulsate;

    /**
     * Creates a new profile instance.
     *
     * @param id            the ID, used as preference key.
     * @param vibrationOnOffSequence a sequence of alternating on and off durations, in milliseconds
     * @param vibrationRepeat        how often the sequence shall be repeated
     */
    public LEDProfile(String id, int[] vibrationOnOffSequence, short vibrationRepeat, int[] colorOnOffSequence, short colorRepeat) {
        this.id = id;
        this.vibrationRepeat = vibrationRepeat;
        this.vibrationOnOffSequence = vibrationOnOffSequence;
        this.colorOnOffSequence = colorOnOffSequence;
        this.colorRepeat = colorRepeat;
    }

    public String getId() {
        return id;
    }

    public int[] getVibrationOnOffSequence() {
        return vibrationOnOffSequence;
    }

    public int[] getColorOnOffSequence() {
        return colorOnOffSequence;
    }

    public short getVibrationRepeat() {
        return vibrationRepeat;
    }
    public short getColorRepeat() {
        return colorRepeat;
    }
}
