package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status;

import java.util.EnumSet;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.SetFileFlagsMessage;

public class SetFileFlagsStatusMessage extends GFDIStatusMessage {
    private final Status status;
    private final FlagsStatus flagsStatus;
    private final int fileIdentifier;
    private final EnumSet<SetFileFlagsMessage.FileFlags> fileFlags;

    public SetFileFlagsStatusMessage(GarminMessage garminMessage, Status status, FlagsStatus flagsStatus, int fileIdentifier, EnumSet<SetFileFlagsMessage.FileFlags> fileFlags) {
        this.garminMessage = garminMessage;
        this.status = status;
        this.flagsStatus = flagsStatus;
        this.fileIdentifier = fileIdentifier;
        this.fileFlags = fileFlags;
    }

    public static SetFileFlagsStatusMessage parseIncoming(MessageReader reader, GarminMessage garminMessage) {
        final Status status = Status.fromCode(reader.readByte());

        if (!status.equals(Status.ACK)) {
            return null;
        }

        final FlagsStatus flagsStatus = FlagsStatus.fromCode(reader.readByte());
        final int originalFileIdentifier = reader.readShort() + 1; //TODO: check if always or only on archival
        final EnumSet<SetFileFlagsMessage.FileFlags> fileFlags = SetFileFlagsMessage.FileFlags.fromBitMask(reader.readByte());

        if (!FlagsStatus.APPLIED.equals(flagsStatus)) {
            LOG.warn("Received {} / {} for file identifier {} and flags {} - message {}", status, flagsStatus, originalFileIdentifier, fileFlags, garminMessage);
        } else {
            LOG.info("Received {} / {} for file identifier {} and flags {} - message {}", status, flagsStatus, originalFileIdentifier, fileFlags, garminMessage);
        }

        return new SetFileFlagsStatusMessage(garminMessage, status, flagsStatus, originalFileIdentifier, fileFlags);

    }


    enum FlagsStatus {
        APPLIED,
        ERROR, //guessed
        ;

        public static FlagsStatus fromCode(final int code) {
            for (final FlagsStatus status : FlagsStatus.values()) {
                if (status.ordinal() == code) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Unknown FlagsStatus code " + code);
        }
    }

}
