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
import androidx.annotation.StringRes;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;

public enum CmfActivityType {
    // Core (non-removable in official app)
    INDOOR_RUNNING(0x03, R.string.activity_type_indoor_running, ActivityKind.TYPE_RUNNING),
    OUTDOOR_RUNNING(0x02, R.string.activity_type_outdoor_running, ActivityKind.TYPE_RUNNING),
    // Fitness
    OUTDOOR_WALKING(0x01, R.string.activity_type_outdoor_walking, ActivityKind.TYPE_WALKING),
    INDOOR_WALKING(0x19, R.string.activity_type_indoor_walking, ActivityKind.TYPE_WALKING),
    OUTDOOR_CYCLING(0x05, R.string.activity_type_outdoor_cycling, ActivityKind.TYPE_CYCLING),
    INDOOR_CYCLING(0x72, R.string.activity_type_indoor_cycling, ActivityKind.TYPE_INDOOR_CYCLING),
    MOUNTAIN_HIKE(0x04, R.string.activity_type_mountain_hike, ActivityKind.TYPE_HIKING),
    HIKING(0x1A, R.string.activity_type_hiking, ActivityKind.TYPE_HIKING),
    CROSS_TRAINER(0x18, R.string.activity_type_cross_trainer),
    FREE_TRAINING(0x10, R.string.activity_type_free_training, ActivityKind.TYPE_STRENGTH_TRAINING),
    STRENGTH_TRAINING(0x13, R.string.activity_type_strength_training, ActivityKind.TYPE_STRENGTH_TRAINING),
    YOGA(0x0F, R.string.activity_type_yoga, ActivityKind.TYPE_YOGA),
    BOXING(0x21, R.string.activity_type_boxing),
    ROWER(0x0E, R.string.activity_type_rower, ActivityKind.TYPE_ROWING_MACHINE),
    DYNAMIC_CYCLE(0x0D, R.string.activity_type_dynamic_cycle),
    STAIR_STEPPER(0x73, R.string.activity_type_stair_stepper),
    TREADMILL(0x26, R.string.activity_type_treadmill, ActivityKind.TYPE_TREADMILL),
    HIIT(0x5C, R.string.activity_type_hiit),
    FITNESS_EXERCISES(0x4E, R.string.activity_type_fitness_exercises),
    JUMP_ROPING(0x06, R.string.activity_type_jump_roping, ActivityKind.TYPE_JUMP_ROPING),
    PILATES(0x2C, R.string.activity_type_pilates),
    CROSSFIT(0x74, R.string.activity_type_crossfit),
    FUNCTIONAL_TRAINING(0x2E, R.string.activity_type_functional_training),
    PHYSICAL_TRAINING(0x2F, R.string.activity_type_physical_training),
    TAEKWONDO(0x25, R.string.activity_type_taekwondo),
    CROSS_COUNTRY_RUNNING(0x1B, R.string.activity_type_cross_country_running),
    KARATE(0x29, R.string.activity_type_karate),
    FENCING(0x54, R.string.activity_type_fencing),
    CORE_TRAINING(0x4B, R.string.activity_type_core_training),
    KENDO(0x75, R.string.activity_type_kendo),
    HORIZONTAL_BAR(0x56, R.string.activity_type_horizontal_bar),
    PARALLEL_BAR(0x57, R.string.activity_type_parallel_bar),
    COOLDOWN(0x92, R.string.activity_type_cooldown),
    CROSS_TRAINING(0x2B, R.string.activity_type_cross_training),
    SIT_UPS(0x11, R.string.activity_type_sit_ups),
    FITNESS_GAMING(0x4D, R.string.activity_type_fitness_gaming),
    AEROBIC_EXERCISE(0x94, R.string.activity_type_aerobic_exercise),
    ROLLING(0x95, R.string.activity_type_rolling),
    FLEXIBILITY(0x31, R.string.activity_type_flexibility),
    GYMNASTICS(0x23, R.string.activity_type_gymnastics),
    TRACK_AND_FIELD(0x27, R.string.activity_type_track_and_field),
    PUSH_UPS(0x67, R.string.activity_type_push_ups),
    BATTLE_ROPE(0x99, R.string.activity_type_battle_rope),
    SMITH_MACHINE(0x9A, R.string.activity_type_smith_machine),
    PULL_UPS(0x66, R.string.activity_type_pull_ups),
    PLANK(0x68, R.string.activity_type_plank),
    JAVELIN(0x9E, R.string.activity_type_javelin),
    LONG_JUMP(0x6C, R.string.activity_type_long_jump),
    HIGH_JUMP(0x6A, R.string.activity_type_high_jump),
    TRAMPOLINE(0x5F, R.string.activity_type_trampoline),
    DUMBBELL(0x9F, R.string.activity_type_dumbbell),
    // Dance
    BELLY_DANCE(0x76, R.string.activity_type_belly_dance),
    JAZZ_DANCE(0x77, R.string.activity_type_jazz_dance),
    LATIN_DANCE(0x33, R.string.activity_type_latin_dance),
    BALLET(0x36, R.string.activity_type_ballet),
    STREET_DANCE(0x34, R.string.activity_type_street_dance),
    ZUMBA(0x9B, R.string.activity_type_zumba),
    OTHER_DANCE(0x78, R.string.activity_type_other_dance),
    // Leisure sports
    ROLLER_SKATING(0x58, R.string.activity_type_roller_skating),
    MARTIAL_ARTS(0x38, R.string.activity_type_martial_arts),
    TAI_CHI(0x1F, R.string.activity_type_tai_chi),
    HULA_HOOPING(0x59, R.string.activity_type_hula_hooping),
    DISC_SPORTS(0x43, R.string.activity_type_disc_sports),
    DARTS(0x5A, R.string.activity_type_darts),
    ARCHERY(0x30, R.string.activity_type_archery),
    HORSE_RIDING(0x1D, R.string.activity_type_horse_riding),
    KITE_FLYING(0x70, R.string.activity_type_kite_flying),
    SWING(0x71, R.string.activity_type_swing),
    STAIRS(0x15, R.string.activity_type_stairs),
    FISHING(0x42, R.string.activity_type_fishing),
    HAND_CYCLING(0x96, R.string.activity_type_hand_cycling),
    MIND_AND_BODY(0x97, R.string.activity_type_mind_and_body),
    WRESTLING(0x53, R.string.activity_type_wrestling),
    KABADDI(0x9C, R.string.activity_type_kabaddi),
    KARTING(0xA0, R.string.activity_type_karting),
    // Ball sports
    BADMINTON(0x09, R.string.activity_type_badminton),
    TABLE_TENNIS(0x0A, R.string.activity_type_table_tennis, ActivityKind.TYPE_PINGPONG),
    TENNIS(0x0C, R.string.activity_type_tennis),
    BILLIARDS(0x7C, R.string.activity_type_billiards),
    BOWLING(0x3B, R.string.activity_type_bowling),
    VOLLEYBALL(0x49, R.string.activity_type_volleyball),
    SHUTTLECOCK(0x20, R.string.activity_type_shuttlecock),
    HANDBALL(0x39, R.string.activity_type_handball),
    BASEBALL(0x3A, R.string.activity_type_baseball),
    SOFTBALL(0x55, R.string.activity_type_softball),
    CRICKET(0x0B, R.string.activity_type_cricket),
    RUGBY(0x44, R.string.activity_type_rugby),
    HOCKEY(0x1E, R.string.activity_type_hockey),
    SQUASH(0x3C, R.string.activity_type_squash),
    DODGEBALL(0x81, R.string.activity_type_dodgeball),
    SOCCER(0x07, R.string.activity_type_soccer, ActivityKind.TYPE_SOCCER),
    BASKETBALL(0x08, R.string.activity_type_basketball, ActivityKind.TYPE_BASKETBALL),
    AUSTRALIAN_FOOTBALL(0x37, R.string.activity_type_australian_football),
    GOLF(0x45, R.string.activity_type_golf),
    PICKLEBALL(0x5B, R.string.activity_type_pickleball),
    LACROSS(0x98, R.string.activity_type_lacross),
    SHOT(0x9D, R.string.activity_type_shot),
    // Water sports
    SAILING(0x82, R.string.activity_type_sailing),
    SURFING(0x64, R.string.activity_type_surfing),
    JET_SKIING(0x87, R.string.activity_type_jet_skiing),
    // Snow sports
    SKATING(0x4C, R.string.activity_type_skating),
    ICE_HOCKEY(0x24, R.string.activity_type_ice_hockey),
    CURLING(0x3D, R.string.activity_type_curling),
    SNOWBOARDING(0x3E, R.string.activity_type_snowboarding),
    CROSS_COUNTRY_SKIING(0x6E, R.string.activity_type_cross_country_skiing),
    SNOW_SPORTS(0x48, R.string.activity_type_snow_sports),
    SKIING(0x22, R.string.activity_type_skiing),
    // Extreme sports
    SKATEBOARDING(0x60, R.string.activity_type_skateboarding),
    ROCK_CLIMBING(0x69, R.string.activity_type_rock_climbing),
    HUNTING(0x93, R.string.activity_type_hunting),
    ;

    private final byte code;
    @StringRes
    private final int nameRes;
    private final int activityKind;

    CmfActivityType(final int code, final int nameRes) {
        this(code, nameRes, ActivityKind.TYPE_UNKNOWN);
    }

    CmfActivityType(final int code, final int nameRes, final int activityKind) {
        this.code = (byte) code;
        this.nameRes = nameRes;
        this.activityKind = activityKind;
    }

    public byte getCode() {
        return code;
    }

    public int getActivityKind() {
        return activityKind;
    }

    @StringRes
    public int getNameRes() {
        return nameRes;
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
