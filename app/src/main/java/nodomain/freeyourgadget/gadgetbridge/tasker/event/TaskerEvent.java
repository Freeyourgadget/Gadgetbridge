package nodomain.freeyourgadget.gadgetbridge.tasker.event;

public class TaskerEvent {

    private TaskerEventType type;
    private int count;

    public TaskerEvent(TaskerEventType type, int count) {
        this.type = type;
        this.count = count;
    }

    public TaskerEventType getType() {
        return type;
    }

    public void setType(TaskerEventType type) {
        this.type = type;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}