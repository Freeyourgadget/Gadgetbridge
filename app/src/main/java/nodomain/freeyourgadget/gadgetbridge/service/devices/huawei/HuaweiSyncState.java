package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

class HuaweiSyncState {
    private static final Logger LOG = LoggerFactory.getLogger(HuaweiSyncState.class);

    private final HuaweiSupportProvider supportProvider;
    private final List<Integer> syncQueue = new ArrayList<>(2);

    private boolean activitySync = false;
    private boolean p2pSync = false;
    private boolean workoutSync = false;
    private int workoutGpsDownload = 0;

    public HuaweiSyncState(HuaweiSupportProvider supportProvider) {
        this.supportProvider = supportProvider;
    }

    public void addActivitySyncToQueue() {
        if (syncQueue.contains(RecordedDataTypes.TYPE_ACTIVITY))
            LOG.info("Activity type sync already queued, ignoring");
        else
            syncQueue.add(RecordedDataTypes.TYPE_ACTIVITY);
    }

    public void addWorkoutSyncToQueue() {
        if (syncQueue.contains(RecordedDataTypes.TYPE_GPS_TRACKS))
            LOG.info("Workout type sync already queued, ignoring");
        else
            syncQueue.add(RecordedDataTypes.TYPE_GPS_TRACKS);
    }

    public int getCurrentSyncType() {
        if (syncQueue.isEmpty())
            return -1;
        return syncQueue.get(0);
    }

    public void setActivitySync(boolean state) {
        LOG.debug("Set activity sync state to {}", state);
        this.activitySync = state;
        if (!state && !this.p2pSync) {
            this.syncQueue.remove((Integer) RecordedDataTypes.TYPE_ACTIVITY);
            supportProvider.fetchRecodedDataFromQueue();
        }
        updateState();
    }

    public void setP2pSync(boolean state) {
        LOG.debug("Set p2p sync state to {}", state);
        this.p2pSync = state;
        if (!state && !this.activitySync) {
            this.syncQueue.remove((Integer) RecordedDataTypes.TYPE_ACTIVITY);
            supportProvider.fetchRecodedDataFromQueue();
        }
        updateState();
    }

    public void setWorkoutSync(boolean state) {
        LOG.debug("Set workout sync state to {}", state);
        this.workoutSync = state;
        if (!state && this.workoutGpsDownload == 0) {
            this.syncQueue.remove((Integer) RecordedDataTypes.TYPE_GPS_TRACKS);
            supportProvider.fetchRecodedDataFromQueue();
        }
        updateState();
    }

    public void startWorkoutGpsDownload() {
        this.workoutGpsDownload += 1;
        LOG.debug("Add GPS download: {}", this.workoutGpsDownload);
    }

    public void stopWorkoutGpsDownload() {
        this.workoutGpsDownload -= 1;
        LOG.debug("Subtract GPS download: {}", this.workoutGpsDownload);
        if (this.workoutGpsDownload == 0 && !this.workoutSync) {
            this.syncQueue.remove((Integer) RecordedDataTypes.TYPE_GPS_TRACKS);
            supportProvider.fetchRecodedDataFromQueue();
        }
        updateState();
    }

    public void updateState() {
        updateState(true);
    }

    public void updateState(boolean needSync) {
        if (!activitySync && !p2pSync && !workoutSync && workoutGpsDownload == 0) {
            if (supportProvider.getDevice().isBusy()) {
                supportProvider.getDevice().unsetBusyTask();
                supportProvider.getDevice().sendDeviceUpdateIntent(supportProvider.getContext());
            }
            if (needSync)
                GB.signalActivityDataFinish(supportProvider.getDevice());
        }
    }
}
