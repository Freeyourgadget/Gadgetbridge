/*  Copyright (C) 2017 João Paulo Barraca

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.hplus;

/*
* @author João Paulo Barraca &lt;jpbarraca@gmail.com&gt;
*/


import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.hplus.HPlusConstants;
import nodomain.freeyourgadget.gadgetbridge.devices.hplus.HPlusCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.hplus.HPlusHealthSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.HPlusHealthActivityOverlay;
import nodomain.freeyourgadget.gadgetbridge.entities.HPlusHealthActivityOverlayDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HPlusHealthActivitySample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceIoThread;


class HPlusHandlerThread extends GBDeviceIoThread {
    private static final Logger LOG = LoggerFactory.getLogger(HPlusHandlerThread.class);

    private int CURRENT_DAY_SYNC_PERIOD = 24 * 60 * 60 * 365; //Never
    private int CURRENT_DAY_SYNC_RETRY_PERIOD = 10;

    private int SLEEP_SYNC_PERIOD = 12 * 60 * 60;
    private int SLEEP_SYNC_RETRY_PERIOD = 30;

    private int DAY_SUMMARY_SYNC_PERIOD = 24 * 60 * 60;
    private int DAY_SUMMARY_SYNC_RETRY_PERIOD = 30;

    private int HELLO_PERIOD = 60 * 2;

    private boolean mQuit = false;
    private HPlusSupport mHPlusSupport;

    private int mLastSlotReceived = -1;
    private int mLastSlotRequested = 0;

    private Calendar mLastSleepDayReceived = GregorianCalendar.getInstance();
    private Calendar mGetDaySlotsTime = GregorianCalendar.getInstance();
    private Calendar mGetSleepTime = GregorianCalendar.getInstance();
    private Calendar mGetDaySummaryTime = GregorianCalendar.getInstance();

    private Calendar mHelloTime = GregorianCalendar.getInstance();

    private boolean mSlotsInitialSync = true;

    private HPlusDataRecordRealtime prevRealTimeRecord = null;

    private final Object waitObject = new Object();

    List<HPlusDataRecordDaySlot> mDaySlotRecords = new ArrayList<>();

    private HPlusDataRecordDaySlot mCurrentDaySlot = null;

    public HPlusHandlerThread(GBDevice gbDevice, Context context, HPlusSupport hplusSupport) {
        super(gbDevice, context);
        LOG.info("Initializing HPlus Handler Thread");
        mQuit = false;

        mHPlusSupport = hplusSupport;
    }


    @Override
    public void run() {
        mQuit = false;

        sync();

        long waitTime = 0;
        while (!mQuit) {

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

            if(gbDevice.getState() == GBDevice.State.NOT_CONNECTED){
                quit();
            }

            Calendar now = GregorianCalendar.getInstance();

            if (now.compareTo(mGetDaySlotsTime) > 0) {
                requestNextDaySlots();
            }

            if (now.compareTo(mGetSleepTime) > 0) {
                requestNextSleepData();
            }

            if(now.compareTo(mGetDaySummaryTime) > 0) {
                requestDaySummaryData();
            }

            if(now.compareTo(mHelloTime) > 0){
                sendHello();
            }

            now = GregorianCalendar.getInstance();
            waitTime = Math.min(mGetDaySummaryTime.getTimeInMillis(), Math.min(mGetDaySlotsTime.getTimeInMillis(), Math.min(mHelloTime.getTimeInMillis(), mGetSleepTime.getTimeInMillis()))) - now.getTimeInMillis();
        }

    }

    @Override
    public void quit() {
        LOG.info("HPlus: Quit Handler Thread");
        mQuit = true;
        synchronized (waitObject) {
            waitObject.notify();
        }
    }


