/*  Copyright (C) 2015-2017 Andreas Shimokawa, Carsten Pfeiffer, Daniele
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

import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.TimePicker;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBAlarm;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;

public class AlarmDetails extends GBActivity {

    private GBAlarm alarm;
    private TimePicker timePicker;
    private CheckBox cbSmartWakeup;
    private CheckBox cbMonday;
    private CheckBox cbTuesday;
    private CheckBox cbWednesday;
    private CheckBox cbThursday;
    private CheckBox cbFriday;
    private CheckBox cbSaturday;
    private CheckBox cbSunday;
    private GBDevice device;
    private TextView smartAlarmLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_details);

        alarm = getIntent().getParcelableExtra("alarm");
        device = getIntent().getParcelableExtra(GBDevice.EXTRA_DEVICE);

        timePicker = (TimePicker) findViewById(R.id.alarm_time_picker);
        smartAlarmLabel = (TextView) findViewById(R.id.alarm_label_smart_wakeup);
        cbSmartWakeup = (CheckBox) findViewById(R.id.alarm_cb_smart_wakeup);
        cbMonday = (CheckBox) findViewById(R.id.alarm_cb_mon);
        cbTuesday = (CheckBox) findViewById(R.id.alarm_cb_tue);
        cbWednesday = (CheckBox) findViewById(R.id.alarm_cb_wed);
        cbThursday = (CheckBox) findViewById(R.id.alarm_cb_thu);
        cbFriday = (CheckBox) findViewById(R.id.alarm_cb_fri);
        cbSaturday = (CheckBox) findViewById(R.id.alarm_cb_sat);
        cbSunday = (CheckBox) findViewById(R.id.alarm_cb_sun);

        timePicker.setIs24HourView(DateFormat.is24HourFormat(GBApplication.getContext()));
        timePicker.setCurrentHour(alarm.getHour());
        timePicker.setCurrentMinute(alarm.getMinute());

        cbSmartWakeup.setChecked(alarm.isSmartWakeup());
        int smartAlarmVisibility = supportsSmartWakeup() ? View.VISIBLE : View.GONE;
        cbSmartWakeup.setVisibility(smartAlarmVisibility);
        smartAlarmLabel.setVisibility(smartAlarmVisibility);

        cbMonday.setChecked(alarm.getRepetition(GBAlarm.ALARM_MON));
        cbTuesday.setChecked(alarm.getRepetition(GBAlarm.ALARM_TUE));
        cbWednesday.setChecked(alarm.getRepetition(GBAlarm.ALARM_WED));
        cbThursday.setChecked(alarm.getRepetition(GBAlarm.ALARM_THU));
        cbFriday.setChecked(alarm.getRepetition(GBAlarm.ALARM_FRI));
        cbSaturday.setChecked(alarm.getRepetition(GBAlarm.ALARM_SAT));
        cbSunday.setChecked(alarm.getRepetition(GBAlarm.ALARM_SUN));

    }

    private boolean supportsSmartWakeup() {
        if (device != null) {
            DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(device);
            return coordinator.supportsSmartWakeup(device);
        }
        return false;
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

    private void updateAlarm() {
        alarm.setSmartWakeup(cbSmartWakeup.isChecked());
        alarm.setRepetition(cbMonday.isChecked(), cbTuesday.isChecked(), cbWednesday.isChecked(), cbThursday.isChecked(), cbFriday.isChecked(), cbSaturday.isChecked(), cbSunday.isChecked());
        alarm.setHour(timePicker.getCurrentHour());
        alarm.setMinute(timePicker.getCurrentMinute());
        alarm.store();
    }

    @Override
    protected void onPause() {
        updateAlarm();
        super.onPause();
    }
}
