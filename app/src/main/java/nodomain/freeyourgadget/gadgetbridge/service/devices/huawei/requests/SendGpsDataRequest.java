/*  Copyright (C) 2024 Martin.JM

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import android.location.Location;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.GpsAndTime;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SendGpsDataRequest extends Request {
    Location location;
    Location lastLocation;
    GpsAndTime.GpsParameters.Response gpsParametersResponse;

    public SendGpsDataRequest(HuaweiSupportProvider support, Location location, Location lastLocation, GpsAndTime.GpsParameters.Response gpsParametersResponse) {
        super(support);
        this.serviceId = GpsAndTime.id;
        this.commandId = GpsAndTime.GpsData.id;
        this.location = location;
        this.lastLocation = lastLocation;
        this.gpsParametersResponse = gpsParametersResponse;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        ArrayList<GpsAndTime.GpsData.Request.GpsDataContainer> gpsList = new ArrayList<>();
        GpsAndTime.GpsData.Request.GpsDataContainer gpsData = new GpsAndTime.GpsData.Request.GpsDataContainer();

        long timeOffset = 1000; // Default 1 second sample time
        float distance = 0.0f;
        if (lastLocation != null) {
            timeOffset = location.getTime() - lastLocation.getTime();
            distance = location.distanceTo(lastLocation);
        }
        if (this.gpsParametersResponse.supportsSpeed && location.hasSpeed()) {
            gpsData.hasSpeed = true;
            gpsData.speed = (short) location.getSpeed();
        }
        if (this.gpsParametersResponse.supportsAltitude && location.hasAltitude()) {
            gpsData.hasAltitude = true;
            gpsData.altitude = (short) location.getAltitude();
        }
        if (this.gpsParametersResponse.supportsLatLon) {
            gpsData.hasLatLon = true;
            gpsData.lat = location.getLatitude();
            gpsData.lon = location.getLongitude();
        }
        if (this.gpsParametersResponse.supportsDirection && location.hasBearing()) {
            gpsData.hasBearing = true;
            gpsData.bearing = location.getBearing();
        }
        if (this.gpsParametersResponse.supportsPrecision && location.hasAccuracy()) {
            gpsData.hasAccuracy = true;
            gpsData.accuracy = location.getAccuracy();
        }
        if (this.gpsParametersResponse.supportsDistance) {
            gpsData.hasDistance = true;
            gpsData.distance = (int)distance;
        }
        long currentTime = System.currentTimeMillis();
        gpsData.hasStartTime = true;
        gpsData.startTime = (int)((currentTime - timeOffset) / 1000);
        gpsData.hasEndTime = true;
        gpsData.endTime = (int)(currentTime / 1000);

        gpsList.add(gpsData);
        try {
            return new GpsAndTime.GpsData.Request(
                    this.paramsProvider,
                    gpsList
            ).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }
}
