package nodomain.freeyourgadget.gadgetbridge.devices.hplus;

/*
* @author João Paulo Barraca &lt;jpbarraca@gmail.com&gt;
*/

import android.support.annotation.NonNull;
import android.util.Log;

import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.List;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.HPlusHealthActivityOverlay;
import nodomain.freeyourgadget.gadgetbridge.entities.HPlusHealthActivityOverlayDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HPlusHealthActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.HPlusHealthActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.service.devices.hplus.HPlusDataRecord;

public class HPlusHealthSampleProvider extends AbstractSampleProvider<HPlusHealthActivitySample> {

    private GBDevice mDevice;
    private DaoSession mSession;

    public HPlusHealthSampleProvider(GBDevice device, DaoSession session) {
        super(device, session);

        mSession = session;
        mDevice = device;
    }

    public int getID() {

        return SampleProvider.PROVIDER_HPLUS;
    }

    public int normalizeType(int rawType) {
        switch (rawType){
            case HPlusDataRecord.TYPE_DAY_SLOT:
            case HPlusDataRecord.TYPE_DAY_SUMMARY:
            case HPlusDataRecord.TYPE_REALTIME:
            case HPlusDataRecord.TYPE_SLEEP:
            case HPlusDataRecord.TYPE_UNKNOWN:
                return ActivityKind.TYPE_UNKNOWN;
            default:
                return rawType;
        }
    }

    public int toRawActivityKind(int activityKind) {
        switch (activityKind){
            case ActivityKind.TYPE_DEEP_SLEEP:
                return ActivityKind.TYPE_DEEP_SLEEP;
            case ActivityKind.TYPE_LIGHT_SLEEP:
                return ActivityKind.TYPE_LIGHT_SLEEP;
            default:
                return HPlusDataRecord.TYPE_DAY_SLOT;
        }

    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return HPlusHealthActivitySampleDao.Properties.Timestamp;
    }

    @Override
    public HPlusHealthActivitySample createActivitySample() {
        return new HPlusHealthActivitySample();
    }

    @Override
    protected Property getRawKindSampleProperty() {
        return null; // HPlusHealthActivitySampleDao.Properties.RawKind;
    }

    @Override
    public float normalizeIntensity(int rawIntensity) {
        return rawIntensity / (float) 100.0;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return HPlusHealthActivitySampleDao.Properties.DeviceId;
    }

    @Override
    public AbstractDao<HPlusHealthActivitySample, ?> getSampleDao() {
        return getSession().getHPlusHealthActivitySampleDao();
    }


    public List<HPlusHealthActivitySample> getActivityamples(int timestamp_from, int timestamp_to) {
        return getAllActivitySamples(timestamp_from, timestamp_to);
    }

    public List<HPlusHealthActivitySample> getSleepSamples(int timestamp_from, int timestamp_to) {
        return getAllActivitySamples(timestamp_from, timestamp_to);
    }

    @NonNull
    @Override
    public List<HPlusHealthActivitySample> getAllActivitySamples(int timestamp_from, int timestamp_to) {
        List<HPlusHealthActivitySample> samples = super.getGBActivitySamples(timestamp_from, timestamp_to, ActivityKind.TYPE_ALL);

        Device dbDevice = DBHelper.findDevice(getDevice(), getSession());
        if (dbDevice == null) {
            return Collections.emptyList();
        }

        QueryBuilder<HPlusHealthActivityOverlay> qb = getSession().getHPlusHealthActivityOverlayDao().queryBuilder();

        qb.where(HPlusHealthActivityOverlayDao.Properties.DeviceId.eq(dbDevice.getId()),
                HPlusHealthActivityOverlayDao.Properties.TimestampFrom.ge(timestamp_from - 3600 * 24),
                HPlusHealthActivityOverlayDao.Properties.TimestampTo.le(timestamp_to),
                HPlusHealthActivityOverlayDao.Properties.TimestampTo.ge(timestamp_from));

        List<HPlusHealthActivityOverlay> overlayRecords = qb.build().list();

        //Todays sample steps will come from the Day Slots messages
        //Historical steps will be provided by Day Summaries messages
        //This will allow both week and current day results to be consistent
        Calendar today = GregorianCalendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        int stepsTodayMax = 0;
        int stepsTodayCount = 0;
        HPlusHealthActivitySample lastSample = null;

        for(HPlusHealthActivitySample sample: samples){
            if(sample.getTimestamp() >= today.getTimeInMillis() / 1000){

                /**Strategy is:
                 * Calculate max steps from realtime messages
                 * Calculate sum of steps from day 10 minute slot summaries
                 */

                 if(sample.getRawKind() == HPlusDataRecord.TYPE_REALTIME) {
                    stepsTodayMax = Math.max(stepsTodayMax, sample.getSteps());
                 }else if(sample.getRawKind() == HPlusDataRecord.TYPE_DAY_SLOT) {
                     stepsTodayCount += sample.getSteps();
                 }

                sample.setSteps(ActivitySample.NOT_MEASURED);
                lastSample = sample;
            }else{
                if (sample.getRawKind() != HPlusDataRecord.TYPE_DAY_SUMMARY) {
                    sample.setSteps(ActivitySample.NOT_MEASURED);
                }
            }
        }

        if(lastSample != null)
            lastSample.setSteps(Math.max(stepsTodayCount, stepsTodayMax));

        for (HPlusHealthActivityOverlay overlay : overlayRecords) {

            //Create fake events to improve activity counters if there are no events around the overlay
            //timestamp boundaries
            //Insert one before, one at the beginning, one at the end, and one 1s after.
            insertVirtualItem(samples, Math.max(overlay.getTimestampFrom() - 1, timestamp_from), overlay.getDeviceId(), overlay.getUserId());
            insertVirtualItem(samples, Math.max(overlay.getTimestampFrom(), timestamp_from), overlay.getDeviceId(), overlay.getUserId());
            insertVirtualItem(samples, Math.min(overlay.getTimestampTo() - 1, timestamp_to - 1), overlay.getDeviceId(), overlay.getUserId());
            insertVirtualItem(samples, Math.min(overlay.getTimestampTo(), timestamp_to), overlay.getDeviceId(), overlay.getUserId());

            for (HPlusHealthActivitySample sample : samples) {

                if (sample.getTimestamp() >= overlay.getTimestampFrom() && sample.getTimestamp() < overlay.getTimestampTo()) {
                    sample.setRawKind(overlay.getRawKind());
                }
            }
        }

        detachFromSession();

        Collections.sort(samples, new Comparator<HPlusHealthActivitySample>() {
            public int compare(HPlusHealthActivitySample one, HPlusHealthActivitySample other) {
                return one.getTimestamp() - other.getTimestamp();
            }
        });

        return samples;
    }

    private List<HPlusHealthActivitySample> insertVirtualItem(List<HPlusHealthActivitySample> samples, int timestamp, long deviceId, long userId) {
        HPlusHealthActivitySample sample = new HPlusHealthActivitySample(
                timestamp,            // ts
                deviceId,
                userId,          // User id
                null,                         // Raw Data
                ActivityKind.TYPE_UNKNOWN,
                1, // Intensity
                ActivitySample.NOT_MEASURED, // Steps
                ActivitySample.NOT_MEASURED, // HR
                ActivitySample.NOT_MEASURED, // Distance
                ActivitySample.NOT_MEASURED  // Calories
        );

        sample.setProvider(this);
        samples.add(sample);

        return samples;
    }
}

