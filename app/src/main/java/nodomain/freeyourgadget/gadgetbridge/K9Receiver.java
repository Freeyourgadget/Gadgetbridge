package nodomain.freeyourgadget.gadgetbridge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.PowerManager;
import android.preference.PreferenceManager;

public class K9Receiver extends BroadcastReceiver {

    private final String TAG = this.getClass().getSimpleName();
    private final Uri k9Uri = Uri.parse("content://com.fsck.k9.messageprovider/inbox_messages");

    @Override
    public void onReceive(Context context, Intent intent) {

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (!sharedPrefs.getBoolean("notifications_k9mail", true)) {
            return;
        }
        if (!sharedPrefs.getBoolean("notifications_k9mail_whenscreenon", false)) {
            PowerManager powermanager = (PowerManager) context.getSystemService(context.POWER_SERVICE);
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

        String sender = "";
        String subject = "";
        String preview = "";

        /*
         * there seems to be no way to specify the the uri in the where clause.
         * If we do so, we just get the newest message, not the one requested.
         * So, we will just search our message and match the uri manually.
         * It should be the first one returned by the query in most cases,
         */
        Cursor c = context.getContentResolver().query(k9Uri, messagesProjection, null, null, null);
        c.moveToFirst();
        do {
            String uri = c.getString(c.getColumnIndex("uri"));
            if (uri.equals(uriWanted)) {
                sender = c.getString(c.getColumnIndex("senderAddress"));
                subject = c.getString(c.getColumnIndex("subject"));
                preview = c.getString(c.getColumnIndex("preview"));
                break;
            }
        } while (c.moveToNext());
        c.close();

        Intent startIntent = new Intent(context, BluetoothCommunicationService.class);
        startIntent.setAction(BluetoothCommunicationService.ACTION_NOTIFICATION_EMAIL);
        startIntent.putExtra("notification_sender", sender);
        startIntent.putExtra("notification_subject", subject);
        startIntent.putExtra("notification_body", preview);

        context.startService(startIntent);
    }
}
