package nodomain.freeyourgadget.gadgetbridge.test;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate;
import nodomain.freeyourgadget.gadgetbridge.util.GpxParser;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;

public class GPXParserTest extends TestBase {

    @Test
    public void shouldReadGPXCorrectly() throws IOException {
        try (final InputStream inputStream = getClass().getResourceAsStream("/gpx-exporter-test-SampleTrack.gpx")) {
            GpxParser gpxParser = new GpxParser(inputStream);
            List<GPSCoordinate> trackPoints = gpxParser.getPoints();
            Assert.assertEquals(trackPoints.size(), 14);
            DecimalFormat df = new DecimalFormat("###.##");
            for (GPSCoordinate tp : trackPoints) {
                Assert.assertEquals(df.format(tp.getLongitude()), "44.15");
                Assert.assertEquals(df.format(tp.getLatitude()), "-68.2");
                Assert.assertThat(df.format(tp.getAltitude()), anyOf(is("40"), is("46")));
            }
        }
    }
}
