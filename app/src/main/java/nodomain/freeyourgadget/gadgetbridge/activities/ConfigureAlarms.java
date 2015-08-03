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

import nodomain.freeyourgadget.gadgetbridge.service.BluetoothCommunicationService;
import nodomain.freeyourgadget.gadgetbridge.impl.GBAlarm;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.adapter.GBAlarmListAdapter;

import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MIBAND_ALARMS;


public class ConfigureAlarms extends ListActivity {

    private GBAlarmListAdapter mGBAlarmListAdapter;
    private Set<String> preferencesAlarmListSet;

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
            sharedPrefs.edit().putStringSet(PREF_MIBAND_ALARMS, preferencesAlarmListSet).commit();
        }

        mGBAlarmListAdapter = new GBAlarmListAdapter(this, preferencesAlarmListSet);

        setListAdapter(mGBAlarmListAdapter);

    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        preferencesAlarmListSet = sharedPrefs.getStringSet(PREF_MIBAND_ALARMS, new HashSet<String>());

        mGBAlarmListAdapter.setAlarmList(preferencesAlarmListSet);
        mGBAlarmListAdapter.notifyDataSetChanged();

        sendAlarmsToDevice();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // back button
                sendAlarmsToDevice();
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void configureAlarm(GBAlarm alarm) {
        Intent startIntent;
        startIntent = new Intent(getApplicationContext(), AlarmDetails.class);
        startIntent.putExtra("alarm", alarm);
        startActivity(startIntent);
    }

    private void sendAlarmsToDevice() {
        Intent startIntent = new Intent(ConfigureAlarms.this, BluetoothCommunicationService.class);
        startIntent.putParcelableArrayListExtra("alarms", mGBAlarmListAdapter.getAlarmList());
        startIntent.setAction(BluetoothCommunicationService.ACTION_SET_ALARMS);
        startService(startIntent);
    }
}
