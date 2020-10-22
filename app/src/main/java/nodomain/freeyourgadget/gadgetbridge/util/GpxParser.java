package nodomain.freeyourgadget.gadgetbridge.util;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate;

public class GpxParser {
    private XmlPullParser parser;
    private List<GPSCoordinate> points;
    private int eventType;

    public GpxParser(InputStream stream) {
        points = new ArrayList<>();
        try {
            parser = createXmlParser(stream);
            parseGpx();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static XmlPullParser createXmlParser(InputStream stream) throws XmlPullParserException {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(stream, null);
        return parser;
    }

    private void parseGpx() throws XmlPullParserException, IOException {
        eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.getName().equals("trkpt"))
                points.add(parsePoint(parser));
            else
                eventType = parser.next();
        }
    }

    private double parseElevation(XmlPullParser parser) throws XmlPullParserException, IOException {
        String eleString = "";
        while (eventType != XmlPullParser.END_TAG) {
            if (eventType == XmlPullParser.TEXT) {
                eleString = parser.getText();
            }
            eventType = parser.next();
        }
        return Double.parseDouble(eleString);
    }

    private GPSCoordinate parsePoint(XmlPullParser parser) throws XmlPullParserException, IOException {
        double lat;
        double lon;
        double ele = 0;
        String latString = parser.getAttributeValue(null, "lat");
        String lonString = parser.getAttributeValue(null, "lon");
        lat = latString != null ? Double.parseDouble(latString) : 0;
        lon = lonString != null ? Double.parseDouble(lonString) : 0;
        while (eventType != XmlPullParser.END_TAG) {
            if (parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equals("ele")) {
                ele = parseElevation(parser);
            }
            eventType = parser.next();
        }
        return new GPSCoordinate(lat, lon, ele);
    }

    public List<GPSCoordinate> getPoints() {
        return points;
    }
}