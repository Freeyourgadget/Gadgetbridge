/*  Copyright (C) 2020-2021 Petr Vaněk

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
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate;

public class GpxParser {
    private static final Logger LOG = LoggerFactory.getLogger(GpxParser.class);
    private XmlPullParser parser;
    private List<GPSCoordinate> points = new ArrayList<>();
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
            if (eventType == XmlPullParser.START_TAG && parser.getName().equals("trkpt")) {
                points.add(parsePoint(parser));
            } else {
                eventType = parser.next();
            }
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