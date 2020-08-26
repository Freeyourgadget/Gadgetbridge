/*  Copyright (C) 2019-2020 vanous

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

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.util.AlarmUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class WidgetAlarmsActivity extends Activity implements View.OnClickListener {

    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context appContext = this.getApplicationContext();
        if (appContext instanceof GBApplication) {
            GBApplication gbApp = (GBApplication) appContext;
            GBDevice selectedDevice = gbApp.getDeviceManager().getSelectedDevice();
            if (selectedDevice == null || !selectedDevice.isInitialized()) {
                GB.toast(this,
                        this.getString(R.string.not_connected),
                        Toast.LENGTH_LONG, GB.WARN);

            } else {
                setContentView(R.layout.widget_alarms_activity_list);
                int userSleepDuration = new ActivityUser().getSleepDuration();
                textView = findViewById(R.id.alarm5);
                if (userSleepDuration > 0) {
                    Resources res = getResources();
                    textView.setText(res.getQuantityString(R.plurals.widget_alarm_target_hours, userSleepDuration, userSleepDuration));
                } else {
                    textView.setVisibility(View.GONE);
                }
            }
        }
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.alarm1:
                setAlarm(5);
                break;
            case R.id.alarm2:
                setAlarm(10);
                break;
            case R.id.alarm3:
                setAlarm(20);
                break;
            case R.id.alarm4:
                setAlarm(60);
                break;
            case R.id.alarm5:
                setAlarm(0);
                break;
            default:
                break;
        }
        //this is to prevent screen flashing during closing
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, 150);
    }

    public void setAlarm(int duration) {
        // current timestamp
        GregorianCalendar calendar = new GregorianCalendar();
        if (duration > 0) {
            calendar.add(Calendar.MINUTE, duration);
        } else {
            int userSleepDuration = new ActivityUser().getSleepDuration();
            // add preferred sleep duration
            if (userSleepDuration > 0) {
                calendar.add(Calendar.HOUR_OF_DAY, userSleepDuration);
            } else { // probably testing
                calendar.add(Calendar.MINUTE, 1);
            }
        }

        // overwrite the first alarm and activate it, without

        Context appContext = this.getApplicationContext();
        if (appContext instanceof GBApplication) {
            GBApplication gbApp = (GBApplication) appContext;
            GBDevice selectedDevice = gbApp.getDeviceManager().getSelectedDevice();
            if (selectedDevice == null || !selectedDevice.isInitialized()) {
                GB.toast(this,
                        this.getString(R.string.appwidget_not_connected),
                        Toast.LENGTH_LONG, GB.WARN);
                return;
            }
        }

        int hours = calendar.get(Calendar.HOUR_OF_DAY);
        int minutes = calendar.get(Calendar.MINUTE);

        GB.toast(this,
                this.getString(R.string.appwidget_setting_alarm, hours, minutes),
                Toast.LENGTH_SHORT, GB.INFO);

        Alarm alarm = AlarmUtils.createSingleShot(0, true, false, calendar);
        ArrayList<Alarm> alarms = new ArrayList<>(1);
        alarms.add(alarm);
        GBApplication.deviceService().onSetAlarms(alarms);


    }
}
