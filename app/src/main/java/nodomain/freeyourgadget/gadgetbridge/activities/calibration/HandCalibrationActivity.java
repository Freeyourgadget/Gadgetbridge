/*  Copyright (C) 2020-2024 Daniel Dakhno, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.activities.calibration;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class HandCalibrationActivity extends AbstractGBActivity {
    private GBDevice device;
    private boolean shouldSave;

    enum HAND {
        MINUTE,
        HOUR,
        SUB;

        public String getDisplayName() {
            return name().substring(0, 1).toUpperCase() + name().substring(1).toLowerCase();
        }

        public String getVariableName() {
            return name();
        }

        @NonNull
        @Override
        public String toString() {
            return getDisplayName();
        }
    }

    enum MOVE_BUTTON {
        CLOCKWISE_ONE(R.id.hand_calibration_clockwise_1, 1),
        CLOCKWISE_TEN(R.id.hand_calibration_clockwise_10, 10),
        CLOCKWISE_HUNDRED(R.id.hand_calibration_clockwise_100, 100),

        COUNTER_CLOCKWISE_ONE(R.id.hand_calibration_counter_clockwise_1, -1),
        COUNTER_CLOCKWISE_TEN(R.id.hand_calibration_counter_clockwise_10, -10),
        COUNTER_CLOCKWISE_HUNDRED(R.id.hand_calibration_counter_clockwise_100, -100),
        ;

        private final int layoutId;
        private final int distance;

        MOVE_BUTTON(final int layoutId, final int distance) {
            this.layoutId = layoutId;
            this.distance = distance;
        }
    }

    HAND selectedHand = HAND.MINUTE;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        device = getIntent().getParcelableExtra(GBDevice.EXTRA_DEVICE);
        if (device == null || !device.isInitialized()) {
            GB.toast(getString(R.string.watch_not_connected), Toast.LENGTH_SHORT, GB.INFO);
            finish();
        }

        setContentView(R.layout.activity_hand_calibration);

        final FloatingActionButton fab = findViewById(R.id.fab_hand_calibration_save);
        fab.setOnClickListener(view -> {
            // TODO save
            shouldSave = true;
            finish();
        });

        // TODO start calibration

        initViews();
    }

    private void initViews() {
        final Spinner handSpinner = findViewById(R.id.hand_calibration_hand_spinner);
        handSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, HAND.values()));
        handSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(final AdapterView<?> parent, final View view, final int position, final long id) {
                selectedHand = (HAND) parent.getSelectedItem();
            }

            @Override
            public void onNothingSelected(final AdapterView<?> parent) {

            }
        });

        for (final MOVE_BUTTON buttonDeclaration : MOVE_BUTTON.values()) {
            final Button button = findViewById(buttonDeclaration.layoutId);

            button.setOnClickListener(v -> {
                final short step = (short) buttonDeclaration.distance;

                // TODO move
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

        if (!shouldSave) {
            // If we're supposed to save, we already did it in the fab
            // TODO abort
            //GBApplication.deviceService(device).onGenericCommand();
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            // back button
            // TODO abort
            //GBApplication.deviceService(device).onGenericCommand();
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
