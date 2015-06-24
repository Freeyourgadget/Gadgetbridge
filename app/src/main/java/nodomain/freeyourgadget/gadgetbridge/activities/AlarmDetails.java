package nodomain.freeyourgadget.gadgetbridge.activities;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.CheckedTextView;
import android.widget.TimePicker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.GBAlarm;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;

import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.PREF_MIBAND_ALARM_PREFIX;

public class AlarmDetails extends Activity {

    private static final Logger LOG = LoggerFactory.getLogger(AlarmDetails.class);


    private GBAlarm alarm;
    private TimePicker timePicker;
    //using CheckedTextView allows for vertically aligned text
    private CheckedTextView ctvSmartWakeup;
    private CheckedTextView ctvMonday;
    private CheckedTextView ctvTuesday;
    private CheckedTextView ctvWednesday;
    private CheckedTextView ctvThursday;
    private CheckedTextView ctvFriday;
    private CheckedTextView ctvSaturday;
    private CheckedTextView ctvSunday;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_details);

        int index = getIntent().getExtras().getInt("alarm_index");
        if (index <0 || index > 2) {
            finish();
        }else {
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
            String pref = PREF_MIBAND_ALARM_PREFIX +(index+1);
            alarm = new GBAlarm(sharedPrefs.getString(pref, ""));
            //TODO: this is horrible and error-prone

            timePicker = (TimePicker) findViewById(R.id.alarm_time_picker);
            ctvSmartWakeup = (CheckedTextView) findViewById(R.id.alarm_ctv_smart_wakeup);
            ctvMonday = (CheckedTextView) findViewById(R.id.alarm_ctv_mon);
            ctvTuesday = (CheckedTextView) findViewById(R.id.alarm_ctv_tue);
            ctvWednesday = (CheckedTextView) findViewById(R.id.alarm_ctv_wed);
            ctvThursday = (CheckedTextView) findViewById(R.id.alarm_ctv_thu);
            ctvFriday = (CheckedTextView) findViewById(R.id.alarm_ctv_fri);
            ctvSaturday = (CheckedTextView) findViewById(R.id.alarm_ctv_sat);
            ctvSunday = (CheckedTextView) findViewById(R.id.alarm_ctv_sun);

            timePicker.setIs24HourView(DateFormat.is24HourFormat(GBApplication.getContext()));
            timePicker.setCurrentHour(alarm.getHour());
            timePicker.setCurrentMinute(alarm.getMinute());

            ctvSmartWakeup.setChecked(alarm.isSmartWakeup());
            ctvSmartWakeup.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((CheckedTextView) v).toggle();
                }
            });

            ctvMonday.setChecked(alarm.getRepetition(GBAlarm.ALARM_MON));
            ctvTuesday.setChecked(alarm.getRepetition(GBAlarm.ALARM_TUE));
            ctvWednesday.setChecked(alarm.getRepetition(GBAlarm.ALARM_WED));
            ctvThursday.setChecked(alarm.getRepetition(GBAlarm.ALARM_THU));
            ctvFriday.setChecked(alarm.getRepetition(GBAlarm.ALARM_FRI));
            ctvSaturday.setChecked(alarm.getRepetition(GBAlarm.ALARM_SAT));
            ctvSunday.setChecked(alarm.getRepetition(GBAlarm.ALARM_SUN));

            ctvMonday.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((CheckedTextView) v).toggle();
                }
            });
            ctvTuesday.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((CheckedTextView) v).toggle();
                }
            });
            ctvWednesday.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((CheckedTextView) v).toggle();
                }
            });
            ctvThursday.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((CheckedTextView) v).toggle();
                }
            });
            ctvFriday.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((CheckedTextView) v).toggle();
                }
            });
            ctvSaturday.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((CheckedTextView) v).toggle();
                }
            });
            ctvSunday.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((CheckedTextView) v).toggle();
                }
            });

        }

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        alarm.setSmartWakeup(ctvSmartWakeup.isChecked());
        alarm.setRepetition(ctvMonday.isChecked(),ctvTuesday.isChecked(),ctvWednesday.isChecked(),ctvThursday.isChecked(),ctvFriday.isChecked(),ctvSaturday.isChecked(),ctvSunday.isChecked());
        alarm.setHour(timePicker.getCurrentHour());
        alarm.setMinute(timePicker.getCurrentMinute());
    }
}
