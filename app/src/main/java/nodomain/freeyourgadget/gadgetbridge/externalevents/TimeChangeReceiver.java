package nodomain.freeyourgadget.gadgetbridge.externalevents;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.GregorianCalendar;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;


public class TimeChangeReceiver extends BroadcastReceiver {

    private static final Logger LOG = LoggerFactory.getLogger(TimeChangeReceiver.class);

    @Override
    public void onReceive(Context context, Intent intent) {
        Prefs prefs = GBApplication.getPrefs();
        final String action = intent.getAction();

        if (prefs.getBoolean("datetime_synconconnect", true) && (action.equals(Intent.ACTION_TIME_CHANGED) || action.equals(Intent.ACTION_TIMEZONE_CHANGED))) {
            Date newTime = GregorianCalendar.getInstance().getTime();
            LOG.info("Time or Timezone changed, syncing with device: " + DateTimeUtils.formatDate(newTime) + " (" + newTime.toGMTString() + "), " + intent.getAction());
            GBApplication.deviceService().onSetTime();
        }
    }
}