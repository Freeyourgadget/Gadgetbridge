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
}

