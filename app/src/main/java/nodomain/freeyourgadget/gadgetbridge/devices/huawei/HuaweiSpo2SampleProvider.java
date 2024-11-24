/*  Copyright (C) 2024 Damien Gaignon, Martin.JM

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.devices.huawei;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractTimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractSpo2Sample;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.HuaweiActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class HuaweiSpo2SampleProvider extends AbstractTimeSampleProvider<HuaweiSpo2SampleProvider.HuaweiSpo2Sample> {
    private static final Logger LOG = LoggerFactory.getLogger(HuaweiSpo2SampleProvider.class);

    private final HuaweiSampleProvider huaweiSampleProvider;

    public HuaweiSpo2SampleProvider(GBDevice device, DaoSession session) {
        super(device, session);
        this.huaweiSampleProvider = new HuaweiSampleProvider(this.getDevice(), this.getSession());
    }

    /**
     * Converts an Huawei activity sample to an SpO2 sample
     * @param sample Activity sample to convert
     * @return SpO sample containing the SpO value, timestamp, userID, and deviceID of the activity sample
     */
    @NonNull
    private HuaweiSpo2Sample activityToSpo2Sample(HuaweiActivitySample sample) {
        return new HuaweiSpo2Sample(
                -1, // No difference between auto and manual for Huawei
                sample.getTimestamp() * 1000L,
                sample.getUserId(),
                sample.getDeviceId(),
                sample.getSpo()
        );
    }

    @NonNull
    @Override
    public List<HuaweiSpo2Sample> getAllSamples(long timestampFrom, long timestampTo) {
        // Using high res data is fine for the SpO2 sample provider at the time of writing
        List<HuaweiActivitySample> activitySamples = huaweiSampleProvider.getAllActivitySamplesHighRes((int) (timestampFrom / 1000L), (int) (timestampTo / 1000L));
        List<HuaweiSpo2Sample> spo2Samples = new ArrayList<>(activitySamples.size());
        for (HuaweiActivitySample sample : activitySamples) {
            if (sample.getSpo() == -1)
                continue;
            spo2Samples.add(activityToSpo2Sample(sample));
        }
        return spo2Samples;
    }

    @Override
    public void addSample(HuaweiSpo2Sample activitySample) {
        LOG.error("Huawei Spo2 sample provider addSample called!");
    }

    @Override
    public void addSamples(List<HuaweiSpo2Sample> activitySamples) {
        LOG.error("Huawei Spo2 sample provider addSamples called!");
    }

    @Nullable
    @Override
    public HuaweiSpo2Sample getLatestSample() {
        QueryBuilder<HuaweiActivitySample> qb = this.huaweiSampleProvider.getSampleDao().queryBuilder();
        final Device dbDevice = DBHelper.findDevice(getDevice(), getSession());
        if (dbDevice == null)
            return null;
        final Property deviceProperty = this.huaweiSampleProvider.getDeviceIdentifierSampleProperty();
        qb
                .where(deviceProperty.eq(dbDevice.getId()))
                .where(HuaweiActivitySampleDao.Properties.Spo.notEq(-1))
                .orderDesc(this.huaweiSampleProvider.getTimestampSampleProperty())
                .limit(1);
        final List<HuaweiActivitySample> samples = qb.build().list();
        if (samples.isEmpty())
            return null;
        return activityToSpo2Sample(samples.get(0));
    }

    @Nullable
    @Override
    public HuaweiSpo2Sample getFirstSample() {
        QueryBuilder<HuaweiActivitySample> qb = this.huaweiSampleProvider.getSampleDao().queryBuilder();
        final Device dbDevice = DBHelper.findDevice(getDevice(), getSession());
        if (dbDevice == null)
            return null;
        final Property deviceProperty = this.huaweiSampleProvider.getDeviceIdentifierSampleProperty();
        qb
                .where(deviceProperty.eq(dbDevice.getId()))
                .where(HuaweiActivitySampleDao.Properties.Spo.notEq(-1))
                .orderAsc(this.huaweiSampleProvider.getTimestampSampleProperty())
                .limit(1);
        final List<HuaweiActivitySample> samples = qb.build().list();
        if (samples.isEmpty())
            return null;
        return activityToSpo2Sample(samples.get(0));
    }

    @Override
    protected void detachFromSession() {
        // Not necessary to do anything here
        LOG.warn("Huawei Spo2 sample provider detachFromSession called!");
    }

    @NonNull
    @Override
    public AbstractDao<HuaweiSpo2Sample, ?> getSampleDao() {
        // This not existing is not an issue (at the time of writing), as this is only used in
        // methods that are overwritten by this class itself.
        LOG.error("Huawei Spo2 sample provider getSampleDao called!");
        return null;
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        LOG.warn("Huawei Spo2 sample provider getTimestampSampleProperty called!");
        return this.huaweiSampleProvider.getTimestampSampleProperty();
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        LOG.warn("Huawei Spo2 sample provider getDeviceIdentifierSampleProperty called!");
        return this.huaweiSampleProvider.getDeviceIdentifierSampleProperty();
    }

    @Override
    public HuaweiSpo2Sample createSample() {
        return new HuaweiSpo2Sample();
    }

    public static class HuaweiSpo2Sample extends AbstractSpo2Sample {
        private int typeNum;
        private long timestamp;
        private long userId;
        private long deviceId;
        private int spo2;

        public HuaweiSpo2Sample() { }

        public HuaweiSpo2Sample(int typeNum, long timestamp, long userId, long deviceId, int spo2) {
            this.typeNum = typeNum;
            this.timestamp = timestamp;
            this.userId = userId;
            this.deviceId = deviceId;
            this.spo2 = spo2;
        }

        @Override
        public int getTypeNum() {
            return typeNum;
        }

        @Override
        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        @Override
        public long getUserId() {
            return userId;
        }

        @Override
        public void setUserId(long userId) {
            this.userId = userId;
        }

        @Override
        public long getDeviceId() {
            return deviceId;
        }

        @Override
        public void setDeviceId(long deviceId) {
            this.deviceId = deviceId;
        }

        @Override
        public void setDevice(final Device device) {

        }

        @Override
        public void setUser(final User user) {

        }

        @Override
        public int getSpo2() {
            return spo2;
        }

        @Override
        public long getTimestamp() {
            return timestamp;
        }
    }
}
