package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble.webview;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

class CurrentPosition {

    private static final Logger LOG = LoggerFactory.getLogger(CurrentPosition.class);

    long timestamp;
    double altitude;
    float latitude, longitude, accuracy, speed;

    float getLatitude() {
        return latitude;
    }

    float getLongitude() {
        return longitude;
    }

    CurrentPosition() {
        Prefs prefs = GBApplication.getPrefs();
        this.latitude = prefs.getFloat("location_latitude", 0);
        this.longitude = prefs.getFloat("location_longitude", 0);
        LOG.info("got longitude/latitude from preferences: " + latitude + "/" + longitude);

        this.timestamp = System.currentTimeMillis() - 86400000; //let accessor know this value is really old

        if (ActivityCompat.checkSelfPermission(GBApplication.getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                prefs.getBoolean("use_updated_location_if_available", false)) {
            LocationManager locationManager = (LocationManager) GBApplication.getContext().getSystemService(Context.LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            String provider = locationManager.getBestProvider(criteria, false);
            if (provider != null) {
                Location lastKnownLocation = locationManager.getLastKnownLocation(provider);
                if (lastKnownLocation != null) {
                    this.timestamp = lastKnownLocation.getTime();
                    this.timestamp = System.currentTimeMillis() - 1000; //TODO: request updating the location and don't fake its age

                    this.latitude = (float) lastKnownLocation.getLatitude();
                    this.longitude = (float) lastKnownLocation.getLongitude();
                    this.accuracy = lastKnownLocation.getAccuracy();
                    this.altitude = (float) lastKnownLocation.getAltitude();
                    this.speed = lastKnownLocation.getSpeed();
                }
            }
        }
    }
}
