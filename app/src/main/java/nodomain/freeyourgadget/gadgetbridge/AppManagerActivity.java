package nodomain.freeyourgadget.gadgetbridge;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.adapter.GBDeviceAppAdapter;


public class AppManagerActivity extends Activity {
    private final String TAG = this.getClass().getSimpleName();

    public static final String ACTION_REFRESH_APPLIST
            = "nodomain.freeyourgadget.gadgetbride.appmanager.action.refresh_applist";

    ListView appListView;
    GBDeviceAppAdapter mGBDeviceAppAdapter;
    final List<GBDeviceApp> appList = new ArrayList<>();

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ControlCenter.ACTION_QUIT)) {
                finish();
            } else if (action.equals(ACTION_REFRESH_APPLIST)) {
                int appCount = intent.getIntExtra("app_count", 0);
                for (Integer i = 0; i < appCount; i++) {
                    String appName = intent.getStringExtra("app_name" + i.toString());
                    String appCreator = intent.getStringExtra("app_creator" + i.toString());
                    appList.add(new GBDeviceApp(appName, appCreator, ""));
                }
                mGBDeviceAppAdapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appmanager);

        appListView = (ListView) findViewById(R.id.appListView);
        mGBDeviceAppAdapter = new GBDeviceAppAdapter(this, appList);
        appListView.setAdapter(this.mGBDeviceAppAdapter);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ControlCenter.ACTION_QUIT);
        filter.addAction(ACTION_REFRESH_APPLIST);
        registerReceiver(mReceiver, filter);

        Intent startIntent = new Intent(this, BluetoothCommunicationService.class);
        startIntent.setAction(BluetoothCommunicationService.ACTION_REQUEST_APPINFO);
        startService(startIntent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }
}
