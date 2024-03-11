package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages;

import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.devices.vivomovehr.GarminCapability;


public class ConfigurationMessage extends GFDIMessage {
    public final Set<GarminCapability> OUR_CAPABILITIES = GarminCapability.ALL_CAPABILITIES;
    private final byte[] incomingConfigurationPayload;
    private final int messageType;
    private final byte[] ourConfigurationPayload = GarminCapability.setToBinary(OUR_CAPABILITIES);

    public ConfigurationMessage(int messageType, byte[] configurationPayload) {
        this.messageType = messageType;
        if (configurationPayload.length > 255)
            throw new IllegalArgumentException("Too long payload");
        this.incomingConfigurationPayload = configurationPayload;

        Set<GarminCapability> capabilities = GarminCapability.setFromBinary(configurationPayload);
        LOG.info("Received configuration message; capabilities: {}", GarminCapability.setToString(capabilities));

        this.statusMessage = this.getStatusMessage(messageType);
    }

    public static ConfigurationMessage parseIncoming(MessageReader reader, int messageType) {
        final int endOfPayload = reader.readByte();
        ConfigurationMessage configurationMessage = new ConfigurationMessage(messageType, reader.readBytes(endOfPayload - reader.getPosition()));
        reader.warnIfLeftover();
        return configurationMessage;
    }

    @Override
    protected boolean generateOutgoing() {
        final MessageWriter writer = new MessageWriter(response);
        writer.writeShort(0); // placeholder for packet size
        writer.writeShort(messageType);
        writer.writeByte(ourConfigurationPayload.length);
        writer.writeBytes(ourConfigurationPayload);
        return true;
    }

}
