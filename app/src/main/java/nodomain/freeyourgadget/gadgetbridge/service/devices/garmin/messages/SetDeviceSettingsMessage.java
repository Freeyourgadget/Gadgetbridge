package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages;

import java.util.Map;

public class SetDeviceSettingsMessage extends GFDIMessage {
    private final Map<GarminDeviceSetting, Object> settings;

    public SetDeviceSettingsMessage(Map<GarminDeviceSetting, Object> settings) {
        this.garminMessage = GarminMessage.DEVICE_SETTINGS;
        this.settings = settings;
        final int settingsCount = settings.size();
        if (settingsCount == 0) throw new IllegalArgumentException("Empty settings");
        if (settingsCount > 255) throw new IllegalArgumentException("Too many settings");

    }

    @Override
    protected boolean generateOutgoing() {
        final MessageWriter writer = new MessageWriter(response);
        writer.writeShort(0); // packet size will be filled below
        writer.writeShort(this.garminMessage.getId());
        writer.writeByte(settings.size());
        for (Map.Entry<GarminDeviceSetting, Object> settingPair : settings.entrySet()) {
            final GarminDeviceSetting setting = settingPair.getKey();
            writer.writeByte(setting.ordinal());
            final Object value = settingPair.getValue();
            if (value instanceof String) {
                writer.writeString((String) value);
            } else if (value instanceof Integer) {
                writer.writeByte(4);
                writer.writeInt((Integer) value);
            } else if (value instanceof Boolean) {
                writer.writeByte(1);
                writer.writeByte(Boolean.TRUE.equals(value) ? 1 : 0);
            } else {
                throw new IllegalArgumentException("Unsupported setting value type " + value);
            }
        }
        return true;
    }

    public enum GarminDeviceSetting {
        DEVICE_NAME,
        CURRENT_TIME,
        DAYLIGHT_SAVINGS_TIME_OFFSET,
        TIME_ZONE_OFFSET,
        NEXT_DAYLIGHT_SAVINGS_START,
        NEXT_DAYLIGHT_SAVINGS_END,
        AUTO_UPLOAD_ENABLED,
        WEATHER_CONDITIONS_ENABLED,
        WEATHER_ALERTS_ENABLED
    }
}
