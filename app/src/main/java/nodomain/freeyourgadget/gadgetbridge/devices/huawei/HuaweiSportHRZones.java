package nodomain.freeyourgadget.gadgetbridge.devices.huawei;

public class HuaweiSportHRZones {
    private final HeartRateZonesConfig otherHRZonesConfig;
    private final HeartRateZonesConfig sittingHRZonesConfig;
    private final HeartRateZonesConfig uprightHRZonesConfig;
    private final HeartRateZonesConfig swimmingHRZonesConfig;

    public HuaweiSportHRZones(int age) {
        this.uprightHRZonesConfig = new HeartRateZonesConfig(HeartRateZonesConfig.TYPE_UPRIGHT, age);
        this.sittingHRZonesConfig = new HeartRateZonesConfig(HeartRateZonesConfig.TYPE_SITTING, age);
        this.swimmingHRZonesConfig = new HeartRateZonesConfig(HeartRateZonesConfig.TYPE_SWIMMING, age);
        this.otherHRZonesConfig = new HeartRateZonesConfig(HeartRateZonesConfig.TYPE_OTHER, age);
    }

    public HeartRateZonesConfig getHRZonesConfigByType(int type) {
        if (type == HeartRateZonesConfig.TYPE_SITTING) {
            return this.sittingHRZonesConfig;
        } else if (type == HeartRateZonesConfig.TYPE_SWIMMING) {
            return this.swimmingHRZonesConfig;
        } else if (type == HeartRateZonesConfig.TYPE_OTHER) {
            return this.otherHRZonesConfig;
        }
        return this.uprightHRZonesConfig;
    }

