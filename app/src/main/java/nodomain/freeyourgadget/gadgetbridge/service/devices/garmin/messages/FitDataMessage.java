package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.GlobalDefinitionsEnum;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;

public class FitDataMessage extends GFDIMessage {
    private final List<RecordData> recordDataList;

    public FitDataMessage(List<RecordData> recordDataList, GarminMessage garminMessage) {
        this.recordDataList = recordDataList;
        this.garminMessage = garminMessage;
    }

    public FitDataMessage(List<RecordData> recordDataList) {
        this.recordDataList = recordDataList;
        this.garminMessage = GarminMessage.FIT_DATA;
    }

    public static FitDataMessage parseIncoming(MessageReader reader, GarminMessage garminMessage) {
        List<RecordData> recordDataList = new ArrayList<>();


        while (!reader.isEndOfPayload()) {
            RecordHeader recordHeader = new RecordHeader((byte) reader.readByte());
            if (recordHeader.isDefinition())
                return null;
            RecordData recordData = new RecordData(GlobalDefinitionsEnum.getRecordDefinitionfromMesgType(recordHeader.getMesgType()));
            recordData.parseDataMessage(reader);
            recordDataList.add(recordData);
        }

        return new FitDataMessage(recordDataList, garminMessage);
    }

    public List<RecordData> getRecordDataList() {
        return recordDataList;
    }

    @Override
    protected boolean generateOutgoing() {
        final MessageWriter writer = new MessageWriter(response);
        writer.writeShort(0); // packet size will be filled below
        writer.writeShort(this.garminMessage.getId());
        for (RecordData recordData : recordDataList) {
            recordData.generateOutgoingDataPayload(writer);
        }
        return true;
    }

}
