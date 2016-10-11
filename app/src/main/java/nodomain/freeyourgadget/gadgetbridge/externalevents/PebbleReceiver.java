package nodomain.freeyourgadget.gadgetbridge.externalevents;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class PebbleReceiver extends BroadcastReceiver {

    private static final Logger LOG = LoggerFactory.getLogger(PebbleReceiver.class);

    @Override
    public void onReceive(Context context, Intent intent) {

        Prefs prefs = GBApplication.getPrefs();
        if ("never".equals(prefs.getString("notification_mode_pebblemsg", "when_screen_off"))) {
            return;
        }
        if ("when_screen_off".equals(prefs.getString("notification_mode_pebblemsg", "when_screen_off"))) {
            PowerManager powermanager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (powermanager.isScreenOn()) {
                return;
            }
        }

        String messageType = intent.getStringExtra("messageType");
        if (!messageType.equals("PEBBLE_ALERT")) {
            LOG.info("non PEBBLE_ALERT message type not supported");
            return;
        }

        if (!intent.hasExtra("notificationData")) {
            LOG.info("missing notificationData extra");
            return;
        }

        NotificationSpec notificationSpec = new NotificationSpec();
        notificationSpec.id = -1;

        String notificationData = intent.getStringExtra("notificationData");
        try {
            JSONArray notificationJSON = new JSONArray(notificationData);
            notificationSpec.title = notificationJSON.getJSONObject(0).getString("title");
            notificationSpec.body = notificationJSON.getJSONObject(0).getString("body");
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        if (notificationSpec.title != null) {
            notificationSpec.type = NotificationType.UNKNOWN;
            String sender = intent.getStringExtra("sender");
            if ("Conversations".equals(sender)) {
                notificationSpec.type = NotificationType.CONVERSATIONS;
            }
            else if ("OsmAnd".equals(sender)) {
                notificationSpec.type = NotificationType.GENERIC_NAVIGATION;
            }
            GBApplication.deviceService().onNotification(notificationSpec);
        }
    }
}
