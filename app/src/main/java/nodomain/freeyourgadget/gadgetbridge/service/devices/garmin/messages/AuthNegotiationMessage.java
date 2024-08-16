package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages;

import org.apache.commons.lang3.EnumUtils;

import java.util.EnumSet;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status.AuthNegotiationStatusMessage;

public class AuthNegotiationMessage extends GFDIMessage {

    private final int unknown;
    private final EnumSet<AuthFlags> requestedAuthFlags;

    public AuthNegotiationMessage(GarminMessage garminMessage, int unknown, EnumSet<AuthFlags> requestedAuthFlags) {
        this.garminMessage = garminMessage;
        this.unknown = unknown;
        this.requestedAuthFlags = requestedAuthFlags;

        LOG.info("Message {}, unkByte: {}, flags: {}", garminMessage, unknown, requestedAuthFlags);

        this.statusMessage = new AuthNegotiationStatusMessage(garminMessage, Status.ACK, AuthNegotiationStatusMessage.AuthNegotiationStatus.GUESS_OK, this.unknown, requestedAuthFlags);
    }

    public static AuthNegotiationMessage parseIncoming(MessageReader reader, GarminMessage garminMessage) {

        final int unk = reader.readByte();
        final EnumSet<AuthFlags> authFlags = AuthFlags.fromBitMask(reader.readInt());

        return new AuthNegotiationMessage(garminMessage, unk, authFlags);
    }

    @Override
    protected boolean generateOutgoing() {

        final MessageWriter writer = new MessageWriter(response);
        writer.writeShort(0); // placeholder for packet size
        writer.writeShort(this.garminMessage.getId());

        //set all to 0 as we don't know what else to do
        writer.writeByte(0);
        writer.writeInt((int) EnumUtils.generateBitVector(AuthFlags.class, EnumSet.noneOf(AuthFlags.class)));

        return false;
    }

    public enum AuthFlags {
        UNK_00000001, //saw in logs
        UNK_00000010,
        UNK_00000100,
        UNK_00001000,
        UNK_00010000,
        UNK_00100000,
        UNK_01000000,
        UNK_10000000,
        ;

        public static EnumSet<AuthFlags> fromBitMask(final int code) {
            return EnumUtils.processBitVector(AuthFlags.class, code);
        }
    }
}
