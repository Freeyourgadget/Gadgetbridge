package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status;

import androidx.annotation.Nullable;

public class FitDataStatusMessage extends GFDIStatusMessage {

    private final Status status;
    private final FitDataStatusCode fitDataStatusCode;

    public FitDataStatusMessage(GarminMessage garminMessage, Status status, FitDataStatusCode fitDataStatusCode) {
        this.garminMessage = garminMessage;
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

    public static FitDataStatusMessage parseIncoming(MessageReader reader, GarminMessage garminMessage) {
        final Status status = Status.fromCode(reader.readByte());
        final int fitDataStatusCodeByte = reader.readByte();
        final FitDataStatusCode fitDataStatusCode = FitDataStatusCode.fromCode(fitDataStatusCodeByte);
        if (fitDataStatusCode == null) {
            LOG.warn("Unknown fit data status code {}", fitDataStatusCodeByte);
            return null;
        }
        return new FitDataStatusMessage(garminMessage, status, fitDataStatusCode);
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
