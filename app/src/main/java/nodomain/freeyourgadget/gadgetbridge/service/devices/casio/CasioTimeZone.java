/*  Copyright (C) 2023-2024 Johannes Krude

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

package nodomain.freeyourgadget.gadgetbridge.service.devices.casio;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.Arrays;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneOffsetTransitionRule;
import java.time.zone.ZoneRules;


public class CasioTimeZone {

/*
There are six clocks on the Casio GW-B5600 / S100
0 is the main clock
1-5 are the world clocks

0x1d 00 01 DST0 DST1 TZ0A TZ0B TZ1A TZ1B ff ff ff ff ff
0x1d 02 03 DST2 DST3 TZ2A TZ2B TZ3A TZ3B ff ff ff ff ff
0x1d 04 05 DST4 DST5 TZ4A TZ4B TZ5A TZ5B ff ff ff ff ff
DST: bitwise flags; bit0: DST on, bit1: DST auto

0x1e 0-5 TZ_A TZ_B TZ_OFF TZ_DSTOFF TZ_DSTRULES
A/B seem to be ignored by the watch
OFF & DSTOFF in 15 minute intervals

0x1f 0-5 (18 bytes ASCII TZ name)

Timezones selectable on the watch:
                   A  B   OFF DSTOFF DSTRULES
BAKER ISLAND       39 01  D0  04     00
PAGO PAGO          D7 00  D4  04     00
HONOLULU           7B 00  D8  04     00
MARQUESAS ISLANDS  3A 01  DA  04     00
ANCHORAGE          0C 00  DC  04     01
LOS ANGELES        A1 00  E0  04     01
DENVER             54 00  E4  04     01
CHICAGO            42 00  E8  04     01
NEW YORK           CA 00  EC  04     01
HALIFAX            71 00  F0  04     01
ST.JOHN'S          0C 01  F2  04     01
RIO DE JANEIRO     F1 00  F4  04     00
F.DE NORONHA       62 00  F8  04     00
PRAIA              E9 00  FC  04     00
UTC                00 00  00  00     00
LONDON             A0 00  00  04     02
PARIS              DC 00  04  04     02
ATHENS             13 00  08  04     02
JEDDAH             85 00  0C  04     00
TEHRAN             16 01  0E  04     2B
DUBAI              5B 00  10  04     00
KABUL              88 00  12  04     00
KARACHI            8B 00  14  04     00
DELHI              52 00  16  04     00
KATHMANDU          8C 00  17  04     00
DHAKA              56 00  18  04     00
YANGON             2F 01  1A  04     00
BANGKOK            1C 00  1C  04     00
HONG KONG          7A 00  20  04     00
PYONGYANG          EA 00  24  04     00
EUCLA              36 01  23  04     00
TOKYO              19 01  24  04     00
ADELAIDE           05 00  26  04     04
SYDNEY             0F 01  28  04     04
LORD HOWE ISLAND   37 01  2A  02     12
NOUMEA             CD 00  2C  04     00
WELLINGTON         2B 01  30  04     05
CHATHAM ISLANDS    3F 00  33  04     17
NUKUALOFA          D0 00  34  04     00
KIRITIMATI         93 00  38  04     00

Timezones NOT selectable on the watch:
                   A  B   OFF DSTOFF DSTRULES
CASABLANCA         3A 00  00  04     0F
BEIRUT             22 00  08  04     0C
JERUSALEM          86 00  08  04     2A
NORFOLK ISLAND     38 01  2C  04     04
EASTER ISLAND      5E 00  E8  04     1C
HAVANA             75 00  EC  04     15
SANTIAGO           02 01  F0  04     1B
ASUNCION           12 00  F0  04     09
PONTA DELGADA      E4 00  FC  04     02

*/

    private byte[] name;
    private byte[] number;
    private byte offset;
    private byte dstOffset;
    private byte dstRules;
    private byte dstSetting;

    // bitwise flags
    final static byte DST_SETTING_ON   = 1;
    final static byte DST_SETTING_AUTO = 2;

    private CasioTimeZone(byte[] name, byte[] number, byte offset, byte dstOffset, byte dstRules, byte dstSetting) {
        this.name = name;
        this.number = number;
        this.offset = offset;
        this.dstOffset = dstOffset;
        this.dstRules = dstRules;
        this.dstSetting = dstSetting;
    }

    static public Set<Casio2C2DSupport.FeatureRequest> requests(int slot) {
        HashSet<Casio2C2DSupport.FeatureRequest> requests = new HashSet();
        requests.add(new Casio2C2DSupport.FeatureRequest(Casio2C2DSupport.FEATURE_DST_WATCH_STATE, (byte) (slot/2*2)));
        requests.add(new Casio2C2DSupport.FeatureRequest(Casio2C2DSupport.FEATURE_DST_SETTING, (byte) slot));
        requests.add(new Casio2C2DSupport.FeatureRequest(Casio2C2DSupport.FEATURE_WORLD_CITY, (byte) slot));
        return requests;
    }

    static public byte[] dstWatchStateBytes(int slotA, CasioTimeZone zoneA, int slotB, CasioTimeZone zoneB) {
        return new byte[] {
            Casio2C2DSupport.FEATURE_DST_WATCH_STATE,
            (byte) slotA,
            (byte) slotB,
            zoneA.dstSetting,
            zoneB.dstSetting,
            zoneA.number[0],
            zoneA.number[1],
            zoneB.number[0],
            zoneB.number[1],
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};
    }

    public byte[] dstSettingBytes(int slot) {
        return new byte[] {
            Casio2C2DSupport.FEATURE_DST_SETTING,
            (byte) slot,
            number[0],
            number[1],
            offset,
            dstOffset,
            dstRules};
    }

    public byte[] worldCityBytes(int slot) {
        byte[] bytes = {
            Casio2C2DSupport.FEATURE_WORLD_CITY,
            (byte) slot,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
        System.arraycopy(name, 0, bytes, 2, Math.min(name.length, 18));
        return bytes;
    }

    public static CasioTimeZone fromWatchResponses(Map<Casio2C2DSupport.FeatureRequest, byte[]> responses, int slot) {
        byte[] name = "unknown".getBytes(StandardCharsets.US_ASCII);
        byte[] number = {0,0};
        byte offset = 0;
        byte dstOffset = 0;
        byte dstRules = 0;
        byte dstSetting = 0;

        for (byte[] response: responses.values()) {
            if (response[0] == Casio2C2DSupport.FEATURE_DST_WATCH_STATE && response.length >= 9) {
                if (response[1] == slot) {
                    dstSetting = response[3];
                    number = new byte[] {response[5], response[6]};
                }
                if (response[2] == slot) {
                    dstSetting = response[4];
                    number = new byte[] {response[7], response[8]};
                }
            } else if (response[0] == Casio2C2DSupport.FEATURE_DST_SETTING && response.length >= 7 && response[1] == slot) {
                number = new byte[] {response[2], response[3]};
                offset = response[4];
                dstOffset = response[5];
                dstRules = response[6];
            } else if (response[0] == Casio2C2DSupport.FEATURE_WORLD_CITY && response.length >= 2 && response[1] == slot) {
                int size;
                for (size = 0; size < response.length-2; size++) {
                    if (response[2+size] == 0) {
                        break;
                    }
                }
                name = Arrays.copyOfRange(response, 2, 2+size);
            }
        }

        return new CasioTimeZone(name, number, offset, dstOffset, dstRules, dstSetting);
    }

    public static CasioTimeZone fromZoneId(ZoneId zone, Instant time, String zoneName) {
        ZoneRules rules = zone.getRules();

        byte[] name = zoneName.getBytes(StandardCharsets.US_ASCII);
        byte[] number = {0,0};
        byte offset = (byte) (rules.getStandardOffset(time).getTotalSeconds() / 60 / 15);
        byte dstOffset = 0; // can be set only later once we now the next transition
        byte dstRules = 0;
        byte dstSetting = 0;

        ZoneOffsetTransition next = rules.nextTransition(time);
        int nextYear = next.getInstant().atZone(zone).getYear();
        ZoneOffsetTransition next2 = (next == null ? null: rules.nextTransition(next.getInstant()));
        int next2Year = (next2 == null ? 0 : next2.getInstant().atZone(zone).getYear());

        if (next == null) {
        // no DST is easy
            dstSetting = DST_SETTING_AUTO;
        } else {
            // we need an Instant with DST on to get the dstOffset
            if (rules.isDaylightSavings(time)) {
                dstOffset = (byte) (rules.getDaylightSavings(time).getSeconds() / 60 / 15);
            } else {
                dstOffset = (byte) (rules.getDaylightSavings(next.getInstant().plusSeconds(1)).getSeconds() / 60 / 15);
            }
            // find Watch DST rules
            dstRules = findWatchDstRules(offset, dstOffset, next, nextYear, next2, next2Year);
            if (dstRules != 0) {
                // DST AUTO if the watch knows at least the next transition
                // otherwise will result in incorrect time between actual DST change and next sync
                dstSetting |= DST_SETTING_AUTO;
            }
            // if DST bit is incorrect, the watch will substract or add time
            if (rules.isDaylightSavings(time)) {
                dstSetting |= DST_SETTING_ON;
            }
        }

        return new CasioTimeZone(name, number, offset, dstOffset, dstRules, dstSetting);
    }

    // We are searching for watch DST rules which match the next two transitions.
    // In case only the next transition matches a rule, the rule is still used
    static byte findWatchDstRules(byte offset, byte dstOffset, ZoneOffsetTransition next, int nextYear, ZoneOffsetTransition next2, int next2Year) {
        WatchDstRules candidate = null;
        for (WatchDstRules r: watchDstRules) {
            int match = r.matches(offset, dstOffset, next, nextYear, next2, next2Year);
            if (match == 2)
                return r.dstRules;
            if (match == 1 && candidate == null)
                candidate = r;
        }
        if (candidate != null)
            return candidate.dstRules;
        return (byte) 0;
    }

    static class WatchDstRules {
        final byte offset;
        final byte dstOffset;
        final byte dstRules;
        final ZoneOffsetTransitionRule ruleA;
        final ZoneOffsetTransitionRule ruleB;

        WatchDstRules(int offset, int dstOffset, int dstRules, ZoneOffsetTransitionRule ruleA, ZoneOffsetTransitionRule ruleB) {
            this.offset = (byte) offset;
            this.dstOffset = (byte) dstOffset;
            this.dstRules = (byte) dstRules;
            this.ruleA = ruleA;
            this.ruleB = ruleB;
        }

        // returns how many of the next transitions match the rules
        int matches(byte offset, byte dstOffset, ZoneOffsetTransition next, int nextYear, ZoneOffsetTransition next2, int next2Year) {
            if (offset != this.offset || dstOffset != this.dstOffset)
                return -1;
            if (this.ruleA.createTransition(nextYear).equals(next)) {
                if (this.ruleB.createTransition(next2Year).equals(next2))
                    return 2;
                return 1;
            }
            if (this.ruleB.createTransition(nextYear).equals(next)) {
                if (this.ruleA.createTransition(next2Year).equals(next2))
                    return 2;
                return 1;
            }
            return 0;
        }
    }

    // All known Watch DST Rules
    // Possibly incomplete and incorrect
    //
    // When adding  new WatchDstRules, test them:
    // 1. Apply the following changes to CasioGWB5600InitOperation
    //    +    static int nextNext = 0;
    //      private void setClocks(TransactionBuilder builder) {
    //         ZoneId tz = ZoneId.systemDefault();
    //         Instant now = Instant.now().plusSeconds(2);
    //    +    now = tz.getRules().nextTransition(now).getInstant();
    //    +    if (nextNext != 0)
    //    +        now = tz.getRules().nextTransition(now).getInstant();
    //    +    nextNext ^= 1;
    //    +    now = now.minusSeconds(10);
    // 2. Sync the time on the watch and observe a DST change
    // 3. Repeat the time sync to obseve a second DST change
    static final WatchDstRules[] watchDstRules = {
        // Europe/London
        new WatchDstRules(0x00, 0x04, 0x02,
            ZoneOffsetTransitionRule.of(Month.MARCH, 25, DayOfWeek.SUNDAY, LocalTime.of(1, 0), false, ZoneOffsetTransitionRule.TimeDefinition.UTC, ZoneOffset.ofTotalSeconds(0), ZoneOffset.ofTotalSeconds(0), ZoneOffset.ofTotalSeconds(3600)),
            ZoneOffsetTransitionRule.of(Month.OCTOBER, 25, DayOfWeek.SUNDAY, LocalTime.of(1, 0), false, ZoneOffsetTransitionRule.TimeDefinition.UTC, ZoneOffset.ofTotalSeconds(0), ZoneOffset.ofTotalSeconds(3600), ZoneOffset.ofTotalSeconds(0))),
        // Europe/Paris
        new WatchDstRules(0x04, 0x04, 0x02,
            ZoneOffsetTransitionRule.of(Month.MARCH, 25, DayOfWeek.SUNDAY, LocalTime.of(1, 0), false, ZoneOffsetTransitionRule.TimeDefinition.UTC, ZoneOffset.ofTotalSeconds(3600), ZoneOffset.ofTotalSeconds(3600), ZoneOffset.ofTotalSeconds(7200)),
            ZoneOffsetTransitionRule.of(Month.OCTOBER, 25, DayOfWeek.SUNDAY, LocalTime.of(1, 0), false, ZoneOffsetTransitionRule.TimeDefinition.UTC, ZoneOffset.ofTotalSeconds(3600), ZoneOffset.ofTotalSeconds(7200), ZoneOffset.ofTotalSeconds(3600))),
        // Europe/Athen
        new WatchDstRules(0x08, 0x04, 0x02,
            ZoneOffsetTransitionRule.of(Month.MARCH, 25, DayOfWeek.SUNDAY, LocalTime.of(1, 0), false, ZoneOffsetTransitionRule.TimeDefinition.UTC, ZoneOffset.ofTotalSeconds(7200), ZoneOffset.ofTotalSeconds(7200), ZoneOffset.ofTotalSeconds(10800)),
            ZoneOffsetTransitionRule.of(Month.OCTOBER, 25, DayOfWeek.SUNDAY, LocalTime.of(1, 0), false, ZoneOffsetTransitionRule.TimeDefinition.UTC, ZoneOffset.ofTotalSeconds(7200), ZoneOffset.ofTotalSeconds(10800), ZoneOffset.ofTotalSeconds(7200))),
        // Asia/Beirut
        new WatchDstRules(0x08, 0x04, 0x0C,
            ZoneOffsetTransitionRule.of(Month.MARCH, 25, DayOfWeek.SUNDAY, LocalTime.of(0, 0), false, ZoneOffsetTransitionRule.TimeDefinition.WALL, ZoneOffset.ofTotalSeconds(7200), ZoneOffset.ofTotalSeconds(7200), ZoneOffset.ofTotalSeconds(10800)),
            ZoneOffsetTransitionRule.of(Month.OCTOBER, 25, DayOfWeek.SUNDAY, LocalTime.of(0, 0), false, ZoneOffsetTransitionRule.TimeDefinition.WALL, ZoneOffset.ofTotalSeconds(7200), ZoneOffset.ofTotalSeconds(10800), ZoneOffset.ofTotalSeconds(7200))),
        // Asia/Jerusalem
        new WatchDstRules(0x08, 0x04, 0x2A,
            ZoneOffsetTransitionRule.of(Month.MARCH, 23, DayOfWeek.FRIDAY, LocalTime.of(2, 0), false, ZoneOffsetTransitionRule.TimeDefinition.WALL, ZoneOffset.ofTotalSeconds(7200), ZoneOffset.ofTotalSeconds(7200), ZoneOffset.ofTotalSeconds(10800)),
            ZoneOffsetTransitionRule.of(Month.OCTOBER, 25, DayOfWeek.SUNDAY, LocalTime.of(2, 0), false, ZoneOffsetTransitionRule.TimeDefinition.WALL, ZoneOffset.ofTotalSeconds(7200), ZoneOffset.ofTotalSeconds(10800), ZoneOffset.ofTotalSeconds(7200))),
        // Australia/Adelaide
        new WatchDstRules(0x26, 0x04, 0x04,
            ZoneOffsetTransitionRule.of(Month.APRIL, 1, DayOfWeek.SUNDAY, LocalTime.of(2, 0), false, ZoneOffsetTransitionRule.TimeDefinition.STANDARD, ZoneOffset.ofTotalSeconds(34200), ZoneOffset.ofTotalSeconds(37800), ZoneOffset.ofTotalSeconds(34200)),
            ZoneOffsetTransitionRule.of(Month.OCTOBER, 1, DayOfWeek.SUNDAY, LocalTime.of(2, 0), false, ZoneOffsetTransitionRule.TimeDefinition.STANDARD, ZoneOffset.ofTotalSeconds(34200), ZoneOffset.ofTotalSeconds(34200), ZoneOffset.ofTotalSeconds(37800))),
        // Australia/Sydney
        new WatchDstRules(0x28, 0x04, 0x04,
            ZoneOffsetTransitionRule.of(Month.APRIL, 1, DayOfWeek.SUNDAY, LocalTime.of(2, 0), false, ZoneOffsetTransitionRule.TimeDefinition.STANDARD, ZoneOffset.ofTotalSeconds(36000), ZoneOffset.ofTotalSeconds(39600), ZoneOffset.ofTotalSeconds(36000)),
            ZoneOffsetTransitionRule.of(Month.OCTOBER, 1, DayOfWeek.SUNDAY, LocalTime.of(2, 0), false, ZoneOffsetTransitionRule.TimeDefinition.STANDARD, ZoneOffset.ofTotalSeconds(36000), ZoneOffset.ofTotalSeconds(36000), ZoneOffset.ofTotalSeconds(39600))),
        // Australia/Lord_Howe
        new WatchDstRules(0x2A, 0x02, 0x12,
            ZoneOffsetTransitionRule.of(Month.APRIL, 1, DayOfWeek.SUNDAY, LocalTime.of(2, 0), false, ZoneOffsetTransitionRule.TimeDefinition.WALL, ZoneOffset.ofTotalSeconds(37800), ZoneOffset.ofTotalSeconds(39600), ZoneOffset.ofTotalSeconds(37800)),
            ZoneOffsetTransitionRule.of(Month.OCTOBER, 1, DayOfWeek.SUNDAY, LocalTime.of(2, 0), false, ZoneOffsetTransitionRule.TimeDefinition.WALL, ZoneOffset.ofTotalSeconds(37800), ZoneOffset.ofTotalSeconds(37800), ZoneOffset.ofTotalSeconds(39600))),
        // Pacific/Norfolk
        new WatchDstRules(0x2C, 0x04, 0x04,
            ZoneOffsetTransitionRule.of(Month.APRIL, 1, DayOfWeek.SUNDAY, LocalTime.of(2, 0), false, ZoneOffsetTransitionRule.TimeDefinition.STANDARD, ZoneOffset.ofTotalSeconds(39600), ZoneOffset.ofTotalSeconds(43200), ZoneOffset.ofTotalSeconds(39600)),
            ZoneOffsetTransitionRule.of(Month.OCTOBER, 1, DayOfWeek.SUNDAY, LocalTime.of(2, 0), false, ZoneOffsetTransitionRule.TimeDefinition.STANDARD, ZoneOffset.ofTotalSeconds(39600), ZoneOffset.ofTotalSeconds(39600), ZoneOffset.ofTotalSeconds(43200))),
        // Pacific/Auckland
        new WatchDstRules(0x30, 0x04, 0x05,
            ZoneOffsetTransitionRule.of(Month.APRIL, 1, DayOfWeek.SUNDAY, LocalTime.of(2, 0), false, ZoneOffsetTransitionRule.TimeDefinition.STANDARD, ZoneOffset.ofTotalSeconds(43200), ZoneOffset.ofTotalSeconds(46800), ZoneOffset.ofTotalSeconds(43200)),
            ZoneOffsetTransitionRule.of(Month.SEPTEMBER, 24, DayOfWeek.SUNDAY, LocalTime.of(2, 0), false, ZoneOffsetTransitionRule.TimeDefinition.STANDARD, ZoneOffset.ofTotalSeconds(43200), ZoneOffset.ofTotalSeconds(43200), ZoneOffset.ofTotalSeconds(46800))),
        // Pacific/Chatham
        new WatchDstRules(0x33, 0x04, 0x17,
            ZoneOffsetTransitionRule.of(Month.APRIL, 1, DayOfWeek.SUNDAY, LocalTime.of(2, 45), false, ZoneOffsetTransitionRule.TimeDefinition.STANDARD, ZoneOffset.ofTotalSeconds(45900), ZoneOffset.ofTotalSeconds(49500), ZoneOffset.ofTotalSeconds(45900)),
            ZoneOffsetTransitionRule.of(Month.SEPTEMBER, 24, DayOfWeek.SUNDAY, LocalTime.of(2, 45), false, ZoneOffsetTransitionRule.TimeDefinition.STANDARD, ZoneOffset.ofTotalSeconds(45900), ZoneOffset.ofTotalSeconds(45900), ZoneOffset.ofTotalSeconds(49500))),
        // America/Anchorage
        new WatchDstRules(0xDC, 0x04, 0x01,
            ZoneOffsetTransitionRule.of(Month.MARCH, 8, DayOfWeek.SUNDAY, LocalTime.of(2, 0), false, ZoneOffsetTransitionRule.TimeDefinition.WALL, ZoneOffset.ofTotalSeconds(-32400), ZoneOffset.ofTotalSeconds(-32400), ZoneOffset.ofTotalSeconds(-28800)),
            ZoneOffsetTransitionRule.of(Month.NOVEMBER, 1, DayOfWeek.SUNDAY, LocalTime.of(2, 0), false, ZoneOffsetTransitionRule.TimeDefinition.WALL, ZoneOffset.ofTotalSeconds(-32400), ZoneOffset.ofTotalSeconds(-28800), ZoneOffset.ofTotalSeconds(-32400))),
        // America/Los_Angeles
        new WatchDstRules(0xE0, 0x04, 0x01,
            ZoneOffsetTransitionRule.of(Month.MARCH, 8, DayOfWeek.SUNDAY, LocalTime.of(2, 0), false, ZoneOffsetTransitionRule.TimeDefinition.WALL, ZoneOffset.ofTotalSeconds(-28800), ZoneOffset.ofTotalSeconds(-28800), ZoneOffset.ofTotalSeconds(-25200)),
            ZoneOffsetTransitionRule.of(Month.NOVEMBER, 1, DayOfWeek.SUNDAY, LocalTime.of(2, 0), false, ZoneOffsetTransitionRule.TimeDefinition.WALL, ZoneOffset.ofTotalSeconds(-28800), ZoneOffset.ofTotalSeconds(-25200), ZoneOffset.ofTotalSeconds(-28800))),
        // America/Denver
        new WatchDstRules(0xE4, 0x04, 0x01,
            ZoneOffsetTransitionRule.of(Month.MARCH, 8, DayOfWeek.SUNDAY, LocalTime.of(2, 0), false, ZoneOffsetTransitionRule.TimeDefinition.WALL, ZoneOffset.ofTotalSeconds(-25200), ZoneOffset.ofTotalSeconds(-25200), ZoneOffset.ofTotalSeconds(-21600)),
            ZoneOffsetTransitionRule.of(Month.NOVEMBER, 1, DayOfWeek.SUNDAY, LocalTime.of(2, 0), false, ZoneOffsetTransitionRule.TimeDefinition.WALL, ZoneOffset.ofTotalSeconds(-25200), ZoneOffset.ofTotalSeconds(-21600), ZoneOffset.ofTotalSeconds(-25200))),
        // America/Chicago
        new WatchDstRules(0xE8, 0x04, 0x01,
            ZoneOffsetTransitionRule.of(Month.MARCH, 8, DayOfWeek.SUNDAY, LocalTime.of(2, 0), false, ZoneOffsetTransitionRule.TimeDefinition.WALL, ZoneOffset.ofTotalSeconds(-21600), ZoneOffset.ofTotalSeconds(-21600), ZoneOffset.ofTotalSeconds(-18000)),
            ZoneOffsetTransitionRule.of(Month.NOVEMBER, 1, DayOfWeek.SUNDAY, LocalTime.of(2, 0), false, ZoneOffsetTransitionRule.TimeDefinition.WALL, ZoneOffset.ofTotalSeconds(-21600), ZoneOffset.ofTotalSeconds(-18000), ZoneOffset.ofTotalSeconds(-21600))),
        // Chile/EasterIsland
        new WatchDstRules(0xE8, 0x04, 0x1C,
            ZoneOffsetTransitionRule.of(Month.APRIL, 2, DayOfWeek.SUNDAY, LocalTime.of(3, 0), false, ZoneOffsetTransitionRule.TimeDefinition.UTC, ZoneOffset.ofTotalSeconds(-21600), ZoneOffset.ofTotalSeconds(-18000), ZoneOffset.ofTotalSeconds(-21600)),
            ZoneOffsetTransitionRule.of(Month.SEPTEMBER, 2, DayOfWeek.SUNDAY, LocalTime.of(4, 0), false, ZoneOffsetTransitionRule.TimeDefinition.UTC, ZoneOffset.ofTotalSeconds(-21600), ZoneOffset.ofTotalSeconds(-21600), ZoneOffset.ofTotalSeconds(-18000))),
        // America/New_York
        new WatchDstRules(0xEC, 0x04, 0x01,
            ZoneOffsetTransitionRule.of(Month.MARCH, 8, DayOfWeek.SUNDAY, LocalTime.of(2, 0), false, ZoneOffsetTransitionRule.TimeDefinition.WALL, ZoneOffset.ofTotalSeconds(-18000), ZoneOffset.ofTotalSeconds(-18000), ZoneOffset.ofTotalSeconds(-14400)),
            ZoneOffsetTransitionRule.of(Month.NOVEMBER, 1, DayOfWeek.SUNDAY, LocalTime.of(2, 0), false, ZoneOffsetTransitionRule.TimeDefinition.WALL, ZoneOffset.ofTotalSeconds(-18000), ZoneOffset.ofTotalSeconds(-14400), ZoneOffset.ofTotalSeconds(-18000))),
        // America/Havana
        new WatchDstRules(0xEC, 0x04, 0x15,
            ZoneOffsetTransitionRule.of(Month.MARCH, 8, DayOfWeek.SUNDAY, LocalTime.of(0, 0), false, ZoneOffsetTransitionRule.TimeDefinition.STANDARD, ZoneOffset.ofTotalSeconds(-18000), ZoneOffset.ofTotalSeconds(-18000), ZoneOffset.ofTotalSeconds(-14400)),
            ZoneOffsetTransitionRule.of(Month.NOVEMBER, 1, DayOfWeek.SUNDAY, LocalTime.of(0, 0), false, ZoneOffsetTransitionRule.TimeDefinition.STANDARD, ZoneOffset.ofTotalSeconds(-18000), ZoneOffset.ofTotalSeconds(-14400), ZoneOffset.ofTotalSeconds(-18000))),
        // America/Santiago
        new WatchDstRules(0xF0, 0x04, 0x1B,
            ZoneOffsetTransitionRule.of(Month.APRIL, 2, DayOfWeek.SUNDAY, LocalTime.of(3, 0), false, ZoneOffsetTransitionRule.TimeDefinition.UTC, ZoneOffset.ofTotalSeconds(-14400), ZoneOffset.ofTotalSeconds(-10800), ZoneOffset.ofTotalSeconds(-14400)),
            ZoneOffsetTransitionRule.of(Month.SEPTEMBER, 2, DayOfWeek.SUNDAY, LocalTime.of(4, 0), false, ZoneOffsetTransitionRule.TimeDefinition.UTC, ZoneOffset.ofTotalSeconds(-14400), ZoneOffset.ofTotalSeconds(-14400), ZoneOffset.ofTotalSeconds(-10800))),
        // America/Halifax
        new WatchDstRules(0xF0, 0x04, 0x01,
            ZoneOffsetTransitionRule.of(Month.MARCH, 8, DayOfWeek.SUNDAY, LocalTime.of(2, 0), false, ZoneOffsetTransitionRule.TimeDefinition.WALL, ZoneOffset.ofTotalSeconds(-14400), ZoneOffset.ofTotalSeconds(-14400), ZoneOffset.ofTotalSeconds(-10800)),
            ZoneOffsetTransitionRule.of(Month.NOVEMBER, 1, DayOfWeek.SUNDAY, LocalTime.of(2, 0), false, ZoneOffsetTransitionRule.TimeDefinition.WALL, ZoneOffset.ofTotalSeconds(-14400), ZoneOffset.ofTotalSeconds(-10800), ZoneOffset.ofTotalSeconds(-14400))),
        // America/Asuncion
        new WatchDstRules(0xF0, 0x04, 0x09,
            ZoneOffsetTransitionRule.of(Month.MARCH, 22, DayOfWeek.SUNDAY, LocalTime.of(0, 0), false, ZoneOffsetTransitionRule.TimeDefinition.WALL, ZoneOffset.ofTotalSeconds(-14400), ZoneOffset.ofTotalSeconds(-10800), ZoneOffset.ofTotalSeconds(-14400)),
            ZoneOffsetTransitionRule.of(Month.OCTOBER, 1, DayOfWeek.SUNDAY, LocalTime.of(0, 0), false, ZoneOffsetTransitionRule.TimeDefinition.WALL, ZoneOffset.ofTotalSeconds(-14400), ZoneOffset.ofTotalSeconds(-14400), ZoneOffset.ofTotalSeconds(-10800))),
        // America/St_Johns
        new WatchDstRules(0xF2, 0x04, 0x01,
            ZoneOffsetTransitionRule.of(Month.MARCH, 8, DayOfWeek.SUNDAY, LocalTime.of(2, 0), false, ZoneOffsetTransitionRule.TimeDefinition.WALL, ZoneOffset.ofTotalSeconds(-12600), ZoneOffset.ofTotalSeconds(-12600), ZoneOffset.ofTotalSeconds(-9000)),
            ZoneOffsetTransitionRule.of(Month.NOVEMBER, 1, DayOfWeek.SUNDAY, LocalTime.of(2, 0), false, ZoneOffsetTransitionRule.TimeDefinition.WALL, ZoneOffset.ofTotalSeconds(-12600), ZoneOffset.ofTotalSeconds(-9000), ZoneOffset.ofTotalSeconds(-12600))),
        // Atlantic/Azores
        new WatchDstRules(0xFC, 0x04, 0x02,
            ZoneOffsetTransitionRule.of(Month.MARCH, 25, DayOfWeek.SUNDAY, LocalTime.of(1, 0), false, ZoneOffsetTransitionRule.TimeDefinition.UTC, ZoneOffset.ofTotalSeconds(-3600), ZoneOffset.ofTotalSeconds(-3600), ZoneOffset.ofTotalSeconds(0)),
            ZoneOffsetTransitionRule.of(Month.OCTOBER, 25, DayOfWeek.SUNDAY, LocalTime.of(1, 0), false, ZoneOffsetTransitionRule.TimeDefinition.UTC, ZoneOffset.ofTotalSeconds(-3600), ZoneOffset.ofTotalSeconds(0), ZoneOffset.ofTotalSeconds(-3600))),
    };
}
