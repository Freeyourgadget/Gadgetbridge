package nodomain.freeyourgadget.gadgetbridge.activities;

import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.adapter.GBAlarmListAdapter;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst;
import nodomain.freeyourgadget.gadgetbridge.impl.GBAlarm;

import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MIBAND_ALARMS;


public class ConfigureAlarms extends ListActivity {

    private static final int REQ_CONFIGURE_ALARM = 1;

    private GBAlarmListAdapter mGBAlarmListAdapter;
    private Set<String> preferencesAlarmListSet;
    private boolean avoidSendAlarmsToDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_configure_alarms);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        preferencesAlarmListSet = sharedPrefs.getStringSet(PREF_MIBAND_ALARMS, new HashSet<String>());
        if (preferencesAlarmListSet.isEmpty()) {
            //initialize the preferences
            preferencesAlarmListSet = new HashSet<>(Arrays.asList(GBAlarm.DEFAULT_ALARMS));
            sharedPrefs.edit().putStringSet(PREF_MIBAND_ALARMS, preferencesAlarmListSet).apply();
        }

        mGBAlarmListAdapter = new GBAlarmListAdapter(this, preferencesAlarmListSet);

        setListAdapter(mGBAlarmListAdapter);
        updateAlarmsFromPrefs();
    }

    @Override
    protected void onPause() {
        if (!avoidSendAlarmsToDevice) {
            sendAlarmsToDevice();
        }
        super.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_CONFIGURE_ALARM) {
            avoidSendAlarmsToDevice = false;
            updateAlarmsFromPrefs();
        }
    }

    private void updateAlarmsFromPrefs() {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        preferencesAlarmListSet = sharedPrefs.getStringSet(PREF_MIBAND_ALARMS, new HashSet<String>());
        int reservedSlots = Integer.parseInt(sharedPrefs.getString(MiBandConst.PREF_MIBAND_RESERVE_ALARM_FOR_CALENDAR, "0"));

        mGBAlarmListAdapter.setAlarmList(preferencesAlarmListSet, reservedSlots);
        mGBAlarmListAdapter.notifyDataSetChanged();
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

    public void configureAlarm(GBAlarm alarm) {
        avoidSendAlarmsToDevice = true;
        Intent startIntent;
        startIntent = new Intent(getApplicationContext(), AlarmDetails.class);
        startIntent.putExtra("alarm", alarm);
        startActivityForResult(startIntent, REQ_CONFIGURE_ALARM);
    }

    private void sendAlarmsToDevice() {
        GBApplication.deviceService().onSetAlarms(mGBAlarmListAdapter.getAlarmList());
    }
}
