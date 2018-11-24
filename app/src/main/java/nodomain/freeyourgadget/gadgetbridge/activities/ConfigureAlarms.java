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

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.adapter.GBAlarmListAdapter;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst;
import nodomain.freeyourgadget.gadgetbridge.entities.Alarm;
import nodomain.freeyourgadget.gadgetbridge.entities.AlarmDao;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBAlarm;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;


public class ConfigureAlarms extends AbstractGBActivity {

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

    private void updateAlarmsFromDB() {
        Prefs prefs = GBApplication.getPrefs();
        int reservedSlots = prefs.getInt(MiBandConst.PREF_MIBAND_RESERVE_ALARM_FOR_CALENDAR, 0);

        DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(gbDevice);
        int alarmSlots = coordinator.getAlarmSlotCount();

        Long deviceId;
        List<Alarm> allAlarms = null;
        try (DBHandler db = GBApplication.acquireDB()) {
            AlarmDao alarmDao = db.getDaoSession().getAlarmDao();
            Device dbDevice = DBHelper.findDevice(gbDevice, db.getDaoSession());
            deviceId = dbDevice.getId();
            QueryBuilder<Alarm> qb = alarmDao.queryBuilder();
            qb.where(AlarmDao.Properties.DeviceId.eq(deviceId)).orderAsc(AlarmDao.Properties.Position).limit(alarmSlots - reservedSlots);
            allAlarms = qb.build().list();
        } catch (Exception e) {
            return;
        }

        List<GBAlarm> gbAlarms = new ArrayList<>();
        if (allAlarms != null) {
            for (Alarm alarm : allAlarms) {
                gbAlarms.add(new GBAlarm(deviceId, alarm.getPosition(), alarm.getEnabled(),
                        alarm.getSmartAlarm(), alarm.getRepetition(), alarm.getHour(), alarm.getMinute()));
            }
        }
        int hour = 5;
        while (gbAlarms.size() < alarmSlots) {
            GBAlarm gbAlarm = new GBAlarm(deviceId, gbAlarms.size(), false, false, 31, hour++, 30);
            gbAlarms.add(gbAlarm);
            gbAlarm.store();
        }

        mGBAlarmListAdapter.setAlarmList(gbAlarms);
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
