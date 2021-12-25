/*  Copyright (C) 2015-2020 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, Frank Slezak, ivanovlev, Kasha, Lem Dulfo, Pavel Elagin, Steffen
    Liebergeld, vanous

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
package nodomain.freeyourgadget.gadgetbridge.activities;

import static android.content.Intent.EXTRA_SUBJECT;
import static nodomain.freeyourgadget.gadgetbridge.util.GB.NOTIFICATION_CHANNEL_ID;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
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

import androidx.core.app.NavUtils;
import androidx.core.app.NotificationCompat;
import androidx.core.app.RemoteInput;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.Widget;
import nodomain.freeyourgadget.gadgetbridge.adapter.SpinnerWithIconAdapter;
import nodomain.freeyourgadget.gadgetbridge.adapter.SpinnerWithIconItem;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
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
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.WidgetPreferenceStorage;

public class DebugActivity extends AbstractGBActivity {
    private static final Logger LOG = LoggerFactory.getLogger(DebugActivity.class);

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
    private static long SELECT_DEVICE = 999L;
    private long selectedTestDeviceKey = SELECT_DEVICE;
    private String selectedTestDeviceMAC;


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
        registerReceiver(mReceiver, filter); // for ACTION_REPLY

        editContent = findViewById(R.id.editContent);

        final ArrayList<String> spinnerArray = new ArrayList<>();
        for (NotificationType notificationType : NotificationType.values()) {
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
                notificationSpec.type = NotificationType.values()[sendTypeSpinner.getSelectedItemPosition()];
                notificationSpec.pebbleColor = notificationSpec.type.color;
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
                new AlertDialog.Builder(DebugActivity.this)
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
                    final GBDevice device = gbApp.getDeviceManager().getSelectedDevice();
                    if (device != null) {
                        new DatePickerDialog(DebugActivity.this, new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                Calendar date = Calendar.getInstance();
                                date.set(year, monthOfYear, dayOfMonth);

                                long timestamp = date.getTimeInMillis() - 1000;
                                GB.toast("Setting lastSyncTimeMillis: " + timestamp, Toast.LENGTH_LONG, GB.INFO);

                                SharedPreferences.Editor editor = GBApplication.getDeviceSpecificSharedPrefs(device.getAddress()).edit();
                                editor.remove("lastSyncTimeMillis"); //FIXME: key reconstruction is BAD
                                editor.putLong("lastSyncTimeMillis", timestamp);
                                editor.apply();
                            }
                        }, currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH), currentDate.get(Calendar.DATE)).show();
                    } else {
                        GB.toast("Device not selected/connected", Toast.LENGTH_LONG, GB.INFO);
                    }
                }


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
                showWarning();
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
                Context context = getApplicationContext();
                GBApplication gbApp = (GBApplication) context;
                final GBDevice device = gbApp.getDeviceManager().getSelectedDevice();
                if (device != null) {
                    GBApplication.deleteDeviceSpecificSharedPrefs(device.getAddress());
                }
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
                LinkedHashMap<String, Pair<Long, Integer>> allDevices;
                allDevices = getAllSupportedDevices(getApplicationContext());

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

                new AlertDialog.Builder(DebugActivity.this)
                        .setCancelable(true)
                        .setTitle("Add test device")
                        .setView(linearLayout)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                createTestDevice();
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

    private void showWarning() {
        new AlertDialog.Builder(this)
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
            File logFile = new File(fileName);
            if (!logFile.exists()) {
                GB.toast("File does not exist", Toast.LENGTH_LONG, GB.INFO);
                return;
            }

            Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
            emailIntent.setType("*/*");
            emailIntent.putExtra(EXTRA_SUBJECT, "Gadgetbridge log file");
            emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(logFile));
            startActivity(Intent.createChooser(emailIntent, "Share File"));
        }
    }

    private void testNotification() {
        Intent notificationIntent = new Intent(getApplicationContext(), DebugActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0,
                notificationIntent, 0);

        RemoteInput remoteInput = new RemoteInput.Builder(EXTRA_REPLY)
                .build();

        Intent replyIntent = new Intent(ACTION_REPLY);

        PendingIntent replyPendingIntent = PendingIntent.getBroadcast(this, 0, replyIntent, 0);

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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
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

    private void createTestDevice() {
        if (selectedTestDeviceKey == SELECT_DEVICE) {
            return;
        }
        DeviceType deviceType = DeviceType.fromKey((int) selectedTestDeviceKey);
        try (
                DBHandler db = GBApplication.acquireDB()) {
            DaoSession daoSession = db.getDaoSession();
            GBDevice gbDevice = new GBDevice(selectedTestDeviceMAC, deviceType.name(), "", deviceType);
            gbDevice.setFirmwareVersion("V1.0");
            gbDevice.setFirmwareVersion2("V1.0");

            //this causes the attributes (fw version) to be stored as well. Not much useful, but still...
            gbDevice.setState(GBDevice.State.INITIALIZED);

            Device device = DBHelper.getDevice(gbDevice, daoSession);
            Intent refreshIntent = new Intent(DeviceManager.ACTION_REFRESH_DEVICELIST);
            LocalBroadcastManager.getInstance(DebugActivity.this).sendBroadcast(refreshIntent);
            GB.toast(DebugActivity.this, "Added test device: " + deviceType.name(), Toast.LENGTH_SHORT, GB.INFO);

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

    private LinkedHashMap getAllSupportedDevices(Context appContext) {
        LinkedHashMap<String, Pair<Long, Integer>> newMap = new LinkedHashMap<>(1);
        newMap.put("Select device", new Pair(SELECT_DEVICE, R.drawable.ic_create));
        GBApplication app = (GBApplication) appContext;
        for (DeviceCoordinator coordinator : DeviceHelper.getInstance().getAllCoordinators()) {
            DeviceType deviceType = coordinator.getDeviceType();
            int icon = deviceType.getIcon();
            String name = app.getString(deviceType.getName()) + " (" + coordinator.getManufacturer() + ")";
            long deviceId = deviceType.getKey();
            newMap.put(name, new Pair(deviceId, icon));
        }

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
