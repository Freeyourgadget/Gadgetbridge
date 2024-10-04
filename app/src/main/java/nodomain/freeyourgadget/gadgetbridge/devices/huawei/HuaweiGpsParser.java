package nodomain.freeyourgadget.gadgetbridge.devices.huawei;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class HuaweiGpsParser {

    public static class GpsPoint {
        public int timestamp;
        public double latitude;
        public double longitude;
        public boolean altitudeSupported;
        public double altitude;

        @NonNull
        @Override
        public String toString() {
            return "GpsPoint{" +
                    "timestamp=" + timestamp +
                    ", longitude=" + longitude +
                    ", latitude=" + latitude +
                    ", altitudeSupported=" + altitudeSupported +
                    ", altitude=" + altitude +
                    '}';
        }
    }

    public static GpsPoint[] parseHuaweiGps(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // Skip trim
        buffer.position(0x20);

        int timestamp;
        double lon_start;
        double lat_start;
        boolean alt_support;
        double alt_start;

        byte fileType = buffer.get();
        if ((fileType & 0x03) != 0x03) {
            alt_support = false;
            timestamp = buffer.getInt();
            lon_start = buffer.getDouble();
            lat_start = buffer.getDouble();
            alt_start = 0;
            buffer.position(62); // Skip past unknown fields/padding
        } else {
            alt_support = true;
            timestamp = buffer.getInt();
            lon_start = buffer.getDouble();
            lat_start = buffer.getDouble();
            alt_start = buffer.getDouble();
            buffer.position(70); // Skip past unknown fields/padding
        }

        lat_start = lat_start * 0.017453292519943;
        lon_start = lon_start * 0.017453292519943;

        // Working values
        int time = timestamp;
        double lat = lat_start;
        double lon = lon_start;
        double alt = alt_start;

        int data_size = 15;
        if (alt_support)
            data_size += 4;

        ArrayList<GpsPoint> retv = new ArrayList<>(buffer.remaining() / data_size);
        while (buffer.remaining() > data_size) {
            short time_delta = buffer.getShort();
            buffer.getShort(); // Unknown value
            float lon_delta = buffer.getFloat();
            float lat_delta = buffer.getFloat();
            buffer.get(); buffer.get(); buffer.get(); // Unknown values

            time = time + time_delta;
            lat = lat + lat_delta;
            lon = lon + lon_delta;

            GpsPoint point = new GpsPoint();
            point.timestamp = time;
            point.latitude = (lat / 6383807.0d + lat_start) / 0.017453292519943d;
            point.longitude = (lon / 6383807.0d / Math.cos(lat_start) + lon_start) / 0.017453292519943d;
            point.altitudeSupported = alt_support;
            if (alt_support) {
                alt = buffer.getShort();
                buffer.getShort(); // Unknown values
                point.altitude = alt;
            }
            retv.add(point);
        }

        return retv.toArray(new GpsPoint[0]);
    }
}
