/*  Copyright (C) 2020-2023 Petr Vaněk, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.util.gpx;

import com.google.gson.internal.bind.util.ISO8601Utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParsePosition;
import java.util.Date;

import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate;
import nodomain.freeyourgadget.gadgetbridge.util.gpx.model.GpxFile;
import nodomain.freeyourgadget.gadgetbridge.util.gpx.model.GpxTrack;
import nodomain.freeyourgadget.gadgetbridge.util.gpx.model.GpxTrackPoint;
import nodomain.freeyourgadget.gadgetbridge.util.gpx.model.GpxTrackSegment;
import nodomain.freeyourgadget.gadgetbridge.util.gpx.model.GpxWaypoint;

public class GpxParser {
    private static final Logger LOG = LoggerFactory.getLogger(GpxParser.class);

    private final XmlPullParser parser;
    private int eventType;

    private final GpxFile.Builder fileBuilder;

    public GpxParser(final InputStream stream) throws GpxParseException {
        this.fileBuilder = new GpxFile.Builder();

        try {
            parser = createXmlParser(stream);
            parseGpx();
        } catch (final Exception e) {
            throw new GpxParseException("Failed to parse gpx", e);
        }
    }

    public GpxFile getGpxFile() {
        return fileBuilder.build();
    }

    private static XmlPullParser createXmlParser(InputStream stream) throws XmlPullParserException {
        final XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(stream, null);
        return parser;
    }

    private void parseGpx() throws Exception {
        eventType = parser.getEventType();

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                switch (parser.getName()) {
                    case "trk":
                        final GpxTrack track = parseTrack();
                        if (!track.isEmpty()) {
                            fileBuilder.withTrack(track);
                        }
                        continue;
                    case "wpt":
                        fileBuilder.withWaypoints(parseWaypoint());
                        continue;
                    case "metadata":
                        parseMetadata();
                        continue;
                }
            }

            eventType = parser.next();
        }
    }

    private GpxTrack parseTrack() throws Exception {
        final GpxTrack.Builder trackBuilder = new GpxTrack.Builder();

        while (eventType != XmlPullParser.END_TAG || !parser.getName().equals("trk")) {
            if (eventType == XmlPullParser.START_TAG) {
                switch (parser.getName()) {
                    case "trkseg":
                        final GpxTrackSegment segment = parseTrackSegment();
                        if (!segment.getTrackPoints().isEmpty()) {
                            trackBuilder.withTrackSegment(segment);
                        }
                        continue;
                }
            }

            eventType = parser.next();
        }

        return trackBuilder.build();
    }

    private GpxTrackSegment parseTrackSegment() throws Exception {
        final GpxTrackSegment.Builder segmentBuilder = new GpxTrackSegment.Builder();

        while (eventType != XmlPullParser.END_TAG || !parser.getName().equals("trkseg")) {
            if (eventType == XmlPullParser.START_TAG) {
                switch (parser.getName()) {
                    case "trkpt":
                        final GpxTrackPoint trackPoint = parseTrackPoint();
                        segmentBuilder.withTrackPoint(trackPoint);
                        continue;
                }
            }

            eventType = parser.next();
        }

        return segmentBuilder.build();
    }

    private String parseStringContent(final String tag) throws Exception {
        String retString = "";
        while (eventType != XmlPullParser.END_TAG || !parser.getName().equals(tag)) {
            if (eventType == XmlPullParser.TEXT) {
                retString = parser.getText();
            }
            eventType = parser.next();
        }
        return retString;
    }

    private double parseElevation() throws Exception {
        return Double.parseDouble(parseStringContent("ele"));
    }

    private Date parseTime() throws Exception {
        return ISO8601Utils.parse(parseStringContent("time"), new ParsePosition(0));
    }

    private GpxTrackPoint parseTrackPoint() throws Exception {
        final GpxTrackPoint.Builder trackPointBuilder = new GpxTrackPoint.Builder();

        final String latString = parser.getAttributeValue(null, "lat");
        final String lonString = parser.getAttributeValue(null, "lon");
        trackPointBuilder.withLatitude(latString != null ? Double.parseDouble(latString) : 0);
        trackPointBuilder.withLongitude(lonString != null ? Double.parseDouble(lonString) : 0);

        while (eventType != XmlPullParser.END_TAG || !parser.getName().equals("trkpt")) {
            if (parser.getEventType() == XmlPullParser.START_TAG) {
                switch (parser.getName()) {
                    case "ele":
                        trackPointBuilder.withAltitude(parseElevation());
                        continue;
                    case "time":
                        trackPointBuilder.withTime(parseTime());
                        continue;
                }
            } 

            eventType = parser.next();
        }

        return trackPointBuilder.build();
    }

    private GpxWaypoint parseWaypoint() throws Exception {
        final GpxWaypoint.Builder waypointBuilder = new GpxWaypoint.Builder();

        final String latString = parser.getAttributeValue(null, "lat");
        final String lonString = parser.getAttributeValue(null, "lon");
        waypointBuilder.withLatitude(latString != null ? Double.parseDouble(latString) : 0);
        waypointBuilder.withLongitude(lonString != null ? Double.parseDouble(lonString) : 0);

        while (eventType != XmlPullParser.END_TAG || !parser.getName().equals("wpt")) {
            if (parser.getEventType() == XmlPullParser.START_TAG) {
                switch (parser.getName()) {
                    case "ele":
                        waypointBuilder.withAltitude(parseElevation());
                        continue;
                    case "name":
                        waypointBuilder.withName(parseStringContent("name"));
                        continue;
                }
            }

            eventType = parser.next();
        }

        return waypointBuilder.build();
    }

    private void parseMetadata() throws Exception {
        while (eventType != XmlPullParser.END_TAG || !parser.getName().equals("metadata")) {
            if (parser.getEventType() == XmlPullParser.START_TAG) {
                switch (parser.getName()) {
                    case "name":
                        fileBuilder.withName(parseStringContent("name"));
                        continue;
                    case "author":
                        // TODO parse author
                        break;
                }
            }

            eventType = parser.next();
        }
    }
}