package nodomain.freeyourgadget.gadgetbridge.tasker.event;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.R;

/**
 * Default set of tasker events.
 * <p>
 * Extend here if you want to add more events. Use {@link TaskerEventType#(String, int)} if you want to use the event together with
 * {@link nodomain.freeyourgadget.gadgetbridge.tasker.settings.activities.TaskerEventsActivity}.
 * <p>
 * Don't forget to add the new event to {@link #getTypes()} method.
 */
public class TaskerEventType implements Serializable {

    public static TaskerEventType BUTTON = new TaskerEventType("button", R.string.tasker_event_button);
    public static TaskerEventType CONNECTION = new TaskerEventType("connection", R.string.tasker_event_connection);
    public static TaskerEventType DATA = new TaskerEventType("data", R.string.tasker_event_data);
    public static TaskerEventType NO_OP = new TaskerEventType("no-op");

    private String type;
    private int index;
    private int localization;

    public TaskerEventType(String type) {
        this.type = type;
        this.index = 1;
    }

    public TaskerEventType(String type, int localization) {
        this(type);
        this.localization = localization;
    }

    /**
     * Scopes the event with an index. I.e. for two buttons.
     *
     * @param index Event index
     * @return Scoped {@link TaskerEventType}
     */
    public TaskerEventType withIndex(int index) {
        TaskerEventType taskerEventType = new TaskerEventType(type, localization);
        taskerEventType.index = this.index;
        return taskerEventType;
    }

    public int getLocalization() {
        return localization;
    }

    public String getType() {
        return type;
    }

    public int getIndex() {
        return index;
    }

    public static List<TaskerEventType> getTypes() {
        return Arrays.asList(BUTTON, CONNECTION, DATA);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !getClass().isAssignableFrom(o.getClass())) return false;
        TaskerEventType that = (TaskerEventType) o;
        return index == that.index &&
                Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, index);
    }
}