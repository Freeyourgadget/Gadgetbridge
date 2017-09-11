/*  Copyright (C) 2017 Daniele Gobbetti

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

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NavUtils;
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

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.util.GB;


public class CalBlacklistActivity extends AbstractGBActivity {

    private final String[] EVENT_PROJECTION = new String[]{
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR
    };
    private ArrayList<Calendar> calendarsArrayList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calblacklist);
        ListView calListView = (ListView) findViewById(R.id.calListView);

        final Uri uri = CalendarContract.Calendars.CONTENT_URI;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            GB.toast(this, "Calendar permission not granted. Nothing to do.", Toast.LENGTH_SHORT, GB.WARN);
            return;
        }
        try (Cursor cur = getContentResolver().query(uri, EVENT_PROJECTION, null, null, null)) {
            calendarsArrayList = new ArrayList<>();
            while (cur != null && cur.moveToNext()) {
                calendarsArrayList.add(new Calendar(cur.getString(0), cur.getInt(1)));
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
                    GBApplication.addCalendarToBlacklist(item.displayName);
                } else {
                    GBApplication.removeFromCalendarBlacklist(item.displayName);
                }
            }
        });


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
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
        private final int color;

        public Calendar(String displayName, int color) {
            this.displayName = displayName;
            this.color = color;
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
            CheckBox checked = (CheckBox) view.findViewById(R.id.item_checkbox);

            if (GBApplication.calendarIsBlacklisted(item.displayName) && !checked.isChecked()) {
                toggleEntry(view);
            }
            color.setBackgroundColor(item.color);
            name.setText(item.displayName);

            return view;
        }
    }
}
