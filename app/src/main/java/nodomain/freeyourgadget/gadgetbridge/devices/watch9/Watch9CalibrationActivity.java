package nodomain.freeyourgadget.gadgetbridge.devices.watch9;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class Watch9CalibrationActivity extends AbstractGBActivity {

    private static final String STATE_DEVICE = "stateDevice";
    GBDevice device;

    NumberPicker pickerHour, pickerMinute, pickerSecond;

    Handler handler;
    Runnable holdCalibration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch9_calibration);

        pickerHour = findViewById(R.id.np_hour);
        pickerMinute = findViewById(R.id.np_minute);
        pickerSecond = findViewById(R.id.np_second);

        pickerHour.setMinValue(1);
        pickerHour.setMaxValue(12);
        pickerHour.setValue(12);
        pickerMinute.setMinValue(0);
        pickerMinute.setMaxValue(59);
        pickerMinute.setValue(0);
        pickerSecond.setMinValue(0);
        pickerSecond.setMaxValue(59);
        pickerSecond.setValue(0);

        handler = new Handler();
        holdCalibration = new Runnable() {
            @Override
            public void run() {
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(Watch9Constants.ACTION_CALIBRATION_HOLD));
                handler.postDelayed(this, 10000);
            }
        };

        Intent intent = getIntent();
        device = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
        if (device == null && savedInstanceState != null) {
            device = savedInstanceState.getParcelable(STATE_DEVICE);
        }
        if (device == null) {
            finish();
        }

        final Button btCalibrate = findViewById(R.id.watch9_bt_calibrate);
        btCalibrate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btCalibrate.setEnabled(false);
                handler.removeCallbacks(holdCalibration);
                Intent calibrationData = new Intent(Watch9Constants.ACTION_CALIBRATION_SEND);
                calibrationData.putExtra(Watch9Constants.VALUE_CALIBRATION_HOUR, pickerHour.getValue());
                calibrationData.putExtra(Watch9Constants.VALUE_CALIBRATION_MINUTE, pickerMinute.getValue());
                calibrationData.putExtra(Watch9Constants.VALUE_CALIBRATION_SECOND, pickerSecond.getValue());
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(calibrationData);
                finish();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(STATE_DEVICE, device);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        device = savedInstanceState.getParcelable(STATE_DEVICE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent calibration = new Intent(Watch9Constants.ACTION_CALIBRATION);
        calibration.putExtra(Watch9Constants.ACTION_ENABLE, true);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(calibration);
        handler.postDelayed(holdCalibration, 1000);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Intent calibration = new Intent(Watch9Constants.ACTION_CALIBRATION);
        calibration.putExtra(Watch9Constants.ACTION_ENABLE, false);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(calibration);
        handler.removeCallbacks(holdCalibration);
    }
}
