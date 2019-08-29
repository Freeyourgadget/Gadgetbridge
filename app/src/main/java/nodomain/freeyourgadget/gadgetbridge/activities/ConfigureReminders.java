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

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.adapter.GBReminderListAdapter;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.Reminder;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;


public class ConfigureReminders extends AbstractGBActivity {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigureReminders.class);

    private static final int REQ_CONFIGURE_REMINDER = 1;

    private GBReminderListAdapter mGBReminderListAdapter;
    private boolean avoidSendRemindersToDevice;
    private GBDevice gbDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_configure_reminders);

        gbDevice = getIntent().getParcelableExtra(GBDevice.EXTRA_DEVICE);

        mGBReminderListAdapter = new GBReminderListAdapter(this);

        RecyclerView remindersRecyclerView = findViewById(R.id.reminder_list);
        remindersRecyclerView.setHasFixedSize(true);
        remindersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        remindersRecyclerView.setAdapter(mGBReminderListAdapter);
        updateRemindersFromDB();

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent startIntent = new Intent(getApplicationContext(), ReminderDetails.class);
                startIntent.putExtra(GBDevice.EXTRA_DEVICE, gbDevice);
                startActivityForResult(startIntent, REQ_CONFIGURE_REMINDER);
                configureReminder(new Reminder());
            }
        });
    }

    @Override
    protected void onPause() {
        if (!avoidSendRemindersToDevice && gbDevice.isInitialized()) {
            sendRemindersToDevice();
        }
        super.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_CONFIGURE_REMINDER && resultCode == 1) {
            // TODO handle result code
            avoidSendRemindersToDevice = false;
            updateRemindersFromDB();
        }
    }

    /**
     * Reads the available reminders from the database and updates the view afterwards.
     */
    private void updateRemindersFromDB() {
        List<Reminder> reminders = DBHelper.getReminders(gbDevice);
        if (reminders.size() == 0) {
            try (DBHandler db = GBApplication.acquireDB()) {
                DaoSession daoSession = db.getDaoSession();
                Device device = DBHelper.getDevice(gbDevice, daoSession);
                User user = DBHelper.getUser(daoSession);
                reminders = new ArrayList<>();
                reminders.add(createDefaultReminder(device, user, reminders.size() + 1));
            } catch (Exception e) {
                LOG.error("Error accessing database", e);
            }
        }

        mGBReminderListAdapter.setReminderList(reminders);
        mGBReminderListAdapter.notifyDataSetChanged();
    }

    private Reminder createDefaultReminder(@NonNull Device device, @NonNull User user, int position) {
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.MINUTE, 5);

        return new Reminder(device.getId(), user.getId(), position, "!", c.getTime(), nodomain.freeyourgadget.gadgetbridge.model.Reminder.ONCE);
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

    public void configureReminder(Reminder reminder) {
        avoidSendRemindersToDevice = true;
        Intent startIntent = new Intent(getApplicationContext(), ReminderDetails.class);
        startIntent.putExtra(Reminder.EXTRA_REMINDER, reminder);
        startActivityForResult(startIntent, REQ_CONFIGURE_REMINDER);
    }

    private void sendRemindersToDevice() {
        GBApplication.deviceService().onSetReminders(mGBReminderListAdapter.getReminderList());
    }
}
