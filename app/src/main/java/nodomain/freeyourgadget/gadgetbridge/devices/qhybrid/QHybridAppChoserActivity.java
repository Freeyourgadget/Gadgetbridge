package nodomain.freeyourgadget.gadgetbridge.devices.qhybrid;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport;

import static android.view.View.GONE;

public class QHybridAppChoserActivity extends AbstractGBActivity {
    boolean hasControl = false;

    PackageConfigHelper helper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qhybrid_app_choser);

        helper = new PackageConfigHelper(getApplicationContext());

        ListView appList = findViewById(R.id.qhybrid_appChooserList);
        PackageManager manager = getPackageManager();
        List<PackageInfo> packages = manager.getInstalledPackages(0);
        new Thread(() -> {
            Collections.sort(packages, (packageInfo, t1) -> manager.getApplicationLabel(packageInfo.applicationInfo).toString().compareToIgnoreCase(manager.getApplicationLabel(t1.applicationInfo).toString()));
            runOnUiThread(() -> {
                appList.setAdapter(new ConfigArrayAdapter(this, R.layout.qhybrid_app_view, packages, manager));
                findViewById(R.id.qhybrid_packageChooserLoading).setVisibility(GONE);
            });
        }).start();
        appList.setOnItemClickListener((adapterView, view, i, l) -> showPackageDialog(packages.get(i)));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        helper.close();
    }

    private void setControl(boolean control) {
        if (hasControl == control) return;
        Intent intent = new Intent(control ? QHybridSupport.commandControl : QHybridSupport.commandUncontrol);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        this.hasControl = control;
    }

    private void setHands(PackageConfig config){
        sendControl(config, QHybridSupport.commandSet);
    }

    private void vibrate(PackageConfig config){
        sendControl(config, QHybridSupport.commandVibrate);
    }

    private void sendControl(PackageConfig config, String request){
        Intent intent = new Intent(request);
        intent.putExtra("CONFIG", config);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void showPackageDialog(PackageInfo info) {
        TimePicker picker = new TimePicker(this, info);

        picker.finishListener = new TimePicker.OnFinishListener() {
            @Override
            public void onFinish(boolean success, PackageConfig config) {
                setControl(false);
                if(success){
                    helper.saveConfig(config);
                    finish();
                }
            }
        };

        picker.handsListener = new TimePicker.OnHandsSetListener() {
            @Override
            public void onHandsSet(PackageConfig config) {
                setHands(config);
            }
        };

        picker.vibrationListener = new TimePicker.OnVibrationSetListener() {
            @Override
            public void onVibrationSet(PackageConfig config) {
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
