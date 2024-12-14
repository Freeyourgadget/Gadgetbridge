package nodomain.freeyourgadget.gadgetbridge.devices.huawei;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiDictData;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiDictDataDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiDictDataValues;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiDictDataValuesDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.TemperatureSample;

public class HuaweiTemperatureSampleProvider implements TimeSampleProvider<TemperatureSample> {

    private final Logger LOG = LoggerFactory.getLogger(HuaweiTemperatureSampleProvider.class);

    protected static class HuaweiTemperatureSample implements TemperatureSample {
        private final long timestamp;
        private final float temperature;

        public HuaweiTemperatureSample(long timestamp, float temperature) {
            this.timestamp = timestamp;
            this.temperature = temperature;
        }

        @Override
        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public float getTemperature() {
            return temperature;
        }

        @Override
        public int getTemperatureType() { return 0;}
    }

    private final GBDevice device;
    private final DaoSession session;

    public HuaweiTemperatureSampleProvider(GBDevice device, DaoSession session) {
        this.device = device;
        this.session = session;
    }

    private double conv2Double(byte[] b) {
        return ByteBuffer.wrap(b).getDouble();
    }

    @NonNull
    @Override
    public List<TemperatureSample> getAllSamples(long timestampFrom, long timestampTo) {

        List<TemperatureSample> ret = new ArrayList<>();

        Long userId = DBHelper.getUser(this.session).getId();
        Long deviceId = DBHelper.getDevice(this.device, this.session).getId();

        if (deviceId == null || userId == null)
            return ret;

        QueryBuilder<HuaweiDictData> qb =  this.session.getHuaweiDictDataDao().queryBuilder();
                qb.where(HuaweiDictDataDao.Properties.DeviceId.eq(deviceId))
                .where(HuaweiDictDataDao.Properties.UserId.eq(userId))
                .where(HuaweiDictDataDao.Properties.DictClass.eq(HuaweiDictTypes.SKIN_TEMPERATURE_CLASS))
                        .where(HuaweiDictDataDao.Properties.StartTimestamp.between(timestampFrom, timestampTo));
        final List<HuaweiDictData> dictData = qb.build().list();

        if (dictData.isEmpty())
            return ret;

        List<Long> ids = dictData.stream().map(HuaweiDictData::getDictId).collect(Collectors.toList());

        QueryBuilder<HuaweiDictDataValues> qbv =  this.session.getHuaweiDictDataValuesDao().queryBuilder();

        qbv.where(HuaweiDictDataValuesDao.Properties.DictType.eq(HuaweiDictTypes.SKIN_TEMPERATURE_VALUE)).where(HuaweiDictDataValuesDao.Properties.Tag.eq(10)).where(HuaweiDictDataValuesDao.Properties.DictId.in(ids));

        final List<HuaweiDictDataValues> valuesData = qbv.build().list();

        if (valuesData.isEmpty())
            return ret;

        for(HuaweiDictDataValues vl: valuesData) {
            double skinTemperature = conv2Double(vl.getValue());
            if(skinTemperature >= 20 && skinTemperature <= 42) {
                ret.add(new HuaweiTemperatureSample(vl.getHuaweiDictData().getStartTimestamp(), (float) skinTemperature));
            }
        }

        return ret;
    }

    @Override
    public void addSample(TemperatureSample timeSample) {
        throw new UnsupportedOperationException("read-only sample provider");

    }

    @Override
    public void addSamples(List<TemperatureSample> timeSamples) {
        throw new UnsupportedOperationException("read-only sample provider");

    }

    @Override
    public TemperatureSample createSample() {
        throw new UnsupportedOperationException("read-only sample provider");
    }

