package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.FileType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.enums.GarminSport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitRecordDataFactory;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;
import nodomain.freeyourgadget.gadgetbridge.util.gpx.GpxParser;
import nodomain.freeyourgadget.gadgetbridge.util.gpx.model.GpxFile;
import nodomain.freeyourgadget.gadgetbridge.util.gpx.model.GpxTrack;
import nodomain.freeyourgadget.gadgetbridge.util.gpx.model.GpxTrackPoint;
import nodomain.freeyourgadget.gadgetbridge.util.gpx.model.GpxTrackSegment;

public class GpxRouteFileConverter {
    private static final Logger LOG = LoggerFactory.getLogger(GpxRouteFileConverter.class);
    final double speed = 1.4; // m/s // TODO: make this configurable (and activity dependent?)
    final int activity = GarminSport.RUN.getType(); //TODO: make this configurable
    private final long timestamp;
    private final GpxFile gpxFile;
    private FitFile convertedFile;
    private String name;

    public GpxRouteFileConverter(byte[] xmlBytes) {
        this.timestamp = System.currentTimeMillis() / 1000;
        this.gpxFile = GpxParser.parseGpx(xmlBytes);
        try {
            this.convertedFile = convertGpxToRoute(gpxFile);
        } catch (Exception e) {
            LOG.error(e.getMessage());
            this.convertedFile = null;
        }
    }

    private static RecordData getFileCreatorRecordData() {
        final RecordData fileCreatorRecord = FitRecordDataFactory.create(
                new RecordDefinition(new RecordHeader((byte) 0x41), ByteOrder.BIG_ENDIAN, GlobalFITMessage.FILE_CREATOR, GlobalFITMessage.FILE_CREATOR.getFieldDefinitions(0), null),
                new RecordHeader((byte) 0x01));
        fileCreatorRecord.setFieldByName("software_version", 1);
        return fileCreatorRecord;
    }

    public FitFile getConvertedFile() {
        return convertedFile;
    }

    public boolean isValid() {
        return this.convertedFile != null;
    }

    public String getName() {
        if (gpxFile == null) {
            return "";
        }

        if (!StringUtils.isNullOrEmpty(this.name))
            return this.name;

        if (!StringUtils.isNullOrEmpty(gpxFile.getName())) {
            return gpxFile.getName();
        } else {
            return String.valueOf(timestamp);
        }
    }

    private FitFile convertGpxToRoute(GpxFile gpxFile) {
        if (gpxFile.getTracks().isEmpty()) {
            LOG.error("Gpx file contains no Tracks.");
            return null;
        }
        //GPX files may contain multiple tracks, we use only the first one
        final GpxTrack track = gpxFile.getTracks().get(0);

        if (track.getTrackSegments().isEmpty()) {
            LOG.error("Gpx track contains no segment.");
            return null;
        }
        //GPX track may contain multiple segments, we use only the first one
        GpxTrackSegment gpxTrackSegment = track.getTrackSegments().get(0);

        List<GpxTrackPoint> gpxTrackPointList = gpxTrackSegment.getTrackPoints();
        if (gpxTrackPointList.isEmpty()) {
            LOG.error("Gpx track segment contains no point");
            return null;
        }

        this.name = track.getName();

        final RecordHeader gpxDataPointRecordHeader = new RecordHeader((byte) 0x05);
        final RecordDefinition gpxDataPointRecordDefinition = new RecordDefinition(new RecordHeader((byte) 0x45), ByteOrder.BIG_ENDIAN, GlobalFITMessage.RECORD, GlobalFITMessage.RECORD.getFieldDefinitions(0, 1, 2, 5, 253), null);
        List<RecordData> gpxPointDataRecords = new ArrayList<>();

        double totalAscent = 0;
        double totalDescent = 0;
        double totalDistance = 0;
        long runningTs = timestamp;

        GPSCoordinate prevPoint = gpxTrackPointList.get(0);

        for (GPSCoordinate point :
                gpxTrackPointList) {
            totalAscent += point.getAscent(prevPoint);
            totalDescent += point.getDescent(prevPoint);
            totalDistance += point.getDistance(prevPoint);
            runningTs += (long) (point.getDistance(prevPoint) / speed);
            final RecordData gpxDataPointRecord = FitRecordDataFactory.create(gpxDataPointRecordDefinition, gpxDataPointRecordHeader);

            gpxDataPointRecord.setFieldByName("latitude", point.getLatitude());
            gpxDataPointRecord.setFieldByName("longitude", point.getLongitude());
            gpxDataPointRecord.setFieldByName("altitude", point.getAltitude());
            gpxDataPointRecord.setFieldByName("distance", totalDistance);
            gpxDataPointRecord.setFieldByName("timestamp", runningTs);

            prevPoint = point;
            gpxPointDataRecords.add(gpxDataPointRecord);
        }

        final RecordData lapRecord = getLapRecordData(gpxTrackPointList);
        lapRecord.setFieldByName("total_distance", totalDistance);
        lapRecord.setFieldByName("total_ascent", totalAscent);
        lapRecord.setFieldByName("total_descent", totalDescent);
        lapRecord.setFieldByName("total_elapsed_time", (runningTs - timestamp));
        lapRecord.setFieldByName("total_timer_time", (runningTs - timestamp));

        final List<RecordData> courseFileDataRecords = new ArrayList<>();
        courseFileDataRecords.add(getFileIdRecordData());
        courseFileDataRecords.add(getFileCreatorRecordData());
        courseFileDataRecords.add(getCourseRecordData());
        courseFileDataRecords.add(lapRecord);

        final RecordHeader eventRecordHeader = new RecordHeader((byte) 0x04);
        final RecordDefinition eventRecordDefinition = new RecordDefinition(new RecordHeader((byte) 0x44), ByteOrder.BIG_ENDIAN, GlobalFITMessage.EVENT, GlobalFITMessage.EVENT.getFieldDefinitions(0, 1, 4, 253), null);
        courseFileDataRecords.add(getEventRecordData(eventRecordDefinition, eventRecordHeader, timestamp, 0));
        courseFileDataRecords.add(getEventRecordData(eventRecordDefinition, eventRecordHeader, runningTs, 9));

        courseFileDataRecords.addAll(gpxPointDataRecords);

        return new FitFile(courseFileDataRecords);
    }

