package nodomain.freeyourgadget.gadgetbridge.devices.idasen;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.Objects;


import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.workouts.WorkoutValueFormatter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.idasen.IdasenDeviceSupport;

public class IdasenControlActivity extends AbstractGBActivity {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    public TextView mDeskHeight, mDeskSpeed;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Objects.requireNonNull(action).equals(IdasenConstants.ACTION_REALTIME_DESK_VALUES)){
                final WorkoutValueFormatter formatter = new WorkoutValueFormatter();
                float height = (float) intent.getSerializableExtra(IdasenConstants.EXTRA_DESK_HEIGHT);
                float speed = (float) intent.getSerializableExtra(IdasenConstants.EXTRA_DESK_SPEED);
                logger.debug("Received desk values: {} {}", speed, height);
                mDeskHeight.setText(formatter.formatValue(height * 100F, "cm"));
                mDeskSpeed.setText(formatter.formatValue(speed * 1000F, "mm/s"));
            }
        }
    };

    LocalBroadcastManager localBroadcastManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_idasen_control);

        localBroadcastManager = LocalBroadcastManager.getInstance(this);

        mDeskHeight = findViewById(R.id.idasen_desk_height);
        mDeskSpeed = findViewById(R.id.idasen_desk_speed);

        IntentFilter filterLocal = new IntentFilter();
        filterLocal.addAction(IdasenConstants.ACTION_REALTIME_DESK_VALUES);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mReceiver, filterLocal);
        Intent intent = new Intent(IdasenDeviceSupport.COMMAND_GET_DESK_VALUES);
        sendLocalBroadcast(intent);

        View.OnTouchListener controlTouchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Intent intent = null;

                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (v.getId() == R.id.idasen_control_button_up) {
                         intent = new Intent(IdasenDeviceSupport.COMMAND_UP);
                    } else if (v.getId() == R.id.idasen_control_button_down) {
                         intent = new Intent(IdasenDeviceSupport.COMMAND_DOWN);
                    } else if (v.getId() == R.id.idasen_control_button_sit) {
                         intent = new Intent(IdasenDeviceSupport.COMMAND_SET_HEIGHT);
                        intent.putExtra(IdasenDeviceSupport.TARGET_HEIGHT, IdasenDeviceSupport.TARGET_POS_SIT);
                    } else if (v.getId() == R.id.idasen_control_button_stand) {
                         intent = new Intent(IdasenDeviceSupport.COMMAND_SET_HEIGHT);
                        intent.putExtra(IdasenDeviceSupport.TARGET_HEIGHT, IdasenDeviceSupport.TARGET_POS_STAND);
                    } else if (v.getId() == R.id.idasen_control_button_mid) {
                         intent = new Intent(IdasenDeviceSupport.COMMAND_SET_HEIGHT);
                        intent.putExtra(IdasenDeviceSupport.TARGET_HEIGHT, IdasenDeviceSupport.TARGET_POS_MID);
                    }
                    sendLocalBroadcast(intent);
                } else {
                    return false;
                }
                return true;
            }
        };
        findViewById(R.id.idasen_control_button_up).setOnTouchListener(controlTouchListener);
        findViewById(R.id.idasen_control_button_down).setOnTouchListener(controlTouchListener);
        findViewById(R.id.idasen_control_button_sit).setOnTouchListener(controlTouchListener);
        findViewById(R.id.idasen_control_button_mid).setOnTouchListener(controlTouchListener);
        findViewById(R.id.idasen_control_button_stand).setOnTouchListener(controlTouchListener);
    }

    private void sendLocalBroadcast(Intent intent) {
        localBroadcastManager.sendBroadcast(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        localBroadcastManager.unregisterReceiver(mReceiver);
    }
}
