package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages;

public class WeatherRequestMessage {
    public final int format;
    public final int latitude;
    public final int longitude;
    public final int hoursOfForecast;

    public WeatherRequestMessage(int format, int latitude, int longitude, int hoursOfForecast) {
        this.format = format;
        this.latitude = latitude;
        this.longitude = longitude;
        this.hoursOfForecast = hoursOfForecast;
    }

    public static WeatherRequestMessage parsePacket(byte[] packet) {
        final MessageReader reader = new MessageReader(packet, 4);
        final int format = reader.readByte();
        final int latitude = reader.readInt();
        final int longitude = reader.readInt();
        final int hoursOfForecast = reader.readByte();

        return new WeatherRequestMessage(format, latitude, longitude, hoursOfForecast);
    }
}