    private RecordData getEventRecordData(RecordDefinition eventRecordDefinition, RecordHeader eventRecordHeader, long timestamp, int eventType) {
        final RecordData startEvent = FitRecordDataFactory.create(
                eventRecordDefinition,
                eventRecordHeader);

        startEvent.setFieldByName("timestamp", timestamp);
        startEvent.setFieldByName("event", 0);
        startEvent.setFieldByName("event_group", 0);
        startEvent.setFieldByName("event_type", eventType);
        return startEvent;
    }

    private RecordData getLapRecordData(List<GpxTrackPoint> gpxTrackPointList) {
        final GPSCoordinate first = gpxTrackPointList.get(0);
        final GPSCoordinate last = gpxTrackPointList.get(gpxTrackPointList.size() - 1);

        final RecordData lapRecord = FitRecordDataFactory.create(
                new RecordDefinition(new RecordHeader((byte) 0x43), ByteOrder.BIG_ENDIAN, GlobalFITMessage.LAP, GlobalFITMessage.LAP.getFieldDefinitions(3, 4, 5, 6, 7, 8, 9, 21, 22, 253), null),
                new RecordHeader((byte) 0x03));
        lapRecord.setFieldByName("start_lat", first.getLatitude());
        lapRecord.setFieldByName("start_long", first.getLongitude());
        lapRecord.setFieldByName("end_lat", last.getLatitude());
        lapRecord.setFieldByName("end_long", last.getLongitude());
        lapRecord.setFieldByName("timestamp", timestamp);
        return lapRecord;
    }

    private RecordData getCourseRecordData() {
        final RecordData courseRecord = FitRecordDataFactory.create(
                new RecordDefinition(new RecordHeader((byte) 0x42), ByteOrder.BIG_ENDIAN, GlobalFITMessage.COURSE, GlobalFITMessage.COURSE.getFieldDefinitions(4, 5), null),
                new RecordHeader((byte) 0x02));
        courseRecord.setFieldByName("sport", activity); //TODO use track.getType()
        courseRecord.setFieldByName("name", this.getName());
        return courseRecord;
    }

    private RecordData getFileIdRecordData() {
        final RecordData fileIdRecord = FitRecordDataFactory.create(
                new RecordDefinition(new RecordHeader((byte) 0x40), ByteOrder.BIG_ENDIAN, GlobalFITMessage.FILE_ID, GlobalFITMessage.FILE_ID.getFieldDefinitions(0, 1, 2, 3, 4, 5), null),
                new RecordHeader((byte) 0x00));
        fileIdRecord.setFieldByName("type", FileType.FILETYPE.COURSES.getSubType());
        fileIdRecord.setFieldByName("manufacturer", 1);
        fileIdRecord.setFieldByName("product", 65534);
        fileIdRecord.setFieldByName("time_created", timestamp);
        fileIdRecord.setFieldByName("serial_number", 1);
        fileIdRecord.setFieldByName("number", 1);
        return fileIdRecord;
    }

}
