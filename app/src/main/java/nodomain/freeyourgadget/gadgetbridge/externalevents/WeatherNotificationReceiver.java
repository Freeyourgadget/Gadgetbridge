package nodomain.freeyourgadget.gadgetbridge.externalevents;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WeatherNotificationReceiver extends BroadcastReceiver {

    private static final Logger LOG = LoggerFactory.getLogger(WeatherNotificationReceiver.class);
    private static final int VERSION = 2;
    private final String TAG = this.getClass().getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getAction().contains("WEATHER_UPDATE_2")) {
            LOG.info("Wrong action");
            return;
        }
        int f = intent.getParcelableExtra("ru.gelin.android.weather.notification.EXTRA_WEATHER");
        // int version = parcel.readInt();
        // if (version != VERSION) {
        //      LOG.info("Wrong version");
        //      return;
        //  }

        //Bundle bundle = parcel.readBundle(this.getClass().getClassLoader());
        //       String location = bundle.getString("weather_location");
        //     LOG.info("got location: " + location);
    }
}