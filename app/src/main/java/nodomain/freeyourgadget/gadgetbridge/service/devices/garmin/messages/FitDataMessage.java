package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.GlobalDefinitionsEnum;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;

public class FitDataMessage extends GFDIMessage {
    private final List<RecordData> recordDataList;
    private final int messageType;

    public FitDataMessage(List<RecordData> recordDataList, int messageType) {
        this.recordDataList = recordDataList;
        this.messageType = messageType;
    }

    public FitDataMessage(List<RecordData> recordDataList) {
        this.recordDataList = recordDataList;
        this.messageType = GarminMessage.FIT_DATA.getId();
    }

    public static FitDataMessage parseIncoming(MessageReader reader, int messageType) {
        List<RecordData> recordDataList = new ArrayList<>();


        while (!reader.isEndOfPayload()) {
            RecordHeader recordHeader = new RecordHeader((byte) reader.readByte());
            if (recordHeader.isDefinition())
                return null;
            RecordData recordData = new RecordData(GlobalDefinitionsEnum.getRecordDefinitionfromMesgType(recordHeader.getMesgType()));
            recordData.parseDataMessage(reader);
            recordDataList.add(recordData);
        }

        return new FitDataMessage(recordDataList, messageType);
    }

    public List<RecordData> getRecordDataList() {
        return recordDataList;
    }

    @Override
    protected boolean generateOutgoing() {
        final MessageWriter writer = new MessageWriter(response);
        writer.writeShort(0); // packet size will be filled below
        writer.writeShort(messageType);
        for (RecordData recordData : recordDataList) {
            recordData.generateOutgoingDataPayload(writer);
        }
        return true;
    }

}
