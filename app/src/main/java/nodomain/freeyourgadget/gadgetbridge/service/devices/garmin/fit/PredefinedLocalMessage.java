package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit;

import androidx.annotation.Nullable;

import java.nio.ByteOrder;
import java.util.List;

public enum PredefinedLocalMessage {
    TODAY_WEATHER_CONDITIONS(6, GlobalFITMessage.WEATHER,
            new int[]{0, 253, 9, 1, 14, 13, 2, 3, 5, 4, 6, 7, 10, 11, 17, 15, 8}
    ),
    HOURLY_WEATHER_FORECAST(9, GlobalFITMessage.WEATHER,
            new int[]{0, 253, 1, 2, 3, 4, 5, 6, 7, 15, 16, 17}
    ),
    DAILY_WEATHER_FORECAST(10, GlobalFITMessage.WEATHER,
            new int[]{0, 253, 14, 13, 2, 5, 12, 17}
    );

    private final int type;
    private final GlobalFITMessage globalFITMessage;
    private final int[] globalDefinitionIds;

    PredefinedLocalMessage(int type, GlobalFITMessage globalFITMessage, int[] globalDefinitionIds) {
        this.type = type;
        this.globalFITMessage = globalFITMessage;
        this.globalDefinitionIds = globalDefinitionIds;
    }

    @Nullable
    public static PredefinedLocalMessage fromType(int type) {
        for (final PredefinedLocalMessage predefinedLocalMessage : PredefinedLocalMessage.values()) {
            if (predefinedLocalMessage.getType() == type) {
                return predefinedLocalMessage;
            }
        }
        return null;
    }

    public RecordDefinition getRecordDefinition() {
        final RecordHeader recordHeader = new RecordHeader(true, false, type, null);
        final List<FieldDefinition> fieldDefinitions = globalFITMessage.getFieldDefinitions(globalDefinitionIds);
        return new RecordDefinition(
                recordHeader,
                ByteOrder.BIG_ENDIAN,
                globalFITMessage,
                fieldDefinitions,
                null
        );
    }

    public int getType() {
        return type;
    }

    public GlobalFITMessage getGlobalFITMessage() {
        return globalFITMessage;
    }
}
