package nodomain.freeyourgadget.gadgetbridge.adapter;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.ConfigureAlarms;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.ChartsActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;

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

    @Override
    public GBDeviceAdapterv2.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        this.parent = parent;
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_itemv2, parent, false);
        ViewHolder vh = new ViewHolder(view);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        final GBDevice device = deviceList.get(position);
        DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(device);

        holder.deviceImageView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (device.isInitialized()) {
                    DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(device);
                    Class<? extends Activity> primaryActivity = coordinator.getPrimaryActivity();
                    if (primaryActivity != null) {
                        Intent startIntent = new Intent(context, primaryActivity);
                        startIntent.putExtra(GBDevice.EXTRA_DEVICE, device);
                        context.startActivity(startIntent);
                    }
                } else {
                    //TODO: move somewhere else
                    GBApplication.deviceService().connect(device);
                }
            }
        });
        holder.deviceImageView.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                //TODO: move somewhere else
                GBApplication.deviceService().disconnect();
                return true;
            }
        });

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
        if (batteryLevel != GBDevice.BATTERY_UNKNOWN) {
            holder.batteryStatusBox.setVisibility(View.VISIBLE);
            holder.batteryStatusLabel.setText(device.getBatteryLevel() + "%");
            BatteryState batteryState = device.getBatteryState();
            if (BatteryState.BATTERY_CHARGING.equals(batteryState) ||
                    BatteryState.BATTERY_CHARGING_FULL.equals(batteryState)) {
                holder.batteryIcon.setImageLevel(device.getBatteryLevel() + 100);
            } else {
                holder.batteryIcon.setImageLevel(device.getBatteryLevel());
            }
        }

        //fetch activity data
        holder.fetchActivityDataBox.setVisibility((device.isInitialized() && coordinator.supportsActivityDataFetching()) ? View.VISIBLE : View.GONE);
        holder.fetchActivityData.setOnClickListener(new View.OnClickListener()

                                                    {
                                                        @Override
                                                        public void onClick(View v) {
                                                            GBApplication.deviceService().onFetchActivityData();
                                                        }
                                                    }
        );


        //take screenshot
        holder.takeScreenshotView.setVisibility((device.isInitialized() && coordinator.supportsScreenshots()) ? View.VISIBLE : View.GONE);
        holder.takeScreenshotView.setOnClickListener(new View.OnClickListener()

                                                     {
                                                         @Override
                                                         public void onClick(View v) {
                                                             GBApplication.deviceService().onScreenshotReq();
                                                         }
                                                     }
        );

        //set alarms
        holder.setAlarmsView.setVisibility(coordinator.supportsAlarmConfiguration() ? View.VISIBLE : View.GONE);
        holder.setAlarmsView.setOnClickListener(new View.OnClickListener()

                                                {
                                                    @Override
                                                    public void onClick(View v) {
                                                        Intent startIntent;
                                                        startIntent = new Intent(context, ConfigureAlarms.class);
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

        //Info icon is last in the row
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

        //find lost device, hidden under details
        holder.findDevice.setVisibility(device.isInitialized() ? View.VISIBLE : View.GONE);
        holder.findDevice.setOnClickListener(new View.OnClickListener()

                                             {
                                                 @Override
                                                 public void onClick(View v) {
                                                     GBApplication.deviceService().onFindDevice(true);
                                                     ProgressDialog.show(
                                                             context,
                                                             context.getString(R.string.control_center_find_lost_device),
                                                             context.getString(R.string.control_center_cancel_to_stop_vibration),
                                                             true, true,
                                                             new DialogInterface.OnCancelListener() {
                                                                 @Override
                                                                 public void onCancel(DialogInterface dialog) {
                                                                     GBApplication.deviceService().onFindDevice(false);
                                                                 }
                                                             });
                                                 }
                                             }

        );

        //remove device, hidden under details
        holder.removeDevice.setOnClickListener(new View.OnClickListener()

        {
            @Override
            public void onClick(View v) {
                //TODO: the logic is bolted to controlcenter, but I don't think it belongs here
            }
        });

        switch (device.getType()) {
            case PEBBLE:
                if (device.isConnected()) {
                    holder.deviceImageView.setImageResource(R.drawable.ic_device_pebble);
                } else {
                    holder.deviceImageView.setImageResource(R.drawable.ic_device_pebble_disabled);
                }
                break;
            case MIBAND:
            case MIBAND2:
                if (device.isConnected()) {
                    holder.deviceImageView.setImageResource(R.drawable.ic_device_miband);
                } else {
                    holder.deviceImageView.setImageResource(R.drawable.ic_device_miband_disabled);
                }
                break;
            case VIBRATISSIMO:
                if (device.isConnected()) {
                    holder.deviceImageView.setImageResource(R.drawable.ic_device_lovetoy);
                } else {
                    holder.deviceImageView.setImageResource(R.drawable.ic_device_lovetoy_disabled);
                }
                break;
            default:
                if (device.isConnected()) {
                    holder.deviceImageView.setImageResource(R.drawable.ic_launcher);
                } else {
                    holder.deviceImageView.setImageResource(R.drawable.ic_device_default_disabled);
                }
        }
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView deviceImageView;
        TextView deviceNameLabel;
        TextView deviceStatusLabel;

        //actions
        LinearLayout batteryStatusBox;
        TextView batteryStatusLabel;
        ImageView batteryIcon;
        LinearLayout fetchActivityDataBox;
        ImageView fetchActivityData;
        ProgressBar busyIndicator;
        ImageView takeScreenshotView;
        ImageView setAlarmsView;
        ImageView showActivityGraphs;

        ImageView deviceInfoView;
        //overflow
        final RelativeLayout deviceInfoBox;
        ListView deviceInfoList;
        ImageView findDevice;
        ImageView removeDevice;

        ViewHolder(View view) {
            super(view);
            deviceImageView = (ImageView) view.findViewById(R.id.device_image);
            deviceNameLabel = (TextView) view.findViewById(R.id.device_name);
            deviceStatusLabel = (TextView) view.findViewById(R.id.device_status);

            //actions
            batteryStatusBox = (LinearLayout) view.findViewById(R.id.device_battery_status_box);
            batteryStatusLabel = (TextView) view.findViewById(R.id.battery_status);
            batteryIcon = (ImageView) view.findViewById(R.id.device_battery_status);
            fetchActivityDataBox = (LinearLayout) view.findViewById(R.id.device_action_fetch_activity_box);
            fetchActivityData = (ImageView) view.findViewById(R.id.device_action_fetch_activity);
            busyIndicator = (ProgressBar) view.findViewById(R.id.device_busy_indicator);
            takeScreenshotView = (ImageView) view.findViewById(R.id.device_action_take_screenshot);
            setAlarmsView = (ImageView) view.findViewById(R.id.device_action_set_alarms);
            showActivityGraphs = (ImageView) view.findViewById(R.id.device_action_show_activity_graphs);
            deviceInfoView = (ImageView) view.findViewById(R.id.device_info_image);

            deviceInfoBox = (RelativeLayout) view.findViewById(R.id.device_item_infos_box);
            //overflow
            deviceInfoList = (ListView) view.findViewById(R.id.device_item_infos);
            findDevice = (ImageView) view.findViewById(R.id.device_action_find);
            removeDevice = (ImageView) view.findViewById(R.id.device_action_remove);
        }

    }

    public void justifyListViewHeightBasedOnChildren(ListView listView) {
        ArrayAdapter adapter = (ArrayAdapter) listView.getAdapter();

        if (adapter == null) {
            return;
        }
        ViewGroup vg = listView;
        int totalHeight = 0;
        for (int i = 0; i < adapter.getCount(); i++) {
            View listItem = adapter.getView(i, null, vg);
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
}
