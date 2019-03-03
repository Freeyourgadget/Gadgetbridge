package nodomain.freeyourgadget.gadgetbridge.tasker.event;

import nodomain.freeyourgadget.gadgetbridge.tasker.settings.TaskerSettings;

/**
 * Tasker event that gets thrown if its corresponding {@link TaskerSettings#isEnabled()} is true.
 * <p>
 * Provides {@link TaskerEventType} and a count.
 * <p>
 * Count resets itself after {@link TaskerSettings#getThreshold()} is expired.
 */
public class TaskerEvent {

    private TaskerEventType type;
    private int count;

    public TaskerEvent(TaskerEventType type, int count) {
        this.type = type;
        this.count = count;
    }

    /**
     * Tasker event type of this event.
     *
     * @return
     */
    public TaskerEventType getType() {
        return type;
    }

    public void setType(TaskerEventType type) {
        this.type = type;
    }

    /**
     * Count how often this event is thrown in the {@link TaskerSettings#getThreshold()}
     *
     * @return Fired times
     */
    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}