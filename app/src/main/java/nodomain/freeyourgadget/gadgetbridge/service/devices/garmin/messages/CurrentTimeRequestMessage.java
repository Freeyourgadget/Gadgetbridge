package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages;


import java.time.Instant;
import java.time.ZoneId;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneRules;
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

        return new CurrentTimeRequestMessage(referenceID, garminMessage);
    }

    @Override
    protected boolean generateOutgoing() {

        final Instant now = Instant.now();
        final ZoneRules zoneRules = ZoneId.systemDefault().getRules();
        final int dstOffset = (int) zoneRules.getDaylightSavings(now).getSeconds();
        final int timeZoneOffset = TimeZone.getDefault().getOffset(now.toEpochMilli()) / 1000;
        final int garminTimestamp = GarminTimeUtils.unixTimeToGarminTimestamp((int) now.getEpochSecond());
        final ZoneOffsetTransition nextTransitionStart = zoneRules.nextTransition(now);

        final int nextTransitionStartsTs = (int) nextTransitionStart.toEpochSecond();
        final int nextTransitionEndsTs = (int) zoneRules.nextTransition(nextTransitionStart.getInstant()).toEpochSecond();


        LOG.info("Processing current time request #{}: time={}, DST={}, ofs={}", referenceID, garminTimestamp, dstOffset, timeZoneOffset);

        final MessageWriter writer = new MessageWriter(response);
        writer.writeShort(0); // packet size will be filled below
        writer.writeShort(GarminMessage.RESPONSE.getId());
        writer.writeShort(this.garminMessage.getId());
        writer.writeByte(Status.ACK.ordinal());
        writer.writeInt(referenceID);
        writer.writeInt(garminTimestamp);
        writer.writeInt(timeZoneOffset);
        writer.writeInt(GarminTimeUtils.unixTimeToGarminTimestamp(nextTransitionEndsTs));
        writer.writeInt(GarminTimeUtils.unixTimeToGarminTimestamp(nextTransitionStartsTs));
        return true;
    }
}
