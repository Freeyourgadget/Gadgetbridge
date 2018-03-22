/*  Copyright (C) 2015-2018 Andreas Shimokawa, Carsten Pfeiffer

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
package nodomain.freeyourgadget.gadgetbridge.externalevents;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.GregorianCalendar;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;


public class TimeChangeReceiver extends BroadcastReceiver {

    private static final Logger LOG = LoggerFactory.getLogger(TimeChangeReceiver.class);

    @Override
    public void onReceive(Context context, Intent intent) {
        Prefs prefs = GBApplication.getPrefs();
        final String action = intent.getAction();

        if (prefs.getBoolean("datetime_synconconnect", true) && (action.equals(Intent.ACTION_TIME_CHANGED) || action.equals(Intent.ACTION_TIMEZONE_CHANGED))) {
            Date newTime = GregorianCalendar.getInstance().getTime();
            LOG.info("Time or Timezone changed, syncing with device: " + DateTimeUtils.formatDate(newTime) + " (" + newTime.toGMTString() + "), " + intent.getAction());
            GBApplication.deviceService().onSetTime();
        }
    }
}