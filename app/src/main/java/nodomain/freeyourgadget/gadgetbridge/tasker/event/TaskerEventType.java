package nodomain.freeyourgadget.gadgetbridge.tasker.event;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.R;

public class TaskerEventType {

    public static TaskerEventType BUTTON = TaskerEventType.create("button").withLocalization(R.string.tasker_event_button);
    public static TaskerEventType CONNECTION = TaskerEventType.create("connection").withLocalization(R.string.tasker_event_connection);
    public static TaskerEventType DATA = TaskerEventType.create("data").withLocalization(R.string.tasker_event_data);
    public static TaskerEventType NO_OP = TaskerEventType.create("no-op");

    private String type;
    private int index;
    private int localization;

    private TaskerEventType() {
    }

    public TaskerEventType withLocalization(int localization) {
        this.localization = localization;
        return this;
    }

    public static TaskerEventType create(String type) {
        TaskerEventType taskerEventType = new TaskerEventType();
        taskerEventType.index = 1;
        taskerEventType.type = type;
        return taskerEventType;
    }

    public TaskerEventType withIndex(int index) {
        TaskerEventType taskerEventType = new TaskerEventType();
        taskerEventType.type = this.type;
        taskerEventType.index = index;
        taskerEventType.localization = this.localization;
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
        if (o == null || getClass() != o.getClass()) return false;
        TaskerEventType that = (TaskerEventType) o;
        return index == that.index &&
                type == that.type;
    }

    @Override
    public int hashCode() {

        return Objects.hash(type, index);
    }
}