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
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.ArraySet;
import android.util.Pair;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.MPPointF;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.snackbar.Snackbar;
import com.jaredrummler.android.colorpicker.ColorPickerDialog;
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.ActivitySummariesActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.BatteryInfoActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.ConfigureAlarms;
import nodomain.freeyourgadget.gadgetbridge.activities.ConfigureReminders;
import nodomain.freeyourgadget.gadgetbridge.activities.ControlCenterv2;
import nodomain.freeyourgadget.gadgetbridge.activities.HeartRateDialog;
import nodomain.freeyourgadget.gadgetbridge.activities.OpenFwAppInstallerActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.VibrationActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.ChartsActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceFolder;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.FormatUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

/**
 * Adapter for displaying GBDevice instances.
 */
public class GBDeviceAdapterv2 extends ListAdapter<GBDevice, GBDeviceAdapterv2.ViewHolder> {
    private static final Logger LOG = LoggerFactory.getLogger(GBDeviceAdapterv2.class);

    private final Context context;
    private List<GBDevice> deviceList;
    private List<GBDevice> devicesListWithFolders;
    private String expandedDeviceAddress = "";
    private String expandedFolderName = "";
    private ViewGroup parent;
    private HashMap<String, long[]> deviceActivityMap = new HashMap();
    private StableIdGenerator idGenerator = new StableIdGenerator();

    public GBDeviceAdapterv2(Context context, List<GBDevice> deviceList, HashMap<String,long[]> deviceMap) {
        super(new GBDeviceDiffUtil());
        this.context = context;
        this.deviceList = deviceList;
        rebuildFolders();
        this.deviceActivityMap = deviceMap;
    }

    public void rebuildFolders(){
        this.devicesListWithFolders = enrichDeviceListWithFolder(deviceList);
    }

    private List<GBDevice> enrichDeviceListWithFolder(List<GBDevice> deviceList) {
        ArrayList<GBDevice> enrichedList = new ArrayList<>();
        Set<String> folders = new ArraySet<>();
        for(GBDevice device : deviceList){
            String parentFolder = device.getParentFolder();
            if(StringUtils.isNullOrEmpty(parentFolder)){
                enrichedList.add(device);
                continue;
            }
            folders.add(parentFolder);
        }

        for(String folder : folders){
            enrichedList.add(new GBDeviceFolder(folder));
            for(GBDevice potentialChild : deviceList){
                String parentFolder = potentialChild.getParentFolder();
                if(StringUtils.isNullOrEmpty(parentFolder)){
                    continue;
                }
                if(!parentFolder.equals(folder)){
                    continue;
                }
                enrichedList.add(potentialChild);
            }
        }

        return enrichedList;
    }

