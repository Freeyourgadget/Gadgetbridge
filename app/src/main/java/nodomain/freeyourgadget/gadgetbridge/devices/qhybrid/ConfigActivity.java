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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.model.GenericItem;
import nodomain.freeyourgadget.gadgetbridge.model.ItemWithDetails;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.buttonconfig.ConfigPayload;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class ConfigActivity extends AbstractGBActivity {
    PackageAdapter adapter;
    ArrayList<NotificationConfiguration> list;
    PackageConfigHelper helper;

    final int REQUEST_CODE_ADD_APP = 0;

    private boolean hasControl = false;

    SharedPreferences prefs;

    TextView timeOffsetView, timezoneOffsetView;

    GBDevice device;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_qhybrid_settings);

        findViewById(R.id.buttonOverwriteButtons).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LocalBroadcastManager.getInstance(ConfigActivity.this).sendBroadcast(new Intent(QHybridSupport.QHYBRID_COMMAND_OVERWRITE_BUTTONS));
            }
        });

        prefs = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        timeOffsetView = findViewById(R.id.qhybridTimeOffset);
        timeOffsetView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int timeOffset = prefs.getInt("QHYBRID_TIME_OFFSET", 0);
                LinearLayout layout2 = new LinearLayout(ConfigActivity.this);
                layout2.setOrientation(LinearLayout.HORIZONTAL);

                final NumberPicker hourPicker = new NumberPicker(ConfigActivity.this);
                hourPicker.setMinValue(0);
                hourPicker.setMaxValue(23);
                hourPicker.setValue(timeOffset / 60);

                final NumberPicker minPicker = new NumberPicker(ConfigActivity.this);
                minPicker.setMinValue(0);
                minPicker.setMaxValue(59);
                minPicker.setValue(timeOffset % 60);

                layout2.addView(hourPicker);
                TextView tw = new TextView(ConfigActivity.this);
                tw.setText(":");
                layout2.addView(tw);
                layout2.addView(minPicker);

                layout2.setGravity(Gravity.CENTER);

                new AlertDialog.Builder(ConfigActivity.this)
                        .setTitle(getString(R.string.qhybrid_offset_time_by))
                        .setView(layout2)
                        .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                prefs.edit().putInt("QHYBRID_TIME_OFFSET", hourPicker.getValue() * 60 + minPicker.getValue()).apply();
                                updateTimeOffset();
                                LocalBroadcastManager.getInstance(ConfigActivity.this).sendBroadcast(new Intent(QHybridSupport.QHYBRID_COMMAND_UPDATE));
                                GB.toast(getString(R.string.qhybrid_changes_delay_prompt), Toast.LENGTH_SHORT, GB.INFO);
                            }
                        })
                        .setNegativeButton("cancel", null)
                        .show();
            }
        });
        updateTimeOffset();


        timezoneOffsetView = findViewById(R.id.timezoneOffset);
        timezoneOffsetView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int timeOffset = prefs.getInt("QHYBRID_TIMEZONE_OFFSET", 0);
                LinearLayout layout2 = new LinearLayout(ConfigActivity.this);
                layout2.setOrientation(LinearLayout.HORIZONTAL);

                final NumberPicker hourPicker = new NumberPicker(ConfigActivity.this);
                hourPicker.setMinValue(0);
                hourPicker.setMaxValue(23);
                hourPicker.setValue(timeOffset / 60);

                final NumberPicker minPicker = new NumberPicker(ConfigActivity.this);
                minPicker.setMinValue(0);
                minPicker.setMaxValue(59);
                minPicker.setValue(timeOffset % 60);

                layout2.addView(hourPicker);
                TextView tw = new TextView(ConfigActivity.this);
                tw.setText(":");
                layout2.addView(tw);
                layout2.addView(minPicker);

                layout2.setGravity(Gravity.CENTER);

                new AlertDialog.Builder(ConfigActivity.this)
                        .setTitle(getString(R.string.qhybrid_offset_timezone))
                        .setView(layout2)
                        .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                prefs.edit().putInt("QHYBRID_TIMEZONE_OFFSET", hourPicker.getValue() * 60 + minPicker.getValue()).apply();
                                updateTimezoneOffset();
                                LocalBroadcastManager.getInstance(ConfigActivity.this).sendBroadcast(new Intent(QHybridSupport.QHYBRID_COMMAND_UPDATE_TIMEZONE));
                                GB.toast(getString(R.string.qhybrid_changes_delay_prompt), Toast.LENGTH_SHORT, GB.INFO);
                            }
                        })
                        .setNegativeButton("cancel", null)
                        .show();
            }
        });
        updateTimezoneOffset();

        setTitle(R.string.preferences_qhybrid_settings);

        ListView appList = findViewById(R.id.qhybrid_appList);

        try {
            helper = new PackageConfigHelper(getApplicationContext());
            list = helper.getNotificationConfigurations();
        } catch (Exception e) {
            GB.toast("error getting configurations", Toast.LENGTH_SHORT, GB.ERROR, e);
            list = new ArrayList<>();
        }
        // null is added to indicate the plus button added handled in PackageAdapter#getView
        list.add(null);
        appList.setAdapter(adapter = new PackageAdapter(this, R.layout.qhybrid_package_settings_item, list));
        appList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(final AdapterView<?> adapterView, View view, final int i, long l) {
                PopupMenu menu = new PopupMenu(ConfigActivity.this, view);
                menu.getMenu().add(0, 0, 0, "edit");
                menu.getMenu().add(0, 1, 1, "delete");
                menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        switch (menuItem.getItemId()) {
                            case 0: {
                                TimePicker picker = new TimePicker(ConfigActivity.this, (NotificationConfiguration) adapterView.getItemAtPosition(i));
                                picker.finishListener = new TimePicker.OnFinishListener() {
                                    @Override
                                    public void onFinish(boolean success, NotificationConfiguration config) {
                                        setControl(false, null);
                                        if (success) {
                                            try {
                                                helper.saveNotificationConfiguration(config);
                                                LocalBroadcastManager.getInstance(ConfigActivity.this).sendBroadcast(new Intent(QHybridSupport.QHYBRID_COMMAND_NOTIFICATION_CONFIG_CHANGED));
                                            } catch (Exception e) {
                                                GB.toast("error saving notification", Toast.LENGTH_SHORT, GB.ERROR, e);
                                            }
                                            refreshList();
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
                                setControl(true, picker.getSettings());
                                break;
                            }
                            case 1: {
                                try {
                                    helper.deleteNotificationConfiguration((NotificationConfiguration) adapterView.getItemAtPosition(i));
                                    LocalBroadcastManager.getInstance(ConfigActivity.this).sendBroadcast(new Intent(QHybridSupport.QHYBRID_COMMAND_NOTIFICATION_CONFIG_CHANGED));
                                } catch (Exception e) {
                                    GB.toast("error deleting setting", Toast.LENGTH_SHORT, GB.ERROR, e);
                                }
                                refreshList();
                                break;
                            }
                        }
                        return true;
                    }
                });
                menu.show();
                return true;
            }
        });

        appList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent notificationIntent = new Intent(QHybridSupport.QHYBRID_COMMAND_NOTIFICATION);
                notificationIntent.putExtra("CONFIG", (NotificationConfiguration) adapterView.getItemAtPosition(i));
                LocalBroadcastManager.getInstance(ConfigActivity.this).sendBroadcast(notificationIntent);
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
                String[] values = {"25", "50", "100"};
                device.addDeviceInfo(new GenericItem(QHybridSupport.ITEM_VIBRATION_STRENGTH, values[progress]));
                Intent intent = new Intent(QHybridSupport.QHYBRID_COMMAND_UPDATE_SETTINGS);
                intent.putExtra("EXTRA_SETTING", QHybridSupport.ITEM_VIBRATION_STRENGTH);
                LocalBroadcastManager.getInstance(ConfigActivity.this).sendBroadcast(intent);
            }
        });

        device = GBApplication.app().getDeviceManager().getSelectedDevice();
        if (device == null || device.getType() != DeviceType.FOSSILQHYBRID || device.getFirmwareVersion().charAt(2) != '0') {
            setSettingsError(getString(R.string.watch_not_connected));
        } else {
            updateSettings();
        }
    }

    private void updateTimeOffset() {
        int timeOffset = prefs.getInt("QHYBRID_TIME_OFFSET", 0);
        DecimalFormat format = new DecimalFormat("00");
        timeOffsetView.setText(
                format.format(timeOffset / 60) + ":" +
                        format.format(timeOffset % 60)
        );
    }


    private void updateTimezoneOffset() {
        int timeOffset = prefs.getInt("QHYBRID_TIMEZONE_OFFSET", 0);
        DecimalFormat format = new DecimalFormat("00");
        timezoneOffsetView.setText(
                format.format(timeOffset / 60) + ":" +
                        format.format(timeOffset % 60)
        );
    }


    private void setSettingsEnabled(boolean enables) {
        findViewById(R.id.settingsLayout).setAlpha(enables ? 1f : 0.2f);
    }

    private void updateSettings() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                EditText et = findViewById(R.id.stepGoalEt);
                et.setOnEditorActionListener(null);
                final String text = device.getDeviceInfo(QHybridSupport.ITEM_STEP_GOAL).getDetails();
                et.setText(text);
                et.setSelection(text.length());
                et.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                        if (i == EditorInfo.IME_ACTION_DONE || i == EditorInfo.IME_ACTION_NEXT) {
                            String t = textView.getText().toString();
                            if (!t.isEmpty()) {
                                device.addDeviceInfo(new GenericItem(QHybridSupport.ITEM_STEP_GOAL, t));
                                Intent intent = new Intent(QHybridSupport.QHYBRID_COMMAND_UPDATE_SETTINGS);
                                intent.putExtra("EXTRA_SETTING", QHybridSupport.ITEM_STEP_GOAL);
                                LocalBroadcastManager.getInstance(ConfigActivity.this).sendBroadcast(intent);
                                updateSettings();
                            }
                            ((InputMethodManager) getApplicationContext().getSystemService(Activity.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                        }
                        return true;
                    }
                });

                if ("true".equals(device.getDeviceInfo(QHybridSupport.ITEM_EXTENDED_VIBRATION_SUPPORT).getDetails())) {
                    final int strengthProgress = (int) (Math.log(Double.parseDouble(device.getDeviceInfo(QHybridSupport.ITEM_VIBRATION_STRENGTH).getDetails()) / 25) / Math.log(2));

                    setSettingsEnabled(true);
                    SeekBar seekBar = findViewById(R.id.vibrationStrengthBar);
                    seekBar.setProgress(strengthProgress);
                } else {
                    findViewById(R.id.vibrationStrengthBar).setEnabled(false);
                    findViewById(R.id.vibrationStrengthLayout).setAlpha(0.5f);
                }
                CheckBox activityHandCheckbox = findViewById(R.id.checkBoxUserActivityHand);
                if (device.getDeviceInfo(QHybridSupport.ITEM_HAS_ACTIVITY_HAND).getDetails().equals("true")) {
                    if (device.getDeviceInfo(QHybridSupport.ITEM_USE_ACTIVITY_HAND).getDetails().equals("true")) {
                        activityHandCheckbox.setChecked(true);
                    }
                    activityHandCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean checked) {
                            if (!device.getDeviceInfo(QHybridSupport.ITEM_STEP_GOAL).getDetails().equals("1000000")) {
                                new AlertDialog.Builder(ConfigActivity.this)
                                        .setMessage(getString(R.string.qhybrid_prompt_million_steps))
                                        .setPositiveButton("ok", null)
                                        .show();
                                buttonView.setChecked(false);
                                return;
                            }
                            device.addDeviceInfo(new GenericItem(QHybridSupport.ITEM_USE_ACTIVITY_HAND, String.valueOf(checked)));
                            Intent intent = new Intent(QHybridSupport.QHYBRID_COMMAND_UPDATE_SETTINGS);
                            intent.putExtra("EXTRA_SETTING", QHybridSupport.ITEM_USE_ACTIVITY_HAND);
                            LocalBroadcastManager.getInstance(ConfigActivity.this).sendBroadcast(intent);
                        }
                    });
                } else {
                    // activityHandCheckbox.setEnabled(false);
                    activityHandCheckbox.setAlpha(0.2f);
                    activityHandCheckbox.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            GB.toast("nah.", Toast.LENGTH_SHORT, GB.INFO);
                            ((CheckBox) v).setChecked(false);
                        }
                    });
                }

                ItemWithDetails item = device.getDeviceInfo(FossilWatchAdapter.ITEM_BUTTONS);
                String buttonJson = null;
                if(item != null) {
                    buttonJson = item.getDetails();
                }
                try {
                    JSONArray buttonConfig_;
                    if (buttonJson == null || buttonJson.isEmpty()) {
                        buttonConfig_ = new JSONArray(new String[]{"", "", ""});
                    }else{
                        buttonConfig_ = new JSONArray(buttonJson);
                    }

                    final JSONArray buttonConfig = buttonConfig_;

                    LinearLayout buttonLayout = findViewById(R.id.buttonConfigLayout);
                    buttonLayout.removeAllViews();
                    findViewById(R.id.buttonOverwriteButtons).setVisibility(View.GONE);
                    final ConfigPayload[] payloads = ConfigPayload.values();
                    final String[] names = new String[payloads.length];
                    for (int i = 0; i < payloads.length; i++)
                        names[i] = payloads[i].getDescription();
                    for (int i = 0; i < buttonConfig.length(); i++) {
                        final int currentIndex = i;
                        String configName = buttonConfig.getString(i);
                        TextView buttonTextView = new TextView(ConfigActivity.this);
                        buttonTextView.setTextColor(Color.WHITE);
                        buttonTextView.setTextSize(20);
                        try {
                            ConfigPayload payload = ConfigPayload.valueOf(configName);
                            buttonTextView.setText("Button " + (i + 1) + ": " + payload.getDescription());
                        } catch (IllegalArgumentException e) {
                            buttonTextView.setText("Button " + (i + 1) + ": Unknown");
                        }

                        buttonTextView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                AlertDialog dialog = new AlertDialog.Builder(ConfigActivity.this)
                                        .setItems(names, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.cancel();
                                                ConfigPayload selected = payloads[which];

                                                try {
                                                    buttonConfig.put(currentIndex, selected.toString());
                                                    device.addDeviceInfo(new GenericItem(FossilWatchAdapter.ITEM_BUTTONS, buttonConfig.toString()));
                                                    updateSettings();
                                                    Intent buttonIntent = new Intent(QHybridSupport.QHYBRID_COMMAND_OVERWRITE_BUTTONS);
                                                    buttonIntent.putExtra(FossilWatchAdapter.ITEM_BUTTONS, buttonConfig.toString());
                                                    LocalBroadcastManager.getInstance(ConfigActivity.this).sendBroadcast(buttonIntent);
                                                } catch (JSONException e) {
                                                    GB.log("error", GB.ERROR, e);
                                                }
                                            }
                                        })
                                        .setTitle("Button " + (currentIndex + 1))
                                        .create();
                                dialog.show();
                            }
                        });

                        buttonLayout.addView(buttonTextView);
                    }
                } catch (JSONException e) {
                    GB.toast("error parsing button config", Toast.LENGTH_LONG, GB.ERROR);
                }
            }
        });
    }

    private void setControl(boolean control, NotificationConfiguration config) {
        if (hasControl == control) return;
        Intent intent = new Intent(control ? QHybridSupport.QHYBRID_COMMAND_CONTROL : QHybridSupport.QHYBRID_COMMAND_UNCONTROL);
        intent.putExtra("CONFIG", config);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        this.hasControl = control;
    }

    private void setHands(NotificationConfiguration config) {
        sendControl(config, QHybridSupport.QHYBRID_COMMAND_SET);
    }

    private void vibrate(NotificationConfiguration config) {
        sendControl(config, QHybridSupport.QHYBRID_COMMAND_VIBRATE);
    }

    private void sendControl(NotificationConfiguration config, String request) {
        Intent intent = new Intent(request);
        intent.putExtra("CONFIG", config);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void refreshList() {
        list.clear();
        try {
            list.addAll(helper.getNotificationConfigurations());
        } catch (Exception e) {
            GB.toast("error getting configurations", Toast.LENGTH_SHORT, GB.ERROR, e);
        }
        // null is added to indicate the plus button added handled in PackageAdapter#getView
        list.add(null);
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshList();
        registerReceiver(buttonReceiver, new IntentFilter(QHybridSupport.QHYBRID_EVENT_BUTTON_PRESS));
        LocalBroadcastManager.getInstance(this).registerReceiver(settingsReceiver, new IntentFilter(QHybridSupport.QHYBRID_EVENT_SETTINGS_UPDATED));
        LocalBroadcastManager.getInstance(this).registerReceiver(fileReceiver, new IntentFilter(QHybridSupport.QHYBRID_EVENT_FILE_UPLOADED));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(buttonReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(settingsReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(fileReceiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }


    private void setSettingsError(final String error) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setSettingsEnabled(false);
                ((TextView) findViewById(R.id.settingsErrorText)).setVisibility(View.VISIBLE);
                ((TextView) findViewById(R.id.settingsErrorText)).setText(error);
            }
        });
    }

    class PackageAdapter extends ArrayAdapter<NotificationConfiguration> {
        PackageManager manager;

        PackageAdapter(@NonNull Context context, int resource, @NonNull List<NotificationConfiguration> objects) {
            super(context, resource, objects);
            manager = context.getPackageManager();
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View view, @NonNull ViewGroup parent) {
            if (!(view instanceof RelativeLayout))
                view = ((LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.qhybrid_package_settings_item, null);
            NotificationConfiguration settings = getItem(position);

            if (settings == null) {
                Button addButton = new Button(ConfigActivity.this);
                addButton.setText("+");
                addButton.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                addButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startActivityForResult(new Intent(ConfigActivity.this, QHybridAppChoserActivity.class), REQUEST_CODE_ADD_APP);
                    }
                });
                return addButton;
            }

            try {
                ((ImageView) view.findViewById(R.id.packageIcon)).setImageDrawable(manager.getApplicationIcon(settings.getPackageName()));
            } catch (PackageManager.NameNotFoundException e) {
                GB.log("error", GB.ERROR, e);
            }
            final int square_side = 100;
            ((TextView) view.findViewById(R.id.packageName)).setText(settings.getAppName());
            Bitmap bitmap = Bitmap.createBitmap(square_side, square_side, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bitmap);

            Paint black = new Paint();
            black.setColor(Color.BLACK);
            black.setStyle(Paint.Style.STROKE);
            black.setStrokeWidth(5);

            c.drawCircle(square_side / 2, square_side / 2, square_side / 2 - 3, black);

            int center = square_side / 2;
            if (settings.getHour() != -1) {
                c.drawLine(
                        center,
                        center,
                        (float) (center + Math.sin(Math.toRadians(settings.getHour())) * (square_side / 4)),
                        (float) (center - Math.cos(Math.toRadians(settings.getHour())) * (square_side / 4)),
                        black
                );
            }
            if (settings.getMin() != -1) {
                c.drawLine(
                        center,
                        center,
                        (float) (center + Math.sin(Math.toRadians(settings.getMin())) * (square_side / 3)),
                        (float) (center - Math.cos(Math.toRadians(settings.getMin())) * (square_side / 3)),
                        black
                );
            }

            ((ImageView) view.findViewById(R.id.packageClock)).setImageBitmap(bitmap);

            return view;
        }
    }

    BroadcastReceiver fileReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean error = intent.getBooleanExtra("EXTRA_ERROR", false);
            if (error) {
                GB.toast(getString(R.string.qhybrid_buttons_overwrite_error), Toast.LENGTH_SHORT, GB.ERROR);
                return;
            }
            GB.toast(getString(R.string.qhybrid_buttons_overwrite_success), Toast.LENGTH_SHORT, GB.INFO);
        }
    };

    BroadcastReceiver buttonReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            GB.toast("Button " + intent.getIntExtra("BUTTON", -1) + " pressed", Toast.LENGTH_SHORT, GB.INFO);
        }
    };

    BroadcastReceiver settingsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            GB.toast("Setting updated", Toast.LENGTH_SHORT, GB.INFO);
            updateSettings();
        }
    };
}