    @Nullable
    @Override
    public TemperatureSample getLatestSample() {
        Long userId = DBHelper.getUser(this.session).getId();
        Long deviceId = DBHelper.getDevice(this.device, this.session).getId();

        if (deviceId == null || userId == null)
            return null;

        QueryBuilder<HuaweiDictData> qb =  this.session.getHuaweiDictDataDao().queryBuilder();
        qb.where(HuaweiDictDataDao.Properties.DeviceId.eq(deviceId))
                .where(HuaweiDictDataDao.Properties.UserId.eq(userId))
                .where(HuaweiDictDataDao.Properties.DictClass.eq(HuaweiDictTypes.SKIN_TEMPERATURE_CLASS));
        qb.orderDesc(HuaweiDictDataDao.Properties.StartTimestamp).limit(1);

        final List<HuaweiDictData> data = qb.build().list();
        if (data.isEmpty())
            return null;


        QueryBuilder<HuaweiDictDataValues> qbv =  this.session.getHuaweiDictDataValuesDao().queryBuilder();
        qbv.where(HuaweiDictDataValuesDao.Properties.DictType.eq(HuaweiDictTypes.SKIN_TEMPERATURE_VALUE)).where(HuaweiDictDataValuesDao.Properties.Tag.eq(10)).where(HuaweiDictDataValuesDao.Properties.DictId.eq(data.get(0).getDictId()));
        final List<HuaweiDictDataValues> valuesData = qbv.build().list();

        if (valuesData.isEmpty())
            return null;

        return new HuaweiTemperatureSample(valuesData.get(0).getHuaweiDictData().getStartTimestamp(), (float) conv2Double(valuesData.get(0).getValue()));
    }

    @Nullable
    @Override
    public TemperatureSample getLatestSample(final long until) {
        Long userId = DBHelper.getUser(this.session).getId();
        Long deviceId = DBHelper.getDevice(this.device, this.session).getId();

        if (deviceId == null || userId == null)
            return null;

        QueryBuilder<HuaweiDictData> qb =  this.session.getHuaweiDictDataDao().queryBuilder();
        qb.where(HuaweiDictDataDao.Properties.StartTimestamp.le(until))
                .where(HuaweiDictDataDao.Properties.DeviceId.eq(deviceId))
                .where(HuaweiDictDataDao.Properties.UserId.eq(userId))
                .where(HuaweiDictDataDao.Properties.DictClass.eq(HuaweiDictTypes.SKIN_TEMPERATURE_CLASS));
        qb.orderDesc(HuaweiDictDataDao.Properties.StartTimestamp).limit(1);

        final List<HuaweiDictData> data = qb.build().list();
        if (data.isEmpty())
            return null;


        QueryBuilder<HuaweiDictDataValues> qbv =  this.session.getHuaweiDictDataValuesDao().queryBuilder();
        qbv.where(HuaweiDictDataValuesDao.Properties.DictType.eq(HuaweiDictTypes.SKIN_TEMPERATURE_VALUE)).where(HuaweiDictDataValuesDao.Properties.Tag.eq(10)).where(HuaweiDictDataValuesDao.Properties.DictId.eq(data.get(0).getDictId()));
        final List<HuaweiDictDataValues> valuesData = qbv.build().list();

        if (valuesData.isEmpty())
            return null;

        return new HuaweiTemperatureSample(valuesData.get(0).getHuaweiDictData().getStartTimestamp(), (float) conv2Double(valuesData.get(0).getValue()));
    }

    @Nullable
    @Override
    public TemperatureSample getFirstSample() {
        Long userId = DBHelper.getUser(this.session).getId();
        Long deviceId = DBHelper.getDevice(this.device, this.session).getId();

        if (deviceId == null || userId == null)
            return null;

        QueryBuilder<HuaweiDictData> qb =  this.session.getHuaweiDictDataDao().queryBuilder();
        qb.where(HuaweiDictDataDao.Properties.DeviceId.eq(deviceId))
                .where(HuaweiDictDataDao.Properties.UserId.eq(userId))
                .where(HuaweiDictDataDao.Properties.DictClass.eq(HuaweiDictTypes.SKIN_TEMPERATURE_CLASS));
        qb.orderAsc(HuaweiDictDataDao.Properties.StartTimestamp).limit(1);

        final List<HuaweiDictData> data = qb.build().list();
        if (data.isEmpty())
            return null;

        QueryBuilder<HuaweiDictDataValues> qbv =  this.session.getHuaweiDictDataValuesDao().queryBuilder();
        qbv.where(HuaweiDictDataValuesDao.Properties.DictType.eq(HuaweiDictTypes.SKIN_TEMPERATURE_VALUE)).where(HuaweiDictDataValuesDao.Properties.Tag.eq(10)).where(HuaweiDictDataValuesDao.Properties.DictId.eq(data.get(0).getDictId()));
        final List<HuaweiDictDataValues> valuesData = qbv.build().list();

        if (valuesData.isEmpty())
            return null;

        return new HuaweiTemperatureSample(valuesData.get(0).getHuaweiDictData().getStartTimestamp(), (float) conv2Double(valuesData.get(0).getValue()));
    }
}
