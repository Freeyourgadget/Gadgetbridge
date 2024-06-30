/*  Copyright (C) 2017-2024 Carsten Pfeiffer, Daniele Gobbetti, José Rebelo,
    Ludovic Jozeau

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.activities;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NavUtils;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.calendar.CalendarManager;


public class CalBlacklistActivity extends AbstractGBActivity {

    private final String[] EVENT_PROJECTION = new String[]{
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR
    };
    private ArrayList<Calendar> calendarsArrayList;

    private GBDevice gbDevice;
    private CalendarManager calendarManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calblacklist);
        ListView calListView = (ListView) findViewById(R.id.calListView);

        gbDevice = getIntent().getParcelableExtra(GBDevice.EXTRA_DEVICE);
        calendarManager = new CalendarManager(this, gbDevice.getAddress());

        final Uri uri = CalendarContract.Calendars.CONTENT_URI;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            GB.toast(this, "Calendar permission not granted. Nothing to do.", Toast.LENGTH_SHORT, GB.WARN);
            return;
        }
        try (Cursor cur = getContentResolver().query(uri, EVENT_PROJECTION, null, null, null)) {
            calendarsArrayList = new ArrayList<>();
            while (cur != null && cur.moveToNext()) {
                calendarsArrayList.add(new Calendar(cur.getString(0), cur.getString(1), cur.getInt(2)));
            }
        }

        ArrayAdapter<Calendar> calAdapter = new CalendarListAdapter(this, calendarsArrayList);
        calListView.setAdapter(calAdapter);
        calListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int i, long id) {
                Calendar item = calendarsArrayList.get(i);
                CheckBox selected = (CheckBox) view.findViewById(R.id.item_checkbox);
                toggleEntry(view);
                if (selected.isChecked()) {
                    calendarManager.addCalendarToBlacklist(item.getUniqueString());
                } else {
                    calendarManager.removeFromCalendarBlacklist(item.getUniqueString());
                }
            }
        });


    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleEntry(View view) {
        TextView name = (TextView) view.findViewById(R.id.calendar_name);
        CheckBox checked = (CheckBox) view.findViewById(R.id.item_checkbox);

        name.setPaintFlags(name.getPaintFlags() ^ Paint.STRIKE_THRU_TEXT_FLAG);
        checked.toggle();
    }

    class Calendar {
        private final String displayName;
        private final String accountName;
        private final int color;

        public Calendar(String displayName, String accountName, int color) {
            this.displayName = displayName;
            this.accountName = accountName;
            this.color = color;
        }

        public String getUniqueString() {
            return accountName + '/' + displayName;
        }
    }

    private class CalendarListAdapter extends ArrayAdapter<Calendar> {

        CalendarListAdapter(@NonNull Context context, @NonNull List<Calendar> calendars) {
            super(context, 0, calendars);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View view, @NonNull ViewGroup parent) {
            Calendar item = getItem(position);

            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) super.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.item_cal_blacklist, parent, false);
            }

            View color = view.findViewById(R.id.calendar_color);
            TextView name = (TextView) view.findViewById(R.id.calendar_name);
            TextView ownerAccount = (TextView) view.findViewById(R.id.calendar_owner_account);
            CheckBox checked = (CheckBox) view.findViewById(R.id.item_checkbox);

            if (calendarManager.calendarIsBlacklisted(item.getUniqueString()) && !checked.isChecked() ||
                    !calendarManager.calendarIsBlacklisted(item.getUniqueString()) && checked.isChecked()) {
                toggleEntry(view);
            }
            color.setBackgroundColor(item.color);
            name.setText(item.displayName);
            ownerAccount.setText(item.accountName);

            return view;
        }
    }
}
