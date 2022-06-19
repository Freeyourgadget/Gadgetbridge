package nodomain.freeyourgadget.gadgetbridge.devices.binary_sensor.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.BinarySensorSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants.SensorState;

public class DataActivity extends AbstractGBActivity {
    TextView stateView, countView;

    BroadcastReceiver stateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (stateView == null) {
                return;
            }
            if (countView == null) {
                return;
            }
            boolean is_closed = intent.getBooleanExtra("EXTRA_SENSOR_CLOSED", false);
            int count = intent.getIntExtra("EXTRA_SENSOR_COUNT", -1);

            stateView.setText(is_closed ? "CLOSED" : "OPEN");
            stateView.setBackgroundResource(is_closed ? android.R.color.holo_green_light : android.R.color.holo_red_light);
            countView.setText("Count: " + count);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_binary_sensor_data);

        stateView = findViewById(R.id.text_sensor_state);
        countView = findViewById(R.id.text_sensor_count);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(BinarySensorSupport.ACTION_SENSOR_STATE_CHANGED);
        LocalBroadcastManager.getInstance(getApplicationContext())
                .registerReceiver(
                        stateReceiver,
                        filter
                );

        LocalBroadcastManager.getInstance(getApplicationContext())
                .sendBroadcast(new Intent(BinarySensorSupport.ACTION_SENSOR_STATE_REQUEST));
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getApplicationContext())
                .unregisterReceiver(
                        stateReceiver
                );
    }
}