    public byte[] getHRZonesData() {
        HeartRateZonesConfig uprightConfig = getHRZonesConfigByType(HeartRateZonesConfig.TYPE_UPRIGHT);
        HeartRateZonesConfig sittingConfig = getHRZonesConfigByType(HeartRateZonesConfig.TYPE_SITTING);
        HeartRateZonesConfig swimmingConfig = getHRZonesConfigByType(HeartRateZonesConfig.TYPE_SWIMMING);
        HeartRateZonesConfig otherConfig = getHRZonesConfigByType(HeartRateZonesConfig.TYPE_OTHER);

        if (!uprightConfig.isValid() || !sittingConfig.isValid() || !swimmingConfig.isValid() || !otherConfig.isValid()) {
            return null;
        }
        HuaweiTLV tlv = new HuaweiTLV();
        if (uprightConfig.hasValidMHRData()) {
            tlv.put(0x2, (byte) uprightConfig.getMHRWarmUp())
                    .put(0x3, (byte) uprightConfig.getMHRFatBurning())
                    .put(0x4, (byte) uprightConfig.getMHRAerobic())
                    .put(0x5, (byte) uprightConfig.getMHRAnaerobic())
                    .put(0x6, (byte) uprightConfig.getMHRExtreme())
                    .put(0x7, (byte) uprightConfig.getMaxHRThreshold())
                    .put(0x8, (byte) (uprightConfig.getWarningEnable() ? 1 : 0))
                    .put(0x9, (byte) uprightConfig.getWarningHRLimit())
                    .put(0xa, (byte) uprightConfig.getCalculateMethod())
                    .put(0xb, (byte) uprightConfig.getMaxHRThreshold());
        }
        tlv.put(0xc, (byte) uprightConfig.getRestHeartRate());
        if (uprightConfig.hasValidHRRData()) {
            tlv.put(0xd, (byte) uprightConfig.getHRRBasicAerobic())
                    .put(0xe, (byte) uprightConfig.getHRRAdvancedAerobic())
                    .put(0xf, (byte) uprightConfig.getHRRLactate())
                    .put(0x10, (byte) uprightConfig.getHRRBasicAnaerobic())
                    .put(0x11, (byte) uprightConfig.getHRRAdvancedAnaerobic());

        }
        if (uprightConfig.hasValidLTHRData()) {
            tlv.put(0x3f, (byte) uprightConfig.getLTHRThresholdHeartRate())
                    .put(0x40, (byte) uprightConfig.getLTHRAnaerobic())
                    .put(0x41, (byte) uprightConfig.getLTHRLactate())
                    .put(0x42, (byte) uprightConfig.getLTHRAdvancedAerobic())
                    .put(0x43, (byte) uprightConfig.getLTHRBasicAerobic())
                    .put(0x44, (byte) uprightConfig.getLTHRWarmUp());
        }

        if (sittingConfig.hasValidMHRData()) {
            tlv.put(0x12, (byte) (sittingConfig.getWarningEnable() ? 1 : 0))
                    .put(0x13, (byte) sittingConfig.getCalculateMethod())
                    .put(0x14, (byte) sittingConfig.getWarningHRLimit())
                    .put(0x15, (byte) sittingConfig.getMHRWarmUp())
                    .put(0x16, (byte) sittingConfig.getMHRFatBurning())
                    .put(0x17, (byte) sittingConfig.getMHRAerobic())
                    .put(0x18, (byte) sittingConfig.getMHRAnaerobic())
                    .put(0x19, (byte) sittingConfig.getMHRExtreme())
                    .put(0x1a, (byte) sittingConfig.getMaxHRThreshold());
        }
        if (sittingConfig.hasValidHRRData()) {
            tlv.put(0x1b, (byte) sittingConfig.getRestHeartRate())
                    .put(0x1c, (byte) sittingConfig.getHRRBasicAerobic())
                    .put(0x1d, (byte) sittingConfig.getHRRAdvancedAerobic())
                    .put(0x1e, (byte) sittingConfig.getHRRLactate())
                    .put(0x1f, (byte) sittingConfig.getHRRBasicAnaerobic())
                    .put(0x20, (byte) sittingConfig.getHRRAdvancedAnaerobic());
        }

        if (swimmingConfig.hasValidMHRData()) {
            tlv.put(0x21, (byte) (swimmingConfig.getWarningEnable() ? 1 : 0))
                    .put(0x22, (byte) swimmingConfig.getCalculateMethod())
                    .put(0x23, (byte) swimmingConfig.getWarningHRLimit())
                    .put(0x24, (byte) swimmingConfig.getMHRWarmUp())
                    .put(0x25, (byte) swimmingConfig.getMHRFatBurning())
                    .put(0x26, (byte) swimmingConfig.getMHRAerobic())
                    .put(0x27, (byte) swimmingConfig.getMHRAnaerobic())
                    .put(0x28, (byte) swimmingConfig.getMHRExtreme())
                    .put(0x29, (byte) swimmingConfig.getMaxHRThreshold());
        }
        if (swimmingConfig.hasValidHRRData()) {
            tlv.put(0x2a, (byte) swimmingConfig.getRestHeartRate())
                    .put(0x2b, (byte) swimmingConfig.getHRRBasicAerobic())
                    .put(0x2c, (byte) swimmingConfig.getHRRAdvancedAerobic())
                    .put(0x2d, (byte) swimmingConfig.getHRRLactate())
                    .put(0x2e, (byte) swimmingConfig.getHRRBasicAnaerobic())
                    .put(0x2f, (byte) swimmingConfig.getHRRAdvancedAnaerobic());
        }

        if (otherConfig.hasValidMHRData()) {
            tlv.put(0x30, (byte) (otherConfig.getWarningEnable() ? 1 : 0))
                    .put(0x31, (byte) otherConfig.getCalculateMethod())
                    .put(0x32, (byte) otherConfig.getWarningHRLimit())
                    .put(0x33, (byte) otherConfig.getMHRWarmUp())
                    .put(0x34, (byte) otherConfig.getMHRFatBurning())
                    .put(0x35, (byte) otherConfig.getMHRAerobic())
                    .put(0x36, (byte) otherConfig.getMHRAnaerobic())
                    .put(0x37, (byte) otherConfig.getMHRExtreme())
                    .put(0x38, (byte) otherConfig.getMaxHRThreshold());
        }
        if (otherConfig.hasValidHRRData()) {
            tlv.put(0x39, (byte) otherConfig.getRestHeartRate())
                    .put(0x3a, (byte) otherConfig.getHRRBasicAerobic())
                    .put(0x3b, (byte) otherConfig.getHRRAdvancedAerobic())
                    .put(0x3c, (byte) otherConfig.getHRRLactate())
                    .put(0x3d, (byte) otherConfig.getHRRBasicAnaerobic())
                    .put(0x3e, (byte) otherConfig.getHRRAdvancedAnaerobic());
        }

        return tlv.serialize();
    }


}
