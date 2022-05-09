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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.adapter.GBWorldClockListAdapter;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.entities.WorldClock;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;


public class ConfigureWorldClocks extends AbstractGBActivity {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigureWorldClocks.class);

    private static final int REQ_CONFIGURE_WORLD_CLOCK = 1;

    private GBWorldClockListAdapter mGBWorldClockListAdapter;
    private GBDevice gbDevice;

    private BroadcastReceiver timeTickBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_configure_world_clocks);

        gbDevice = getIntent().getParcelableExtra(GBDevice.EXTRA_DEVICE);

        mGBWorldClockListAdapter = new GBWorldClockListAdapter(this);

        final RecyclerView worldClocksRecyclerView = findViewById(R.id.world_clock_list);
        worldClocksRecyclerView.setHasFixedSize(true);
        worldClocksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        worldClocksRecyclerView.setAdapter(mGBWorldClockListAdapter);
        updateWorldClocksFromDB();

        final FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(gbDevice);

                int deviceSlots = coordinator.getWorldClocksSlotCount();

                if (mGBWorldClockListAdapter.getItemCount() >= deviceSlots) {
                    // No more free slots
                    new AlertDialog.Builder(v.getContext())
                            .setTitle(R.string.world_clock_no_free_slots_title)
                            .setMessage(getBaseContext().getString(R.string.world_clock_no_free_slots_description, String.format(Locale.getDefault(), "%d", deviceSlots)))
                            .setIcon(R.drawable.ic_warning)
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(final DialogInterface dialog, final int whichButton) {
                                }
                            })
                            .show();
                    return;
                }

                final WorldClock worldClock;
                try (DBHandler db = GBApplication.acquireDB()) {
                    final DaoSession daoSession = db.getDaoSession();
                    final Device device = DBHelper.getDevice(gbDevice, daoSession);
                    final User user = DBHelper.getUser(daoSession);
                    worldClock = createDefaultWorldClock(device, user);
                } catch (final Exception e) {
                    LOG.error("Error accessing database", e);
                    return;
                }

                configureWorldClock(worldClock);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        timeTickBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, Intent intent) {
                if (Intent.ACTION_TIME_TICK.equals(intent.getAction())) {
                    // Refresh the UI, to update the current time in each timezone
                    mGBWorldClockListAdapter.notifyDataSetChanged();
                }
            }
        };

        registerReceiver(timeTickBroadcastReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (timeTickBroadcastReceiver != null) {
            unregisterReceiver(timeTickBroadcastReceiver);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh to update the current time on each clock
        mGBWorldClockListAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_CONFIGURE_WORLD_CLOCK && resultCode == 1) {
            updateWorldClocksFromDB();
            sendWorldClocksToDevice();
        }
    }

    private WorldClock createDefaultWorldClock(@NonNull Device device, @NonNull User user) {
        final WorldClock worldClock = new WorldClock();
        final String timezone = TimeZone.getDefault().getID();
        worldClock.setTimeZoneId(timezone);
        final String[] timezoneParts = timezone.split("/");
        worldClock.setLabel(timezoneParts[timezoneParts.length - 1]);

        worldClock.setDeviceId(device.getId());
        worldClock.setUserId(user.getId());
        worldClock.setWorldClockId(UUID.randomUUID().toString());

        return worldClock;
    }

    /**
     * Reads the available worldClocks from the database and updates the view afterwards.
     */
    private void updateWorldClocksFromDB() {
        final List<WorldClock> worldClocks = DBHelper.getWorldClocks(gbDevice);

        mGBWorldClockListAdapter.setWorldClockList(worldClocks);
        mGBWorldClockListAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // back button
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void configureWorldClock(final WorldClock worldClock) {
        final Intent startIntent = new Intent(getApplicationContext(), WorldClockDetails.class);
        startIntent.putExtra(GBDevice.EXTRA_DEVICE, gbDevice);
        startIntent.putExtra(WorldClock.EXTRA_WORLD_CLOCK, worldClock);
        startActivityForResult(startIntent, REQ_CONFIGURE_WORLD_CLOCK);
    }

    public void deleteWorldClock(final WorldClock worldClock) {
        DBHelper.delete(worldClock);
        updateWorldClocksFromDB();
        sendWorldClocksToDevice();
    }

    private void sendWorldClocksToDevice() {
        if (gbDevice.isInitialized()) {
            GBApplication.deviceService().onSetWorldClocks(mGBWorldClockListAdapter.getWorldClockList());
        }
    }
}
