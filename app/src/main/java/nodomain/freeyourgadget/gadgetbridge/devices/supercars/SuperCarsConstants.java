package nodomain.freeyourgadget.gadgetbridge.devices.supercars;

import java.util.UUID;

public class SuperCarsConstants {

    public static final UUID SERVICE_UUID_FFF = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
    public static final UUID CHARACTERISTIC_UUID_FFF1 = UUID.fromString("d44bc439-abfd-45a2-b575-925416129600");
    public static final UUID CHARACTERISTIC_UUID_FFF2 = UUID.fromString("d44bc439-abfd-45a2-b575-92541612960a");
    public static final UUID CHARACTERISTIC_UUID_FFF3 = UUID.fromString("d44bc439-abfd-45a2-b575-92541612960b");
    public static final UUID CHARACTERISTIC_UUID_FFF4 = UUID.fromString("d44bc439-abfd-45a2-b575-925416129601");

    public static final UUID SERVICE_UUID_FD = UUID.fromString("0000fd00-0000-1000-8000-00805f9b34fb");
    public static final UUID CHARACTERISTIC_UUID_FD1 = UUID.fromString("0000fd01-0000-1000-8000-00805f9b34fb");
    public static final UUID CHARACTERISTIC_UUID_FD2 = UUID.fromString("0000fd02-0000-1000-8000-00805f9b34fb");

    public static final byte[] idle_normal = new byte[]{0x02, 0x5e, 0x69, 0x5a, 0x48, (byte) 0xff, 0x2a, 0x43, (byte) 0x8c, (byte) 0xa6, (byte) 0x80, (byte) 0xf8, 0x3e, 0x04, (byte) 0xe4, 0x5d};
    public static final byte[] up_normal = new byte[]{0x29, 0x60, (byte) 0x9c, 0x66, 0x48, 0x52, (byte) 0xcf, (byte) 0xf1, (byte) 0xb0, (byte) 0xf0, (byte) 0xcb, (byte) 0xb9, (byte) 0x80, 0x14, (byte) 0xbd, 0x2c};
    public static final byte[] down_normal = new byte[]{0x03, 0x20, (byte) 0x99, 0x09, (byte) 0xba, (byte) 0x9d, (byte) 0xa1, (byte) 0xc8, (byte) 0xb9, (byte) 0x86, 0x16, 0x3c, 0x6d, 0x48, 0x46, 0x55};
    public static final byte[] up_left_normal = new byte[]{(byte) 0x99, 0x28, (byte) 0xe5, (byte) 0x90, (byte) 0xdf, (byte) 0xe8, 0x21, 0x48, 0x5f, 0x41, 0x4f, (byte) 0xbb, 0x63, 0x3d, 0x5c, 0x4e};
    public static final byte[] up_right_normal = new byte[]{0x0f, 0x2c, (byte) 0xe5, 0x66, 0x62, (byte) 0xd4, (byte) 0xfd, (byte) 0x9d, 0x32, (byte) 0xa4, 0x4f, 0x10, 0x2b, (byte) 0xf2, 0x0a, (byte) 0xa7};
    public static final byte[] down_left_normal = new byte[]{(byte) 0x98, (byte) 0xce, (byte) 0x98, 0x1d, 0x58, (byte) 0xd1, 0x15, (byte) 0xaf, (byte) 0xe1, 0x19, 0x60, (byte) 0xbf, 0x46, 0x13, (byte) 0x92, 0x5c};
    public static final byte[] down_right_normal = new byte[]{(byte) 0xf2, 0x52, 0x0f, (byte) 0xba, 0x31, 0x44, (byte) 0xfb, 0x11, 0x46, (byte) 0x8f, (byte) 0xe0, (byte) 0x80, (byte) 0xc6, (byte) 0xc2, (byte) 0xc2, 0x3c};

    public static final byte[] idle_lights = new byte[]{0x39, (byte) 0xb5, 0x3b, (byte) 0x9b, (byte) 0xb7, (byte) 0xa0, (byte) 0xe0, (byte) 0xd6, 0x52, 0x54, (byte) 0xf9, (byte) 0xea, (byte) 0x84, 0x5d, (byte) 0xde, (byte) 0xee};
    public static final byte[] up_lights = new byte[]{0x2e, (byte) 0x90, (byte) 0xb1, (byte) 0xd3, 0x6b, (byte) 0x8f, (byte) 0xad, 0x5f, (byte) 0x96, 0x7d, (byte) 0xb3, 0x2e, 0x6e, 0x6c, (byte) 0xfd, 0x6e};
    public static final byte[] down_lights = new byte[]{0x2b, 0x41, 0x0a, 0x47, (byte) 0xce, 0x03, 0x59, (byte) 0xe0, 0x4d, 0x75, (byte) 0xfd, 0x0e, (byte) 0xe3, (byte) 0x9f, (byte) 0xe1, (byte) 0xbd};
    public static final byte[] up_right_lights = new byte[]{0x06, 0x4e, 0x40, (byte) 0x8c, 0x32, 0x52, (byte) 0xfb, 0x32, 0x7b, (byte) 0xd9, (byte) 0xfc, 0x54, (byte) 0x9b, 0x53, (byte) 0xee, 0x3e};
    public static final byte[] up_left_lights = new byte[]{0x23, 0x77, 0x1d, (byte) 0xc2, 0x2a, 0x73, (byte) 0x89, (byte) 0x99, 0x2f, 0x53, (byte) 0xac, 0x59, 0x38, (byte) 0xc1, 0x78, (byte) 0x91};
    public static final byte[] down_right_lights = new byte[]{(byte) 0xbb, (byte) 0xd6, 0x2a, (byte) 0xac, 0x32, (byte) 0x8c, (byte) 0x9e, 0x31, 0x65, 0x33, (byte) 0xc8, 0x0e, (byte) 0x9a, (byte) 0xcb, (byte) 0xf6, 0x4b};
    public static final byte[] down_left_lights = new byte[]{0x6e, (byte) 0xaa, (byte) 0xf0, (byte) 0xc0, (byte) 0x85, (byte) 0x8f, 0x14, 0x77, 0x6f, (byte) 0xd8, (byte) 0xf0, 0x71, 0x39, (byte) 0xa0, 0x08, (byte) 0xf2};

