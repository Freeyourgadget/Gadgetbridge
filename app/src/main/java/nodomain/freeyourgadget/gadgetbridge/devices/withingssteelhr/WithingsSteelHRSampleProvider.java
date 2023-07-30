/*  Copyright (C) 2021 Frank Ertl

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
package nodomain.freeyourgadget.gadgetbridge.devices.withingssteelhr;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import de.greenrobot.dao.query.Query;
import de.greenrobot.dao.query.QueryBuilder;
import de.greenrobot.dao.query.WhereCondition;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.WithingsSteelHRActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.WithingsSteelHRActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.conversation.ActivitySampleHandler;

public class WithingsSteelHRSampleProvider extends AbstractSampleProvider<WithingsSteelHRActivitySample> {
    private static final Logger logger = LoggerFactory.getLogger(WithingsSteelHRSampleProvider.class);

    public WithingsSteelHRSampleProvider(GBDevice device, DaoSession session) {
        super(device, session);
    }

    @Override
    public AbstractDao<WithingsSteelHRActivitySample, ?> getSampleDao() {
        return getSession().getWithingsSteelHRActivitySampleDao();
    }

    @Nullable
    @Override
    protected Property getRawKindSampleProperty() {
        return WithingsSteelHRActivitySampleDao.Properties.RawKind;
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return WithingsSteelHRActivitySampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return WithingsSteelHRActivitySampleDao.Properties.DeviceId;
    }

    @Override
    public List<WithingsSteelHRActivitySample> getActivitySamples(int timestamp_from, int timestamp_to) {
        return super.getGBActivitySamples(timestamp_from, timestamp_to, ActivityKind.TYPE_ALL);
    }

    @Override
    public int normalizeType(int rawType) {
        return rawType;
    }

    @Override
    public int toRawActivityKind(int activityKind) {
        switch (activityKind) {
            case ActivityKind.TYPE_UNKNOWN:
                return 0;
            case ActivityKind.TYPE_LIGHT_SLEEP:
                return 1;
            case ActivityKind.TYPE_DEEP_SLEEP:
                return 2;
            default:
                return activityKind;
        }
    }

    @Override
    public float normalizeIntensity(int rawIntensity) {
        if (rawIntensity > 0) {
            return (float) (Math.log(rawIntensity) / 8);
        }

        return 0;
    }

    @Override
    public WithingsSteelHRActivitySample createActivitySample() {
        return new WithingsSteelHRActivitySample();
    }
}
