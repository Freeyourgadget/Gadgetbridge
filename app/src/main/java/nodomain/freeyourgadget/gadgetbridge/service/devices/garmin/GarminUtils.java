package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin;

import android.location.Location;
import android.os.Build;

import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiCore;

public final class GarminUtils {
    private GarminUtils() {
        // utility class
    }

    public static GdiCore.CoreService.LocationData toLocationData(final Location location, final GdiCore.CoreService.DataType dataType) {
        final GdiCore.CoreService.LatLon positionForWatch = GdiCore.CoreService.LatLon.newBuilder()
                .setLat((int) ((location.getLatitude() * 2.147483648E9d) / 180.0d))
                .setLon((int) ((location.getLongitude() * 2.147483648E9d) / 180.0d))
                .build();

        float vAccuracy = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vAccuracy = location.getVerticalAccuracyMeters();
        }

        return GdiCore.CoreService.LocationData.newBuilder()
                .setPosition(positionForWatch)
                .setAltitude((float) location.getAltitude())
                .setTimestamp(GarminTimeUtils.javaMillisToGarminTimestamp(location.getTime()))
                .setHAccuracy(location.getAccuracy())
                .setVAccuracy(vAccuracy)
                .setPositionType(dataType)
                .setBearing(location.getBearing())
                .setSpeed(location.getSpeed())
                .build();
    }
}
