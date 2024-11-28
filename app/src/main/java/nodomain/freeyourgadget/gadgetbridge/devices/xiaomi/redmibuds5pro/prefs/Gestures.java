package nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.redmibuds5pro.prefs;

public class Gestures {

    public enum InteractionType {
        SINGLE((byte) 0x04),
        DOUBLE((byte) 0x01),
        TRIPLE((byte) 0x02),
        LONG((byte) 0x03);

        public final byte value;

        InteractionType(byte value) {
            this.value = value;
        }
    }

    public enum Position {
        LEFT,
        RIGHT
    }
}
