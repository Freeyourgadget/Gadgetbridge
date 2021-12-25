package nodomain.freeyourgadget.gadgetbridge.devices.qhybrid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.PopupMenuCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class AppsManagementActivity extends AbstractGBActivity {
    ListView appsListView;
    String[] appNames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qhybrid_apps_management);

        initViews();
        refreshInstalledApps();
    }

    private void toast(String data) {
        GB.toast(data, Toast.LENGTH_LONG, GB.INFO);
    }

    private void refreshInstalledApps() {
        try {
            GBDevice selected = GBApplication.app().getDeviceManager().getSelectedDevice();
            if (selected.getType() != DeviceType.FOSSILQHYBRID || !selected.isConnected() || !selected.getModel().startsWith("DN") || selected.getState() != GBDevice.State.INITIALIZED) {
                throw new RuntimeException("Device not connected");
            }
            String installedAppsJson = selected.getDeviceInfo("INSTALLED_APPS").getDetails();
            if (installedAppsJson == null || installedAppsJson.isEmpty()) {
                throw new RuntimeException("cant get installed apps");
            }
            JSONArray apps = new JSONArray(installedAppsJson);
            appNames = new String[apps.length()];
            for (int i = 0; i < apps.length(); i++) {
                appNames[i] = apps.getString(i);
            }
            appsListView.setAdapter(new AppsListAdapter(this, appNames));
        } catch (Exception e) {
            toast(e.getMessage());
            finish();
            return;
        }
    }

    class AppsListAdapter extends ArrayAdapter<String> {
        public AppsListAdapter(@NonNull Context context, @NonNull String[] objects) {
            super(context, 0, objects);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater.from(getContext()));
                convertView = inflater.inflate(R.layout.fossil_hr_row_installed_app, null);
            }
            TextView nameView = convertView.findViewById(R.id.fossil_hr_row_app_name);
            nameView.setText(getItem(position));
            return nameView;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(deviceUpdateReceiver);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(deviceUpdateReceiver, new IntentFilter(GBDevice.ACTION_DEVICE_CHANGED));
    }

    BroadcastReceiver deviceUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshInstalledApps();
        }
    };

    private void initViews() {
        appsListView = findViewById(R.id.qhybrid_apps_list);
        appsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                PopupMenu menu = new PopupMenu(AppsManagementActivity.this, view);
                menu.getMenu()
                        .add("uninstall")
                        .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                Intent intent = new Intent(QHybridSupport.QHYBRID_COMMAND_UNINSTALL_APP);
                                intent.putExtra("EXTRA_APP_NAME", appNames[position]);
                                LocalBroadcastManager.getInstance(AppsManagementActivity.this).sendBroadcast(intent);
                                return true;
                            }
                        });
                menu.show();
            }
        });
    }
}
