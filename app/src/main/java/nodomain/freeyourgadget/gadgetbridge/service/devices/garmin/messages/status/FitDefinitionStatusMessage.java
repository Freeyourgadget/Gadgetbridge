package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status;

import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.MessageReader;

public class FitDefinitionStatusMessage extends GFDIStatusMessage {

    private final Status status;
    private final FitDefinitionStatusCode fitDefinitionStatusCode;
    private final int messageType;

    public FitDefinitionStatusMessage(int messageType, Status status, FitDefinitionStatusCode fitDefinitionStatusCode) {
        this.messageType = messageType;
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

    public static FitDefinitionStatusMessage parseIncoming(MessageReader reader, int messageType) {
        final Status status = Status.fromCode(reader.readByte());
        final FitDefinitionStatusCode fitDefinitionStatusCode = FitDefinitionStatusCode.fromCode(reader.readByte());

        reader.warnIfLeftover();
        return new FitDefinitionStatusMessage(messageType, status, fitDefinitionStatusCode);
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
