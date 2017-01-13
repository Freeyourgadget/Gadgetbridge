package nodomain.freeyourgadget.gadgetbridge.activities;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.widget.TextView;

import nodomain.freeyourgadget.gadgetbridge.R;

public class WeatherNotificationConfig extends GBActivity {
    @Override
    public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        setContentView(R.layout.activity_weather_notification);
    }
}
