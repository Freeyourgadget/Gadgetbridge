package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages;

import java.util.Collections;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.deviceevents.WeatherRequestDeviceEvent;

public class WeatherMessage extends GFDIMessage {
    private final WeatherRequestDeviceEvent weatherRequestDeviceEvent;
    public WeatherMessage(int format, int latitude, int longitude, int hoursOfForecast, GarminMessage garminMessage) {

        this.garminMessage = garminMessage;
        weatherRequestDeviceEvent = new WeatherRequestDeviceEvent(format, latitude, longitude, hoursOfForecast);
        this.statusMessage = this.getStatusMessage();

    }

    public static WeatherMessage parseIncoming(MessageReader reader, GarminMessage garminMessage) {
        final int format = reader.readByte();
        final int latitude = reader.readInt();
        final int longitude = reader.readInt();
        final int hoursOfForecast = reader.readByte();

        return new WeatherMessage(format, latitude, longitude, hoursOfForecast, garminMessage);
    }

    public List<GBDeviceEvent> getGBDeviceEvent() {
        return Collections.singletonList(weatherRequestDeviceEvent);
    }

    @Override
    protected boolean generateOutgoing() {
        return false;
    }

}
