/*  Copyright (C) 2022 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.TimeZone;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.entities.WorldClock;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class WorldClockDetails extends AbstractGBActivity {
    private static final Logger LOG = LoggerFactory.getLogger(WorldClockDetails.class);

    private WorldClock worldClock;
    private GBDevice device;

    ArrayAdapter<String> timezoneAdapter;

    TextView worldClockTimezone;
    EditText worldClockLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_world_clock_details);

        worldClock = (WorldClock) getIntent().getSerializableExtra(WorldClock.EXTRA_WORLD_CLOCK);

        if (worldClock == null) {
            GB.toast("No worldClock provided to WorldClockDetails Activity", Toast.LENGTH_LONG, GB.ERROR);
            finish();
            return;
        }

        worldClockTimezone = findViewById(R.id.world_clock_timezone);
        worldClockLabel = findViewById(R.id.world_clock_label);

        device = getIntent().getParcelableExtra(GBDevice.EXTRA_DEVICE);
        final DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(device);

        final String[] timezoneIDs = TimeZone.getAvailableIDs();
        timezoneAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, timezoneIDs);

        final View cardTimezone = findViewById(R.id.card_timezone);
        cardTimezone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(WorldClockDetails.this).setAdapter(timezoneAdapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        worldClock.setTimeZoneId(timezoneIDs[i]);
                        updateUiFromWorldClock();
                    }
                }).create().show();
            }
        });

        worldClockLabel.setFilters(new InputFilter[]{new InputFilter.LengthFilter(coordinator.getWorldClocksLabelLength())});
        worldClockLabel.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(final CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(final Editable s) {
                worldClock.setLabel(s.toString());
            }
        });

        final FloatingActionButton fab = findViewById(R.id.fab_save);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateWorldClock();
                WorldClockDetails.this.setResult(1);
                finish();
            }
        });

        updateUiFromWorldClock();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // back button
                // TODO confirm when exiting without saving
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateWorldClock() {
        DBHelper.store(worldClock);
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putSerializable("worldClock", worldClock);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        worldClock = (WorldClock) savedInstanceState.getSerializable("worldClock");
        updateUiFromWorldClock();
    }

    public void updateUiFromWorldClock() {
        final String oldTimezone = worldClockTimezone.getText().toString();

        worldClockTimezone.setText(worldClock.getTimeZoneId());

        // Check if the label was still the default (the timezone city name)
        // If so, and if the user changed the timezone, update the label to match the new city name
        if (!oldTimezone.equals(worldClock.getTimeZoneId())) {
            final String[] oldTimezoneParts = oldTimezone.split("/");
            final String[] newTimezoneParts = worldClock.getTimeZoneId().split("/");
            final String newLabel = newTimezoneParts[newTimezoneParts.length - 1];
            final String oldLabel = oldTimezoneParts[oldTimezoneParts.length - 1];
            final String userLabel = worldClockLabel.getText().toString();

            if (userLabel.equals(oldLabel)) {
                // The label was still the original, so let's override it with the new city
                worldClock.setLabel(newLabel);
            }
        }

        worldClockLabel.setText(worldClock.getLabel());
    }
}
