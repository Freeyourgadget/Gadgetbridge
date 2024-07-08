/*  Copyright (C) 2024 Arjan Schrijver

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
package nodomain.freeyourgadget.gadgetbridge.devices.colmi.samples;

import androidx.annotation.NonNull;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractTimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.ColmiSleepStageSample;
import nodomain.freeyourgadget.gadgetbridge.entities.ColmiSleepStageSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class ColmiSleepStageSampleProvider extends AbstractTimeSampleProvider<ColmiSleepStageSample> {
    public ColmiSleepStageSampleProvider(final GBDevice device, final DaoSession session) {
        super(device, session);
    }

    @NonNull
    @Override
    public AbstractDao<ColmiSleepStageSample, ?> getSampleDao() {
        return getSession().getColmiSleepStageSampleDao();
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return ColmiSleepStageSampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return ColmiSleepStageSampleDao.Properties.DeviceId;
    }

    @Override
    public ColmiSleepStageSample createSample() {
        return new ColmiSleepStageSample();
    }
}
