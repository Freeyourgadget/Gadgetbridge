/*  Copyright (C) 2015-2017 0nse, Andreas Shimokawa, Carsten Pfeiffer,
    Normano64

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

import android.app.NotificationManager;
import android.app.NotificationManager.Policy;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.telephony.SmsMessage;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class SMSReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Prefs prefs = GBApplication.getPrefs();
        if ("never".equals(prefs.getString("notification_mode_sms", "when_screen_off"))) {
            return;
        }
        if ("when_screen_off".equals(prefs.getString("notification_mode_sms", "when_screen_off"))) {
            PowerManager powermanager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (powermanager.isScreenOn()) {
                return;
            }
        }

        NotificationSpec notificationSpec = new NotificationSpec();
        notificationSpec.id = -1;
        notificationSpec.type = NotificationType.GENERIC_SMS;

        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            Object[] pdus = (Object[]) bundle.get("pdus");
            if (pdus != null) {
                for (Object pdu1 : pdus) {
                    byte[] pdu = (byte[]) pdu1;
                    SmsMessage message = SmsMessage.createFromPdu(pdu);
                    notificationSpec.body = message.getDisplayMessageBody();
                    notificationSpec.phoneNumber = message.getOriginatingAddress();
                    if (notificationSpec.phoneNumber != null) {
                        switch (GBApplication.getGrantedInterruptionFilter()) {
                            case NotificationManager.INTERRUPTION_FILTER_ALL:
                                break;
                            case NotificationManager.INTERRUPTION_FILTER_ALARMS:
                            case NotificationManager.INTERRUPTION_FILTER_NONE:
                                return;
                            case NotificationManager.INTERRUPTION_FILTER_PRIORITY:
                                if (GBApplication.isPriorityNumber(Policy.PRIORITY_CATEGORY_MESSAGES, notificationSpec.phoneNumber)) {
                                    break;
                                }
                                return;
                        }
                        GBApplication.deviceService().onNotification(notificationSpec);
                    }
                }
            }
        }
    }
}
