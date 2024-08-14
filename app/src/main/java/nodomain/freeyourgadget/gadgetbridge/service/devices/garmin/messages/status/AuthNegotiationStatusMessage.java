package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status;

import androidx.annotation.Nullable;

import org.apache.commons.lang3.EnumUtils;

import java.util.EnumSet;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.AuthNegotiationMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.MessageWriter;

public class AuthNegotiationStatusMessage extends GFDIStatusMessage {

    private final Status status;
    private final AuthNegotiationStatus authNegotiationStatus;
    private final int unknown;
    private final EnumSet<AuthNegotiationMessage.AuthFlags> authFlags;
    private final boolean sendOutgoing;

    public AuthNegotiationStatusMessage(GarminMessage garminMessage, Status status, AuthNegotiationStatus authNegotiationStatus, int unknown, EnumSet<AuthNegotiationMessage.AuthFlags> authFlags) {
        this(garminMessage, status, authNegotiationStatus, unknown, authFlags, true);
    }

    public AuthNegotiationStatusMessage(GarminMessage garminMessage, Status status, AuthNegotiationStatus authNegotiationStatus, int unknown, EnumSet<AuthNegotiationMessage.AuthFlags> authFlags, boolean sendOutgoing) {
        this.garminMessage = garminMessage;
        this.status = status;
        this.authNegotiationStatus = authNegotiationStatus;
        this.unknown = unknown;
        this.authFlags = authFlags;
        this.sendOutgoing = sendOutgoing;
    }

    public static AuthNegotiationStatusMessage parseIncoming(MessageReader reader, GarminMessage garminMessage) {
        final Status status = Status.fromCode(reader.readByte());
        final int authNegotiationStatusCode = reader.readByte();
        final AuthNegotiationStatus authNegotiationStatus = AuthNegotiationStatus.fromCode(authNegotiationStatusCode);
        if (null == authNegotiationStatus) {
            LOG.warn("Unknown auth negotiation status code {}", authNegotiationStatusCode);
            return null;
        }
        final int unk = reader.readByte();
        final EnumSet<AuthNegotiationMessage.AuthFlags> authFlags = AuthNegotiationMessage.AuthFlags.fromBitMask(reader.readInt());

        switch (authNegotiationStatus) {
            case GUESS_OK:
                LOG.info("Received {}/{} for message {} unkByte: {}, flags: {}", status, authNegotiationStatus, garminMessage, unk, authFlags);
                break;
            default:
                LOG.warn("Received {}/{} for message {} unkByte: {}, flags: {}", status, authNegotiationStatus, garminMessage, unk, authFlags);
        }
        return new AuthNegotiationStatusMessage(garminMessage, status, authNegotiationStatus, unk, authFlags, false);
    }

    @Override
    protected boolean generateOutgoing() {
        final MessageWriter writer = new MessageWriter(response);
        writer.writeShort(0); // packet size will be filled below
        writer.writeShort(GarminMessage.RESPONSE.getId());
        writer.writeShort(garminMessage.getId());
        writer.writeByte(status.ordinal());
        writer.writeByte(authNegotiationStatus.ordinal());
        writer.writeByte(unknown);
        writer.writeInt((int) EnumUtils.generateBitVector(AuthNegotiationMessage.AuthFlags.class, authFlags));

        return sendOutgoing;
    }

    public enum AuthNegotiationStatus {
        GUESS_OK,
        GUESS_KO,
        ;

        @Nullable
        public static AuthNegotiationStatus fromCode(final int code) {
            for (final AuthNegotiationStatus authNegotiationStatus : AuthNegotiationStatus.values()) {
                if (authNegotiationStatus.ordinal() == code) {
                    return authNegotiationStatus;
                }
            }
            return null;
        }
    }
}
