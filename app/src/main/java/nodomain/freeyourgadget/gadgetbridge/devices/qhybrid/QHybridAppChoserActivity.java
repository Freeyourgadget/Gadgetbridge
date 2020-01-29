/*  Copyright (C) 2019-2020 Daniel Dakhno

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
package nodomain.freeyourgadget.gadgetbridge.devices.qhybrid;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

import static android.view.View.GONE;

public class QHybridAppChoserActivity extends AbstractGBActivity {
    boolean hasControl = false;

    private PackageConfigHelper helper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qhybrid_app_choser);

        helper = new PackageConfigHelper(getApplicationContext());

        final ListView appList = findViewById(R.id.qhybrid_appChooserList);
        final PackageManager manager = getPackageManager();
        final List<PackageInfo> packages = manager.getInstalledPackages(0);
        new Thread(new Runnable() {
            @Override
            public void run() {
                final IdentityHashMap<PackageInfo, String> nameMap = new IdentityHashMap(packages.size());
                for(PackageInfo info : packages){
                    CharSequence label = manager.getApplicationLabel(info.applicationInfo);
                    if(label == null) label = info.packageName;
                    nameMap.put(info, label.toString());
                }

                Collections.sort(packages, new Comparator<PackageInfo>() {
                    @Override
                    public int compare(PackageInfo packageInfo, PackageInfo t1) {
                        return nameMap.get(packageInfo)
                                .compareToIgnoreCase(
                                        nameMap.get(t1)
                                );
                    }
                });
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        appList.setAdapter(new ConfigArrayAdapter(QHybridAppChoserActivity.this, R.layout.qhybrid_app_view, packages, manager));
                        findViewById(R.id.qhybrid_packageChooserLoading).setVisibility(GONE);
                    }
                });
            }
        }).start();
        appList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                showPackageDialog(packages.get(i));
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void setControl(boolean control) {
        if (hasControl == control) return;
        Intent intent = new Intent(control ? QHybridSupport.QHYBRID_COMMAND_CONTROL : QHybridSupport.QHYBRID_COMMAND_UNCONTROL);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        this.hasControl = control;
    }

    private void setHands(NotificationConfiguration config){
        sendControl(config, QHybridSupport.QHYBRID_COMMAND_SET);
    }

    private void vibrate(NotificationConfiguration config){
        sendControl(config, QHybridSupport.QHYBRID_COMMAND_VIBRATE);
    }

    private void sendControl(NotificationConfiguration config, String request){
        Intent intent = new Intent(request);
        intent.putExtra("CONFIG", config);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void showPackageDialog(PackageInfo info) {
        TimePicker picker = new TimePicker(this, info);

        picker.finishListener = new TimePicker.OnFinishListener() {
            @Override
            public void onFinish(boolean success, NotificationConfiguration config) {
                setControl(false);
                if(success){
                    try {
                        helper.saveNotificationConfiguration(config);
                        LocalBroadcastManager.getInstance(QHybridAppChoserActivity.this).sendBroadcast(new Intent(QHybridSupport.QHYBRID_COMMAND_NOTIFICATION_CONFIG_CHANGED));
                    } catch (Exception e) {
                        GB.toast("error saving configuration", Toast.LENGTH_SHORT, GB.ERROR, e);
                    }
                    finish();
                }
            }
        };

        picker.handsListener = new TimePicker.OnHandsSetListener() {
            @Override
            public void onHandsSet(NotificationConfiguration config) {
                setHands(config);
            }
        };

        picker.vibrationListener = new TimePicker.OnVibrationSetListener() {
            @Override
            public void onVibrationSet(NotificationConfiguration config) {
                vibrate(config);
            }
        };

        setControl(true);
    }




    @Override
    protected void onPause() {
        super.onPause();
        setControl(false);
        finish();
    }

    class ConfigArrayAdapter extends ArrayAdapter<PackageInfo> {
        PackageManager manager;

        public ConfigArrayAdapter(@NonNull Context context, int resource, @NonNull List<PackageInfo> objects, PackageManager manager) {
            super(context, resource, objects);
            this.manager = manager;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View view, @NonNull ViewGroup parent) {
            if (view == null)
                view = ((LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.qhybrid_app_view, null);

            ApplicationInfo info = getItem(position).applicationInfo;
            ((ImageView) view.findViewById(R.id.qhybrid_appChooserItemIcon)).setImageDrawable(manager.getApplicationIcon(info));
            ((TextView) view.findViewById(R.id.qhybrid_appChooserItemText)).setText(manager.getApplicationLabel(info));

            return view;
        }
    }
}
