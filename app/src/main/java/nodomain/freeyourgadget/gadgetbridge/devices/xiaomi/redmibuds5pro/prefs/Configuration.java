package nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.redmibuds5pro.prefs;

public class Configuration {

    public enum Config {
        GESTURES((byte) 0x02),
        AUTO_ANSWER((byte) 0x03),
        DOUBLE_CONNECTION((byte) 0x04),
        EAR_DETECTION((byte) 0x06),
        EQ_PRESET((byte) 0x07),
        LONG_GESTURES((byte) 0x0a),
        EFFECT_STRENGTH((byte) 0x0b),
        ADAPTIVE_ANC((byte) 0x25),
        ADAPTIVE_SOUND((byte) 0x29),
        EQ_CURVE((byte) 0x37),
        CUSTOMIZED_ANC((byte) 0x3b),
        UNKNOWN((byte) 0xff);

        public final byte value;

        Config(byte value) {
            this.value = value;
        }

        public static Config fromCode(final byte code) {
            for (final Config config : values()) {
                if (config.value == code) {
                    return config;
                }
            }
            return Config.UNKNOWN;
        }

    }

    public enum StrengthTarget {
        ANC((byte) 0x01),
        TRANSPARENCY((byte) 0x02);

        public final byte value;

        StrengthTarget(byte value) {
            this.value = value;
        }
    }

}
