package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit;

public class RecordHeader {
    private final boolean definition;
    private final boolean developerData;
    private final MesgType mesgType;
    private final Integer timeOffset;

    public RecordHeader(boolean definition, boolean developerData, MesgType mesgType, Integer timeOffset) {
        this.definition = definition;
        this.developerData = developerData;
        this.mesgType = mesgType;
        this.timeOffset = timeOffset;
    }

    //see https://github.com/polyvertex/fitdecode/blob/master/fitdecode/reader.py#L512
    public RecordHeader(byte header) {
        if ((header & 0x80) == 0x80) { //compressed timestamp TODO add support
            definition = false;
            developerData = false;
            mesgType = MesgType.fromIdentifier((header >> 5) & 0x3);
            timeOffset = header & 0x1f;
        } else {
            definition = ((header & 0x40) == 0x40);
            developerData = ((header & 0x20) == 0x20);
            mesgType = MesgType.fromIdentifier(header & 0xf);
            timeOffset = null;
        }
    }

    public boolean isDeveloperData() {
        return developerData;
    }

    public boolean isDefinition() {
        return definition;
    }

    public MesgType getMesgType() {
        return mesgType;
    }

    public byte generateOutgoingDefinitionPayload() {
        if (!definition && !developerData)
            return (byte) (timeOffset | (((byte) mesgType.getIdentifier()) << 5));
        byte base = (byte) mesgType.getIdentifier();
        if (definition)
            base = (byte) (base | 0x40);
        if (developerData)
            base = (byte) (base | 0x20);

        return base;
    }

    public byte generateOutgoingDataPayload() { //TODO: unclear if correct
        if (!definition && !developerData)
            return (byte) (timeOffset | (((byte) mesgType.getIdentifier()) << 5));
        byte base = (byte) mesgType.getIdentifier();
        if (developerData)
            base = (byte) (base | 0x20);

        return base;
    }
}
