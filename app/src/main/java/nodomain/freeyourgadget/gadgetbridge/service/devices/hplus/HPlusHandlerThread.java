package nodomain.freeyourgadget.gadgetbridge.service.devices.hplus;

/*
* @author João Paulo Barraca &lt;jpbarraca@gmail.com&gt;
*/


import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.hplus.HPlusConstants;
import nodomain.freeyourgadget.gadgetbridge.devices.hplus.HPlusHealthSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.HPlusHealthActivityOverlay;
import nodomain.freeyourgadget.gadgetbridge.entities.HPlusHealthActivityOverlayDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HPlusHealthActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceIoThread;


public class HPlusHandlerThread extends GBDeviceIoThread {

    private int SYNC_PERIOD = 60 * 10;
    private int SYNC_RETRY_PERIOD = 6;
    private int SLEEP_SYNC_PERIOD = 12 * 60 * 60;
    private int SLEEP_RETRY_PERIOD = 30;

    private int HELLO_INTERVAL = 30;

    private static final Logger LOG = LoggerFactory.getLogger(HPlusHandlerThread.class);

    private boolean mQuit = false;
    private HPlusSupport mHPlusSupport;
    private int mLastSlotReceived = 0;
    private int mLastSlotRequested = 0;

    private Calendar mLastSleepDayReceived = Calendar.getInstance();

    private Calendar mHelloTime = Calendar.getInstance();

    private Calendar mGetDaySlotsTime = Calendar.getInstance();
    private Calendar mGetSleepTime = Calendar.getInstance();

    private Object waitObject = new Object();

    private HPlusDataRecordRealtime prevRealTimeRecord = null;

    public HPlusHandlerThread(GBDevice gbDevice, Context context, HPlusSupport hplusSupport) {
        super(gbDevice, context);

        mQuit = false;

        mHPlusSupport = hplusSupport;

        mLastSleepDayReceived.setTimeInMillis(0);
        mGetSleepTime.setTimeInMillis(0);
        mGetDaySlotsTime.setTimeInMillis(0);
    }


