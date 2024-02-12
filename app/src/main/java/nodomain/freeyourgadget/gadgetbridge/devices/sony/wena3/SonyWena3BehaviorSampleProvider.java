/*  Copyright (C) 2023-2024 akasaka / Genjitsu Labs

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
package nodomain.freeyourgadget.gadgetbridge.devices.sony.wena3;

import androidx.annotation.NonNull;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractTimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Wena3BehaviorSample;
import nodomain.freeyourgadget.gadgetbridge.entities.Wena3BehaviorSampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class SonyWena3BehaviorSampleProvider extends AbstractTimeSampleProvider<Wena3BehaviorSample> {
    public SonyWena3BehaviorSampleProvider(final GBDevice device, final DaoSession session) {
        super(device, session);
    }

    @NonNull
    @Override
    public AbstractDao<Wena3BehaviorSample, ?> getSampleDao() {
        return getSession().getWena3BehaviorSampleDao();
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return Wena3BehaviorSampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return Wena3BehaviorSampleDao.Properties.DeviceId;
    }

    @Override
    public Wena3BehaviorSample createSample() {
        return new Wena3BehaviorSample();
    }
}
