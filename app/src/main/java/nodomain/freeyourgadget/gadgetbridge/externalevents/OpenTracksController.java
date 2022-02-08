/*  Copyright (C) 2022 Arjan Schrijver

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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class OpenTracksController extends Activity {
    private static final String EXTRAS_PROTOCOL_VERSION = "PROTOCOL_VERSION";
    private static final String ACTION_DASHBOARD = "Intent.OpenTracks-Dashboard";
    private static final String ACTION_DASHBOARD_PAYLOAD = ACTION_DASHBOARD + ".Payload";

    private final Logger LOG = LoggerFactory.getLogger(OpenTracksController.class);

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        GBApplication gbApp = GBApplication.app();
        Intent intent = getIntent();
        int protocolVersion = intent.getIntExtra(EXTRAS_PROTOCOL_VERSION, 1);
        final ArrayList<Uri> uris = intent.getParcelableArrayListExtra(ACTION_DASHBOARD_PAYLOAD);
        if (uris != null) {
            if (gbApp.getOpenTracksObserver() != null) {
                LOG.info("Unregistering old OpenTracksContentObserver");
                gbApp.getOpenTracksObserver().unregister();
            }
            Uri tracksUri = uris.get(0);
            LOG.info("Registering OpenTracksContentObserver with tracks URI: " + tracksUri);
            gbApp.setOpenTracksObserver(new OpenTracksContentObserver(this, tracksUri, protocolVersion));
            try {
                getContentResolver().registerContentObserver(tracksUri, false, gbApp.getOpenTracksObserver());
            } catch (final SecurityException se) {
                LOG.error("Error registering OpenTracksContentObserver", se);
            }
        }
        moveTaskToBack(true);
    }

    public static void sendIntent(Context context, String className) {
        Prefs prefs = GBApplication.getPrefs();
        String packageName = prefs.getString("opentracks_packagename", "de.dennisguse.opentracks");
        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClassName(packageName, className);
        intent.putExtra("STATS_TARGET_PACKAGE", context.getPackageName());
        intent.putExtra("STATS_TARGET_CLASS", "nodomain.freeyourgadget.gadgetbridge.externalevents.OpenTracksController");
        context.startActivity(intent);
    }

    public static void startRecording(Context context) {
        sendIntent(context, "de.dennisguse.opentracks.publicapi.StartRecording");
    }

    public static void stopRecording(Context context) {
        sendIntent(context, "de.dennisguse.opentracks.publicapi.StopRecording");
        GBApplication.app().getOpenTracksObserver().finish();
    }
}