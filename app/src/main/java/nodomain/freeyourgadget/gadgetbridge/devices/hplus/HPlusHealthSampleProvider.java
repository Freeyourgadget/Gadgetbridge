package nodomain.freeyourgadget.gadgetbridge.devices.hplus;

/*
* @author João Paulo Barraca &lt;jpbarraca@gmail.com&gt;
*/

import android.support.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Comparator;
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

public class HPlusHealthSampleProvider extends AbstractSampleProvider<HPlusHealthActivitySample> {
    private static final Logger LOG = LoggerFactory.getLogger(HPlusHealthSampleProvider.class);


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

        return rawType;
    }

    public int toRawActivityKind(int activityKind) {

        return activityKind;
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
        return HPlusHealthActivitySampleDao.Properties.RawKind;
    }

    @Override
    public float normalizeIntensity(int rawIntensity) {
        return rawIntensity; //TODO: Calculate actual value
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

    @Override
    public List<HPlusHealthActivitySample> getAllActivitySamples(int timestamp_from, int timestamp_to) {
        List<HPlusHealthActivitySample> samples = super.getGBActivitySamples(timestamp_from, timestamp_to, ActivityKind.TYPE_ALL);

        Device dbDevice = DBHelper.findDevice(getDevice(), getSession());
        if (dbDevice == null) {
            return Collections.emptyList();
        }

        QueryBuilder<HPlusHealthActivityOverlay> qb = getSession().getHPlusHealthActivityOverlayDao().queryBuilder();

        qb.where(HPlusHealthActivityOverlayDao.Properties.DeviceId.eq(dbDevice.getId()), HPlusHealthActivityOverlayDao.Properties.TimestampFrom.ge(timestamp_from - 24 * 60 * 60))
                .where(HPlusHealthActivityOverlayDao.Properties.TimestampTo.le(timestamp_to));

        List<HPlusHealthActivityOverlay> overlayRecords = qb.build().list();

        for (HPlusHealthActivityOverlay overlay : overlayRecords) {
            insertVirtualItem(samples, overlay.getTimestampFrom(), overlay.getDeviceId(), overlay.getUserId());
            insertVirtualItem(samples, overlay.getTimestampTo() - 1, overlay.getDeviceId(), overlay.getUserId());

            for (HPlusHealthActivitySample sample : samples) {
                if (overlay.getTimestampFrom() <= sample.getTimestamp() && sample.getTimestamp() < overlay.getTimestampTo()) {
                    sample.setRawKind(overlay.getRawKind());
                }
            }
        }

        detachFromSession();

        LOG.debug("Returning " + samples.size() + " samples processed by " + overlayRecords.size() + " overlays");

        Collections.sort(samples, new Comparator<HPlusHealthActivitySample>() {
            public int compare(HPlusHealthActivitySample one, HPlusHealthActivitySample other) {
                return one.getTimestamp() - other.getTimestamp();
            }
        });

        return samples;
    }

    private List<HPlusHealthActivitySample> insertVirtualItem(List<HPlusHealthActivitySample> samples, int timestamp, long deviceId, long userId){
        HPlusHealthActivitySample sample = new HPlusHealthActivitySample(
                timestamp,            // ts
                deviceId,
                userId,          // User id
                null,                         // Raw Data
                ActivityKind.TYPE_UNKNOWN,
                ActivitySample.NOT_MEASURED, // Intensity
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
