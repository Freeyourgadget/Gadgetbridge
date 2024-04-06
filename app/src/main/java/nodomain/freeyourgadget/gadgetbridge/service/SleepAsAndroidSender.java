package nodomain.freeyourgadget.gadgetbridge.service;

import android.content.Context;
import android.content.Intent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.devices.SleepAsAndroidFeature;
import nodomain.freeyourgadget.gadgetbridge.externalevents.sleepasandroid.SleepAsAndroidAction;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class SleepAsAndroidSender {

    private final Logger LOG = LoggerFactory.getLogger(SleepAsAndroidSender.class);
    private final String PACKAGE_SLEEP_AS_ANDROID = "com.urbandroid.sleep";
    private final String ACTION_EXTRA_DATA_UPDATE = "com.urbandroid.sleep.ACTION_EXTRA_DATA_UPDATE";
    private final String ACTION_MOVEMENT_DATA_UPDATE = "com.urbandroid.sleep.watch.DATA_UPDATE";
    private final String ACTION_HEART_RATE_DATA_UPDATE = "com.urbandroid.sleep.watch.HR_DATA_UPDATE";
    private final String ACTION_RESUME_FROM_WATCH = "com.urbandroid.sleep.watch.RESUME_FROM_WATCH";
    private final String ACTION_PAUSE_FROM_WATCH = "com.urbandroid.sleep.watch.PAUSE_FROM_WATCH";
    private final String ACTION_SNOOZE_FROM_WATCH = "com.urbandroid.sleep.watch.SNOOZE_FROM_WATCH";
    private final String ACTION_DISMISS_FROM_WATCH = "com.urbandroid.sleep.watch.DISMISS_FROM_WATCH";

    private final String MAX_RAW_DATA = "MAX_RAW_DATA";
    private final String DATA = "DATA";
    private final String EXTRA_DATA_HR = "com.urbandroid.sleep.EXTRA_DATA_HR";
    private final String EXTRA_DATA_RR = "com.urbandroid.sleep.EXTRA_DATA_RR";
    private final String EXTRA_DATA_SPO2 = "com.urbandroid.sleep.EXTRA_DATA_SPO2";
    private final String EXTRA_DATA_SDNN = "com.urbandroid.sleep.EXTRA_DATA_SDNN";
    private final String EXTRA_DATA_TIMESTAMP = "com.urbandroid.sleep.EXTRA_DATA_TIMESTAMP";
    private final String EXTRA_DATA_FRAMERATE = "com.urbandroid.sleep.EXTRA_DATA_FRAMERATE";
    private final String EXTRA_DATA_BATCH = "com.urbandroid.sleep.EXTRA_DATA_BATCH";


    private GBDevice device;
    private boolean trackingOngoing = false;
    private boolean trackingPaused = false;

    private ScheduledExecutorService trackingPauseScheduler;
    private long batchSize = 1;
    private long lastRawDataMs = 0;
    private float maxRawData = 0;
    private long lastHrDataMs = 0;
    private ArrayList<Float> hrData = new ArrayList<>();

    private ArrayList<Float> accData = new ArrayList<>();
    private ScheduledExecutorService accDataScheduler;
    private Set<SleepAsAndroidFeature> features;

    public SleepAsAndroidSender(GBDevice gbDevice) {
        this.device = gbDevice;
        this.features = gbDevice.getDeviceCoordinator().getSleepAsAndroidFeatures();
    }

    /**
     * Check if a SleepAsAndroid feature is enabled
     *
     * @param feature the feature
     * @return true if the feature is enabled
     */
    public boolean hasFeature(SleepAsAndroidFeature feature) {
        return this.features.contains(feature);
    }

    /**
     * Check if a SleepAsAndroid feature is enabled
     * @param feature
     * @return
     */
    public boolean isFeatureEnabled(SleepAsAndroidFeature feature) {
        boolean enabled = isSleepAsAndroidEnabled();
        if (enabled) {
            switch (feature) {
                case ACCELEROMETER:
                    enabled = GBApplication.getPrefs().getBoolean("pref_key_sleepasandroid_feat_movement", false);
                    break;
                case HEART_RATE:
                    enabled = GBApplication.getPrefs().getBoolean("pref_key_sleepasandroid_feat_hr", false);
                    break;
                case SPO2:
                    enabled = GBApplication.getPrefs().getBoolean("pref_key_sleepasandroid_feat_spo2", false);
                    break;
                case OXIMETRY:
                    enabled = GBApplication.getPrefs().getBoolean("pref_key_sleepasandroid_feat_oximetry", false);
                    break;
                case NOTIFICATIONS:
                    enabled = GBApplication.getPrefs().getBoolean("pref_key_sleepasandroid_feat_notifications", false);
                    break;
                case ALARMS:
                    enabled = GBApplication.getPrefs().getBoolean("pref_key_sleepasandroid_feat_alarms", false);
                    break;
                default:
                    break;
            }
        }
        return enabled;
    }

    /**
     * Get all enabled features
     *
     * @return all enabled features
     */
    public Set<SleepAsAndroidFeature> getFeatures() {
        return features;
    }

    /**
     * Start tracking
     */
    public void startTracking() {
        if (!isDeviceDefault()) return;

        stopTracking();

        accDataScheduler = Executors.newSingleThreadScheduledExecutor();
        accDataScheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                aggregateAndSendAccelData();
            }
        }, 9999, 9999, TimeUnit.MILLISECONDS);

        lastRawDataMs = System.currentTimeMillis();
        lastHrDataMs = System.currentTimeMillis();

        this.trackingOngoing = true;
    }

    /**
     * Stop tracking
     */
    public void stopTracking() {
        if (!isDeviceDefault() || !trackingOngoing) return;
        if (accDataScheduler != null) {
            accDataScheduler.shutdownNow();
            accDataScheduler = null;
        }

        this.trackingOngoing = false;
        this.hrData = new ArrayList<>();
        this.accData = new ArrayList<>();
        this.lastHrDataMs = 0;
        this.lastRawDataMs = 0;
    }

    /**
     * Pause tracking
     *
     * @param timeout the timeout in milliseconds before resuming
     */
    public void pauseTracking(long timeout) {
        if (!isDeviceDefault() || !trackingOngoing) return;

        if (timeout <= 0) {
            resumeTracking();
            return;
        }

        pauseTracking();
        trackingPauseScheduler = setPauseTracking(timeout);
    }

    /**
     * Same as {@link #pauseTracking(long)} but pausing and resuming is controlled by a toggle
     *
     * @param suspended true if the tracking should be paused, false if should be resumed
     */
    public void pauseTracking(boolean suspended) {
        if (!isDeviceDefault() || !trackingOngoing) return;
        trackingPaused = suspended;
        if (!trackingPaused) {
            resumeTracking();
            return;
        }
    }

    /**
     * Set a scheduler to resume tracking
     *
     * @param delay the delay
     * @return the scheduler
     */
    private ScheduledExecutorService setPauseTracking(long delay) {
        trackingPaused = true;
        trackingPauseScheduler = Executors.newSingleThreadScheduledExecutor();
        trackingPauseScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                resumeTracking();
            }
        }, delay, TimeUnit.MILLISECONDS);

        return trackingPauseScheduler;
    }

    /**
     * Pause tracking
     */
    private void pauseTracking() {
        if (!isDeviceDefault() && trackingPaused) return;
        trackingPaused = true;
        stopTracking();
    }

    /**
     * Resume tracking
     */
    private void resumeTracking() {
        if (!isDeviceDefault() && !trackingPaused) return;
        if (trackingPauseScheduler != null) {
            trackingPauseScheduler.shutdownNow();
            trackingPauseScheduler = null;
        }
        trackingPaused = false;
        startTracking();
    }

    /**
     * Set the batch size
     *
     * @param batchSize the batch size
     */
    public void setBatchSize(long batchSize) {
        if (!isDeviceDefault()) return;
        LOG.debug("Setting batch size to " + batchSize);
        this.batchSize = batchSize;
    }

    /**
     * Confirm that the device is connected
     */
    public void confirmConnected() {
        if (!isDeviceDefault()) return;
        LOG.debug("Confirming connected");
        Intent intent = new Intent(SleepAsAndroidAction.CONFIRM_CONNECTED);
        broadcastToSleepAsAndroid(intent);
    }

    /**
     * On accelerometer changed
     *
     * @param x the x value
     * @param y the y value
     * @param z the z value
     */
    public void onAccelChanged(float x, float y, float z) {
        if (!isDeviceDefault() || !isFeatureEnabled(SleepAsAndroidFeature.ACCELEROMETER) || !hasFeature(SleepAsAndroidFeature.ACCELEROMETER) || !trackingOngoing)
            return;
        if (trackingPaused)
            return;

        updateMaxRawData(x, y, z);
    }

    /**
     * Aggregate and send the acceleration data
     */
    private synchronized void aggregateAndSendAccelData() {
        if (!trackingOngoing || trackingPaused) return;
        if (maxRawData > 0) {
            accData.add(maxRawData);
            maxRawData = 0;
            if (accData.size() == batchSize) {
                sendAccelData();
            }
        }
    }

    /**
     * Send the acceleration data
     */
    private void sendAccelData() {
        LOG.debug("Sending movement data: " + this.accData + " batch size: " + batchSize + " array size: " + accData.size());
        Intent intent = new Intent(ACTION_MOVEMENT_DATA_UPDATE);
        intent.putExtra(MAX_RAW_DATA, convertToFloatArray(this.accData));
        accData.clear();
        broadcastToSleepAsAndroid(intent);
    }

    /**
     * Update the max raw data
     * @param x the x value
     * @param y the y value
     * @param z the z value
     */
    private void updateMaxRawData(float x, float y, float z) {
        float maxRaw = calculateAccelerationMagnitude(x, y, z);
        if (maxRaw > maxRawData) {
            maxRawData = maxRaw;
        }
    }

    /**
     * Calculate the acceleration magnitude
     * @param x the x value
     * @param y the y value
     * @param z the z value
     * @return
     */
    protected float calculateAccelerationMagnitude(float x, float y, float z) {
        double sqrt = Math.sqrt((x * x) + (y * y) + (z * z));
        return (float)sqrt;
    }

    /**
     * On heart rate changed
     *
     * @param hr        the heart rate
     * @param sendDelay the send delay in ms. If 0 the data will be send right away. Anything bigger will gather all the data then send it all after the specified interval
     */
    public void onHrChanged(float hr, long sendDelay) {
        if (!isDeviceDefault() || !isFeatureEnabled(SleepAsAndroidFeature.HEART_RATE) || !hasFeature(SleepAsAndroidFeature.HEART_RATE) || !trackingOngoing)
            return;
        if (trackingPaused) return;

        updateLastHrData(hr);

        if (lastHrDataMs == 0) {
            lastHrDataMs = System.currentTimeMillis();
        }
        long ms = System.currentTimeMillis();
        if (ms - lastHrDataMs >= sendDelay) {
            lastHrDataMs = ms;
            sendHrData();
        }
    }

    /**
     * Send the heart rate data
     */
    private synchronized void sendHrData() {
        LOG.debug("Sending heart rate data: " + this.hrData);
        Intent intent = new Intent(ACTION_HEART_RATE_DATA_UPDATE);
        intent.putExtra(DATA, convertToFloatArray(this.hrData));
        broadcastToSleepAsAndroid(intent);
        this.hrData.clear();
    }

    /**
     * Update the last heart rate data
     * @param hr the heart rate
     */
    private void updateLastHrData(float hr) {
        this.hrData.add(hr);
    }

    /**
     * This is a generic intent to carry data from various sensors.
     * See Sleep As Android documentation for parameters values.
     * <a href="https://docs.sleep.urbandroid.org/devs/wearable_api.html#send-various-body-sensors-data-hr-rr-spo2-sdnn">...</a>
     */
    public synchronized void sendExtra(Float hr, Long extraDataTimestamp, Long extraDataFramerate, ArrayList<Float> extraDataRR, ArrayList<Float> spo2Batch, Float sdnn, ArrayList<Float> extraDataBatch) {

        if (!isDeviceDefault() || !trackingOngoing) return;
        if (trackingPaused) return;
        Context context = GBApplication.getContext();
        Intent intent = new Intent(ACTION_EXTRA_DATA_UPDATE);

        // Heart Rate
        if (hr != null && (hasFeature(SleepAsAndroidFeature.HEART_RATE) && isFeatureEnabled(SleepAsAndroidFeature.HEART_RATE))) {
            intent.putExtra(EXTRA_DATA_HR, hr);
        }

        // SpO2
        if (spo2Batch != null && (hasFeature(SleepAsAndroidFeature.SPO2) && isFeatureEnabled(SleepAsAndroidFeature.SPO2))) {
            intent.putExtra(EXTRA_DATA_SPO2, true);
            intent.putExtra(EXTRA_DATA_BATCH, convertToFloatArray(spo2Batch));
        }

        // SDNN
        if (sdnn != null && (hasFeature(SleepAsAndroidFeature.HEART_RATE) && isFeatureEnabled(SleepAsAndroidFeature.HEART_RATE))) {
            intent.putExtra(EXTRA_DATA_SDNN, sdnn);
        }

        // RR Intervals
        if (extraDataRR != null && (hasFeature(SleepAsAndroidFeature.HEART_RATE) && isFeatureEnabled(SleepAsAndroidFeature.HEART_RATE))) {
            intent.putExtra(EXTRA_DATA_RR, convertToFloatArray(extraDataRR));
        }

        if (extraDataBatch != null) {
            if (isDeviceDefault()) {
                for (int i = 0; i < extraDataBatch.size(); i++) {
                    extraDataBatch.set(i, 0.0f);
                }
            }
            intent.putExtra(EXTRA_DATA_BATCH, convertToFloatArray(extraDataBatch));
        }

        if (extraDataTimestamp != null) {
            intent.putExtra(EXTRA_DATA_TIMESTAMP, extraDataTimestamp);
        }
        if (extraDataFramerate != null) {
            intent.putExtra(EXTRA_DATA_FRAMERATE, extraDataFramerate);
        }

        context.sendBroadcast(intent);
    }

    /**
     * This is a generic intent to carry data from various sensors.
     * See Sleep As Android documentation for parameters values.
     * <a href="https://docs.sleep.urbandroid.org/devs/wearable_api.html#send-various-body-sensors-data-hr-rr-spo2-sdnn">...</a>
     */
    public void sendExtra(Float hr, Float extraDataRR, Float spo2, Float sdnn, Long sdnnTimestamp) {

        if (!isDeviceDefault() || !trackingOngoing) return;
        if (trackingPaused) return;
        Intent intent = new Intent(ACTION_EXTRA_DATA_UPDATE);

        // Heart Rate
        if (hr != null && (hasFeature(SleepAsAndroidFeature.HEART_RATE) && isFeatureEnabled(SleepAsAndroidFeature.HEART_RATE))) {
            intent.putExtra(EXTRA_DATA_HR, hr);
        }

        // SpO2
        if (spo2 != null && (hasFeature(SleepAsAndroidFeature.SPO2) && isFeatureEnabled(SleepAsAndroidFeature.SPO2))) {
            intent.putExtra(EXTRA_DATA_SPO2, spo2);
        }

        // SDNN
        if (sdnn != null && (hasFeature(SleepAsAndroidFeature.HEART_RATE) && isFeatureEnabled(SleepAsAndroidFeature.HEART_RATE))) {
            intent.putExtra(EXTRA_DATA_SDNN, sdnn);
        }

        if (extraDataRR != null && (hasFeature(SleepAsAndroidFeature.HEART_RATE) && isFeatureEnabled(SleepAsAndroidFeature.HEART_RATE))) {
            intent.putExtra(EXTRA_DATA_RR, extraDataRR);
        }

        if (sdnnTimestamp != null) {
            intent.putExtra(EXTRA_DATA_TIMESTAMP, sdnnTimestamp);
        }
        broadcastToSleepAsAndroid(intent);
    }

    /**
     * Adds 5 minutes of tracking pause time
     */
    public void sendPauseTracking() {
        if (!isDeviceDefault() || !trackingOngoing) return;
        LOG.debug("Sending pause");
        Intent intent = new Intent(ACTION_PAUSE_FROM_WATCH);
        broadcastToSleepAsAndroid(intent);
    }

    /**
     * Resume tracking
     */
    public void sendResumeTracking() {
        if (!isDeviceDefault() || !trackingOngoing) return;
        LOG.debug("Sending resume");
        Intent intent = new Intent(ACTION_RESUME_FROM_WATCH);
        broadcastToSleepAsAndroid(intent);
    }

    /**
     * Snooze the current alarm
     */
    public void sendSnoozeAlarm() {
        if (!isDeviceDefault()) return;
        if (!hasFeature(SleepAsAndroidFeature.ALARMS) || !isFeatureEnabled(SleepAsAndroidFeature.ALARMS)) return;
        LOG.debug("Sending snooze");
        Intent intent = new Intent(ACTION_SNOOZE_FROM_WATCH);
        broadcastToSleepAsAndroid(intent);
    }

    /**
     * Dismiss the current alarm
     */
    public void sendDismissAlarm() {
        if (!isDeviceDefault()) return;
        if (!hasFeature(SleepAsAndroidFeature.ALARMS) || !isFeatureEnabled(SleepAsAndroidFeature.ALARMS)) return;
        LOG.debug("Sending dismiss");
        Intent intent = new Intent(ACTION_DISMISS_FROM_WATCH);
        broadcastToSleepAsAndroid(intent);
    }

    private void broadcastToSleepAsAndroid(Intent intent) {
        if (!isDeviceDefault()) return;
        intent.setPackage(PACKAGE_SLEEP_AS_ANDROID);
        GBApplication.getContext().sendBroadcast(intent);
    }

    /**
     * Check if the device is set as default provider for Sleep As Android
     *
     * @return true if the device is set as default
     */
    public boolean isDeviceDefault() {
        if (device == null || !device.isInitialized()) return false;
        if (isSleepAsAndroidEnabled()) {
            return device.getAddress().equals(GBApplication.getPrefs().getString("sleepasandroid_device", ""));
        }
        return false;
    }

    public boolean isSleepAsAndroidEnabled() {
        return GBApplication.getPrefs().getBoolean("pref_key_sleepasandroid_enable", false);
    }

    /**
     * Validate if the device is allowed to receive a specific action
     * @param action the action send my the broadcast receiver
     */
    public void validateAction(String action) {
        if (isDeviceDefault()) {
            switch (action) {
                case SleepAsAndroidAction.HINT:
                case SleepAsAndroidAction.SHOW_NOTIFICATION:
                    if (!hasFeature(SleepAsAndroidFeature.NOTIFICATIONS) || !isFeatureEnabled(SleepAsAndroidFeature.NOTIFICATIONS)) {
                        throw new UnsupportedOperationException("Action not valid");
                    }
                    break;
                case SleepAsAndroidAction.START_ALARM:
                case SleepAsAndroidAction.STOP_ALARM:
                case SleepAsAndroidAction.UPDATE_ALARM:
                    if (!hasFeature(SleepAsAndroidFeature.ALARMS) || !isFeatureEnabled(SleepAsAndroidFeature.ALARMS)) {
                        throw new UnsupportedOperationException("Action not valid");
                    }
                    break;
                case SleepAsAndroidAction.START_TRACKING:
                case SleepAsAndroidAction.STOP_TRACKING:
                case SleepAsAndroidAction.SET_BATCH_SIZE:
                    if (!hasFeature(SleepAsAndroidFeature.ACCELEROMETER) || !isFeatureEnabled(SleepAsAndroidFeature.ACCELEROMETER)) {
                        throw new UnsupportedOperationException("Action not valid");
                    }
                    break;
                default:
                    break;
            }
        } else {
            throw new UnsupportedOperationException("Action not valid");
        }
    }

    /**
     * Convert an ArrayList to a float array
     *
     * @param list the ArrayList
     * @return the float array
     */
    private float[] convertToFloatArray(ArrayList<Float> list) {
        float[] result = new float[list.size()];
        int i = 0;
        for (float f : list) {
            result[i++] = f;
        }
        return result;
    }

    /**
     * Get configured alarm slot
     *
     * @return the alarm slot to be used for setting alarms on the watch
     */
    public static int getAlarmSlot() {
        Prefs prefs = GBApplication.getPrefs();
        String slotString = prefs.getString("sleepasandroid_alarm_slot", "");
        if (!slotString.isEmpty()) {
            return Integer.parseInt(slotString);
        }
        return 0;
    }
}
