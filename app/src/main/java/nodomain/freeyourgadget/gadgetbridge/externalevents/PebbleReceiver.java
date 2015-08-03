package nodomain.freeyourgadget.gadgetbridge.externalevents;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.service.DeviceCommunicationService;

public class PebbleReceiver extends BroadcastReceiver {

    private static final Logger LOG = LoggerFactory.getLogger(PebbleReceiver.class);

    @Override
    public void onReceive(Context context, Intent intent) {

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        if ("never".equals(sharedPrefs.getString("notification_mode_pebblemsg", "when_screen_off"))) {
            return;
        }
        if ("when_screen_off".equals(sharedPrefs.getString("notification_mode_pebblemsg", "when_screen_off"))) {
            PowerManager powermanager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (powermanager.isScreenOn()) {
                return;
            }
        }

        String title;
        String body;

        String messageType = intent.getStringExtra("messageType");
        if (!messageType.equals("PEBBLE_ALERT")) {
            LOG.info("non PEBBLE_ALERT message type not supported");
            return;
        }
        String notificationData = intent.getStringExtra("notificationData");
        try {
            JSONArray notificationJSON = new JSONArray(notificationData);
            title = notificationJSON.getJSONObject(0).getString("title");
            body = notificationJSON.getJSONObject(0).getString("body");
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        if (title != null && body != null) {
            Intent startIntent = new Intent(context, DeviceCommunicationService.class);
            startIntent.setAction(DeviceCommunicationService.ACTION_NOTIFICATION_SMS);
            startIntent.putExtra("notification_sender", title);
            startIntent.putExtra("notification_body", body);
            context.startService(startIntent);
        }
    }
}
