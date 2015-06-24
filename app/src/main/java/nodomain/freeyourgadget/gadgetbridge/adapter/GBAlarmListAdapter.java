package nodomain.freeyourgadget.gadgetbridge.adapter;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import nodomain.freeyourgadget.gadgetbridge.GBAlarm;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AlarmDetails;


import java.util.List;

import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.PREF_MIBAND_ALARM1;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.PREF_MIBAND_ALARM2;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.PREF_MIBAND_ALARM3;


public class GBAlarmListAdapter extends ArrayAdapter<GBAlarm> {
    private final Context mContext;

    private List<GBAlarm> alarmList;

    public GBAlarmListAdapter(Context context, List<GBAlarm> alarmList) {
        super(context, 0, alarmList);

        this.mContext = context;
        this.alarmList = alarmList;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {

        final GBAlarm alarm = getItem(position);

        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.alarm_item, parent, false);
        }

        TextView alarmTime = (TextView) view.findViewById(R.id.alarm_item_time);
        Switch isEnabled = (Switch) view.findViewById(R.id.alarm_item_toggle);
        TextView isSmartWakeup = (TextView) view.findViewById(R.id.alarm_smart_wakeup);

        highlightDay((TextView) view.findViewById(R.id.alarm_item_sunday), alarm.getRepetition(GBAlarm.ALARM_SUN));
        highlightDay((TextView) view.findViewById(R.id.alarm_item_monday), alarm.getRepetition(GBAlarm.ALARM_MON));
        highlightDay((TextView) view.findViewById(R.id.alarm_item_tuesday), alarm.getRepetition(GBAlarm.ALARM_TUE));
        highlightDay((TextView) view.findViewById(R.id.alarm_item_wednesday), alarm.getRepetition(GBAlarm.ALARM_WED));
        highlightDay((TextView) view.findViewById(R.id.alarm_item_thursday), alarm.getRepetition(GBAlarm.ALARM_THU));
        highlightDay((TextView) view.findViewById(R.id.alarm_item_friday), alarm.getRepetition(GBAlarm.ALARM_FRI));
        highlightDay((TextView) view.findViewById(R.id.alarm_item_saturday), alarm.getRepetition(GBAlarm.ALARM_SAT));

        isEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                alarm.setEnabled(isChecked);
            }
        });

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent startIntent;
                startIntent = new Intent(mContext, AlarmDetails.class);
                startIntent.putExtra("alarm_index", alarm.getIndex());
                mContext.startActivity(startIntent);
            }
        });
        alarmTime.setText(alarm.getTime());
        isEnabled.setChecked(alarm.isEnabled());
        if(alarm.isSmartWakeup()) {
            isSmartWakeup.setVisibility(TextView.VISIBLE);
        } else {
            isSmartWakeup.setVisibility(TextView.GONE);
        }

        return view;
    }

    private void highlightDay(TextView view, boolean isOn) {
        if (isOn) {
            view.setTextColor(Color.BLUE);
        } else {
            view.setTextColor(Color.BLACK);
        }
    }
}
