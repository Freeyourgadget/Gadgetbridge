/*  Copyright (C) 2023 Alicia Hormann

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
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package nodomain.freeyourgadget.gadgetbridge.devices.femometer;

import androidx.annotation.NonNull;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractTimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.FemometerVinca2TemperatureSample;
import nodomain.freeyourgadget.gadgetbridge.entities.FemometerVinca2TemperatureSampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class FemometerVinca2SampleProvider extends AbstractTimeSampleProvider<FemometerVinca2TemperatureSample> {

    public FemometerVinca2SampleProvider(GBDevice device, DaoSession session) {
        super(device, session);
    }

    @Override
    @NonNull
    public AbstractDao<FemometerVinca2TemperatureSample, ?> getSampleDao() {
        return getSession().getFemometerVinca2TemperatureSampleDao();
    }

    @NonNull
    protected Property getTimestampSampleProperty() {
        return FemometerVinca2TemperatureSampleDao.Properties.Timestamp;
    }

    @NonNull
    protected Property getDeviceIdentifierSampleProperty() {
        return FemometerVinca2TemperatureSampleDao.Properties.DeviceId;
    }

    @Override
    public FemometerVinca2TemperatureSample createSample() {
        return new FemometerVinca2TemperatureSample();
    }
}
