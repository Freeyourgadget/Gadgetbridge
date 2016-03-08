package nodomain.freeyourgadget.gadgetbridge.externalevents;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.PowerManager;
import android.preference.PreferenceManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;

public class K9Receiver extends BroadcastReceiver {

    private static final Logger LOG = LoggerFactory.getLogger(K9Receiver.class);
    private final Uri k9Uri = Uri.parse("content://com.fsck.k9.messageprovider/inbox_messages");

    @Override
    public void onReceive(Context context, Intent intent) {

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        if ("never".equals(sharedPrefs.getString("notification_mode_k9mail", "when_screen_off"))) {
            return;
        }
        if ("when_screen_off".equals(sharedPrefs.getString("notification_mode_k9mail", "when_screen_off"))) {
            PowerManager powermanager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (powermanager.isScreenOn()) {
                return;
            }
        }

        String uriWanted = intent.getData().toString();

        String[] messagesProjection = {
                "senderAddress",
                "subject",
                "preview",
                "uri"
        };

        NotificationSpec notificationSpec = new NotificationSpec();
        notificationSpec.id = -1;
        notificationSpec.type = NotificationType.EMAIL;

        /*
         * there seems to be no way to specify the the uri in the where clause.
         * If we do so, we just get the newest message, not the one requested.
         * So, we will just search our message and match the uri manually.
         * It should be the first one returned by the query in most cases,
         */

        try (Cursor c = context.getContentResolver().query(k9Uri, messagesProjection, null, null, null)) {
            if (c != null) {
                while (c.moveToNext()) {
                    String uri = c.getString(c.getColumnIndex("uri"));
                    if (uri.equals(uriWanted)) {
                        notificationSpec.sender = c.getString(c.getColumnIndex("senderAddress"));
                        notificationSpec.subject = c.getString(c.getColumnIndex("subject"));
                        notificationSpec.body = c.getString(c.getColumnIndex("preview"));
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            notificationSpec.sender = "Gadgetbridge";
            notificationSpec.subject = "Permission Error?";
            notificationSpec.body = "Please reinstall Gadgetbridge to enable K-9 Mail notifications";
        }

        GBApplication.deviceService().onNotification(notificationSpec);
    }
}
