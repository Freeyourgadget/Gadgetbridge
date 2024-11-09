/*  Copyright (C) 2024 Damien Gaignon, Martin.JM

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket.ParseException;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiTLV;

// TODO: complete responses

public class Alarms {

    public static class EventAlarm {
        public byte index;
        public boolean status = false;
        public byte startHour = -1;
        public byte startMinute = -1;
        public byte repeat = 0;
        public String name = "Alarm";

        public EventAlarm(HuaweiTLV tlv) throws ParseException {
            this.index = tlv.getByte(0x03);
            if (tlv.contains(0x04))
                this.status = tlv.getBoolean(0x04);
            if (this.status || tlv.contains(0x05)) {
                // I think we should refuse to set the alarm to a default time if it's enabled.
                // If it is enabled, it should have the time set.
                this.startHour = (byte) ((tlv.getShort(0x05) >> 8) & 0xFF);
                this.startMinute = (byte) (tlv.getShort(0x05) & 0xFF);
            }
            if (tlv.contains(0x06))
                this.repeat = tlv.getByte(0x06);
            if (tlv.contains(0x07))
                this.name = tlv.getString(0x07);
        }

        public EventAlarm(byte index, boolean status, byte startHour, byte startMinute, byte repeat, String name) {
            this.index = index;
            this.status = status;
            this.startHour = startHour;
            this.startMinute = startMinute;
            this.repeat = repeat;
            this.name = name;
        }

        public HuaweiTLV asTlv() {
            return new HuaweiTLV()
                    .put(0x03, index)
                    .put(0x04, status)
                    .put(0x05, (short) ((startHour << 8) | (startMinute & 0xFF)))
                    .put(0x06, repeat)
                    .put(0x07, name);
        }

        @Override
        public String toString() {
            return "EventAlarm{" +
                    "index=" + index +
                    ", status=" + status +
                    ", startHour=" + startHour +
                    ", startMinute=" + startMinute +
                    ", repeat=" + repeat +
                    ", name='" + name + '\'' +
                    '}';
        }
    }

    public static class SmartAlarm {
        public byte index;
        public boolean status;
        public byte startHour;
        public byte startMinute;
        public byte repeat;
        public byte aheadTime;

        public SmartAlarm(HuaweiTLV tlv) throws ParseException {
            this.index = tlv.getByte(0x03);
            this.status = tlv.getBoolean(0x04, false);
            this.startHour = (byte) ((tlv.getShort(0x05, (short) 0) >> 8) & 0xFF);
            this.startMinute = (byte) (tlv.getShort(0x05, (short) 0) & 0xFF);
            this.repeat = tlv.getByte(0x06, (byte) 0);
            this.aheadTime = tlv.getByte(0x07, (byte) 0);
        }

        public SmartAlarm(boolean status, byte startHour, byte startMinute, byte repeat, byte aheadTime) {
            this.index = 1;
            this.status = status;
            this.startHour = startHour;
            this.startMinute = startMinute;
            this.repeat = repeat;
            this.aheadTime = aheadTime;
        }

        public HuaweiTLV asTlv() {
            return new HuaweiTLV()
                    .put(0x03, index)
                    .put(0x04, status)
                    .put(0x05, (short) ((startHour << 8) | (startMinute & 0xFF)))
                    .put(0x06, repeat)
                    .put(0x07, aheadTime);
        }

        @Override
        public String toString() {
            return "SmartAlarm{" +
                    "index=" + index +
                    ", status=" + status +
                    ", startHour=" + startHour +
                    ", startMinute=" + startMinute +
                    ", repeat=" + repeat +
                    ", aheadTime=" + aheadTime +
                    '}';
        }
    }

    public static final byte id = 0x08;

    public static class EventAlarmsRequest extends HuaweiPacket {
        public static final byte id = 0x01;

        // TODO: move to list
        private final HuaweiTLV alarms;

        public EventAlarmsRequest(ParamsProvider paramsProvider) {
            super(paramsProvider);

            this.serviceId = Alarms.id;
            this.commandId = id;

            alarms = new HuaweiTLV();
        }

        public void addEventAlarm(EventAlarm alarm) {
            // TODO: 5 is a max and we may need to check for that and throw an exception if passed
            alarms.put(0x82, alarm.asTlv());
        }

        @Override
        public List<byte[]> serialize() throws CryptoException {
            if (this.alarms.get().size() == 0) {
                // Empty alarms - this will disable them all
                this.alarms.put(0x82, new HuaweiTLV().put(0x03, (byte) 0x01));
            }

            this.tlv = new HuaweiTLV().put(0x81, this.alarms);
            this.complete = true;

            return super.serialize();
        }
    }

    public static class SmartAlarmRequest extends HuaweiPacket {
        public static final int id = 0x02;

        public SmartAlarmRequest(
                ParamsProvider paramsProvider,
                SmartAlarm smartAlarm
        ) {
            super(paramsProvider);

            this.serviceId = Alarms.id;
            this.commandId = id;
            this.tlv = new HuaweiTLV()
                    .put(0x81, new HuaweiTLV()
                            .put(0x82, smartAlarm.asTlv())
                    );
            this.complete = true;
        }
    }

    public static class EventAlarmsList {
        public static final int id = 0x03;

        public static class Request extends HuaweiPacket {
            public Request(ParamsProvider paramsProvider) {
                super(paramsProvider);

                this.serviceId = Alarms.id;
                this.commandId = id;

                this.tlv = new HuaweiTLV().put(0x01);

                this.complete = true;
            }
        }

        public static class Response extends HuaweiPacket {
            public List<EventAlarm> eventAlarms;

            public Response(ParamsProvider paramsProvider) {
                super(paramsProvider);
            }

            @Override
            public void parseTlv() throws ParseException {
                eventAlarms = new ArrayList<>();

                HuaweiTLV tlv = this.tlv.getObject(0x81);
                for (HuaweiTLV subTlv : tlv.getObjects(0x82)) {
                    eventAlarms.add(new EventAlarm(subTlv));
                }
            }
        }
    }

    public static class SmartAlarmList {
        public static final int id = 0x04;

        public static class Request extends HuaweiPacket {
            public Request(ParamsProvider paramsProvider) {
                super(paramsProvider);

                this.serviceId = Alarms.id;
                this.commandId = id;

                this.tlv = new HuaweiTLV().put(0x01);

                this.complete = true;
            }
        }

        public static class Response extends HuaweiPacket {
            public SmartAlarm smartAlarm;

            public Response(ParamsProvider paramsProvider) {
                super(paramsProvider);
            }

            @Override
            public void parseTlv() throws ParseException {
                HuaweiTLV tlv = this.tlv.getObject(0x81);
                if (tlv.contains(0x82)) {
                    this.smartAlarm = new SmartAlarm(tlv.getObject(0x82));
                } else {
                    this.smartAlarm = null;
                }
            }
        }
    }
}
