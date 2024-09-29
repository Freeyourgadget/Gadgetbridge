package nodomain.freeyourgadget.gadgetbridge.service.devices.banglejs;

import static java.lang.Integer.parseInt;

import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.HR_AVG;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.INTERNAL_HAS_GPS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.SPEED_AVG;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.banglejs.BangleJSActivityPoint;
import nodomain.freeyourgadget.gadgetbridge.devices.banglejs.BangleJSWorkoutParser;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.export.ActivityTrackExporter;
import nodomain.freeyourgadget.gadgetbridge.export.GPXExporter;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityPoint;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryData;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityTrack;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

class BangleJSActivityTrack {

    private static final Logger LOG = LoggerFactory.getLogger(BangleJSActivityTrack.class);

    static JSONObject compileTracksListRequest(GBDevice device, Context context) {
        stopAndRestartTimeout(device, context);
        signalFetchingStarted(device, context);
        //GB.toast("TYPE_GPS_TRACKS says hi!", Toast.LENGTH_LONG, GB.INFO);

        String lastSyncedID = getLatestFetchedRecorderLog(device);

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

    private static JSONArray tracksList;
    static JSONObject handleActTrksList(JSONObject json, GBDevice device, Context context) throws JSONException {
        stopAndRestartTimeout(device, context);
        tracksList = json.getJSONArray("list");
        LOG.debug("trksList says hi!");
        //GB.toast(getContext(), "trksList says hi!", Toast.LENGTH_LONG, GB.INFO);
        LOG.info("New recorder logs since last fetch: " + String.valueOf(tracksList));
        if (tracksList.length()==0) {
            signalFetchingEnded(device, context);
            return null;
        } else {
            JSONObject requestTrackObj = BangleJSActivityTrack.compileTrackRequest(tracksList.getString(0), 1==tracksList.length());
            tracksList.remove(0);
            return requestTrackObj;
        }
    }

    private static int lastPacketCount = -1;
    static JSONObject handleActTrk(JSONObject json, GBDevice device, Context context) throws JSONException {
        stopAndRestartTimeout(device, context);

        JSONObject returnObj;

        JSONObject stopObj = new JSONObject().put("t","fetchRec").put("id","stop");
        int currPacketCount;
        if (!json.has("cnt")) {
            currPacketCount = 0;
        } else {
            currPacketCount = json.getInt("cnt");
        }
        if (currPacketCount != lastPacketCount+1) {
            LOG.error("Activity Track Packets came out of order - aborting.");
            LOG.debug("packetCount Aborting: " + lastPacketCount);
            signalFetchingEnded(device, context);
            stopTimeoutTask();
            return stopObj;
        }

        LOG.debug("actTrk says hi!");
        //GB.toast(context, "actTrk says hi!", Toast.LENGTH_LONG, GB.INFO);
        String log = json.getString("log");
        LOG.debug(log);
        String filename = "recorder.log" + log + ".csv";
        File dir;
        try {
            dir = new File(FileUtils.getExternalFilesDir() + "/" + FileUtils.makeValidFileName(device.getName()));
            if (!dir.isDirectory()) {
                if (!dir.mkdir()) {
                    throw new IOException("Cannot create device specific directory for " + device.getName());
                }
            }
        } catch (IOException e) {
            LOG.error("Failed at getting external files directory with error: " + e);
            resetPacketCount();
            return null;
        }

        if (!json.has("lines")) { // if no lines were sent with this json object, it signifies that the whole recorder log has been transmitted.
            setLatestFetchedRecorderLog(log, device);
            parseFetchedRecorderCSV(dir, filename, log, device, context); // I tried refactoring to parse all fetched logs in one go at the end instead. But that only gave me more troubles. This seems like a more stable approach at least in the Bangle.js case.
            if (tracksList.length()==0) {
                signalFetchingEnded(device, context);
                LOG.debug("packetCount reset1: " + lastPacketCount);
                returnObj = null;
            } else {
                JSONObject requestTrackObj = BangleJSActivityTrack.compileTrackRequest(tracksList.getString(0), 1==tracksList.length());
                tracksList.remove(0);
                resetPacketCount();
                LOG.debug("packetCount reset2: " + lastPacketCount);
                returnObj = requestTrackObj;
            }
        } else { // We received a lines of the csv, now we append it to the file in storage.

            String lines = json.getString("lines");
            LOG.debug(lines);

            writeToRecorderCSV(lines, dir, filename);

            lastPacketCount += 1;
            LOG.debug("packetCount continue: " + lastPacketCount);
            returnObj = null;
        }

        return returnObj;
    }

    private static void parseFetchedRecorderCSV(File dir, String filename, String log, GBDevice device, Context context) {
        stopTimeoutTask(); // Parsing can take a while if there are many data. Restart at end of parsing.

        File inputFile = new File(dir, filename);
        try {
            BaseActivitySummary summary = new BaseActivitySummary();

            final List<BangleJSActivityPoint> banglePoints = BangleJSActivityPoint.fromCsv(inputFile);
            if (banglePoints == null || banglePoints.isEmpty()) {
                // Should never happen?
                return;
            }
            Date startTime = new Date(banglePoints.get(0).getTime());
            Date endTime = new Date(banglePoints.get(banglePoints.size() - 1).getTime());
            summary.setName(log);
            summary.setStartTime(startTime);
            summary.setEndTime(endTime);
            ActivitySummaryData summaryData = BangleJSWorkoutParser.dataFromPoints(banglePoints);
            summary.setSummaryData(summaryData.toString());
            ActivityKind activityKind;
            final double speedAvg = summaryData.getNumber(SPEED_AVG, -1).doubleValue();
            if (speedAvg >= 10) {
                activityKind = ActivityKind.ACTIVITY;
            } else if (speedAvg >= 3) {
                activityKind = ActivityKind.RUNNING;
            } else if (speedAvg >= 0) {
                activityKind = ActivityKind.WALKING;
            } else {
                activityKind = ActivityKind.ACTIVITY;
            }
            summary.setActivityKind(activityKind.getCode()); // TODO: Make this depend on info from watch (currently this info isn't supplied in Bangle.js recorder logs).
            summary.setRawDetailsPath(String.valueOf(inputFile));

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
            boolean hasGPXReading = summaryData.has(INTERNAL_HAS_GPS);
            boolean hasHRMReading = summaryData.has(HR_AVG);
            for (final BangleJSActivityPoint banglePoint : banglePoints) {
                track.addTrackPoint(banglePoint.toActivityPoint());
            }

            ActivityTrackExporter exporter = new GPXExporter();
            String trackType = "track";
            switch (ActivityKind.fromCode(summary.getActivityKind())) {
                case CYCLING:
                    trackType = context.getString(R.string.activity_type_biking);
                    break;
                case RUNNING:
                    trackType = context.getString(R.string.activity_type_running);
                    break;
                case WALKING:
                    trackType = context.getString(R.string.activity_type_walking);
                    break;
                case HIKING:
                    trackType = context.getString(R.string.activity_type_hiking);
                    break;
                case CLIMBING:
                    trackType = context.getString(R.string.activity_type_climbing);
                    break;
                case SWIMMING:
                    trackType = context.getString(R.string.activity_type_swimming);
                    break;
            }

            String fileName = FileUtils.makeValidFileName("gadgetbridge-" + trackType.toLowerCase() + "-" + summary.getName() + ".gpx");
            dir = new File(FileUtils.getExternalFilesDir() + "/" + FileUtils.makeValidFileName(device.getName()));
            File targetFile = new File(dir, fileName);

            if (hasGPXReading /*|| hasHRMReading*/) {
                try {
                    exporter.performExport(track, targetFile);
                    summary.setGpxTrack(targetFile.getAbsolutePath());
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
            LOG.error("IOException when parsing fetched CSV: " + e);
        }

        stopAndRestartTimeout(device,context);
    }

    private static void resetPacketCount() {
        lastPacketCount = -1;
    }

    private static JSONObject compileTrackRequest(String id, Boolean isLastId) {
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

    private static void signalFetchingStarted(GBDevice device, Context context) {
        GB.updateTransferNotification(context.getString(R.string.activity_detail_start_label) + " : " + context.getString(R.string.busy_task_fetch_sports_details),"", true, 0, context);
        device.setBusyTask(context.getString(R.string.busy_task_fetch_sports_details));
        GB.toast(context.getString(R.string.activity_detail_start_label) + " : " + context.getString(R.string.busy_task_fetch_sports_details), Toast.LENGTH_SHORT, GB.INFO);
    }

    private static void signalFetchingEnded(GBDevice device, Context context) {
        stopTimeoutTask();
        resetPacketCount();
        device.unsetBusyTask();
        device.sendDeviceUpdateIntent(context);
        GB.signalActivityDataFinish(device);
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

    private static String getLatestFetchedRecorderLog(GBDevice device) {
        // "lastSportsActivityTimeMillis" is what
        // `ActivitySummaryActivity.resetFetchTimestampToChosenDate()` uses, so we have to
        // control if the user changed that value, and if so recompile a new sync id from that info.

        String lastSyncedId = GBApplication.getDeviceSpecificSharedPrefs(device.getAddress()).
                getString("lastSportsActivityIdBangleJS","19700101a");
        LOG.debug("lastSyncedId: " + lastSyncedId);

        long lastSportsActivityTimeMillis = GBApplication.getDeviceSpecificSharedPrefs(device.getAddress()).
                getLong("lastSportsActivityTimeMillis",0);
        LOG.debug("lastSportsActivityTimeMillis: " + lastSportsActivityTimeMillis);

        Calendar dateFromId = parseCalendarFromBangleJSLogId(lastSyncedId);
        Calendar dateFromMillis = parseCalendarFromMillis(lastSportsActivityTimeMillis);

        LOG.debug("lastSyncedIdMillis: " + dateFromId.getTimeInMillis());

       int intMillis = parseInt(compileDateStringFromCalendar(dateFromMillis));
       int intBangle = parseInt(lastSyncedId.substring(0,lastSyncedId.length()-1));

        LOG.debug("intMillis: " + intMillis);
        LOG.debug("intBangle: " + intBangle);

        //if (dateFromMillis.before(dateFromId)) { // This would not work b/c the millis didn't ever become the same in my testing, even if compiled from the same sync id string.
        if (intMillis < intBangle) {
            dateFromMillis.add(Calendar.DATE, -1); // We want the day before so we fetch from the day the user reset to.
            int year = dateFromMillis.get(Calendar.YEAR);
            int month = dateFromMillis.get(Calendar.MONTH);
            int dayBefore = dateFromMillis.get(Calendar.DATE);


            String yearString = String.valueOf(year);
            String monthString = String.valueOf(month);
            if (month<10) monthString = "0" + monthString;
            String dayBeforeString = String.valueOf(dayBefore);
            if (dayBefore<10) dayBeforeString = "0" + dayBeforeString;
            String letter = "z";

            return yearString + monthString + dayBeforeString + letter;
        } else {
           return lastSyncedId;
        }
    }

    private static void setLatestFetchedRecorderLog(String log, GBDevice device) {
        Calendar date = parseCalendarFromBangleJSLogId(log);

        SharedPreferences.Editor editor = GBApplication.getDeviceSpecificSharedPrefs(device.getAddress()).edit();
        editor.remove("lastSportsActivityIdBangleJS"); //FIXME: key reconstruction is BAD (FIXME inherited from `ActivitySummaryActivity`.
        editor.remove("lastSportsActivityTimeMillis"); //FIXME: key reconstruction is BAD
        editor.putString("lastSportsActivityIdBangleJS", log);
        editor.putLong("lastSportsActivityTimeMillis", date.getTimeInMillis());
        editor.apply();
    }

    private static Calendar parseCalendarFromBangleJSLogId(String log) {
        int year = parseInt(log.substring(0,4));
        int month = parseInt(log.substring(4,6));
        int day = parseInt(log.substring(6,8));

        LOG.debug("DateFromId: " + year+ "|"+ month + "|" +day);

        Calendar date = Calendar.getInstance();
        date.set(year,month,day);

        return date;
    }

    private static Calendar parseCalendarFromMillis(long millis) {
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(millis);

        return date;
    }

    private static String compileDateStringFromCalendar(Calendar date) {
        int year = date.get(Calendar.YEAR);
        int month = date.get(Calendar.MONTH);
        int day = date.get(Calendar.DATE);

        return String.format(Locale.ROOT, "%d%02d%02d", year, month, day);
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
}
