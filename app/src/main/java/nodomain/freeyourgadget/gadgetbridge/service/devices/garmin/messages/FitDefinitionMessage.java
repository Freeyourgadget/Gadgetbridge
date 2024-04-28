package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;

public class FitDefinitionMessage extends GFDIMessage {

    private final List<RecordDefinition> recordDefinitions;

    public FitDefinitionMessage(List<RecordDefinition> recordDefinitions, GarminMessage garminMessage) {
        this.recordDefinitions = recordDefinitions;
        this.garminMessage = garminMessage;
        this.statusMessage = this.getStatusMessage();
    }

    public FitDefinitionMessage(List<RecordDefinition> recordDefinitions) {
        this.recordDefinitions = recordDefinitions;
        this.garminMessage = GarminMessage.FIT_DEFINITION;
    }

    public static FitDefinitionMessage parseIncoming(MessageReader reader, GarminMessage garminMessage) {
        List<RecordDefinition> recordDefinitions = new ArrayList<>();

        while (reader.remaining() > 0) {
            RecordHeader recordHeader = new RecordHeader((byte) reader.readByte());
            recordDefinitions.add(RecordDefinition.parseIncoming(reader, recordHeader));
        }

        return new FitDefinitionMessage(recordDefinitions, garminMessage);
    }

    public List<RecordDefinition> getRecordDefinitions() {
        return recordDefinitions;
    }

    @Override
    protected boolean generateOutgoing() {
        final MessageWriter writer = new MessageWriter(response);
        writer.writeShort(0); // packet size will be filled below
        writer.writeShort(garminMessage.getId());
        for (RecordDefinition recordDefinition : recordDefinitions) {
            recordDefinition.generateOutgoingPayload(writer);
        }
        return true;
    }

}
