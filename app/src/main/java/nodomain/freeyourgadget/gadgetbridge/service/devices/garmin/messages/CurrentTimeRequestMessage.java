package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages;


import java.util.Calendar;
import java.util.TimeZone;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminTimeUtils;

public class CurrentTimeRequestMessage extends GFDIMessage {
    private final int referenceID;

    public CurrentTimeRequestMessage(int referenceID, GarminMessage garminMessage) {
        this.garminMessage = garminMessage;
        this.referenceID = referenceID;
        this.statusMessage = this.getStatusMessage();
    }

    public static CurrentTimeRequestMessage parseIncoming(MessageReader reader, GarminMessage garminMessage) {
        final int referenceID = reader.readInt();

        reader.warnIfLeftover();
        return new CurrentTimeRequestMessage(referenceID, garminMessage);
    }

    @Override
    protected boolean generateOutgoing() {
        long now = System.currentTimeMillis();
        final TimeZone timeZone = TimeZone.getDefault();
        final Calendar calendar = Calendar.getInstance(timeZone);
        calendar.setTimeInMillis(now);
        int dstOffset = calendar.get(Calendar.DST_OFFSET) / 1000;
        int timeZoneOffset = timeZone.getOffset(now) / 1000;
        int garminTimestamp = GarminTimeUtils.javaMillisToGarminTimestamp(now);

        LOG.info("Processing current time request #{}: time={}, DST={}, ofs={}", referenceID, garminTimestamp, dstOffset, timeZoneOffset);

        final MessageWriter writer = new MessageWriter(response);
        writer.writeShort(0); // packet size will be filled below
        writer.writeShort(GarminMessage.RESPONSE.getId());
        writer.writeShort(this.garminMessage.getId());
        writer.writeByte(Status.ACK.ordinal());
        writer.writeInt(referenceID);
        writer.writeInt(garminTimestamp);
        writer.writeInt(timeZoneOffset);
        // TODO: next DST start/end
        writer.writeInt(0);
        writer.writeInt(0);

        return true;
    }
}
