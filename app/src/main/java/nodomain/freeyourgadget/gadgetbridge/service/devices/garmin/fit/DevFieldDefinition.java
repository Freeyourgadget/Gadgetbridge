package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.MessageReader;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.MessageWriter;

public class DevFieldDefinition {
    public final ByteBuffer valueHolder;
    private final int localNumber;
    private final int size;
    private final int developerDataIndex;
    private final String name;

    public DevFieldDefinition(int localNumber, int size, int developerDataIndex, String name) {
        this.localNumber = localNumber;
        this.size = size;
        this.developerDataIndex = developerDataIndex;
        this.name = name;
        this.valueHolder = ByteBuffer.allocate(size);
    }

    public static DevFieldDefinition parseIncoming(MessageReader reader) {
        int number = reader.readByte();
        int size = reader.readByte();
        int developerDataIndex = reader.readByte();

        return new DevFieldDefinition(number, size, developerDataIndex, "");

    }

    public int getLocalNumber() {
        return localNumber;
    }

    public int getSize() {
        return size;
    }

    public String getName() {
        return name;
    }

    public void generateOutgoingPayload(MessageWriter writer) { //TODO
    }

    public Object decode() { //TODO
        return null;
    }


    public void encode(Object o) { //TODO
    }


}
