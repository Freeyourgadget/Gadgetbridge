package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages;

import nodomain.freeyourgadget.gadgetbridge.devices.vivomovehr.VivomoveConstants;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.BinaryUtils;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.ChecksumCalculator;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.GarminDeviceSetting;

import java.util.Map;

public class SetDeviceSettingsMessage {
    public final byte[] packet;

    public SetDeviceSettingsMessage(Map<GarminDeviceSetting, Object> settings) {
        final int settingsCount = settings.size();
        if (settingsCount == 0) throw new IllegalArgumentException("Empty settings");
        if (settingsCount > 255) throw new IllegalArgumentException("Too many settings");

        final MessageWriter writer = new MessageWriter();
        writer.writeShort(0); // packet size will be filled below
        writer.writeShort(VivomoveConstants.MESSAGE_DEVICE_SETTINGS);
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
        writer.writeShort(0); // CRC will be filled below
        final byte[] packet = writer.getBytes();
        BinaryUtils.writeShort(packet, 0, packet.length);
        BinaryUtils.writeShort(packet, packet.length - 2, ChecksumCalculator.computeCrc(packet, 0, packet.length - 2));
        this.packet = packet;
    }
}
