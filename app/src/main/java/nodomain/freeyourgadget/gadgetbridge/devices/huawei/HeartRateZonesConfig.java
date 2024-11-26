package nodomain.freeyourgadget.gadgetbridge.devices.huawei;

// TODO: make this configurable
// TODO: retrieve from device and set real Rest HR
// NOTE: algorithms used in this class are generic. So this data can be used with other devices.
// We can move this class to global scope.
public class HeartRateZonesConfig {

    public static final int TYPE_UPRIGHT = 1;
    public static final int TYPE_SITTING = 2;
    public static final int TYPE_SWIMMING = 3;
    public static final int TYPE_OTHER = 4;

    public static final int CALCULATE_METHOD_MHR = 0;
    public static final int CALCULATE_METHOD_HRR = 1;
    public static final int CALCULATE_METHOD_LTHR = 3;


    private static final int DEFAULT_REST_HEART_RATE = 60;
    public static final int MAXIMUM_HEART_RATE = 220;

    private final int configType;
    private int calculateMethod = CALCULATE_METHOD_MHR; // 0 - MHR, 1 - HRR, 3 - LTHR

    private int maxHRThreshold;
    private int restHeartRate = DEFAULT_REST_HEART_RATE;

    private boolean warningEnable;
    private int warningHRLimit;

    //MHR percentage
    private int MHRExtreme;
    private int MHRAnaerobic;
    private int MHRAerobic;
    private int MHRFatBurning;
    private int MHRWarmUp;

    //HRR percentage
    private int HRRAdvancedAnaerobic;
    private int HRRBasicAnaerobic;
    private int HRRLactate;
    private int HRRAdvancedAerobic;
    private int HRRBasicAerobic;

    //LTHR percentage
    private int LTHRAnaerobic;
    private int LTHRLactate;
    private int LTHRAdvancedAerobic;
    private int LTHRBasicAerobic;
    private int LTHRWarmUp;


    private int LTHRThresholdHeartRate;


    public HeartRateZonesConfig(int type, int age) {
        this.configType = type;
        this.warningEnable = true;
        this.warningHRLimit = MAXIMUM_HEART_RATE - age;
        resetHRZones(age);
    }

    public void resetHRZones(int age) {
        this.maxHRThreshold = (MAXIMUM_HEART_RATE - age) - getHRCorrection();
        resetHRRZonesConfig();
        resetMHRZonesConfig();
        //NOTE: LTHR only supported for TYPE_UPRIGHT
        if (this.configType == TYPE_UPRIGHT) {
            resetLTHRZonesConfig();
        }
    }

    private void resetLTHRZonesConfig() {
        this.LTHRThresholdHeartRate = Math.round(((float) this.restHeartRate) + (((float) ((this.maxHRThreshold - this.restHeartRate) * 85)) / 100.0f));
        this.LTHRAnaerobic = Math.round(((float) (this.LTHRThresholdHeartRate * 102)) / 100.0f);
        this.LTHRLactate = Math.round(((float) (this.LTHRThresholdHeartRate * 97)) / 100.0f);
        this.LTHRAdvancedAerobic = Math.round(((float) (this.LTHRThresholdHeartRate * 89)) / 100.0f);
        this.LTHRBasicAerobic = Math.round(((float) (this.LTHRThresholdHeartRate * 80)) / 100.0f);
        this.LTHRWarmUp = Math.round(((float) (this.LTHRThresholdHeartRate * 67)) / 100.0f);
    }

    private void resetMHRZonesConfig() {
        this.MHRExtreme = Math.round(((float) (this.maxHRThreshold * 90)) / 100.0f);
        this.MHRAnaerobic = Math.round(((float) (this.maxHRThreshold * 80)) / 100.0f);
        this.MHRAerobic = Math.round(((float) (this.maxHRThreshold * 70)) / 100.0f);
        this.MHRFatBurning = Math.round(((float) (this.maxHRThreshold * 60)) / 100.0f);
        this.MHRWarmUp = Math.round(((float) (this.maxHRThreshold * 50)) / 100.0f);
    }

    private void resetHRRZonesConfig() {
        int calcHR = this.maxHRThreshold - this.restHeartRate;
        this.HRRAdvancedAnaerobic = Math.round(((float) (calcHR * 95)) / 100.0f) + this.restHeartRate;
        this.HRRBasicAnaerobic = Math.round(((float) (calcHR * 88)) / 100.0f) + this.restHeartRate;
        this.HRRLactate = Math.round(((float) (calcHR * 84)) / 100.0f) + this.restHeartRate;
        this.HRRAdvancedAerobic = Math.round(((float) (calcHR * 74)) / 100.0f) + this.restHeartRate;
        this.HRRBasicAerobic = Math.round(((float) (calcHR * 59)) / 100.0f) + this.restHeartRate;
    }

    //TODO: I am not sure about this. But it looks correct.
    private int getHRCorrection() {
        switch (this.configType) {
            case TYPE_SITTING:
                return 6;
            case TYPE_SWIMMING:
                return 10;
            case TYPE_OTHER:
                return 5;
            default:
                return 0;
        }
    }

    public int getCalculateMethod() {
        return this.calculateMethod;
    }

    public boolean getWarningEnable() {
        return this.warningEnable;
    }

    public int getWarningHRLimit() {
        return this.warningHRLimit;
    }

    public int getMaxHRThreshold() {
        return this.maxHRThreshold;
    }

    public int getRestHeartRate() {
        return this.restHeartRate;
    }

