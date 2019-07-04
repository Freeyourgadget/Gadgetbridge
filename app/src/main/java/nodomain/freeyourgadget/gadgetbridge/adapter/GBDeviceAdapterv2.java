/*  Copyright (C) 2015-2019 Andreas Shimokawa, Carsten Pfeiffer, Daniele
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.jaredrummler.android.colorpicker.ColorPickerDialog;
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener;

import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.ActivitySummariesActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.ConfigureAlarms;
import nodomain.freeyourgadget.gadgetbridge.activities.VibrationActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.ChartsActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager;
import nodomain.freeyourgadget.gadgetbridge.devices.watch9.Watch9CalibrationActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

/**
 * Adapter for displaying GBDevice instances.
 */
public class GBDeviceAdapterv2 extends RecyclerView.Adapter<GBDeviceAdapterv2.ViewHolder> {

    private final Context context;
    private List<GBDevice> deviceList;
    private int expandedDevicePosition = RecyclerView.NO_POSITION;
    private ViewGroup parent;

    public GBDeviceAdapterv2(Context context, List<GBDevice> deviceList) {
        this.context = context;
        this.deviceList = deviceList;
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
        holder.batteryStatusBox.setVisibility(View.GONE);
        short batteryLevel = device.getBatteryLevel();
        float batteryVoltage = device.getBatteryVoltage();
        BatteryState batteryState = device.getBatteryState();

        if (batteryLevel != GBDevice.BATTERY_UNKNOWN) {
            holder.batteryStatusBox.setVisibility(View.VISIBLE);
            holder.batteryStatusLabel.setText(device.getBatteryLevel() + "%");
            if (BatteryState.BATTERY_CHARGING.equals(batteryState) ||
                    BatteryState.BATTERY_CHARGING_FULL.equals(batteryState)) {
                holder.batteryIcon.setImageLevel(device.getBatteryLevel() + 100);
            } else {
                holder.batteryIcon.setImageLevel(device.getBatteryLevel());
            }
        } else if (BatteryState.NO_BATTERY.equals(batteryState) && batteryVoltage != GBDevice.BATTERY_UNKNOWN) {
            holder.batteryStatusBox.setVisibility(View.VISIBLE);
            holder.batteryStatusLabel.setText(String.format(Locale.getDefault(), "%.2f", batteryVoltage));
            holder.batteryIcon.setImageLevel(200);
        }

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
        holder.findDevice.setOnClickListener(new View.OnClickListener()

                                             {
                                                 @Override
                                                 public void onClick(View v) {
                                                     if (device.getType() == DeviceType.VIBRATISSIMO) {
                                                         Intent startIntent;
                                                         startIntent = new Intent(context, VibrationActivity.class);
                                                         startIntent.putExtra(GBDevice.EXTRA_DEVICE, device);
                                                         context.startActivity(startIntent);
                                                         return;
                                                     }
                                                     GBApplication.deviceService().onFindDevice(true);
                                                     //TODO: extract string resource if we like this solution.
                                                     Snackbar.make(parent, R.string.control_center_find_lost_device, Snackbar.LENGTH_INDEFINITE).setAction("Found it!", new View.OnClickListener() {
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
//                                                     ProgressDialog.show(
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

        holder.calibrateDevice.setVisibility(device.isInitialized() && device.getType() == DeviceType.WATCH9 ? View.VISIBLE : View.GONE);
        holder.calibrateDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent startIntent = new Intent(context, Watch9CalibrationActivity.class);
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
                                float frequency = Float.valueOf(input.getText().toString());
                                // Trim to 1 decimal place, discard the rest
                                frequency = Float.valueOf(String.format(Locale.getDefault(), "%.1f", frequency));
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
        LinearLayout batteryStatusBox;
        TextView batteryStatusLabel;
        ImageView batteryIcon;
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

        ImageView deviceInfoView;
        //overflow
        final RelativeLayout deviceInfoBox;
        ListView deviceInfoList;
        ImageView findDevice;
        ImageView removeDevice;
        LinearLayout fmFrequencyBox;
        TextView fmFrequencyLabel;
        ImageView ledColor;

        ViewHolder(View view) {
            super(view);
            container = view.findViewById(R.id.card_view);

            deviceImageView = view.findViewById(R.id.device_image);
            deviceNameLabel = view.findViewById(R.id.device_name);
            deviceStatusLabel = view.findViewById(R.id.device_status);

            //actions
            batteryStatusBox = view.findViewById(R.id.device_battery_status_box);
            batteryStatusLabel = view.findViewById(R.id.battery_status);
            batteryIcon = view.findViewById(R.id.device_battery_status);
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
            fmFrequencyBox = view.findViewById(R.id.device_fm_frequency_box);
            fmFrequencyLabel = view.findViewById(R.id.fm_frequency);
            ledColor = view.findViewById(R.id.device_led_color);
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
        String deviceName = device.getName();
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

}
