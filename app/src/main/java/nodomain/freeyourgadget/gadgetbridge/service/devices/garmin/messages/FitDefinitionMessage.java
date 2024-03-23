package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;

public class FitDefinitionMessage extends GFDIMessage {

    private final List<RecordDefinition> recordDefinitions;
    private final int messageType;

    public FitDefinitionMessage(List<RecordDefinition> recordDefinitions, int messageType) {
        this.recordDefinitions = recordDefinitions;
        this.messageType = messageType;
    }

    public FitDefinitionMessage(List<RecordDefinition> recordDefinitions) {
        this.recordDefinitions = recordDefinitions;
        this.messageType = GarminMessage.FIT_DEFINITION.getId();
    }

    public static FitDefinitionMessage parseIncoming(MessageReader reader, int messageType) {
        List<RecordDefinition> recordDefinitions = new ArrayList<>();

        while (!reader.isEndOfPayload()) {
            RecordHeader recordHeader = new RecordHeader((byte) reader.readByte());
            recordDefinitions.add(RecordDefinition.parseIncoming(reader, recordHeader));
        }

        return new FitDefinitionMessage(recordDefinitions, messageType);
    }

    public List<RecordDefinition> getRecordDefinitions() {
        return recordDefinitions;
    }

    @Override
    protected boolean generateOutgoing() {
        final MessageWriter writer = new MessageWriter(response);
        writer.writeShort(0); // packet size will be filled below
        writer.writeShort(messageType);
        for (RecordDefinition recordDefinition : recordDefinitions) {
            recordDefinition.generateOutgoingPayload(writer);
        }
        return true;
    }

}
