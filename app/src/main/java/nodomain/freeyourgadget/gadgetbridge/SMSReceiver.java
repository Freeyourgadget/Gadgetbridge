package nodomain.freeyourgadget.gadgetbridge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.telephony.SmsMessage;

public class SMSReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (!sharedPrefs.getBoolean("notifications_sms", true)) {
            return;
        }
        if (!sharedPrefs.getBoolean("notifications_sms_whenscreenon", false)) {
            PowerManager powermanager = (PowerManager) context.getSystemService(context.POWER_SERVICE);
            if (powermanager.isScreenOn()) {
                return;
            }
        }

        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            Object[] pdus = (Object[]) bundle.get("pdus");
            for (int i = 0; i < pdus.length; i++) {
                byte[] pdu = (byte[]) pdus[i];
                SmsMessage message = SmsMessage.createFromPdu(pdu);
                String body = message.getDisplayMessageBody();
                String sender = message.getOriginatingAddress();
                if (sender != null && body != null) {
                    Intent startIntent = new Intent(context, BluetoothCommunicationService.class);
                    startIntent.setAction(BluetoothCommunicationService.ACTION_NOTIFICATION_SMS);
                    startIntent.putExtra("notification_sender", sender);
                    startIntent.putExtra("notification_body", body);
                    context.startService(startIntent);
                }
            }
        }
    }
}
