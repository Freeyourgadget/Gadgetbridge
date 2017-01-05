package nodomain.freeyourgadget.gadgetbridge.service.devices.hplus;

/*
* @author João Paulo Barraca &lt;jpbarraca@gmail.com&gt;
*/


import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.hplus.HPlusConstants;
import nodomain.freeyourgadget.gadgetbridge.devices.hplus.HPlusHealthSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.HPlusHealthActivityOverlay;
import nodomain.freeyourgadget.gadgetbridge.entities.HPlusHealthActivityOverlayDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HPlusHealthActivitySample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceIoThread;


class HPlusHandlerThread extends GBDeviceIoThread {
    private static final Logger LOG = LoggerFactory.getLogger(HPlusHandlerThread.class);

    private int CURRENT_DAY_SYNC_PERIOD = 60 * 10;
    private int CURRENT_DAY_SYNC_RETRY_PERIOD = 10;
    private int SLEEP_SYNC_PERIOD = 12 * 60 * 60;
    private int SLEEP_SYNC_RETRY_PERIOD = 30;

    private int DAY_SUMMARY_SYNC_PERIOD = 24 * 60 * 60;

    private int HELLO_INTERVAL = 60;

    private boolean mQuit = false;
    private HPlusSupport mHPlusSupport;
    private int mLastSlotReceived = 0;
    private int mLastSlotRequested = 0;
    private int mSlotsToRequest = 6;

    private Calendar mLastSleepDayReceived = GregorianCalendar.getInstance();
    private Calendar mHelloTime = GregorianCalendar.getInstance();
    private Calendar mGetDaySlotsTime = GregorianCalendar.getInstance();
    private Calendar mGetSleepTime = GregorianCalendar.getInstance();
    private Calendar mGetDaySummaryTime = GregorianCalendar.getInstance();

    private HPlusDataRecordRealtime prevRealTimeRecord = null;

    private final Object waitObject = new Object();
    List<HPlusHealthActivitySample> mDaySlotSamples = new ArrayList<>();

    public HPlusHandlerThread(GBDevice gbDevice, Context context, HPlusSupport hplusSupport) {
        super(gbDevice, context);

        mQuit = false;

        mHPlusSupport = hplusSupport;
    }


