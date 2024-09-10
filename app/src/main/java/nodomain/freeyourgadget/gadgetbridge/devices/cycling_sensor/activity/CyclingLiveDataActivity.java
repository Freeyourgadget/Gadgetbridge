package nodomain.freeyourgadget.gadgetbridge.devices.cycling_sensor.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.SettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.entities.CyclingSample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class CyclingLiveDataActivity extends AbstractGBActivity {
    private TextView speedView, tripDistanceView, totalDistanceView;
    private GBDevice selectedDevice;
    private float tripStartDistance = 0, tripCurrentDistance = 0;
    private float toUnitFactor = 1;
    private int
        speedStringResource = R.string.km_h,
        tripStringResource = R.string.label_distance_trip,
        totalStringResource = R.string.label_distance_total;

    private static final String PREFS_KEY_TRIP_START = "CYCLING_TRIP_START";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getIntent() == null) {
            selectedDevice = getStoredCyclingSensor();
        }else if(getIntent().getExtras() == null) {
            selectedDevice = getStoredCyclingSensor();
        }else if((selectedDevice =
                getIntent().getExtras().getParcelable("device")) == null) {
            selectedDevice = getStoredCyclingSensor();
        }

        if (selectedDevice == null) {
            GB.toast(getString(R.string.error_no_cycling_sensor_found), Toast.LENGTH_SHORT, GB.ERROR);
            finish();
            return;
        }

        setContentView(R.layout.activity_cycling_live_data);

        speedView = findViewById(R.id.cycling_data_speed);
        tripDistanceView = findViewById(R.id.cycling_data_trip_distance);
        totalDistanceView = findViewById(R.id.cycling_data_total_distance);

        tripStartDistance = GBApplication
                .getDevicePrefs(selectedDevice)
                .getFloat(PREFS_KEY_TRIP_START, 0);

        tripDistanceView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if(tripCurrentDistance == 0) {
                    return true;
                }

                tripStartDistance = tripCurrentDistance;

                GBApplication
                        .getDeviceSpecificSharedPrefs(selectedDevice.getAddress())
                        .edit()
                        .putFloat(PREFS_KEY_TRIP_START, tripStartDistance)
                        .apply();

                tripDistanceView.setText(getString(tripStringResource, 0f));

                return true;
            }
        });

        String measurementSystem = GBApplication.getPrefs().getString(SettingsActivity.PREF_MEASUREMENT_SYSTEM, "metric");

        if(!measurementSystem.equals("metric")) {
            toUnitFactor = 0.621371f;

            speedStringResource = R.string.mi_h;
            tripStringResource = R.string.label_distance_trip_mph;
            totalStringResource = R.string.label_distance_total_mph;
        }
    }

    private GBDevice getStoredCyclingSensor(){
        List<GBDevice> devices = GBApplication
                .app()
                .getDeviceManager()
                .getDevices();

        for(GBDevice device: devices) {
            if (device.getState() != GBDevice.State.INITIALIZED) continue;
            if (device.getType() != DeviceType.CYCLING_SENSOR) continue;

            return device;
        }

        return null;
    }

    private BroadcastReceiver cyclingDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String deviceAddress = intent.getStringExtra("EXTRA_DEVICE_ADDRESS");

            if(deviceAddress == null) {
                return;
            }

            if(!deviceAddress.equals(selectedDevice.getAddress())) {
                return;
            }

            CyclingSample sample = (CyclingSample) intent.getSerializableExtra(DeviceService.EXTRA_REALTIME_SAMPLE);

            if(sample == null) {
                return;
            }

            Float metersPerSecond = sample.getSpeed();
            if(metersPerSecond != null) {
                float kmh = metersPerSecond * 3.6f * toUnitFactor;
                speedView.setText(String.format("%.1f %s", kmh, getString(speedStringResource)));
            }else{
                speedView.setText(String.format("%.1f %s", 0f, getString(speedStringResource)));
            }

            tripCurrentDistance = sample.getDistance();

            tripDistanceView.setText(getString(tripStringResource, ((tripCurrentDistance - tripStartDistance) * toUnitFactor) / 1000));
            totalDistanceView.setText(getString(totalStringResource, (tripCurrentDistance * toUnitFactor) / 1000));
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        LocalBroadcastManager
                .getInstance(this)
                .registerReceiver(
                        cyclingDataReceiver,
                        new IntentFilter(DeviceService.ACTION_REALTIME_SAMPLES)
                );
    }

    @Override
    protected void onPause() {
        super.onPause();

        LocalBroadcastManager
                .getInstance(this)
                .unregisterReceiver(cyclingDataReceiver);
    }

}
