/*  Copyright (C) 2015-2017 Andreas Shimokawa, Carsten Pfeiffer

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
package nodomain.freeyourgadget.gadgetbridge.devices.pebble;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.PebbleMisfitSample;
import nodomain.freeyourgadget.gadgetbridge.entities.PebbleMisfitSampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class PebbleMisfitSampleProvider extends AbstractSampleProvider<PebbleMisfitSample> {

    protected final float movementDivisor = 300f;

    public PebbleMisfitSampleProvider(GBDevice device, DaoSession session) {
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
        return rawIntensity / movementDivisor;
    }

    @Override
    public PebbleMisfitSample createActivitySample() {
        return new PebbleMisfitSample();
    }

    @Override
    public int getID() {
        return SampleProvider.PROVIDER_PEBBLE_MISFIT;
    }

    @Override
    public AbstractDao<PebbleMisfitSample, ?> getSampleDao() {
        return getSession().getPebbleMisfitSampleDao();
    }

    @Override
    protected Property getRawKindSampleProperty() {
        return null;
    }

    protected Property getTimestampSampleProperty() {
        return PebbleMisfitSampleDao.Properties.Timestamp;
    }

    protected Property getDeviceIdentifierSampleProperty() {
        return PebbleMisfitSampleDao.Properties.DeviceId;
    }
}
