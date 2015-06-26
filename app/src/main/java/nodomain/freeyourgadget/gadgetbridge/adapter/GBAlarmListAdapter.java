package nodomain.freeyourgadget.gadgetbridge.adapter;


import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBAlarm;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.ConfigureAlarms;


public class GBAlarmListAdapter extends ArrayAdapter<GBAlarm> {


    private final Context mContext;
    private ArrayList<GBAlarm> alarmList;

    public GBAlarmListAdapter(Context context, ArrayList<GBAlarm> alarmList) {
        super(context, 0, alarmList);

        this.mContext = context;
        this.alarmList = alarmList;
    }

    public GBAlarmListAdapter(Context context,Set<String> preferencesAlarmListSet) {
        super(context, 0, new ArrayList<GBAlarm>());

        this.mContext = context;
        alarmList = new ArrayList<GBAlarm>();

        if (preferencesAlarmListSet != null) {
            Iterator<String> iterator = preferencesAlarmListSet.iterator();

            while (iterator.hasNext()) {
                String alarmString = iterator.next();
                alarmList.add(new GBAlarm(alarmString));
            }
        }

        Collections.sort(alarmList);
    }

    public void setAlarmList(Set<String> preferencesAlarmListSet) {
        alarmList = new ArrayList<GBAlarm>();

        if (preferencesAlarmListSet != null) {
            Iterator<String> iterator = preferencesAlarmListSet.iterator();

            while (iterator.hasNext()) {
                String alarmString = iterator.next();
                alarmList.add(new GBAlarm(alarmString));
            }
        }

        Collections.sort(alarmList);
    }

    public ArrayList<GBAlarm> getAlarmList() {
        return alarmList;
    }


    public void update(GBAlarm alarm) {
        for (GBAlarm a : alarmList) {
            if(alarm.equals(a)) {
             a = alarm;
            }
        }
        alarm.store();
    }

    @Override
    public int getCount() {
        if (alarmList != null) {
            return alarmList.size();
        }
        return 0;
    }

    @Override
    public GBAlarm getItem(int position) {
        if (alarmList != null) {
            return alarmList.get(position);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        if (alarmList != null) {
            return alarmList.get(position).getIndex();
        }
        return 0;
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
                update(alarm);
            }
        });

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((ConfigureAlarms)mContext).configureAlarm(alarm);
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
