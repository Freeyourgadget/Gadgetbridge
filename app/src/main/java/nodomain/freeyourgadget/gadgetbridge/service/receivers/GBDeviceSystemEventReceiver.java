/*  Copyright (C) 2015-2021 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, Gabe Schrecker, Petr Vaněk

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
package nodomain.freeyourgadget.gadgetbridge.service.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventDeviceSystemEvent;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class GBDeviceSystemEventReceiver extends BroadcastReceiver {
    private static final Logger LOG = LoggerFactory.getLogger(GBDeviceSystemEventReceiver.class);
    public static final String ACTION_SYSTEM_EVENT_CONTROL = "nodomain.freeyourgadget.gadgetbridge.systemeventcontrol";

    @Override
    public void onReceive(Context context, Intent intent) {
        GBDeviceEventDeviceSystemEvent.Event systemEvent = GBDeviceEventDeviceSystemEvent.Event.values()[intent.getIntExtra("event", 0)];
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        switch (systemEvent) {
            case CONNECTED:
                GB.createNotification("oh mine... connected!", context);
                LOG.debug("petr connected");
                try {
                    audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                } catch (SecurityException e) {
                    LOG.error("SecurityException when trying to set ringer (no permission granted :/ ?), not setting it then.");
                }
                break;
            case DISCONNECTED:
                LOG.debug("petr disconnected");
                GB.createNotification("oh mine... disconnected!", context);
                try {
                    audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                } catch (SecurityException e) {
                    LOG.error("SecurityException when trying to set ringer (no permission granted :/ ?), not setting it then.");
                }
                break;
            case BATTERY_FULL:
                GB.createNotification("battery full!", context);
                break;
            default:
                return;
        }


    }


}