    @Override
    public void run() {
        mQuit = false;

        sync();

        boolean starting = true;
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

            Calendar now = Calendar.getInstance();

            if (now.compareTo(mHelloTime) > 0) {
                sendHello();
            }

            if (now.compareTo(mGetDaySlotsTime) > 0) {
                requestNextDaySlots();
            }

            if (now.compareTo(mGetSleepTime) > 0) {
                requestNextSleepData();
            }

            now = Calendar.getInstance();
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

    public void sendHello() {
        mHelloTime = Calendar.getInstance();
        mHelloTime.add(Calendar.SECOND, HELLO_INTERVAL);

        TransactionBuilder builder = new TransactionBuilder("hello");
        builder.write(mHPlusSupport.ctrlCharacteristic, HPlusConstants.CMD_ACTION_HELLO);

        builder.queue(mHPlusSupport.getQueue());
    }


    public void processIncomingDaySlotData(byte[] data) {

        HPlusDataRecordDay record;
        try{
            record = new HPlusDataRecordDay(data);
        } catch(IllegalArgumentException e){
            LOG.debug((e.getMessage()));
            return;
        }

        if ((record.slot == 0 && mLastSlotReceived == 0) || (record.slot == mLastSlotReceived + 1)) {
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
                        ActivitySample.NOT_MEASURED,    // Intensity
                        record.steps,                   // Steps
                        record.heartRate,               // HR
                        ActivitySample.NOT_MEASURED,    // Distance
                        ActivitySample.NOT_MEASURED     // Calories
                );
                sample.setProvider(provider);

                provider.addGBActivitySample(sample);

            } catch (GBException ex) {
                LOG.debug((ex.getMessage()));
            } catch (Exception ex) {
                LOG.debug(ex.getMessage());
            }

            if (record.slot >= mLastSlotRequested) {
                synchronized (waitObject) {
                    mGetDaySlotsTime.setTimeInMillis(0);
                    waitObject.notify();
                }
            }
        }
    }

    private void requestNextDaySlots() {
        LOG.debug("Request Next Slot: Got: " + mLastSlotReceived + " Request: " + mLastSlotRequested);

        //Sync Day Stats
        byte hour = (byte) ((mLastSlotReceived) / 6);
        byte nextHour = (byte) (hour + 1);

        byte nextMinute = 0;

        if (nextHour == (byte) Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            nextMinute = (byte) Calendar.getInstance().get(Calendar.MINUTE);
        }

        byte minute = (byte) ((mLastSlotReceived % 6) * 10);

        mLastSlotRequested = (nextHour) * 6 + Math.round(nextMinute / 10);

        if (nextHour >= 24 && nextMinute > 0) { // 24 * 6
            LOG.debug("Reached End of the Day");
            mLastSlotRequested = 0;
            mLastSlotReceived = 0;
            mGetDaySlotsTime = Calendar.getInstance();
            mGetDaySlotsTime.add(Calendar.SECOND, SYNC_PERIOD);

            return;
        }

        if (nextHour > Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            LOG.debug("Day data is up to date");
            mGetDaySlotsTime = Calendar.getInstance();
            mGetDaySlotsTime.add(Calendar.SECOND, SYNC_PERIOD);
            return;
        }

        LOG.debug("Making new Request From " + hour + ":" + minute + " to " + nextHour + ":" + nextMinute);

        byte[] msg = new byte[]{39, hour, minute, nextHour, nextMinute}; //Request the entire day
        TransactionBuilder builder = new TransactionBuilder("getNextDaySlot");
        builder.write(mHPlusSupport.ctrlCharacteristic, msg);
        builder.queue(mHPlusSupport.getQueue());

        mGetDaySlotsTime = Calendar.getInstance();
        mGetDaySlotsTime.add(Calendar.SECOND, SYNC_RETRY_PERIOD);
    }

    public void processIncomingSleepData(byte[] data){
        LOG.debug("Processing Sleep Data");

        HPlusDataRecordSleep record;

        try{
            record = new HPlusDataRecordSleep(data);
        } catch(IllegalArgumentException e){
            LOG.debug((e.getMessage()));
            return;
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
                    ActivitySample.NOT_MEASURED, // Intensity
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

        mGetSleepTime = Calendar.getInstance();
        mGetSleepTime.add(Calendar.SECOND, SLEEP_SYNC_PERIOD);

    }

    private void requestNextSleepData() {
        LOG.debug("Request New Sleep Data");

        mGetSleepTime = Calendar.getInstance();
        mGetSleepTime.add(Calendar.SECOND, SLEEP_RETRY_PERIOD);

        TransactionBuilder builder = new TransactionBuilder("requestSleepStats");
        builder.write(mHPlusSupport.ctrlCharacteristic, new byte[]{HPlusConstants.CMD_GET_SLEEP});
        builder.queue(mHPlusSupport.getQueue());
    }


    public void processRealtimeStats(byte[] data) {
        LOG.debug("Processing Real time Stats");

        HPlusDataRecordRealtime record;

        try{
            record = new HPlusDataRecordRealtime(data);
        } catch(IllegalArgumentException e){
            LOG.debug((e.getMessage()));
            return;
        }

        if(record.same(prevRealTimeRecord))
            return;

        prevRealTimeRecord = record;

        getDevice().setBatteryLevel(record.battery);
        getDevice().sendDeviceUpdateIntent(getContext());

        //Skip when measuring
        if(record.heartRate == 255) {
            getDevice().setFirmwareVersion2("---");
            getDevice().sendDeviceUpdateIntent(getContext());
            return;
        }

        getDevice().setFirmwareVersion2(""+record.heartRate);
        getDevice().sendDeviceUpdateIntent(getContext());

        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            DaoSession session = dbHandler.getDaoSession();

            HPlusHealthSampleProvider provider = new HPlusHealthSampleProvider(getDevice(), dbHandler.getDaoSession());
            HPlusHealthActivityOverlayDao overlayDao = session.getHPlusHealthActivityOverlayDao();

            Long userId = DBHelper.getUser(dbHandler.getDaoSession()).getId();
            Long deviceId = DBHelper.getDevice(getDevice(), dbHandler.getDaoSession()).getId();

            HPlusHealthActivitySample sample = new HPlusHealthActivitySample(
                    record.timestamp,            // ts
                    deviceId, userId,            // User id
                    record.getRawData(),         // Raw Data
                    record.activityKind,
                    ActivitySample.NOT_MEASURED, // Intensity
                    ActivitySample.NOT_MEASURED, // Steps
                    record.heartRate,            // HR
                    record.distance,             // Distance
                    record.calories              // Calories
            );

            sample.setProvider(provider);
            provider.addGBActivitySample(sample);

            if(record.activeTime > 0){
                //TODO: Register ACTIVITY Time

                //Insert the Overlays
                //List<HPlusHealthActivityOverlay> overlayList = new ArrayList<>();
                //overlayList.add(new HPlusHealthActivityOverlay(record.timestamp - record.activeTime * 60, record.timestamp, ActivityKind.TYPE_ACTIVITY, deviceId, userId, null));
                //overlayDao.insertOrReplaceInTx(overlayList);
            }

        } catch (GBException ex) {
            LOG.debug((ex.getMessage()));
        } catch (Exception ex) {
            LOG.debug(ex.getMessage());
        }
    }


    public void processStepStats(byte[] data) {
        LOG.debug("Processing Step Stats");
        HPlusDataRecordSteps record;

        try{
            record = new HPlusDataRecordSteps(data);
        } catch(IllegalArgumentException e){
            LOG.debug((e.getMessage()));
            return;
        }

        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            HPlusHealthSampleProvider provider = new HPlusHealthSampleProvider(getDevice(), dbHandler.getDaoSession());

            Long userId = DBHelper.getUser(dbHandler.getDaoSession()).getId();
            Long deviceId = DBHelper.getDevice(getDevice(), dbHandler.getDaoSession()).getId();
            HPlusHealthActivitySample sample = new HPlusHealthActivitySample(
                    record.timestamp,               // ts
                    deviceId, userId,               // User id
                    record.getRawData(),            // Raw Data
                    ActivityKind.TYPE_UNKNOWN,
                    ActivitySample.NOT_MEASURED,    // Intensity
                    record.steps,                   // Steps
                    ActivitySample.NOT_MEASURED,    // HR
                    record.distance,                // Distance
                    ActivitySample.NOT_MEASURED     // Calories
            );
            sample.setProvider(provider);
            provider.addGBActivitySample(sample);
        } catch (GBException ex) {
            LOG.debug((ex.getMessage()));
        } catch (Exception ex) {
            LOG.debug(ex.getMessage());
        }
    }

    public boolean processVersion(byte[] data) {
        LOG.debug("Process Version");

        int major = data[2] & 0xFF;
        int minor = data[1] & 0xFF;

        getDevice().setFirmwareVersion(major + "." + minor);

        getDevice().sendDeviceUpdateIntent(getContext());

        return true;
    }

    public static HPlusHealthActivitySample createActivitySample(Device device, User user, int timestampInSeconds, SampleProvider provider) {
        HPlusHealthActivitySample sample = new HPlusHealthActivitySample();
        sample.setDevice(device);
        sample.setUser(user);
        sample.setTimestamp(timestampInSeconds);
        sample.setProvider(provider);

        return sample;
    }
}