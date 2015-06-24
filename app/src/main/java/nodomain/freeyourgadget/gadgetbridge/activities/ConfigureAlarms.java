package nodomain.freeyourgadget.gadgetbridge.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.app.Activity;
import android.preference.PreferenceManager;
import android.widget.ListView;


import nodomain.freeyourgadget.gadgetbridge.BluetoothCommunicationService;
import nodomain.freeyourgadget.gadgetbridge.GBAlarm;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.adapter.GBAlarmListAdapter;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.PREF_MIBAND_ALARM1;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.PREF_MIBAND_ALARM2;
import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.PREF_MIBAND_ALARM3;


import java.util.ArrayList;
import java.util.List;


public class ConfigureAlarms extends Activity {

    ListView alarmListView;
    private GBAlarmListAdapter mGBAlarmListAdapter;

    final List<GBAlarm> alarmList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configure_alarms);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        //The GBAlarm class initializes the sharedPrefs values if they're missing, no need to handle it here
        alarmList.add(new GBAlarm(sharedPrefs.getString(PREF_MIBAND_ALARM1, GBAlarm.DEFAULT_ALARM1)));
        alarmList.add(new GBAlarm(sharedPrefs.getString(PREF_MIBAND_ALARM2, GBAlarm.DEFAULT_ALARM2)));
        alarmList.add(new GBAlarm(sharedPrefs.getString(PREF_MIBAND_ALARM3, GBAlarm.DEFAULT_ALARM3)));

        alarmListView = (ListView) findViewById(R.id.alarmListView);
        mGBAlarmListAdapter = new GBAlarmListAdapter(this, alarmList);
        alarmListView.setAdapter(this.mGBAlarmListAdapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent startIntent = new Intent(ConfigureAlarms.this, BluetoothCommunicationService.class);
        startIntent.setAction(BluetoothCommunicationService.ACTION_SET_ALARMS);
        startService(startIntent);
    }
}
