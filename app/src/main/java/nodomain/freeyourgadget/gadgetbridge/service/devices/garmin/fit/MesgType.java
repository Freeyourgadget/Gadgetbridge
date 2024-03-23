package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit;

public enum MesgType {
    TODAY_WEATHER_CONDITIONS(6, 128),
    HOURLY_WEATHER_FORECAST(9, 128),
    DAILY_WEATHER_FORECAST(10, 128);

    private final int identifier;
    private final int globalMesgNum;

    MesgType(int id, int globalMesgNum) {
        this.identifier = id;
        this.globalMesgNum = globalMesgNum;
    }

    public static MesgType fromIdentifier(int identifier) {
        for (final MesgType mesgType : MesgType.values()) {
            if (mesgType.getIdentifier() == identifier) {
                return mesgType;
            }
        }
        throw new IllegalArgumentException("Unknown type " + identifier); //TODO: perhaps we need to handle unknown message types
    }

    public int getIdentifier() {
        return identifier;
    }

    public int getGlobalMesgNum() {
        return globalMesgNum;
    }
}
