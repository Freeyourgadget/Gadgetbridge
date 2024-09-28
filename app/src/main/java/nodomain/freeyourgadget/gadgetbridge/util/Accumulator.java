package nodomain.freeyourgadget.gadgetbridge.util;

/**
 * A simple class to accumulate stats (min, max, count, avg).
 */
public class Accumulator {
    private double min = Double.MAX_VALUE;
    private double max = -Double.MAX_VALUE;
    private double sum = 0;
    private int count;

    public void add(final double value) {
        sum += value;
        count++;

        if (value > max) {
            max = value;
        }
        if (value < min) {
            min = value;
        }
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getSum() {
        return sum;
    }

    public int getCount() {
        return count;
    }

    public double getAverage() {
        if (count > 0) {
            return sum / count;
        } else {
            return 0;
        }
    }
}