    public int getMHRWarmUp() {
        return this.MHRWarmUp;
    }

    public int getMHRFatBurning() {
        return this.MHRFatBurning;
    }

    public int getMHRAerobic() {
        return this.MHRAerobic;
    }

    public int getMHRAnaerobic() {
        return this.MHRAnaerobic;
    }

    public int getMHRExtreme() {
        return this.MHRExtreme;
    }

    public int getHRRBasicAerobic() {
        return this.HRRBasicAerobic;
    }

    public int getHRRAdvancedAerobic() {
        return this.HRRAdvancedAerobic;
    }

    public int getHRRLactate() {
        return this.HRRLactate;
    }

    public int getHRRBasicAnaerobic() {
        return this.HRRBasicAnaerobic;
    }

    public int getHRRAdvancedAnaerobic() {
        return this.HRRAdvancedAnaerobic;
    }

    public int getLTHRThresholdHeartRate() {
        return this.LTHRThresholdHeartRate;
    }

    public int getLTHRAnaerobic() {
        return this.LTHRAnaerobic;
    }

    public int getLTHRLactate() {
        return this.LTHRLactate;
    }

    public int getLTHRAdvancedAerobic() {
        return this.LTHRAdvancedAerobic;
    }

    public int getLTHRBasicAerobic() {
        return this.LTHRBasicAerobic;
    }

    public int getLTHRWarmUp() {
        return this.LTHRWarmUp;
    }

    private boolean checkValue(int val) {
        return val >= 0 && val < MAXIMUM_HEART_RATE;
    }

    public boolean isValid() {
        return checkValue(this.configType) &&
                checkValue(this.calculateMethod) &&
                checkValue(this.warningHRLimit) &&
                checkValue(this.maxHRThreshold) &&
                checkValue(this.restHeartRate) &&
                checkValue(this.MHRWarmUp) &&
                checkValue(this.MHRFatBurning) &&
                checkValue(this.MHRAerobic) &&
                checkValue(this.MHRAnaerobic) &&
                checkValue(this.MHRExtreme) &&
                checkValue(this.HRRBasicAerobic) &&
                checkValue(this.HRRAdvancedAerobic) &&
                checkValue(this.HRRLactate) &&
                checkValue(this.HRRBasicAnaerobic) &&
                checkValue(this.HRRAdvancedAnaerobic) &&
                checkValue(this.LTHRThresholdHeartRate) &&
                checkValue(this.LTHRAnaerobic) &&
                checkValue(this.LTHRLactate) &&
                checkValue(this.LTHRAdvancedAerobic) &&
                checkValue(this.LTHRBasicAerobic) &&
                checkValue(this.LTHRWarmUp) &&
                this.warningHRLimit > 0;
    }

    public boolean hasValidMHRData() {
        return MHRWarmUp > 0 && MHRFatBurning > 0 && MHRAerobic > 0 && MHRAnaerobic > 0 && MHRExtreme > 0 && maxHRThreshold > 0;
    }

    public boolean hasValidHRRData() {
        return restHeartRate > 0 && HRRBasicAerobic > 0 && HRRAdvancedAerobic > 0 && HRRLactate > 0 && HRRBasicAnaerobic > 0 && HRRAdvancedAnaerobic > 0;
    }

    public boolean hasValidLTHRData() {
        return LTHRThresholdHeartRate > 0 && LTHRAnaerobic > 0 && LTHRLactate > 0 && LTHRAdvancedAerobic > 0 && LTHRBasicAerobic > 0 && LTHRWarmUp > 0;
    }

    private int getZoneForHR(int heartRate, int zone5Threshold, int zone4Threshold, int zone3Threshold, int zone2Threshold, int zone1Threshold) {
        if (heartRate >= MAXIMUM_HEART_RATE) {
            return -1;
        }
        if (heartRate >= zone5Threshold) {
            return 4;
        }
        if (heartRate >= zone4Threshold) {
            return 3;
        }
        if (heartRate >= zone3Threshold) {
            return 2;
        }
        if (heartRate >= zone2Threshold) {
            return 1;
        }
        return heartRate >= zone1Threshold ? 0 : -1;
    }

    public int getMHRZone(int heartRate) {
        return getZoneForHR(heartRate, MHRExtreme, MHRAnaerobic, MHRAerobic, MHRFatBurning, MHRWarmUp);
    }

    public int getHHRZone(int heartRate) {
        return getZoneForHR(heartRate, HRRAdvancedAnaerobic, HRRBasicAnaerobic, HRRLactate, HRRAdvancedAerobic, HRRBasicAerobic);
    }

    public int getLTHRZone(int heartRate) {
        return getZoneForHR(heartRate, LTHRAnaerobic, LTHRLactate, LTHRAdvancedAerobic, LTHRBasicAerobic, LTHRWarmUp);
    }

    public int getZoneByMethod(int heartRate, int method) {
        if(method == CALCULATE_METHOD_LTHR) {
            return getLTHRZone(heartRate);
        } else if(method == CALCULATE_METHOD_MHR) {
            return getMHRZone(heartRate);
        }
        return getHHRZone(heartRate);
    }

    public static boolean isCalculateMethodValidFroType(int type, int method) {
        if(method == CALCULATE_METHOD_LTHR && type == TYPE_UPRIGHT) {
            return true;
        }
        return (method == CALCULATE_METHOD_MHR) || (method == CALCULATE_METHOD_HRR);

    }


}
