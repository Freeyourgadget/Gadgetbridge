package nodomain.freeyourgadget.gadgetbridge.devices.qhybrid;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.IBinder;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
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

    SharedPreferences prefs;

    TextView timeOffsetView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qhybrid_settings);

        Log.d("Config", "device: " + (getIntent().getParcelableExtra(GBDevice.EXTRA_DEVICE) == null));

        prefs = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        timeOffsetView = findViewById(R.id.qhybridTimeOffset);
        timeOffsetView.setOnClickListener(view -> {
            int timeOffset = prefs.getInt("QHYBRID_TIME_OFFSET", 0);
            LinearLayout layout2 = new LinearLayout(this);
            layout2.setOrientation(LinearLayout.HORIZONTAL);

            NumberPicker hourPicker = new NumberPicker(this);
            hourPicker.setMinValue(0);
            hourPicker.setMaxValue(23);
            hourPicker.setValue(timeOffset / 60);

            NumberPicker minPicker = new NumberPicker(this);
            minPicker.setMinValue(0);
            minPicker.setMaxValue(59);
            minPicker.setValue(timeOffset % 60);

            layout2.addView(hourPicker);
            TextView tw = new TextView(this);
            tw.setText(":");
            layout2.addView(tw);
            layout2.addView(minPicker);

            layout2.setGravity(Gravity.CENTER);

            new AlertDialog.Builder(this)
                    .setTitle("offset time by")
                    .setView(layout2)
                    .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            prefs.edit().putInt("QHYBRID_TIME_OFFSET", hourPicker.getValue() * 60 + minPicker.getValue()).apply();
                            updateTimeOffset();
                            LocalBroadcastManager.getInstance(ConfigActivity.this).sendBroadcast(new Intent(QHybridSupport.commandUpdate));
                            Toast.makeText(ConfigActivity.this, "change might take some seconds...", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("cancel", null)
                    .show();

        });
        updateTimeOffset();

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
                        switch (menuItem.getTitle().toString()) {
                            case "edit": {
                                TimePicker picker = new TimePicker(ConfigActivity.this, list.get(i));
                                picker.finishListener = new TimePicker.OnFinishListener() {
                                    @Override
                                    public void onFinish(boolean success, PackageConfig config) {
                                        setControl(false, null);
                                        if (success) {
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
                                setControl(true, picker.getSettings());
                                break;
                            }
                            case "delete": {
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
            int start;

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                start = seekBar.getProgress();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress;
                if ((progress = seekBar.getProgress()) == start) return;
                support.setVibrationStrength((int) Math.pow(2, progress) * 25);
                updateSettings();
            }
        });
    }

    private void updateTimeOffset() {
        int timeOffset = prefs.getInt("QHYBRID_TIME_OFFSET", 0);
        DecimalFormat format = new DecimalFormat("00");
        timeOffsetView.setText(
                format.format(timeOffset / 60) + ":" +
                        format.format(timeOffset % 60)
        );
    }

    private void setSettingsEnables(boolean enables) {
        findViewById(R.id.settingsLayout).setAlpha(enables ? 1f : 0.2f);
        findViewById(R.id.vibrationSettingProgressBar).setVisibility(enables ? View.GONE : View.VISIBLE);
    }

    private void updateSettings() {
        runOnUiThread(() -> setSettingsEnables(false));
        this.support.getGoal(goal -> runOnUiThread(() -> {
            EditText et = findViewById(R.id.stepGoalEt);
            et.setOnEditorActionListener(null);
            String text = String.valueOf(goal);
            et.setText(text);
            et.setSelection(text.length());
            et.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                    if (i == EditorInfo.IME_ACTION_DONE) {
                        Log.d("Settings", "enter");
                        String t = textView.getText().toString();
                        if (!t.equals(text)) {
                            support.setGoal(Integer.parseInt(t));
                            updateSettings();
                        }
                        ((InputMethodManager) getApplicationContext().getSystemService(Activity.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                    }
                    return true;
                }
            });
        }));
        this.support.getVibrationStrength(this);
    }

    private void setControl(boolean control, PackageConfig config) {
        if (hasControl == control) return;
        Intent intent = new Intent(control ? QHybridSupport.commandControl : QHybridSupport.commandUncontrol);
        intent.putExtra("CONFIG", config);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        this.hasControl = control;
    }

    private void setHands(PackageConfig config) {
        sendControl(config, QHybridSupport.commandSet);
    }

    private void vibrate(PackageConfig config) {
        sendControl(config, QHybridSupport.commandVibrate);
    }

    private void sendControl(PackageConfig config, String request) {
        Intent intent = new Intent(request);
        intent.putExtra("CONFIG", config);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void refreshList() {
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
        DeviceCommunicationService.CommunicationServiceBinder binder = (DeviceCommunicationService.CommunicationServiceBinder) iBinder;
        if (binder == null) {
            Log.d("Config", "Service not running");
            setSettingsError("Service not running");
            return;
        }
        DeviceSupport support = ((DeviceCommunicationService.CommunicationServiceBinder) iBinder).getDeviceSupport();
        if (!(support instanceof QHybridSupport)) {
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
        int strengthProgress = (int) (Math.log(strength / 25) / Math.log(2));
        Log.d("Config", "got strength: " + strength);
        runOnUiThread(() -> {
            setSettingsEnables(true);
            SeekBar seekBar = findViewById(R.id.vibrationStrengthBar);
            seekBar.setProgress(strengthProgress);
        });
    }

    private void setSettingsError(String error) {
        runOnUiThread(() -> {
            setSettingsEnables(false);
            findViewById(R.id.vibrationSettingProgressBar).setVisibility(View.GONE);
            ((TextView) findViewById(R.id.settingsErrorText)).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.settingsErrorText)).setText(error);
        });
    }

    class PackageAdapter extends ArrayAdapter<PackageConfig> {
        PackageManager manager;

        public PackageAdapter(@NonNull Context context, int resource, @NonNull List<PackageConfig> objects) {
            super(context, resource, objects);
            manager = context.getPackageManager();
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View view, @NonNull ViewGroup parent) {
            if (view == null)
                view = ((LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.qhybrid_package_settings_item, null);
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
            if (settings.getHour() != -1) {
                c.drawLine(
                        center,
                        center,
                        (float) (center + Math.sin(Math.toRadians(settings.getHour())) * (width / 4)),
                        (float) (center - Math.cos(Math.toRadians(settings.getHour())) * (width / 4)),
                        black
                );
            }
            if (settings.getMin() != -1) {
                c.drawLine(
                        center,
                        center,
                        (float) (center + Math.sin(Math.toRadians(settings.getMin())) * (width / 3)),
                        (float) (center - Math.cos(Math.toRadians(settings.getMin())) * (width / 3)),
                        black
                );
            }

            ((ImageView) view.findViewById(R.id.packageClock)).setImageBitmap(bitmap);

            return view;
        }
    }


}
