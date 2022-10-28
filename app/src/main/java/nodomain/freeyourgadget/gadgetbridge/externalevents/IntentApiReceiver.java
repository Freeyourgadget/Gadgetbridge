/*  Copyright (C) 2022 José Rebelo

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
import android.content.IntentFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.PeriodicExporter;
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class IntentApiReceiver extends BroadcastReceiver {
    private static final Logger LOG = LoggerFactory.getLogger(IntentApiReceiver.class);

    public static final String COMMAND_ACTIVITY_SYNC = "nodomain.freeyourgadget.gadgetbridge.command.ACTIVITY_SYNC";
    public static final String COMMAND_TRIGGER_EXPORT = "nodomain.freeyourgadget.gadgetbridge.command.TRIGGER_EXPORT";

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (intent.getAction() == null) {
            LOG.warn("Action is null");
            return;
        }

        final Prefs prefs = GBApplication.getPrefs();

        switch (intent.getAction()) {
            case COMMAND_ACTIVITY_SYNC:
                if (!prefs.getBoolean("intent_api_allow_activity_sync", false)) {
                    LOG.warn("Intent API activity sync trigger not allowed");
                    return;
                }

                LOG.info("Triggering activity sync");

                GBApplication.deviceService().onFetchRecordedData(RecordedDataTypes.TYPE_ACTIVITY);
                break;
            case COMMAND_TRIGGER_EXPORT:
                if (!prefs.getBoolean("intent_api_allow_trigger_export", false)) {
                    LOG.warn("Intent API export trigger not allowed");
                    return;
                }

                LOG.info("Triggering export");

                final Intent exportIntent = new Intent(context, PeriodicExporter.class);
                context.sendBroadcast(exportIntent);
                break;
        }
    }

    public IntentFilter buildFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(COMMAND_ACTIVITY_SYNC);
        intentFilter.addAction(COMMAND_TRIGGER_EXPORT);
        return intentFilter;
    }
}
