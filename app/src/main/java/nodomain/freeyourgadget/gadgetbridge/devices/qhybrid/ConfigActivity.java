package nodomain.freeyourgadget.gadgetbridge.devices.qhybrid;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport;

public class ConfigActivity extends AbstractGBActivity {
    PackageAdapter adapter;
    ArrayList<PackageConfig> list;
    PackageConfigHelper helper;

    final int REQUEST_CODE_ADD_APP = 0;

    private boolean hasControl = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qhybrid_settings);

        setTitle(R.string.preferences_qhybrid_settings);

        ListView appList = findViewById(R.id.qhybrid_appList);
        findViewById(R.id.qhybrid_addApp).setOnClickListener(view -> {
            startActivityForResult(new Intent(this, QHybridAppChoserActivity.class), REQUEST_CODE_ADD_APP);
        });

        helper = new PackageConfigHelper(getApplicationContext());
        list = helper.getSettings();
        appList.setAdapter(adapter = new PackageAdapter(this, R.layout.qhybrid_package_settings_item, list));
        appList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                PopupMenu menu = new PopupMenu(ConfigActivity.this, view);
                menu.getMenu().add("edit");
                menu.getMenu().add("delete");
                menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        switch (menuItem.getTitle().toString()){
                            case "edit":{
                                TimePicker picker = new TimePicker(ConfigActivity.this, list.get(i));
                                picker.finishListener = new TimePicker.OnFinishListener() {
                                    @Override
                                    public void onFinish(boolean success, PackageConfig config) {
                                        setControl(false);
                                        if(success){
                                            helper.saveConfig(config);
                                            refreshList();
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
                                break;
                            }
                            case "delete":{
                                helper.deleteConfig(list.get(i));
                                refreshList();
                                break;
                            }
                        }
                        return false;
                    }
                });
                menu.show();
                return false;
            }
        });
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

    private void refreshList(){
        list.clear();
        list.addAll(helper.getSettings());
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        helper.close();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshList();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

    }

    class PackageAdapter extends ArrayAdapter<PackageConfig>{
        PackageManager manager;

        public PackageAdapter(@NonNull Context context, int resource, @NonNull List<PackageConfig> objects) {
            super(context, resource, objects);
            manager = context.getPackageManager();
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View view, @NonNull ViewGroup parent) {
            if(view == null) view = ((LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.qhybrid_package_settings_item, null);
            PackageConfig settings = getItem(position);

            try {
                ((ImageView) view.findViewById(R.id.packageIcon)).setImageDrawable(manager.getApplicationIcon(settings.getPackageName()));
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            final int width = 100;
            ((TextView) view.findViewById(R.id.packageName)).setText(settings.getAppName());
            Bitmap bitmap = Bitmap.createBitmap(width, width, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bitmap);

            Paint black = new Paint();
            black.setColor(Color.BLACK);
            black.setStyle(Paint.Style.STROKE);
            black.setStrokeWidth(5);

            c.drawCircle(width / 2, width / 2, width / 2 - 3, black);

            int center = width / 2;
            if(settings.getHour() != -1){
                c.drawLine(
                        center,
                        center,
                        (float)(center + Math.sin(Math.toRadians(settings.getHour())) * (width / 4)),
                        (float)(center - Math.cos(Math.toRadians(settings.getHour())) * (width / 4)),
                        black
                        );
            }
            if(settings.getMin() != -1){
                c.drawLine(
                        center,
                        center,
                        (float)(center + Math.sin(Math.toRadians(settings.getMin())) * (width / 3)),
                        (float)(center - Math.cos(Math.toRadians(settings.getMin())) * (width / 3)),
                        black
                );
            }

            ((ImageView) view.findViewById(R.id.packageClock)).setImageBitmap(bitmap);

            return view;
        }
    }


}
