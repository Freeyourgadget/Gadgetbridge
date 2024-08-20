package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status;


import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.GFDIMessage;

public abstract class GFDIStatusMessage extends GFDIMessage {
    private Status status;

    public static GFDIStatusMessage parseIncoming(MessageReader reader, GarminMessage garminMessage) {
        int originalMessageType = reader.readShort();
        final GarminMessage originalGarminMessage = GFDIMessage.GarminMessage.fromId(originalMessageType);

        if (GarminMessage.PROTOBUF_REQUEST.equals(originalGarminMessage) || GarminMessage.PROTOBUF_RESPONSE.equals(originalGarminMessage)) {
            return ProtobufStatusMessage.parseIncoming(reader, originalGarminMessage);
        } else if (GarminMessage.NOTIFICATION_DATA.equals(originalGarminMessage)) {
            return NotificationDataStatusMessage.parseIncoming(reader, originalGarminMessage);
        } else if (GarminMessage.UPLOAD_REQUEST.equals(originalGarminMessage)) {
            return UploadRequestStatusMessage.parseIncoming(reader, originalGarminMessage);
        } else if (GarminMessage.DOWNLOAD_REQUEST.equals(originalGarminMessage)) {
            return DownloadRequestStatusMessage.parseIncoming(reader, originalGarminMessage);
        } else if (GarminMessage.FILE_TRANSFER_DATA.equals(originalGarminMessage)) {
            return FileTransferDataStatusMessage.parseIncoming(reader, originalGarminMessage);
        } else if (GarminMessage.CREATE_FILE.equals(originalGarminMessage)) {
            return CreateFileStatusMessage.parseIncoming(reader, originalGarminMessage);
        } else if (GarminMessage.SUPPORTED_FILE_TYPES_REQUEST.equals(originalGarminMessage)) {
            SupportedFileTypesStatusMessage supportedFileTypesStatusMessage = SupportedFileTypesStatusMessage.parseIncoming(reader, garminMessage);
            LOG.info("{}", supportedFileTypesStatusMessage);
            return supportedFileTypesStatusMessage;
        } else if (GarminMessage.SET_FILE_FLAG.equals(originalGarminMessage)) {
            return SetFileFlagsStatusMessage.parseIncoming(reader, garminMessage);
        } else if (GarminMessage.FIT_DEFINITION.equals(originalGarminMessage)) {
            return FitDefinitionStatusMessage.parseIncoming(reader, originalGarminMessage);
        } else if (GarminMessage.FIT_DATA.equals(originalGarminMessage)) {
            return FitDataStatusMessage.parseIncoming(reader, originalGarminMessage);
        } else if (GarminMessage.AUTH_NEGOTIATION.equals(originalGarminMessage)) {
            return AuthNegotiationStatusMessage.parseIncoming(reader, originalGarminMessage);
        } else {
            final Status status = Status.fromCode(reader.readByte());

            if (Status.ACK == status) {
                LOG.info("Received ACK for message {}", originalGarminMessage);
            } else {
                LOG.warn("Received {} for message {}", status, (null == originalGarminMessage) ? originalMessageType : originalGarminMessage.name());
            }

            return new GenericStatusMessage(originalGarminMessage, status, false); //don't ack the ack
        }
    }

    @Override
    protected boolean generateOutgoing() {
        return false;
    }

    protected Status getStatus() {
        return status;
    }
}
