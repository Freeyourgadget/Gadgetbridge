package nodomain.freeyourgadget.gadgetbridge.model;

public class Measurement {
    private final int value;
    private final long timestamp;

    public Measurement(int value, long timestamp) {
        this.value = value;
        this.timestamp = timestamp;
    }

    public int getValue() {
        return value;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public int hashCode() {
        return (int) (71 ^ value ^ timestamp);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Measurement) {
            Measurement m = (Measurement) o;
            return timestamp == m.timestamp && value == m.value;
        }
        return super.equals(o);
    }
}
