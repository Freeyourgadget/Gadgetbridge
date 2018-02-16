/*  Copyright (C) 2015-2017 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti

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
package nodomain.freeyourgadget.gadgetbridge.devices.miband;

import java.util.List;

import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.devices.miband2.MiBand2Const;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.MiBandActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.MiBandActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import static nodomain.freeyourgadget.gadgetbridge.devices.miband2.MiBand2Const.*;

public class MiBand2SampleProvider extends AbstractMiBandSampleProvider {

    public MiBand2SampleProvider(GBDevice device, DaoSession session) {
        super(device, session);
    }

    @Override
    protected List<MiBandActivitySample> getGBActivitySamples(int timestamp_from, int timestamp_to, int activityType) {
        List<MiBandActivitySample> samples = super.getGBActivitySamples(timestamp_from, timestamp_to, activityType);
        postprocess(samples);
        return samples;
    }

    /**
     * "Temporary" runtime post processing of activity kinds.
     * @param samples
     */
    private void postprocess(List<MiBandActivitySample> samples) {
        if (samples.isEmpty()) {
            return;
        }

        int lastValidKind = determinePreviousValidActivityType(samples.get(0));
        for (MiBandActivitySample sample : samples) {
            int rawKind = sample.getRawKind();
            if (rawKind != TYPE_UNSET) {
                rawKind &= 0xf;
                sample.setRawKind(rawKind);
            }

            switch (rawKind) {
                case TYPE_IGNORE:
                case TYPE_NO_CHANGE:
                    if (lastValidKind != TYPE_UNSET) {
                        sample.setRawKind(lastValidKind);
                    }
                    break;
                default:
                    lastValidKind = rawKind;
                    break;
            }
        }
    }

    private int determinePreviousValidActivityType(MiBandActivitySample sample) {
        QueryBuilder<MiBandActivitySample> qb = getSampleDao().queryBuilder();
        qb.where(MiBandActivitySampleDao.Properties.DeviceId.eq(sample.getDeviceId()),
                MiBandActivitySampleDao.Properties.UserId.eq(sample.getUserId()),
                MiBandActivitySampleDao.Properties.Timestamp.lt(sample.getTimestamp()),
                MiBandActivitySampleDao.Properties.RawKind.notIn(TYPE_NO_CHANGE, TYPE_IGNORE, TYPE_UNSET, 16, 80, 96, 112)); // all I ever had that are 0 when doing &=0xf
        qb.orderDesc(MiBandActivitySampleDao.Properties.Timestamp);
        qb.limit(1);
        List<MiBandActivitySample> result = qb.build().list();
        if (result.size() > 0) {
            return result.get(0).getRawKind() & 0xf;
        }
        return TYPE_UNSET;
    }

    @Override
    public int normalizeType(int rawType) {
        return MiBand2Const.toActivityKind(rawType);
    }

    @Override
    public int toRawActivityKind(int activityKind) {
        return MiBand2Const.toRawActivityType(activityKind);
    }
}
