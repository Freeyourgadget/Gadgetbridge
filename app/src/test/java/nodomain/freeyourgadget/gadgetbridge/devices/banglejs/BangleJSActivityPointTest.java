package nodomain.freeyourgadget.gadgetbridge.devices.banglejs;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate;

public class BangleJSActivityPointTest {
    @Test
    public void testParseCsvLine5s() {
        testTemplate(
                "Time,Latitude,Longitude,Altitude,Heartrate,Confidence,Source,Steps,Battery Percentage,Battery Voltage,Charging",
                "1710610740,,,,92,50,,0,96,3.30644531249,false",
                new BangleJSActivityPoint(
                        1710610740_000L,
                        null,
                        92,
                        50,
                        "",
                        0,
                        96,
                        3.30644531249,
                        false,
                        0,
                        0,
                        GPSCoordinate.UNKNOWN_ALTITUDE
                )
        );
    }

    @Test
    public void testParseCsvLine5sWithLocation() {
        testTemplate(
                "Time,Latitude,Longitude,Altitude,Heartrate,Confidence,Source,Steps,Battery Percentage,Battery Voltage,Charging",
                "1710610740,-65.999000,10.12300,,92,50,,0,96,3.30644531249,false",
                new BangleJSActivityPoint(
                        1710610740_000L,
                        new GPSCoordinate(10.123d, -65.999d),
                        92,
                        50,
                        "",
                        0,
                        96,
                        3.30644531249,
                        false,
                        0,
                        0,
                        GPSCoordinate.UNKNOWN_ALTITUDE
                )
        );
    }

    @Test
    public void testParseCsvLine5sWithLocationAndAltitude() {
        testTemplate(
                "Time,Latitude,Longitude,Altitude,Heartrate,Confidence,Source,Steps,Battery Percentage,Battery Voltage,Charging",
                "1710610740,-65.999000,10.12300,55,92,50,,0,96,3.30644531249,false",
                new BangleJSActivityPoint(
                        1710610740_000L,
                        new GPSCoordinate(10.123d, -65.999d, 55d),
                        92,
                        50,
                        "",
                        0,
                        96,
                        3.30644531249,
                        false,
                        0,
                        0,
                        GPSCoordinate.UNKNOWN_ALTITUDE
                )
        );
    }

    @Test
    public void testParseCsvLine1s() {
        testTemplate(
                "Time,Battery Percentage,Battery Voltage,Charging,Steps,Barometer Temperature,Barometer Pressure,Barometer Altitude,Heartrate,Confidence,Source,Latitude,Longitude,Altitude",
                "1700265185.2,78,3.31787109374,false,0,33.39859771728,1012.66780596669,4.84829130165,95.7,0,,,,",
                new BangleJSActivityPoint(
                        1700265185_200L,
                        null,
                        96,
                        0,
                        "",
                        0,
                        78,
                        3.31787109374,
                        false,
                        33.39859771728,
                        1012.66780596669,
                        4.84829130165
                )
        );
    }

    @Test
    public void testParseCsvLineBthrm() {
        testTemplate(
                "Time,Heartrate,Confidence,Source,Latitude,Longitude,Altitude,Int Heartrate,Int Confidence,BT Heartrate,BT Battery,Energy expended,Contact,RR,Barometer Temperature,Barometer Pressure,Barometer Altitude,Steps,Battery Percentage,Battery Voltage,Charging",
                "1727544008.4,61,100,bthrm,,,,0,32,61,,,,1069,31.20888417561,994.92400814020,153.70596141680,0,88,3.32226562499,false",
                new BangleJSActivityPoint(
                        1727544008_400L,
                        null,
                        61,
                        100,
                        "bthrm",
                        0,
                        88,
                        3.32226562499,
                        false,
                        31.20888417561,
                        994.92400814020,
                        153.70596141680
                )
        );
    }

    private void testTemplate(final String headerStr, final String csvLine, final BangleJSActivityPoint expected) {
        final List<String> header = Arrays.asList(headerStr.split(","));
        final BangleJSActivityPoint point = BangleJSActivityPoint.fromCsvLine(header, csvLine);
        assertPointEquals(expected, point);
    }

    private void assertPointEquals(final BangleJSActivityPoint expected, final BangleJSActivityPoint actual) {
        assertEquals("Mismatch on Time", expected.getTime(), actual.getTime());
        assertEquals("Mismatch on Location", expected.getLocation(), actual.getLocation());
        assertEquals("Mismatch on HeartRate", expected.getHeartRate(), actual.getHeartRate());
        assertEquals("Mismatch on HrConfidence", expected.getHrConfidence(), actual.getHrConfidence());
        assertEquals("Mismatch on HrSource", expected.getHrSource(), actual.getHrSource());
        assertEquals("Mismatch on Steps", expected.getSteps(), actual.getSteps());
        assertEquals("Mismatch on BatteryPercentage", expected.getBatteryPercentage(), actual.getBatteryPercentage());
        assertEquals("Mismatch on BatteryVoltage", expected.getBatteryVoltage(), actual.getBatteryVoltage(), 0.000001d);
        assertEquals("Mismatch on Charging", expected.isCharging(), actual.isCharging());
        assertEquals("Mismatch on BarometerTemperature", expected.getBarometerTemperature(), actual.getBarometerTemperature(), 0.000001d);
        assertEquals("Mismatch on BarometerPressure", expected.getBarometerPressure(), actual.getBarometerPressure(), 0.000001d);
        assertEquals("Mismatch on BarometerAltitude", expected.getBarometerAltitude(), actual.getBarometerAltitude(), 0.000001d);
    }
}
