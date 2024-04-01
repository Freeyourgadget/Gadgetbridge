/*  Copyright (C) 2024 Vitalii Tomin, Martin.JM

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

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiTLV;

public class GpsAndTime {
    public static final byte id = 0x18;

    public static class GpsParameters {
        public static final byte id = 0x01;

        public static class Request extends HuaweiPacket {
            public Request(ParamsProvider paramsProvider) {
                super(paramsProvider);

                this.serviceId = GpsAndTime.id;
                this.commandId = id;

                this.tlv = new HuaweiTLV().put(0x81);

                this.isEncrypted = true;
                this.complete = true;
            }
        }

        public static class Response extends HuaweiPacket {
            public byte supportBitmap; // Supported bitmap, parsed into the booleans below
            public byte format = 0; // 1 to send a single location per message (id 3), 2 for multiple
            public byte locationsPerMessage = 0; // Only for format 2, the number of locations to send per message
            public byte threshold = 0;

            public boolean supportsSpeed;
            public boolean supportsDistance;
            public boolean supportsAltitude;
            public boolean supportsTotalDistance;
            public boolean supportsLatLon;
            public boolean supportsMarsLatLon;
            public boolean supportsDirection;
            public boolean supportsPrecision;

            public Response(ParamsProvider paramsProvider) {
                super(paramsProvider);
            }

            @Override
            public void parseTlv() throws ParseException {
                HuaweiTLV subTlv = this.tlv.getObject(0x81);

                supportBitmap = subTlv.getByte(0x02);
                supportsSpeed = (supportBitmap & 0x01) != 0;
                supportsDistance = (supportBitmap & 0x02) != 0;
                supportsAltitude = (supportBitmap & 0x04) != 0;
                supportsTotalDistance = (supportBitmap & 0x08) != 0;
                supportsLatLon = (supportBitmap & 0x10) != 0;
                supportsMarsLatLon = (supportBitmap & 0x20) != 0;
                supportsDirection = (supportBitmap & 0x40) != 0;
                supportsPrecision = (supportBitmap & 0x80) != 0;

                if (subTlv.contains(0x03))
                    format = subTlv.getByte(0x03);
                if (subTlv.contains(0x04))
                    locationsPerMessage = subTlv.getByte(0x04);
                if (subTlv.contains(0x05))
                    threshold = subTlv.getByte(0x05);
            }
        }
    }

    public static class GpsStatus {
        public static final byte id = 0x02;

        public static class Response extends HuaweiPacket {
            public boolean enableGps;

            public Response(ParamsProvider paramsProvider) {
                super(paramsProvider);
            }

            @Override
            public void parseTlv() throws ParseException {
                enableGps = this.tlv.getByte(0x01) == 0x01;
            }
        }
    }

    public static class GpsData {
        public static final byte id = 0x03;

        public static class Request extends HuaweiPacket {
            public static class GpsDataContainer {
                public boolean hasSpeed = false;
                public short speed;

                public boolean hasDistance = false;
                public int distance;

                public boolean hasAltitude = false;
                public short altitude;

                public boolean hasTotalDistance = false;
                public int totalDistance;

                public boolean hasStartTime = false;
                public int startTime;

                public boolean hasEndTime = false;
                public int endTime;

                public boolean hasLatLon = false;
                public double lat;
                public double lon;

                public boolean hasMarsLatLon = false;
                public double marsLat;
                public double marsLon;

                public boolean hasBearing = false;
                public double bearing;

                public boolean hasAccuracy = false;
                public double accuracy;
            }

            public Request(
                    ParamsProvider paramsProvider,
                    List<GpsDataContainer> gpsDataList
            ) {
                super(paramsProvider);

                this.serviceId = GpsAndTime.id;
                this.commandId = id;

                HuaweiTLV subTlv = new HuaweiTLV();
                for (GpsDataContainer gpsData : gpsDataList) {
                    HuaweiTLV gpsTlv = new HuaweiTLV();
                    if (gpsData.hasSpeed)
                        gpsTlv.put(0x03, gpsData.speed);
                    if (gpsData.hasDistance)
                        gpsTlv.put(0x04, gpsData.distance);
                    if (gpsData.hasAltitude)
                        gpsTlv.put(0x05, gpsData.altitude);
                    if (gpsData.hasTotalDistance)
                        gpsTlv.put(0x06, gpsData.totalDistance);
                    if (gpsData.hasStartTime)
                        gpsTlv.put(0x07, gpsData.startTime);
                    if (gpsData.hasEndTime)
                        gpsTlv.put(0x08, gpsData.endTime);
                    if (gpsData.hasLatLon) {
                        gpsTlv.put(0x09, gpsData.lat);
                        gpsTlv.put(0x0a, gpsData.lon);
                    }
                    if (gpsData.hasMarsLatLon) {
                        gpsTlv.put(0x0b, gpsData.marsLat);
                        gpsTlv.put(0x0c, gpsData.marsLon);
                    }
                    if (gpsData.hasBearing)
                        gpsTlv.put(0x0d, gpsData.bearing);
                    if (gpsData.hasAccuracy)
                        gpsTlv.put(0x0e, gpsData.accuracy);
                    subTlv.put(0x82, gpsTlv);
                }
                this.tlv = new HuaweiTLV().put(0x81, subTlv);

                this.isEncrypted = true;
                this.complete = true;
            }
        }

        public static class Response extends HuaweiPacket {
            boolean shouldStop;

            public Response(ParamsProvider paramsProvider) {
                super(paramsProvider);
            }

            @Override
            public void parseTlv() throws ParseException {
                // If the response is not 0x000186A0, sending should be stopped
                shouldStop = this.tlv.getInteger(0x7f) != 0x000186A0;
            }
        }
    }

    public static class CurrentGPSRequest extends HuaweiPacket {
        public static final byte id = 0x07;
        public CurrentGPSRequest (
                ParamsProvider paramsProvider,
                int timestamp,
                double lat,
                double lon
        ) {
            super(paramsProvider);

            this.serviceId = GpsAndTime.id;
            this.commandId = id;
            this.tlv = new HuaweiTLV()
                    .put(0x01, timestamp)
                    .put(0x02, lon)
                    .put(0x03, lat);
            this.isEncrypted = true;
            this.complete = true;
        }
    }
}
