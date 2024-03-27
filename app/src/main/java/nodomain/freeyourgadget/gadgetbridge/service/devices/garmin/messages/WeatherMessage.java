package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.GlobalDefinitionsEnum;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;

public class WeatherMessage extends GFDIMessage {
    private final int format;
    private final int latitude;
    private final int longitude;
    private final int hoursOfForecast;


    private final List<RecordDefinition> weatherDefinitions;

    public WeatherMessage(int format, int latitude, int longitude, int hoursOfForecast, GarminMessage garminMessage) {
        this.format = format;
        this.latitude = latitude;
        this.longitude = longitude;
        this.hoursOfForecast = hoursOfForecast;
        this.garminMessage = garminMessage;


        weatherDefinitions = new ArrayList<>(3);
        weatherDefinitions.add(GlobalDefinitionsEnum.TODAY_WEATHER_CONDITIONS.getRecordDefinition());
        weatherDefinitions.add(GlobalDefinitionsEnum.HOURLY_WEATHER_FORECAST.getRecordDefinition());
        weatherDefinitions.add(GlobalDefinitionsEnum.DAILY_WEATHER_FORECAST.getRecordDefinition());

        this.statusMessage = this.getStatusMessage();

    }

    public static WeatherMessage parseIncoming(MessageReader reader, GarminMessage garminMessage) {
        final int format = reader.readByte();
        final int latitude = reader.readInt();
        final int longitude = reader.readInt();
        final int hoursOfForecast = reader.readByte();

        return new WeatherMessage(format, latitude, longitude, hoursOfForecast, garminMessage);
    }

    @Override
    protected boolean generateOutgoing() {
        final MessageWriter writer = new MessageWriter(response);
        writer.writeShort(0); // packet size will be filled below
        writer.writeShort(GarminMessage.FIT_DEFINITION.getId());
        for (RecordDefinition definition : weatherDefinitions) {
            definition.generateOutgoingPayload(writer);
        }
        return true;
    }
}
