package nodomain.freeyourgadget.gadgetbridge.service.devices.um25.Data;

import java.io.Serializable;

public class CaptureGroup implements Serializable {
    private int index;
    private int flownCurrent;
    private int flownWattage;

    public CaptureGroup(int index, int flownCurrent, int flownWattage) {
        this.flownCurrent = flownCurrent;
        this.flownWattage = flownWattage;
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getFlownCurrent() {
        return flownCurrent;
    }

    public void setFlownCurrent(int flownCurrent) {
        this.flownCurrent = flownCurrent;
    }

    public int getFlownWattage() {
        return flownWattage;
    }

    public void setFlownWattage(int flownWattage) {
        this.flownWattage = flownWattage;
    }
}
