/*  Copyright (C) 2019 José Rebelo

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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.ConfigureReminders;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.entities.Reminder;

/**
 * Adapter for displaying Reminder instances.
 */
public class GBReminderListAdapter extends RecyclerView.Adapter<GBReminderListAdapter.ViewHolder> {

    private final Context mContext;
    private ArrayList<Reminder> reminderList;

    public GBReminderListAdapter(Context context) {
        this.mContext = context;
    }

    public void setReminderList(List<Reminder> reminders) {
        this.reminderList = new ArrayList<>(reminders);
    }

    public ArrayList<Reminder> getReminderList() {
        return reminderList;
    }

    private void updateInDB(Reminder reminder) {
        DBHelper.store(reminder);
    }

    @NonNull
    @Override
    public GBReminderListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reminder, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        final Reminder reminder = reminderList.get(position);

        holder.container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((ConfigureReminders) mContext).configureReminder(reminder);
            }
        });

        holder.reminderMessage.setText(reminder.getMessage());

        Date time = reminder.getDate();
        SimpleDateFormat format = new SimpleDateFormat("???", Locale.US);
        int stringResId = 0;

        switch(reminder.getRepetition()) {
            case Reminder.ONCE:
                stringResId = R.string.reminder_time_once;
                format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
                break;
            case Reminder.EVERY_DAY:
                stringResId = R.string.reminder_time_every_day;
                format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
                break;
            case Reminder.EVERY_WEEK:
                stringResId = R.string.reminder_time_every_week;
                format = new SimpleDateFormat("EEE HH:mm", Locale.US);
                break;
            case Reminder.EVERY_MONTH:
                stringResId = R.string.reminder_time_every_month;
                format = new SimpleDateFormat("dd HH:mm", Locale.US);
                break;
            case Reminder.EVERY_YEAR:
                stringResId = R.string.reminder_time_every_year;
                format = new SimpleDateFormat("MM-dd HH:mm", Locale.US);
                break;
        }

        final String reminderTimeText = mContext.getString(stringResId, format.format(time));
        holder.reminderTime.setText(reminderTimeText);
    }

    @Override
    public int getItemCount() {
        return reminderList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView container;

        TextView reminderTime;
        TextView reminderMessage;

        ViewHolder(View view) {
            super(view);

            container = view.findViewById(R.id.card_view);

            reminderTime = view.findViewById(R.id.reminder_item_time);
            reminderMessage = view.findViewById(R.id.reminder_item_message);
        }
    }

}
