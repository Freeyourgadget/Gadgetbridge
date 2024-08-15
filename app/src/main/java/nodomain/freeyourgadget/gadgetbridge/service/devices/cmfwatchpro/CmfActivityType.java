/*  Copyright (C) 2024 José Rebelo

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.cmfwatchpro;

import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;

public enum CmfActivityType {
    // Core (non-removable in official app)
    INDOOR_RUNNING(0x03, ActivityKind.INDOOR_RUNNING),
    OUTDOOR_RUNNING(0x02, ActivityKind.OUTDOOR_RUNNING),
    // Fitness
    OUTDOOR_WALKING(0x01, ActivityKind.OUTDOOR_WALKING),
    INDOOR_WALKING(0x19, ActivityKind.INDOOR_WALKING),
    OUTDOOR_CYCLING(0x05, ActivityKind.OUTDOOR_CYCLING),
    INDOOR_CYCLING(0x72, ActivityKind.INDOOR_CYCLING),
    MOUNTAIN_HIKE(0x04, ActivityKind.MOUNTAIN_HIKE),
    HIKING(0x1A, ActivityKind.HIKING),
    CROSS_TRAINER(0x18, ActivityKind.CROSS_TRAINER),
    FREE_TRAINING(0x10, ActivityKind.FREE_TRAINING),
    STRENGTH_TRAINING(0x13, ActivityKind.STRENGTH_TRAINING),
    YOGA(0x0F, ActivityKind.YOGA),
    BOXING(0x21, ActivityKind.BOXING),
    ROWER(0x0E, ActivityKind.ROWING_MACHINE),
    DYNAMIC_CYCLE(0x0D, ActivityKind.DYNAMIC_CYCLE),
    STAIR_STEPPER(0x73, ActivityKind.STAIR_STEPPER),
    TREADMILL(0x26, ActivityKind.TREADMILL),
    KICKBOXING(0x35, ActivityKind.KICKBOXING),
    HIIT(0x5C, ActivityKind.HIIT),
    FITNESS_EXERCISES(0x4E, ActivityKind.FITNESS_EXERCISES),
    JUMP_ROPING(0x06, ActivityKind.JUMP_ROPING), // moved to leisure sports in watch 2
    PILATES(0x2C, ActivityKind.PILATES),
    CROSSFIT(0x74, ActivityKind.CROSSFIT),
    FUNCTIONAL_TRAINING(0x2E, ActivityKind.FUNCTIONAL_TRAINING),
    PHYSICAL_TRAINING(0x2F, ActivityKind.PHYSICAL_TRAINING),
    TAEKWONDO(0x25, ActivityKind.TAEKWONDO),
    TAE_BO(0x50, ActivityKind.TAE_BO),
    CROSS_COUNTRY_RUNNING(0x1B, ActivityKind.CROSS_COUNTRY_RUNNING),
    KARATE(0x29, ActivityKind.KARATE),
    FENCING(0x54, ActivityKind.FENCING),
    CORE_TRAINING(0x4B, ActivityKind.CORE_TRAINING),
    KENDO(0x75, ActivityKind.KENDO),
    HORIZONTAL_BAR(0x56, ActivityKind.HORIZONTAL_BAR),
    PARALLEL_BAR(0x57, ActivityKind.PARALLEL_BAR),
    COOLDOWN(0x92, ActivityKind.COOLDOWN),
    CROSS_TRAINING(0x2B, ActivityKind.CROSS_TRAINING),
    SIT_UPS(0x11, ActivityKind.SIT_UPS),
    FITNESS_GAMING(0x4D, ActivityKind.FITNESS_GAMING),
    AEROBIC_EXERCISE(0x94, ActivityKind.AEROBIC_EXERCISE),
    ROLLING(0x95, ActivityKind.ROLLING),
    FLEXIBILITY(0x31, ActivityKind.FLEXIBILITY),
    GYMNASTICS(0x23, ActivityKind.GYMNASTICS),
    TRACK_AND_FIELD(0x27, ActivityKind.TRACK_AND_FIELD),
    PUSH_UPS(0x67, ActivityKind.PUSH_UPS),
    BATTLE_ROPE(0x99, ActivityKind.BATTLE_ROPE),
    SMITH_MACHINE(0x9A, ActivityKind.SMITH_MACHINE),
    PULL_UPS(0x66, ActivityKind.PULL_UPS),
    PLANK(0x68, ActivityKind.PLANK),
    JAVELIN(0x9E, ActivityKind.JAVELIN),
    LONG_JUMP(0x6C, ActivityKind.LONG_JUMP),
    HIGH_JUMP(0x6A, ActivityKind.HIGH_JUMP),
    TRAMPOLINE(0x5F, ActivityKind.TRAMPOLINE),
    DUMBBELL(0x9F, ActivityKind.DUMBBELL),
    // Dance
    BELLY_DANCE(0x76, ActivityKind.BELLY_DANCE),
    JAZZ_DANCE(0x77, ActivityKind.JAZZ_DANCE),
    LATIN_DANCE(0x33, ActivityKind.LATIN_DANCE),
    BALLET(0x36, ActivityKind.BALLET),
    STREET_DANCE(0x34, ActivityKind.STREET_DANCE),
    ZUMBA(0x9B, ActivityKind.ZUMBA),
    OTHER_DANCE(0x78, ActivityKind.DANCE),
    // Leisure sports
    ROLLER_SKATING(0x58, ActivityKind.ROLLER_SKATING),
    MARTIAL_ARTS(0x38, ActivityKind.MARTIAL_ARTS),
    TAI_CHI(0x1F, ActivityKind.TAI_CHI),
    HULA_HOOPING(0x59, ActivityKind.HULA_HOOPING),
    DISC_SPORTS(0x43, ActivityKind.DISC_SPORTS),
    DARTS(0x5A, ActivityKind.DARTS),
    ARCHERY(0x30, ActivityKind.ARCHERY),
    HORSE_RIDING(0x1D, ActivityKind.HORSE_RIDING),
    KITE_FLYING(0x70, ActivityKind.KITE_FLYING),
    SWING(0x71, ActivityKind.SWING),
    STAIRS(0x15, ActivityKind.STAIRS),
    FISHING(0x42, ActivityKind.FISHING),
    HAND_CYCLING(0x96, ActivityKind.HANDCYCLING),
    MIND_AND_BODY(0x97, ActivityKind.MIND_AND_BODY),
    WRESTLING(0x53, ActivityKind.WRESTLING),
    KABADDI(0x9C, ActivityKind.KABADDI),
    KARTING(0xA0, ActivityKind.KARTING),
    // Ball sports
    BADMINTON(0x09, ActivityKind.BADMINTON),
    TABLE_TENNIS(0x0A, ActivityKind.TABLE_TENNIS),
    TENNIS(0x0C, ActivityKind.TENNIS),
    BILLIARDS(0x7C, ActivityKind.BILLIARDS),
    BOWLING(0x3B, ActivityKind.BOWLING),
    VOLLEYBALL(0x49, ActivityKind.VOLLEYBALL),
    SHUTTLECOCK(0x20, ActivityKind.SHUTTLECOCK),
    HANDBALL(0x39, ActivityKind.HANDBALL),
    BASEBALL(0x3A, ActivityKind.BASEBALL),
    SOFTBALL(0x55, ActivityKind.SOFTBALL),
    CRICKET(0x0B, ActivityKind.CRICKET),
    RUGBY(0x44, ActivityKind.RUGBY),
    HOCKEY(0x1E, ActivityKind.HOCKEY),
    SQUASH(0x3C, ActivityKind.SQUASH),
    DODGEBALL(0x81, ActivityKind.DODGEBALL),
    SOCCER(0x07, ActivityKind.SOCCER),
    BASKETBALL(0x08, ActivityKind.BASKETBALL),
    AUSTRALIAN_FOOTBALL(0x37, ActivityKind.AUSTRALIAN_FOOTBALL),
    GOLF(0x45, ActivityKind.GOLF),
    PICKLEBALL(0x5B, ActivityKind.PICKLEBALL),
    LACROSS(0x98, ActivityKind.LACROSS),
    SHOT(0x9D, ActivityKind.SHOT),
    BEACH_SOCCER(0x7d, ActivityKind.BEACH_SOCCER),
    BEACH_VOLLEYBALL(0x7e, ActivityKind.BEACH_VOLLEYBALL),
    GATEBALL(0x7f, ActivityKind.GATEBALL),
    SEPAK_TAKRAW(0x80, ActivityKind.SEPAK_TAKRAW),
    // Water sports
    SAILING(0x82, ActivityKind.SAILING),
    SURFING(0x64, ActivityKind.SURFING),
    JET_SKIING(0x87, ActivityKind.JET_SKIING),
    // Snow sports
    SKATING(0x4C, ActivityKind.SKATING),
    ICE_HOCKEY(0x24, ActivityKind.ICE_HOCKEY),
    CURLING(0x3D, ActivityKind.CURLING),
    SNOWBOARDING(0x3E, ActivityKind.SNOWBOARDING),
    CROSS_COUNTRY_SKIING(0x6E, ActivityKind.CROSS_COUNTRY_SKIING),
    SNOW_SPORTS(0x48, ActivityKind.SNOW_SPORTS),
    SKIING(0x22, ActivityKind.SKIING),
    LUGE(0x8a, ActivityKind.LUGE),
    // Extreme sports
    SKATEBOARDING(0x60, ActivityKind.SKATEBOARDING),
    ROCK_CLIMBING(0x69, ActivityKind.ROCK_CLIMBING),
    HUNTING(0x93, ActivityKind.HUNTING),
    // Other
    PARACHUTING(0x8e, ActivityKind.PARACHUTING),
    AUTO_RACING(0x8f, ActivityKind.AUTO_RACING),
    PARKOUR(0x62, ActivityKind.PARKOUR),
    ;

    private final byte code;
    private final ActivityKind activityKind;

    CmfActivityType(final int code, final ActivityKind activityKind) {
        this.code = (byte) code;
        this.activityKind = activityKind;
    }

    public byte getCode() {
        return code;
    }

    public ActivityKind getActivityKind() {
        return activityKind;
    }

    @Nullable
    public static CmfActivityType fromCode(final byte code) {
        for (final CmfActivityType cmd : CmfActivityType.values()) {
            if (cmd.getCode() == code) {
                return cmd;
            }
        }

        return null;
    }
}
