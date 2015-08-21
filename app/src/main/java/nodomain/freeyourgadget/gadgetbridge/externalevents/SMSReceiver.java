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

public class SMSReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        if ("never".equals(sharedPrefs.getString("notification_mode_sms", "when_screen_off"))) {
            return;
        }
        if ("when_screen_off".equals(sharedPrefs.getString("notification_mode_sms", "when_screen_off"))) {
            PowerManager powermanager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (powermanager.isScreenOn()) {
                return;
            }
        }

        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            Object[] pdus = (Object[]) bundle.get("pdus");
            for (Object pdu1 : pdus) {
                byte[] pdu = (byte[]) pdu1;
                SmsMessage message = SmsMessage.createFromPdu(pdu);
                String body = message.getDisplayMessageBody();
                String sender = message.getOriginatingAddress();
                if (sender != null && body != null) {
                    GBApplication.deviceService().onSMS(sender, body);
                }
            }
        }
    }
}
