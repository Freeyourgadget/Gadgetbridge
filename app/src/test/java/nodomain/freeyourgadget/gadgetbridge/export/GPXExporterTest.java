package nodomain.freeyourgadget.gadgetbridge.export;

import com.google.gson.internal.bind.util.ISO8601Utils;

import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.export.ActivityTrackExporter.GPXTrackEmptyException;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityPoint;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityTrack;
import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate;
import nodomain.freeyourgadget.gadgetbridge.test.TestBase;

public class GPXExporterTest extends TestBase {
    @Test
    public void shouldCreateValidGpxFromSimulatedData() throws IOException, ParseException, GPXTrackEmptyException, SAXException {
        final List<ActivityPoint> points = readActivityPoints("/GPXExporterTest-SampleTracks.csv");

        final GPXExporter gpxExporter = new GPXExporter();
        gpxExporter.setCreator("Gadgetbridge Test");
        final ActivityTrack track = createTestTrack(points);

        final File tempFile = File.createTempFile("gpx-exporter-test-track", ".gpx");
        tempFile.deleteOnExit();

        gpxExporter.performExport(track, tempFile);
        validateGpxFile(tempFile);
    }

    @Test
    public void shouldCreateValidGpxFromSimulatedDataWithHeartrate() throws IOException, ParseException, GPXTrackEmptyException, ParserConfigurationException, SAXException {
        final List<ActivityPoint> points = readActivityPoints("/GPXExporterTest-SampleTracksHR.csv");

        final GPXExporter gpxExporter = new GPXExporter();
        gpxExporter.setCreator("Gadgetbridge Test");
        final ActivityTrack track = createTestTrack(points);

        final File tempFile = File.createTempFile("gpx-exporter-test-track", ".gpx");
        tempFile.deleteOnExit();

        gpxExporter.performExport(track, tempFile);
        validateGpxFile(tempFile);
    }

    private ActivityTrack createTestTrack(List<ActivityPoint> points) {
        final User user = new User();
        user.setName("Test User");

        Device device = new Device();
        device.setName("Test Device");

        final ActivityTrack track = new ActivityTrack();
        track.setName("Test Track");
        track.setBaseTime(new Date());
        track.setUser(user);
        track.setDevice(device);

        for (final ActivityPoint point : points) {
            track.addTrackPoint(point);
        }
        return track;
    }

    private List<ActivityPoint> readActivityPoints(String resourcePath) throws IOException, ParseException {
        final List<ActivityPoint> points = new ArrayList<>();
        try (final InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
            try (final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String nextLine = reader.readLine();
                while (nextLine != null) {
                    final String[] pieces = nextLine.split("\\s+");
                    final ActivityPoint point = new ActivityPoint();
                    point.setLocation(new GPSCoordinate(
                            Double.parseDouble(pieces[0]),
                            Double.parseDouble(pieces[1]),
                            Double.parseDouble(pieces[2]))
                    );

                    final int dateIndex;
                    if (pieces.length == 5) {
                        point.setHeartRate(Integer.parseInt(pieces[3]));
                        dateIndex = 4;
                    } else {
                        dateIndex = 3;
                    }
                    // Not sure about this parser but seemed safe to use
                    point.setTime(ISO8601Utils.parse(pieces[dateIndex], new ParsePosition(0)));

                    points.add(point);
                    nextLine = reader.readLine();
                }
            }
        }
        return points;
    }

    private void validateGpxFile(File tempFile) throws SAXException, IOException {
        final Source xmlFile = new StreamSource(tempFile);
        final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        final Schema schema = schemaFactory.newSchema(new StreamSource(getClass().getResourceAsStream("/gpx.xsd")));
        final Validator validator = schema.newValidator();
        validator.validate(xmlFile);
    }
}