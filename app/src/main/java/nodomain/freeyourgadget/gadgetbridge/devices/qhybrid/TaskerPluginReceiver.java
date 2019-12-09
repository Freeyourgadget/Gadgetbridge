/*  Copyright (C) 2019 Daniel Dakhno

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
package nodomain.freeyourgadget.gadgetbridge.devices.qhybrid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.PlayNotificationRequest;

public class TaskerPluginReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String min = intent.getStringExtra(TaskerPluginActivity.key_minute);
        String hour = intent.getStringExtra(TaskerPluginActivity.key_hours);
        String vibration = intent.getStringExtra(TaskerPluginActivity.key_vibration);

        int minDegrees = (int)Float.parseFloat(min);
        int hourDegrees = (int)Float.parseFloat(hour);

        NotificationConfiguration config = new NotificationConfiguration(
                (short)minDegrees,
                (short)hourDegrees,
                null,
                null,
                false,
                PlayNotificationRequest.VibrationType.fromValue(Byte.parseByte(vibration))
        );

        Intent send = new Intent(QHybridSupport.QHYBRID_COMMAND_NOTIFICATION);
        send.putExtra("CONFIG", config);
        LocalBroadcastManager.getInstance(context).sendBroadcast(send);
    }
}
