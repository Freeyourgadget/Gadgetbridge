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
package nodomain.freeyourgadget.gadgetbridge.activities;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.entities.Reminder;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class ReminderDetails extends AbstractGBActivity implements TimePickerDialog.OnTimeSetListener, DatePickerDialog.OnDateSetListener {

    private Reminder reminder;
    private GBDevice device;

    ArrayAdapter<String> repeatAdapter;

    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.US);

    TextView reminderRepeat;
    TextView reminderDate;
    TextView reminderTime;
    EditText reminderText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder_details);

        reminder = (Reminder) getIntent().getSerializableExtra(Reminder.EXTRA_REMINDER);

        if (reminder == null) {
            reminder = new Reminder();
        }

        reminderRepeat = findViewById(R.id.reminder_repeat);
        reminderDate = findViewById(R.id.reminder_date);
        reminderTime = findViewById(R.id.reminder_time);
        reminderText = findViewById(R.id.reminder_message);

        device = getIntent().getParcelableExtra(GBDevice.EXTRA_DEVICE);
        String[] repeatStrings = getResources().getStringArray(R.array.reminder_repeat);
        repeatAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, repeatStrings);

        View cardRepeat = findViewById(R.id.card_repeat);
        cardRepeat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(ReminderDetails.this).setAdapter(repeatAdapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        reminder.setRepetition(i);
                        updateUiFromReminder();
                    }
                }).create().show();
            }
        });

        View cardDate = findViewById(R.id.card_date);
        cardDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new DatePickerDialog(
                        ReminderDetails.this,
                        ReminderDetails.this,
                        reminder.getDate().getYear(),
                        reminder.getDate().getMonth(),
                        reminder.getDate().getDate()
                ).show();
            }
        });

        View cardTime = findViewById(R.id.card_time);
        cardTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new TimePickerDialog(
                        ReminderDetails.this,
                        ReminderDetails.this,
                        reminder.getDate().getHours(),
                        reminder.getDate().getMinutes(),
                        DateFormat.is24HourFormat(GBApplication.getContext())
                ).show();
            }
        });

        FloatingActionButton fab = findViewById(R.id.fab_save);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateReminder();
                ReminderDetails.this.setResult(1);
                finish();
            }
        });

        updateUiFromReminder();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // back button
                // TODO confirm when exiting without saving
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateReminder() {
        DBHelper.store(reminder);
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putSerializable("reminder", reminder);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        reminder = (Reminder) savedInstanceState.getSerializable("reminder");
        updateUiFromReminder();
    }

    @Override
    public void onTimeSet(TimePicker timePicker, int hour, int minute) {
        reminder.getDate().setHours(hour);
        reminder.getDate().setMinutes(minute);
        updateUiFromReminder();
    }

    @Override
    public void onDateSet(DatePicker datePicker, int year, int month, int dayOfMonth) {
        reminder.getDate().setYear(year);
        reminder.getDate().setMonth(month);
        reminder.getDate().setDate(dayOfMonth);
        updateUiFromReminder();
    }

    public void updateUiFromReminder() {
        reminderRepeat.setText(repeatAdapter.getItem(reminder.getRepetition()));
        reminderText.setText(reminder.getMessage());

        reminderDate.setText(dateFormat.format(reminder.getDate()));
        reminderTime.setText(timeFormat.format(reminder.getDate()));
    }
}
