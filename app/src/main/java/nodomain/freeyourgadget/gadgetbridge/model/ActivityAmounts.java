package nodomain.freeyourgadget.gadgetbridge.model;

import java.util.ArrayList;
import java.util.List;

public class ActivityAmounts {
    private List<ActivityAmount> amounts = new ArrayList<>(4);
    private long totalSeconds;

    public void addAmount(ActivityAmount amount) {
        amounts.add(amount);
        totalSeconds += amount.getTotalSeconds();
    }

    public List<ActivityAmount> getAmounts() {
        return amounts;
    }

    public long getTotalSeconds() {
        return totalSeconds;
    }

    public void calculatePercentages() {
        for (ActivityAmount amount : amounts) {
            float fraction = amount.getTotalSeconds() / (float) totalSeconds;
            amount.setPercent((short) (fraction * 100));
        }
    }
}
