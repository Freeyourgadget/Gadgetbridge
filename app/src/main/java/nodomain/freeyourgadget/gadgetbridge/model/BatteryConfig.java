package nodomain.freeyourgadget.gadgetbridge.model;

public class BatteryConfig {

    private final int batteryIndex;
    private final int icon;
    private final int label;

    public BatteryConfig(int batteryIndex, int icon, int label) {
        this.batteryIndex = batteryIndex;
        this.icon = icon;
        this.label = label;
    }

    public int getBatteryIndex() {
        return batteryIndex;
    }

    public int icon() {
        return icon;
    }

    public int label() {
        return label;
    }

}


