package nodomain.freeyourgadget.gadgetbridge.externalevents;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
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
        notificationSpec.type = NotificationType.SMS;

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
                        GBApplication.deviceService().onNotification(notificationSpec);
                    }
                }
            }
        }
    }
}
