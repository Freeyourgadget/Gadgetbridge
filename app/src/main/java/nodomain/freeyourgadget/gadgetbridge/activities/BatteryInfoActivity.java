package nodomain.freeyourgadget.gadgetbridge.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class BatteryInfoActivity extends AbstractGBActivity {
    private static final Logger LOG = LoggerFactory.getLogger(BatteryInfoActivity.class);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        GBDevice gbDevice;
        super.onCreate(savedInstanceState);

        final Context appContext = this.getApplicationContext();
        if (appContext instanceof GBApplication) {
            setContentView(R.layout.activity_battery_info);
        }

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            gbDevice = bundle.getParcelable(GBDevice.EXTRA_DEVICE);
        } else {
            throw new IllegalArgumentException("Must provide a device when invoking this activity");
        }

        final BatteryInfoChartFragment batteryInfoChartFragment = new BatteryInfoChartFragment();

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.batteryChartFragmentHolder, batteryInfoChartFragment)
                .commit();

        int timeTo = (int) (System.currentTimeMillis() / 1000);
        int timeFrom = timeTo - 24 * 3600 * 14; //two weeks of data for the start

        batteryInfoChartFragment.setDateAndGetData(gbDevice, timeFrom, timeTo);

        TextView battery_status_device_name_text = (TextView) findViewById(R.id.battery_status_device_name_text);
        ImageView battery_status_device_icon = findViewById(R.id.battery_status_device_icon);
        battery_status_device_icon.setImageResource(gbDevice.isInitialized() ? gbDevice.getType().getIcon() : gbDevice.getType().getDisabledIcon());
        TextView battery_status_battery_level_text = (TextView) findViewById(R.id.battery_status_battery_level_text);

        String level = String.valueOf(gbDevice.getBatteryLevel());
        String state = String.valueOf(gbDevice.getBatteryState());
        battery_status_device_name_text.setText(gbDevice.getName());
        battery_status_battery_level_text.setText(String.format("%s%%", level));
    }

}


