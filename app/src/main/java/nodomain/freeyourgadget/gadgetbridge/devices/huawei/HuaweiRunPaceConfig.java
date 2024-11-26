package nodomain.freeyourgadget.gadgetbridge.devices.huawei;

// TODO: make this configurable
// NOTE: algorithms used in this class are generic. So this data can be used with other devices.
// We can move this class to global scope.
public class HuaweiRunPaceConfig {

    private int zone5HIITRunMax = 300;
    private int zone5HIITRunMin = 330;
    private int zone4AnaerobicMin = 360;
    private int zone3LactateThresholdMin = 390;
    private int zone2MarathonMin = 420;
    private int zone1JogMin = 450;

    public int getZone5HIITRunMax() {
        return zone5HIITRunMax;
    }

    public void setZone5HIITRunMax(int zone5HIITRunMax) {
        this.zone5HIITRunMax = zone5HIITRunMax;
    }

    public int getZone5HIITRunMin() {
        return zone5HIITRunMin;
    }

    public void setZone5HIITRunMin(int zone5HIITRunMin) {
        this.zone5HIITRunMin = zone5HIITRunMin;
    }

    public int getZone4AnaerobicMin() {
        return zone4AnaerobicMin;
    }

    public void setZone4AnaerobicMin(int zone4AnaerobicMin) {
        this.zone4AnaerobicMin = zone4AnaerobicMin;
    }

    public int getZone3LactateThresholdMin() {
        return zone3LactateThresholdMin;
    }

    public void setZone3LactateThresholdMin(int zone3LactateThresholdMin) {
        this.zone3LactateThresholdMin = zone3LactateThresholdMin;
    }

    public int getZone2MarathonMin() {
        return zone2MarathonMin;
    }

    public void setZone2MarathonMin(int zone2MarathonMin) {
        this.zone2MarathonMin = zone2MarathonMin;
    }

    public int getZone1JogMin() {
        return zone1JogMin;
    }

    public void setZone1JogMin(int zone1JogMin) {
        this.zone1JogMin = zone1JogMin;
    }
}