    public void sync() {
        LOG.info("HPlus: Starting data synchronization");

        mGetSleepTime.setTimeInMillis(0);
        mGetDaySlotsTime.setTimeInMillis(0);
        mGetDaySummaryTime.setTimeInMillis(0);
        mLastSleepDayReceived.setTimeInMillis(0);
        mHelloTime = GregorianCalendar.getInstance();
        mHelloTime.add(Calendar.SECOND, HELLO_PERIOD);

        mSlotsInitialSync = true;
        mLastSlotReceived = -1;
        mLastSlotRequested = 0;
        mCurrentDaySlot = null;
        mDaySlotRecords.clear();

        try {
            if(!mHPlusSupport.isConnected())
                mHPlusSupport.connect();

            TransactionBuilder builder = new TransactionBuilder("startSyncDayStats");

            builder.write(mHPlusSupport.ctrlCharacteristic, new byte[]{HPlusConstants.CMD_GET_DEVICE_ID});
            builder.write(mHPlusSupport.ctrlCharacteristic, new byte[]{HPlusConstants.CMD_GET_VERSION});
            builder.write(mHPlusSupport.ctrlCharacteristic, new byte[]{HPlusConstants.CMD_GET_CURR_DATA});

            mHPlusSupport.performConnected(builder.getTransaction());
        }catch(Exception e){
            LOG.warn("HPlus: Synchronization exception: " + e);
        }

        synchronized (waitObject) {
            waitObject.notify();
        }
    }

    public void sendHello(){
        try {
            TransactionBuilder builder = new TransactionBuilder("hello");
            builder.write(mHPlusSupport.ctrlCharacteristic, HPlusConstants.CMD_ACTION_HELLO);
            mHPlusSupport.performConnected(builder.getTransaction());

        }catch(Exception e){

        }
        mHelloTime = GregorianCalendar.getInstance();
        mHelloTime.add(Calendar.SECOND, HELLO_PERIOD);

        synchronized (waitObject) {
            waitObject.notify();
        }
    }
    /**
     * Process a message containing information regarding a day slot
     * A slot summarizes 10 minutes of data
     *
     * @param data the message from the device
     * @return boolean indicating success or fail
     */
    public boolean processIncomingDaySlotData(byte[] data, int age) {

        HPlusDataRecordDaySlot record;

        try{
            record = new HPlusDataRecordDaySlot(data, age);
        } catch(IllegalArgumentException e){
            LOG.info((e.getMessage()));
            return false;
        }

        Calendar now = GregorianCalendar.getInstance();
        int nowSlot = now.get(Calendar.HOUR_OF_DAY) * 6 + (now.get(Calendar.MINUTE) / 10);
        if(record.slot == nowSlot){
            if(mCurrentDaySlot != null && mCurrentDaySlot != record){
                mCurrentDaySlot.accumulate(record);
                mDaySlotRecords.add(mCurrentDaySlot);
                mCurrentDaySlot = null;
            }else{
                //Store it to a temp variable as this is an intermediate value
                mCurrentDaySlot = record;
                if(!mSlotsInitialSync)
                    return true;
            }
        }

        if(mSlotsInitialSync) {

            //If the slot is in the future, actually it is from the previous day
            //Subtract a day of seconds
            if(record.slot > nowSlot){
                record.timestamp -= 3600 * 24;
            }

            if (record.slot == mLastSlotReceived + 1) {
                mLastSlotReceived = record.slot;
            }

            //Ignore the current slot as it is incomplete
            if(record.slot != nowSlot)
                mDaySlotRecords.add(record);

            //Still fetching ring buffer. Request the next slots
            if (record.slot == mLastSlotRequested) {
                mGetDaySlotsTime.clear();
                synchronized (waitObject) {
                    waitObject.notify();
                }
            }

            //Keep buffering
            if(record.slot != 143)
                return true;
        }  else {
            mGetDaySlotsTime = GregorianCalendar.getInstance();
            mGetDaySlotsTime.add(Calendar.DAY_OF_MONTH, 1);
        }

        if(mDaySlotRecords.size() > 0) {
            //Sort the samples
            Collections.sort(mDaySlotRecords, new Comparator<HPlusDataRecordDaySlot>() {
                public int compare(HPlusDataRecordDaySlot one, HPlusDataRecordDaySlot other) {
                    return one.timestamp - other.timestamp;
                }
            });

            try (DBHandler dbHandler = GBApplication.acquireDB()) {
                HPlusHealthSampleProvider provider = new HPlusHealthSampleProvider(getDevice(), dbHandler.getDaoSession());
                List<HPlusHealthActivitySample> samples = new ArrayList<>();

                for (HPlusDataRecordDaySlot storedRecord : mDaySlotRecords) {

                    //Invalid records (no data) will be ignored
                    if(!storedRecord.isValid())
                        continue;

                    HPlusHealthActivitySample sample = createSample(dbHandler, storedRecord.timestamp);

                    sample.setRawHPlusHealthData(storedRecord.getRawData());
                    sample.setSteps(storedRecord.steps);
                    sample.setRawIntensity(storedRecord.intensity);
                    sample.setHeartRate(storedRecord.heartRate);
                    sample.setRawKind(storedRecord.type);
                    sample.setProvider(provider);
                    samples.add(sample);
                }

                provider.getSampleDao().insertOrReplaceInTx(samples);
                mDaySlotRecords.clear();

            } catch (GBException ex) {
                LOG.info((ex.getMessage()));
            } catch (Exception ex) {
                LOG.info(ex.getMessage());
            }
        }

        return true;
    }


