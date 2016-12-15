package nodomain.freeyourgadget.gadgetbridge.devices.miband;

import android.content.Context;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.alertnotification.AlertLevel;

public class VibrationProfile {
    public static final Context CONTEXT = GBApplication.getContext();
    public static final String ID_STACCATO = CONTEXT.getString(R.string.p_staccato);
    public static final String ID_SHORT = CONTEXT.getString(R.string.p_short);
    public static final String ID_MEDIUM = CONTEXT.getString(R.string.p_medium);
    public static final String ID_LONG = CONTEXT.getString(R.string.p_long);
    public static final String ID_WATERDROP = CONTEXT.getString(R.string.p_waterdrop);
    public static final String ID_RING = CONTEXT.getString(R.string.p_ring);
    public static final String ID_ALARM_CLOCK = CONTEXT.getString(R.string.p_alarm_clock);

    public static VibrationProfile getProfile(String id, short repeat) {
        if (ID_STACCATO.equals(id)) {
            return new VibrationProfile(id, new int[]{100, 0}, repeat);
        }
        if (ID_SHORT.equals(id)) {
            return new VibrationProfile(id, new int[]{200, 200}, repeat);
        }
        if (ID_LONG.equals(id)) {
            return new VibrationProfile(id, new int[]{500, 1000}, repeat);
        }
        if (ID_WATERDROP.equals(id)) {
            return new VibrationProfile(id, new int[]{100, 1500}, repeat);
        }
        if (ID_RING.equals(id)) {
            return new VibrationProfile(id, new int[]{300, 200, 600, 2000}, repeat);
        }
        if (ID_ALARM_CLOCK.equals(id)) {
            return new VibrationProfile(id, new int[]{30, 35, 30, 35, 30, 35, 30, 800}, repeat);
        }
        // medium
        return new VibrationProfile(id, new int[]{300, 600}, repeat);
    }

    private final String id;

    private final int[] onOffSequence;
    private final short repeat;
    private int alertLevel = AlertLevel.MildAlert.getId();

    /**
     * Creates a new profile instance.
     *
     * @param id            the ID, used as preference key.
     * @param onOffSequence a sequence of alternating on and off durations, in milliseconds
     * @param repeat        how often the sequence shall be repeated
     */
    public VibrationProfile(String id, int[] onOffSequence, short repeat) {
        this.id = id;
        this.repeat = repeat;
        this.onOffSequence = onOffSequence;
    }

    public String getId() {
        return id;
    }

    public int[] getOnOffSequence() {
        return onOffSequence;
    }

    public short getRepeat() {
        return repeat;
    }

    public void setAlertLevel(int alertLevel) {
        this.alertLevel = alertLevel;
    }

    public int getAlertLevel() {
        return alertLevel;
    }
}
