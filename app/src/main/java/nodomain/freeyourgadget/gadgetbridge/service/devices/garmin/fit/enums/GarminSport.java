package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.enums;

import java.util.Optional;

// Taken from CHANGELOG.fit of a Venu 3,
// Garmin API doc (https://developer.garmin.com/connect-iq/api-docs/Toybox/Activity.html)
// and FIT files
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
    TRANSITION(3, 0),
    FITNESS_EQUIPMENT(4, 0),
    ELLIPTICAL(4, 15),
    STAIR_STEPPER(4, 16),
    PILATES(4, 44),
    SWIMMING(5, 0),
    POOL_SWIM(5, 17),
    OPEN_WATER(5, 18),
    BASKETBALL(6, 0),
    SOCCER(7, 0),
    TENNIS(8, 0),
    PLATFORM_TENNIS(8, 93),
    TABLE_TENNIS(8, 97),
    AMERICAN_FOOTBALL(9, 0),
    TRAINING(10, 0),
    STRENGTH(10, 20),
    CARDIO(10, 26),
    YOGA(10, 43),
    BREATHWORK(10, 62),
    WALK(11, 0),
    WALK_INDOOR(11, 27),
    XC_CLASSIC_SKI(12, 0),
    SKI(13, 0),
    SNOWBOARD(14, 0),
    ROWING(15, 0),
    MOUNTAINEERING(16, 0),
    HIKE(17, 0),
    MULTISPORT(18, 0),
    PADDLING(19, 0),
    FLYING(20, 0),
    E_BIKING(21, 0),
    MOTORCYCLING(22, 0),
    BOATING(23, 0),
    DRIVING(24, 0),
    GOLF(25, 0),
    HANG_GLIDING(26, 0),
    HORSEBACK_RIDING(27,0),
    HUNTING(28, 0),
    FISHING(29, 0),
    INLINE_SKATING(30, 0),
    ROCK_CLIMBING(31, 0),
    CLIMB_INDOOR(31, 68),
    BOULDERING(31, 69),
    SAIL(32, 0),
    SAIL_RACE(32, 65),
    SAIL_EXPEDITION(32, 66),
    ICE_SKATING(33, 0),
    SKY_DIVING(34, 0),
    SNOWSHOE(35, 0),
    SNOWMOBILING(36, 0),
    STAND_UP_PADDLEBOARDING(37, 0),
    SURFING(38, 0),
    WAKEBOARDING(39, 0),
    WATER_SKIING(40, 0),
    KAYAKING(41, 0),
    RAFTING(42, 0),
    WINDSURFING(43, 0),
    KITESURFING(44, 0),
    TACTICAL(45, 0),
    JUMPMASTER(46, 0),
    BOXING(47, 0),
    FLOOR_CLIMBING(48, 0),
    BASEBALL(49, 0),
    SOFTBALL(50, 0),
    SOFTBALL_SLOW_PITCH(51, 0),
    SHOOTING(56, 0),
    AUTO_RACING(57, 0),
    WINTER_SPORT(58, 0),
    GRINDING(59, 0),
    HEALTH_SNAPSHOT(60, 0),
    MARINE(61, 0),
    HIIT(62, 0),
    VIDEO_GAMING(63, 0),
    RACKET(64, 0),
    PICKLEBALL(64, 84),
    PADEL(64, 85),
    SQUASH(64, 94),
    RACQUETBALL(64, 96),
    PUSH_WALK_SPEED(65, 0),
    INDOOR_PUSH_WALK_SPEED(65, 86),
    PUSH_RUN_SPEED(66, 0),
    INDOOR_PUSH_RUN_SPEED(66, 87),
    MEDITATION(67, 0),
    PARA_SPORT(68, 0),
    DISC_GOLF(69, 0),
    ULTIMATE_DISC(69, 92),
    TEAM_SPORT(70, 0),
    CRICKET(71, 0),
    RUGBY(72, 0),
    HOCKEY(73, 0),
    LACROSSE(74, 0),
    VOLLEYBALL(75, 0),
    WATER_TUBING(76, 0),
    WAKESURFING(77, 0),
    MIXED_MARTIAL_ARTS(80, 0), // aka MMA
    DANCE(83, 0),
    JUMP_ROPE(84, 0),
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
