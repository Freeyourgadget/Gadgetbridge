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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.adapter.GBAlarmListAdapter;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst;
import nodomain.freeyourgadget.gadgetbridge.impl.GBAlarm;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MIBAND_ALARMS;


public class ConfigureAlarms extends AbstractGBActivity {

    private static final int REQ_CONFIGURE_ALARM = 1;

    private GBAlarmListAdapter mGBAlarmListAdapter;
    private Set<String> preferencesAlarmListSet;
    private boolean avoidSendAlarmsToDevice;
    private GBDevice device;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_configure_alarms);

        device = getIntent().getParcelableExtra(GBDevice.EXTRA_DEVICE);

        Prefs prefs = GBApplication.getPrefs();
        preferencesAlarmListSet = prefs.getStringSet(PREF_MIBAND_ALARMS, new HashSet<String>());
        if (preferencesAlarmListSet.isEmpty()) {
            //initialize the preferences
            preferencesAlarmListSet = new HashSet<>(Arrays.asList(GBAlarm.DEFAULT_ALARMS));
            prefs.getPreferences().edit().putStringSet(PREF_MIBAND_ALARMS, preferencesAlarmListSet).apply();
        }

        mGBAlarmListAdapter = new GBAlarmListAdapter(this, preferencesAlarmListSet);

        RecyclerView alarmsRecyclerView = (RecyclerView) findViewById(R.id.alarm_list);
        alarmsRecyclerView.setHasFixedSize(true);
        alarmsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        alarmsRecyclerView.setAdapter(mGBAlarmListAdapter);
        updateAlarmsFromPrefs();
    }

    @Override
    protected void onPause() {
        if (!avoidSendAlarmsToDevice) {
            sendAlarmsToDevice();
        }
        super.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_CONFIGURE_ALARM) {
            avoidSendAlarmsToDevice = false;
            updateAlarmsFromPrefs();
        }
    }

    private void updateAlarmsFromPrefs() {
        Prefs prefs = GBApplication.getPrefs();
        preferencesAlarmListSet = prefs.getStringSet(PREF_MIBAND_ALARMS, new HashSet<String>());
        int reservedSlots = prefs.getInt(MiBandConst.PREF_MIBAND_RESERVE_ALARM_FOR_CALENDAR, 0);

        mGBAlarmListAdapter.setAlarmList(preferencesAlarmListSet, reservedSlots);
        mGBAlarmListAdapter.notifyDataSetChanged();
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

    public void configureAlarm(GBAlarm alarm) {
        avoidSendAlarmsToDevice = true;
        Intent startIntent = new Intent(getApplicationContext(), AlarmDetails.class);
        startIntent.putExtra("alarm", alarm);
        startIntent.putExtra(GBDevice.EXTRA_DEVICE, getDevice());
        startActivityForResult(startIntent, REQ_CONFIGURE_ALARM);
    }

    private GBDevice getDevice() {
        return device;
    }

    private void sendAlarmsToDevice() {
        GBApplication.deviceService().onSetAlarms(mGBAlarmListAdapter.getAlarmList());
    }
}
