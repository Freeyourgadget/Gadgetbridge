/*  Copyright (C) 2023-2024 Andreas Shimokawa, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.builder.CompareToBuilder;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;

public class XiaomiActivityFileId implements Comparable<XiaomiActivityFileId> {
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

    public int getTypeCode() {
        return type;
    }

    public Subtype getSubtype() {
        return Subtype.fromCode(getType(), subtype);
    }

    public int getSubtypeCode() {
        return subtype;
    }

    public DetailType getDetailType() {
        return DetailType.fromCode(detailType);
    }

    public int getDetailTypeCode() {
        return detailType;
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
                ", type=" + (typeName + String.format("(0x%02X)", type)) +
                ", subtype=" + (subtypeName + String.format("(0x%02X)", subtype)) +
                ", detailType=" + (detailTypeName + String.format("(0x%02X)", detailType)) +
                ", version=" + version +
                "}";
    }

    @Override
    public int compareTo(final XiaomiActivityFileId o) {
        return new CompareToBuilder()
                .append(timestamp, o.timestamp)
                .append(timezone, o.timezone)
                .append(type, o.type)
                .append(subtype, o.subtype)
                .append(getDetailType().getFetchOrder(), o.getDetailType().getFetchOrder())
                .append(version, o.version)
                .build();
    }

    public String getFilename() {
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.US);

        return String.format(
                Locale.ROOT,
                "xiaomi_%s_%02X_%02X_%02X_v%d.bin",
                sdf.format(getTimestamp()),
                getTypeCode(),
                getSubtypeCode(),
                getDetailTypeCode(),
                getVersion()
        );
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
        ACTIVITY_DAILY(Type.ACTIVITY, 0x00),
        ACTIVITY_SLEEP_STAGES(Type.ACTIVITY, 0x03),
        ACTIVITY_MANUAL_SAMPLES(Type.ACTIVITY, 0x06),
        ACTIVITY_SLEEP(Type.ACTIVITY, 0x08),
        SPORTS_OUTDOOR_RUNNING(Type.SPORTS, 0x01),
        SPORTS_OUTDOOR_WALKING_V1(Type.SPORTS, 0x02),
        SPORTS_TREADMILL(Type.SPORTS, 0x03),
        SPORTS_OUTDOOR_CYCLING_V2(Type.SPORTS, 0x06),
        SPORTS_INDOOR_CYCLING(Type.SPORTS, 0x07),
        SPORTS_FREESTYLE(Type.SPORTS, 0x08),
        SPORTS_POOL_SWIMMING(Type.SPORTS, 0x09),
        SPORTS_HIIT(Type.SPORTS, 0x10),
        SPORTS_ELLIPTICAL(Type.SPORTS, 0x0B),
        SPORTS_ROWING(Type.SPORTS, 0x0D),
        SPORTS_JUMP_ROPING(Type.SPORTS, 0x0E),
        SPORTS_OUTDOOR_WALKING_V2(Type.SPORTS, 0x16),
        SPORTS_OUTDOOR_CYCLING(Type.SPORTS, 0x17),
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
        SUMMARY(0x01),
        GPS_TRACK(0x02),
        ;

        private final int code;

        DetailType(final int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public int getFetchOrder() {
            // Fetch summary first, so we have the summary data for workouts
            // before parsing the gps track
            switch (this) {
                case SUMMARY:
                    return 0;
                case DETAILS:
                    return 1;
                case GPS_TRACK:
                    return 2;
            }

            return 3;
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
