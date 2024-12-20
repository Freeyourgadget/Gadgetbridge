/*  Copyright (C) 2015-2024 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, José Rebelo, Petr Vaněk, Sebastian Krey, Your Name

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
package nodomain.freeyourgadget.gadgetbridge.model;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import nodomain.freeyourgadget.gadgetbridge.R;

public enum ActivityKind {
    NOT_MEASURED(-1, R.string.activity_type_not_measured, R.drawable.ic_activity_not_measured),
    UNKNOWN(0x00000000, R.string.activity_type_unknown),
    ACTIVITY(0x00000001, R.string.activity_type_activity),
    LIGHT_SLEEP(0x00000002, R.string.activity_type_light_sleep, R.drawable.ic_activity_sleep),
    DEEP_SLEEP(0x00000004, R.string.activity_type_deep_sleep, R.drawable.ic_activity_sleep),
    NOT_WORN(0x00000008, R.string.activity_type_not_worn),
    RUNNING(0x00000010, R.string.activity_type_running, R.drawable.ic_activity_running),
    WALKING(0x00000020, R.string.activity_type_walking, R.drawable.ic_activity_walking),
    SWIMMING(0x00000040, R.string.activity_type_swimming, R.drawable.ic_activity_swimming),
    CYCLING(0x00000080, R.string.activity_type_biking, R.drawable.ic_activity_biking),
    TREADMILL(0x00000100, R.string.activity_type_treadmill, R.drawable.ic_activity_threadmill),
    EXERCISE(0x00000200, R.string.activity_type_exercise, R.drawable.ic_activity_exercise),
    SWIMMING_OPENWATER(0x00000400, R.string.activity_type_swimming_openwater, R.drawable.ic_activity_swimming),
    INDOOR_CYCLING(0x00000800, R.string.activity_type_indoor_cycling, R.drawable.ic_activity_bike_trainer),
    ELLIPTICAL_TRAINER(0x00001000, R.string.activity_type_elliptical_trainer, R.drawable.ic_activity_eliptical),
    JUMP_ROPING(0x00002000, R.string.activity_type_jump_roping, R.drawable.ic_activity_rope_jump),
    YOGA(0x00004000, R.string.activity_type_yoga, R.drawable.ic_activity_yoga),
    SOCCER(0x00008000, R.string.activity_type_soccer, R.drawable.ic_activity_soccer),
    ROWING_MACHINE(0x00010000, R.string.activity_type_rowing_machine, R.drawable.ic_activity_rowing),
    CRICKET(0x00020000, R.string.activity_type_cricket, R.drawable.ic_activity_cricket),
    BASKETBALL(0x00040000, R.string.activity_type_basketball, R.drawable.ic_activity_basketball),
    PINGPONG(0x00080000, R.string.activity_type_pingpong, R.drawable.ic_activity_pingpong),
    BADMINTON(0x00100000, R.string.activity_type_badminton, R.drawable.ic_activity_badmington),
    STRENGTH_TRAINING(0x00200000, R.string.activity_type_strength_training, R.drawable.ic_activity_dumbbell),
    HIKING(0x00400000, R.string.activity_type_hiking, R.drawable.ic_activity_hiking),
    CLIMBING(0x00800000, R.string.activity_type_climbing, R.drawable.ic_activity_climbing),
    REM_SLEEP(0x01000000, R.string.abstract_chart_fragment_kind_rem_sleep, R.drawable.ic_activity_sleep),
    SLEEP_ANY(0x00000002 | 0x00000004 | 0x01000000 | 0x02000000, R.string.menuitem_sleep, R.drawable.ic_activity_sleep),
    AWAKE_SLEEP(0x02000000, R.string.abstract_chart_fragment_kind_awake_sleep, R.drawable.ic_activity_sleep),

    // FIXME: Deprecate these - they're just kept around while we do not support reading from the old db
    VIVOMOVE_HR_TRANSITION(0x00000001 | 0x00000010 | 0x00000020 | 0x00000200 | 0x00000040, R.string.transition),

    // Non-legacy activity kinds after 0x04000000
    NAVIGATE(0x04000000, R.string.activity_type_navigate, R.drawable.ic_navigation),
    INDOOR_TRACK(0x04000001, R.string.activity_type_indoor_track),
    HANDCYCLING(0x04000002, R.string.activity_type_handcycling),
    E_BIKE(0x04000003, R.string.activity_type_e_bike, R.drawable.ic_activity_biking),
    BIKE_COMMUTE(0x04000004, R.string.activity_type_bike_commute, R.drawable.ic_activity_biking),
    HANDCYCLING_INDOOR(0x04000005, R.string.activity_type_handcycling_indoor),
    TRANSITION(0x04000006, R.string.activity_type_transition),
    FITNESS_EQUIPMENT(0x04000007, R.string.activity_type_fitness_equipment),
    STAIR_STEPPER(0x04000008, R.string.activity_type_stair_stepper, R.drawable.ic_activity_stair_stepper),
    PILATES(0x04000009, R.string.activity_type_pilates, R.drawable.ic_activity_pilates),
    POOL_SWIM(0x0400000a, R.string.activity_type_pool_swimming, R.drawable.ic_activity_swimming),
    TENNIS(0x0400000b, R.string.activity_type_tennis),
    PLATFORM_TENNIS(0x0400000c, R.string.activity_type_platform_tennis),
    TABLE_TENNIS(0x0400000d, R.string.activity_type_table_tennis, R.drawable.ic_activity_pingpong),
    AMERICAN_FOOTBALL(0x0400000e, R.string.activity_type_american_football),
    TRAINING(0x0400000f, R.string.activity_type_training),
    CARDIO(0x04000010, R.string.activity_type_cardio, R.drawable.ic_heartrate),
    BREATHWORK(0x04000011, R.string.activity_type_breathwork),
    INDOOR_WALKING(0x04000012, R.string.activity_type_indoor_walking, R.drawable.ic_activity_walking),
    XC_CLASSIC_SKI(0x04000013, R.string.activity_type_xc_classic_ski),
    SKIING(0x04000014, R.string.activity_type_skiing),
    SNOWBOARDING(0x04000015, R.string.activity_type_snowboarding),
    ROWING(0x04000016, R.string.activity_type_rowing, R.drawable.ic_activity_rowing),
    MOUNTAINEERING(0x04000017, R.string.activity_type_mountaineering, R.drawable.ic_activity_climbing),
    MULTISPORT(0x04000019, R.string.activity_type_multisport),
    PADDLING(0x0400001a, R.string.activity_type_paddling, R.drawable.ic_activity_rowing),
    FLYING(0x0400001b, R.string.activity_type_flying),
    MOTORCYCLING(0x0400001d, R.string.activity_type_motorcycling),
    BOATING(0x0400001e, R.string.activity_type_boating, R.drawable.ic_activity_boating),
    DRIVING(0x0400001f, R.string.activity_type_driving),
    GOLF(0x04000020, R.string.activity_type_golf),
    HANG_GLIDING(0x04000021, R.string.activity_type_hang_gliding),
    HUNTING(0x04000023, R.string.activity_type_hunting),
    FISHING(0x04000024, R.string.activity_type_fishing),
    INLINE_SKATING(0x04000025, R.string.activity_type_inline_skating),
    ROCK_CLIMBING(0x04000026, R.string.activity_type_rock_climbing, R.drawable.ic_activity_rock_climbing),
    CLIMB_INDOOR(0x04000027, R.string.activity_type_climb_indoor),
    BOULDERING(0x04000028, R.string.activity_type_bouldering),
    SAIL_RACE(0x0400002a, R.string.activity_type_sail_race, R.drawable.ic_activity_sailing),
    SAIL_EXPEDITION(0x0400002b, R.string.activity_type_sail_expedition, R.drawable.ic_activity_sailing),
    ICE_SKATING(0x0400002c, R.string.activity_type_ice_skating, R.drawable.ic_activity_ice_skating),
    SKY_DIVING(0x0400002d, R.string.activity_type_sky_diving),
    SNOWSHOE(0x0400002e, R.string.activity_type_snowshoe),
    SNOWMOBILING(0x0400002f, R.string.activity_type_snowmobiling),
    STAND_UP_PADDLEBOARDING(0x04000030, R.string.activity_type_stand_up_paddleboarding, R.drawable.ic_activity_sup),
    SURFING(0x04000031, R.string.activity_type_surfing, R.drawable.ic_activity_surfing),
    WAKEBOARDING(0x04000032, R.string.activity_type_wakeboarding, R.drawable.ic_activity_wakeboarding),
    WATER_SKIING(0x04000033, R.string.activity_type_water_skiing, R.drawable.ic_activity_waterskiing),
    KAYAKING(0x04000034, R.string.activity_type_kayaking, R.drawable.ic_activity_rowing),
    RAFTING(0x04000035, R.string.activity_type_rafting, R.drawable.ic_activity_rowing),
    WINDSURFING(0x04000036, R.string.activity_type_windsurfing, R.drawable.ic_activity_windsurfing),
    KITESURFING(0x04000037, R.string.activity_type_kitesurfing, R.drawable.ic_activity_kitesurfing),
    TACTICAL(0x04000038, R.string.activity_type_tactical),
    JUMPMASTER(0x04000039, R.string.activity_type_jumpmaster),
    BOXING(0x0400003a, R.string.activity_type_boxing),
    FLOOR_CLIMBING(0x0400003b, R.string.activity_type_floor_climbing),
    BASEBALL(0x0400003c, R.string.activity_type_baseball),
    SOFTBALL(0x0400003d, R.string.activity_type_softball),
    SOFTBALL_SLOW_PITCH(0x0400003e, R.string.activity_type_softball_slow_pitch),
    SHOOTING(0x0400003f, R.string.activity_type_shooting),
    AUTO_RACING(0x04000040, R.string.activity_type_auto_racing),
    WINTER_SPORT(0x04000041, R.string.activity_type_winter_sport),
    GRINDING(0x04000042, R.string.activity_type_grinding),
    HEALTH_SNAPSHOT(0x04000043, R.string.activity_type_health_snapshot),
    MARINE(0x04000044, R.string.activity_type_marine),
    HIIT(0x04000045, R.string.activity_type_hiit),
    VIDEO_GAMING(0x04000046, R.string.activity_type_video_gaming, R.drawable.ic_videogame),
    RACKET(0x04000047, R.string.activity_type_racket),
    PICKLEBALL(0x04000048, R.string.activity_type_pickleball),
    PADEL(0x04000049, R.string.activity_type_padel),
    SQUASH(0x0400004a, R.string.activity_type_squash),
    RACQUETBALL(0x0400004b, R.string.activity_type_racquetball),
    PUSH_WALK_SPEED(0x0400004c, R.string.activity_type_push_walk_speed),
    INDOOR_PUSH_WALK_SPEED(0x0400004d, R.string.activity_type_indoor_push_walk_speed),
    PUSH_RUN_SPEED(0x0400004e, R.string.activity_type_push_run_speed),
    INDOOR_PUSH_RUN_SPEED(0x0400004f, R.string.activity_type_indoor_push_run_speed),
    MEDITATION(0x04000050, R.string.activity_type_meditation),
    PARA_SPORT(0x04000051, R.string.activity_type_para_sport),
    DISC_GOLF(0x04000052, R.string.activity_type_disc_golf),
    ULTIMATE_DISC(0x04000053, R.string.activity_type_ultimate_disc),
    TEAM_SPORT(0x04000054, R.string.activity_type_team_sport),
    RUGBY(0x04000055, R.string.activity_type_rugby),
    HOCKEY(0x04000056, R.string.activity_type_hockey),
    LACROSSE(0x04000057, R.string.activity_type_lacrosse),
    VOLLEYBALL(0x04000058, R.string.activity_type_volleyball),
    WATER_TUBING(0x04000059, R.string.activity_type_water_tubing, R.drawable.ic_activity_watertubing),
    WAKESURFING(0x0400005a, R.string.activity_type_wakesurfing),
    MIXED_MARTIAL_ARTS(0x0400005b, R.string.activity_type_mixed_martial_arts), // aka MMA
    DANCE(0x0400005c, R.string.activity_type_dance),
    MOUNTAIN_HIKE(0x040000e2, R.string.activity_type_mountain_hike, R.drawable.ic_activity_climbing),
    CROSS_TRAINER(0x0400005d, R.string.activity_type_cross_trainer),
    FREE_TRAINING(0x0400005e, R.string.activity_type_free_training, R.drawable.ic_activity_free_training),
    DYNAMIC_CYCLE(0x0400005f, R.string.activity_type_dynamic_cycle),
    KICKBOXING(0x04000060, R.string.activity_type_kickboxing),
    FITNESS_EXERCISES(0x04000061, R.string.activity_type_fitness_exercises),
    CROSSFIT(0x04000062, R.string.activity_type_crossfit),
    FUNCTIONAL_TRAINING(0x04000063, R.string.activity_type_functional_training),
    PHYSICAL_TRAINING(0x04000064, R.string.activity_type_physical_training),
    TAEKWONDO(0x04000065, R.string.activity_type_taekwondo),
    TAE_BO(0x04000066, R.string.activity_type_tae_bo),
    CROSS_COUNTRY_RUNNING(0x04000067, R.string.activity_type_cross_country_running),
    KARATE(0x04000068, R.string.activity_type_karate),
    FENCING(0x04000069, R.string.activity_type_fencing),
    CORE_TRAINING(0x0400006a, R.string.activity_type_core_training),
    KENDO(0x0400006b, R.string.activity_type_kendo),
    HORIZONTAL_BAR(0x0400006c, R.string.activity_type_horizontal_bar),
    PARALLEL_BAR(0x0400006d, R.string.activity_type_parallel_bar),
    COOLDOWN(0x0400006e, R.string.activity_type_cooldown),
    CROSS_TRAINING(0x0400006f, R.string.activity_type_cross_training),
    SIT_UPS(0x04000070, R.string.activity_type_sit_ups),
    FITNESS_GAMING(0x04000071, R.string.activity_type_fitness_gaming),
    AEROBIC_EXERCISE(0x04000072, R.string.activity_type_aerobic_exercise),
    ROLLING(0x04000073, R.string.activity_type_rolling),
    FLEXIBILITY(0x04000074, R.string.activity_type_flexibility),
    GYMNASTICS(0x04000075, R.string.activity_type_gymnastics),
    TRACK_AND_FIELD(0x04000076, R.string.activity_type_track_and_field),
    PUSH_UPS(0x04000077, R.string.activity_type_push_ups),
    BATTLE_ROPE(0x04000078, R.string.activity_type_battle_rope),
    SMITH_MACHINE(0x04000079, R.string.activity_type_smith_machine),
    PULL_UPS(0x0400007a, R.string.activity_type_pull_ups),
    PLANK(0x0400007b, R.string.activity_type_plank),
    JAVELIN(0x0400007c, R.string.activity_type_javelin),
    LONG_JUMP(0x0400007d, R.string.activity_type_long_jump),
    HIGH_JUMP(0x0400007e, R.string.activity_type_high_jump),
    TRAMPOLINE(0x0400007f, R.string.activity_type_trampoline),
    DUMBBELL(0x04000080, R.string.activity_type_dumbbell),
    BELLY_DANCE(0x04000081, R.string.activity_type_belly_dance),
    JAZZ_DANCE(0x04000082, R.string.activity_type_jazz_dance),
    LATIN_DANCE(0x04000083, R.string.activity_type_latin_dance),
    BALLET(0x04000084, R.string.activity_type_ballet),
    STREET_DANCE(0x04000085, R.string.activity_type_street_dance),
    ZUMBA(0x04000086, R.string.activity_type_zumba),
    ROLLER_SKATING(0x04000087, R.string.activity_type_roller_skating),
    MARTIAL_ARTS(0x04000088, R.string.activity_type_martial_arts),
    TAI_CHI(0x04000089, R.string.activity_type_tai_chi),
    HULA_HOOPING(0x0400008a, R.string.activity_type_hula_hooping, R.drawable.ic_activity_hula_hoop),
    DISC_SPORTS(0x0400008b, R.string.activity_type_disc_sports),
    DARTS(0x0400008c, R.string.activity_type_darts),
    ARCHERY(0x0400008d, R.string.activity_type_archery, R.drawable.ic_activity_archery),
    HORSE_RIDING(0x0400008e, R.string.activity_type_horse_riding),
    KITE_FLYING(0x0400008f, R.string.activity_type_kite_flying),
    SWING(0x04000090, R.string.activity_type_swing),
    STAIRS(0x04000091, R.string.activity_type_stairs, R.drawable.ic_activity_stairs),
    MIND_AND_BODY(0x04000092, R.string.activity_type_mind_and_body),
    WRESTLING(0x04000093, R.string.activity_type_wrestling),
    KABADDI(0x04000094, R.string.activity_type_kabaddi),
    KARTING(0x04000095, R.string.activity_type_karting),
    BILLIARDS(0x04000096, R.string.activity_type_billiards),
    BOWLING(0x04000097, R.string.activity_type_bowling, R.drawable.ic_activity_bowling),
    SHUTTLECOCK(0x04000098, R.string.activity_type_shuttlecock),
    HANDBALL(0x04000099, R.string.activity_type_handball),
    DODGEBALL(0x0400009a, R.string.activity_type_dodgeball),
    AUSTRALIAN_FOOTBALL(0x0400009b, R.string.activity_type_australian_football),
    LACROSS(0x0400009c, R.string.activity_type_lacross),
    SHOT(0x0400009d, R.string.activity_type_shot),
    BEACH_SOCCER(0x0400009e, R.string.activity_type_beach_soccer),
    BEACH_VOLLEYBALL(0x0400009f, R.string.activity_type_beach_volleyball),
    GATEBALL(0x040000a0, R.string.activity_type_gateball),
    SEPAK_TAKRAW(0x040000a1, R.string.activity_type_sepak_takraw),
    SAILING(0x040000a2, R.string.activity_type_sailing, R.drawable.ic_activity_sailing),
    JET_SKIING(0x040000a3, R.string.activity_type_jet_skiing),
    SKATING(0x040000a4, R.string.activity_type_skating),
    ICE_HOCKEY(0x040000a5, R.string.activity_type_ice_hockey),
    CURLING(0x040000a6, R.string.activity_type_curling, R.drawable.ic_activity_curling),
    CROSS_COUNTRY_SKIING(0x040000a8, R.string.activity_type_cross_country_skiing),
    SNOW_SPORTS(0x040000a9, R.string.activity_type_snow_sports),
    LUGE(0x040000ab, R.string.activity_type_luge),
    SKATEBOARDING(0x040000ac, R.string.activity_type_skateboarding),
    PARACHUTING(0x040000ae, R.string.activity_type_parachuting),
    PARKOUR(0x040000af, R.string.activity_type_parkour),
    INDOOR_RUNNING(0x040000b0, R.string.activity_type_indoor_running, R.drawable.ic_activity_indoor_running),
    OUTDOOR_RUNNING(0x040000b1, R.string.activity_type_outdoor_running, R.drawable.ic_activity_running),
    OUTDOOR_WALKING(0x040000b2, R.string.activity_type_outdoor_walking, R.drawable.ic_activity_outdoor_walking),
    OUTDOOR_CYCLING(0x040000b3, R.string.activity_type_outdoor_cycling, R.drawable.ic_activity_biking),
    AEROBIC_COMBO(0x040000b4, R.string.activity_type_aerobic_combo),
    AEROBICS(0x040000b5, R.string.activity_type_aerobics),
    AIR_WALKER(0x040000b6, R.string.activity_type_air_walker),
    ARTISTIC_SWIMMING(0x040000b7, R.string.activity_type_artistic_swimming),
    BALLROOM_DANCE(0x040000b8, R.string.activity_type_ballroom_dance),
    BMX(0x040000b9, R.string.activity_type_bmx),
    BOARD_GAME(0x040000ba, R.string.activity_type_board_game),
    BOCCE(0x040000bb, R.string.activity_type_bocce),
    BREAKING(0x040000bc, R.string.activity_type_breaking),
    BRIDGE(0x040000bd, R.string.activity_type_bridge),
    CARDIO_COMBAT(0x040000be, R.string.activity_type_cardio_combat),
    CHECKERS(0x040000bf, R.string.activity_type_checkers),
    CHESS(0x040000c0, R.string.activity_type_chess),
    DRAGON_BOAT(0x040000c1, R.string.activity_type_dragon_boat),
    ESPORTS(0x040000c2, R.string.activity_type_esports),
    FINSWIMMING(0x040000c3, R.string.activity_type_finswimming),
    FLOWRIDING(0x040000c4, R.string.activity_type_flowriding),
    FOLK_DANCE(0x040000c5, R.string.activity_type_folk_dance),
    FRISBEE(0x040000c6, R.string.activity_type_frisbee, R.drawable.ic_activity_frisbee),
    FUTSAL(0x040000c7, R.string.activity_type_futsal),
    HACKY_SACK(0x040000c8, R.string.activity_type_hacky_sack),
    HIP_HOP(0x040000c9, R.string.activity_type_hip_hop),
    HULA_HOOP(0x040000ca, R.string.activity_type_hula_hoop, R.drawable.ic_activity_hula_hoop),
    INDOOR_FITNESS(0x040000cb, R.string.activity_type_indoor_fitness),
    INDOOR_ICE_SKATING(0x040000cc, R.string.activity_type_indoor_ice_skating, R.drawable.ic_activity_ice_skating),
    JAI_ALAI(0x040000cd, R.string.activity_type_jai_alai),
    JUDO(0x040000ce, R.string.activity_type_judo),
    JUJITSU(0x040000cf, R.string.activity_type_jujitsu),
    MASS_GYMNASTICS(0x040000d0, R.string.activity_type_mass_gymnastics),
    MODERN_DANCE(0x040000d1, R.string.activity_type_modern_dance),
    MUAY_THAI(0x040000d2, R.string.activity_type_muay_thai),
    PARALLEL_BARS(0x040000d3, R.string.activity_type_parallel_bars),
    POLE_DANCE(0x040000d4, R.string.activity_type_pole_dance),
    RACE_WALKING(0x040000d5, R.string.activity_type_race_walking),
    SHUFFLEBOARD(0x040000d6, R.string.activity_type_shuffleboard),
    SNORKELING(0x040000d7, R.string.activity_type_snorkeling, R.drawable.ic_activity_snorkeling),
    SOMATOSENSORY_GAME(0x040000d8, R.string.activity_type_somatosensory_game),
    SPINNING(0x040000d9, R.string.activity_type_spinning),
    SQUARE_DANCE(0x040000da, R.string.activity_type_square_dance),
    STAIR_CLIMBER(0x040000db, R.string.activity_type_stair_climber),
    STEPPER(0x040000dc, R.string.activity_type_stepper),
    STRETCHING(0x040000dd, R.string.activity_type_stretching, R.drawable.ic_activity_stretching),
    TABLE_FOOTBALL(0x040000de, R.string.activity_type_table_football),
    TUG_OF_WAR(0x040000df, R.string.activity_type_tug_of_war),
    WALL_BALL(0x040000e0, R.string.activity_type_wall_ball),
    WATER_POLO(0x040000e1, R.string.activity_type_water_polo),
    WEIQI(0x040000e3, R.string.activity_type_weiqi),
    FREE_SPARRING(0x040000e4, R.string.activity_type_free_sparring),
    BODY_COMBAT(0x040000e5, R.string.activity_type_body_combat),
    PLAZA_DANCING(0x040000e6, R.string.activity_type_plaza_dancing),
    LASER_TAG(0x040000e7, R.string.activity_type_laser_tag),
    OBSTACLE_RACE(0x040000e8, R.string.activity_type_obstacle_race, R.drawable.ic_activity_obstacle_race),
    BILLIARD_POOL(0x040000e9, R.string.activity_type_billiard_pool, R.drawable.ic_activity_billiard_pool),
    CANOEING(0x040000ea, R.string.activity_type_canoeing),
    WATER_SCOOTER(0x040000eb, R.string.activity_type_water_scooter),
    BOBSLEIGH(0x040000ec, R.string.activity_type_bobsleigh),
    SLEDDING(0x040000ed, R.string.activity_type_sledding),
    BIATHLON(0x040000ee, R.string.activity_type_biathlon),
    BUNGEE_JUMPING(0x040000ef, R.string.activity_type_bungee_jumping),
    ORIENTEERING(0x040000f0, R.string.activity_type_orienteering),
    TREKKING(0x040000f1, R.string.activity_type_trekking, R.drawable.ic_activity_hiking),
    TRAIL_RUN(0x040000f2, R.string.activity_type_trail_run, R.drawable.ic_activity_trail_run),
    ;

    private final int code;
    private final int label;
    private final int icon;

    ActivityKind(final int code) {
        this(code, R.string.activity_type_unknown);
    }

    ActivityKind(final int code, @StringRes final int label) {
        this(code, label, R.drawable.ic_activity_unknown_small);
    }

    ActivityKind(final int code, @StringRes final int label, @DrawableRes final int icon) {
        this.code = code;
        this.label = label;
        this.icon = icon;
    }

    public int getCode() {
        return code;
    }

    @StringRes
    public int getLabel() {
        return label;
    }

    public String getLabel(final Context context) {
        return context.getString(label);
    }

    @DrawableRes
    public int getIcon() {
        return icon;
    }

    public static ActivityKind fromCode(final int code) {
        for (final ActivityKind kind : ActivityKind.values()) {
            if (kind.code == code) {
                return kind;
            }
        }

        //throw new IllegalArgumentException("Unknown ActivityKind code " + code);
        return UNKNOWN;
    }

    public static boolean isSleep(final ActivityKind activityKind) {
        return activityKind == ActivityKind.SLEEP_ANY
                || activityKind == ActivityKind.LIGHT_SLEEP
                || activityKind == ActivityKind.DEEP_SLEEP
                || activityKind == ActivityKind.REM_SLEEP
                || activityKind == ActivityKind.AWAKE_SLEEP;
    }

    public static boolean isPaceActivity(final ActivityKind activityKind) {
        return activityKind.name().contains("RUN") || activityKind.name().contains("SWIM") ||
                activityKind.name().contains("TREADMILL") || activityKind.name().contains("WALK");
    }

}