    @Override
    public void run() {
        mQuit = false;

        sync();

        long waitTime = 0;
        while (!mQuit) {
            //LOG.debug("Waiting " + (waitTime));
            if (waitTime > 0) {
                synchronized (waitObject) {
                    try {
                        waitObject.wait(waitTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (mQuit) {
                break;
            }

            Calendar now = GregorianCalendar.getInstance();

            if (now.compareTo(mHelloTime) > 0) {
                sendHello();
            }

            if (now.compareTo(mGetDaySlotsTime) > 0) {
                requestNextDaySlots();
            }

            if (now.compareTo(mGetSleepTime) > 0) {
                requestNextSleepData();
            }

            if(now.compareTo(mGetDaySummaryTime) > 0) {
                requestDaySummaryData();
            }

            now = GregorianCalendar.getInstance();
            waitTime = Math.min(Math.min(mGetDaySlotsTime.getTimeInMillis(), mGetSleepTime.getTimeInMillis()), mHelloTime.getTimeInMillis()) - now.getTimeInMillis();
        }

    }

    @Override
    public void quit() {
        mQuit = true;
        synchronized (waitObject) {
            waitObject.notify();
        }
    }

    public void sync() {
        mGetSleepTime.setTimeInMillis(0);
        mGetDaySlotsTime.setTimeInMillis(0);
        mGetDaySummaryTime.setTimeInMillis(0);

        TransactionBuilder builder = new TransactionBuilder("startSyncDayStats");

        builder.write(mHPlusSupport.ctrlCharacteristic, new byte[]{HPlusConstants.CMD_GET_SLEEP});
        builder.write(mHPlusSupport.ctrlCharacteristic, new byte[]{HPlusConstants.CMD_GET_DAY_DATA});
        builder.write(mHPlusSupport.ctrlCharacteristic, new byte[]{HPlusConstants.CMD_GET_ACTIVE_DAY});
        builder.write(mHPlusSupport.ctrlCharacteristic, new byte[]{HPlusConstants.CMD_GET_DEVICE_ID});
        builder.write(mHPlusSupport.ctrlCharacteristic, new byte[]{HPlusConstants.CMD_GET_VERSION});
        builder.write(mHPlusSupport.ctrlCharacteristic, new byte[]{HPlusConstants.CMD_GET_CURR_DATA});

        builder.write(mHPlusSupport.ctrlCharacteristic, new byte[]{HPlusConstants.CMD_SET_ALLDAY_HRM, HPlusConstants.ARG_HEARTRATE_ALLDAY_ON});

        builder.queue(mHPlusSupport.getQueue());

        synchronized (waitObject) {
            waitObject.notify();
        }
    }

    private void sendHello() {
        TransactionBuilder builder = new TransactionBuilder("hello");
        builder.write(mHPlusSupport.ctrlCharacteristic, HPlusConstants.CMD_ACTION_HELLO);

        builder.queue(mHPlusSupport.getQueue());
        scheduleHello();
    }

    public void scheduleHello(){
        mHelloTime = GregorianCalendar.getInstance();
        mHelloTime.add(Calendar.SECOND, HELLO_INTERVAL);
    }

    public void requestDaySummaryData(){
        TransactionBuilder builder = new TransactionBuilder("startSyncDaySummary");
        builder.write(mHPlusSupport.ctrlCharacteristic, new byte[]{HPlusConstants.CMD_GET_DAY_DATA});
        builder.queue(mHPlusSupport.getQueue());

        mGetDaySummaryTime = GregorianCalendar.getInstance();
        mGetDaySummaryTime.add(Calendar.SECOND, DAY_SUMMARY_SYNC_PERIOD);
    }

    public boolean processIncomingDaySlotData(byte[] data) {

        HPlusDataRecordDaySlot record;
        try{
            record = new HPlusDataRecordDaySlot(data);
        } catch(IllegalArgumentException e){
            LOG.debug((e.getMessage()));
            return true;
        }
        mLastSlotReceived = record.slot;

        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            HPlusHealthSampleProvider provider = new HPlusHealthSampleProvider(getDevice(), dbHandler.getDaoSession());

            Long userId = DBHelper.getUser(dbHandler.getDaoSession()).getId();
            Long deviceId = DBHelper.getDevice(getDevice(), dbHandler.getDaoSession()).getId();
            HPlusHealthActivitySample sample = new HPlusHealthActivitySample(
                    record.timestamp,               // ts
                    deviceId, userId,               // User id
                    record.getRawData(),            // Raw Data
                    ActivityKind.TYPE_UNKNOWN,
                    0,    // Intensity
                    record.steps,                   // Steps
                    record.heartRate,               // HR
                    ActivitySample.NOT_MEASURED,    // Distance
                    ActivitySample.NOT_MEASURED     // Calories
            );
            sample.setProvider(provider);
            mDaySlotSamples.add(sample);

            Calendar now = GregorianCalendar.getInstance();

            //Dump buffered samples to DB
            if ((record.timestamp + (60*100) >= (now.getTimeInMillis() / 1000L) )) {

                provider.getSampleDao().insertOrReplaceInTx(mDaySlotSamples);

                mGetDaySlotsTime.setTimeInMillis(0);
                mDaySlotSamples.clear();
                mSlotsToRequest = 144 - mLastSlotReceived;
            }
        } catch (GBException ex) {
            LOG.debug((ex.getMessage()));
        } catch (Exception ex) {
            LOG.debug(ex.getMessage());
        }

        //Request next slot
        if(mLastSlotReceived == mLastSlotRequested){
            synchronized (waitObject) {
                mGetDaySlotsTime.setTimeInMillis(0);
                waitObject.notify();
            }
        }


        return true;
    }

    private void requestNextDaySlots() {

        Calendar now = GregorianCalendar.getInstance();

        if (mLastSlotReceived >= 144 + 6) { // 24 * 6 + 6
            LOG.debug("Reached End of the Day");
            mLastSlotReceived = 0;
            mSlotsToRequest = 6; // 1h
            mGetDaySlotsTime = now;
            mGetDaySlotsTime.add(Calendar.SECOND, CURRENT_DAY_SYNC_PERIOD);
            mLastSlotRequested = 0;
            return;
        }

        //Sync Day Stats
        mLastSlotRequested = Math.min(mLastSlotReceived + mSlotsToRequest, 144);

        LOG.debug("Requesting slot " + mLastSlotRequested);

        byte nextHour = (byte) (mLastSlotRequested / 6);
        byte nextMinute = (byte) ((mLastSlotRequested % 6) * 10);

        if (nextHour == (byte) now.get(Calendar.HOUR_OF_DAY)) {
            nextMinute = (byte) now.get(Calendar.MINUTE);
        }

        byte hour = (byte) (mLastSlotReceived / 6);
        byte minute = (byte) ((mLastSlotReceived % 6) * 10);

        byte[] msg = new byte[]{39, hour, minute, nextHour, nextMinute};

        TransactionBuilder builder = new TransactionBuilder("getNextDaySlot");
        builder.write(mHPlusSupport.ctrlCharacteristic, msg);
        builder.queue(mHPlusSupport.getQueue());

        mGetDaySlotsTime = now;
        if(mSlotsToRequest == 6) {
            mGetDaySlotsTime.add(Calendar.SECOND, CURRENT_DAY_SYNC_RETRY_PERIOD);
        }else{
            mGetDaySlotsTime.add(Calendar.SECOND, CURRENT_DAY_SYNC_PERIOD);
        }
        LOG.debug("Requesting next slot " + mLastSlotRequested+ " at " + mGetDaySlotsTime.getTime());

    }

    public boolean processIncomingSleepData(byte[] data){
        HPlusDataRecordSleep record;

        try{
            record = new HPlusDataRecordSleep(data);
        } catch(IllegalArgumentException e){
            LOG.debug((e.getMessage()));
            return true;
        }

        mLastSleepDayReceived.setTimeInMillis(record.bedTimeStart * 1000L);

        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            DaoSession session = dbHandler.getDaoSession();
            Long userId = DBHelper.getUser(session).getId();
            Long deviceId = DBHelper.getDevice(getDevice(), session).getId();

            HPlusHealthActivityOverlayDao overlayDao = session.getHPlusHealthActivityOverlayDao();
            HPlusHealthSampleProvider provider = new HPlusHealthSampleProvider(getDevice(), dbHandler.getDaoSession());

            //Insert the Overlays
            List<HPlusHealthActivityOverlay> overlayList = new ArrayList<>();
            List<HPlusDataRecord.RecordInterval> intervals = record.getIntervals();
            for(HPlusDataRecord.RecordInterval interval : intervals){
                overlayList.add(new HPlusHealthActivityOverlay(interval.timestampFrom, interval.timestampTo, interval.activityKind, deviceId, userId, null));
            }

            overlayDao.insertOrReplaceInTx(overlayList);

            //Store the data
            HPlusHealthActivitySample sample = new HPlusHealthActivitySample(
                    record.timestamp,            // ts
                    deviceId, userId,            // User id
                    record.getRawData(),         // Raw Data
                    record.activityKind,
                    0,                           // Intensity
                    ActivitySample.NOT_MEASURED, // Steps
                    ActivitySample.NOT_MEASURED, // HR
                    ActivitySample.NOT_MEASURED, // Distance
                    ActivitySample.NOT_MEASURED  // Calories
            );

            sample.setProvider(provider);

            provider.addGBActivitySample(sample);


        } catch (Exception ex) {
            LOG.debug(ex.getMessage());
        }

        mGetSleepTime = GregorianCalendar.getInstance();
        mGetSleepTime.add(GregorianCalendar.SECOND, SLEEP_SYNC_PERIOD);

        return true;
    }

    private void requestNextSleepData() {
        mGetSleepTime = GregorianCalendar.getInstance();
        mGetSleepTime.add(GregorianCalendar.SECOND, SLEEP_SYNC_RETRY_PERIOD);

        TransactionBuilder builder = new TransactionBuilder("requestSleepStats");
        builder.write(mHPlusSupport.ctrlCharacteristic, new byte[]{HPlusConstants.CMD_GET_SLEEP});
        builder.queue(mHPlusSupport.getQueue());
    }


    public boolean processRealtimeStats(byte[] data) {
        HPlusDataRecordRealtime record;

        try{
            record = new HPlusDataRecordRealtime(data);
        } catch(IllegalArgumentException e){
            LOG.debug((e.getMessage()));
            return true;
        }

        if(record.same(prevRealTimeRecord))
            return true;

        prevRealTimeRecord = record;

        getDevice().setBatteryLevel(record.battery);
        getDevice().sendDeviceUpdateIntent(getContext());

        //Skip when measuring
        if(record.heartRate == 255) {
            getDevice().setFirmwareVersion2("---");
            getDevice().sendDeviceUpdateIntent(getContext());
            return true;
        }

        getDevice().setFirmwareVersion2("" + record.heartRate);
        getDevice().sendDeviceUpdateIntent(getContext());

        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            HPlusHealthSampleProvider provider = new HPlusHealthSampleProvider(getDevice(), dbHandler.getDaoSession());
            Long userId = DBHelper.getUser(dbHandler.getDaoSession()).getId();
            Long deviceId = DBHelper.getDevice(getDevice(), dbHandler.getDaoSession()).getId();

            HPlusHealthActivitySample sample = new HPlusHealthActivitySample(
                    record.timestamp,            // ts
                    deviceId, userId,            // User id
                    record.getRawData(),         // Raw Data
                    record.activityKind,
                    record.intensity, // Intensity
                    ActivitySample.NOT_MEASURED, // Steps
                    record.heartRate,            // HR
                    record.distance,             // Distance
                    record.calories              // Calories
            );

            sample.setProvider(provider);
            provider.addGBActivitySample(sample);

            //TODO: Handle Active Time. With Overlay?

        } catch (GBException ex) {
            LOG.debug((ex.getMessage()));
        } catch (Exception ex) {
            LOG.debug(ex.getMessage());
        }
        return true;
    }


    public boolean processDaySummary(byte[] data) {
        LOG.debug("Process Day Summary");
        HPlusDataRecordDaySummary record;

        try{
            record = new HPlusDataRecordDaySummary(data);
        } catch(IllegalArgumentException e){
            LOG.debug((e.getMessage()));
            return true;
        }
        LOG.debug("Received: " + record);

        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            HPlusHealthSampleProvider provider = new HPlusHealthSampleProvider(getDevice(), dbHandler.getDaoSession());

            Long userId = DBHelper.getUser(dbHandler.getDaoSession()).getId();
            Long deviceId = DBHelper.getDevice(getDevice(), dbHandler.getDaoSession()).getId();

            //Hugly (?) fix.
            //This message returns the day summary, but the DB already has some detailed entries with steps and distance.
            //However DB data is probably incomplete as some update messages could be missing
            //Proposed fix: Calculate the total steps and distance and store a new sample with the remaining data
            //Existing data will reflect user activity with the issue of a potencially large number of steps at midnight.
            //Steps counters by day will be OK with this

            List<HPlusHealthActivitySample> samples = provider.getActivitySamples(record.timestamp - 3600 * 24 + 1, record.timestamp);

            int missingDistance = record.distance;
            int missingSteps = record.steps;

            for(HPlusHealthActivitySample sample : samples){
                if(sample.getSteps() > 0) {
                    missingSteps -= sample.getSteps();
                }
                if(sample.getDistance() > 0){
                    missingDistance -= sample.getDistance();
                }
            }

            HPlusHealthActivitySample sample = new HPlusHealthActivitySample(
                    record.timestamp,               // ts
                    deviceId, userId,               // User id
                    record.getRawData(),            // Raw Data
                    ActivityKind.TYPE_UNKNOWN,
                    0,                              // Intensity
                    Math.max( missingSteps, 0),     // Steps
                    ActivitySample.NOT_MEASURED,    // HR
                    Math.max( missingDistance, 0),  // Distance
                    ActivitySample.NOT_MEASURED     // Calories
            );

            sample.setProvider(provider);
            provider.addGBActivitySample(sample);
        } catch (GBException ex) {
            LOG.debug((ex.getMessage()));
        } catch (Exception ex) {
            LOG.debug(ex.getMessage());
        }

        return true;
    }

    public boolean processVersion(byte[] data) {
        int major = data[2] & 0xFF;
        int minor = data[1] & 0xFF;

        getDevice().setFirmwareVersion(major + "." + minor);

        getDevice().sendDeviceUpdateIntent(getContext());

        return true;
    }
}