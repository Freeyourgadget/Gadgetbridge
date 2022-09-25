package nodomain.freeyourgadget.gadgetbridge.devices.supercars;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.service.devices.supercars.SuperCarsSupport;

public class ControlActivity extends AbstractGBActivity {
    LocalBroadcastManager localBroadcastManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_supercars_control);
        localBroadcastManager = LocalBroadcastManager.getInstance(this);

        View.OnTouchListener controlTouchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                String command = "idle";
                if (v.getId() == R.id.supercars_control_button_left_up) {
                    command = "left_up";
                } else if (v.getId() == R.id.supercars_control_button_center_up) {
                    command = "center_up";
                } else if (v.getId() == R.id.supercars_control_button_right_up) {
                    command = "right_up";
                }
                if (v.getId() == R.id.supercars_control_button_left_down) {
                    command = "left_down";
                } else if (v.getId() == R.id.supercars_control_button_center_down) {
                    command = "center_down";
                } else if (v.getId() == R.id.supercars_control_button_right_down) {
                    command = "right_down";
                }
                Intent intent = new Intent(SuperCarsSupport.COMMAND_DRIVE_CONTROL);
                intent.putExtra(SuperCarsSupport.EXTRA_DIRECTION, command);
                sendLocalBroadcast(intent);
                return true;
            }
        };

        findViewById(R.id.supercars_control_button_left_up).setOnTouchListener(controlTouchListener);
        findViewById(R.id.supercars_control_button_center_up).setOnTouchListener(controlTouchListener);
        findViewById(R.id.supercars_control_button_right_up).setOnTouchListener(controlTouchListener);
        findViewById(R.id.supercars_control_button_left_down).setOnTouchListener(controlTouchListener);
        findViewById(R.id.supercars_control_button_center_down).setOnTouchListener(controlTouchListener);
        findViewById(R.id.supercars_control_button_right_down).setOnTouchListener(controlTouchListener);


    }

    private void sendLocalBroadcast(Intent intent) {
        localBroadcastManager.sendBroadcast(intent);
    }

}