    @NonNull
    @Override
    public GBDeviceAdapterv2.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.parent = parent;
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_itemv2, parent, false);
        return new ViewHolder(view);
    }

    private int countDevicesInFolder(String folderName, boolean needsToBeConnected){
        int count = 0;
        for(GBDevice device : deviceList){
            if(folderName.equals(device.getParentFolder()) && ((!needsToBeConnected) || device.isConnected())){
                count++;
            }
        }
        return count;
    }

    private void showDeviceFolder(ViewHolder holder, final GBDeviceFolder folder){
        holder.container.setVisibility(View.VISIBLE);
        holder.deviceNameLabel.setText(folder.getName());
        holder.infoIcons.setVisibility(View.GONE);
        holder.deviceInfoBox.setVisibility(View.GONE);
        holder.cardViewActivityCardLayout.setVisibility(View.GONE);
        if(countDevicesInFolder(folder.getName(), true) == 0){
            holder.deviceImageView.setImageResource(R.drawable.ic_device_folder_disabled);
        }else{

            holder.deviceImageView.setImageResource(R.drawable.ic_device_folder);
        }
        holder.deviceInfoView.setVisibility(View.GONE);
        int countInFolder = countDevicesInFolder(folder.getName(), false);
        int connectedInFolder = countDevicesInFolder(folder.getName(), true);
        holder.deviceStatusLabel.setText(context.getString(R.string.controlcenter_connected_fraction, connectedInFolder, countInFolder));

        holder.container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(expandedFolderName.equals(folder.getName())){
                    // collapse open folder
                    expandedFolderName = "";
                }else {
                    expandedFolderName = folder.getName();
                }
                notifyDataSetChanged();
            }
        });
        holder.container.setOnLongClickListener(null);
    }

    private void setItemMargin(ViewHolder holder, GBDevice device){
        Resources r = context.getResources();
        int widthDp = 8;
        if(!StringUtils.isNullOrEmpty(device.getParentFolder())){
            widthDp = 16;
        }
        float px = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                widthDp,
                r.getDisplayMetrics()
        );
        CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) holder.container.getLayoutParams();
        layoutParams.setMarginStart((int) px);
        holder.container.setLayoutParams(layoutParams);

        int alpha = 0;
        if(device instanceof GBDeviceFolder && device.getName().equals(expandedFolderName)){
            alpha = 50;
        }else if(!StringUtils.isNullOrEmpty(device.getParentFolder()) && expandedFolderName.equals(device.getParentFolder())){
            alpha = 50;
        }

        holder.root.setBackgroundColor(Color.argb(alpha, 0, 0, 0));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        final GBDevice device = devicesListWithFolders.get(position);

        setItemMargin(holder, device);

        if(device instanceof GBDeviceFolder){
            showDeviceFolder(holder, (GBDeviceFolder) device);
            return;
        }

        String parentFolder = device.getParentFolder();
        if(!StringUtils.isNullOrEmpty(parentFolder)){
            if(parentFolder.equals(expandedFolderName)){
                holder.container.setVisibility(View.VISIBLE);
            }else{
                holder.container.setVisibility(View.GONE);
            }
        }else{
            holder.container.setVisibility(View.VISIBLE);
        }

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
                showDeviceSubmenu(v, device);
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

            // Hide the battery status level, if it has no text
            if (TextUtils.isEmpty(batteryStatusLabels[batteryIndex].getText())) {
                batteryStatusLabels[batteryIndex].setVisibility(View.GONE);
            } else {
                batteryStatusLabels[batteryIndex].setVisibility(View.VISIBLE);
            }
        }
        holder.heartRateStatusBox.setVisibility((device.isInitialized() && coordinator.supportsRealtimeData() && coordinator.supportsHeartRateMeasurement(device)) ? View.VISIBLE : View.GONE);
        if (parent.getContext() instanceof ControlCenterv2) {
            ActivitySample sample = ((ControlCenterv2) parent.getContext()).getCurrentHRSample();
            if (sample != null) {
                holder.heartRateStatusLabel.setText(String.valueOf(sample.getHeartRate()));
            } else {
                holder.heartRateStatusLabel.setText("");
            }

            // Hide the level, if it has no text
            if (TextUtils.isEmpty(holder.heartRateStatusLabel.getText())) {
                holder.heartRateStatusLabel.setVisibility(View.GONE);
            } else {
                holder.heartRateStatusLabel.setVisibility(View.VISIBLE);
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
        holder.deviceSpecificSettingsView.setVisibility(coordinator.getSupportedDeviceSpecificSettings(device)  != null ? View.VISIBLE : View.GONE);
        holder.deviceSpecificSettingsView.setOnClickListener(new View.OnClickListener()

                                                {
                                                    @Override
                                                    public void onClick(View v) {
                                                        Intent startIntent;
                                                        startIntent = new Intent(context, DeviceSettingsActivity.class);
                                                        startIntent.putExtra(GBDevice.EXTRA_DEVICE, device);
                                                        startIntent.putExtra(DeviceSettingsActivity.MENU_ENTRY_POINT, DeviceSettingsActivity.MENU_ENTRY_POINTS.DEVICE_SETTINGS);
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

        //set reminders
        holder.setRemindersView.setVisibility(coordinator.getReminderSlotCount() > 0 ? View.VISIBLE : View.GONE);
        holder.setRemindersView.setOnClickListener(new View.OnClickListener()

                                                {
                                                    @Override
                                                    public void onClick(View v) {
                                                        Intent startIntent;
                                                        startIntent = new Intent(context, ConfigureReminders.class);
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

        holder.infoIcons.setVisibility(View.VISIBLE);

        final boolean detailsShown = expandedDeviceAddress.equals(device.getAddress());
        boolean showInfoIcon = device.hasDeviceInfos() && !device.isBusy();
        holder.deviceInfoBox.setActivated(detailsShown);
        holder.deviceInfoBox.setVisibility(detailsShown ? View.VISIBLE : View.GONE);
        holder.deviceInfoView.setVisibility(View.VISIBLE);
        holder.deviceInfoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeviceSubmenu(v, device);
            }
        });

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
        final float FREQ_MIN = 87.5F;
        final float FREQ_MAX = 108.0F;
        final int FREQ_MIN_INT = (int) Math.floor(FREQ_MIN);
        final int FREQ_MAX_INT = (int) Math.round(FREQ_MAX);
        final AlertDialog alert[] = new AlertDialog[1];

        holder.fmFrequencyBox.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(context);
                final LayoutInflater inflater = LayoutInflater.from(context);
                final View frequency_picker_view = inflater.inflate(R.layout.dialog_frequency_picker, null);
                builder.setTitle(R.string.preferences_fm_frequency);
                final float[] fm_presets = new float[3];

                fm_presets[0] = GBApplication.getDeviceSpecificSharedPrefs(device.getAddress()).getFloat("fm_preset0", 99);
                fm_presets[1] = GBApplication.getDeviceSpecificSharedPrefs(device.getAddress()).getFloat("fm_preset1", 100);
                fm_presets[2] = GBApplication.getDeviceSpecificSharedPrefs(device.getAddress()).getFloat("fm_preset2", 101);

                final NumberPicker frequency_decimal_picker = frequency_picker_view.findViewById(R.id.frequency_dec);
                frequency_decimal_picker.setMinValue(FREQ_MIN_INT);
                frequency_decimal_picker.setMaxValue(FREQ_MAX_INT);

                final NumberPicker frequency_fraction_picker = frequency_picker_view.findViewById(R.id.frequency_fraction);
                frequency_fraction_picker.setMinValue(0);
                frequency_fraction_picker.setMaxValue(9);

                final NumberPicker.OnValueChangeListener picker_listener = new NumberPicker.OnValueChangeListener() {
                    @Override
                    public void onValueChange(NumberPicker numberPicker, int oldVal, int newVal) {

                        int decimal_value = numberPicker.getValue();
                        if (decimal_value == FREQ_MIN_INT) {
                            frequency_fraction_picker.setMinValue(5);
                            frequency_fraction_picker.setMaxValue(9);
                        } else if (decimal_value == FREQ_MAX_INT) {
                            frequency_fraction_picker.setMinValue(0);
                            frequency_fraction_picker.setMaxValue(0);
                        } else {
                            frequency_fraction_picker.setMinValue(0);
                            frequency_fraction_picker.setMaxValue(9);
                        }
                    }
                };

                frequency_decimal_picker.setOnValueChangedListener(picker_listener);

                final Button[] button_presets = new Button[]{
                        frequency_picker_view.findViewById(R.id.frequency_preset1),
                        frequency_picker_view.findViewById(R.id.frequency_preset2),
                        frequency_picker_view.findViewById(R.id.frequency_preset3)
                };

                for (int i = 0; i < button_presets.length; i++) {
                    final int index = i;
                    button_presets[index].setText(String.valueOf(fm_presets[index]));
                    button_presets[index].setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            final float frequency = fm_presets[index];
                            device.setExtraInfo("fm_frequency", fm_presets[index]);
                            fmFrequencyLabel.setText(String.format(Locale.getDefault(), "%.1f", (float) frequency));
                            GBApplication.deviceService().onSetFmFrequency(frequency);
                            alert[0].dismiss();
                        }
                    });
                    button_presets[index].setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View view) {
                            final float frequency = (float) (frequency_decimal_picker.getValue() + (0.1 * frequency_fraction_picker.getValue()));
                            fm_presets[index] = frequency;
                            button_presets[index].setText(String.valueOf(frequency));
                            SharedPreferences.Editor editor = GBApplication.getDeviceSpecificSharedPrefs(device.getAddress()).edit();
                            editor.putFloat((String.format("fm_preset%s", index)), frequency);
                            editor.apply();
                            editor.commit();
                            return true;
                        }
                    });

                }

                final float frequency = (float) device.getExtraInfo("fm_frequency");
                final int decimal = (int) frequency;
                final int fraction = Math.round((frequency - decimal) * 10);
                frequency_decimal_picker.setValue(decimal);
                picker_listener.onValueChange(frequency_decimal_picker, frequency_decimal_picker.getValue(), decimal);
                frequency_fraction_picker.setValue(fraction);

                builder.setView(frequency_picker_view);

                builder.setPositiveButton(context.getResources().getString(android.R.string.ok),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                float frequency = (float) (frequency_decimal_picker.getValue() + (0.1 * frequency_fraction_picker.getValue()));
                                if (frequency < FREQ_MIN || frequency > FREQ_MAX) {
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
                                    fmFrequencyLabel.setText(String.format(Locale.getDefault(), "%.1f", frequency));
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

                alert[0] = builder.create();
                alert[0].show();
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

        holder.powerOff.setVisibility(View.GONE);
        if (device.isInitialized() && coordinator.supportsPowerOff()) {
            holder.powerOff.setVisibility(View.VISIBLE);
            holder.powerOff.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new AlertDialog.Builder(context)
                            .setTitle(R.string.controlcenter_power_off_confirm_title)
                            .setMessage(R.string.controlcenter_power_off_confirm_description)
                            .setIcon(R.drawable.ic_power_settings_new)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(final DialogInterface dialog, final int whichButton) {
                                    GBApplication.deviceService().onPowerOff();
                                }
                            })
                            .setNegativeButton(android.R.string.no, null)
                            .show();
                }
            });
        }

        holder.cardViewActivityCardLayout.setVisibility(coordinator.supportsActivityTracking() ? View.VISIBLE : View.GONE);
        holder.cardViewActivityCardLayout.setMinimumWidth(coordinator.supportsActivityTracking() ? View.VISIBLE : View.GONE);

        if (coordinator.supportsActivityTracking()) {
            setActivityCard(holder, device, dailyTotals);
        }
    }

    private boolean showInstallerItem(GBDevice device) {
        final DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(device);
        return coordinator.supportsAppsManagement() || coordinator.supportsFlashing();
    }

    private void showDeviceSubmenu(final View v, final GBDevice device) {
        boolean deviceConnected = device.getState() != GBDevice.State.NOT_CONNECTED;

        PopupMenu menu = new PopupMenu(v.getContext(), v);
        menu.inflate(R.menu.activity_controlcenterv2_device_submenu);

        final boolean detailsShown = expandedDeviceAddress.equals(device.getAddress());
        boolean showInfoIcon = device.hasDeviceInfos() && !device.isBusy();

        menu.getMenu().findItem(R.id.controlcenter_device_submenu_connect).setVisible(!deviceConnected);
        menu.getMenu().findItem(R.id.controlcenter_device_submenu_disconnect).setVisible(deviceConnected);
        menu.getMenu().findItem(R.id.controlcenter_device_submenu_show_details).setEnabled(showInfoIcon);
        menu.getMenu().findItem(R.id.controlcenter_device_submenu_installer).setEnabled(deviceConnected);
        menu.getMenu().findItem(R.id.controlcenter_device_submenu_installer).setVisible(showInstallerItem(device));

        menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()){
                    case R.id.controlcenter_device_submenu_connect:
                        if (device.getState() != GBDevice.State.CONNECTED) {
                            showTransientSnackbar(R.string.controlcenter_snackbar_connecting);
                            GBApplication.deviceService().connect(device);
                        }
                        return true;
                    case R.id.controlcenter_device_submenu_disconnect:
                        if (device.getState() != GBDevice.State.NOT_CONNECTED) {
                            showTransientSnackbar(R.string.controlcenter_snackbar_disconnecting);
                            GBApplication.deviceService().disconnect(device);
                        }
                        return true;
                    case R.id.controlcenter_device_submenu_set_alias:
                        showSetAliasDialog(device);
                        return true;
                    case R.id.controlcenter_device_submenu_set_preferences:
                        setAppPreferences(device);
                        return true;
                    case R.id.controlcenter_device_submenu_remove:
                        showRemoveDeviceDialog(device);
                        return true;
                    case R.id.controlcenter_device_submenu_show_details:
                        final String previouslyExpandedDeviceAddress = expandedDeviceAddress;
                        expandedDeviceAddress = detailsShown ? "" : device.getAddress();

                        if (!previouslyExpandedDeviceAddress.isEmpty()) {
                            // Notify the previously expanded device for a change (collapsing it)
                            for (int i = 0; i < devicesListWithFolders.size(); i++) {
                                final GBDevice gbDevice = devicesListWithFolders.get(i);
                                if (gbDevice.getAddress().equals(previouslyExpandedDeviceAddress)) {
                                    notifyItemChanged(devicesListWithFolders.indexOf(gbDevice));
                                    break;
                                }
                            }
                        }

                        // Update the current one
                        notifyItemChanged(devicesListWithFolders.indexOf(device));
                        return true;
                    case R.id.controlcenter_device_submenu_set_parent_folder:
                        showSetParentFolderDialog(device);
                        return true;
                    case R.id.controlcenter_device_submenu_installer:
                        Intent openFwIntent = new Intent(context, OpenFwAppInstallerActivity.class);
                        openFwIntent.putExtra(GBDevice.EXTRA_DEVICE, device);
                        context.startActivity(openFwIntent);
                        return false;
                }
                return false;
            }
        });
        menu.show();
    }

    private void showRemoveDeviceDialog(final GBDevice device) {
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
                            GB.toast(context, context.getString(R.string.error_deleting_device, ex.getMessage()), Toast.LENGTH_LONG, GB.ERROR, ex);
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

    private void showSetParentFolderDialog(final GBDevice device) {
        final String[] selectedFolder = new String[1];

        final LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        final LinearLayout newFolderLayout = new LinearLayout(context);
        newFolderLayout.setOrientation(LinearLayout.HORIZONTAL);
        newFolderLayout.setPadding(context.getResources().getDimensionPixelSize(R.dimen.dialog_margin),
                0, context.getResources().getDimensionPixelSize(R.dimen.dialog_margin), 0);

        final TextView newFolderLabel = new TextView(context);
        newFolderLabel.setText(R.string.controlcenter_folder_name);
        final EditText newFolderInput = new EditText(context);
        newFolderInput.setInputType(InputType.TYPE_CLASS_TEXT);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        newFolderInput.setLayoutParams(params);

        newFolderInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                selectedFolder[0] = editable.toString();
            }
        });

        newFolderLayout.addView(newFolderLabel);
        newFolderLayout.addView(newFolderInput);

        final Spinner deviceListSpinner = new Spinner(context);
        ArrayList<SpinnerWithIconItem> foldersList = new ArrayList<>();
        for (GBDevice oneDevice : deviceList) {
            String folder = oneDevice.getParentFolder();
            if (StringUtils.isNullOrEmpty(folder)) {
                continue;
            }
            if (folderListContainsName(foldersList, folder)) {
                continue;
            }
            foldersList.add(new SpinnerWithIconItem(folder, 2L, R.drawable.ic_folder));
        }

        foldersList.add(new SpinnerWithIconItem(context.getString(R.string.controlcenter_add_new_folder), 0L, R.drawable.ic_create_new_folder));
        if (foldersList.toArray().length > 1) {
            foldersList.add(new SpinnerWithIconItem(context.getString(R.string.controlcenter_unset_folder), 1L, R.drawable.ic_folder_delete));
        }

        final SpinnerWithIconAdapter deviceListAdapter = new SpinnerWithIconAdapter((Activity) context,
                R.layout.spinner_with_image_layout, R.id.spinner_item_text, foldersList);
        deviceListSpinner.setAdapter(deviceListAdapter);

        deviceListSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                SpinnerWithIconItem selectedItem = (SpinnerWithIconItem) parent.getItemAtPosition(pos);
                int folderId = selectedItem.getId().intValue();
                switch (folderId) {
                    case 0: //Add new folder from test input
                        newFolderLayout.setVisibility(View.VISIBLE);
                        selectedFolder[0] = newFolderInput.getText().toString();
                        break;
                    case 1: //Unset folder
                        newFolderLayout.setVisibility(View.GONE);
                        selectedFolder[0] = "";
                        break;
                    default: //Set folder from selection
                        newFolderLayout.setVisibility(View.GONE);
                        selectedFolder[0] = selectedItem.getText();
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub
            }
        });

        linearLayout.addView(deviceListSpinner);
        linearLayout.addView(newFolderLayout);

        new AlertDialog.Builder(context)
                .setCancelable(true)
                .setTitle(R.string.controlcenter_set_folder_title)
                .setView(linearLayout)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        try (DBHandler dbHandler = GBApplication.acquireDB()) {
                            DaoSession session = dbHandler.getDaoSession();
                            Device dbDevice = DBHelper.getDevice(device, session);
                            String parentFolder = selectedFolder[0];
                            dbDevice.setParentFolder(parentFolder);
                            dbDevice.update();
                            device.setParentFolder(parentFolder);
                            expandedFolderName = parentFolder;
                        } catch (Exception ex) {
                            GB.toast(context, context.getString(R.string.error_setting_parent_folder, ex.getMessage()), Toast.LENGTH_LONG, GB.ERROR, ex);
                        } finally {
                            Intent refreshIntent = new Intent(DeviceManager.ACTION_REFRESH_DEVICELIST);
                            LocalBroadcastManager.getInstance(context).sendBroadcast(refreshIntent);
                        }
                    }
                })
                .setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    private boolean folderListContainsName(ArrayList<SpinnerWithIconItem> list, String name){
        for (SpinnerWithIconItem item: list) {
            if (item.getText().equals(name)){
                return true;
            }
        }
        return false;
    }

    private void setAppPreferences(GBDevice device) {
        Intent startIntent;
        startIntent = new Intent(context, DeviceSettingsActivity.class);
        startIntent.putExtra(GBDevice.EXTRA_DEVICE, device);
        startIntent.putExtra(DeviceSettingsActivity.MENU_ENTRY_POINT, DeviceSettingsActivity.MENU_ENTRY_POINTS.APPLICATION_SETTINGS);
        context.startActivity(startIntent);
    }
    private void showSetAliasDialog(final GBDevice device) {
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

    @Override
    public int getItemCount() {
        return devicesListWithFolders.size();
    }

    @Override
    public long getItemId(int position) {
        return idGenerator.getId(devicesListWithFolders.get(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        View root;
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
        ImageView setRemindersView;
        ImageView showActivityGraphs;
        ImageView showActivityTracks;
        ImageView calibrateDevice;
        LinearLayout heartRateStatusBox;
        ImageView heartRateIcon;
        TextView heartRateStatusLabel;
        FlexboxLayout infoIcons;


        ImageView deviceInfoView;
        //overflow
        final RelativeLayout deviceInfoBox;
        ListView deviceInfoList;
        ImageView findDevice;
        LinearLayout fmFrequencyBox;
        TextView fmFrequencyLabel;
        ImageView ledColor;
        ImageView powerOff;

        //activity card
        LinearLayout cardViewActivityCardLayout;
        PieChart TotalStepsChart;
        PieChart TotalDistanceChart;
        PieChart SleepTimeChart;

        ViewHolder(View view) {
            super(view);

            root = view;

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
            setRemindersView = view.findViewById(R.id.device_action_set_reminders);
            showActivityGraphs = view.findViewById(R.id.device_action_show_activity_graphs);
            showActivityTracks = view.findViewById(R.id.device_action_show_activity_tracks);
            deviceInfoView = view.findViewById(R.id.device_info_image);
            calibrateDevice = view.findViewById(R.id.device_action_calibrate);

            deviceInfoBox = view.findViewById(R.id.device_item_infos_box);
            //overflow
            deviceInfoList = view.findViewById(R.id.device_item_infos);
            findDevice = view.findViewById(R.id.device_action_find);
            fmFrequencyBox = view.findViewById(R.id.device_fm_frequency_box);
            fmFrequencyLabel = view.findViewById(R.id.fm_frequency);
            ledColor = view.findViewById(R.id.device_led_color);
            powerOff = view.findViewById(R.id.device_action_power_off);
            heartRateStatusBox = view.findViewById(R.id.device_heart_rate_status_box);
            heartRateStatusLabel = view.findViewById(R.id.heart_rate_status);
            heartRateIcon = view.findViewById(R.id.device_heart_rate_status);
            infoIcons = view.findViewById(R.id.device_info_icons);

            cardViewActivityCardLayout = view.findViewById(R.id.card_view_activity_card_layout);

            TotalStepsChart = view.findViewById(R.id.activity_dashboard_piechart1);
            TotalDistanceChart = view.findViewById(R.id.activity_dashboard_piechart2);
            SleepTimeChart = view.findViewById(R.id.activity_dashboard_piechart3);
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

    private void setActivityCard(ViewHolder holder, final GBDevice device, long[] dailyTotals) {
        int steps = (int) dailyTotals[0];
        int sleep = (int) dailyTotals[1];
        ActivityUser activityUser = new ActivityUser();
        int stepGoal = activityUser.getStepsGoal();
        int sleepGoal = activityUser.getSleepDurationGoal();
        int sleepGoalMinutes = sleepGoal * 60;
        int distanceGoal = activityUser.getDistanceGoalMeters() * 100;
        int stepLength = activityUser.getStepLengthCm();
        double distanceMeters = dailyTotals[0] * stepLength * 0.01;
        String distanceFormatted = FormatUtils.getFormattedDistanceLabel(distanceMeters);

        setUpChart(holder.TotalStepsChart);
        setChartsData(holder.TotalStepsChart, steps, stepGoal, context.getString(R.string.steps), String.valueOf(steps), context);

        setUpChart(holder.TotalDistanceChart);
        setChartsData(holder.TotalDistanceChart, steps * stepLength, distanceGoal, context.getString(R.string.distance), distanceFormatted, context);

        setUpChart(holder.SleepTimeChart);
        setChartsData(holder.SleepTimeChart, sleep, sleepGoalMinutes, context.getString(R.string.prefs_activity_in_device_card_sleep_title), String.format("%1s", getHM(sleep)), context);

        boolean showActivityCard = GBApplication.getDeviceSpecificSharedPrefs(device.getAddress()).getBoolean(DeviceSettingsPreferenceConst.PREFS_ACTIVITY_IN_DEVICE_CARD, true);
        holder.cardViewActivityCardLayout.setVisibility(showActivityCard ? View.VISIBLE : View.GONE);

        boolean showActivitySteps = GBApplication.getDeviceSpecificSharedPrefs(device.getAddress()).getBoolean(DeviceSettingsPreferenceConst.PREFS_ACTIVITY_IN_DEVICE_CARD_STEPS, true);
        boolean showActivitySleep = GBApplication.getDeviceSpecificSharedPrefs(device.getAddress()).getBoolean(DeviceSettingsPreferenceConst.PREFS_ACTIVITY_IN_DEVICE_CARD_SLEEP, true);
        boolean showActivityDistance = GBApplication.getDeviceSpecificSharedPrefs(device.getAddress()).getBoolean(DeviceSettingsPreferenceConst.PREFS_ACTIVITY_IN_DEVICE_CARD_DISTANCE, true);

        //do the multiple mini-charts for activities in a loop
        Hashtable<PieChart, Pair<Boolean, Integer>> activitiesStatusMiniCharts = new Hashtable<>();
        activitiesStatusMiniCharts.put(holder.TotalStepsChart, new Pair<>(showActivitySteps && steps > 0, ChartsActivity.getChartsTabIndex("stepsweek", device, context)));
        activitiesStatusMiniCharts.put(holder.SleepTimeChart, new Pair<>(showActivitySleep && sleep > 0, ChartsActivity.getChartsTabIndex("sleep", device, context)));
        activitiesStatusMiniCharts.put(holder.TotalDistanceChart, new Pair<>(showActivityDistance && steps > 0, ChartsActivity.getChartsTabIndex("activity", device, context)));

        for (Map.Entry<PieChart, Pair<Boolean, Integer>> miniCharts : activitiesStatusMiniCharts.entrySet()) {
            PieChart miniChart = miniCharts.getKey();
            final Pair<Boolean, Integer> parameters = miniCharts.getValue();
            miniChart.setVisibility(parameters.first ? View.VISIBLE : View.GONE);
            miniChart.setOnClickListener(new View.OnClickListener() {
                                             @Override
                                             public void onClick(View v) {
                                                 Intent startIntent;
                                                 startIntent = new Intent(context, ChartsActivity.class);
                                                 startIntent.putExtra(GBDevice.EXTRA_DEVICE, device);
                                                 startIntent.putExtra(ChartsActivity.EXTRA_FRAGMENT_ID, parameters.second);
                                                 context.startActivity(startIntent);
                                             }
                                         }
            );
        }
    }

    private String getHM(long value) {
        return DateTimeUtils.formatDurationHoursMinutes(value, TimeUnit.MINUTES);
    }
    private void setUpChart(PieChart DashboardChart) {
        DashboardChart.setTouchEnabled(false);
        DashboardChart.setNoDataText("");
        DashboardChart.getLegend().setEnabled(false);
        DashboardChart.setDrawHoleEnabled(true);
        DashboardChart.setHoleColor(Color.WHITE);
        DashboardChart.getDescription().setText("");
        DashboardChart.setTransparentCircleColor(Color.WHITE);
        DashboardChart.setTransparentCircleAlpha(110);
        DashboardChart.setHoleRadius(70f);
        DashboardChart.setTransparentCircleRadius(75f);
        DashboardChart.setDrawCenterText(true);
        DashboardChart.setRotationEnabled(true);
        DashboardChart.setHighlightPerTapEnabled(true);
        DashboardChart.setCenterTextOffset(0, 0);
    }
    private void setChartsData(PieChart pieChart, float value, float target, String label, String stringValue, Context context) {
        final String CHART_COLOR_START = "#e74c3c";
        final String CHART_COLOR_END = "#2ecc71";

        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry((float) value, context.getResources().getDrawable(R.drawable.ic_star_gold)));

        if (value < target) {
            entries.add(new PieEntry((float) (target - value)));
        }

        pieChart.setCenterText(String.format("%s\n%s", stringValue, label));
        float colorValue = Math.max(0, Math.min(1, value / target));
        int chartColor = interpolateColor(Color.parseColor(CHART_COLOR_START), Color.parseColor(CHART_COLOR_END), colorValue);

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setDrawIcons(false);
        dataSet.setIconsOffset(new MPPointF(0, -66));

        if (colorValue == 1) {
            dataSet.setDrawIcons(true);
        }
        dataSet.setSliceSpace(0f);
        dataSet.setSelectionShift(5f);
        dataSet.setColors(chartColor, Color.LTGRAY);

        PieData data = new PieData(dataSet);
        data.setValueTextSize(0f);
        data.setValueTextColor(Color.WHITE);

        pieChart.setData(data);
        pieChart.invalidate();
    }
    private float interpolate(float a, float b, float proportion) {
        return (a + ((b - a) * proportion));
    }

    private int interpolateColor(int a, int b, float proportion) {
        float[] hsva = new float[3];
        float[] hsvb = new float[3];
        Color.colorToHSV(a, hsva);
        Color.colorToHSV(b, hsvb);
        for (int i = 0; i < 3; i++) {
            hsvb[i] = interpolate(hsva[i], hsvb[i], proportion);
        }
        return Color.HSVToColor(hsvb);
    }

    private static class GBDeviceDiffUtil extends DiffUtil.ItemCallback<GBDevice> {
        @Override
        public boolean areItemsTheSame(@NonNull GBDevice oldItem, @NonNull GBDevice newItem) {
            return new EqualsBuilder()
                    .append(oldItem.getAddress(), newItem.getAddress())
                    .append(oldItem.getName(), newItem.getName())
                    .isEquals();
        }

        @Override
        public boolean areContentsTheSame(@NonNull GBDevice oldItem, @NonNull GBDevice newItem) {
            return EqualsBuilder.reflectionEquals(oldItem, newItem);
        }
    }

    /**
     * A generator of stable IDs, given a string, since hashCode can easily have collisions.
     */
    private static class StableIdGenerator {
        private final Map<String, Long> idMapping = new HashMap<String, Long>();

        private long nextId = 0;

        public long getId(final GBDevice device) {
            final String str = String.format("%s_%s", device.getAddress(), device.getName());

            if (!idMapping.containsKey(str)) {
                idMapping.put(str, nextId++);
            }

            return idMapping.get(str);
        }
    }
}
