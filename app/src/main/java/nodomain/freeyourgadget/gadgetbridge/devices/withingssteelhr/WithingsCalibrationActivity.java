/*  Copyright (C) 2023-2024 Frank Ertl

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.devices.withingssteelhr;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.WithingsSteelHRDeviceSupport;

public class WithingsCalibrationActivity extends AbstractGBActivity {

    enum Hands {
        HOURS((short)1),
        MINUTES((short)0),
        ACTIVITY_TARGET((short)2);

        private final short code;
        Hands(short code) {
            this.code = code;
        }

    }

    private GBDevice device;
    private LocalBroadcastManager localBroadcastManager;
    private final String[] calibrationAdvices = new String[3];
    private final Hands[] hands = new Hands[]{Hands.HOURS, Hands.MINUTES, Hands.ACTIVITY_TARGET};
    private short handIndex = 0;
    private Button previousButton;
    private Button nextButton;
    private Button okButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_withings_calibration);
        List<GBDevice> devices = GBApplication.app().getDeviceManager().getSelectedDevices();
        for(GBDevice device : devices){
            if(device.getType() == DeviceType.WITHINGS_STEEL_HR ){
                this.device = device;
                break;
            }
        }

        if (device == null) {
            Toast.makeText(this, R.string.watch_not_connected, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        initView();
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.sendBroadcast(new Intent(WithingsSteelHRDeviceSupport.START_HANDS_CALIBRATION_CMD));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (localBroadcastManager != null) {
            localBroadcastManager.sendBroadcast(new Intent(WithingsSteelHRDeviceSupport.STOP_HANDS_CALIBRATION_CMD));
        }
    }

    private void initView() {

        calibrationAdvices[0] = getString(R.string.withings_calibration_text_hours);
        calibrationAdvices[1] = getString(R.string.withings_calibration_text_minutes);
        calibrationAdvices[2] = getString(R.string.withings_calibration_text_activity_target);

        RotaryControl rotaryControl = findViewById(R.id.rotary_control);
        rotaryControl.setRotationListener(new RotaryControl.RotationListener() {
            @Override
            public void onRotation(short movementAmount) {
                Intent calibration = new Intent(WithingsSteelHRDeviceSupport.HANDS_CALIBRATION_CMD);
                calibration.putExtra("hand", hands[handIndex].code);
                calibration.putExtra("movementAmount", movementAmount);
                localBroadcastManager.sendBroadcast(calibration);
            }
        });

        TextView textView = findViewById(R.id.withings_calibration_textview);
        textView.setText(calibrationAdvices[0]);
        previousButton = findViewById(R.id.withings_calibration_button_previous);
        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handIndex--;
                enableButtons();
                textView.setText(calibrationAdvices[handIndex]);
                rotaryControl.reset();
            }
        });
        nextButton = findViewById(R.id.withings_calibration_button_next);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handIndex++;
                enableButtons();
                textView.setText(calibrationAdvices[handIndex]);
                rotaryControl.reset();
            }
        });

        okButton = findViewById(R.id.withings_calibration_button_ok);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        enableButtons();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void enableButtons() {
        nextButton.setEnabled(handIndex < 2);
        previousButton.setEnabled(handIndex > 0);
        okButton.setEnabled(handIndex == 2);
    }
}