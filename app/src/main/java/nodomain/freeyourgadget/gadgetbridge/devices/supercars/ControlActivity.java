package nodomain.freeyourgadget.gadgetbridge.devices.supercars;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
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
    CountDownTimer periodicDataSenderRunner;

    SuperCarsConstants.Direction direction = SuperCarsConstants.Direction.CENTER;
    SuperCarsConstants.Movement movement = SuperCarsConstants.Movement.IDLE;
    SuperCarsConstants.Speed speed = SuperCarsConstants.Speed.NORMAL;
    SuperCarsConstants.Light light = SuperCarsConstants.Light.OFF;

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

        //when this activity is open, data is sent continuously every 200ms
        periodicDataSender();
        periodicDataSenderRunner.start();
    }

    private void sendLocalBroadcast(Intent intent) {
        localBroadcastManager.sendBroadcast(intent);
    }

    public void periodicDataSender() {
        periodicDataSenderRunner = new CountDownTimer(Long.MAX_VALUE, 200) {

            public void onTick(long millisUntilFinished) {
                create_intent_with_data();
            }

            public void onFinish() {
                start();
            }
        };
    }

    private void create_intent_with_data() {
        Intent intent = new Intent(SuperCarsSupport.COMMAND_DRIVE_CONTROL);
        intent.putExtra(SuperCarsSupport.EXTRA_DIRECTION, direction);
        intent.putExtra(SuperCarsSupport.EXTRA_MOVEMENT, movement);
        intent.putExtra(SuperCarsSupport.EXTRA_SPEED, speed);
        intent.putExtra(SuperCarsSupport.EXTRA_LIGHT, light);
        sendLocalBroadcast(intent);
    }

    @Override
    public void onJoystickMoved(float xPercent, float yPercent, int id) {

        if (yPercent < 0) {
            movement = SuperCarsConstants.Movement.UP;
        } else if (yPercent > 0) {
            movement = SuperCarsConstants.Movement.DOWN;
        } else {
            movement = SuperCarsConstants.Movement.IDLE;
        }

        if (xPercent < -0.5) {
            direction = SuperCarsConstants.Direction.LEFT;
        } else if (xPercent > 0.5) {
            direction = SuperCarsConstants.Direction.RIGHT;
        } else {
            direction = SuperCarsConstants.Direction.CENTER;
        }

        if (lights) {
            light = SuperCarsConstants.Light.ON;
        } else {
            light = SuperCarsConstants.Light.OFF;
        }
        if (turbo) {
            speed = SuperCarsConstants.Speed.TURBO;
        } else {
            speed = SuperCarsConstants.Speed.NORMAL;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        periodicDataSenderRunner.cancel();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        periodicDataSenderRunner.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        periodicDataSenderRunner.cancel();
    }
}
