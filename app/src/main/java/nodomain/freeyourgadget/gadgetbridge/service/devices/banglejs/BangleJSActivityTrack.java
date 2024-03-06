package nodomain.freeyourgadget.gadgetbridge.service.devices.banglejs;

import static java.lang.Math.cos;
import static java.lang.Math.sqrt;

import android.content.Context;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.export.ActivityTrackExporter;
import nodomain.freeyourgadget.gadgetbridge.export.GPXExporter;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityPoint;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityTrack;
import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

class BangleJSActivityTrack extends BangleJSDeviceSupport {

    private static final Logger LOG = LoggerFactory.getLogger(BangleJSActivityTrack.class);

    static JSONObject compileTracksListRequest(GBDevice device, Context context) {
        stopAndRestartTimeout(device, context);
        signalFetchingStarted(device, context);
        //GB.toast("TYPE_GPS_TRACKS says hi!", Toast.LENGTH_LONG, GB.INFO);

        String lastSyncedID = getLatestFetchedRecorderLog();

        JSONObject o = new JSONObject();
        try {
            o.put("t", "listRecs");
            o.put("id", lastSyncedID);
            //uartTxJSON("requestActivityTracksList", o);
        } catch (JSONException e) {
            LOG.error("JSONException: " + e.getLocalizedMessage());
        }

        return o;
    }

    static JSONArray handleActTrksList(JSONObject json, GBDevice device, Context context) throws JSONException {
        stopAndRestartTimeout(device, context);
        LOG.debug("trksList says hi!");
        //GB.toast(getContext(), "trksList says hi!", Toast.LENGTH_LONG, GB.INFO);
        JSONArray tracksList = json.getJSONArray("list");
        LOG.info("New recorder logs since last fetch: " + String.valueOf(tracksList));
        if (tracksList.length()==0) {
            signalFetchingEnded(device, context);
            return null;
        } else {
            return tracksList;
        }
    }

    static JSONObject compileTrackRequest(String id, Boolean isLastId) {
        JSONObject o = new JSONObject();
        try {
            o.put("t", "fetchRec");
            o.put("id", id);
            o.put("last", String.valueOf(isLastId));
        } catch (JSONException e) {
            LOG.error("JSONException: " + e.getLocalizedMessage());
        }
       return o; 
    }

    static JSONArray handleActTrk(JSONObject json, JSONArray tracksList, int prevPacketCount, GBDevice device, Context context) throws JSONException {
        stopAndRestartTimeout(device, context);

        JSONArray returnArray;

        JSONObject stopObj = new JSONObject().put("t","fetchRec").put("id","stop");
        int currPacketCount;
        if (json.has("cnt")) {
            currPacketCount = json.getInt("cnt");
        } else {
            currPacketCount = 0;
        }
        if (currPacketCount != prevPacketCount+1) {
            LOG.error("Activity Track Packets came out of order - aborting.");
            LOG.debug("packetCount Aborting: " + prevPacketCount);
            returnArray = new JSONArray().put(stopObj).put(tracksList).put(prevPacketCount);
            signalFetchingEnded(device, context);
            stopTimeoutTask();
            return returnArray;
        }

        LOG.debug("actTrk says hi!");
        //GB.toast(context, "actTrk says hi!", Toast.LENGTH_LONG, GB.INFO);
        String log = json.getString("log");
        LOG.debug(log);
        String filename = "recorder.log" + log + ".csv";
        File dir;
        try {
            dir = FileUtils.getExternalFilesDir();
        } catch (IOException e) {
            returnArray = new JSONArray().put(null).put(tracksList).put(currPacketCount);
            return returnArray;
        }

        if (!json.has("lines")) { // if no lines were sent with this json object, it signifies that the whole recorder log has been transmitted.
            setLatestFetchedRecorderLog(dir, log);
            parseFetchedRecorderCSV(dir, filename, log, device, context); // I tried refactoring to parse all fetched logs in one go at the end instead. But that only gave me more troubles. This seems like a more stable approach at least in the Bangle.js case.
            if (tracksList.length()==0) {
                signalFetchingEnded(device, context);
                int resetPacketCount = -1;
                LOG.debug("packetCount reset1: " + resetPacketCount);
                returnArray = new JSONArray().put(null).put(tracksList).put(resetPacketCount);
            } else {
                JSONObject requestTrackObj = BangleJSActivityTrack.compileTrackRequest(tracksList.getString(0), 1==tracksList.length());
                tracksList.remove(0);
                int resetPacketCount = -1;
                LOG.debug("packetCount reset2: " + resetPacketCount);
                returnArray = new JSONArray().put(requestTrackObj).put(tracksList).put(resetPacketCount);
            }
        } else { // We received a lines of the csv, now we append it to the file in storage.

            String lines = json.getString("lines");
            LOG.debug(lines);

            writeToRecorderCSV(lines, dir, filename);

            LOG.debug("packetCount continue: " + currPacketCount);
            returnArray = new JSONArray().put(null).put(tracksList).put(currPacketCount);
        }

        return returnArray;
    }

