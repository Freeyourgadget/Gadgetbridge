package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.deviceevents;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;

public class WeatherRequestDeviceEvent extends GBDeviceEvent {
    private final int format;
    private final int latitude;
    private final int longitude;
    private final int hoursOfForecast;
    public WeatherRequestDeviceEvent(int format, int latitude, int longitude, int hoursOfForecast) {
        this.format = format;
        this.latitude = latitude;
        this.longitude = longitude;
        this.hoursOfForecast = hoursOfForecast;
    }


}
