package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages;

import nodomain.freeyourgadget.gadgetbridge.devices.vivomovehr.VivomoveConstants;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.ChecksumCalculator;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.ancs.AncsCategory;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.ancs.AncsEvent;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.ancs.AncsEventFlag;
import org.apache.commons.lang3.EnumUtils;

import java.util.Set;

public class GncsNotificationSourceMessage {
    public final byte[] packet;

    public GncsNotificationSourceMessage(AncsEvent event, Set<AncsEventFlag> eventFlags, AncsCategory category, int categoryCount, int notificationUID) {
        final MessageWriter writer = new MessageWriter(15);
        writer.writeShort(0); // packet size will be filled below
        writer.writeShort(VivomoveConstants.MESSAGE_NOTIFICATION_SOURCE);

        writer.writeByte(event.ordinal());
        writer.writeByte(eventFlags == null ? 26 : ((int) EnumUtils.generateBitVector(AncsEventFlag.class, eventFlags)));
        writer.writeByte(category.ordinal());
        writer.writeByte(Math.min(categoryCount, 127));
        writer.writeInt(notificationUID);
        // TODO: Extra flags?
        writer.writeByte(3);

        writer.writeShort(0); // CRC will be filled below
        final byte[] packet = writer.getBytes();
        BLETypeConversions.writeUint16(packet, 0, packet.length);
        BLETypeConversions.writeUint16(packet, packet.length - 2, ChecksumCalculator.computeCrc(packet, 0, packet.length - 2));
        this.packet = packet;
    }
}
