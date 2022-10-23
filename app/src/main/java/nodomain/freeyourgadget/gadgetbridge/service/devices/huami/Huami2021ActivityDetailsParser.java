/*  Copyright (C) 2022 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami;

import androidx.annotation.Nullable;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityPoint;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityTrack;
import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate;

public class Huami2021ActivityDetailsParser extends AbstractHuamiActivityDetailsParser {
    private static final Logger LOG = LoggerFactory.getLogger(Huami2021ActivityDetailsParser.class);

    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US);

    static {
        SDF.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private Date timestamp;
    private long offset = 0;

    private long longitude;
    private long latitude;
    private double altitude;

    private final ActivityTrack activityTrack;
    private ActivityPoint lastActivityPoint;

    public Huami2021ActivityDetailsParser(final BaseActivitySummary summary) {
        this.timestamp = summary.getStartTime();

        this.longitude = summary.getBaseLongitude();
        this.latitude = summary.getBaseLatitude();
        this.altitude = summary.getBaseAltitude();

        this.activityTrack = new ActivityTrack();
        this.activityTrack.setUser(summary.getUser());
        this.activityTrack.setDevice(summary.getDevice());
        this.activityTrack.setName(createActivityName(summary));
    }

    @Override
    public ActivityTrack parse(final byte[] bytes) throws GBException {
        final ByteBuffer buf = ByteBuffer.wrap(bytes)
                .order(ByteOrder.LITTLE_ENDIAN);

        // Keep track of unknown type codes so we can print them without spamming the logs
        final Map<Byte, Integer> unknownTypeCodes = new HashMap<>();
        
        while (buf.position() < buf.limit()) {
            final byte typeCode = buf.get();
            final byte length = buf.get();
            final int initialPosition = buf.position();

            final Type type = Type.fromCode(typeCode);
            if (type == null) {
                if (!unknownTypeCodes.containsKey(typeCode)) {
                    unknownTypeCodes.put(typeCode, 0);
                }

                unknownTypeCodes.put(typeCode, unknownTypeCodes.get(typeCode) + 1);
                //LOG.warn("Unknown type code {} of length {}", String.format("0x%X", typeCode), length);
                // Consume the reported length
                buf.get(new byte[length]);
                continue;
            } else if (length != type.getExpectedLength()) {
                LOG.warn("Unexpected length {} for type {}", length, type);
                // Consume the reported length
                buf.get(new byte[length]);
                continue;
            }

            // Consume
            switch (type) {
                case TIMESTAMP:
                    consumeTimestamp(buf);
                    break;
                case GPS_COORDS:
                    consumeGpsCoords(buf);
                    break;
                case GPS_DELTA:
                    consumeGpsDelta(buf);
                    break;
                case STATUS:
                    consumeStatus(buf);
                    break;
                case SPEED:
                    consumeSpeed(buf);
                    break;
                case ALTITUDE:
                    consumeAltitude(buf);
                    break;
                case HEARTRATE:
                    consumeHeartRate(buf);
                    break;
                case STRENGTH_SET:
                    // TODO parse strength sets: weight, count, type
                default:
                    LOG.warn("No consumer for for type {}", type);
                    // Consume the reported length
                    buf.get(new byte[length]);
                    continue;
            }

            final int expectedPosition = initialPosition + length;
            if (buf.position() != expectedPosition) {
                // Should never happen unless there's a bug in one of the consumers
                throw new IllegalStateException("Unexpected position " + buf.position() + ", expected " + expectedPosition + ", after consuming " + type);
            }
        }

        if (!unknownTypeCodes.isEmpty()) {
            for (final Map.Entry<Byte, Integer> e : unknownTypeCodes.entrySet()) {
                LOG.warn("Unknown type code {} seen {} times", String.format("0x%X", e.getKey()), e.getValue());
            }
        }

        return this.activityTrack;
    }

    private void consumeTimestamp(final ByteBuffer buf) {
        buf.getInt(); // ?
        this.timestamp = new Date(buf.getLong());
        this.offset = 0;

        //trace("Consumed timestamp");
    }

    private void consumeTimestampOffset(final ByteBuffer buf) {
        this.offset = buf.getShort();
    }

    private void consumeGpsCoords(final ByteBuffer buf) {
        buf.get(new byte[6]); // ?
        this.longitude = buf.getInt();
        this.latitude = buf.getInt();
        buf.get(new byte[6]); // ?

        // TODO which one is the time offset? Not sure it is the first

        addNewGpsCoordinates();

        final double longitudeDeg = convertHuamiValueToDecimalDegrees(longitude);
        final double latitudeDeg = convertHuamiValueToDecimalDegrees(latitude);

        //trace("Consumed GPS coords: {} {}", longitudeDeg, latitudeDeg);
    }

    private void consumeGpsDelta(final ByteBuffer buf) {
        consumeTimestampOffset(buf);
        final short longitudeDelta = buf.getShort();
        final short latitudeDelta = buf.getShort();
        buf.getShort(); // ? seems to always be 2

        this.longitude += longitudeDelta;
        this.latitude += latitudeDelta;

        if (lastActivityPoint == null) {
            final String timestampStr = SDF.format(new Date(timestamp.getTime() + offset));
            LOG.warn("{}: Got GPS delta before GPS coords, ignoring", timestampStr);
            return;
        }

        addNewGpsCoordinates();

        //trace("Consumed GPS delta: {} {}", longitudeDelta, latitudeDelta);
    }

    private void consumeStatus(final ByteBuffer buf) {
        consumeTimestampOffset(buf);

        final int statusCode = buf.getShort();
        final String status;
        switch (statusCode) {
            case 1:
                status = "start";
                break;
            case 4:
                status = "pause";
                break;
            case 5:
                status = "resume";
                break;
            case 6:
                status = "stop";
                break;
            default:
                status = String.format("unknown (0x%X)", statusCode);
                LOG.warn("Unknown status code {}", String.format("0x%X", statusCode));
        }

        // TODO split track into multiple segments?

        //trace("Consumed Status: {}", status);
    }

    private void consumeSpeed(final ByteBuffer buf) {
        consumeTimestampOffset(buf);

        final short cadence = buf.getShort(); // spm
        final short stride = buf.getShort(); // cm
        final short pace = buf.getShort(); // sec/km

        // TODO integrate into gpx

        //trace("Consumed speed: cadence={}, stride={}, ?={}", cadence, stride, );
    }

    private void consumeAltitude(final ByteBuffer buf) {
        consumeTimestampOffset(buf);
        altitude = (int) (buf.getInt() / 100.0f);

        final ActivityPoint ap = getCurrentActivityPoint();
        if (ap != null) {
            final GPSCoordinate newCoordinate = new GPSCoordinate(
                    ap.getLocation().getLongitude(),
                    ap.getLocation().getLatitude(),
                    altitude
            );

            ap.setLocation(newCoordinate);
        }

        //trace("Consumed altitude: {}", altitude);
    }

    private void consumeHeartRate(final ByteBuffer buf) {
        consumeTimestampOffset(buf);
        final int heartRate = buf.get() & 0xff;

        final ActivityPoint ap = getCurrentActivityPoint();
        if (ap != null) {
            ap.setHeartRate(heartRate);
        }

        //trace("Consumed HeartRate: {}", heartRate);
    }

    @Nullable
    private ActivityPoint getCurrentActivityPoint() {
        if (lastActivityPoint == null) {
            return null;
        }

        // Round to the nearest second
        final long currentTime = timestamp.getTime() + offset;
        if (currentTime - lastActivityPoint.getTime().getTime() > 500) {
            addNewGpsCoordinates();
            return lastActivityPoint;
        }

        return lastActivityPoint;
    }

    private void addNewGpsCoordinates() {
        final GPSCoordinate coordinate = new GPSCoordinate(
                convertHuamiValueToDecimalDegrees(longitude),
                convertHuamiValueToDecimalDegrees(latitude),
                altitude
        );

        if (lastActivityPoint != null && lastActivityPoint.getLocation() != null && lastActivityPoint.getLocation().equals(coordinate)) {
            // Ignore repeated location
            return;
        }

        final ActivityPoint ap = new ActivityPoint(new Date(timestamp.getTime() + offset));
        ap.setLocation(coordinate);
        add(ap);
    }

    private void add(final ActivityPoint ap) {
        if (ap == lastActivityPoint) {
            LOG.debug("skipping point!");
            return;
        }

        lastActivityPoint = ap;
        activityTrack.addTrackPoint(ap);
    }

    private void trace(final String format, final Object... args) {
        final Object[] argsWithDate = ArrayUtils.insert(0, args, SDF.format(new Date(timestamp.getTime() + offset)));
        LOG.debug("{}: " + format, argsWithDate);
    }

    private enum Type {
        TIMESTAMP(1, 12),
        GPS_COORDS(2, 20),
        GPS_DELTA(3, 8),
        STATUS(4, 4),
        SPEED(5, 8),
        ALTITUDE(7, 6),
        HEARTRATE(8, 3),
        STRENGTH_SET(15, 34),
        ;

        private final byte code;
        private final int expectedLength;

        Type(final int code, final int expectedLength) {
            this.code = (byte) code;
            this.expectedLength = expectedLength;
        }

        public byte getCode() {
            return this.code;
        }

        public int getExpectedLength() {
            return this.expectedLength;
        }

        public static Type fromCode(final byte code) {
            for (final Type type : values()) {
                if (type.getCode() == code) {
                    return type;
                }
            }

            return null;
        }
    }
}
