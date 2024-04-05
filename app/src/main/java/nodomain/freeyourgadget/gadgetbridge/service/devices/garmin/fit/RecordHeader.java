package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit;

import androidx.annotation.Nullable;


public class RecordHeader {
    private final boolean definition;
    private final boolean developerData;
    private final LocalMessage localMessage;
    private final int rawLocalMessageType;
    private final Integer timeOffset;
    private long referenceTimestamp;

    public RecordHeader(boolean definition, boolean developerData, LocalMessage localMessage, Integer timeOffset) {
        this.definition = definition;
        this.developerData = developerData;
        this.localMessage = localMessage;
        this.rawLocalMessageType = localMessage.getType();
        this.timeOffset = timeOffset;
    }

    //see https://github.com/polyvertex/fitdecode/blob/master/fitdecode/reader.py#L512
    public RecordHeader(byte header) {
        if ((header & 0x80) == 0x80) { //compressed timestamp TODO add support
            definition = false;
            developerData = false;
            rawLocalMessageType = (header >> 5) & 0x3;
            timeOffset = header & 0x1f;
        } else {
            definition = ((header & 0x40) == 0x40);
            developerData = ((header & 0x20) == 0x20);
            rawLocalMessageType = header & 0xf;
            timeOffset = null;
        }
        localMessage = LocalMessage.fromType(rawLocalMessageType);
    }

    public void setReferenceTimestamp(long referenceTimestamp) {
        this.referenceTimestamp = referenceTimestamp;
    }

    public Integer getTimeOffset() {
        return timeOffset;
    }

    public boolean isCompressedTimestamp() {
        return timeOffset != null;
    }

    public Long getResultingTimestamp() {
        return referenceTimestamp + timeOffset;
    }

    public boolean isDeveloperData() {
        return developerData;
    }

    public boolean isDefinition() {
        return definition;
    }

    @Nullable
    public LocalMessage getLocalMessage() {
        return localMessage;
    }

    public byte generateOutgoingDefinitionPayload() {
        if (!definition && !developerData)
            return (byte) (timeOffset | (((byte) localMessage.getType()) << 5));
        byte base = (byte) (null == localMessage ? rawLocalMessageType : localMessage.getType());
        if (definition)
            base = (byte) (base | 0x40);
        if (developerData)
            base = (byte) (base | 0x20);

        return base;
    }

    public byte generateOutgoingDataPayload() { //TODO: unclear if correct
        if (!definition && !developerData)
            return (byte) (timeOffset | (((byte) localMessage.getType()) << 5));
        byte base = (byte) (null == localMessage ? rawLocalMessageType : localMessage.getType());
        if (developerData)
            base = (byte) (base | 0x20);

        return base;
    }

    public String toString() {
        return "Local Message: " + (null == localMessage ? "raw: " + rawLocalMessageType : "type: " + localMessage.name());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RecordHeader that = (RecordHeader) o;

        if (rawLocalMessageType != that.rawLocalMessageType) return false;
        return localMessage == that.localMessage;
    }

    @Override
    public int hashCode() {
        int result = (localMessage != null ? localMessage.hashCode() : 0);
        result = 31 * result + rawLocalMessageType;
        return result;
    }
}
