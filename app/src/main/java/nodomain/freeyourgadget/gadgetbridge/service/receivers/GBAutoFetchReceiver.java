/*  Copyright (C) 2018-2024 Daniele Gobbetti, José Rebelo, Martin

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
package nodomain.freeyourgadget.gadgetbridge.service.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes;


public class GBAutoFetchReceiver extends BroadcastReceiver {
    private static final Logger LOG = LoggerFactory.getLogger(GBAutoFetchReceiver.class);

    private Date lastSync = new Date();

    @Override
    public void onReceive(final Context context, final Intent intent) {
        synchronized (this) {
            final Date now = new Date();
            final long timeSinceLast = now.getTime() - lastSync.getTime();
            if (timeSinceLast < 2500L) {
                // #4165 - prevent multiple syncs in very quick succession
                LOG.warn("Throttling auto fetch by {}, last one was {}ms ago", intent.getAction(), timeSinceLast);
                return;
            }
            final Date nextSync = DateUtils.addMinutes(lastSync, GBApplication.getPrefs().getInt("auto_fetch_interval_limit", 0));
            if (nextSync.before(now)) {
                LOG.info("Trigger auto fetch by {}", intent.getAction());
                GBApplication.deviceService().onFetchRecordedData(RecordedDataTypes.TYPE_SYNC);
                lastSync = now;
            }
        }
    }
}
