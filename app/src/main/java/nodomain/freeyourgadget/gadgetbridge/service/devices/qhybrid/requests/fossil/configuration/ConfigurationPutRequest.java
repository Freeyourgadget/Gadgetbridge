package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.configuration;

import android.graphics.Bitmap;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FileCloseAndPutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FilePutRequest;

public class ConfigurationPutRequest extends FilePutRequest {
    private static HashMap<Short, Class<? extends ConfigItem>> itemsById = new HashMap<>();

    static {
        itemsById.put((short)3, DailyStepGoalConfigItem.class);
        itemsById.put((short)10, VibrationStrengthConfigItem.class);
        itemsById.put((short)2, CurrentStepCountConfigItem.class);
        itemsById.put((short)3, DailyStepGoalConfigItem.class);
        itemsById.put((short)12, TimeConfigItem.class);
    }

    static ConfigItem[] parsePayload(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        ArrayList<ConfigItem> configItems = new ArrayList<>();

        while(buffer.hasRemaining()){
            short id = buffer.getShort();
            byte length = buffer.get();
            byte[] payload = new byte[length];

            for(int i = 0; i < length; i++){
                payload[i] = buffer.get();
            }

            Class<? extends ConfigItem> configClass = itemsById.get(id);
            if(configClass == null){
                continue;
            }
            ConfigItem item = null;
            try {
                item = configClass.newInstance();
            } catch (IllegalAccessException | InstantiationException e) {
                e.printStackTrace();
                continue;
            }

            item.parseData(payload);

            configItems.add(item);
        }

        return configItems.toArray(new ConfigItem[0]);
    }

    public ConfigurationPutRequest(ConfigItem item, FossilWatchAdapter adapter) {
        super((short) 0x0800, createFileContent(new ConfigItem[]{item}), adapter);
    }

    public ConfigurationPutRequest(ConfigItem[] items, FossilWatchAdapter adapter) {
        super((short) 0x0800, createFileContent(items), adapter);
    }

    private static byte[] createFileContent(ConfigItem[] items) {
        int overallSize = 0;
        for(ConfigItem item : items){
            overallSize += item.getItemSize() + 3;
        }
        ByteBuffer buffer = ByteBuffer.allocate(overallSize);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        for(ConfigItem item : items){
            buffer.putShort(item.getId());
            buffer.put((byte) item.getItemSize());
            buffer.put(item.getContent());
        }

        return buffer.array();
    }

    public static abstract class ConfigItem {
        public abstract int getItemSize();

        public abstract short getId();

        public abstract byte[] getContent();

        public abstract void parseData(byte[] data);
    }

    static public class GenericConfigItem<T> extends ConfigItem {
        private T value;
        private short configId;

        public GenericConfigItem(short configId, T value) {
            this.value = value;
            this.configId = configId;
        }

        public T getValue(){
            return value;
        }

        @Override
        public int getItemSize() {
            switch (value.getClass().getName()) {
                case "java.lang.Byte":
                    return 1;
                case "java.lang.Short":
                    return 2;
                case "java.lang.Integer":
                    return 4;
                case "java.lang.Long":
                    return 8;
            }
            throw new UnsupportedOperationException("config type " + value.getClass().getName() + " not supported");
        }

        @Override
        public short getId() {
            return this.configId;
        }

        @Override
        public byte[] getContent() {
            ByteBuffer buffer = ByteBuffer.allocate(getItemSize());
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            switch (value.getClass().getName()) {
                case "java.lang.Byte": {
                    buffer.put((Byte) this.value);
                    break;
                }
                case "java.lang.Integer": {
                    buffer.putInt((Integer) this.value);
                    break;
                }
                case "java.lang.Long": {
                    buffer.putLong((Long) this.value);
                    break;
                }
                case "java.lang.Short": {
                    buffer.putShort((Short) this.value);
                    break;
                }
            }
            return buffer.array();
        }

        @Override
        public void parseData(byte[] data) {
            ByteBuffer buffer = ByteBuffer.wrap(data);
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            switch (data.length){
                case 1:{
                    this.value = (T) (Byte) buffer.get();
                    break;
                }
                case 2:{
                    this.value = (T) (Short) buffer.getShort();
                    break;
                }
                case 4:{
                    this.value = (T) (Integer) buffer.getInt();
                    break;
                }
                case 8:{
                    this.value = (T) (Long) buffer.getLong();
                    break;
                }
            }
        }
    }

    static public class DailyStepGoalConfigItem extends GenericConfigItem<Integer> {
        public DailyStepGoalConfigItem(){
            this(-1);
        }

        public DailyStepGoalConfigItem(int value) {
            super((short) 3, value);
        }
    }

    static public class TimezoneOffsetConfigItem extends GenericConfigItem<Short> {
        public TimezoneOffsetConfigItem(Short value) {
            super((short) 17, value);
        }
    }

    static public class VibrationStrengthConfigItem extends GenericConfigItem<Byte> {
        public VibrationStrengthConfigItem(){
            this((byte) -1);
        }

        public VibrationStrengthConfigItem(Byte value) {
            super((short) 10, value);
        }
    }

    static public class CurrentStepCountConfigItem extends GenericConfigItem<Integer> {
        public CurrentStepCountConfigItem(){
            this(-1);
        }

        public CurrentStepCountConfigItem(Integer value) {
            super((short) 2, value);
        }
    }

    static public class TimeConfigItem extends ConfigItem {
        private int epochSeconds;
        private short millis, offsetMinutes;

        public TimeConfigItem(){
            this(-1, (short) -1, (short) -1);
        }

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

        @Override
        public void parseData(byte[] data) {
            if(data.length != 8) throw new RuntimeException("wrong data");

            ByteBuffer buffer = ByteBuffer.wrap(data);
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            this.epochSeconds = buffer.getInt();
            this.millis = buffer.getShort();
            this.offsetMinutes = buffer.getShort();
        }
    }
}

