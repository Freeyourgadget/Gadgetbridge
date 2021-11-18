/*  Copyright (C) 2015-2020 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, José Rebelo, Lem Dulfo, maxirnilian

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
package nodomain.freeyourgadget.gadgetbridge.adapter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.FragmentActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.jaredrummler.android.colorpicker.ColorPickerDialog;
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.ActivitySummariesActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.BatteryInfoActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.ConfigureAlarms;
import nodomain.freeyourgadget.gadgetbridge.activities.ControlCenterv2;
import nodomain.freeyourgadget.gadgetbridge.activities.HeartRateDialog;
import nodomain.freeyourgadget.gadgetbridge.activities.SettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.VibrationActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.ActivityListingDashboard;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.ChartsActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.database.DBAccess;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.model.DailyTotals;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

/**
 * Adapter for displaying GBDevice instances.
 */
public class GBDeviceAdapterv2 extends RecyclerView.Adapter<GBDeviceAdapterv2.ViewHolder> {
    private static final Logger LOG = LoggerFactory.getLogger(GBDeviceAdapterv2.class);

    private final Context context;
    private List<GBDevice> deviceList;
    private int expandedDevicePosition = RecyclerView.NO_POSITION;
    private ViewGroup parent;
    private HashMap<String, long[]> deviceActivityMap = new HashMap();

    public GBDeviceAdapterv2(Context context, List<GBDevice> deviceList, HashMap<String,long[]> deviceMap) {
        this.context = context;
        this.deviceList = deviceList;
        this.deviceActivityMap = deviceMap;
    }

    @NonNull
    @Override
    public GBDeviceAdapterv2.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.parent = parent;
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_itemv2, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        final GBDevice device = deviceList.get(position);
        long[] dailyTotals = new long[]{0, 0};
        if (deviceActivityMap.containsKey(device.getAddress())) {
            dailyTotals = deviceActivityMap.get(device.getAddress());
        }

        final DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(device);

