/*  Copyright (C) 2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.cmfwatchpro.samples;

import androidx.annotation.NonNull;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractTimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.CmfSpo2Sample;
import nodomain.freeyourgadget.gadgetbridge.entities.CmfSpo2SampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class CmfSpo2SampleProvider extends AbstractTimeSampleProvider<CmfSpo2Sample> {
    public CmfSpo2SampleProvider(final GBDevice device, final DaoSession session) {
        super(device, session);
    }

    @NonNull
    @Override
    public AbstractDao<CmfSpo2Sample, ?> getSampleDao() {
        return getSession().getCmfSpo2SampleDao();
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return CmfSpo2SampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return CmfSpo2SampleDao.Properties.DeviceId;
    }

    @Override
    public CmfSpo2Sample createSample() {
        return new CmfSpo2Sample();
    }
}
