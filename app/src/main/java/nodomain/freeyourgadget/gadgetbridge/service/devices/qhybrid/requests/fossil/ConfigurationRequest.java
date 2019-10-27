package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil;

import android.graphics.Bitmap;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;

public class ConfigurationRequest extends FilePutRequest {
    private static HashMap<Byte, Class<? extends ConfigItem>> itemsById;

    static {
        itemsById.put((byte)3, DailyStepGoalConfigItem.class);
        itemsById.put((byte)10, VibrationStrengthConfigItem.class);
        itemsById.put((byte)2, CurrentStepCountConfigItem.class);
        itemsById.put((byte)3, DailyStepGoalConfigItem.class);
        itemsById.put((byte)12, TimeConfigItem.class);
    }

    static ConfigItem parsePayload(byte id, byte[] data) throws InstantiationException, IllegalAccessException {
        Class<? extends ConfigItem> itemClass = itemsById.get(id);

        ConfigItem item = itemClass.newInstance();

        item.parseData(data);

        return item;
    }

    public ConfigurationRequest(ConfigItem item, FossilWatchAdapter adapter) {
        super((short) 0x0800, createFileContent(item), adapter);
    }

    public static byte[] createFileContent(ConfigItem item) {
        ByteBuffer buffer = ByteBuffer.allocate(item.getItemSize() + 3);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putShort(item.getId());
        buffer.put((byte) item.getItemSize());
        buffer.put(item.getContent());

        return buffer.array();
    }

    public interface ConfigItem {
        public int getItemSize();

        public short getId();

        public byte[] getContent();

        public void parseData(byte[] data);
    }

    static public class GenericConfigItem<T> implements ConfigItem {
        T value;
        short configId;

        public GenericConfigItem(short configId, T value) {
            this.value = value;
            this.configId = configId;
        }

        @Override
        public int getItemSize() {
            switch (value.getClass().getName()) {
                case "Byte":
                    return 1;
                case "Integer":
                    return 4;
                case "Long":
                    return 8;
            }
            throw new UnsupportedOperationException("config type " + value.getClass().getName() + " not supported");
        }

        @Override
        public short getId() {
            return 0;
        }

        @Override
        public byte[] getContent() {
            ByteBuffer buffer = ByteBuffer.allocate(getItemSize());
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            switch (value.getClass().getName()) {
                case "Byte": {
                    buffer.put((Byte) this.value);
                    break;
                }
                case "Integer": {
                    buffer.putInt((Integer) this.value);
                    break;
                }
                case "Long": {
                    buffer.putLong((Long) this.value);
                    break;
                }
            }
            return buffer.array();
        }
    }

    static public class DailyStepGoalConfigItem extends GenericConfigItem<Integer> {
        public DailyStepGoalConfigItem(int value) {
            super((short) 3, value);
        }
    }

    static public class VibrationStrengthConfigItem extends GenericConfigItem<Byte> {
        public VibrationStrengthConfigItem(Byte value) {
            super((short) 10, value);
        }
    }

    static public class CurrentStepCountConfigItem extends GenericConfigItem<Integer> {
        public CurrentStepCountConfigItem(Integer value) {
            super((short) 2, value);
        }
    }

    static public class TimeConfigItem implements ConfigItem {
        private int epochSeconds;
        private short millis, offsetMinutes;

        public TimeConfigItem(int epochSeconds, short millis, short offsetMinutes) {
            this.epochSeconds = epochSeconds;
            this.millis = millis;
            this.offsetMinutes = offsetMinutes;
        }

        @Override
        public int getItemSize() {
            return 8;
        }

        @Override
        public short getId() {
            return (short) 12;
        }

        @Override
        public byte[] getContent() {
            ByteBuffer buffer = ByteBuffer.allocate(getItemSize());
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.putInt(this.epochSeconds);
            buffer.putShort(millis);
            buffer.putShort(offsetMinutes);
            return buffer.array();
        }
    }
}