    /**
     * Process sleep data from the device
     * Devices send a single sleep message for each sleep period
     * This message contains the duration of the sub-intervals (rem, deep, etc...)
     *
     * @param data the message from the device
     * @return boolean indicating success or fail
     */
    public boolean processIncomingSleepData(byte[] data){
        HPlusDataRecordSleep record;

        try{
            record = new HPlusDataRecordSleep(data);
        } catch(IllegalArgumentException e){
            LOG.info((e.getMessage()));
            return false;
        }

        mLastSleepDayReceived.setTimeInMillis(record.bedTimeStart * 1000L);

        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            DaoSession session = dbHandler.getDaoSession();
            Long userId = DBHelper.getUser(session).getId();
            Long deviceId = DBHelper.getDevice(getDevice(), session).getId();

            HPlusHealthActivityOverlayDao overlayDao = session.getHPlusHealthActivityOverlayDao();
            HPlusHealthSampleProvider provider = new HPlusHealthSampleProvider(getDevice(), dbHandler.getDaoSession());

            //Get the individual Sleep overlays and insert them
            List<HPlusHealthActivityOverlay> overlayList = new ArrayList<>();
            List<HPlusDataRecord.RecordInterval> intervals = record.getIntervals();

            for(HPlusDataRecord.RecordInterval interval : intervals){
                overlayList.add(new HPlusHealthActivityOverlay(interval.timestampFrom, interval.timestampTo, interval.activityKind, deviceId, userId, null));
            }

            overlayDao.insertOrReplaceInTx(overlayList);

            //Store the data
            HPlusHealthActivitySample sample = createSample(dbHandler, record.timestamp);
            sample.setRawHPlusHealthData(record.getRawData());
            sample.setRawKind(record.activityKind);

            sample.setProvider(provider);

            provider.addGBActivitySample(sample);
        } catch (Exception ex) {
            LOG.info(ex.getMessage());
        }

        mGetSleepTime = GregorianCalendar.getInstance();
        mGetSleepTime.add(GregorianCalendar.SECOND, SLEEP_SYNC_PERIOD);

