/*  Copyright (C) 2015-2018 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti

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
package nodomain.freeyourgadget.gadgetbridge.adapter;


import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.ConfigureAlarms;
import nodomain.freeyourgadget.gadgetbridge.impl.GBAlarm;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;

/**
 * Adapter for displaying GBAlarm instances.
 */
public class GBAlarmListAdapter extends RecyclerView.Adapter<GBAlarmListAdapter.ViewHolder> {


    private final Context mContext;
    private List<GBAlarm> alarmList;

    public GBAlarmListAdapter(Context context) {
        this.mContext = context;
    }

    public void setAlarmList(List<GBAlarm> alarmList) {
        this.alarmList = alarmList;
    }

    public ArrayList getAlarmList() {
        return (ArrayList) alarmList;
    }


    public void update(GBAlarm alarm) {
        alarm.store();
    }

    @NonNull
    @Override
    public GBAlarmListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_alarm, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {

        final GBAlarm alarm = alarmList.get(position);

        holder.alarmDayMonday.setChecked(alarm.getRepetition(Alarm.ALARM_MON));
        holder.alarmDayTuesday.setChecked(alarm.getRepetition(Alarm.ALARM_TUE));
        holder.alarmDayWednesday.setChecked(alarm.getRepetition(Alarm.ALARM_WED));
        holder.alarmDayThursday.setChecked(alarm.getRepetition(Alarm.ALARM_THU));
        holder.alarmDayFriday.setChecked(alarm.getRepetition(Alarm.ALARM_FRI));
        holder.alarmDaySaturday.setChecked(alarm.getRepetition(Alarm.ALARM_SAT));
        holder.alarmDaySunday.setChecked(alarm.getRepetition(Alarm.ALARM_SUN));

        holder.isEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                alarm.setEnabled(isChecked);
                update(alarm);
            }
        });

        holder.container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((ConfigureAlarms) mContext).configureAlarm(alarm);
            }
        });
        holder.alarmTime.setText(alarm.getTime());
        holder.isEnabled.setChecked(alarm.isEnabled());
        if (alarm.isSmartWakeup()) {
            holder.isSmartWakeup.setVisibility(TextView.VISIBLE);
        } else {
            holder.isSmartWakeup.setVisibility(TextView.GONE);
        }
    }


    @Override
    public int getItemCount() {
        return alarmList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        CardView container;

        TextView alarmTime;
        Switch isEnabled;
        TextView isSmartWakeup;

        CheckedTextView alarmDayMonday;
        CheckedTextView alarmDayTuesday;
        CheckedTextView alarmDayWednesday;
        CheckedTextView alarmDayThursday;
        CheckedTextView alarmDayFriday;
        CheckedTextView alarmDaySaturday;
        CheckedTextView alarmDaySunday;

        ViewHolder(View view) {
            super(view);

            container = view.findViewById(R.id.card_view);

            alarmTime = view.findViewById(R.id.alarm_item_time);
            isEnabled = view.findViewById(R.id.alarm_item_toggle);
            isSmartWakeup = view.findViewById(R.id.alarm_smart_wakeup);

            alarmDayMonday = view.findViewById(R.id.alarm_item_monday);
            alarmDayTuesday = view.findViewById(R.id.alarm_item_tuesday);
            alarmDayWednesday = view.findViewById(R.id.alarm_item_wednesday);
            alarmDayThursday = view.findViewById(R.id.alarm_item_thursday);
            alarmDayFriday = view.findViewById(R.id.alarm_item_friday);
            alarmDaySaturday = view.findViewById(R.id.alarm_item_saturday);
            alarmDaySunday = view.findViewById(R.id.alarm_item_sunday);
        }
    }

}
