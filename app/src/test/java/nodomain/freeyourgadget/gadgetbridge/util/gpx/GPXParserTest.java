package nodomain.freeyourgadget.gadgetbridge.util.gpx;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate;
import nodomain.freeyourgadget.gadgetbridge.test.TestBase;
import nodomain.freeyourgadget.gadgetbridge.util.gpx.model.GpxFile;
import nodomain.freeyourgadget.gadgetbridge.util.gpx.model.GpxTrackPoint;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;

public class GPXParserTest extends TestBase {

    @Test
    public void shouldReadGPXCorrectly() throws IOException, GpxParseException {
        try (final InputStream inputStream = getClass().getResourceAsStream("/gpx-exporter-test-SampleTrack.gpx")) {
            GpxParser gpxParser = new GpxParser(inputStream);
            List<GpxTrackPoint> trackPoints = gpxParser.getGpxFile().getPoints();
            Assert.assertEquals(trackPoints.size(), 14);
            DecimalFormat df = new DecimalFormat("###.##");
            for (GPSCoordinate tp : trackPoints) {
                Assert.assertEquals(df.format(tp.getLongitude()), "-68.2");
                Assert.assertEquals(df.format(tp.getLatitude()), "44.15");
                Assert.assertThat(df.format(tp.getAltitude()), anyOf(is("40"), is("46")));
            }
            Assert.assertEquals(
                    new GpxTrackPoint(-68.200293, 44.152462, 40, new Date(1546300800000L)),
                    trackPoints.get(0)
            );
        }
    }

    @Test
    public void shouldParseMultipleSegments() throws IOException, GpxParseException, ParseException {
        try (final InputStream inputStream = getClass().getResourceAsStream("/gpx-parser-test-multiple-segments.gpx")) {
            final GpxParser gpxParser = new GpxParser(inputStream);
            final GpxFile gpxFile = gpxParser.getGpxFile();
            Assert.assertEquals(1, gpxFile.getTracks().size());
            Assert.assertEquals(2, gpxFile.getTracks().get(0).getTrackSegments().size());

            final List<GpxTrackPoint> segment1 = new ArrayList<GpxTrackPoint>() {{
                add(new GpxTrackPoint(-8.2695876, -70.6666343, 790.0, new Date(1680969788000L), 123));
                add(new GpxTrackPoint(-8.2653274, -70.6670617, 296.0, new Date(1680970639000L), 56));
            }};

            final List<GpxTrackPoint> segment2 = new ArrayList<GpxTrackPoint>() {{
                add(new GpxTrackPoint(-8.2653274, -70.6670617, 205.0, new Date(1680971684000L), 85));
                add(new GpxTrackPoint(-8.2695876, -70.6666343, 209.0, new Date(1680973017000L), 150));
            }};

            Assert.assertEquals(gpxFile.getTracks().get(0).getTrackSegments().get(0).getTrackPoints(), segment1);
            Assert.assertEquals(gpxFile.getTracks().get(0).getTrackSegments().get(1).getTrackPoints(), segment2);
        }
    }

    @Test
    public void shouldParseOutOfOrder() throws IOException, GpxParseException {
        try (final InputStream inputStream = getClass().getResourceAsStream("/gpx-parser-test-order.gpx")) {
            final GpxParser gpxParser = new GpxParser(inputStream);
            final GpxFile gpxFile = gpxParser.getGpxFile();
            Assert.assertEquals(1, gpxFile.getTracks().size());
            Assert.assertEquals(1, gpxFile.getTracks().get(0).getTrackSegments().size());

            final List<GpxTrackPoint> segment1 = new ArrayList<GpxTrackPoint>() {{
                add(new GpxTrackPoint(-8.2695876, -70.6666343, 790.0, new Date(1680969788000L), 123));
                add(new GpxTrackPoint(-8.2653274, -70.6670617, 296.0, new Date(1680970639000L), 56));
            }};

            Assert.assertEquals(gpxFile.getTracks().get(0).getTrackSegments().get(0).getTrackPoints(), segment1);
        }
    }
}