        holder.container.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                
                if (device.isInitialized() || device.isConnected()) {
                    showTransientSnackbar(R.string.controlcenter_snackbar_need_longpress);
                } else {
                    showTransientSnackbar(R.string.controlcenter_snackbar_connecting);
                    GBApplication.deviceService().connect(device);
                }
            }
        });

        holder.container.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (device.getState() != GBDevice.State.NOT_CONNECTED) {
                    showTransientSnackbar(R.string.controlcenter_snackbar_disconnecting);
                    GBApplication.deviceService().disconnect();
                }
                return true;
            }
        });
        holder.deviceImageView.setImageResource(device.isInitialized() ? device.getType().getIcon() : device.getType().getDisabledIcon());

        holder.deviceNameLabel.setText(getUniqueDeviceName(device));

        if (device.isBusy()) {
            holder.deviceStatusLabel.setText(device.getBusyTask());
            holder.busyIndicator.setVisibility(View.VISIBLE);
        } else {
            holder.deviceStatusLabel.setText(device.getStateString());
            holder.busyIndicator.setVisibility(View.INVISIBLE);
        }

        //begin of action row
        //battery
        // multiple battery support: at this point we support up to three batteries
        // to support more batteries, the battery UI would need to be extended

        holder.batteryStatusBox0.setVisibility(coordinator.getBatteryCount() > 0 ? View.VISIBLE : View.GONE);
        holder.batteryStatusBox1.setVisibility(coordinator.getBatteryCount() > 1 ? View.VISIBLE : View.GONE);
        holder.batteryStatusBox2.setVisibility(coordinator.getBatteryCount() > 2 ? View.VISIBLE : View.GONE);

        LinearLayout[] batteryStatusBoxes = {holder.batteryStatusBox0, holder.batteryStatusBox1, holder.batteryStatusBox2};
        TextView[] batteryStatusLabels = {holder.batteryStatusLabel0, holder.batteryStatusLabel1, holder.batteryStatusLabel2};
        ImageView[] batteryIcons = {holder.batteryIcon0, holder.batteryIcon1, holder.batteryIcon2};

        for (int batteryIndex = 0; batteryIndex < coordinator.getBatteryCount(); batteryIndex++) {

            int batteryLevel = device.getBatteryLevel(batteryIndex);
            float batteryVoltage = device.getBatteryVoltage(batteryIndex);
            BatteryState batteryState = device.getBatteryState(batteryIndex);
            int batteryIcon = device.getBatteryIcon(batteryIndex);
            int batteryLabel = device.getBatteryLabel(batteryIndex); //unused for now
            batteryIcons[batteryIndex].setImageResource(R.drawable.level_list_battery);

            if (batteryIcon != GBDevice.BATTERY_ICON_DEFAULT){
                batteryIcons[batteryIndex].setImageResource(batteryIcon);
            }

            if (batteryLevel != GBDevice.BATTERY_UNKNOWN) {
                batteryStatusLabels[batteryIndex].setText(device.getBatteryLevel(batteryIndex) + "%");
                if (BatteryState.BATTERY_CHARGING.equals(batteryState) ||
                        BatteryState.BATTERY_CHARGING_FULL.equals(batteryState)) {
                    batteryIcons[batteryIndex].setImageLevel(device.getBatteryLevel(batteryIndex) + 100);
                } else {
                    batteryIcons[batteryIndex].setImageLevel(device.getBatteryLevel(batteryIndex));
                }
            } else if (BatteryState.NO_BATTERY.equals(batteryState) && batteryVoltage != GBDevice.BATTERY_UNKNOWN) {
                batteryStatusLabels[batteryIndex].setText(String.format(Locale.getDefault(), "%.2f", batteryVoltage));
                batteryIcons[batteryIndex].setImageLevel(200);
            } else {
                //should be the "default" status, shown when the device is not connected
                batteryStatusLabels[batteryIndex].setText("");
                batteryIcons[batteryIndex].setImageLevel(50);
            }
            final int finalBatteryIndex = batteryIndex;
            batteryStatusBoxes[batteryIndex].setOnClickListener(new View.OnClickListener() {
                                                               @Override
                                                               public void onClick(View v) {
                                                                   Intent startIntent;
                                                                   startIntent = new Intent(context, BatteryInfoActivity.class);
                                                                   startIntent.putExtra(GBDevice.EXTRA_DEVICE, device);
                                                                   startIntent.putExtra(GBDevice.BATTERY_INDEX, finalBatteryIndex);
                                                                   context.startActivity(startIntent);
                                                               }
                                                           }
            );

        }
        holder.heartRateStatusBox.setVisibility((device.isInitialized() && coordinator.supportsRealtimeData() && coordinator.supportsHeartRateMeasurement(device)) ? View.VISIBLE : View.GONE);
        if (parent.getContext() instanceof ControlCenterv2) {
            ActivitySample sample = ((ControlCenterv2) parent.getContext()).getCurrentHRSample();
            if (sample != null) {
                holder.heartRateStatusLabel.setText(String.valueOf(sample.getHeartRate()));
            } else {
                holder.heartRateStatusLabel.setText("");
            }
        }

        holder.heartRateStatusBox.setOnClickListener(new View.OnClickListener() {
                                                         @Override
                                                         public void onClick(View v) {
                                                             GBApplication.deviceService().onHeartRateTest();
                                                             HeartRateDialog dialog = new HeartRateDialog(context);
                                                             dialog.show();
                                                         }
                                                     }
        );

        //device specific settings
        holder.deviceSpecificSettingsView.setVisibility(coordinator.getSupportedDeviceSpecificSettings(device) != null ? View.VISIBLE : View.GONE);
        holder.deviceSpecificSettingsView.setOnClickListener(new View.OnClickListener()

                                                {
                                                    @Override
                                                    public void onClick(View v) {
                                                        Intent startIntent;
                                                        startIntent = new Intent(context, DeviceSettingsActivity.class);
                                                        startIntent.putExtra(GBDevice.EXTRA_DEVICE, device);
                                                        context.startActivity(startIntent);
                                                    }
                                                }
        );

        //fetch activity data
        holder.fetchActivityDataBox.setVisibility((device.isInitialized() && coordinator.supportsActivityDataFetching()) ? View.VISIBLE : View.GONE);
        holder.fetchActivityData.setOnClickListener(new View.OnClickListener()

                                                    {
                                                        @Override
                                                        public void onClick(View v) {
                                                            showTransientSnackbar(R.string.busy_task_fetch_activity_data);
                                                            GBApplication.deviceService().onFetchRecordedData(RecordedDataTypes.TYPE_ACTIVITY);
                                                        }
                                                    }
        );


        //take screenshot
        holder.takeScreenshotView.setVisibility((device.isInitialized() && coordinator.supportsScreenshots()) ? View.VISIBLE : View.GONE);
        holder.takeScreenshotView.setOnClickListener(new View.OnClickListener()

                                                     {
                                                         @Override
                                                         public void onClick(View v) {
                                                             showTransientSnackbar(R.string.controlcenter_snackbar_requested_screenshot);
                                                             GBApplication.deviceService().onScreenshotReq();
                                                         }
                                                     }
        );

        //manage apps
        holder.manageAppsView.setVisibility((device.isInitialized() && coordinator.supportsAppsManagement()) ? View.VISIBLE : View.GONE);
        holder.manageAppsView.setOnClickListener(new View.OnClickListener()

                                                 {
                                                     @Override
                                                     public void onClick(View v) {
                                                         DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(device);
                                                         Class<? extends Activity> appsManagementActivity = coordinator.getAppsManagementActivity();
                                                         if (appsManagementActivity != null) {
                                                             Intent startIntent = new Intent(context, appsManagementActivity);
                                                             startIntent.putExtra(GBDevice.EXTRA_DEVICE, device);
                                                             context.startActivity(startIntent);
                                                         }
                                                     }
                                                 }
        );

        //set alarms
        holder.setAlarmsView.setVisibility(coordinator.getAlarmSlotCount() > 0 ? View.VISIBLE : View.GONE);
        holder.setAlarmsView.setOnClickListener(new View.OnClickListener()

                                                {
                                                    @Override
                                                    public void onClick(View v) {
                                                        Intent startIntent;
                                                        startIntent = new Intent(context, ConfigureAlarms.class);
                                                        startIntent.putExtra(GBDevice.EXTRA_DEVICE, device);
                                                        context.startActivity(startIntent);
                                                    }
                                                }
        );

        //show graphs
        holder.showActivityGraphs.setVisibility(coordinator.supportsActivityTracking() ? View.VISIBLE : View.GONE);
        holder.showActivityGraphs.setOnClickListener(new View.OnClickListener()

                                                     {
                                                         @Override
                                                         public void onClick(View v) {
                                                             Intent startIntent;
                                                             startIntent = new Intent(context, ChartsActivity.class);
                                                             startIntent.putExtra(GBDevice.EXTRA_DEVICE, device);
                                                             context.startActivity(startIntent);
                                                         }
                                                     }
        );

        //show activity tracks
        holder.showActivityTracks.setVisibility(coordinator.supportsActivityTracks() ? View.VISIBLE : View.GONE);
        holder.showActivityTracks.setOnClickListener(new View.OnClickListener()
                                                     {
                                                         @Override
                                                         public void onClick(View v) {
                                                             Intent startIntent;
                                                             startIntent = new Intent(context, ActivitySummariesActivity.class);
                                                             startIntent.putExtra(GBDevice.EXTRA_DEVICE, device);
                                                             context.startActivity(startIntent);
                                                         }
                                                     }
        );

        ItemWithDetailsAdapter infoAdapter = new ItemWithDetailsAdapter(context, device.getDeviceInfos());
        infoAdapter.setHorizontalAlignment(true);
        holder.deviceInfoList.setAdapter(infoAdapter);
        justifyListViewHeightBasedOnChildren(holder.deviceInfoList);
        holder.deviceInfoList.setFocusable(false);

        final boolean detailsShown = position == expandedDevicePosition;
        boolean showInfoIcon = device.hasDeviceInfos() && !device.isBusy();
        holder.deviceInfoView.setVisibility(showInfoIcon ? View.VISIBLE : View.GONE);
        holder.deviceInfoBox.setActivated(detailsShown);
        holder.deviceInfoBox.setVisibility(detailsShown ? View.VISIBLE : View.GONE);
        holder.deviceInfoView.setOnClickListener(new View.OnClickListener() {
                                                     @Override
                                                     public void onClick(View v) {
                                                         expandedDevicePosition = detailsShown ? -1 : position;
                                                         TransitionManager.beginDelayedTransition(parent);
                                                         notifyDataSetChanged();
                                                     }
                                                 }

        );

        holder.findDevice.setVisibility(device.isInitialized() && coordinator.supportsFindDevice() ? View.VISIBLE : View.GONE);
        holder.findDevice.setOnClickListener(new View.OnClickListener() {
                                                 @Override
                                                 public void onClick(View v) {
                                                     new AlertDialog.Builder(context)
                                                             .setCancelable(true)
                                                             .setTitle(context.getString(R.string.controlcenter_find_device))
                                                             .setMessage(context.getString(R.string.find_lost_device_message, device.getName()))
                                                             .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                                                 @Override
                                                                 public void onClick(DialogInterface dialog, int which) {
                                                                     if (device.getType() == DeviceType.VIBRATISSIMO) {
                                                                         Intent startIntent;
                                                                         startIntent = new Intent(context, VibrationActivity.class);
                                                                         startIntent.putExtra(GBDevice.EXTRA_DEVICE, device);
                                                                         context.startActivity(startIntent);
                                                                         return;
                                                                     }
                                                                     GBApplication.deviceService().onFindDevice(true);
                                                                     Snackbar.make(parent, R.string.control_center_find_lost_device, Snackbar.LENGTH_INDEFINITE).setAction(R.string.find_lost_device_you_found_it, new View.OnClickListener() {
                                                                         @Override
                                                                         public void onClick(View v) {
                                                                             GBApplication.deviceService().onFindDevice(false);
                                                                         }
                                                                     }).setCallback(new Snackbar.Callback() {
                                                                         @Override
                                                                         public void onDismissed(Snackbar snackbar, int event) {
                                                                             GBApplication.deviceService().onFindDevice(false);
                                                                             super.onDismissed(snackbar, event);
                                                                         }
                                                                     }).show();

                                                                 }
                                                             })
                                                             .setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                                                                 @Override
                                                                 public void onClick(DialogInterface dialog, int which) {
                                                                     // do nothing
                                                                 }
                                                             })
                                                             .show();
