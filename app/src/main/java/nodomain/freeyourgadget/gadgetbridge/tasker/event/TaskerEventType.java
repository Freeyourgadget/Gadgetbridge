package nodomain.freeyourgadget.gadgetbridge.tasker.event;

import java.util.Objects;

public class TaskerEventType {

    public static TaskerEventType BUTTON = TaskerEventType.create("button");
    public static TaskerEventType CONNECTION = TaskerEventType.create("connection");
    public static TaskerEventType DATA = TaskerEventType.create("data");

    private String type;
    private int index;

    private TaskerEventType(String type) {
        this.type = type;
        this.index = 1;
    }

    private TaskerEventType(String type, int index) {
        this.type = type;
        this.index = index;
    }

    public static TaskerEventType create(String type) {
        return new TaskerEventType(type);
    }

    public TaskerEventType setIndex(int index) {
        return new TaskerEventType(this.type, index);
    }

    public String getType() {
        return type;
    }

    public int getIndex() {
        return index;
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