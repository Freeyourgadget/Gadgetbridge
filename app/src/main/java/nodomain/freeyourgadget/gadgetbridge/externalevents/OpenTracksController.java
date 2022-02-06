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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class OpenTracksController extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                // Handle received OpenTracks Dashboard API intent
            }
        }
    }

    public static void sendIntent(Context context, String className) {
        Prefs prefs = GBApplication.getPrefs();
        String packageName = prefs.getString("opentracks_packagename", "de.dennisguse.opentracks");
        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClassName(packageName, className);
        context.startActivity(intent);
    }

    public static void startRecording(Context context) {
        sendIntent(context, "de.dennisguse.opentracks.publicapi.StartRecording");
    }

    public static void stopRecording(Context context) {
        sendIntent(context, "de.dennisguse.opentracks.publicapi.StopRecording");
    }
}