//                                                             ProgressDialog.show(
//                                                             context,
//                                                             context.getString(R.string.control_center_find_lost_device),
//                                                             context.getString(R.string.control_center_cancel_to_stop_vibration),
//                                                             true, true,
//                                                             new DialogInterface.OnCancelListener() {
//                                                                 @Override
//                                                                 public void onCancel(DialogInterface dialog) {
//                                                                     GBApplication.deviceService().onFindDevice(false);
//                                                                 }
//                                                             });
                                                 }
                                             }
        );

        holder.calibrateDevice.setVisibility(device.isInitialized() && (coordinator.getCalibrationActivity() != null) ? View.VISIBLE : View.GONE);
        holder.calibrateDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent startIntent = new Intent(context, coordinator.getCalibrationActivity());
                startIntent.putExtra(GBDevice.EXTRA_DEVICE, device);
                context.startActivity(startIntent);
            }
        });

        holder.fmFrequencyBox.setVisibility(View.GONE);
        if (device.isInitialized() && device.getExtraInfo("fm_frequency") != null) {
            holder.fmFrequencyBox.setVisibility(View.VISIBLE);
            holder.fmFrequencyLabel.setText(String.format(Locale.getDefault(), "%.1f", (float) device.getExtraInfo("fm_frequency")));
        }
        final TextView fmFrequencyLabel = holder.fmFrequencyLabel;
        holder.fmFrequencyBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(R.string.preferences_fm_frequency);

                final EditText input = new EditText(context);

                input.setSelection(input.getText().length());
                input.setRawInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                input.setText(String.format(Locale.getDefault(), "%.1f", (float) device.getExtraInfo("fm_frequency")));
                builder.setView(input);

                builder.setPositiveButton(context.getResources().getString(android.R.string.ok),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                float frequency = Float.parseFloat(input.getText().toString());
                                // Trim to 1 decimal place, discard the rest
                                frequency = Float.parseFloat(String.format(Locale.getDefault(), "%.1f", frequency));
                                if (frequency < 87.5 || frequency > 108.0) {
                                    new AlertDialog.Builder(context)
                                            .setTitle(R.string.pref_invalid_frequency_title)
                                            .setMessage(R.string.pref_invalid_frequency_message)
                                            .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int which) {
                                                }
                                            })
                                            .show();
                                } else {
                                    device.setExtraInfo("fm_frequency", frequency);
                                    fmFrequencyLabel.setText(String.format(Locale.getDefault(), "%.1f", (float) device.getExtraInfo("fm_frequency")));
                                    GBApplication.deviceService().onSetFmFrequency(frequency);
                                }
                            }
                        });
                builder.setNegativeButton(context.getResources().getString(R.string.Cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();
            }
        });

        holder.ledColor.setVisibility(View.GONE);
        if (device.isInitialized() && device.getExtraInfo("led_color") != null && coordinator.supportsLedColor()) {
            holder.ledColor.setVisibility(View.VISIBLE);
            final GradientDrawable ledColor = (GradientDrawable) holder.ledColor.getDrawable().mutate();
            ledColor.setColor((int) device.getExtraInfo("led_color"));
            holder.ledColor.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ColorPickerDialog.Builder builder = ColorPickerDialog.newBuilder();
                    builder.setDialogTitle(R.string.preferences_led_color);

                    int[] presets = coordinator.getColorPresets();

                    builder.setColor((int) device.getExtraInfo("led_color"));
                    builder.setShowAlphaSlider(false);
                    builder.setShowColorShades(false);
                    if (coordinator.supportsRgbLedColor()) {
                        builder.setAllowCustom(true);
                        if (presets.length == 0) {
                            builder.setDialogType(ColorPickerDialog.TYPE_CUSTOM);
                        }
                    } else {
                        builder.setAllowCustom(false);
                    }

                    if (presets.length > 0) {
                        builder.setAllowPresets(true);
                        builder.setPresets(presets);
                    }

                    ColorPickerDialog dialog = builder.create();
                    dialog.setColorPickerDialogListener(new ColorPickerDialogListener() {
                        @Override
                        public void onColorSelected(int dialogId, int color) {
                            ledColor.setColor(color);
                            device.setExtraInfo("led_color", color);
                            GBApplication.deviceService().onSetLedColor(color);
                        }

                        @Override
                        public void onDialogDismissed(int dialogId) {
                            // Nothing to do
                        }
                    });
                    dialog.show(((Activity) context).getFragmentManager(), "color-picker-dialog");
                }
            });
        }

        //remove device, hidden under details
        holder.removeDevice.setOnClickListener(new View.OnClickListener()

        {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(context)
                        .setCancelable(true)
                        .setTitle(context.getString(R.string.controlcenter_delete_device_name, device.getName()))
                        .setMessage(R.string.controlcenter_delete_device_dialogmessage)
                        .setPositiveButton(R.string.Delete, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(device);
                                    if (coordinator != null) {
                                        coordinator.deleteDevice(device);
                                    }
                                    DeviceHelper.getInstance().removeBond(device);
                                } catch (Exception ex) {
                                    GB.toast(context, "Error deleting device: " + ex.getMessage(), Toast.LENGTH_LONG, GB.ERROR, ex);
                                } finally {
                                    Intent refreshIntent = new Intent(DeviceManager.ACTION_REFRESH_DEVICELIST);
                                    LocalBroadcastManager.getInstance(context).sendBroadcast(refreshIntent);
                                }
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
        });

        //set alias, hidden under details
        holder.setAlias.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                final EditText input = new EditText(context);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                input.setText(device.getAlias());
                FrameLayout container = new FrameLayout(context);
                FrameLayout.LayoutParams params = new  FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.leftMargin = context.getResources().getDimensionPixelSize(R.dimen.dialog_margin);
                params.rightMargin = context.getResources().getDimensionPixelSize(R.dimen.dialog_margin);
                input.setLayoutParams(params);
                container.addView(input);
                // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text

                new AlertDialog.Builder(context)
                        .setView(container)
                        .setCancelable(true)
                        .setTitle(context.getString(R.string.controlcenter_set_alias))
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try (DBHandler dbHandler = GBApplication.acquireDB()) {
                                    DaoSession session = dbHandler.getDaoSession();
                                    Device dbDevice = DBHelper.getDevice(device, session);
                                    String alias = input.getText().toString();
                                    dbDevice.setAlias(alias);
                                    dbDevice.update();
                                    device.setAlias(alias);
                                } catch (Exception ex) {
                                    GB.toast(context, context.getString(R.string.error_setting_alias) + ex.getMessage(), Toast.LENGTH_LONG, GB.ERROR, ex);
                                } finally {
                                    Intent refreshIntent = new Intent(DeviceManager.ACTION_REFRESH_DEVICELIST);
                                    LocalBroadcastManager.getInstance(context).sendBroadcast(refreshIntent);
                                }
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
        });

        holder.cardViewActivityCardLayout.setVisibility(coordinator.supportsActivityTracking() ? View.VISIBLE : View.GONE);
        holder.cardViewActivityCardLayout.setMinimumWidth(coordinator.supportsActivityTracking() ? View.VISIBLE : View.GONE);
        holder.cardViewActivityCardLayout.setOnClickListener(new View.OnClickListener() {
                                                                 @Override
                                                                 public void onClick(View v) {
                                                                     Intent startIntent;
                                                                     startIntent = new Intent(context, ChartsActivity.class);
                                                                     startIntent.putExtra(GBDevice.EXTRA_DEVICE, device);
                                                                     context.startActivity(startIntent);
                                                                 }
                                                             }
        );
        if (coordinator.supportsActivityDataFetching()) {
            setActivityCard(holder, device, dailyTotals);
        }
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        CardView container;

        ImageView deviceImageView;
        TextView deviceNameLabel;
        TextView deviceStatusLabel;

        //actions
        LinearLayout batteryStatusBox0;
        TextView batteryStatusLabel0;
        ImageView batteryIcon0;
        LinearLayout batteryStatusBox1;
        TextView batteryStatusLabel1;
        ImageView batteryIcon1;
        LinearLayout batteryStatusBox2;
        TextView batteryStatusLabel2;
        ImageView batteryIcon2;
        ImageView deviceSpecificSettingsView;
        LinearLayout fetchActivityDataBox;
        ImageView fetchActivityData;
        ProgressBar busyIndicator;
        ImageView takeScreenshotView;
        ImageView manageAppsView;
        ImageView setAlarmsView;
        ImageView showActivityGraphs;
        ImageView showActivityTracks;
        ImageView calibrateDevice;
        LinearLayout heartRateStatusBox;
        ImageView heartRateIcon;
        TextView heartRateStatusLabel;


        ImageView deviceInfoView;
        //overflow
        final RelativeLayout deviceInfoBox;
        ListView deviceInfoList;
        ImageView findDevice;
        ImageView removeDevice;
        ImageView setAlias;
        LinearLayout fmFrequencyBox;
        TextView fmFrequencyLabel;
        ImageView ledColor;

        LinearLayout cardViewActivityCardLayout;
        LinearLayout cardViewActivityCardStepsLayout;
        LinearLayout cardViewActivityCardSleepLayout;
        LinearLayout cardViewActivityCardDistanceLayout;
        TextView cardViewActivityCardSteps;
        TextView cardViewActivityCardDistance;
        TextView cardViewActivityCardSleep;
        ProgressBar cardViewActivityCardStepsProgress;
        ProgressBar cardViewActivityCardDistanceProgress;
        ProgressBar cardViewActivityCardSleepProgress;

        ViewHolder(View view) {
            super(view);

            container = view.findViewById(R.id.card_view);

            deviceImageView = view.findViewById(R.id.device_image);
            deviceNameLabel = view.findViewById(R.id.device_name);
            deviceStatusLabel = view.findViewById(R.id.device_status);

            //actions
            batteryStatusBox0 = view.findViewById(R.id.device_battery_status_box);
            batteryStatusLabel0 = view.findViewById(R.id.battery_status);
            batteryIcon0 = view.findViewById(R.id.device_battery_status);
            batteryStatusBox1 = view.findViewById(R.id.device_battery_status_box1);
            batteryStatusLabel1 = view.findViewById(R.id.battery_status1);
            batteryIcon1 = view.findViewById(R.id.device_battery_status1);
            batteryStatusBox2 = view.findViewById(R.id.device_battery_status_box2);
            batteryStatusLabel2 = view.findViewById(R.id.battery_status2);
            batteryIcon2 = view.findViewById(R.id.device_battery_status2);



            deviceSpecificSettingsView = view.findViewById(R.id.device_specific_settings);
            fetchActivityDataBox = view.findViewById(R.id.device_action_fetch_activity_box);
            fetchActivityData = view.findViewById(R.id.device_action_fetch_activity);
            busyIndicator = view.findViewById(R.id.device_busy_indicator);
            takeScreenshotView = view.findViewById(R.id.device_action_take_screenshot);
            manageAppsView = view.findViewById(R.id.device_action_manage_apps);
            setAlarmsView = view.findViewById(R.id.device_action_set_alarms);
            showActivityGraphs = view.findViewById(R.id.device_action_show_activity_graphs);
            showActivityTracks = view.findViewById(R.id.device_action_show_activity_tracks);
            deviceInfoView = view.findViewById(R.id.device_info_image);
            calibrateDevice = view.findViewById(R.id.device_action_calibrate);

            deviceInfoBox = view.findViewById(R.id.device_item_infos_box);
            //overflow
            deviceInfoList = view.findViewById(R.id.device_item_infos);
            findDevice = view.findViewById(R.id.device_action_find);
            removeDevice = view.findViewById(R.id.device_action_remove);
            setAlias = view.findViewById(R.id.device_action_set_alias);
            fmFrequencyBox = view.findViewById(R.id.device_fm_frequency_box);
            fmFrequencyLabel = view.findViewById(R.id.fm_frequency);
            ledColor = view.findViewById(R.id.device_led_color);
            heartRateStatusBox = view.findViewById(R.id.device_heart_rate_status_box);
            heartRateStatusLabel = view.findViewById(R.id.heart_rate_status);
            heartRateIcon = view.findViewById(R.id.device_heart_rate_status);
            
            cardViewActivityCardLayout = view.findViewById(R.id.card_view_activity_card_layout);
            cardViewActivityCardStepsLayout = view.findViewById(R.id.card_view_activity_card_steps_layout);
            cardViewActivityCardSleepLayout = view.findViewById(R.id.card_view_activity_card_sleep_layout);
            cardViewActivityCardDistanceLayout = view.findViewById(R.id.card_view_activity_card_distance_layout);

            cardViewActivityCardSteps = view.findViewById(R.id.card_view_activity_card_steps);
            cardViewActivityCardDistance = view.findViewById(R.id.card_view_activity_card_distance);
            cardViewActivityCardSleep = view.findViewById(R.id.card_view_activity_card_sleep);
            cardViewActivityCardStepsProgress = view.findViewById(R.id.card_view_activity_card_steps_progress);
            cardViewActivityCardDistanceProgress = view.findViewById(R.id.card_view_activity_card_distance_progress);
            cardViewActivityCardSleepProgress = view.findViewById(R.id.card_view_activity_card_sleep_progress);

        }

    }

    private void justifyListViewHeightBasedOnChildren(ListView listView) {
        ArrayAdapter adapter = (ArrayAdapter) listView.getAdapter();

        if (adapter == null) {
            return;
        }
        int totalHeight = 0;
        for (int i = 0; i < adapter.getCount(); i++) {
            View listItem = adapter.getView(i, null, listView);
            listItem.measure(0, 0);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams par = listView.getLayoutParams();
        par.height = totalHeight + (listView.getDividerHeight() * (adapter.getCount() - 1));
        listView.setLayoutParams(par);
        listView.requestLayout();
    }

    private String getUniqueDeviceName(GBDevice device) {
        String deviceName = device.getAliasOrName();

        if (!isUniqueDeviceName(device, deviceName)) {
            if (device.getModel() != null) {
                deviceName = deviceName + " " + device.getModel();
                if (!isUniqueDeviceName(device, deviceName)) {
                    deviceName = deviceName + " " + device.getShortAddress();
                }
            } else {
                deviceName = deviceName + " " + device.getShortAddress();
            }
        }
        return deviceName;
    }

    private boolean isUniqueDeviceName(GBDevice device, String deviceName) {
        for (int i = 0; i < deviceList.size(); i++) {
            GBDevice item = deviceList.get(i);
            if (item == device) {
                continue;
            }
            if (deviceName.equals(item.getName())) {
                return false;
            }
        }
        return true;
    }

    private void showTransientSnackbar(int resource) {
        Snackbar snackbar = Snackbar.make(parent, resource, Snackbar.LENGTH_SHORT);

        //View snackbarView = snackbar.getView();

        // change snackbar text color
        //int snackbarTextId = android.support.design.R.id.snackbar_text;
        //TextView textView = snackbarView.findViewById(snackbarTextId);
        //textView.setTextColor();
        //snackbarView.setBackgroundColor(Color.MAGENTA);
        snackbar.show();
    }

    private void setActivityCard(ViewHolder holder, GBDevice device, long[] dailyTotals) {
        int steps = (int) dailyTotals[0];
        int sleep = (int) dailyTotals[1];
        ActivityUser activityUser = new ActivityUser();
        int stepGoal = activityUser.getStepsGoal();
        int sleepGoal = activityUser.getSleepDuration();
        int sleepGoalMinutes = sleepGoal * 60;
        int distanceGoal = activityUser.getDistanceMeters() * 100;
        int stepLength = activityUser.getStepLengthCm();
        double distanceMeters = dailyTotals[0] * stepLength / 100;
        double distanceFeet = distanceMeters * 3.28084f;
        double distanceFormatted = 0;

        String unit = "###m";
        distanceFormatted = distanceMeters;
        if (distanceMeters > 2000) {
            distanceFormatted = distanceMeters / 1000;
            unit = "###.#km";
        }
        String units = GBApplication.getPrefs().getString(SettingsActivity.PREF_MEASUREMENT_SYSTEM, GBApplication.getContext().getString(R.string.p_unit_metric));
        if (units.equals(GBApplication.getContext().getString(R.string.p_unit_imperial))) {
            unit = "###ft";
            distanceFormatted = distanceFeet;
            if (distanceFeet > 6000) {
                distanceFormatted = distanceFeet * 0.0001893939f;
                unit = "###.#mi";
            }
        }
        DecimalFormat df = new DecimalFormat(unit);


        holder.cardViewActivityCardSteps.setText(String.format("%1s", steps));
        holder.cardViewActivityCardSleep.setText(String.format("%1s", getHM(sleep)));
        holder.cardViewActivityCardDistance.setText(df.format(distanceFormatted));

        holder.cardViewActivityCardStepsProgress.setMax(stepGoal);
        holder.cardViewActivityCardStepsProgress.setProgress(steps);

        holder.cardViewActivityCardSleepProgress.setMax(sleepGoalMinutes);
        holder.cardViewActivityCardSleepProgress.setProgress(sleep);

        holder.cardViewActivityCardDistanceProgress.setMax(distanceGoal);
        holder.cardViewActivityCardDistanceProgress.setProgress(steps * stepLength);

        boolean showActivityCard = GBApplication.getDeviceSpecificSharedPrefs(device.getAddress()).getBoolean(DeviceSettingsPreferenceConst.PREFS_ACTIVITY_IN_DEVICE_CARD, true);
        holder.cardViewActivityCardLayout.setVisibility(showActivityCard ? View.VISIBLE : View.GONE);

        boolean showActivitySteps = GBApplication.getDeviceSpecificSharedPrefs(device.getAddress()).getBoolean(DeviceSettingsPreferenceConst.PREFS_ACTIVITY_IN_DEVICE_CARD_STEPS, true);
        holder.cardViewActivityCardStepsLayout.setVisibility(showActivitySteps ? View.VISIBLE : View.GONE);

        boolean showActivitySleep = GBApplication.getDeviceSpecificSharedPrefs(device.getAddress()).getBoolean(DeviceSettingsPreferenceConst.PREFS_ACTIVITY_IN_DEVICE_CARD_SLEEP, true);
        holder.cardViewActivityCardSleepLayout.setVisibility(showActivitySleep ? View.VISIBLE : View.GONE);

        boolean showActivityDistance = GBApplication.getDeviceSpecificSharedPrefs(device.getAddress()).getBoolean(DeviceSettingsPreferenceConst.PREFS_ACTIVITY_IN_DEVICE_CARD_DISTANCE, true);
        holder.cardViewActivityCardDistanceLayout.setVisibility(showActivityDistance ? View.VISIBLE : View.GONE);

    }

    private String getHM(long value) {
        return DateTimeUtils.formatDurationHoursMinutes(value, TimeUnit.MINUTES);
    }
}