    private static void parseFetchedRecorderCSV(File dir, String filename, String log, GBDevice device, Context context) {
        stopTimeoutTask(); // Parsing can take a while if there are many data. Restart at end of parsing.

        File inputFile = new File(dir, filename);
        try { // FIXME: There is maybe code inside this try-statement that should be outside of it.

            // Read from the previously stored log into a string.
            BufferedReader reader = new BufferedReader(new FileReader(inputFile));
            StringBuilder storedLogBuilder = new StringBuilder(reader.readLine() + "\n");
            String line;
            while ((line = reader.readLine()) != null) {
                storedLogBuilder.append(line).append("\n");
            }
            reader.close();
            String storedLog = String.valueOf(storedLogBuilder);
            storedLog = storedLog.replace(",",", "); // So all rows (internal arrays) in storedLogArray2 get the same number of entries.
            LOG.debug("Contents of log read from GB storage:\n" + storedLog);

            // Turn the string log into a 2d array in two steps.
            String[] storedLogArray = storedLog.split("\n") ;
            String[][] storedLogArray2 = new String[storedLogArray.length][1];

            for (int i = 0; i < storedLogArray.length; i++) {
                storedLogArray2[i] = storedLogArray[i].split(",");
                for (int j = 0; j < storedLogArray2[i].length;j++) {
                    storedLogArray2[i][j] = storedLogArray2[i][j].trim(); // Remove the extra spaces we introduced above for getting the same number of entries on all rows.
                }
            }

            LOG.debug("Contents of storedLogArray2:\n" + Arrays.deepToString(storedLogArray2));

            // Turn the 2d array into an object for easier access later on.
            JSONObject storedLogObject = new JSONObject();
            JSONArray valueArray = new JSONArray();
            for (int i = 0; i < storedLogArray2[0].length; i++){
                for (int j = 1; j < storedLogArray2.length; j++) {
                    valueArray.put(storedLogArray2[j][i]);
                }
                storedLogObject.put(storedLogArray2[0][i], valueArray);
                valueArray = new JSONArray();
            }

            // Clean out heartrate==0...
            if (storedLogObject.has("Heartrate")) {
                JSONArray heartrateArray = storedLogObject.getJSONArray("Heartrate");
                for (int i = 0; i < heartrateArray.length(); i++){
                    if (Objects.equals(heartrateArray.getString(i), "0") ||
                            Objects.equals(heartrateArray.getString(i), "0.0")) {
                        heartrateArray.put(i,"");
                    }
                }
                //storedLogObject.remove("Heartrate");
                storedLogObject.put("Heartrate", heartrateArray);

            }

            LOG.debug("storedLogObject:\n" + storedLogObject);

            // Calculate and store analytical data (distance, speed, cadence, etc.).
            JSONObject analyticsObject = new JSONObject();
            JSONArray calculationsArray = new JSONArray();
            int logLength = storedLogObject.getJSONArray("Time").length();

            // Add elapsed time since first reading (seconds).
            valueArray = storedLogObject.getJSONArray("Time");
            for (int i = 0; i < logLength; i++) {
                calculationsArray.put(valueArray.getDouble(i)-valueArray.getDouble(0));
            }
            analyticsObject.put("Elapsed Time", calculationsArray);

            valueArray = new JSONArray();
            calculationsArray = new JSONArray();

            JSONArray valueArray2 = new JSONArray();

            //LOG.debug("check here 0");
            // Add analytics based on GPS coordinates.
            if (storedLogObject.has("Latitude")) {
                // Add distance between last and current reading.
                valueArray = storedLogObject.getJSONArray("Latitude");
                valueArray2 = storedLogObject.getJSONArray("Longitude");
                for (int i = 0; i < logLength; i++) {
                    if (i == 0) {
                        calculationsArray.put("0");
                    } else {
                        String distance;
                        if (Objects.equals(valueArray.getString(i), "") ||
                                Objects.equals(valueArray.getString(i - 1), "")) {
                            // FIXME: GPS data can be missing for some entries which is handled here.
                            // Should use more complex logic to be more accurate. Use interpolation.
                            // Should distances be done via the GPX file we generate instead?
                            distance = "0";
                        } else {
                            distance = distanceFromCoordinatePairs(
                                    (String) valueArray.get(i - 1),
                                    (String) valueArray2.get(i - 1),
                                    (String) valueArray.get(i),
                                    (String) valueArray2.get(i)
                            );
                        }
                        calculationsArray.put(distance);
                    }
                }
                analyticsObject.put("Intermediate Distance", calculationsArray);

                valueArray = new JSONArray();
                valueArray2 = new JSONArray();
                calculationsArray = new JSONArray();

                //LOG.debug("check here 1");
                // Add stride lengths between consecutive readings.
                if (storedLogObject.has("Steps")) {
                    for (int i = 0; i < logLength; i++) {
                        if (Objects.equals(storedLogObject.getJSONArray("Steps").getString(i), "0") ||
                                Objects.equals(storedLogObject.getJSONArray("Steps").getString(i), "")) {
                            calculationsArray.put("");
                        } else if (Objects.equals(analyticsObject.getJSONArray("Intermediate Distance").getString(i), "0")) {
                            calculationsArray.put("0");
                        } else {
                            double steps = storedLogObject.getJSONArray("Steps").getDouble(i);
                            double calculation =
                                    analyticsObject.getJSONArray("Intermediate Distance").getDouble(i) / steps;
                            calculationsArray.put(calculation);
                        }
                    }
                    analyticsObject.put("Stride", calculationsArray);

                    calculationsArray = new JSONArray();
                }

                //LOG.debug("check here 2");
            } else if (storedLogObject.has("Steps")) {
                for (int i = 0; i < logLength; i++) {
                    if (i==0 ||
                            Objects.equals(storedLogObject.getJSONArray("Steps").getString(i), "0") ||
                            Objects.equals(storedLogObject.getJSONArray("Steps").getString(i), "")) {
                        calculationsArray.put(0);
                    } else {
                        double avgStep = (0.67+0.762)/2; // https://marathonhandbook.com/average-stride-length/  (female+male)/2
                        double stride = 2*avgStep; // TODO: Depend on user defined stride length?
                        double calculation = stride * (storedLogObject.getJSONArray("Steps").getDouble(i));
                        //if (calculation == 0) calculation = 0.001; // To avoid potential division by zero later on.
                        calculationsArray.put(calculation);
                    }
                }
                analyticsObject.put("Intermediate Distance", calculationsArray);

                calculationsArray = new JSONArray();

            }

            //LOG.debug("check here 3");
            if (analyticsObject.has("Intermediate Distance")) {
                // Add total distance from start of activity up to each reading.
                for (int i = 0; i < logLength; i++) {
                    if (i==0) {
                        calculationsArray.put(0);
                    } else {
                        double calculation = calculationsArray.getDouble(i-1) + analyticsObject.getJSONArray("Intermediate Distance").getDouble(i);
                        calculationsArray.put(calculation);
                    }
                }
                analyticsObject.put("Total Distance", calculationsArray);

                calculationsArray = new JSONArray();

                //LOG.debug("check here 4");
                // Add average speed between last and current reading (m/s).
                for (int i = 0; i < logLength; i++) {
                    if (i==0) {
                        calculationsArray.put("");
                    } else {
                        double timeDiff =
                                (analyticsObject.getJSONArray("Elapsed Time").getDouble(i) -
                                        analyticsObject.getJSONArray("Elapsed Time").getDouble(i-1));
                        if (timeDiff==0) timeDiff = 1; // On older versions of the Recorder Bangle.js app the time reporting could be the same for two data points due to rounding.
                        double calculation =
                                analyticsObject.getJSONArray("Intermediate Distance").getDouble(i) / timeDiff;
                        calculationsArray.put(calculation);
                    }
                }
                //LOG.debug("check " + calculationsArray);
                analyticsObject.put("Speed", calculationsArray);

                calculationsArray = new JSONArray();

                //LOG.debug("check here 5");
                // Add average pace between last and current reading (s/km). (Was gonna do this as min/km but summary seems to expect s/km).
                for (int i = 0; i < logLength; i++) {
                    String speed = analyticsObject.getJSONArray("Speed").getString(i);
                    //LOG.debug("check: " + speed);
                    if (i==0 || Objects.equals(speed, "0") || Objects.equals(speed, "0.0") || Objects.equals(speed, "")) {
                        calculationsArray.put("");
                    } else {
                        double calculation = (1000.0) * 1/ analyticsObject.getJSONArray("Speed").getDouble(i);
                        calculationsArray.put(calculation);
                    }
                }
                analyticsObject.put("Pace", calculationsArray);

                calculationsArray = new JSONArray();
            }

            //LOG.debug("check here 6");
            if (storedLogObject.has("Steps")) {
                for (int i = 0; i < logLength; i++) {
                    if (i==0 || Objects.equals(storedLogObject.getJSONArray("Steps").getString(i), "")) {
                        calculationsArray.put(0);
                    } else {
                        // FIXME: Should cadence be steps/min or half that? https://www.polar.com/blog/what-is-running-cadence/
                        // The Bangle.js App Loader has Cadence = (steps/min)/2,  https://github.com/espruino/BangleApps/blob/master/apps/recorder/interface.html#L103,
                        // as discussed here: https://github.com/espruino/BangleApps/pull/3068#issuecomment-1790293879 .
                        double timeDiff =
                                (storedLogObject.getJSONArray("Time").getDouble(i) -
                                        storedLogObject.getJSONArray("Time").getDouble(i-1));
                        if (timeDiff==0) timeDiff = 1;
                        double calculation = 0.5 * 60 *
                                (storedLogObject.getJSONArray("Steps").getDouble(i) / timeDiff);
                        calculationsArray.put(calculation);
                    }
                }
                analyticsObject.put("Cadence", calculationsArray);

                calculationsArray = new JSONArray();
            }
            //LOG.debug("check here AnalyticsObject:\n" + analyticsObject.toString());

            //LOG.debug("check here 7");
            BaseActivitySummary summary = null;

            Date startTime = new Date(Long.parseLong(storedLogArray2[1][0].split("\\.\\d")[0])*1000L);
            Date endTime = new Date(Long.parseLong(storedLogArray2[storedLogArray2.length-1][0].split("\\.\\d")[0])*1000L);
            summary = new BaseActivitySummary();
            summary.setName(log);
            summary.setStartTime(startTime);
            summary.setEndTime(endTime);
            int activityKind;
            if (analyticsObject.has("Speed")) {
                if ((float) 3 > averageOfJSONArray(analyticsObject.getJSONArray("Speed"))) {
                    activityKind = ActivityKind.TYPE_WALKING;
                } else {
                    activityKind = ActivityKind.TYPE_RUNNING;
                }
            } else {
                activityKind = ActivityKind.TYPE_ACTIVITY;
            }
            summary.setActivityKind(activityKind); // TODO: Make this depend on info from watch (currently this info isn't supplied in Bangle.js recorder logs).
            summary.setRawDetailsPath(String.valueOf(inputFile));

            JSONObject summaryData = new JSONObject();
            //     put("Activity", Arrays.asList(
            //             "distanceMeters", "steps", "activeSeconds", "caloriesBurnt", "totalStride",
            //             "averageHR", "maxHR", "minHR", "averageStride", "maxStride", "minStride"
            //     ));
            if (analyticsObject.has("Intermediate Distance")) summaryData =
                    addSummaryData(summaryData, "distanceMeters",
                            (float) analyticsObject.getJSONArray("Total Distance").getDouble(logLength - 1),
                            "m");
            if (storedLogObject.has("Steps"))
                summaryData = addSummaryData(summaryData, "steps", sumOfJSONArray(storedLogObject.getJSONArray("Steps")), "steps");
            //summaryData = addSummaryData(summaryData,"activeSeconds",3,"mm"); // FIXME: Is this suppose to exclude the time of inactivity in a workout?
            //summaryData = addSummaryData(summaryData,"caloriesBurnt",3,"mm"); // TODO: Should this be calculated on Gadgetbridge side or be reported by Bangle.js?
            //summaryData = addSummaryData(summaryData,"totalStride",3,"mm"); // FIXME: What is this?
            if (storedLogObject.has("Heartrate")) {
                summaryData = addSummaryData(summaryData, "averageHR", averageOfJSONArray(storedLogObject.getJSONArray("Heartrate")), "bpm");
                summaryData = addSummaryData(summaryData, "maxHR", maxOfJSONArray(storedLogObject.getJSONArray("Heartrate")), "bpm");
                summaryData = addSummaryData(summaryData, "minHR", minOfJSONArray(storedLogObject.getJSONArray("Heartrate")), "bpm");
            }
            if (analyticsObject.has("Stride")) {
                summaryData = addSummaryData(summaryData, "averageStride",
                        (float) (analyticsObject.getJSONArray("Total Distance").getDouble(logLength - 1) /
                                (0.5 * sumOfJSONArray(storedLogObject.getJSONArray("Steps")))),
                        "m"); // FIXME: Is this meant to be stride length as I've assumed?
                summaryData = addSummaryData(summaryData, "maxStride", maxOfJSONArray(analyticsObject.getJSONArray("Stride")), "m");
                summaryData = addSummaryData(summaryData, "minStride", minOfJSONArray(analyticsObject.getJSONArray("Stride")), "m");
            }

            //     put("Speed", Arrays.asList(
            //             "averageSpeed", "maxSpeed", "minSpeed", "averageKMPaceSeconds", "minPace",
            //             "maxPace", "averageSpeed2", "averageCadence", "maxCadence", "minCadence"
            //     ));
            try {
                if (analyticsObject.has("Speed")) {
                    summaryData = addSummaryData(summaryData,"averageSpeed", averageOfJSONArray(analyticsObject.getJSONArray("Speed")),"m/s"); // This seems to be calculated somewhere else automatically.
                    summaryData = addSummaryData(summaryData, "maxSpeed", maxOfJSONArray(analyticsObject.getJSONArray("Speed")), "m/s");
                    summaryData = addSummaryData(summaryData, "minSpeed", minOfJSONArray(analyticsObject.getJSONArray("Speed")), "m/s");
                    summaryData = addSummaryData(summaryData, "averageKMPaceSeconds", averageOfJSONArray(analyticsObject.getJSONArray("Pace")), "s/km"); // Is this also calculated automatically then?
                    //summaryData = addSummaryData(summaryData, "averageKMPaceSeconds",
                    //        (float) (1000.0 * analyticsObject.getJSONArray("Elapsed Time").getDouble(logLength-1) /
                    //                analyticsObject.getJSONArray("Total Distance").getDouble(logLength-1)),
                    //        "s/km"
                    //);
                    summaryData = addSummaryData(summaryData, "minPace", maxOfJSONArray(analyticsObject.getJSONArray("Pace")), "s/km");
                    summaryData = addSummaryData(summaryData, "maxPace", minOfJSONArray(analyticsObject.getJSONArray("Pace")), "s/km");
                    //summaryData = addSummaryData(summaryData,"averageSpeed2",3,"mm");
                }
                if (analyticsObject.has("Cadence")) {
                    //summaryData = addSummaryData(summaryData, "averageCadence", averageOfJSONArray(analyticsObject.getJSONArray("Cadence")), "cycles/min"); // Is this also calculated automatically then?
                    summaryData = addSummaryData(summaryData, "averageCadence",
                            (float) 0.5 * 60 * sumOfJSONArray(storedLogObject.getJSONArray("Steps")) /
                                    (float) analyticsObject.getJSONArray("Elapsed Time").getDouble(logLength - 1),
                            "cycles/min"
                    );
                    summaryData = addSummaryData(summaryData, "maxCadence", maxOfJSONArray(analyticsObject.getJSONArray("Cadence")), "cycles/min");
                    summaryData = addSummaryData(summaryData, "minCadence", minOfJSONArray(analyticsObject.getJSONArray("Cadence")), "cycles/min");
                }
            } catch (Exception e) {
                LOG.error(e + ". (thrown when trying to add summary data");
            }
            //            private JSONObject createActivitySummaryGroups(){
            // final Map<String, List<String>> groupDefinitions = new HashMap<String, List<String>>() {{
            //     put("Strokes", Arrays.asList(
            //             "averageStrokeDistance", "averageStrokesPerSecond", "strokes"
            //     ));

            //     put("Swimming", Arrays.asList(
            //             "swolfIndex", "swimStyle"
            //     ));

            //     put("Elevation", Arrays.asList(
            //             "ascentMeters", "descentMeters", "maxAltitude", "minAltitude", "averageAltitude",
            //             "baseAltitude", "ascentSeconds", "descentSeconds", "flatSeconds", "ascentDistance",
            //             "descentDistance", "flatDistance", "elevationGain", "elevationLoss"
            //     ));
            //}
            if (storedLogObject.has("Altitude") || storedLogObject.has("Barometer Altitude")) {
                String altitudeToUseKey = null;
                if (storedLogObject.has("Altitude")) {
                    altitudeToUseKey = "Altitude";
                } else if (storedLogObject.has("Barometer Altitude")) {
                    altitudeToUseKey = "Barometer Altitude";
                }
                //summaryData = addSummaryData(summaryData, "ascentMeters", 3, "m");
                //summaryData = addSummaryData(summaryData, "descentMeters", 3, "m");
                summaryData = addSummaryData(summaryData, "maxAltitude", maxOfJSONArray(storedLogObject.getJSONArray(altitudeToUseKey)), "m");
                summaryData = addSummaryData(summaryData, "minAltitude", minOfJSONArray(storedLogObject.getJSONArray(altitudeToUseKey)), "m");
                summaryData = addSummaryData(summaryData, "averageAltitude", averageOfJSONArray(storedLogObject.getJSONArray(altitudeToUseKey)), "m");
                //summaryData = addSummaryData(summaryData, "baseAltitude", 3, "m");
                //summaryData = addSummaryData(summaryData, "ascentSeconds", 3, "s");
                //summaryData = addSummaryData(summaryData, "descentSeconds", 3, "s");
                //summaryData = addSummaryData(summaryData, "flatSeconds", 3, "s");
                //if (analyticsObject.has("Intermittent Distance")) {
                //    summaryData = addSummaryData(summaryData, "ascentDistance", 3, "m");
                //    summaryData = addSummaryData(summaryData, "descentDistance", 3, "m");
                //    summaryData = addSummaryData(summaryData, "flatDistance", 3, "m");
                //}
                //summaryData = addSummaryData(summaryData, "elevationGain", 3, "mm");
                //summaryData = addSummaryData(summaryData, "elevationLoss", 3, "mm");
            }
            //     put("HeartRateZones", Arrays.asList(
            //             "hrZoneNa", "hrZoneWarmUp", "hrZoneFatBurn", "hrZoneAerobic", "hrZoneAnaerobic",
            //             "hrZoneExtreme"
            //     ));
            // TODO: Implement hrZones by doing calculations on Gadgetbridge side or make Bangle.js report this (Karvonen method implemented to a degree in watch app "Run+")?
            //summaryData = addSummaryData(summaryData,"hrZoneNa",3,"mm");
            //summaryData = addSummaryData(summaryData,"hrZoneWarmUp",3,"mm");
            //summaryData = addSummaryData(summaryData,"hrZoneFatBurn",3,"mm");
            //summaryData = addSummaryData(summaryData,"hrZoneAerobic",3,"mm");
            //summaryData = addSummaryData(summaryData,"hrZoneAnaerobic",3,"mm");
            //summaryData = addSummaryData(summaryData,"hrZoneExtreme",3,"mm");
            //     put("TrainingEffect", Arrays.asList(
            //             "aerobicTrainingEffect", "anaerobicTrainingEffect", "currentWorkoutLoad",
            //             "maximumOxygenUptake"
            //     ));

            //     put("Laps", Arrays.asList(
            //             "averageLapPace", "laps"
            //     ));
            // TODO: Does Bangle.js report laps in recorder logs?
            //summaryData = addSummaryData(summaryData,"averageLapPace",3,"mm");
            //summaryData = addSummaryData(summaryData,"laps",3,"mm");
            // }};
            summary.setSummaryData(summaryData.toString());

            ActivityTrack track = new ActivityTrack(); // detailsParser.parse(buffer.toByteArray());
            track.startNewSegment();
            track.setBaseTime(startTime);
            track.setName(log);
            try (DBHandler dbHandler = GBApplication.acquireDB()) {
                DaoSession session = dbHandler.getDaoSession();
                Device deviceDB = DBHelper.getDevice(device, session);
                User user = DBHelper.getUser(session);
                track.setDevice(deviceDB);
                track.setUser(user);
            } catch (Exception ex) {
                GB.toast(context, "Error setting user for activity track.", Toast.LENGTH_LONG, GB.ERROR, ex);
            }
            ActivityPoint point = new ActivityPoint();
            Date timeOfPoint = new Date();
            boolean hasGPXReading = false;
            boolean hasHRMReading = false;
            for (int i = 0; i < storedLogObject.getJSONArray("Time").length(); i++) {
                timeOfPoint.setTime(storedLogObject.getJSONArray("Time").getLong(i)*1000L);
                point.setTime(timeOfPoint);
                if (storedLogObject.has("Longitude")) {
                    if (!Objects.equals(storedLogObject.getJSONArray("Longitude").getString(i), "")
                            && !Objects.equals(storedLogObject.getJSONArray("Latitude").getString(i), "")
                            && !Objects.equals(storedLogObject.getJSONArray("Altitude").getString(i), "")) {

                        point.setLocation(new GPSCoordinate(
                                        storedLogObject.getJSONArray("Longitude").getDouble(i),
                                        storedLogObject.getJSONArray("Latitude").getDouble(i),
                                        storedLogObject.getJSONArray("Altitude").getDouble(i)
                                )
                        );

                        if (!hasGPXReading) hasGPXReading = true;
                    }
                }
                if (storedLogObject.has("Heartrate") && !Objects.equals(storedLogObject.getJSONArray("Heartrate").getString(i), "")) {
                    point.setHeartRate(storedLogObject.getJSONArray("Heartrate").getInt(i));

                    if (!hasHRMReading) hasHRMReading = true;
                }
                track.addTrackPoint(point);
                LOG.debug("Activity Point:\n" + point.getHeartRate());
                point = new ActivityPoint();
            }

            ActivityTrackExporter exporter = createExporter();
            String trackType = "track";
            switch (summary.getActivityKind()) {
                case ActivityKind.TYPE_CYCLING:
                    trackType = context.getString(R.string.activity_type_biking);
                    break;
                case ActivityKind.TYPE_RUNNING:
                    trackType = context.getString(R.string.activity_type_running);
                    break;
                case ActivityKind.TYPE_WALKING:
                    trackType = context.getString(R.string.activity_type_walking);
                    break;
                case ActivityKind.TYPE_HIKING:
                    trackType = context.getString(R.string.activity_type_hiking);
                    break;
                case ActivityKind.TYPE_CLIMBING:
                    trackType = context.getString(R.string.activity_type_climbing);
                    break;
                case ActivityKind.TYPE_SWIMMING:
                    trackType = context.getString(R.string.activity_type_swimming);
                    break;
            }

            String fileName = FileUtils.makeValidFileName("gadgetbridge-" + trackType.toLowerCase() + "-" + summary.getName() + ".gpx");
            File targetFile = new File(FileUtils.getExternalFilesDir(), fileName);

            if (hasGPXReading /*|| hasHRMReading*/) {
                try {
                    exporter.performExport(track, targetFile);

                    try (DBHandler dbHandler = GBApplication.acquireDB()) {
                        summary.setGpxTrack(targetFile.getAbsolutePath());
                        //dbHandler.getDaoSession().getBaseActivitySummaryDao().update(summary);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                } catch (ActivityTrackExporter.GPXTrackEmptyException ex) {
                    GB.toast(context, "This activity does not contain GPX tracks.", Toast.LENGTH_LONG, GB.ERROR, ex);
                }
            }

            //summary.setSummaryData(null); // remove json before saving to database,

            try (DBHandler dbHandler = GBApplication.acquireDB()) {
                DaoSession session = dbHandler.getDaoSession();
                Device deviceDB = DBHelper.getDevice(device, session);
                User user = DBHelper.getUser(session);
                summary.setDevice(deviceDB);
                summary.setUser(user);
                session.getBaseActivitySummaryDao().insertOrReplace(summary);
            } catch (Exception ex) {
                GB.toast(context, "Error saving activity summary", Toast.LENGTH_LONG, GB.ERROR, ex);
            }

            LOG.debug("Activity track:\n" + track.getSegments());

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        stopAndRestartTimeout(device,context);
    }

    private static void signalFetchingStarted(GBDevice device, Context context) {
        GB.updateTransferNotification(context.getString(R.string.activity_detail_start_label) + " : " + context.getString(R.string.busy_task_fetch_sports_details),"", true, 0, context);
        device.setBusyTask(context.getString(R.string.busy_task_fetch_sports_details));
        GB.toast(context.getString(R.string.activity_detail_start_label) + " : " + context.getString(R.string.busy_task_fetch_sports_details), Toast.LENGTH_SHORT, GB.INFO);
    }

    private static void signalFetchingEnded(GBDevice device, Context context) {
        stopTimeoutTask();
        device.unsetBusyTask();
        device.sendDeviceUpdateIntent(context);
        GB.updateTransferNotification(null, "", false, 100, context);
        GB.toast(context.getString(R.string.activity_detail_end_label) + " : " + context.getString(R.string.busy_task_fetch_sports_details), Toast.LENGTH_SHORT, GB.INFO);
    }

    private static Timer timeout;
    private static TimerTask timeoutTask;

    private static void startTimeout(GBDevice device, Context context) {
        timeout = new Timer();

        initializeTimeoutTask(device, context);

        timeout.schedule(timeoutTask, 5000);
    }

    private static void stopTimeoutTask() {
        if (timeout != null) {
            timeout.cancel();
            timeout = null;
        }
    }

    private static void initializeTimeoutTask(GBDevice device, Context context) {

        timeoutTask = new TimerTask() {
            public void run() {
                signalFetchingEnded(device, context);
                LOG.warn(context.getString(R.string.busy_task_fetch_sports_details_interrupted));
                GB.toast(context.getString(R.string.busy_task_fetch_sports_details_interrupted), Toast.LENGTH_LONG, GB.INFO);
                // TODO: We could send a stop message back to Bangle.js here if we want to hinder
                //  the potential event it would start sending lines again after Gadgetbridge
                //  determined the fetch had been interrupted. On the other hand I think if we
                //  started receiving lines again, they would only be appended to the file in
                //  storage and the fetch would continue as if nothing happend.
            }
        };
    }

    private static void stopAndRestartTimeout(GBDevice device, Context context) {
        stopTimeoutTask();
        startTimeout(device, context);
    }

    private static String getLatestFetchedRecorderLog() {
        File dir;
        try {
            dir = FileUtils.getExternalFilesDir();
        } catch (IOException e) {
            return null;
        }
        String filename = "latestFetchedRecorderLog.txt";
        File inputFile = new File(dir, filename);
        String lastSyncedID = "";
        try {
            lastSyncedID = FileUtils.getStringFromFile(inputFile).replace("\n","");
        } catch (IOException ignored) {
        }
        //lastSyncedID = "20230706x"; // DEBUGGING

        LOG.info("Last Synced log ID: " + lastSyncedID);
        //requestActivityTracksList(lastSyncedID);

        return lastSyncedID;
    }

    private static void setLatestFetchedRecorderLog(File dir, String log) {

        String filenameLogID = "latestFetchedRecorderLog.txt";
        File outputFileLogID = new File(dir, filenameLogID);
        try {
            FileUtils.copyStringToFile(log,outputFileLogID,"");
            //GB.toast(context, "Log ID " + log + " written to " + filenameLogID, Toast.LENGTH_LONG, GB.INFO);
        } catch (IOException e) {
            LOG.error("Could not write to file", e);
        }
    }

    private static void writeToRecorderCSV(String lines, File dir, String filename) {
        String mode = "append";
        if (lines.equals("erase")) {
            mode = "write";
            lines = "";
        }

        File outputFile = new File(dir, filename);
        try {
            FileUtils.copyStringToFile(lines,outputFile,mode);
            //GB.toast(context, "Log written to " + filename, Toast.LENGTH_LONG, GB.INFO);
        } catch (IOException e) {
            LOG.error("Could not write to file", e);
        }
    }

    private static ActivityTrackExporter createExporter() {
        GPXExporter exporter = new GPXExporter();
        exporter.setCreator(GBApplication.app().getNameAndVersion());
        return exporter;
    }

    private static JSONObject addSummaryData(JSONObject summaryData, String key, float value, String unit) {
        if (value > 0) {
            try {
                JSONObject innerData = new JSONObject();
                innerData.put("value", value);
                innerData.put("unit", unit);
                summaryData.put(key, innerData);
            } catch (JSONException ignore) {
            }
        }
        return summaryData;
    }

    // protected JSONObject addSummaryData(JSONObject summaryData, String key, String value) {
    //     if (key != null && !key.equals("") && value != null && !value.equals("")) {
    //         try {
    //             JSONObject innerData = new JSONObject();
    //             innerData.put("value", value);
    //             innerData.put("unit", "string");
    //             summaryData.put(key, innerData);
    //         } catch (JSONException ignore) {
    //         }
    //     }
    //     return summaryData;
    // }

    private static String distanceFromCoordinatePairs(String latA, String lonA, String latB, String lonB) {
        // https://en.wikipedia.org/wiki/Geographic_coordinate_system#Length_of_a_degree
        //phi = latitude
        //lambda = longitude
        //length of 1 degree lat:
        //111132.92 - 559.82*cos(2*phi) + 1.175*cos(4*phi) - 0.0023*cos(6*phi)
        //length of 1 degree lon:
        //111412.84*cos(phi) - 93.5*cos(3*phi) + 0.118*cos(5*phi)
        double latADouble = Double.parseDouble(latA);
        double latBDouble = Double.parseDouble(latB);
        double lonADouble = Double.parseDouble(lonA);
        double lonBDouble = Double.parseDouble(lonB);

        double lengthPerDegreeLat = 111132.92 - 559.82*cos(2*latADouble) + 1.175*cos(4*latADouble) - 0.0023*cos(6*latADouble);
        double lengthPerDegreeLon = 111412.84*cos(latADouble) - 93.5*cos(3*latADouble) + 0.118*cos(5*latADouble);

        double latDist = (latBDouble-latADouble)*lengthPerDegreeLat;
        double lonDist = (lonBDouble-lonADouble)*lengthPerDegreeLon;

        return String.valueOf(sqrt(latDist*latDist+lonDist*lonDist));
    }

    private static float sumOfJSONArray(JSONArray a) throws JSONException {
        double sum = 0;
        for (int i=0; i<a.length(); i++) {
            if (!Objects.equals(a.getString(i), "")) sum += a.getDouble(i);
        }
        return (float) sum;
    }

    private static float averageOfJSONArray(JSONArray a) throws JSONException {
        JSONArray b = new JSONArray();
        // Disregard empty lines.
        for (int i=0; i<a.length(); i++) {
            if (!Objects.equals(a.getString(i), ""))  b.put(a.getString(i));
        }
        return sumOfJSONArray(b) / b.length();
    }

    private static float minOfJSONArray(JSONArray a) throws JSONException {
        double min = 999999999;
        for (int i=0; i<a.length(); i++) {
            if (!Objects.equals(a.getString(i), "")) min = Math.min(min, a.getDouble(i));
        }
        return (float) min;
    }

    private static float maxOfJSONArray(JSONArray a) throws JSONException {
        double max = -999999999;
        for (int i=0; i<a.length(); i++) {
            if (!Objects.equals(a.getString(i), "")) max = Math.max(max, a.getDouble(i));
        }
        return (float) max;
    }

}
