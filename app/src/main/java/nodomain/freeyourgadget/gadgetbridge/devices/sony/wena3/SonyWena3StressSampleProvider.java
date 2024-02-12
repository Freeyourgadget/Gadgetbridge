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

import java.util.List;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractTimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Wena3StressSample;
import nodomain.freeyourgadget.gadgetbridge.entities.Wena3StressSampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class SonyWena3StressSampleProvider extends AbstractTimeSampleProvider<Wena3StressSample> {
    public SonyWena3StressSampleProvider(final GBDevice device, final DaoSession session) {
        super(device, session);
    }

    @NonNull
    @Override
    public AbstractDao<Wena3StressSample, ?> getSampleDao() {
        return getSession().getWena3StressSampleDao();
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return Wena3StressSampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return Wena3StressSampleDao.Properties.DeviceId;
    }

    @NonNull
    @Override
    public List<Wena3StressSample> getAllSamples(long timestampFrom, long timestampTo) {
        List<Wena3StressSample> samples = super.getAllSamples(timestampFrom, timestampTo);
        for(Wena3StressSample sample: samples) {
            if(sample.getStress() < -100 || sample.getStress() > 100) {
                sample.setStress(-1);
            } else {
                // Move from the original -100 .. 100 range to 0 .. 100
                sample.setStress(Math.round(((float)sample.getStress() + 100.0f) / 2.0f));
            }
        }
        return samples;
    }

    @Override
    public Wena3StressSample createSample() {
        return new Wena3StressSample();
    }
}
