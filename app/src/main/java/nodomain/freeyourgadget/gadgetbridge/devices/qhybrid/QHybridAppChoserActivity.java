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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

import static android.view.View.GONE;

public class QHybridAppChoserActivity extends AbstractGBActivity {
    boolean hasControl = false;

    PackageConfigHelper helper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qhybrid_app_choser);

        try {
            helper = new PackageConfigHelper(getApplicationContext());
        } catch (GBException e) {
            e.printStackTrace();
            GB.toast("error getting database helper", Toast.LENGTH_SHORT, GB.ERROR, e);
            finish();
            return;
        }

        final ListView appList = findViewById(R.id.qhybrid_appChooserList);
        final PackageManager manager = getPackageManager();
        final List<PackageInfo> packages = manager.getInstalledPackages(0);
        new Thread(new Runnable() {
            @Override
            public void run() {
                Collections.sort(packages, new Comparator<PackageInfo>() {
                    @Override
                    public int compare(PackageInfo packageInfo, PackageInfo t1) {
                        return manager.getApplicationLabel(packageInfo.applicationInfo)
                                .toString()
                                .compareToIgnoreCase(
                                        manager.getApplicationLabel(t1.applicationInfo)
                                                .toString()
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
                    } catch (GBException e) {
                        e.printStackTrace();
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
