/*  Copyright (C) 2022 José Rebelo

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
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;

/**
 * The workout types, used to start / when workout tracking starts on the band.
 */
public enum Huami2021WorkoutTrackActivityType {
    AerobicCombo(0x33),
    Aerobics(0x6d),
    AirWalker(0x90),
    Archery(0x5d),
    ArtisticSwimming(0x9c),
    Badminton(0x5c),
    Ballet(0x47),
    BallroomDance(0x4b),
    Baseball(0x4f),
    Basketball(0x55),
    BattleRope(0xa7),
    BeachVolleyball(0x7a),
    BellyDance(0x48),
    Billiards(0x97),
    bmx(0x30),
    BoardGame(0xb1),
    Bocce(0xaa),
    Bowling(0x50),
    Boxing(0x61),
    Breaking(0xa8),
    Bridge(0xb0),
    CardioCombat(0x72),
    Checkers(0xae),
    Chess(0xad),
    CoreTraining(0x32),
    Cricket(0x4e),
    CrossTraining(0x82),
    Curling(0x29),
    Dance(0x4c),
    Darts(0x75),
    Dodgeball(0x99),
    DragonBoat(0x8a),
    Elliptical(0x09),
    Esports(0xbd),
    Esquestrian(0x5e),
    Fencing(0x94),
    Finswimming(0x9b),
    Fishing(0x40),
    Flexibility(0x37),
    Flowriding(0xac),
    FolkDance(0x92),
    Freestyle(0x05),
    Frisbee(0x74),
    Futsal(0xa4),
    Gateball(0x57),
    Gymnastics(0x3b),
    HackySack(0xa9),
    Handball(0x5b),
    HIIT(0x31),
    HipHop(0xa5),
    HorizontalBar(0x95),
    HulaHoop(0x73),
    IceHockey(0x9e),
    IceSkating(0x2c),
    IndoorCycling(0x08),
    IndoorFitness(0x18),
    IndoorIceSkating(0x2d),
    JaiAlai(0xab),
    JazzDance(0x71),
    Judo(0x62),
    Jujitsu(0x93),
    JumpRope(0x15),
    Karate(0x60),
    Kayaking(0x8c),
    Kendo(0x5f),
    Kickboxing(0x68),
    KiteFlying(0x76),
    LatinDance(0x70),
    MartialArts(0x67),
    MassGymnastics(0x6f),
    ModernDance(0xb9),
    MuayThai(0x65),
    OutdoorCycling(0x04),
    OutdoorRunning(0x01),
    ParallelBars(0x96),
    Parkour(0x81),
    Pilates(0x3d),
    PoleDance(0xa6),
    PoolSwimming(0x06),
    RaceWalking(0x83),
    RockClimbing(0x46),
    RollerSkating(0x45),
    Rowing(0x17),
    Sailing(0x41),
    SepakTakraw(0x98),
    Shuffleboard(0xa0),
    Shuttlecock(0xa2),
    Skateboarding(0x43),
    Snorkeling(0x9d),
    Soccer(0xbf),
    Softball(0x56),
    SomatosensoryGame(0xa3),
    Spinning(0x8f),
    SquareDance(0x49),
    Squash(0x51),
    StairClimber(0x36),
    Stepper(0x39),
    StreetDance(0x4a),
    Strength(0x34),
    Stretching(0x35),
    Swinging(0x9f),
    TableFootball(0xa1),
    TableTennis(0x59),
    TaiChi(0x64),
    Taekwondo(0x66),
    Tennis(0x11),
    Treadmill(0x02),
    TugOfWar(0x77),
    Volleyball(0x58),
    Walking(0x03),
    WallBall(0x91),
    WaterPolo(0x9a),
    WaterRowing(0x42),
    Weiqi(0xaf),
    Wrestling(0x63),
    Yoga(0x3c),
    Zumba(0x4d),
    ;

    private static final Logger LOG = LoggerFactory.getLogger(Huami2021WorkoutTrackActivityType.class);

    private final byte code;

    Huami2021WorkoutTrackActivityType(final int code) {
        this.code = (byte) code;
    }

    public byte getCode() {
        return code;
    }

    public int toActivityKind() {
        switch (this) {
            case Badminton:
                return ActivityKind.TYPE_BADMINTON;
            case Basketball:
                return ActivityKind.TYPE_BASKETBALL;
            case Cricket:
                return ActivityKind.TYPE_CRICKET;
            case Elliptical:
                return ActivityKind.TYPE_ELLIPTICAL_TRAINER;
            case Freestyle:
            case IndoorFitness:
                return ActivityKind.TYPE_EXERCISE;
            case IndoorCycling:
                return ActivityKind.TYPE_INDOOR_CYCLING;
            case JumpRope:
                return ActivityKind.TYPE_JUMP_ROPING;
            case OutdoorCycling:
                return ActivityKind.TYPE_CYCLING;
            case OutdoorRunning:
                return ActivityKind.TYPE_RUNNING;
            case PoolSwimming:
                return ActivityKind.TYPE_SWIMMING;
            case Rowing:
                return ActivityKind.TYPE_ROWING_MACHINE;
            case Soccer:
                return ActivityKind.TYPE_SOCCER;
            case Treadmill:
                return ActivityKind.TYPE_TREADMILL;
            case Walking:
            case RaceWalking:
                return ActivityKind.TYPE_WALKING;
            case Strength:
                return ActivityKind.TYPE_STRENGTH_TRAINING;
            case Yoga:
                return ActivityKind.TYPE_YOGA;
        }

        LOG.warn("Unmapped workout type {}", this);

        return ActivityKind.TYPE_UNKNOWN;
    }

    public static Huami2021WorkoutTrackActivityType fromCode(final byte code) {
        for (final Huami2021WorkoutTrackActivityType type : values()) {
            if (type.getCode() == code) {
                return type;
            }
        }

        return null;
    }
}
