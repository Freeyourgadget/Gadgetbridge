/*  Copyright (C) 2022-2024 Petr Vaněk

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
package nodomain.freeyourgadget.gadgetbridge.devices.supercars;

import java.util.UUID;

public class SuperCarsConstants {

    //https://gist.github.com/scrool/e79d6a4cb50c26499746f4fe473b3768#encryption
    public static final byte[] aes_key = new byte[]{(byte) 0x34, (byte) 0x52, (byte) 0x2A, (byte) 0x5B, (byte) 0x7A, (byte) 0x6E, (byte) 0x49, (byte) 0x2C, (byte) 0x08, (byte) 0x09, (byte) 0x0A, (byte) 0x9D, (byte) 0x8D, (byte) 0x2A, (byte) 0x23, (byte) 0xF8};

    public static final UUID SERVICE_UUID_FFF = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
    public static final UUID CHARACTERISTIC_UUID_FFF1 = UUID.fromString("d44bc439-abfd-45a2-b575-925416129600");
    public static final UUID CHARACTERISTIC_UUID_FFF2 = UUID.fromString("d44bc439-abfd-45a2-b575-92541612960a");
    public static final UUID CHARACTERISTIC_UUID_FFF3 = UUID.fromString("d44bc439-abfd-45a2-b575-92541612960b");
    public static final UUID CHARACTERISTIC_UUID_FFF4 = UUID.fromString("d44bc439-abfd-45a2-b575-925416129601");

    public static final UUID SERVICE_UUID_FD = UUID.fromString("0000fd00-0000-1000-8000-00805f9b34fb");
    public static final UUID CHARACTERISTIC_UUID_FD1 = UUID.fromString("0000fd01-0000-1000-8000-00805f9b34fb");
    public static final UUID CHARACTERISTIC_UUID_FD2 = UUID.fromString("0000fd02-0000-1000-8000-00805f9b34fb");

    public enum Speed {
        NORMAL, TURBO
    }

    public enum Light {
        ON, OFF
    }

    public enum Movement {
        UP, DOWN, IDLE
    }

    public enum Direction {
        LEFT, RIGHT, CENTER
    }

    public enum Tricks {
        OFF, CIRCLE_RIGHT, CIRCLE_LEFT, U_TURN_LEFT, U_TURN_RIGHT
    }

    static Enum[] fwd_r = {Movement.UP, Direction.RIGHT};
    static Enum[] fwd_l = {Movement.UP, Direction.LEFT};
    static Enum[] stop = {Movement.IDLE, Direction.CENTER};

    public static final Enum[][] tricks_circle_right = {
            fwd_r, fwd_r, fwd_r, fwd_r, fwd_r, fwd_r, fwd_r, fwd_r, fwd_r, fwd_r,
            fwd_r, fwd_r, fwd_r, fwd_r, fwd_r, fwd_r, fwd_r, fwd_r, fwd_r, fwd_r,
            fwd_r, fwd_r, fwd_r, fwd_r, fwd_r, fwd_r, fwd_r, fwd_r, fwd_r, fwd_r,
            stop
    };

    public static final Enum[][] tricks_circle_left = {
            fwd_l, fwd_l, fwd_l, fwd_l, fwd_l, fwd_l, fwd_l, fwd_l, fwd_l, fwd_l,
            fwd_l, fwd_l, fwd_l, fwd_l, fwd_l, fwd_l, fwd_l, fwd_l, fwd_l, fwd_l,
            fwd_l, fwd_l, fwd_l, fwd_l, fwd_l, fwd_l, fwd_l, fwd_l, fwd_l, fwd_l,
            stop
    };

    public static final Enum[][] tricks_u_turn_right = {
            fwd_r, fwd_r, fwd_r, fwd_r, fwd_r, fwd_r, fwd_r, fwd_r, fwd_r, fwd_r,
            fwd_r, fwd_r, fwd_r, fwd_r, fwd_r, fwd_r, fwd_r, fwd_r, fwd_r, fwd_r,
            stop
    };

    public static final Enum[][] tricks_u_turn_left = {
            fwd_l, fwd_l, fwd_l, fwd_l, fwd_l, fwd_l, fwd_l, fwd_l, fwd_l, fwd_l,
            fwd_l, fwd_l, fwd_l, fwd_l, fwd_l, fwd_l, fwd_l, fwd_l, fwd_l, fwd_l,
            stop
    };


    public static final Enum[][] get_trick(Tricks trick) {
        switch (trick) {
            case CIRCLE_RIGHT:
                return tricks_circle_right;
            case CIRCLE_LEFT:
                return tricks_circle_left;
            case U_TURN_LEFT:
                return tricks_u_turn_left;
            case U_TURN_RIGHT:
                return tricks_u_turn_right;
        }
        return tricks_circle_right;
    }
}

