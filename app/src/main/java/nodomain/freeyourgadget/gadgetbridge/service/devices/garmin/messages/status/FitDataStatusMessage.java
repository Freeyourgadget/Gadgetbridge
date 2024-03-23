package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status;

import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.MessageReader;

public class FitDataStatusMessage extends GFDIStatusMessage {

    private final Status status;
    private final FitDataStatusCode fitDataStatusCode;
    private final int messageType;

    public FitDataStatusMessage(int messageType, Status status, FitDataStatusCode fitDataStatusCode) {
        this.messageType = messageType;
        this.status = status;
        this.fitDataStatusCode = fitDataStatusCode;
        switch (fitDataStatusCode) {
            case APPLIED:
                LOG.info("FIT DATA RETURNED STATUS: {}", fitDataStatusCode.name());
                break;
            default:
                LOG.warn("FIT DATA RETURNED STATUS: {}", fitDataStatusCode.name());
        }
    }

    public static FitDataStatusMessage parseIncoming(MessageReader reader, int messageType) {
        final Status status = Status.fromCode(reader.readByte());
        final FitDataStatusCode fitDataStatusCode = FitDataStatusCode.fromCode(reader.readByte());

        reader.warnIfLeftover();
        return new FitDataStatusMessage(messageType, status, fitDataStatusCode);
    }

    public enum FitDataStatusCode {
        APPLIED,
        NO_DEFINITION,
        MISMATCH,
        NOT_READY,
        ;

        @Nullable
        public static FitDataStatusCode fromCode(final int code) {
            for (final FitDataStatusCode fitDataStatusCode : FitDataStatusCode.values()) {
                if (fitDataStatusCode.ordinal() == code) {
                    return fitDataStatusCode;
                }
            }
            return null;
        }
    }
}
