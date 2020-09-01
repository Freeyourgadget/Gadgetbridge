package nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.entities.time;

public enum BandDaylightSavingTime {
    STANDARD_TIME(0, 0),
    HALF_AN_HOUR_DST(2, 30),
    DST(4, 60),
    DOUBLE_DST( 8, 120);

    final int key;
    private final long saving;

    BandDaylightSavingTime(int key, int min) {
        this.key = key;
        this.saving = 60000L * min;
    }

    public static BandDaylightSavingTime fromOffset(final int dstSaving) {
        for (BandDaylightSavingTime dst: values()){
            if (dst.saving == dstSaving)
                return dst;
        }
        throw new RuntimeException("wrong dst saving: " + dstSaving);
    }
}
