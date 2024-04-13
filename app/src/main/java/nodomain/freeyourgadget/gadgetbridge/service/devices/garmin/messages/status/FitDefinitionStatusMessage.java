package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status;

import androidx.annotation.Nullable;

public class FitDefinitionStatusMessage extends GFDIStatusMessage {

    private final Status status;
    private final FitDefinitionStatusCode fitDefinitionStatusCode;

    public FitDefinitionStatusMessage(GarminMessage garminMessage, Status status, FitDefinitionStatusCode fitDefinitionStatusCode) {
        this.garminMessage = garminMessage;
        this.status = status;
        this.fitDefinitionStatusCode = fitDefinitionStatusCode;
        switch (fitDefinitionStatusCode) {
            case APPLIED:
                LOG.info("FIT DEFINITION RETURNED STATUS: {}", fitDefinitionStatusCode.name());
                break;
            default:
                LOG.warn("FIT DEFINITION RETURNED STATUS: {}", fitDefinitionStatusCode.name());
        }
    }

    public static FitDefinitionStatusMessage parseIncoming(MessageReader reader, GarminMessage garminMessage) {
        final Status status = Status.fromCode(reader.readByte());
        final int fitDefinitionStatusCodeByte = reader.readByte();
        final FitDefinitionStatusCode fitDefinitionStatusCode = FitDefinitionStatusCode.fromCode(fitDefinitionStatusCodeByte);
        if (fitDefinitionStatusCode == null) {
            LOG.warn("Unknown fit definition status code {}", fitDefinitionStatusCodeByte);
            return null;
        }
        return new FitDefinitionStatusMessage(garminMessage, status, fitDefinitionStatusCode);
    }

    public enum FitDefinitionStatusCode {
        APPLIED,
        NOT_UNIQUE,
        OUT_OF_RANGE,
        NOT_READY,
        ;

        @Nullable
        public static FitDefinitionStatusCode fromCode(final int code) {
            for (final FitDefinitionStatusCode fitDefinitionStatusCode : FitDefinitionStatusCode.values()) {
                if (fitDefinitionStatusCode.ordinal() == code) {
                    return fitDefinitionStatusCode;
                }
            }
            return null;
        }
    }
}
