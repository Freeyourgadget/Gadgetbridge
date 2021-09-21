/*  Copyright (C) 2016-2020 Petr Vaněk

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

package nodomain.freeyourgadget.gadgetbridge.devices.fitpro;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.FitProActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.FitProActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;

public class FitProSampleProvider extends AbstractSampleProvider<FitProActivitySample> {
    public FitProSampleProvider(GBDevice device, DaoSession session) {
        super(device, session);
    }

    @Override
    public AbstractDao<FitProActivitySample, ?> getSampleDao() {
        return getSession().getFitProActivitySampleDao();
    }


    // as per FitProDeviceSupport.rawActivityKindToUniqueKind

    @Override
    public int normalizeType(int rawType) {
        switch (rawType) {
            case 1:
                return ActivityKind.TYPE_ACTIVITY;
            case 11:
                return ActivityKind.TYPE_DEEP_SLEEP;
            case 12:
                return ActivityKind.TYPE_LIGHT_SLEEP;
            default:
                return ActivityKind.TYPE_UNKNOWN;
        }
    }

    @Override
    public int toRawActivityKind(int activityKind) {
        switch (activityKind) {
            case ActivityKind.TYPE_ACTIVITY:
                return 1;
            case ActivityKind.TYPE_DEEP_SLEEP:
                return 11;
            case ActivityKind.TYPE_LIGHT_SLEEP:
                return 12;
            default:
                return 1;
        }
    }

    @Override
    public float normalizeIntensity(int rawIntensity) {
        return rawIntensity / 2000f; //samples are per 5 minutes, so this should be sufficient
    }

    @Override
    public FitProActivitySample createActivitySample() {
        return new FitProActivitySample();
    }

    @Nullable
    @Override
    protected Property getRawKindSampleProperty() {
        return FitProActivitySampleDao.Properties.RawKind;
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return FitProActivitySampleDao.Properties.Timestamp;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return FitProActivitySampleDao.Properties.DeviceId;
    }
}