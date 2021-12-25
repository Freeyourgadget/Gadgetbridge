/*  Copyright (C) 2020-2021 Daniel Dakhno

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
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.devices.qhybrid;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport;

public class CalibrationActivity extends AbstractGBActivity {
    enum HAND{
        MINUTE,
        HOUR,
        SUB;

        public String getDisplayName(){
            return name().substring(0, 1).toUpperCase() + name().substring(1).toLowerCase();
        }

        public String getVariableName(){
            return name();
        }


        @NonNull
        @Override
        public String toString() {
            return getDisplayName();
        }
    }

    enum MOVE_BUTTON{
        CLOCKWISE_ONE(R.id.qhybrid_calibration_clockwise_1, 1),
        CLOCKWISE_TEN(R.id.qhybrid_calibration_clockwise_10, 10),
        CLOCKWISE_HUNRED(R.id.qhybrid_calibration_clockwise_100, 100),

        COUNTER_CLOCKWISE_ONE(R.id.qhybrid_calibration_counter_clockwise_1, -1),
        COUNTER_CLOCKWISE_TEN(R.id.qhybrid_calibration_counter_clockwise_10, -10),
        COUNTER_CLOCKWISE_HUNRED(R.id.qhybrid_calibration_counter_clockwise_100, -100),
        ;

        int layoutId;
        int distance;

        MOVE_BUTTON(int layoutId, int distance) {
            this.layoutId = layoutId;
            this.distance = distance;
        }
    }

    HAND selectedHand = HAND.MINUTE;
    LocalBroadcastManager localBroadcastManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qhybrid_calibration);

        GBDevice device = GBApplication.app().getDeviceManager().getSelectedDevice();

        if(device == null || device.getType() != DeviceType.FOSSILQHYBRID){
            Toast.makeText(this, R.string.watch_not_connected, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        localBroadcastManager = LocalBroadcastManager.getInstance(this);

        localBroadcastManager.sendBroadcast(
                new Intent(QHybridSupport.QHYBRID_COMMAND_CONTROL)
        );

        initViews();
    }

    private void initViews(){
        Spinner handSpinner = findViewById(R.id.qhybrid_calibration_hand_spinner);
        handSpinner.setAdapter(new ArrayAdapter<HAND>(this, android.R.layout.simple_spinner_dropdown_item, HAND.values()));
        handSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedHand = (HAND) parent.getSelectedItem();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        for(final MOVE_BUTTON buttonDeclaration : MOVE_BUTTON.values()) {
            final Button button = findViewById(buttonDeclaration.layoutId);

            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(QHybridSupport.QHYBRID_COMMAND_MOVE);
                    intent.putExtra("EXTRA_DISTANCE_" + selectedHand.getVariableName(), (short) buttonDeclaration.distance);

                    localBroadcastManager.sendBroadcast(intent);
                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        localBroadcastManager.sendBroadcast(new Intent(QHybridSupport.QHYBRID_COMMAND_SAVE_CALIBRATION));
        localBroadcastManager.sendBroadcast(new Intent(QHybridSupport.QHYBRID_COMMAND_UNCONTROL));
    }
}
