/*  Copyright (C) 2023 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.operations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiSupport;

/**
 * An operation that fetches statistics from /storage/statistics/ (hm_statis_data* files). We do not
 * know what these are or how to parse them, but syncing them helps free up memory.
 */
public class FetchStatisticsOperation extends AbstractRepeatingFetchOperation {
    private static final Logger LOG = LoggerFactory.getLogger(FetchStatisticsOperation.class);

    public FetchStatisticsOperation(final HuamiSupport support) {
        super(support, HuamiService.COMMAND_ACTIVITY_DATA_TYPE_STATISTICS, "statistics data");
    }

    @Override
    protected String taskDescription() {
        return getContext().getString(R.string.busy_task_fetch_statistics);
    }

    @Override
    protected boolean handleActivityData(final GregorianCalendar timestamp, final byte[] bytes) {
        LOG.debug("Ignoring {} bytes of statistics", bytes.length);

        timestamp.setTime(new Date());

        return true;
    }

    @Override
    protected GregorianCalendar getLastSuccessfulSyncTime() {
        // One minute ago - we don't really care about this data, and we don't want to fetch it all
        final GregorianCalendar sinceWhen = BLETypeConversions.createCalendar();
        sinceWhen.add(Calendar.MINUTE, -1);
        return sinceWhen;
    }

    @Override
    protected String getLastSyncTimeKey() {
        return "lastStatisticsTimeMillis";
    }
}
