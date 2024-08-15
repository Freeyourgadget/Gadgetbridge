/*  Copyright (C) 2022-2024 José Rebelo, Sebastian Reichel

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;

/**
 * The workout types, used to start / when workout tracking starts on the band.
 */
public enum ZeppOsActivityType {
    AerobicCombo(0x33, ActivityKind.AEROBIC_COMBO),
    Aerobics(0x6d, ActivityKind.AEROBICS),
    AirWalker(0x90, ActivityKind.AIR_WALKER),
    Archery(0x5d, ActivityKind.ARCHERY),
    ArtisticSwimming(0x9c, ActivityKind.ARTISTIC_SWIMMING),
    Badminton(0x5c, ActivityKind.BADMINTON),
    Ballet(0x47, ActivityKind.BALLET),
    BallroomDance(0x4b, ActivityKind.BALLROOM_DANCE),
    Baseball(0x4f, ActivityKind.BASEBALL),
    Basketball(0x55, ActivityKind.BASKETBALL),
    BattleRope(0xa7, ActivityKind.BATTLE_ROPE),
    BeachVolleyball(0x7a, ActivityKind.BEACH_VOLLEYBALL),
    BellyDance(0x48, ActivityKind.BELLY_DANCE),
    Billiards(0x97, ActivityKind.BILLIARDS),
    bmx(0x30, ActivityKind.BMX),
    BoardGame(0xb1, ActivityKind.BOARD_GAME),
    Bocce(0xaa, ActivityKind.BOCCE),
    Bowling(0x50, ActivityKind.BOWLING),
    Boxing(0x61, ActivityKind.BOXING),
    Breaking(0xa8, ActivityKind.BREAKING),
    Bridge(0xb0, ActivityKind.BRIDGE),
    CardioCombat(0x72, ActivityKind.CARDIO_COMBAT),
    Checkers(0xae, ActivityKind.CHECKERS),
    Chess(0xad, ActivityKind.CHESS),
    CoreTraining(0x32, ActivityKind.CORE_TRAINING),
    Cricket(0x4e, ActivityKind.CRICKET),
    CrossTraining(0x82, ActivityKind.CROSS_TRAINING),
    Curling(0x29, ActivityKind.CURLING),
    Dance(0x4c, ActivityKind.DANCE),
    Darts(0x75, ActivityKind.DARTS),
    Dodgeball(0x99, ActivityKind.DODGEBALL),
    DragonBoat(0x8a, ActivityKind.DRAGON_BOAT),
    Driving(0x84, ActivityKind.DRIVING),
    Elliptical(0x09, ActivityKind.ELLIPTICAL_TRAINER),
    Esports(0xbd, ActivityKind.ESPORTS),
    Esquestrian(0x5e, ActivityKind.HORSE_RIDING),
    Fencing(0x94, ActivityKind.FENCING),
    Finswimming(0x9b, ActivityKind.FINSWIMMING),
    Fishing(0x40, ActivityKind.FISHING),
    Flexibility(0x37, ActivityKind.FLEXIBILITY),
    Flowriding(0xac, ActivityKind.FLOWRIDING),
    FolkDance(0x92, ActivityKind.FOLK_DANCE),
    Freestyle(0x05, ActivityKind.FREE_TRAINING),
    Frisbee(0x74, ActivityKind.FRISBEE),
    Futsal(0xa4, ActivityKind.FUTSAL),
    Gateball(0x57, ActivityKind.GATEBALL),
    Gymnastics(0x3b, ActivityKind.GYMNASTICS),
    HackySack(0xa9, ActivityKind.HACKY_SACK),
    Handball(0x5b, ActivityKind.HANDBALL),
    HIIT(0x31, ActivityKind.HIIT),
    HipHop(0xa5, ActivityKind.HIP_HOP),
    HorizontalBar(0x95, ActivityKind.HORIZONTAL_BAR),
    HulaHoop(0x73, ActivityKind.HULA_HOOP),
    IceHockey(0x9e, ActivityKind.ICE_HOCKEY),
    IceSkating(0x2c, ActivityKind.ICE_SKATING),
    IndoorCycling(0x08, ActivityKind.INDOOR_CYCLING),
    IndoorFitness(0x18, ActivityKind.INDOOR_FITNESS),
    IndoorIceSkating(0x2d, ActivityKind.INDOOR_ICE_SKATING),
    JaiAlai(0xab, ActivityKind.JAI_ALAI),
    JazzDance(0x71, ActivityKind.JAZZ_DANCE),
    Judo(0x62, ActivityKind.JUDO),
    Jujitsu(0x93, ActivityKind.JUJITSU),
    JumpRope(0x15, ActivityKind.JUMP_ROPING),
    Karate(0x60, ActivityKind.KARATE),
    Kayaking(0x8c, ActivityKind.KAYAKING),
    Kendo(0x5f, ActivityKind.KENDO),
    Kickboxing(0x68, ActivityKind.KICKBOXING),
    KiteFlying(0x76, ActivityKind.KITE_FLYING),
    LatinDance(0x70, ActivityKind.LATIN_DANCE),
    MartialArts(0x67, ActivityKind.MARTIAL_ARTS),
    MassGymnastics(0x6f, ActivityKind.MASS_GYMNASTICS),
    ModernDance(0xb9, ActivityKind.MODERN_DANCE),
    MuayThai(0x65, ActivityKind.MUAY_THAI),
    OutdoorCycling(0x04, ActivityKind.OUTDOOR_CYCLING),
    OutdoorHiking(0x0f, ActivityKind.HIKING),
    OutdoorRunning(0x01, ActivityKind.OUTDOOR_RUNNING),
    OutdoorSwimming(0x07, ActivityKind.SWIMMING_OPENWATER),
    ParallelBars(0x96, ActivityKind.PARALLEL_BARS),
    Parkour(0x81, ActivityKind.PARKOUR),
    Pilates(0x3d, ActivityKind.PILATES),
    PoleDance(0xa6, ActivityKind.POLE_DANCE),
    PoolSwimming(0x06, ActivityKind.POOL_SWIM),
    RaceWalking(0x83, ActivityKind.RACE_WALKING),
    RockClimbing(0x46, ActivityKind.ROCK_CLIMBING),
    RollerSkating(0x45, ActivityKind.ROLLER_SKATING),
    Rowing(0x17, ActivityKind.ROWING),
    Sailing(0x41, ActivityKind.SAILING),
    SepakTakraw(0x98, ActivityKind.SEPAK_TAKRAW),
    Shuffleboard(0xa0, ActivityKind.SHUFFLEBOARD),
    Shuttlecock(0xa2, ActivityKind.SHUTTLECOCK),
    Skateboarding(0x43, ActivityKind.SKATEBOARDING),
    Snorkeling(0x9d, ActivityKind.SNORKELING),
    Soccer(0xbf, ActivityKind.SOCCER),
    Softball(0x56, ActivityKind.SOFTBALL),
    SomatosensoryGame(0xa3, ActivityKind.SOMATOSENSORY_GAME),
    Spinning(0x8f, ActivityKind.SPINNING),
    SquareDance(0x49, ActivityKind.SQUARE_DANCE),
    Squash(0x51, ActivityKind.SQUASH),
    StairClimber(0x36, ActivityKind.STAIR_CLIMBER),
    Stepper(0x39, ActivityKind.STEPPER),
    StreetDance(0x4a, ActivityKind.STREET_DANCE),
    Strength(0x34, ActivityKind.STRENGTH_TRAINING),
    Stretching(0x35, ActivityKind.STRETCHING),
    Swinging(0x9f, ActivityKind.SWING),
    TableFootball(0xa1, ActivityKind.TABLE_FOOTBALL),
    TableTennis(0x59, ActivityKind.TABLE_TENNIS),
    TaiChi(0x64, ActivityKind.TAI_CHI),
    Taekwondo(0x66, ActivityKind.TAEKWONDO),
    Tennis(0x11, ActivityKind.TENNIS),
    Treadmill(0x02, ActivityKind.TREADMILL),
    TugOfWar(0x77, ActivityKind.TUG_OF_WAR),
    Volleyball(0x58, ActivityKind.VOLLEYBALL),
    Walking(0x03, ActivityKind.WALKING),
    WallBall(0x91, ActivityKind.WALL_BALL),
    WaterPolo(0x9a, ActivityKind.WATER_POLO),
    WaterRowing(0x42, ActivityKind.ROWING),
    Weiqi(0xaf, ActivityKind.WEIQI),
    Wrestling(0x63, ActivityKind.WRESTLING),
    Yoga(0x3c, ActivityKind.YOGA),
    Zumba(0x4d, ActivityKind.ZUMBA),
    ;

    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsActivityType.class);

    private final byte code;
    private final ActivityKind activityKind;

    ZeppOsActivityType(final int code, final ActivityKind activityKind) {
        this.code = (byte) code;
        this.activityKind = activityKind;
    }

    public byte getCode() {
        return code;
    }

    public ActivityKind toActivityKind() {
        switch (this) {
            case Badminton:
                return ActivityKind.BADMINTON;
            case Basketball:
                return ActivityKind.BASKETBALL;
            case Cricket:
                return ActivityKind.CRICKET;
            case Elliptical:
                return ActivityKind.ELLIPTICAL_TRAINER;
            case Freestyle:
            case IndoorFitness:
                return ActivityKind.EXERCISE;
            case IndoorCycling:
                return ActivityKind.INDOOR_CYCLING;
            case JumpRope:
                return ActivityKind.JUMP_ROPING;
            case OutdoorCycling:
                return ActivityKind.CYCLING;
            case OutdoorHiking:
                return ActivityKind.HIKING;
            case OutdoorRunning:
                return ActivityKind.RUNNING;
            case OutdoorSwimming:
                return ActivityKind.SWIMMING_OPENWATER;
            case PoolSwimming:
                return ActivityKind.SWIMMING;
            case RockClimbing:
                return ActivityKind.CLIMBING;
            case Rowing:
                return ActivityKind.ROWING_MACHINE;
            case Soccer:
                return ActivityKind.SOCCER;
            case TableTennis:
                return ActivityKind.PINGPONG;
            case Treadmill:
                return ActivityKind.TREADMILL;
            case Walking:
            case RaceWalking:
                return ActivityKind.WALKING;
            case Strength:
                return ActivityKind.STRENGTH_TRAINING;
            case Yoga:
                return ActivityKind.YOGA;
        }

        LOG.warn("Unmapped workout type {}", this);

        return ActivityKind.UNKNOWN;
    }

    public static ZeppOsActivityType fromCode(final byte code) {
        for (final ZeppOsActivityType type : values()) {
            if (type.getCode() == code) {
                return type;
            }
        }

        return null;
    }
}
