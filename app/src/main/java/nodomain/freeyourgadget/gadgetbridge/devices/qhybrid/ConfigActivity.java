package nodomain.freeyourgadget.gadgetbridge.devices.qhybrid;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceCommunicationService;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport;

public class ConfigActivity extends AbstractGBActivity implements ServiceConnection, QHybridSupport.OnVibrationStrengthListener {
    PackageAdapter adapter;
    ArrayList<PackageConfig> list;
    PackageConfigHelper helper;

    final int REQUEST_CODE_ADD_APP = 0;

    private boolean hasControl = false;

    QHybridSupport support;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qhybrid_settings);

        bindService(new Intent(getApplicationContext(), DeviceCommunicationService.class), this, 0);

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
        SeekBar vibeBar = findViewById(R.id.vibrationStrengthBar);
        vibeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {}

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int[] strength = {25, 50, 100};
                support.setVibrationStrength(strength[seekBar.getProgress()]);
                updateSettings();
            }
        });
    }

    private void setSettingsEnables(boolean enables){
        findViewById(R.id.settingsLayout).setAlpha(enables ? 1f : 0.2f);
        findViewById(R.id.vibrationSettingProgressBar).setVisibility(enables ? View.GONE : View.VISIBLE);
    }

    private void updateSettings(){
        runOnUiThread(() -> setSettingsEnables(false));
        this.support.getGoal(goal -> runOnUiThread(() -> {
            EditText et = findViewById(R.id.stepGoalEt);
            et.setOnEditorActionListener(null);
            et.setText(String.valueOf(goal));
            et.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                    if(i == EditorInfo.IME_ACTION_DONE){
                        Log.d("Settings", "enter");
                        support.setGoal(Long.parseLong(textView.getText().toString()));
                        ((InputMethodManager) getApplicationContext().getSystemService(Activity.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                        updateSettings();
                    }
                    return true;
                }
            });
        }));
        this.support.getVibrationStrength(this);
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
        unbindService(this);
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

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        Log.d("Config", "service connected");
        DeviceCommunicationService.CommunicationServiceBinder binder = (DeviceCommunicationService.CommunicationServiceBinder)iBinder;
        if(binder == null){
            Log.d("Config", "Service not running");
            setSettingsError("Service not running");
            return;
        }
        DeviceSupport support = ((DeviceCommunicationService.CommunicationServiceBinder)iBinder).getDeviceSupport();
        if(!(support instanceof QHybridSupport)){
            Log.d("Config", "Watch not connected");
            setSettingsError("Watch not connected");
            return;
        }
        this.support = (QHybridSupport) support;
        updateSettings();
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {

    }

    @Override
    public void onVibrationStrength(int strength) {
        int strengthProgress = strength == 100 ? 2 : strength == 50 ? 1 : 0;
        Log.d("Config", "got strength: " + strength);
        runOnUiThread(() -> {
            setSettingsEnables(true);
            SeekBar seekBar = findViewById(R.id.vibrationStrengthBar);
            seekBar.setProgress(strengthProgress);
        });
    }

    private void setSettingsError(String error){
        runOnUiThread(() -> {
            setSettingsEnables(false);
            findViewById(R.id.vibrationSettingProgressBar).setVisibility(View.GONE);
            ((TextView) findViewById(R.id.settingsErrorText)).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.settingsErrorText)).setText(error);
        });
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
