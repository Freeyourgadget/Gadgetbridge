package nodomain.freeyourgadget.gadgetbridge.export;

import android.support.annotation.NonNull;
import android.util.Xml;

import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.activities.HeartRateUtils;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityPoint;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityTrack;
import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;

public class GPXExporter implements ActivityTrackExporter {
    private static final String NS_DEFAULT = "";
    private static final String NS_DEFAULT_URI = "http://www.topografix.com/GPX/1/1";
    private static final String NS_DEFAULT_PREFIX = "";
    private static final String NS_TRACKPOINT_EXTENSION = "gpxtpx";
    private static final String NS_TRACKPOINT_EXTENSION_URI = "http://www.garmin.com/xmlschemas/TrackPointExtension/v1";
    private static final String NS_XSI_URI = "http://www.w3.org/2001/XMLSchema-instance";

    private String creator;
    private boolean includeHeartRate = true;

    @NonNull
    @Override
    public String getDefaultFileName(@NonNull ActivityTrack track) {
        return FileUtils.makeValidFileName(track.getName());
    }

    @Override
    public void performExport(ActivityTrack track, File targetFile) throws IOException {
        String encoding = StandardCharsets.UTF_8.name();
        XmlSerializer ser = Xml.newSerializer();
        try {
            ser.setOutput(new FileOutputStream(targetFile), encoding);
            ser.startDocument(encoding, Boolean.TRUE);
            ser.setPrefix("xsi", NS_XSI_URI);
            ser.setPrefix(NS_DEFAULT_PREFIX, NS_DEFAULT);

            ser.startTag(NS_DEFAULT, "gpx");
            ser.attribute(NS_DEFAULT, "version", "1.1");
            ser.attribute(NS_DEFAULT, "creator", getCreator());
            ser.attribute(NS_XSI_URI, "schemaLocation", NS_DEFAULT_URI + " " + "http://www.topografix.com/GPX/1/1/gpx.xsd");

            exportMetadata(ser, track);
            exportTrack(ser, track);

            ser.endTag(NS_DEFAULT, "gpx");
            ser.endDocument();
        } finally {
            ser.flush();
        }
    }

    private void exportMetadata(XmlSerializer ser, ActivityTrack track) throws IOException {
        ser.startTag(NS_DEFAULT, "metadata");
        ser.startTag(NS_DEFAULT, "name").text(track.getName()).endTag(NS_DEFAULT, "name");

        ser.startTag(NS_DEFAULT, "author");
        ser.startTag(NS_DEFAULT, "name").text(track.getUser().getName()).endTag(NS_DEFAULT, "name");
        ser.endTag(NS_DEFAULT, "author");

        ser.startTag(NS_DEFAULT, "time").text(formatTime(new Date())).endTag(NS_DEFAULT, "time");

        ser.endTag(NS_DEFAULT, "metadata");
    }

    private String formatTime(Date date) {
        return DateTimeUtils.formatIso8601(date);
    }

    private void exportTrack(XmlSerializer ser, ActivityTrack track) throws IOException {
        ser.startTag(NS_DEFAULT, "trk");
        ser.startTag(NS_DEFAULT, "trkseg");

        List<ActivityPoint> trackPoints = track.getTrackPoints();
        String source = getSource(track);
        for (ActivityPoint point : trackPoints) {
            exportTrackPoint(ser, point, source);
        }

        ser.endTag(NS_DEFAULT, "trkseg");
        ser.endTag(NS_DEFAULT, "trk");
    }

    private String getSource(ActivityTrack track) {
        return track.getDevice().getName();
    }

    private void exportTrackPoint(XmlSerializer ser, ActivityPoint point, String source) throws IOException {
        GPSCoordinate location = point.getLocation();
        if (location == null) {
            return; // skip invalid points, that just contain hr data, for example
        }
        ser.startTag(NS_DEFAULT, "trkpt");
        ser.attribute(NS_DEFAULT, "lon", formatLocation(location.getLongitude()));
        ser.attribute(NS_DEFAULT, "lat", formatLocation(location.getLatitude()));
        ser.startTag(NS_DEFAULT, "ele").text(formatLocation(location.getAltitude())).endTag(NS_DEFAULT, "ele");
        ser.startTag(NS_DEFAULT, "time").text(formatTime(point.getTime())).endTag(NS_DEFAULT, "time");
        String description = point.getDescription();
        if (description != null) {
            ser.startTag(NS_DEFAULT, "desc").text(description).endTag(NS_DEFAULT, "desc");
        }
        ser.startTag(NS_DEFAULT, "src").text(source).endTag(NS_DEFAULT, "src");

        exportTrackpointExtensions(ser, point);

        ser.endTag(NS_DEFAULT, "trkpt");
    }

    private void exportTrackpointExtensions(XmlSerializer ser, ActivityPoint point) throws IOException {
        if (!includeHeartRate) {
            return;
        }

        int hr = point.getHeartRate();
        if (!HeartRateUtils.isValidHeartRateValue(hr)) {
            return;
        }
        ser.startTag(NS_DEFAULT, "extensions");

        ser.setPrefix(NS_TRACKPOINT_EXTENSION, NS_TRACKPOINT_EXTENSION_URI);
        ser.startTag(NS_TRACKPOINT_EXTENSION_URI, "hr").text(String.valueOf(hr)).endTag(NS_TRACKPOINT_EXTENSION_URI, "hr");

        ser.endTag(NS_DEFAULT, "extensions");
    }

    private String formatLocation(double value) {
        return new BigDecimal(value).setScale(GPSCoordinate.GPS_DECIMAL_DEGREES_SCALE, RoundingMode.HALF_UP).toPlainString();
    }

    public String getCreator() {
        return creator; // TODO: move to some kind of BrandingInfo class
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public void setIncludeHeartRate(boolean includeHeartRate) {
        this.includeHeartRate = includeHeartRate;
    }

    public boolean isIncludeHeartRate() {
        return includeHeartRate;
    }
}
