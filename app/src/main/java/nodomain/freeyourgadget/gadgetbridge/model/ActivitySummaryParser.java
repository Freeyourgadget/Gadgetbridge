/*  Copyright (C) 2020-2024 Andreas Shimokawa

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
package nodomain.freeyourgadget.gadgetbridge.model;

import java.util.Date;
import java.util.List;

import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummaryDao;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public interface ActivitySummaryParser {
    /**
     * Re-parse an existing {@link BaseActivitySummary}, updating it from the existing binary data.
     *
     * @param summary    the existing {@link BaseActivitySummary}. It's not guaranteed that it
     *                   contains any raw binary data.
     * @param forDetails whether the parsing is for the details page. If this is false, the parser
     *                   should avoid slow operations such as reading and parsing raw files from
     *                   storage.
     * @return the update {@link BaseActivitySummary}
     */
    BaseActivitySummary parseBinaryData(BaseActivitySummary summary, final boolean forDetails);

    static BaseActivitySummary findOrCreateBaseActivitySummary(final DaoSession session,
                                                               final GBDevice gbDevice,
                                                               final int timestampSeconds) {
        final Device device = DBHelper.getDevice(gbDevice, session);
        return findOrCreateBaseActivitySummary(session, device.getId(), timestampSeconds);
    }

    static BaseActivitySummary findOrCreateBaseActivitySummary(final DaoSession session,
                                                               final long deviceId,
                                                               final int timestampSeconds) {
        final User user = DBHelper.getUser(session);
        final BaseActivitySummaryDao summaryDao = session.getBaseActivitySummaryDao();
        final QueryBuilder<BaseActivitySummary> qb = summaryDao.queryBuilder();
        qb.where(BaseActivitySummaryDao.Properties.StartTime.eq(new Date(timestampSeconds * 1000L)));
        qb.where(BaseActivitySummaryDao.Properties.DeviceId.eq(deviceId));
        qb.where(BaseActivitySummaryDao.Properties.UserId.eq(user.getId()));
        final List<BaseActivitySummary> summaries = qb.build().list();
        if (summaries.isEmpty()) {
            final BaseActivitySummary summary = new BaseActivitySummary();
            summary.setStartTime(new Date(timestampSeconds * 1000L));
            summary.setDeviceId(deviceId);
            summary.setUser(user);

            // These will be set later, once we parse the summary
            summary.setEndTime(new Date(timestampSeconds * 1000L));
            summary.setActivityKind(ActivityKind.UNKNOWN.getCode());

            return summary;
        }

        return summaries.get(0);
    }
}
