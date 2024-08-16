package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


public class RecordHeader {
    private final boolean definition;
    private final boolean developerData;
    private final int localMessageType;
    private final Integer timeOffset;

    public RecordHeader(boolean definition, boolean developerData, int localMessageType, Integer timeOffset) {
        this.definition = definition;
        this.developerData = developerData;
        this.localMessageType = localMessageType;
        this.timeOffset = timeOffset;
    }

    //see https://github.com/polyvertex/fitdecode/blob/master/fitdecode/reader.py#L512
    public RecordHeader(byte header) {
        if ((header & 0x80) == 0x80) { //compressed timestamp
            definition = false;
            developerData = false;
            localMessageType = (header >> 5) & 0x3;
            timeOffset = header & 0x1f;
        } else {
            definition = ((header & 0x40) == 0x40);
            developerData = ((header & 0x20) == 0x20);
            localMessageType = header & 0xf;
            timeOffset = null;
        }
    }

    public int getLocalMessageType() {
        return localMessageType;
    }

    @Nullable
    public Integer getTimeOffset() {
        return timeOffset;
    }

    public boolean isCompressedTimestamp() {
        return timeOffset != null;
    }

    public boolean isDeveloperData() {
        return developerData;
    }

    public boolean isDefinition() {
        return definition;
    }

    public byte generateOutgoingDefinitionPayload() {
        if (!definition && !developerData) {
            assert timeOffset != null;
            return (byte) (timeOffset | (((byte) localMessageType) << 5));
        }
        byte base = (byte) localMessageType;
        if (definition)
            base = (byte) (base | 0x40);
        if (developerData)
            base = (byte) (base | 0x20);

        return base;
    }

    public byte generateOutgoingDataPayload() { //TODO: unclear if correct
        if (!definition && !developerData) {
            if (timeOffset != null)
            return (byte) (timeOffset | (((byte) localMessageType) << 5));
        }
        byte base = (byte) localMessageType;
        if (developerData)
            base = (byte) (base | 0x20);

        return base;
    }

    @NonNull
    @Override
    public String toString() {
        return "Local Message: " + localMessageType;
    }
}
