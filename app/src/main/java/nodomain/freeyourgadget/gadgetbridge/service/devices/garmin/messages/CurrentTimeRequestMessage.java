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
        this.statusMessage = null; //our outgoing message is an ACK message
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

        final int nextTransitionEndsGarminTs;
        final int nextTransitionStartsGarminTs;

        if (nextTransitionStart != null) {
            final int nextTransitionStartsTs = (int) nextTransitionStart.toEpochSecond();
            nextTransitionStartsGarminTs = GarminTimeUtils.unixTimeToGarminTimestamp(nextTransitionStartsTs);

            final ZoneOffsetTransition nextTransitionEnd = zoneRules.nextTransition(nextTransitionStart.getInstant());

            if (nextTransitionEnd != null) {
                final int nextTransitionEndsTs = (int) nextTransitionEnd.toEpochSecond();
                nextTransitionEndsGarminTs = GarminTimeUtils.unixTimeToGarminTimestamp(nextTransitionEndsTs);
            } else {
                nextTransitionEndsGarminTs = 0;
            }
        } else {
            nextTransitionEndsGarminTs = 0;
            nextTransitionStartsGarminTs = 0;
        }

        LOG.info(
                "Processing current time request #{}: time={}, DST={}, ofs={}, nextTransitionEnds={}, nextTransitionStarts={}",
                referenceID,
                garminTimestamp,
                dstOffset,
                timeZoneOffset,
                nextTransitionEndsGarminTs,
                nextTransitionStartsGarminTs
        );

        final MessageWriter writer = new MessageWriter(response);
        writer.writeShort(0); // packet size will be filled below
        writer.writeShort(GarminMessage.RESPONSE.getId());
        writer.writeShort(this.garminMessage.getId());
        writer.writeByte(Status.ACK.ordinal());
        writer.writeInt(referenceID);
        writer.writeInt(garminTimestamp);
        writer.writeInt(timeZoneOffset);
        writer.writeInt(nextTransitionEndsGarminTs);
        writer.writeInt(nextTransitionStartsGarminTs);
        return true;
    }
}