    public static final byte[] idle_turbo = new byte[]{(byte) 0xd9, (byte) 0x94, (byte) 0xb3, (byte) 0x71, (byte) 0xc3, (byte) 0xbe, (byte) 0x2a, (byte) 0x9a, (byte) 0x9d, (byte) 0x04, (byte) 0x88, (byte) 0xa1, (byte) 0x04, (byte) 0x4b, (byte) 0x7f, (byte) 0x67};
    public static final byte[] up_turbo = new byte[]{(byte) 0xe6, (byte) 0x55, (byte) 0x67, (byte) 0xda, (byte) 0x8e, (byte) 0x6c, (byte) 0x56, (byte) 0x0d, (byte) 0x09, (byte) 0xd3, (byte) 0x73, (byte) 0x3a, (byte) 0x7f, (byte) 0x47, (byte) 0xff, (byte) 0x06};
    public static final byte[] down_turbo = new byte[]{(byte) 0xce, (byte) 0xc2, (byte) 0xff, (byte) 0x1d, (byte) 0x7a, (byte) 0xcc, (byte) 0x16, (byte) 0x3c, (byte) 0xd1, (byte) 0x3b, (byte) 0x7e, (byte) 0x61, (byte) 0x53, (byte) 0xad, (byte) 0x5c, (byte) 0x45};
    public static final byte[] up_right_turbo = new byte[]{(byte) 0xfb, (byte) 0x97, (byte) 0x6f, (byte) 0xba, (byte) 0x04, (byte) 0xaf, (byte) 0x87, (byte) 0x02, (byte) 0x22, (byte) 0x26, (byte) 0xec, (byte) 0x50, (byte) 0xae, (byte) 0x82, (byte) 0xf8, (byte) 0xc4};
    public static final byte[] up_left_turbo = new byte[]{0x59, (byte) 0x23, (byte) 0x81, (byte) 0xc9, (byte) 0x43, (byte) 0xa4, (byte) 0x17, (byte) 0xca, (byte) 0x1b, (byte) 0xc3, (byte) 0xb5, (byte) 0x94, (byte) 0x00, (byte) 0xe0, (byte) 0xfc, (byte) 0x12};
    public static final byte[] down_right_turbo = new byte[]{(byte) 0x80, (byte) 0xdf, (byte) 0xb2, (byte) 0x16, (byte) 0x5f, (byte) 0x32, (byte) 0x60, (byte) 0xf1, (byte) 0xd9, (byte) 0x83, (byte) 0x77, (byte) 0x50, (byte) 0xf4, (byte) 0x3a, (byte) 0x43, (byte) 0xda};
    public static final byte[] down_left_turbo = new byte[]{(byte) 0xd5, (byte) 0x4a, (byte) 0xd5, (byte) 0x58, (byte) 0x57, (byte) 0xd3, (byte) 0x27, (byte) 0x74, (byte) 0x5f, (byte) 0x14, (byte) 0x1d, (byte) 0xd0, (byte) 0x0d, (byte) 0x67, (byte) 0x15, (byte) 0x95};

