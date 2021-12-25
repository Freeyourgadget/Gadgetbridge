/*  Copyright (C) 2018-2021 Andreas Böhler, Cre3per, Daniele Gobbetti,
    Sebastian Kranz

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
package nodomain.freeyourgadget.gadgetbridge.devices.casio;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.CasioGBX100ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.CasioGBX100ActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class CasioGBX100SampleProvider extends AbstractSampleProvider<CasioGBX100ActivitySample> {
    private static final Logger LOG = LoggerFactory.getLogger(CasioGBX100SampleProvider.class);

    public CasioGBX100SampleProvider(GBDevice device, DaoSession session) {
        super(device, session);
    }

    @Override
    public int normalizeType(int rawType) {
        return rawType;
    }

    @Override
    public int toRawActivityKind(int activityKind) {
        return activityKind;
    }

    @Override
    public float normalizeIntensity(int rawIntensity) {
        // The magic number 1500 is based on
        // https://www.livestrong.com/article/474836-what-sport-burns-the-most-calories-per-hour/
        return (rawIntensity / 1500f);
    }

    @Override
    public CasioGBX100ActivitySample createActivitySample() {
        return new CasioGBX100ActivitySample();
    }

    @Override
    public AbstractDao<CasioGBX100ActivitySample, ?> getSampleDao() {
        return getSession().getCasioGBX100ActivitySampleDao();
    }

    @Nullable
    @Override
    protected Property getRawKindSampleProperty() {
        return CasioGBX100ActivitySampleDao.Properties.RawKind;
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return CasioGBX100ActivitySampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return CasioGBX100ActivitySampleDao.Properties.DeviceId;
    }


    @Override
    public List<CasioGBX100ActivitySample> getActivitySamples(int timestamp_from, int timestamp_to) {
        return super.getActivitySamples(timestamp_from, timestamp_to);
    }

    @Override
    public List<CasioGBX100ActivitySample> getAllActivitySamples(int timestamp_from, int timestamp_to) {
        return super.getActivitySamples(timestamp_from, timestamp_to);
    }
}
