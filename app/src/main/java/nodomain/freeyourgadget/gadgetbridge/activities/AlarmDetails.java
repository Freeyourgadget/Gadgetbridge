package nodomain.freeyourgadget.gadgetbridge.activities;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateFormat;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.TimePicker;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBAlarm;

public class AlarmDetails extends AppCompatActivity {

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_details);

        Parcelable p = getIntent().getExtras().getParcelable("alarm");
        alarm = (GBAlarm) p;

        timePicker = (TimePicker) findViewById(R.id.alarm_time_picker);
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

        cbMonday.setChecked(alarm.getRepetition(GBAlarm.ALARM_MON));
        cbTuesday.setChecked(alarm.getRepetition(GBAlarm.ALARM_TUE));
        cbWednesday.setChecked(alarm.getRepetition(GBAlarm.ALARM_WED));
        cbThursday.setChecked(alarm.getRepetition(GBAlarm.ALARM_THU));
        cbFriday.setChecked(alarm.getRepetition(GBAlarm.ALARM_FRI));
        cbSaturday.setChecked(alarm.getRepetition(GBAlarm.ALARM_SAT));
        cbSunday.setChecked(alarm.getRepetition(GBAlarm.ALARM_SUN));

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
