/*  Copyright (C) 2015-2018 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, Lem Dulfo

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

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.adapter.GBAlarmListAdapter;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.entities.Alarm;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.AlarmUtils;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;


public class ConfigureAlarms extends AbstractGBActivity {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigureAlarms.class);

    private static final int REQ_CONFIGURE_ALARM = 1;

    private GBAlarmListAdapter mGBAlarmListAdapter;
    private boolean avoidSendAlarmsToDevice;
    private GBDevice gbDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_configure_alarms);

        gbDevice = getIntent().getParcelableExtra(GBDevice.EXTRA_DEVICE);

        mGBAlarmListAdapter = new GBAlarmListAdapter(this);

        RecyclerView alarmsRecyclerView = findViewById(R.id.alarm_list);
        alarmsRecyclerView.setHasFixedSize(true);
        alarmsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        alarmsRecyclerView.setAdapter(mGBAlarmListAdapter);
        updateAlarmsFromDB();
    }

    @Override
    protected void onPause() {
        if (!avoidSendAlarmsToDevice && gbDevice.isInitialized()) {
            sendAlarmsToDevice();
        }
        super.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_CONFIGURE_ALARM) {
            avoidSendAlarmsToDevice = false;
            updateAlarmsFromDB();
        }
    }

    /**
     * Reads the available alarms from the database and updates the view afterwards.
     */
    private void updateAlarmsFromDB() {
        List<Alarm> alarms = DBHelper.getAlarms(getGbDevice());
        if (alarms.isEmpty()) {
            alarms = AlarmUtils.readAlarmsFromPrefs(getGbDevice());
            storeMigratedAlarms(alarms);
        }
        addMissingAlarms(alarms);

        mGBAlarmListAdapter.setAlarmList(alarms);
        mGBAlarmListAdapter.notifyDataSetChanged();
    }

    private void storeMigratedAlarms(List<Alarm> alarms) {
        for (Alarm alarm : alarms) {
            DBHelper.store(alarm);
        }
    }

    private void addMissingAlarms(List<Alarm> alarms) {
        DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(getGbDevice());
        int supportedNumAlarms = coordinator.getAlarmSlotCount();
        if (supportedNumAlarms > alarms.size()) {
            try (DBHandler db = GBApplication.acquireDB()) {
                DaoSession daoSession = db.getDaoSession();
                Device device = DBHelper.getDevice(getGbDevice(), daoSession);
                User user = DBHelper.getUser(daoSession);
                for (int position = 0; position < supportedNumAlarms; position++) {
                    boolean found = false;
                    for (Alarm alarm : alarms) {
                        if (alarm.getPosition() == position) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        LOG.info("adding missing alarm at position " + position);
                        alarms.add(position, createDefaultAlarm(device, user, position));
                    }
                }
            } catch (Exception e) {
                LOG.error("Error accessing database", e);
            }
        }
    }

    private Alarm createDefaultAlarm(@NonNull Device device, @NonNull User user, int position) {
        return new Alarm(device.getId(), user.getId(), position, false, false,0, 6, 30);
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

    public void configureAlarm(Alarm alarm) {
        avoidSendAlarmsToDevice = true;
        Intent startIntent = new Intent(getApplicationContext(), AlarmDetails.class);
        startIntent.putExtra(Alarm.EXTRA_ALARM, alarm);
        startIntent.putExtra(GBDevice.EXTRA_DEVICE, getGbDevice());
        startActivityForResult(startIntent, REQ_CONFIGURE_ALARM);
    }

    private GBDevice getGbDevice() {
        return gbDevice;
    }

    private void sendAlarmsToDevice() {
        GBApplication.deviceService().onSetAlarms(mGBAlarmListAdapter.getAlarmList());
    }
}
