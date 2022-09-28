package nodomain.freeyourgadget.gadgetbridge.devices.supercars;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.service.devices.supercars.SuperCarsSupport;

public class ControlActivity extends AbstractGBActivity implements JoystickView.JoystickListener {
    private static final Logger LOG = LoggerFactory.getLogger(ControlActivity.class);
    LocalBroadcastManager localBroadcastManager;
    boolean lights = false;
    boolean turbo = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_supercars_control);
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        CheckBox turboMode = findViewById(R.id.turboMode);
        CheckBox lightsOn = findViewById(R.id.lightsOn);

        turboMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                turbo = isChecked;
            }
        });

        lightsOn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                lights = isChecked;
            }
        });
    }

    private void sendLocalBroadcast(Intent intent) {
        localBroadcastManager.sendBroadcast(intent);
    }

    @Override

    public void onJoystickMoved(float xPercent, float yPercent, int id) {
        if (yPercent != 0 && yPercent != 0) {
            SuperCarsConstants.Directions command;
            SuperCarsConstants.SpeedModes mode;

            if (yPercent < 0) {
                command = SuperCarsConstants.Directions.UP;
                if (xPercent < -0.5) {
                    command = SuperCarsConstants.Directions.UP_LEFT;
                } else if (xPercent > 0.5) {
                    command = SuperCarsConstants.Directions.UP_RIGHT;
                }
            } else {
                command = SuperCarsConstants.Directions.DOWN;
                if (xPercent < -0.5) {
                    command = SuperCarsConstants.Directions.DOWN_LEFT;
                } else if (xPercent > 0.5) {
                    command = SuperCarsConstants.Directions.DOWN_RIGHT;
                }
            }

            mode = SuperCarsConstants.SpeedModes.NORMAL;
            if (lights) {
                mode = SuperCarsConstants.SpeedModes.LIGHTS;
                if (turbo) {
                    mode = SuperCarsConstants.SpeedModes.TURBO_LIGHTS;
                }
            } else {
                if (turbo) {
                    mode = SuperCarsConstants.SpeedModes.TURBO;
                }
            }

            Intent intent = new Intent(SuperCarsSupport.COMMAND_DRIVE_CONTROL);
            intent.putExtra(SuperCarsSupport.EXTRA_DIRECTION, command);
            intent.putExtra(SuperCarsSupport.EXTRA_MODE, mode);
            sendLocalBroadcast(intent);
        }

    }
}
