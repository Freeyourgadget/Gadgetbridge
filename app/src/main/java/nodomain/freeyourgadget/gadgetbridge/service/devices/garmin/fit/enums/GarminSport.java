package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.enums;

import nodomain.freeyourgadget.gadgetbridge.util.Optional;

// Taken from CHANGELOG.fit of a Venu 3
public enum GarminSport {
    NAVIGATE(0, 50),
    RUN(1, 0),
    TREADMILL(1, 1),
    INDOOR_TRACK(1, 45),
    BIKE(2, 0),
    BIKE_INDOOR(2, 6),
    HANDCYCLING(2, 12),
    E_BIKE(2, 28),
    BIKE_COMMUTE(2, 48),
    HANDCYCLING_INDOOR(2, 88),
    ELLIPTICAL(4, 15),
    STAIR_STEPPER(4, 16),
    PILATES(4, 44),
    POOL_SWIM(5, 17),
    OPEN_WATER(5, 18),
    SOCCER(7, 0),
    TENNIS(8, 0),
    PLATFORM_TENNIS(8, 93),
    TABLE_TENNIS(8, 97),
    STRENGTH(10, 20),
    CARDIO(10, 26),
    YOGA(10, 43),
    BREATHWORK(10, 62),
    WALK(11, 0),
    WALK_INDOOR(11, 27),
    XC_CLASSIC_SKI(12, 0),
    SKI(13, 0),
    SNOWBOARD(14, 0),
    HIKE(17, 0),
    CLIMB_INDOOR(31, 68),
    BOULDERING(31, 69),
    SNOWSHOE(35, 0),
    STAND_UP_PADDLEBOARDING(37, 0),
    SOFTBALL(50, 0),
    HEALTH_SNAPSHOT(60, 0),
    HIIT(62, 0),
    PICKLEBALL(64, 84),
    PADEL(64, 85),
    SQUASH(64, 94),
    RACQUETBALL(64, 96),
    PUSH_WALK_SPEED(65, 0),
    INDOOR_PUSH_WALK_SPEED(65, 86),
    PUSH_RUN_SPEED(66, 0),
    INDOOR_PUSH_RUN_SPEED(66, 87),
    DISC_GOLF(69, 0),
    ULTIMATE_DISC(69, 92),
    RUGBY(72, 0),
    LACROSSE(74, 0),
    VOLLEYBALL(75, 0),
    MIXED_MARTIAL_ARTS(80, 0), // aka MMA
    ;

    private final int type;
    private final int subtype;

    GarminSport(final int type, final int subtype) {
        this.type = type;
        this.subtype = subtype;
    }

    public int getType() {
        return type;
    }

    public int getSubtype() {
        return subtype;
    }

    public static Optional<GarminSport> fromCodes(final int type, final int subtype) {
        for (final GarminSport value : GarminSport.values()) {
            if (value.getType() == type && value.getSubtype() == subtype) {
                return Optional.of(value);
            }
        }

        return Optional.empty();
    }
}
