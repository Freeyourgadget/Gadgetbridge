/*  Copyright (C) 2015-2024 Andreas Shimokawa, Arjan Schrijver, Carsten
    Pfeiffer, Daniel Dakhno, Daniele Gobbetti, Dmitriy Bogdanov, Frank Slezak,
    Ganblejs, ivanovlev, José Rebelo, Kamalei Zestri, Kasha, Lem Dulfo, Pavel
    Elagin, Petr Vaněk, Steffen Liebergeld, Tim

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.activities;

import static android.content.Intent.EXTRA_SUBJECT;
import static nodomain.freeyourgadget.gadgetbridge.util.GB.NOTIFICATION_CHANNEL_ID;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.bluetooth.BluetoothDevice;
import android.companion.AssociationRequest;
import android.companion.BluetoothDeviceFilter;
import android.companion.CompanionDeviceManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Pair;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NavUtils;
import androidx.core.app.NotificationCompat;
import androidx.core.app.RemoteInput;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.TreeMap;

import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.Widget;
import nodomain.freeyourgadget.gadgetbridge.adapter.SpinnerWithIconAdapter;
import nodomain.freeyourgadget.gadgetbridge.adapter.SpinnerWithIconItem;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventCameraRemote;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.externalevents.gps.GBLocationService;
import nodomain.freeyourgadget.gadgetbridge.externalevents.opentracks.OpenTracksContentObserver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.opentracks.OpenTracksController;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes;
import nodomain.freeyourgadget.gadgetbridge.model.Weather;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.PendingIntentUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;
import nodomain.freeyourgadget.gadgetbridge.util.WidgetPreferenceStorage;

public class DebugActivity extends AbstractGBActivity {
    private static final Logger LOG = LoggerFactory.getLogger(DebugActivity.class);

    private static Bundle dataLossSave;

    private static final String EXTRA_REPLY = "reply";
    private static final String ACTION_REPLY
            = "nodomain.freeyourgadget.gadgetbridge.DebugActivity.action.reply";
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (Objects.requireNonNull(intent.getAction())) {
                case ACTION_REPLY: {
                    Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
                    CharSequence reply = remoteInput.getCharSequence(EXTRA_REPLY);
                    LOG.info("got wearable reply: " + reply);
                    GB.toast(context, "got wearable reply: " + reply, Toast.LENGTH_SHORT, GB.INFO);
                    break;
                }
                case DeviceService.ACTION_REALTIME_SAMPLES:
                    handleRealtimeSample(intent.getSerializableExtra(DeviceService.EXTRA_REALTIME_SAMPLE));
                    break;
                default:
                    LOG.info("ignoring intent action " + intent.getAction());
                    break;
            }
        }
    };
    private Spinner sendTypeSpinner;
    private EditText editContent;
    public static final long SELECT_DEVICE = -1;
    private long selectedTestDeviceKey = SELECT_DEVICE;
    private String selectedTestDeviceMAC;

    private static final int SELECT_DEVICE_REQUEST_CODE = 1;

    private void handleRealtimeSample(Serializable extra) {
        if (extra instanceof ActivitySample) {
            ActivitySample sample = (ActivitySample) extra;
            GB.toast(this, "Heart Rate measured: " + sample.getHeartRate(), Toast.LENGTH_LONG, GB.INFO);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_REPLY);
        filter.addAction(DeviceService.ACTION_REALTIME_SAMPLES);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filter);
        ContextCompat.registerReceiver(this, mReceiver, filter, ContextCompat.RECEIVER_EXPORTED); // for ACTION_REPLY

        editContent = findViewById(R.id.editContent);

        final ArrayList<String> spinnerArray = new ArrayList<>();
        for (NotificationType notificationType : NotificationType.sortedValues()) {
            spinnerArray.add(notificationType.name());
        }
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, spinnerArray);
        sendTypeSpinner = findViewById(R.id.sendTypeSpinner);
        sendTypeSpinner.setAdapter(spinnerArrayAdapter);

        Button sendButton = findViewById(R.id.sendButton);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NotificationSpec notificationSpec = new NotificationSpec();
                String testString = editContent.getText().toString();
                notificationSpec.phoneNumber = testString;
                notificationSpec.body = testString;
                notificationSpec.sender = testString;
                notificationSpec.subject = testString;
                if (notificationSpec.type != NotificationType.GENERIC_SMS) {
                    // SMS notifications don't have a source app ID when sent by the SMSReceiver,
                    // so let's not set it here as well for consistency
                    notificationSpec.sourceAppId = BuildConfig.APPLICATION_ID;
                }
                notificationSpec.sourceName = getApplicationContext().getApplicationInfo()
                        .loadLabel(getApplicationContext().getPackageManager())
                        .toString();
                notificationSpec.type = NotificationType.sortedValues()[sendTypeSpinner.getSelectedItemPosition()];
                notificationSpec.pebbleColor = notificationSpec.type.color;
                notificationSpec.attachedActions = new ArrayList<>();

                if (notificationSpec.type == NotificationType.GENERIC_SMS) {
                    // REPLY action
                    NotificationSpec.Action replyAction = new NotificationSpec.Action();
                    replyAction.title = "Reply";
                    replyAction.type = NotificationSpec.Action.TYPE_SYNTECTIC_REPLY_PHONENR;
                    notificationSpec.attachedActions.add(replyAction);
                } else if (notificationSpec.type == NotificationType.CONVERSATIONS) {
                    // REPLY action
                    NotificationSpec.Action replyAction = new NotificationSpec.Action();
                    replyAction.title = "Reply";
                    replyAction.type = NotificationSpec.Action.TYPE_WEARABLE_REPLY;
                    notificationSpec.attachedActions.add(replyAction);
                }

                GBApplication.deviceService().onNotification(notificationSpec);
            }
        });

        Button incomingCallButton = findViewById(R.id.incomingCallButton);
        incomingCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CallSpec callSpec = new CallSpec();
                callSpec.command = CallSpec.CALL_INCOMING;
                callSpec.number = editContent.getText().toString();
                GBApplication.deviceService().onSetCallState(callSpec);
            }
        });
        Button outgoingCallButton = findViewById(R.id.outgoingCallButton);
        outgoingCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CallSpec callSpec = new CallSpec();
                callSpec.command = CallSpec.CALL_OUTGOING;
                callSpec.number = editContent.getText().toString();
                GBApplication.deviceService().onSetCallState(callSpec);
            }
        });

        Button startCallButton = findViewById(R.id.startCallButton);
        startCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CallSpec callSpec = new CallSpec();
                callSpec.command = CallSpec.CALL_START;
                GBApplication.deviceService().onSetCallState(callSpec);
            }
        });
        Button endCallButton = findViewById(R.id.endCallButton);
        endCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CallSpec callSpec = new CallSpec();
                callSpec.command = CallSpec.CALL_END;
                GBApplication.deviceService().onSetCallState(callSpec);
            }
        });

        Button rebootButton = findViewById(R.id.rebootButton);
        rebootButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GBApplication.deviceService().onReset(GBDeviceProtocol.RESET_FLAGS_REBOOT);
            }
        });

        Button factoryResetButton = findViewById(R.id.factoryResetButton);
        factoryResetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new MaterialAlertDialogBuilder(DebugActivity.this)
                        .setCancelable(true)
                        .setTitle(R.string.debugactivity_really_factoryreset_title)
                        .setMessage(R.string.debugactivity_really_factoryreset)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                GBApplication.deviceService().onReset(GBDeviceProtocol.RESET_FLAGS_FACTORY_RESET);
                            }
                        })
                        .setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .show();
            }
        });

        Button heartRateButton = findViewById(R.id.HeartRateButton);
        heartRateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GB.toast("Measuring heart rate, please wait...", Toast.LENGTH_LONG, GB.INFO);
                GBApplication.deviceService().onHeartRateTest();
            }
        });

        Button setFetchTimeButton = findViewById(R.id.SetFetchTimeButton);
        setFetchTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final Calendar currentDate = Calendar.getInstance();
                Context context = getApplicationContext();

                if (context instanceof GBApplication) {
                    GBApplication gbApp = (GBApplication) context;
                    final List<GBDevice> devices = gbApp.getDeviceManager().getSelectedDevices();
                    if(devices.size() == 0){
                        GB.toast("Device not selected/connected", Toast.LENGTH_LONG, GB.INFO);
                        return;
                    }
                    new DatePickerDialog(DebugActivity.this, new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                            Calendar date = Calendar.getInstance();
                            date.set(year, monthOfYear, dayOfMonth);

                            long timestamp = date.getTimeInMillis() - 1000;
                            GB.toast("Setting lastSyncTimeMillis: " + timestamp, Toast.LENGTH_LONG, GB.INFO);

                            for(GBDevice device : devices){
                                SharedPreferences.Editor editor = GBApplication.getDeviceSpecificSharedPrefs(device.getAddress()).edit();
                                editor.remove("lastSyncTimeMillis"); //FIXME: key reconstruction is BAD
                                editor.putLong("lastSyncTimeMillis", timestamp);
                                editor.apply();
                            }
                        }
                    }, currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH), currentDate.get(Calendar.DATE)).show();
                }


            }
        });

        Button setWeatherButton = findViewById(R.id.setWeatherButton);
        setWeatherButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Weather.getInstance().getWeatherSpec() == null) {
                    final WeatherSpec weatherSpec = new WeatherSpec();
                    weatherSpec.forecasts = new ArrayList<>();

                    weatherSpec.location = "Green Hill";
                    weatherSpec.currentConditionCode = 601; // snow
                    weatherSpec.currentCondition = Weather.getConditionString(weatherSpec.currentConditionCode);

                    weatherSpec.currentTemp = 15 + 273;
                    weatherSpec.currentHumidity = 30;

                    weatherSpec.windSpeed = 10;
                    weatherSpec.windDirection = 12;
                    weatherSpec.timestamp = (int) (System.currentTimeMillis() / 1000);
                    weatherSpec.todayMinTemp = 10 + 273;
                    weatherSpec.todayMaxTemp = 25 + 273;

                    for (int i = 0; i < 5; i++) {
                        final WeatherSpec.Daily gbForecast = new WeatherSpec.Daily();
                        gbForecast.minTemp = 10 + i + 273;
                        gbForecast.maxTemp = 25 + i + 273;

                        gbForecast.conditionCode = 800; // clear
                        weatherSpec.forecasts.add(gbForecast);
                    }

                    Weather.getInstance().setWeatherSpec(new ArrayList<>(Collections.singletonList(weatherSpec)));
                }

                final ArrayList<WeatherSpec> specs = new ArrayList<>(Weather.getInstance().getWeatherSpecs());
                GBApplication.deviceService().onSendWeather(specs);
            }
        });

        Button showCachedWeatherButton = findViewById(R.id.showCachedWeatherButton);
        showCachedWeatherButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                final List<WeatherSpec> weatherSpecs = Weather.getInstance().getWeatherSpecs();

                if (weatherSpecs == null || weatherSpecs.isEmpty()) {
                    displayWeatherInfo(null);
                    return;
                } else if (weatherSpecs.size() == 1) {
                    displayWeatherInfo(weatherSpecs.get(0));
                    return;
                }

                final String[] weatherLocations = new String[weatherSpecs.size()];

                for (int i = 0; i < weatherSpecs.size(); i++) {
                    weatherLocations[i] = weatherSpecs.get(i).location;
                }

                new MaterialAlertDialogBuilder(DebugActivity.this)
                        .setCancelable(true)
                        .setTitle("Choose Location")
                        .setItems(weatherLocations, (dialog, which) -> displayWeatherInfo(weatherSpecs.get(which)))
                        .setNegativeButton("Cancel", (dialog, which) -> {
                        })
                        .show();
            }
        });

        Button setMusicInfoButton = findViewById(R.id.setMusicInfoButton);
        setMusicInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MusicSpec musicSpec = new MusicSpec();
                String testString = editContent.getText().toString();
                musicSpec.artist = testString + "(artist)";
                musicSpec.album = testString + "(album)";
                musicSpec.track = testString + "(track)";
                musicSpec.duration = 10;
                musicSpec.trackCount = 5;
                musicSpec.trackNr = 2;

                GBApplication.deviceService().onSetMusicInfo(musicSpec);

                MusicStateSpec stateSpec = new MusicStateSpec();
                stateSpec.position = 0;
                stateSpec.state = 0x01; // playing
                stateSpec.playRate = 100;
                stateSpec.repeat = 1;
                stateSpec.shuffle = 1;

                GBApplication.deviceService().onSetMusicState(stateSpec);
            }
        });

        Button setTimeButton = findViewById(R.id.setTimeButton);
        setTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GBApplication.deviceService().onSetTime();
            }
        });

        Button testNotificationButton = findViewById(R.id.testNotificationButton);
        testNotificationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testNotification();
            }
        });

        Button testPebbleKitNotificationButton = findViewById(R.id.testPebbleKitNotificationButton);
        testPebbleKitNotificationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testPebbleKitNotification();
            }
        });

        Button fetchDebugLogsButton = findViewById(R.id.fetchDebugLogsButton);
        fetchDebugLogsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GBApplication.deviceService().onFetchRecordedData(RecordedDataTypes.TYPE_DEBUGLOGS);
            }
        });

        Button testNewFunctionalityButton = findViewById(R.id.testNewFunctionality);
        testNewFunctionalityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testNewFunctionality();
            }
        });

        Button shareLogButton = findViewById(R.id.shareLog);
        shareLogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(DebugActivity.this);
                Prefs prefs = new Prefs(sharedPrefs);
                boolean logging_enabled = prefs.getBoolean("log_to_file", false);
                if (logging_enabled) {
                    showLogSharingWarning();
                } else {
                    showLogSharingNotEnabledAlert();
                }
            }
        });

        Button showWidgetsButton = findViewById(R.id.showWidgetsButton);
        showWidgetsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAllRegisteredAppWidgets();
            }
        });

        Button unregisterWidgetsButton = findViewById(R.id.deleteWidgets);
        unregisterWidgetsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                unregisterAllRegisteredAppWidgets();
            }
        });

        Button showWidgetsPrefsButton = findViewById(R.id.showWidgetsPrefs);
        showWidgetsPrefsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAppWidgetsPrefs();
            }
        });

        Button deleteWidgetsPrefsButton = findViewById(R.id.deleteWidgetsPrefs);
        deleteWidgetsPrefsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteWidgetsPrefs();
            }
        });

        Button removeDevicePreferencesButton = findViewById(R.id.removeDevicePreferences);
        removeDevicePreferencesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new MaterialAlertDialogBuilder(DebugActivity.this)
                        .setCancelable(true)
                        .setTitle(R.string.debugactivity_confirm_remove_device_preferences_title)
                        .setMessage(R.string.debugactivity_confirm_remove_device_preferences)
                        .setPositiveButton(R.string.ok, (dialog, which) -> {
                            final GBApplication gbApp = (GBApplication) getApplicationContext();
                            final List<GBDevice> devices = gbApp.getDeviceManager().getSelectedDevices();
                            for(final GBDevice device : devices){
                                GBApplication.deleteDeviceSpecificSharedPrefs(device.getAddress());
                            }
                        })
                        .setNegativeButton(R.string.Cancel, (dialog, which) -> {})
                        .show();
            }
        });

        Button runDebugFunction = findViewById(R.id.runDebugFunction);
        runDebugFunction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //SharedPreferences.Editor editor = GBApplication.getPrefs().getPreferences().edit();
                //editor.remove("notification_list_is_blacklist").apply();
            }
        });

        Button addDeviceButtonDebug = findViewById(R.id.addDeviceButtonDebug);
        addDeviceButtonDebug.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Map<String, Pair<Long, Integer>> allDevices = getAllSupportedDevices(getApplicationContext());

                final LinearLayout linearLayout = new LinearLayout(DebugActivity.this);
                linearLayout.setOrientation(LinearLayout.VERTICAL);

                final LinearLayout macLayout = new LinearLayout(DebugActivity.this);
                macLayout.setOrientation(LinearLayout.HORIZONTAL);
                macLayout.setPadding(20, 0, 20, 0);

                final TextView textView = new TextView(DebugActivity.this);
                textView.setText("MAC Address: ");
                final EditText editText = new EditText(DebugActivity.this);
                selectedTestDeviceMAC = randomMac();
                editText.setText(selectedTestDeviceMAC);
                editText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void afterTextChanged(Editable editable) {
                        selectedTestDeviceMAC = editable.toString();

                    }
                });

                macLayout.addView(textView);
                macLayout.addView(editText);

                final Spinner deviceListSpinner = new Spinner(DebugActivity.this);
                ArrayList<SpinnerWithIconItem> deviceListArray = new ArrayList<>();
                for (Map.Entry<String, Pair<Long, Integer>> item : allDevices.entrySet()) {
                    deviceListArray.add(new SpinnerWithIconItem(item.getKey(), item.getValue().first, item.getValue().second));
                }
                final SpinnerWithIconAdapter deviceListAdapter = new SpinnerWithIconAdapter(DebugActivity.this,
                        R.layout.spinner_with_image_layout, R.id.spinner_item_text, deviceListArray);
                deviceListSpinner.setAdapter(deviceListAdapter);
                addListenerOnSpinnerDeviceSelection(deviceListSpinner);

                linearLayout.addView(deviceListSpinner);
                linearLayout.addView(macLayout);

                new MaterialAlertDialogBuilder(DebugActivity.this)
                        .setCancelable(true)
                        .setTitle(R.string.add_test_device)
                        .setView(linearLayout)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                createTestDevice(DebugActivity.this, selectedTestDeviceKey, selectedTestDeviceMAC);
                            }
                        })
                        .setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .show();
            }
        });

        CheckBox activity_list_debug_extra_time_range = findViewById(R.id.activity_list_debug_extra_time_range);
        activity_list_debug_extra_time_range.setAllCaps(true);
        boolean activity_list_debug_extra_time_range_value = GBApplication.getPrefs().getPreferences().getBoolean("activity_list_debug_extra_time_range", false);
        activity_list_debug_extra_time_range.setChecked(activity_list_debug_extra_time_range_value);

        activity_list_debug_extra_time_range.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                GBApplication.getPrefs().getPreferences().getBoolean("activity_list_debug_extra_time_range", false);
                SharedPreferences.Editor editor = GBApplication.getPrefs().getPreferences().edit();
                editor.putBoolean("activity_list_debug_extra_time_range", b).apply();
            }
        });

        Button startFitnessAppTracking = findViewById(R.id.startFitnessAppTracking);
        startFitnessAppTracking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OpenTracksController.startRecording(DebugActivity.this);
            }
        });

        Button stopFitnessAppTracking = findViewById(R.id.stopFitnessAppTracking);
        stopFitnessAppTracking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OpenTracksController.stopRecording(DebugActivity.this);
            }
        });

        Button stopPhoneGpsLocationListener = findViewById(R.id.stopPhoneGpsLocationListener);
        stopPhoneGpsLocationListener.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GBLocationService.stop(DebugActivity.this, null);
            }
        });

        Button showCompanionDevices = findViewById(R.id.showCompanionDevices);
        showCompanionDevices.setVisibility(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? View.VISIBLE : View.GONE);
        showCompanionDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    LOG.warn("Android version < O, companion devices not supported");
                    return;
                }

                final CompanionDeviceManager manager = (CompanionDeviceManager) GBApplication.getContext().getSystemService(Context.COMPANION_DEVICE_SERVICE);
                final List<String> associations = new ArrayList<>(manager.getAssociations());
                Collections.sort(associations);
                String companionDevicesList = String.format(Locale.ROOT, "%d companion devices", associations.size());
                if (!associations.isEmpty()) {
                    companionDevicesList += "\n\n" + StringUtils.join("\n", associations.toArray(new String[0]));
                }

                new MaterialAlertDialogBuilder(DebugActivity.this)
                        .setCancelable(false)
                        .setTitle("Companion Devices")
                        .setMessage(companionDevicesList)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .show();
            }
        });

        final Button pairAsCompanion = findViewById(R.id.pairAsCompanion);
        pairAsCompanion.setVisibility(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? View.VISIBLE : View.GONE);
        pairAsCompanion.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                pairCurrentAsCompanion();
            }
        });

        Button showStatusFitnessAppTracking = findViewById(R.id.showStatusFitnessAppTracking);
        final int delay = 2 * 1000;

        showStatusFitnessAppTracking.setOnClickListener(new View.OnClickListener() {
            final Handler handler = new Handler();
            Runnable runnable;

            @Override
            public void onClick(View v) {
                final MaterialAlertDialogBuilder fitnesStatusBuilder = new MaterialAlertDialogBuilder(DebugActivity.this);
                fitnesStatusBuilder
                        .setCancelable(false)
                        .setTitle("openTracksObserver Status")
                        .setMessage("Starting openTracksObserver watcher, waiting for an update, refreshing every: " + delay + "ms")
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                handler.removeCallbacks(runnable);
                            }
                        });
                final AlertDialog alert = fitnesStatusBuilder.show();


                runnable = new Runnable() {
                    @Override
                    public void run() {
                        LOG.debug("openTracksObserver debug watch dialog running");
                        handler.postDelayed(this, delay); //schedule next execution

                        OpenTracksContentObserver openTracksObserver = GBApplication.app().getOpenTracksObserver();
                        if (openTracksObserver == null) {
                            LOG.debug("openTracksObserver is null");
                            alert.cancel();
                            alert.setMessage("openTracksObserver not running");
                            alert.show();
                            return;
                        }
                        LOG.debug("openTracksObserver is not null, updating debug view");
                        long timeSecs = openTracksObserver.getTimeMillisChange() / 1000;
                        float distanceCM = openTracksObserver.getDistanceMeterChange() * 100;

                        LOG.debug("Time: " + timeSecs + " distanceCM " + distanceCM);
                        alert.cancel();
                        alert.setMessage("TimeSec: " + timeSecs + " distanceCM " + distanceCM);
                        alert.show();
                    }
                };
                handler.postDelayed(runnable, delay);
            }
        });

        Button cameraOpenButton = findViewById(R.id.cameraOpen);
        cameraOpenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent cameraIntent = new Intent(getApplicationContext(), CameraActivity.class);
                cameraIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                cameraIntent.putExtra(CameraActivity.intentExtraEvent, GBDeviceEventCameraRemote.eventToInt(GBDeviceEventCameraRemote.Event.OPEN_CAMERA));
                getApplicationContext().startActivity(cameraIntent);
            }
        });
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                requestCode == SELECT_DEVICE_REQUEST_CODE &&
                resultCode == Activity.RESULT_OK) {

            final BluetoothDevice deviceToPair = data.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE);

            if (deviceToPair != null) {
                if (deviceToPair.getBondState() != BluetoothDevice.BOND_BONDED) {
                    GB.toast("Creating bond...", Toast.LENGTH_SHORT, GB.INFO);
                    deviceToPair.createBond();
                } else {
                    GB.toast("Bonding complete", Toast.LENGTH_LONG, GB.INFO);
                }
            } else {
                GB.toast("No device to pair", Toast.LENGTH_LONG, GB.ERROR);
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void pairCurrentAsCompanion() {
        final GBApplication gbApp = (GBApplication) getApplicationContext();
        final List<GBDevice> devices = gbApp.getDeviceManager().getSelectedDevices();
        if (devices.size() != 1) {
            GB.toast("Please connect to a single device that you want to pair as companion", Toast.LENGTH_LONG, GB.WARN);
            return;
        }

        final GBDevice device = devices.get(0);

        final CompanionDeviceManager manager = (CompanionDeviceManager) getSystemService(Context.COMPANION_DEVICE_SERVICE);

        if (manager.getAssociations().contains(device.getAddress())) {
            GB.toast(device.getAliasOrName() + " already paired as companion", Toast.LENGTH_LONG, GB.INFO);
            return;
        }

        final BluetoothDeviceFilter deviceFilter = new BluetoothDeviceFilter.Builder()
                .setAddress(device.getAddress())
                .build();

        final AssociationRequest pairingRequest = new AssociationRequest.Builder()
                .addDeviceFilter(deviceFilter)
                .setSingleDevice(true)
                .build();

        CompanionDeviceManager.Callback callback = new CompanionDeviceManager.Callback() {
            @Override
            public void onFailure(final CharSequence error) {
                GB.toast("Companion pairing failed: " + error, Toast.LENGTH_LONG, GB.ERROR);
            }

            @Override
            public void onDeviceFound(@NonNull final IntentSender chooserLauncher) {
                GB.toast("Found device", Toast.LENGTH_SHORT, GB.INFO);

                try {
                    ActivityCompat.startIntentSenderForResult(
                            DebugActivity.this,
                            chooserLauncher,
                            SELECT_DEVICE_REQUEST_CODE,
                            null,
                            0,
                            0,
                            0,
                            null
                    );
                } catch (final IntentSender.SendIntentException e) {
                    LOG.error("Failed to send intent", e);
                }
            }
        };

        manager.associate(pairingRequest, callback, null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (dataLossSave != null ) {
            dataLossSave.clear();
            dataLossSave = null ;
        }
        dataLossSave = new Bundle();
        dataLossSave.putString("editContent", editContent.getText().toString());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (dataLossSave != null ) {
            editContent.setText(dataLossSave.getString("editContent", ""));
        }else{
            editContent.setText("Test");
        }
    }

    private void deleteWidgetsPrefs() {
        WidgetPreferenceStorage widgetPreferenceStorage = new WidgetPreferenceStorage();
        widgetPreferenceStorage.deleteWidgetsPrefs(DebugActivity.this);
        widgetPreferenceStorage.showAppWidgetsPrefs(DebugActivity.this);
    }

    private void showAppWidgetsPrefs() {
        WidgetPreferenceStorage widgetPreferenceStorage = new WidgetPreferenceStorage();
        widgetPreferenceStorage.showAppWidgetsPrefs(DebugActivity.this);

    }

    private void showAllRegisteredAppWidgets() {
        //https://stackoverflow.com/questions/17387191/check-if-a-widget-is-exists-on-homescreen-using-appwidgetid

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(DebugActivity.this);
        AppWidgetHost appWidgetHost = new AppWidgetHost(DebugActivity.this, 1); // for removing phantoms
        int[] appWidgetIDs = appWidgetManager.getAppWidgetIds(new ComponentName(DebugActivity.this, Widget.class));
        GB.toast("Number of registered app widgets: " + appWidgetIDs.length, Toast.LENGTH_SHORT, GB.INFO);
        for (int appWidgetID : appWidgetIDs) {
            GB.toast("Widget: " + appWidgetID, Toast.LENGTH_SHORT, GB.INFO);
        }
    }

    private void unregisterAllRegisteredAppWidgets() {
        //https://stackoverflow.com/questions/17387191/check-if-a-widget-is-exists-on-homescreen-using-appwidgetid

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(DebugActivity.this);
        AppWidgetHost appWidgetHost = new AppWidgetHost(DebugActivity.this, 1); // for removing phantoms
        int[] appWidgetIDs = appWidgetManager.getAppWidgetIds(new ComponentName(DebugActivity.this, Widget.class));
        GB.toast("Number of registered app widgets: " + appWidgetIDs.length, Toast.LENGTH_SHORT, GB.INFO);
        for (int appWidgetID : appWidgetIDs) {
            appWidgetHost.deleteAppWidgetId(appWidgetID);
            GB.toast("Removing widget: " + appWidgetID, Toast.LENGTH_SHORT, GB.INFO);
        }
    }

    private void showLogSharingNotEnabledAlert() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.note)
                .setPositiveButton(R.string.ok, null)
                .setMessage(R.string.share_log_not_enabled_message)
                .show();
    }

    private void showLogSharingWarning() {
        new MaterialAlertDialogBuilder(this)
                .setCancelable(true)
                .setTitle(R.string.warning)
                .setMessage(R.string.share_log_warning)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        shareLog();
                    }
                })
                .setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .show();
    }

    private void testNewFunctionality() {
        GBApplication.deviceService().onTestNewFunction();
    }

    private void shareLog() {
        String fileName = GBApplication.getLogPath();
        if (fileName != null && fileName.length() > 0) {
            // Flush the logs, so that we ensure latest lines are also there
            GBApplication.getLogging().setImmediateFlush(true);
            LOG.debug("Flushing logs before sharing");
            GBApplication.getLogging().setImmediateFlush(false);

            File logFile = new File(fileName);
            if (!logFile.exists()) {
                GB.toast("File does not exist", Toast.LENGTH_LONG, GB.INFO);
                return;
            }

            final Uri providerUri = FileProvider.getUriForFile(
                    this,
                    getApplicationContext().getPackageName() + ".screenshot_provider",
                    logFile
            );

            Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
            emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            emailIntent.setType("*/*");
            emailIntent.putExtra(EXTRA_SUBJECT, "Gadgetbridge log file");
            emailIntent.putExtra(Intent.EXTRA_STREAM, providerUri);
            startActivity(Intent.createChooser(emailIntent, "Share File"));
        }
    }

    private void testNotification() {
        Intent notificationIntent = new Intent(getApplicationContext(), DebugActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntentUtils.getActivity(getApplicationContext(), 0,
                notificationIntent, 0, false);

        RemoteInput remoteInput = new RemoteInput.Builder(EXTRA_REPLY)
                .build();

        Intent replyIntent = new Intent(ACTION_REPLY);

        PendingIntent replyPendingIntent = PendingIntentUtils.getBroadcast(this, 0, replyIntent, 0, true);

        NotificationCompat.Action action =
                new NotificationCompat.Action.Builder(android.R.drawable.ic_input_add, "Reply", replyPendingIntent)
                        .addRemoteInput(remoteInput)
                        .build();

        NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender().addAction(action);

        NotificationCompat.Builder ncomp = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(getString(R.string.test_notification))
                .setContentText(getString(R.string.this_is_a_test_notification_from_gadgetbridge))
                .setTicker(getString(R.string.this_is_a_test_notification_from_gadgetbridge))
                .setSmallIcon(R.drawable.ic_notification)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .extend(wearableExtender);

        GB.notify((int) System.currentTimeMillis(), ncomp.build(), this);
    }

    private void testPebbleKitNotification() {
        Intent pebbleKitIntent = new Intent("com.getpebble.action.SEND_NOTIFICATION");
        pebbleKitIntent.putExtra("messageType", "PEBBLE_ALERT");
        pebbleKitIntent.putExtra("notificationData", "[{\"title\":\"PebbleKitTest\",\"body\":\"sent from Gadgetbridge\"}]");
        getApplicationContext().sendBroadcast(pebbleKitIntent);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        unregisterReceiver(mReceiver);
    }

    private void addListenerOnSpinnerDeviceSelection(Spinner spinner) {
        spinner.setOnItemSelectedListener(new CustomOnDeviceSelectedListener());
    }

    public static void createTestDevice(Context context, long deviceKey, String deviceMac) {
        if (deviceKey == SELECT_DEVICE) {
            return;
        }
        DeviceType deviceType = DeviceType.values()[(int) deviceKey];
        String deviceName = deviceType.name();
        int deviceNameResource = deviceType.getDeviceCoordinator().getDeviceNameResource();
        if(deviceNameResource != 0){
            deviceName = context.getString(deviceNameResource);
        }
        try (
            DBHandler db = GBApplication.acquireDB()) {
            DaoSession daoSession = db.getDaoSession();
            GBDevice gbDevice = new GBDevice(deviceMac, deviceName, "", null, deviceType);
            gbDevice.setFirmwareVersion("N/A");
            gbDevice.setFirmwareVersion2("N/A");

            //this causes the attributes (fw version) to be stored as well. Not much useful, but still...
            gbDevice.setState(GBDevice.State.INITIALIZED);

            Device device = DBHelper.getDevice(gbDevice, daoSession); //the addition happens here
            Intent refreshIntent = new Intent(DeviceManager.ACTION_REFRESH_DEVICELIST);
            LocalBroadcastManager.getInstance(context).sendBroadcast(refreshIntent);
            GB.toast(context, "Added test device: " + deviceName, Toast.LENGTH_SHORT, GB.INFO);

        } catch (
                Exception e) {
            GB.log("Error accessing database", GB.ERROR, e);
        }
    }

    private String randomMac() {
        Random random = new Random();
        String separator = ":";
        String[] mac = {
                String.format("%02x", random.nextInt(0xff)),
                String.format("%02x", random.nextInt(0xff)),
                String.format("%02x", random.nextInt(0xff)),
                String.format("%02x", random.nextInt(0xff)),
                String.format("%02x", random.nextInt(0xff)),
                String.format("%02x", random.nextInt(0xff))
        };
        return TextUtils.join(separator, mac).toUpperCase(Locale.ROOT);
    }

    private void displayWeatherInfo(final WeatherSpec weatherSpec) {
        final String weatherInfo = getWeatherInfo(weatherSpec);

        new MaterialAlertDialogBuilder(DebugActivity.this)
                .setCancelable(true)
                .setTitle("Cached Weather Data")
                .setMessage(weatherInfo)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                })
                .setNeutralButton(android.R.string.copy, (dialog, which) -> {
                    final ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Weather Info", weatherInfo);
                    clipboard.setPrimaryClip(clip);
                })
                .show();
    }

    private String getWeatherInfo(final WeatherSpec weatherSpec) {
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ROOT);

        final StringBuilder builder = new StringBuilder();

        if (weatherSpec == null)
            return "Weather cache is empty.";

        builder.append("Location: ").append(weatherSpec.location).append("\n");
        builder.append("Timestamp: ").append(weatherSpec.timestamp).append("\n");
        builder.append("Current Temp: ").append(weatherSpec.currentTemp).append(" K\n");
        builder.append("Max Temp: ").append(weatherSpec.todayMaxTemp).append(" K\n");
        builder.append("Min Temp: ").append(weatherSpec.todayMinTemp).append(" K\n");
        builder.append("Condition: ").append(weatherSpec.currentCondition).append("\n");
        builder.append("Condition Code: ").append(weatherSpec.currentConditionCode).append("\n");
        builder.append("Humidity: ").append(weatherSpec.currentHumidity).append("\n");
        builder.append("Wind Speed: ").append(weatherSpec.windSpeed).append(" kmph\n");
        builder.append("Wind Direction: ").append(weatherSpec.windDirection).append(" deg\n");
        builder.append("UV Index: ").append(weatherSpec.uvIndex).append("\n");
        builder.append("Precip Probability: ").append(weatherSpec.precipProbability).append(" %\n");
        builder.append("Dew Point: ").append(weatherSpec.dewPoint).append(" K\n");
        builder.append("Pressure: ").append(weatherSpec.pressure).append(" mb\n");
        builder.append("Cloud Cover: ").append(weatherSpec.cloudCover).append(" %\n");
        builder.append("Visibility: ").append(weatherSpec.visibility).append(" m\n");
        builder.append("Sun Rise: ").append(sdf.format(new Date(weatherSpec.sunRise * 1000L))).append("\n");
        builder.append("Sun Set: ").append(sdf.format(new Date(weatherSpec.sunSet * 1000L))).append("\n");
        builder.append("Moon Rise: ").append(sdf.format(new Date(weatherSpec.moonRise * 1000L))).append("\n");
        builder.append("Moon Set: ").append(sdf.format(new Date(weatherSpec.moonSet * 1000L))).append("\n");
        builder.append("Moon Phase: ").append(weatherSpec.moonPhase).append(" deg\n");
        builder.append("Latitude: ").append(weatherSpec.latitude).append("\n");
        builder.append("Longitude: ").append(weatherSpec.longitude).append("\n");
        builder.append("Feels Like Temp: ").append(weatherSpec.feelsLikeTemp).append(" K\n");
        builder.append("Is Current Location: ").append(weatherSpec.isCurrentLocation).append("\n");

        if (weatherSpec.airQuality != null) {
            builder.append("Air Quality aqi: ").append(weatherSpec.airQuality.aqi).append("\n");
            builder.append("Air Quality co: ").append(weatherSpec.airQuality.co).append("\n");
            builder.append("Air Quality no2: ").append(weatherSpec.airQuality.no2).append("\n");
            builder.append("Air Quality o3: ").append(weatherSpec.airQuality.o3).append("\n");
            builder.append("Air Quality pm10: ").append(weatherSpec.airQuality.pm10).append("\n");
            builder.append("Air Quality pm25: ").append(weatherSpec.airQuality.pm25).append("\n");
            builder.append("Air Quality so2: ").append(weatherSpec.airQuality.so2).append("\n");
            builder.append("Air Quality coAqi: ").append(weatherSpec.airQuality.coAqi).append("\n");
            builder.append("Air Quality no2Aqi: ").append(weatherSpec.airQuality.no2Aqi).append("\n");
            builder.append("Air Quality o3Aqi: ").append(weatherSpec.airQuality.o3Aqi).append("\n");
            builder.append("Air Quality pm10Aqi: ").append(weatherSpec.airQuality.pm10Aqi).append("\n");
            builder.append("Air Quality pm25Aqi: ").append(weatherSpec.airQuality.pm25Aqi).append("\n");
            builder.append("Air Quality so2Aqi: ").append(weatherSpec.airQuality.so2Aqi).append("\n");
        } else {
            builder.append("Air Quality: null\n");
        }

        int i = 0;
        for (final WeatherSpec.Daily daily : weatherSpec.forecasts) {
            builder.append("-------------\n");
            builder.append("-->Day ").append(i++).append("\n");
            builder.append("Max Temp: ").append(daily.maxTemp).append(" K\n");
            builder.append("Min Temp: ").append(daily.minTemp).append(" K\n");
            builder.append("Condition Code: ").append(daily.conditionCode).append("\n");
            builder.append("Humidity: ").append(daily.humidity).append("\n");
            builder.append("Wind Speed: ").append(daily.windSpeed).append(" kmph\n");
            builder.append("Wind Direction: ").append(daily.windDirection).append(" deg\n");
            builder.append("UV Index: ").append(daily.uvIndex).append("\n");
            builder.append("Precip Probability: ").append(daily.precipProbability).append(" %\n");
            builder.append("Sun Rise: ").append(sdf.format(new Date(daily.sunRise * 1000L))).append("\n");
            builder.append("Sun Set: ").append(sdf.format(new Date(daily.sunSet * 1000L))).append("\n");
            builder.append("Moon Rise: ").append(sdf.format(new Date(daily.moonRise * 1000L))).append("\n");
            builder.append("Moon Set: ").append(sdf.format(new Date(daily.moonSet * 1000L))).append("\n");
            builder.append("Moon Phase: ").append(daily.moonPhase).append(" deg\n");

            if (daily.airQuality != null) {
                builder.append("Air Quality aqi: ").append(daily.airQuality.aqi).append("\n");
                builder.append("Air Quality co: ").append(daily.airQuality.co).append("\n");
                builder.append("Air Quality no2: ").append(daily.airQuality.no2).append("\n");
                builder.append("Air Quality o3: ").append(daily.airQuality.o3).append("\n");
                builder.append("Air Quality pm10: ").append(daily.airQuality.pm10).append("\n");
                builder.append("Air Quality pm25: ").append(daily.airQuality.pm25).append("\n");
                builder.append("Air Quality so2: ").append(daily.airQuality.so2).append("\n");
                builder.append("Air Quality coAqi: ").append(daily.airQuality.coAqi).append("\n");
                builder.append("Air Quality no2Aqi: ").append(daily.airQuality.no2Aqi).append("\n");
                builder.append("Air Quality o3Aqi: ").append(daily.airQuality.o3Aqi).append("\n");
                builder.append("Air Quality pm10Aqi: ").append(daily.airQuality.pm10Aqi).append("\n");
                builder.append("Air Quality pm25Aqi: ").append(daily.airQuality.pm25Aqi).append("\n");
                builder.append("Air Quality so2Aqi: ").append(daily.airQuality.so2Aqi).append("\n");
            } else {
                builder.append("Air Quality: null\n");
            }
        }

        builder.append("=============\n");

        for (final WeatherSpec.Hourly hourly : weatherSpec.hourly) {
            builder.append("-------------\n");
            builder.append("-->Hour: ").append(sdf.format(new Date(hourly.timestamp * 1000L))).append("\n");
            builder.append("Max Temp: ").append(hourly.temp).append(" K\n");
            builder.append("Condition Code: ").append(hourly.conditionCode).append("\n");
            builder.append("Humidity: ").append(hourly.humidity).append("\n");
            builder.append("Wind Speed: ").append(hourly.windSpeed).append(" kmph\n");
            builder.append("Wind Direction: ").append(hourly.windDirection).append(" deg\n");
            builder.append("UV Index: ").append(hourly.uvIndex).append("\n");
            builder.append("Precip Probability: ").append(hourly.precipProbability).append(" %\n");
        }

        return builder.toString();
    }

    public static Map<String, Pair<Long, Integer>> getAllSupportedDevices(Context appContext) {
        LinkedHashMap<String, Pair<Long, Integer>> newMap = new LinkedHashMap<>(1);
        GBApplication app = (GBApplication) appContext;
        for (DeviceType deviceType : DeviceType.values()) {
            DeviceCoordinator coordinator = deviceType.getDeviceCoordinator();
            int icon = coordinator.getDefaultIconResource();
            String name = app.getString(coordinator.getDeviceNameResource());
            if (!name.startsWith(coordinator.getManufacturer())) {
                name += " (" + coordinator.getManufacturer() + ")";
            }
            long deviceId = deviceType.ordinal();
            newMap.put(name, new Pair(deviceId, icon));
        }
        TreeMap <String, Pair<Long, Integer>> sortedMap = new TreeMap<>(newMap);
        newMap = new LinkedHashMap<>(1);
        newMap.put(app.getString(R.string.widget_settings_select_device_title), new Pair(SELECT_DEVICE, R.drawable.ic_device_unknown));
        newMap.putAll(sortedMap);

        return newMap;
    }

    public class CustomOnDeviceSelectedListener implements AdapterView.OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            SpinnerWithIconItem selectedItem = (SpinnerWithIconItem) parent.getItemAtPosition(pos);
            selectedTestDeviceKey = selectedItem.getId();
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
            // TODO Auto-generated method stub
        }

    }

}
