/*  Copyright (C) 2019-2021 Daniel Dakhno, Taavi Eomäe

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.configuration;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.file.FileHandle;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FilePutRequest;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class ConfigurationPutRequest extends FilePutRequest {
    private static final HashMap<Short, Class<? extends ConfigItem>> itemsById = new HashMap<>();

    static {
        itemsById.put((short) 0x02, CurrentStepCountConfigItem.class);
        itemsById.put((short) 0x03, DailyStepGoalConfigItem.class);
        itemsById.put((short) 0x09, InactivityWarningItem.class);
        itemsById.put((short) 0x0A, VibrationStrengthConfigItem.class);
        itemsById.put((short) 0x0C, TimeConfigItem.class);
        itemsById.put((short) 0x0D, BatteryConfigItem.class);
        itemsById.put((short) 0x0E, HeartRateMeasurementModeItem.class);
        itemsById.put((short) 0x10, UnitsConfigItem.class);
        itemsById.put((short) 0x14, FitnessConfigItem.class);
    }

    public static ConfigItem[] parsePayload(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        ArrayList<ConfigItem> configItems = new ArrayList<>();

        while (buffer.hasRemaining()) {
            short id = buffer.getShort();
            byte length = buffer.get();
            byte[] payload = new byte[length];

            for (int i = 0; i < length; i++) {
                payload[i] = buffer.get();
            }

            Class<? extends ConfigItem> configClass = itemsById.get(id);
            if (configClass == null) {
                continue;
            }

            ConfigItem item;
            try {
                item = configClass.newInstance();
            } catch (IllegalAccessException | InstantiationException e) {
                GB.log("error", GB.ERROR, e);
                continue;
            }
            item.parseData(payload);

            configItems.add(item);
        }

        return configItems.toArray(new ConfigItem[0]);
    }

    public ConfigurationPutRequest(ConfigItem item, FossilWatchAdapter adapter) {
        super(FileHandle.CONFIGURATION, createFileContent(new ConfigItem[]{item}), adapter);
    }

    public ConfigurationPutRequest(ConfigItem[] items, FossilWatchAdapter adapter) {
        super(FileHandle.CONFIGURATION, createFileContent(items), adapter);
    }

    private static byte[] createFileContent(ConfigItem[] items) {
        int overallSize = 0;
        for (ConfigItem item : items) {
            overallSize += item.getItemSize() + 3;
        }
        ByteBuffer buffer = ByteBuffer.allocate(overallSize);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        for (ConfigItem item : items) {
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
        private final short configId;

        public GenericConfigItem(short configId, T value) {
            this.value = value;
            this.configId = configId;
        }

        public T getValue() {
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

            switch (data.length) {
                case 1: {
                    this.value = (T) (Byte) buffer.get();
                    break;
                }
                case 2: {
                    this.value = (T) (Short) buffer.getShort();
                    break;
                }
                case 4: {
                    this.value = (T) (Integer) buffer.getInt();
                    break;
                }
                case 8: {
                    this.value = (T) (Long) buffer.getLong();
                    break;
                }
            }
        }
    }

    static public class BatteryConfigItem extends ConfigItem {
        private int batteryPercentage, batteryVoltage;

        public int getBatteryPercentage() {
            return batteryPercentage;
        }

        public int getBatteryVoltage() {
            return batteryVoltage;
        }

        @Override
        public int getItemSize() {
            return 3;
        }

        @Override
        public short getId() {
            return 0x0D;
        }

        @Override
        public byte[] getContent() {
            return new byte[0];
        }

        @Override
        public void parseData(byte[] data) {
            ByteBuffer buffer = ByteBuffer.wrap(data);
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            this.batteryVoltage = buffer.getShort();
            this.batteryPercentage = buffer.get();
        }
    }

    static public class HeartRateMeasurementModeItem extends GenericConfigItem<Byte> {
        public HeartRateMeasurementModeItem() {
            this((byte) -1);
        }

        public HeartRateMeasurementModeItem(byte value) {
            super((short) 14, value);
        }
    }

    static public class DailyStepGoalConfigItem extends GenericConfigItem<Integer> {
        public DailyStepGoalConfigItem() {
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
        public VibrationStrengthConfigItem() {
            this((byte) -1);
        }

        public VibrationStrengthConfigItem(Byte value) {
            super((short) 10, value);
        }
    }

    static public class CurrentStepCountConfigItem extends GenericConfigItem<Integer> {
        public CurrentStepCountConfigItem() {
            this(-1);
        }

        public CurrentStepCountConfigItem(Integer value) {
            super((short) 2, value);
        }
    }

    static public class UnitsConfigItem extends GenericConfigItem<Integer> {
        public UnitsConfigItem() {
            this(-1);
        }

        public UnitsConfigItem(Integer value) {
            super((short) 16, value);
        }
    }

    static public class TimeConfigItem extends ConfigItem {
        private int epochSeconds;
        private short millis, offsetMinutes;

        public TimeConfigItem() {
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
            if (data.length != 8) throw new RuntimeException("wrong data");

            ByteBuffer buffer = ByteBuffer.wrap(data);
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            this.epochSeconds = buffer.getInt();
            this.millis = buffer.getShort();
            this.offsetMinutes = buffer.getShort();
        }
    }


    static public class FitnessConfigItem extends ConfigItem {
        boolean recognizeRunning = false;
        boolean askRunning = false;
        int minutesRunning = 3;

        boolean recognizeBiking = false;
        boolean askBiking = false;
        int minutesBiking = 5;

        boolean recognizeWalking = false;
        boolean askWalking = false;
        int minutesWalking = 10;

        boolean recognizeRowing = false;
        boolean askRowing = false;
        int minutesRowing = 3;

        public FitnessConfigItem(boolean recognizeRunning,
                                 boolean askRunning,
                                 int minutesRunning,
                                 boolean recognizeBiking,
                                 boolean askBiking,
                                 int minutesBiking,
                                 boolean recognizeWalking,
                                 boolean askWalking,
                                 int minutesWalking,
                                 boolean recognizeRowing,
                                 boolean askRowing,
                                 int minutesRowing) {
            this.recognizeRunning = recognizeRunning;
            this.askRunning = askRunning;
            this.minutesRunning = minutesRunning;
            this.recognizeBiking = recognizeBiking;
            this.askBiking = askBiking;
            this.minutesBiking = minutesBiking;
            this.recognizeWalking = recognizeWalking;
            this.askWalking = askWalking;
            this.minutesWalking = minutesWalking;
            this.recognizeRowing = recognizeRowing;
            this.askRowing = askRowing;
            this.minutesRowing = minutesRowing;
        }

        public FitnessConfigItem() {
        }

        @Override
        public int getItemSize() {
            return 30;
        }

        @Override
        public short getId() {
            return 0x14;
        }

        @Override
        public byte[] getContent() {
            byte[] data = new byte[]{
                    // 2nd byte of each workout type is activation mode
                    // 3rd byte of each is amount of minutes before asking/activating
                    // Running
                    (byte) 0x01, (byte) 0x00, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x05,
                    // Biking
                    (byte) 0x02, (byte) 0x00, (byte) 0x05, (byte) 0x01, (byte) 0x01, (byte) 0x01,
                    // Walking
                    (byte) 0x08, (byte) 0x00, (byte) 0x0A, (byte) 0x01, (byte) 0x01, (byte) 0x05,
                    // Rowing
                    (byte) 0x09, (byte) 0x00, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x01
            };
            if (recognizeRunning) {
                data[1] |= 0x01;
                if (askRunning) {
                    data[1] |= 0x02;
                }
                data[2] = (byte) (minutesRunning & 0xFF);
            }
            if (recognizeBiking) {
                data[7] |= 0x01;
                if (askBiking) {
                    data[7] |= 0x02;
                }
                data[8] = (byte) (minutesBiking & 0xFF);
            }
            if (recognizeWalking) {
                data[13] |= 0x01;
                if (askWalking) {
                    data[13] |= 0x02;
                }
                data[14] = (byte) (minutesWalking & 0xFF);
            }
            if (recognizeRowing) {
                data[19] |= 0x01;
                if (askRowing) {
                    data[19] |= 0x02;
                }
                data[20] = (byte) (minutesRowing & 0xFF);
            }
            return data;
        }

        @Override
        public void parseData(byte[] data) {
            recognizeRunning = (data[1] & 0x01) == 0x01;
            askRunning = (data[1] & 0x02) == 0x02;
            minutesRunning = data[2] & 0xFF;

            recognizeBiking = (data[7] & 0x01) == 0x01;
            askBiking = (data[7] & 0x02) == 0x02;
            minutesBiking = data[8] & 0xFF;

            recognizeWalking = (data[13] & 0x01) == 0x01;
            askWalking = (data[13] & 0x02) == 0x02;
            minutesWalking = data[14] & 0xFF;

            recognizeRowing = (data[19] & 0x01) == 0x01;
            askRowing = (data[19] & 0x02) == 0x02;
            minutesRowing = data[20] & 0xFF;
        }

        @NonNull
        @Override
        public String toString() {
            return
                    "recognizeRunning: " + recognizeRunning + "   askRunning: " + askRunning + "  minutesRunning: " + minutesRunning + "\n" +
                            "recognizeBiking: " + recognizeBiking + "   askBiking: " + askBiking + "  minutesBiking: " + minutesBiking + "\n" +
                            "recognizeWalking: " + recognizeWalking + "   askWalking: " + askWalking + "  minutesWalking: " + minutesWalking + "\n" +
                            "recognizeRowing: " + recognizeRowing + "   askRowing: " + askRowing + "  minutesRowing: " + minutesRowing;
        }
    }

    static public class InactivityWarningItem extends ConfigItem {
        private int fromTimeHour, fromTimeMinute, untilTimeHour, untilTimeMinute, inactiveMinutes;
        boolean enabled;

        public InactivityWarningItem() {
            this(0, 0, 0, 0, 0, false);
        }

        public InactivityWarningItem(int fromTimeHour, int fromTimeMinute, int untilTimeHour, int untilTimeMinute, int inactiveMinutes, boolean enabled) {
            this.fromTimeHour = fromTimeHour;
            this.fromTimeMinute = fromTimeMinute;
            this.untilTimeHour = untilTimeHour;
            this.untilTimeMinute = untilTimeMinute;
            this.inactiveMinutes = inactiveMinutes;
            this.enabled = enabled;
        }

        @Override
        public int getItemSize() {
            return 6;
        }

        @Override
        public short getId() {
            return (short) 9;
        }

        @Override
        public byte[] getContent() {
            ByteBuffer buffer = ByteBuffer.allocate(getItemSize());
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.put((byte) this.fromTimeHour);
            buffer.put((byte) this.fromTimeMinute);
            buffer.put((byte) this.untilTimeHour);
            buffer.put((byte) this.untilTimeMinute);
            buffer.put((byte) this.inactiveMinutes);
            buffer.put((byte) (this.enabled ? 1 : 0));
            return buffer.array();
        }

        @Override
        public void parseData(byte[] data) {
            if (data.length != getItemSize()) throw new RuntimeException("wrong data");

            ByteBuffer buffer = ByteBuffer.wrap(data);
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            this.fromTimeHour = buffer.get();
            this.fromTimeMinute = buffer.get();
            this.untilTimeHour = buffer.get();
            this.untilTimeMinute = buffer.get();
            this.inactiveMinutes = buffer.get();
            this.enabled = buffer.get() == 0x01;
        }
    }

}

