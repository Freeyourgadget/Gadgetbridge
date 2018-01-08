package nodomain.freeyourgadget.gadgetbridge.database;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.SystemClock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;

/**
 * Created by maufl on 1/4/18.
 */

public class PeriodicExporter extends BroadcastReceiver {
    private static final Logger LOG = LoggerFactory.getLogger(PeriodicExporter.class);


    public static void sheduleAlarm(Context context, Integer autoExportPeriod, boolean autoExportEnabled) {
        Intent i = new Intent(context, PeriodicExporter.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0 , i, 0);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pi);
        if (!autoExportEnabled) {
            return;
        }
        int exportPeriod = autoExportPeriod * 60 * 60 * 1000;
        if (exportPeriod == 0) {
            return;
        }
        LOG.info("Enabling periodic export");
        am.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + exportPeriod,
                exportPeriod,
                pi
        );
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        LOG.info("Exporting DB");
        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            DBHelper helper = new DBHelper(context);
            String dst = GBApplication.getPrefs().getString(GBPrefs.AUTO_EXPORT_LOCATION, null);
            if (dst == null) {
                LOG.info("Unable to export DB, export location not set");
                return;
            }
            Uri dstUri = Uri.parse(dst);
            OutputStream out = context.getContentResolver().openOutputStream(dstUri);
            helper.exportDB(dbHandler, out);
        } catch (Exception ex) {
            GB.updateExportFailedNotification(context.getString(R.string.notif_export_failed_title), context);
            LOG.info("Exception while exporting DB: ", ex);
        }
    }
}
