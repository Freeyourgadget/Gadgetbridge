/*  Copyright (C) 2024 Severin von Wnuck-Lipinski

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
package nodomain.freeyourgadget.gadgetbridge.devices.miscale;

import androidx.annotation.NonNull;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractTimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.MiScaleWeightSample;
import nodomain.freeyourgadget.gadgetbridge.entities.MiScaleWeightSampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class MiScaleSampleProvider extends AbstractTimeSampleProvider<MiScaleWeightSample> {
    public MiScaleSampleProvider(GBDevice device, DaoSession session) {
        super(device, session);
    }

    @Override
    @NonNull
    public AbstractDao<MiScaleWeightSample, ?> getSampleDao() {
        return getSession().getMiScaleWeightSampleDao();
    }

    @NonNull
    protected Property getTimestampSampleProperty() {
        return MiScaleWeightSampleDao.Properties.Timestamp;
    }

    @NonNull
    protected Property getDeviceIdentifierSampleProperty() {
        return MiScaleWeightSampleDao.Properties.DeviceId;
    }

    @Override
    public MiScaleWeightSample createSample() {
        return new MiScaleWeightSample();
    }
}