    public static final byte[] idle_turbo_lights = new byte[]{(byte) 0x1a, (byte) 0x01, (byte) 0x1e, (byte) 0x9e, (byte) 0x6e, (byte) 0xfc, (byte) 0xce, (byte) 0x22, (byte) 0xbe, (byte) 0x8e, (byte) 0xb7, (byte) 0xff, (byte) 0xb6, (byte) 0x29, (byte) 0xfa, (byte) 0x75};
    public static final byte[] up_turbo_lights = new byte[]{(byte) 0xd6, (byte) 0xc7, (byte) 0x63, (byte) 0x8e, (byte) 0x0e, (byte) 0x9a, (byte) 0x80, (byte) 0xbe, (byte) 0x60, (byte) 0x88, (byte) 0xfc, (byte) 0x44, (byte) 0x43, (byte) 0x07, (byte) 0xa1, (byte) 0x78};
    public static final byte[] down_turbo_lights = new byte[]{(byte) 0x0d, (byte) 0x4f, (byte) 0xf8, (byte) 0x23, (byte) 0xac, (byte) 0xf9, (byte) 0xb7, (byte) 0xef, (byte) 0x1c, (byte) 0x26, (byte) 0xd4, (byte) 0xb4, (byte) 0x56, (byte) 0x51, (byte) 0x59, (byte) 0x52};
    public static final byte[] up_right_turbo_lights = new byte[]{(byte) 0x83, (byte) 0xe6, (byte) 0x59, (byte) 0x42, (byte) 0x3d, (byte) 0x4b, (byte) 0x78, (byte) 0x48, (byte) 0x14, (byte) 0x5d, (byte) 0x86, (byte) 0xa1, (byte) 0x7b, (byte) 0x54, (byte) 0x7e, (byte) 0x58};
    public static final byte[] up_left_turbo_lights = new byte[]{(byte) 0xd1, (byte) 0x7f, (byte) 0x6c, (byte) 0x5e, (byte) 0xe6, (byte) 0xba, (byte) 0x81, (byte) 0xe2, (byte) 0xb5, (byte) 0x80, (byte) 0x90, (byte) 0xa3, (byte) 0xcc, (byte) 0x76, (byte) 0x37, (byte) 0x9f};
    public static final byte[] down_right_turbo_lights = new byte[]{(byte) 0xd4, (byte) 0x5d, (byte) 0xc9, (byte) 0x8f, (byte) 0x76, (byte) 0x58, (byte) 0xf9, (byte) 0x02, (byte) 0x0f, (byte) 0x93, (byte) 0xa0, (byte) 0xfd, (byte) 0x80, (byte) 0xe2, (byte) 0x2d, (byte) 0x45};
    public static final byte[] down_left_turbo_lights = new byte[]{(byte) 0x96, (byte) 0xc0, (byte) 0x35, (byte) 0x77, (byte) 0xe9, (byte) 0xcd, (byte) 0xa8, (byte) 0xb9, (byte) 0x70, (byte) 0x21, (byte) 0x5f, (byte) 0xaf, (byte) 0x35, (byte) 0x00, (byte) 0x3b, (byte) 0x74};

    public static final byte[] left_data = new byte[]{0x51, 0x38, 0x21, 0x12, 0x13, 0x5c, (byte) 0xcc, (byte) 0xdb, (byte) 0x46, (byte) 0xcf, (byte) 0x89, 0x21, (byte) 0xb7, 0x05, 0x49, (byte) 0x9a};
    public static final byte[] right_data = new byte[]{0x1b, 0x57, 0x69, (byte) 0xcd, (byte) 0xf1, 0x3e, (byte) 0x8a, (byte) 0xb6, 0x27, 0x08, 0x0f, (byte) 0xf3, (byte) 0xce, (byte) 0xfc, 0x3b, (byte) 0xc0};

    public enum SpeedModes {
        NORMAL, TURBO, LIGHTS, TURBO_LIGHTS
    }

    public enum Directions {
        UP, DOWN, UP_LEFT, UP_RIGHT, DOWN_LEFT, DOWN_RIGHT
    }

    public static byte[] get_directions_data(SpeedModes speedModes, Directions directions) {
        switch (speedModes) {
            case NORMAL:
                switch (directions) {
                    case UP:
                        return up_normal;
                    case UP_LEFT:
                        return up_left_normal;
                    case UP_RIGHT:
                        return up_right_normal;
                    case DOWN:
                        return down_normal;
                    case DOWN_LEFT:
                        return down_left_normal;
                    case DOWN_RIGHT:
                        return down_right_normal;
                }
                return idle_normal;
            case TURBO:
                switch (directions) {
                    case UP:
                        return up_turbo;
                    case UP_LEFT:
                        return up_left_turbo;
                    case UP_RIGHT:
                        return up_right_turbo;
                    case DOWN:
                        return down_turbo;
                    case DOWN_LEFT:
                        return down_left_turbo;
                    case DOWN_RIGHT:
                        return down_right_turbo;
                }
                return idle_turbo;
            case LIGHTS:
                switch (directions) {
                    case UP:
                        return up_lights;
                    case UP_LEFT:
                        return up_left_lights;
                    case UP_RIGHT:
                        return up_right_lights;
                    case DOWN:
                        return down_lights;
                    case DOWN_LEFT:
                        return down_left_lights;
                    case DOWN_RIGHT:
                        return down_right_lights;
                }
                return idle_lights;
            case TURBO_LIGHTS:
                switch (directions) {
                    case UP:
                        return up_turbo_lights;
                    case UP_LEFT:
                        return up_left_turbo_lights;
                    case UP_RIGHT:
                        return up_right_turbo_lights;
                    case DOWN:
                        return down_turbo_lights;
                    case DOWN_LEFT:
                        return down_left_turbo_lights;
                    case DOWN_RIGHT:
                        return down_right_turbo_lights;
                }
                return idle_turbo_lights;
        }
        return idle_lights;
    }

}

