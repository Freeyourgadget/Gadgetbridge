/*
Copyright (C) 2024 enoint

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
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package nodomain.freeyourgadget.gadgetbridge.devices.hama.fit6900;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public final class Message {
    private static byte encodeBoolean(boolean value) {
        return (byte) ((value) ? 1 : 0);
    }

    private static void encodeInt16(byte[] data, int offset, int value) {
        data[offset + 0] = (byte) (value & 0xFF);
        data[offset + 1] = (byte) ((value >> 8) & 0xFF);
    }

    private static void encodeInt32(byte[] data, int offset, int value) {
        data[offset + 0] = (byte) (value & 0xFF);
        data[offset + 1] = (byte) ((value >> 8) & 0xFF);
        data[offset + 2] = (byte) ((value >> 16) & 0xFF);
        data[offset + 3] = (byte) ((value >> 24) & 0xFF);
    }

    private static void encodeInt32Goal(byte[] data, int offset, int value) {
        encodeInt32(data, offset, value);
        data[offset + 3] = 0;
    }

    private static byte encodeTimeFormat(TimeFormat tf) {
        return (byte) ((tf == TimeFormat.Format12H) ? 1 : 0);
    }

    public static int decodeInt32(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 24) | ((data[offset + 1] & 0xFF) << 16) | ((data[offset + 2] & 0xFF) << 8) | (data[offset + 3] & 0xFF);
    }

    private static byte[] byteArrayAdd(byte[] b1, byte[] b2) {
        if (b2 == null || b2.length == 0)
            return b1;

        byte[] result = new byte[b1.length + b2.length];
        System.arraycopy(b1, 0, result, 0, b1.length);
        System.arraycopy(b2, 0, result, b1.length, b2.length);

        return result;
    }

    public static int calculateCrc(byte[] data) {
        int crc = 0xFF;
        for (byte b : data) {
            crc ^= b & 0xFF;
            for (int k = 0; k < 8; k++) {
                if ((crc & 1) != 0) {
                    crc = (crc >> 1) ^ 184;
                } else {
                    crc = (crc >> 1);
                }
            }
        }
        return crc;
    }

    private static final byte HEADER_MAGIC_NUMBER = (byte) 186;
    private static final byte PROTOCOL_VERSION_0 = (byte) 0;

    private static byte[] encodeMessage(byte[] commandData) {
        // msgType | 1: single message or end of multi-part message, 3: one part of a multi-part message
        byte msgType = 1;
        byte unknown1 = 0; // 1 bit
        byte unknown2 = 0; // 4 bits
        int length = commandData.length;
        int crc = calculateCrc(commandData);
        int msgCounter = 0; // returned with the response

        byte[] header = new byte[8];
        header[0] = HEADER_MAGIC_NUMBER;
        header[1] = (byte) ((msgType << 5) | (unknown1 << 4) | unknown2);
        header[2] = (byte) ((length >> 8) & 0xFF);
        header[3] = (byte) (length & 0xFF);
        header[4] = (byte) ((crc >> 8) & 0xFF);
        header[5] = (byte) (crc & 0xFF);
        header[6] = (byte) ((msgCounter >> 8) & 0xFF);
        header[7] = (byte) (msgCounter & 0xFF);

        return byteArrayAdd(header, commandData);
    }


    private static byte[] encodeCommand(byte cmd, byte cmdKey, byte[] argsData) {
        int length = (argsData != null) ? argsData.length : 0;

        byte[] header = new byte[5];
        header[0] = cmd;
        header[1] = PROTOCOL_VERSION_0;
        header[2] = cmdKey;
        header[3] = (byte) ((length >> 8) & 1);
        header[4] = (byte) (length & 0xFF);

        return byteArrayAdd(header, argsData);
    }

    public static byte[] encodeCommandMessage(int cmd, int cmdKey, byte[] cmdArgsData) {
        assert (cmd > 0);
        assert (cmd <= 0xFF);
        assert (cmdKey > 0);
        assert (cmdKey <= 0xFF);
        return encodeMessage(encodeCommand((byte) cmd, (byte) cmdKey, cmdArgsData));
    }

    public static class CommandMessage { // decoded message
        public byte msgType;

        public int cmd;
        public int key;
        public byte[] cmdArgs;
    }


    public static CommandMessage decodeCommandMessage(byte[] data) {
        final int MESSAGE_HEADER_SIZE = 8;
        final int COMMAND_HEADER_SIZE = 5;

        if (data[0] != HEADER_MAGIC_NUMBER) {
            return null;
        }

        byte messageType = (byte) (data[1] >> 5);
        int messageLength = ((data[2] & 0xFF) << 8) | (data[3] & 0xFF);
        int crcReceived = ((data[4] & 0xFF) << 8) | (data[5] & 0xFF);
        //int msgCounter = ((data[6] & 0xFF) << 8) | (data[7] & 0xFF);

        if (data.length != MESSAGE_HEADER_SIZE + messageLength) {
            return null;
        }

        {
            byte[] commandData = new byte[messageLength];
            System.arraycopy(data, MESSAGE_HEADER_SIZE, commandData, 0, messageLength);

            int crc = calculateCrc(commandData);
            if (crc != crcReceived) {
                return null;
            }
        }

        int cmd = data[MESSAGE_HEADER_SIZE + 0] & 0xFF;
        int version = data[MESSAGE_HEADER_SIZE + 1] & 0xFF;
        int key = data[MESSAGE_HEADER_SIZE + 2] & 0xFF;
        int commandLength = ((data[MESSAGE_HEADER_SIZE + 3] & 1) << 8) | (data[MESSAGE_HEADER_SIZE + 4] & 0xFF);

        if (version != PROTOCOL_VERSION_0) {
            return null;
        }

        if (data.length != MESSAGE_HEADER_SIZE + COMMAND_HEADER_SIZE + commandLength) {
            return null;
        }

        byte[] commandArgs = new byte[commandLength];
        System.arraycopy(data, MESSAGE_HEADER_SIZE + COMMAND_HEADER_SIZE, commandArgs, 0, commandLength);

        CommandMessage result = new CommandMessage();
        result.msgType = messageType;
        result.cmd = cmd;
        result.key = key;
        result.cmdArgs = commandArgs;

        return result;
    }

    public static byte[] encodeGetFirmwareVersion() {
        return encodeCommandMessage(1, 18, null);
    }

    public static byte[] encodeGetBatteryStatus() {
        return encodeCommandMessage(4, 64, null);
    }

    private static void encodeDateTime(byte[] data, int dataOffset, int year, int month, int day, int hour, int minute, int second) {
        data[dataOffset + 0] = (byte) ((year % 100) & 0xFF);
        data[dataOffset + 1] = (byte) (month & 0xFF);
        data[dataOffset + 2] = (byte) (day & 0xFF);
        data[dataOffset + 3] = (byte) (hour & 0xFF);
        data[dataOffset + 4] = (byte) (minute & 0xFF);
        data[dataOffset + 5] = (byte) (second & 0xFF);
    }

    public enum TimeFormat {
        Format12H,
        Format24H
    }

    public static byte[] encodeSetDateTime(Calendar dt, TimeFormat timeFormat) {
        byte[] args = new byte[7];
        encodeDateTime(args, 0, dt.get(Calendar.YEAR), dt.get(Calendar.MONTH) + 1,
                dt.get(Calendar.DAY_OF_MONTH), dt.get(Calendar.HOUR_OF_DAY), dt.get(Calendar.MINUTE), dt.get(Calendar.SECOND));
        // time format value is optional. shorter message is also accepted
        args[6] = encodeTimeFormat(timeFormat);
        return encodeCommandMessage(2, 32, args);
    }

    public enum Gender {
        FEMALE,
        MALE
    }

    public static byte[] encodeSetUserInfo(Gender gender, int age, int heightCm, int weightKg, int stepsGoal) {
        byte[] args = new byte[8];
        args[0] = (byte) ((gender == Gender.MALE) ? 1 : 0);
        args[1] = (byte) (age & 0xFF);
        args[2] = (byte) (heightCm & 0xFF);
        args[3] = (byte) (weightKg & 0xFF);
        encodeInt32Goal(args, 4, stepsGoal);

        return encodeCommandMessage(2, 35, args);
    }

    public static byte[] encodeFindDevice() {
        return encodeCommandMessage(5, 80, null);
    }

    public enum NotificationType {
        INCOMING_CALL(0), // shows popup with hang up button

        SMS(1),
        MQQ(2),
        WEIXIN(3),
        FACEBOOK(4),
        TWITTER(6),
        WHATSAPP(7),
        INSTAGRAM(8),
        LINKEDIN(9),

        CALL_REJECT(15), // closes INCOMING_CALL notification
        CALL_ACCEPT(16), // closes INCOMING call notification

        UNKNOWN(255);

        private final byte value;

        NotificationType(final int value) {
            this.value = (byte) value;
        }

        public byte getValue() {
            return this.value;
        }
    }

    public static byte[] encodeShowNotification(NotificationType type, String text) {
        final int TEXT_LENGTH_MAX = 64;

        text = text.trim();
        if (text.length() > TEXT_LENGTH_MAX) {
            text = StringUtils.truncate(text, TEXT_LENGTH_MAX);
            text = text.trim(); // trim again so text is centered on screen
        }

        byte[] args = byteArrayAdd(new byte[]{type.getValue()}, text.getBytes(StandardCharsets.UTF_16LE));
        return encodeCommandMessage(6, 96, args);
    }

    public static byte[] encodeSetAlarms(ArrayList<? extends Alarm> alarms) {
        final int ENTRY_COUNT = 5;
        final int ENTRY_SIZE = 5;

        byte[] args = new byte[ENTRY_COUNT * ENTRY_SIZE];
        Arrays.fill(args, (byte) 0);

        int offset = 0;
        for (Alarm alarm : alarms) {
            // When all properties of an alarm are 0, it will not be listed in the watch UI.
            // Disabled alarms with non-0 properties will be shown in disabled state.
            // Show only enabled:
            if (alarm.getEnabled() && !alarm.getUnused()) {
                args[offset + 0] = (byte) alarm.getHour();
                args[offset + 1] = (byte) alarm.getMinute();
                args[offset + 2] = (byte) alarm.getRepetition();
                args[offset + 3] = (byte) 1;
                args[offset + 4] = encodeBoolean(alarm.getEnabled());

                offset += ENTRY_SIZE;
            }
        }

        return encodeCommandMessage(2, 33, args);
    }

    private static final Map<String, Integer> LANGUAGES = new HashMap<String, Integer>() {{
        put("en", 1);
        put("es", 3);
        put("de", 4);
        put("it", 5);
        put("fr", 6);
        put("sv", 18);

        put("ru", 2);
        put("pt", 7);
        put("pl", 8);
        put("nl", 9);
        put("el", 10);
        put("tr", 11);
        put("ro", 12);
        put("ja", 13);
        put("he", 15);
        put("da", 16);
        put("sr", 17);
        put("cs", 19);
        put("sk", 20);
        put("hu", 21);
        put("ar", 22);
        put("bg", 23);
        put("th", 24);
        put("uk", 25);
        put("fi", 26);
        put("nb", 27);
        put("ko", 28);
        put("id", 29);
        put("lv", 30);
        put("lt", 31);
        put("et", 32);
        put("my", 33);
        put("vi", 34);
        put("hr", 35);
    }};

    private static int resolveLanguageId(String language_, String country_) {
        String language = language_.toLowerCase();
        String country = country_.toLowerCase();

        if (language.equals("zh")) {
            switch (country) {
                case "cn":
                    return 0;
                case "tw":
                case "hk":
                case "mo":
                    return 14;
            }
        } else if (language.equals("pt") && country.equals("br")) {
            return 36;
        } else {
            Integer languageId = LANGUAGES.get(language);
            if (languageId != null)
                return languageId;
        }

        return LANGUAGES.get("en");
    }

    public static byte[] encodeSetSystemData(String lang, String country, TimeFormat timeFormat) {
        byte[] args = new byte[4];
        args[0] = (byte) resolveLanguageId(lang, country);
        args[1] = encodeTimeFormat(timeFormat);
        args[2] = (byte) 60; // screen. unclear
        args[3] = (byte) 0; // pair. 1 has no effect

        return encodeCommandMessage(2, 39, args);
    }

    public static byte[] encodeSetDoNotDisturb(boolean enable, int startHour, int startMinute, int endHour, int endMinute) {
        byte[] args = new byte[5];
        args[0] = encodeBoolean(enable);
        args[1] = (byte) startHour;
        args[2] = (byte) startMinute;
        args[3] = (byte) endHour;
        args[4] = (byte) endMinute;

        return encodeCommandMessage(6, 100, args);
    }

    public static byte[] encodeSetUnit(boolean isMetric) {
        byte[] args = new byte[2];
        args[0] = args[1] = (byte) ((isMetric) ? 0 : 1); // 0: metric, 1: imperial
        return encodeCommandMessage(2, 1, args);
    }

    public static byte[] encodeSetLiftWristDisplayOn(boolean enable) {
        byte[] args = new byte[3];
        args[0] = (byte) 1; // hand
        args[1] = encodeBoolean(enable); // raise
        args[2] = encodeBoolean(enable); // wrist

        return encodeCommandMessage(4, 74, args);
    }

    public static byte[] encodeSetAutoHeartRate(boolean enable, int startHour, int startMinute, int endHour, int endMinute, int intervalMinutes) {
        byte[] args = new byte[7];
        args[0] = encodeBoolean(enable);
        args[1] = (byte) startHour;
        args[2] = (byte) startMinute;
        args[3] = (byte) endHour;
        args[4] = (byte) endMinute;
        encodeInt16(args, 5, intervalMinutes);

        return encodeCommandMessage(9, 146, args);
    }

    public static byte[] encodeSetHydrationReminder(boolean enable, int startHour, int startMinute, int endHour, int endMinute, int intervalMinutes) {
        byte[] args = new byte[8];
        args[0] = encodeBoolean(enable);
        args[1] = (byte) startHour;
        args[2] = (byte) startMinute;
        args[3] = (byte) endHour;
        args[4] = (byte) endMinute;
        args[5] = (byte) 0x7F; // repeat: bitmask for days of week, bit 0=Monday
        encodeInt16(args, 6, intervalMinutes);

        return encodeCommandMessage(2, 40, args);
    }

    public static byte[] encodeFactoryReset() {
        return encodeCommandMessage(2, 199, null);
    }
}
