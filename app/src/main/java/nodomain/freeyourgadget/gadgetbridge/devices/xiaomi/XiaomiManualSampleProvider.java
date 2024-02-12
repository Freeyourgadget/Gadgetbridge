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
package nodomain.freeyourgadget.gadgetbridge.devices.xiaomi;

import androidx.annotation.NonNull;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractTimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.XiaomiManualSample;
import nodomain.freeyourgadget.gadgetbridge.entities.XiaomiSleepStageSampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class XiaomiManualSampleProvider extends AbstractTimeSampleProvider<XiaomiManualSample> {
    public static final int TYPE_HR = 0x11;
    public static final int TYPE_SPO2 = 0x12;
    public static final int TYPE_STRESS = 0x13;
    public static final int TYPE_TEMPERATURE = 0x44;

    public XiaomiManualSampleProvider(final GBDevice device, final DaoSession session) {
        super(device, session);
    }

    @NonNull
    @Override
    public AbstractDao<XiaomiManualSample, ?> getSampleDao() {
        return getSession().getXiaomiManualSampleDao();
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return XiaomiSleepStageSampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return XiaomiSleepStageSampleDao.Properties.DeviceId;
    }

    @Override
    public XiaomiManualSample createSample() {
        return new XiaomiManualSample();
    }
}
