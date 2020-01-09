package nodomain.freeyourgadget.gadgetbridge.service.devices.jyou;

import java.util.Timer;
import java.util.TimerTask;

import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;

public abstract class RealtimeSamplesSupport {
    private final long delay;
    private final long period;

    protected int steps;
    protected int heartrateBpm;
    private int lastSteps;
    // subclasses may add more

    private Timer realtimeStorageTimer;

    public RealtimeSamplesSupport(long delay, long period) {
        this.delay = delay;
        this.period = period;
    }

    public synchronized void start() {
        if (isRunning()) {
            return; // already running
        }
        realtimeStorageTimer = new Timer("JYou Realtime Storage Timer");
        realtimeStorageTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                triggerCurrentSample();
            }
        }, delay, period);
    }

    public synchronized void stop() {
        if (realtimeStorageTimer != null) {
            realtimeStorageTimer.cancel();
            realtimeStorageTimer.purge();
            realtimeStorageTimer = null;
        }
    }

    public synchronized boolean isRunning() {
        return realtimeStorageTimer != null;
    }

    public synchronized void setSteps(int stepsPerMinute) {
        this.steps = stepsPerMinute;
    }

    /**
     * Returns the number of steps recorded since the last measurements. If no
     * steps are available yet, ActivitySample.NOT_MEASURED is returned.
     * @return
     */
    public synchronized int getSteps() {
        if (steps == ActivitySample.NOT_MEASURED) {
            return ActivitySample.NOT_MEASURED;
        }
        if (lastSteps == 0)  {
            return ActivitySample.NOT_MEASURED; // wait until we have a delta between two samples
        }
        int delta = steps - lastSteps;
        if (delta < 0) {
            return 0;
        }
        return delta;
    }

    public void setHeartrateBpm(int hrBpm) {
        this.heartrateBpm = hrBpm;
    }

    public int getHeartrateBpm() {
        return heartrateBpm;
    }

    public void triggerCurrentSample() {
        doCurrentSample();
        resetCurrentValues();
    }

    protected synchronized void resetCurrentValues() {
        if (steps >= lastSteps) {
            lastSteps = steps;
        }
        steps = ActivitySample.NOT_MEASURED;
        heartrateBpm = ActivitySample.NOT_MEASURED;
    }

    protected abstract void doCurrentSample();
}

