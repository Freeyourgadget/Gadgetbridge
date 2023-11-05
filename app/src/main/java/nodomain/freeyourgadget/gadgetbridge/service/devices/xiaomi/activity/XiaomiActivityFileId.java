/*  Copyright (C) 2023 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;

import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;

public class XiaomiActivityFileId {
    private final Date timestamp;
    private final int timezone;
    private final int type;
    private final int subtype;
    private final int detailType;
    private final int version;

    public XiaomiActivityFileId(final Date timestamp,
                                final int timezone,
                                final int type,
                                final int subtype,
                                final int detailType,
                                final int version) {
        this.timestamp = timestamp;
        this.timezone = timezone;
        this.type = type;
        this.subtype = subtype;
        this.detailType = detailType;
        this.version = version;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public int getTimezone() {
        return timezone;
    }

    public Type getType() {
        return Type.fromCode(type);
    }

    public Subtype getSubtype() {
        return Subtype.fromCode(getType(), subtype);
    }

    public DetailType getDetailType() {
        return DetailType.fromCode(detailType);
    }

    public int getVersion() {
        return version;
    }

    public byte[] toBytes() {
        final ByteBuffer buf = ByteBuffer.allocate(7).order(ByteOrder.LITTLE_ENDIAN);

        buf.putInt((int) (timestamp.getTime() / 1000));
        buf.put((byte) timezone);
        buf.put((byte) version);
        buf.put((byte) (type << 7 | subtype << 2 | detailType));

        return buf.array();
    }

    public static XiaomiActivityFileId from(final byte[] bytes) {
        assert bytes.length == 7;

        return from(ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN));
    }

    public static XiaomiActivityFileId from(final ByteBuffer buf) {
        final int ts = buf.getInt();
        final int tz = buf.get(); // 15 min blocks
        final int version = buf.get();
        final int flags = buf.get();
        // bit 0 is type - 0 activity, 1 sports
        final int type = (flags >> 7) & 1;
        // bit 1 to 6 bits are subtype
        //   for activity: activity, sleep, measurements, etc
        //   for workout: workout type (8 freestyle)
        final int subtype = (flags & 127) >> 2;
        // bit 6 and 7 - 0 details, 1 summary
        final int detailType = flags & 3;

        return new XiaomiActivityFileId(new Date(ts * 1000L), tz, type, subtype, detailType, version);
    }

    @NonNull
    @Override
    public String toString() {
        final Type typeName = Type.fromCode(type);
        final Subtype subtypeName = Subtype.fromCode(typeName, subtype);
        final DetailType detailTypeName = DetailType.fromCode(detailType);

        return getClass().getSimpleName() + "{" +
                "timestamp=" + DateTimeUtils.formatIso8601(timestamp) +
                ", timezone=" + timezone +
                ", type=" + (typeName + "(" + type + ")") +
                ", subtype=" + (subtypeName + "(" + subtype + ")") +
                ", detailType=" + (detailTypeName + "(" + detailType + ")") +
                ", version=" + version +
                "}";
    }

    public enum Type {
        UNKNOWN(-1),
        ACTIVITY(0),
        SPORTS(1),
        ;

        private final int code;

        Type(final int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static Type fromCode(final int code) {
            for (final Type type : values()) {
                if (type.getCode() == code) {
                    return type;
                }
            }
            return UNKNOWN;
        }
    }

    public enum Subtype {
        UNKNOWN(Type.UNKNOWN, -1),
        ACTIVITY_DAILY(Type.ACTIVITY, 0),
        ACTIVITY_SLEEP(Type.ACTIVITY,8),
        SPORTS_OUTDOOR_RUNNING(Type.SPORTS, 1),
        SPORTS_FREESTYLE(Type.SPORTS, 8),
        SPORTS_ELLIPTICAL(Type.SPORTS, 11),
        SPORTS_OUTDOOR_CYCLING(Type.SPORTS, 23),
        ;

        private final Type type;
        private final int code;

        Subtype(final Type type, final int code) {
            this.type = type;
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static Subtype fromCode(final Type type, final int code) {
            for (final Subtype subtype : values()) {
                if (subtype.type == type && subtype.getCode() == code) {
                    return subtype;
                }
            }
            return UNKNOWN;
        }
    }

    public enum DetailType {
        UNKNOWN(-1),
        DETAILS(0),
        SUMMARY(1),
        GPS_TRACK(2),
        ;

        private final int code;

        DetailType(final int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static DetailType fromCode(final int code) {
            for (final DetailType detailType : values()) {
                if (detailType.getCode() == code) {
                    return detailType;
                }
            }
            return UNKNOWN;
        }
    }
}