        return true;
    }

    /**
     * Process a message containing real time information
     *
     * @param data the message from the device
     * @return boolean indicating success or fail
     */
    public boolean processRealtimeStats(byte[] data, int age) {
        HPlusDataRecordRealtime record;

        try{
            record = new HPlusDataRecordRealtime(data, age);
        } catch(IllegalArgumentException e){
            LOG.info((e.getMessage()));
            return false;
        }

        //Skip duplicated messages as the device seems to send the same record multiple times
        //This can be used to detect the user is moving (not sleeping)
        if(prevRealTimeRecord != null && record.same(prevRealTimeRecord))
            return true;

        prevRealTimeRecord = record;

        getDevice().setBatteryLevel(record.battery);

        //Skip when measuring heart rate
        //Calories and Distance are updated and these values will be lost.
        //Because a message with a valid Heart Rate will be provided, this loss very limited
        if(record.heartRate == ActivityKind.TYPE_NOT_MEASURED) {
            getDevice().setFirmwareVersion2("---");
            getDevice().sendDeviceUpdateIntent(getContext());
        }else {
            getDevice().setFirmwareVersion2("" + record.heartRate);
            getDevice().sendDeviceUpdateIntent(getContext());
        }

        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            HPlusHealthSampleProvider provider = new HPlusHealthSampleProvider(getDevice(), dbHandler.getDaoSession());

            HPlusHealthActivitySample sample = createSample(dbHandler, record.timestamp);
            sample.setRawKind(record.type);
            sample.setRawIntensity(record.intensity);
            sample.setHeartRate(record.heartRate);
            sample.setDistance(record.distance);
            sample.setCalories(record.calories);
            sample.setSteps(record.steps);

            sample.setRawHPlusHealthData(record.getRawData());
            sample.setProvider(provider);

            provider.addGBActivitySample(sample);

            sample.setSteps(sample.getSteps() - prevRealTimeRecord.steps);

            Intent intent = new Intent(DeviceService.ACTION_REALTIME_SAMPLES)
                    .putExtra(DeviceService.EXTRA_REALTIME_SAMPLE, sample)
                    .putExtra(DeviceService.EXTRA_TIMESTAMP, System.currentTimeMillis());
            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);


            //TODO: Handle Active Time. With Overlay?
        } catch (GBException ex) {
            LOG.info((ex.getMessage()));
        } catch (Exception ex) {
            LOG.info(ex.getMessage());
        }
        return true;
    }

    /**
     * Process a day summary message
     * This message includes aggregates regarding an entire day
     *
     * @param data the message from the device
     * @return boolean indicating success or fail
     */
    public boolean processDaySummary(byte[] data) {
        HPlusDataRecordDaySummary record;

        try{
            record = new HPlusDataRecordDaySummary(data);
        } catch(IllegalArgumentException e){
            LOG.info((e.getMessage()));
            return false;
        }

        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            HPlusHealthSampleProvider provider = new HPlusHealthSampleProvider(getDevice(), dbHandler.getDaoSession());

            HPlusHealthActivitySample sample = createSample(dbHandler, record.timestamp);

            sample.setRawKind(record.type);
            sample.setSteps(record.steps);
            sample.setDistance(record.distance);
            sample.setCalories(record.calories);
            sample.setDistance(record.distance);
            sample.setHeartRate((record.maxHeartRate - record.minHeartRate) / 2); //TODO: Find an alternative approach for Day Summary Heart Rate
            sample.setRawHPlusHealthData(record.getRawData());

            sample.setProvider(provider);
            provider.addGBActivitySample(sample);
        } catch (GBException ex) {
            LOG.info((ex.getMessage()));
        } catch (Exception ex) {
            LOG.info(ex.getMessage());
        }

        mGetDaySummaryTime = GregorianCalendar.getInstance();
        mGetDaySummaryTime.add(Calendar.SECOND, DAY_SUMMARY_SYNC_PERIOD);
        return true;
    }

    /**
     * Process a message containing information regarding firmware version
     *
     * @param data the message from the device
     * @return boolean indicating success or fail
     */
    public boolean processVersion(byte[] data) {
        int major, minor;

        if(data.length >= 11){
            major = data[10] & 0xFF;
            minor = data[9] & 0xFF;

            int hwMajor = data[2] & 0xFF;
            int hwMinor = data[1] & 0xFF;

            getDevice().setFirmwareVersion2(hwMajor + "." + hwMinor);
            mHPlusSupport.setUnicodeSupport((data[3] != 0));
        }else {
            major = data[2] & 0xFF;
            minor = data[1] & 0xFF;
        }

        getDevice().setFirmwareVersion(major + "." + minor);
        getDevice().sendDeviceUpdateIntent(getContext());

        return true;
    }

    /**
     * Issue a message requesting the next batch of sleep data
     */
    private void requestNextSleepData() {
        try {
            TransactionBuilder builder = new TransactionBuilder("requestSleepStats");
            builder.write(mHPlusSupport.ctrlCharacteristic, new byte[]{HPlusConstants.CMD_GET_SLEEP});
            mHPlusSupport.performConnected(builder.getTransaction());
        }catch(Exception e){

        }

        mGetSleepTime = GregorianCalendar.getInstance();
        mGetSleepTime.add(GregorianCalendar.SECOND, SLEEP_SYNC_RETRY_PERIOD);
    }

    /**
     * Issue a message requesting the next set of slots
     * The process will sync 1h at a time until the device is in sync
     * Then it will request samples until the end of the day in order to minimize data loss
     * Messages will be provided every 10 minutes after they are available
     */
    private void requestNextDaySlots() {
        Calendar now = GregorianCalendar.getInstance();
        int currentSlot = now.get(Calendar.HOUR_OF_DAY) * 6 + now.get(Calendar.MINUTE) / 10;

        //Finished dumping the entire ring buffer
        //Sync to current time
        mGetDaySlotsTime = now;

        if(mSlotsInitialSync) {
            if(mLastSlotReceived == 143) {
                mSlotsInitialSync = false;
                mGetDaySlotsTime.set(Calendar.SECOND, CURRENT_DAY_SYNC_PERIOD); //Sync complete. Delay timer forever
                mLastSlotReceived = -1;
                mLastSlotRequested = mLastSlotReceived + 1;
                return;
            }else {
                mGetDaySlotsTime.add(Calendar.SECOND, CURRENT_DAY_SYNC_RETRY_PERIOD);
            }
        }else{
            //Sync complete. Delay timer forever
            mGetDaySlotsTime.set(Calendar.SECOND, CURRENT_DAY_SYNC_PERIOD);
            return;
        }

        if(mLastSlotReceived == 143)
            mLastSlotReceived = -1;

        byte hour = (byte) ((mLastSlotReceived + 1)/ 6);
        byte minute = (byte) (((mLastSlotReceived + 1) % 6) * 10);

        byte nextHour = hour;
        byte nextMinute = 59;

        mLastSlotRequested = nextHour * 6 + (nextMinute / 10);

        byte[] msg = new byte[]{HPlusConstants.CMD_GET_ACTIVE_DAY, hour, minute, nextHour, nextMinute};
        try {

            TransactionBuilder builder = new TransactionBuilder("getNextDaySlot");
            builder.write(mHPlusSupport.ctrlCharacteristic, msg);
            mHPlusSupport.performConnected(builder.getTransaction());
        }catch(Exception e){

        }
    }
    /**
     * Request a batch of data with the summary of the previous days
     */
    public void requestDaySummaryData(){
        try {
            TransactionBuilder builder = new TransactionBuilder("startSyncDaySummary");
            builder.write(mHPlusSupport.ctrlCharacteristic, new byte[]{HPlusConstants.CMD_GET_DAY_DATA});
            mHPlusSupport.performConnected(builder.getTransaction());
        }catch(Exception e){

        }
        mGetDaySummaryTime = GregorianCalendar.getInstance();
        mGetDaySummaryTime.add(Calendar.SECOND, DAY_SUMMARY_SYNC_RETRY_PERIOD);
    }

    /**
     * Helper function to create a sample
     * @param dbHandler The database handler
     * @param timestamp The sample timestamp
     * @return The sample just created
     */
    private HPlusHealthActivitySample createSample(DBHandler dbHandler, int timestamp){
        Long userId = DBHelper.getUser(dbHandler.getDaoSession()).getId();
        Long deviceId = DBHelper.getDevice(getDevice(), dbHandler.getDaoSession()).getId();
        HPlusHealthActivitySample sample = new HPlusHealthActivitySample(
                timestamp,                      // ts
                deviceId, userId,               // User id
                null,            // Raw Data
                ActivityKind.TYPE_UNKNOWN,
                0,                              // Intensity
                ActivitySample.NOT_MEASURED,     // Steps
                ActivitySample.NOT_MEASURED,    // HR
                ActivitySample.NOT_MEASURED,  // Distance
                ActivitySample.NOT_MEASURED     // Calories
        );

        return sample;
    }

    public void setHPlusSupport(HPlusSupport HPlusSupport) {
        LOG.info("Updating HPlusSupport object");
        this.mHPlusSupport = HPlusSupport;
    }
}
