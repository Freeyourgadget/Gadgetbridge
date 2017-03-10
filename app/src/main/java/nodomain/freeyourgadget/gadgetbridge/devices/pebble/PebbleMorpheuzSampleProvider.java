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
import nodomain.freeyourgadget.gadgetbridge.entities.PebbleMorpheuzSample;
import nodomain.freeyourgadget.gadgetbridge.entities.PebbleMorpheuzSampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class PebbleMorpheuzSampleProvider extends AbstractSampleProvider<PebbleMorpheuzSample> {

    protected float movementDivisor = 5000f;

    public PebbleMorpheuzSampleProvider(GBDevice device, DaoSession session) {
        super(device, session);
    }

    @Override
    public AbstractDao<PebbleMorpheuzSample, ?> getSampleDao() {
        return getSession().getPebbleMorpheuzSampleDao();
    }

    @Override
    protected Property getTimestampSampleProperty() {
        return PebbleMorpheuzSampleDao.Properties.Timestamp;
    }

    @Override
    protected Property getRawKindSampleProperty() {
        return null; // not supported
    }

    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return PebbleMorpheuzSampleDao.Properties.DeviceId;
    }

    @Override
    public PebbleMorpheuzSample createActivitySample() {
        return new PebbleMorpheuzSample();
    }


    @Override
    public float normalizeIntensity(int rawIntensity) {
        return rawIntensity / movementDivisor;
    }

    @Override
    public int getID() {
        return SampleProvider.PROVIDER_PEBBLE_MORPHEUZ;
    }

    @Override
    public int normalizeType(int rawType) {
        return rawType;
    }

    @Override
    public int toRawActivityKind(int activityKind) {
        return activityKind;
    }
}
