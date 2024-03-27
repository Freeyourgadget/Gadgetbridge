package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages;

public class SystemEventMessage extends GFDIMessage {

    private final GarminSystemEventType eventType;
    private final Object value;

    public SystemEventMessage(GarminSystemEventType eventType, Object value) {
        this.eventType = eventType;
        this.value = value;
        this.garminMessage = GarminMessage.SYSTEM_EVENT;
    }

    @Override
    protected boolean generateOutgoing() {
        final MessageWriter writer = new MessageWriter(response);
        writer.writeShort(0); // packet size will be filled below
        writer.writeShort(this.garminMessage.getId());
        writer.writeByte(eventType.ordinal());
        if (value instanceof String) {
            writer.writeString((String) value);
        } else if (value instanceof Integer) {
            writer.writeByte((Integer) value);
        } else {
            throw new IllegalArgumentException("Unsupported event value type " + value);
        }
        return true;
    }

    public enum GarminSystemEventType {
        SYNC_COMPLETE,
        SYNC_FAIL,
        FACTORY_RESET,
        PAIR_START,
        PAIR_COMPLETE,
        PAIR_FAIL,
        HOST_DID_ENTER_FOREGROUND,
        HOST_DID_ENTER_BACKGROUND,
        SYNC_READY,
        NEW_DOWNLOAD_AVAILABLE,
        DEVICE_SOFTWARE_UPDATE,
        DEVICE_DISCONNECT,
        TUTORIAL_COMPLETE,
        SETUP_WIZARD_START,
        SETUP_WIZARD_COMPLETE,
        SETUP_WIZARD_SKIPPED,
        TIME_UPDATED
    }
}
