package nodomain.freeyourgadget.gadgetbridge.adapter;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.ContextCompat;
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

import java.util.Collections;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.ConfigureAlarms;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.ChartsActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.model.ItemWithDetails;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;

/**
 * Adapter for displaying GBDevice instances.
 */
public class GBDeviceAdapterv2 extends ArrayAdapter<GBDevice> {

    private final Context context;
    private DeviceCoordinator coordinator;

    public GBDeviceAdapterv2(Context context, List<GBDevice> deviceList) {
        super(context, 0, deviceList);

        this.context = context;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        final GBDevice device = getItem(position);
        coordinator = DeviceHelper.getInstance().getCoordinator(device);

        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            view = inflater.inflate(R.layout.device_itemv2, parent, false);
        }
        TextView deviceStatusLabel = (TextView) view.findViewById(R.id.device_status);
        TextView deviceNameLabel = (TextView) view.findViewById(R.id.device_name);

        TextView batteryStatusLabel = (TextView) view.findViewById(R.id.battery_status);
        final ImageView deviceImageView = (ImageView) view.findViewById(R.id.device_image);

        ProgressBar busyIndicator = (ProgressBar) view.findViewById(R.id.device_busy_indicator);

        deviceNameLabel.setText(getUniqueDeviceName(device));

        if (device.isBusy()) {
            deviceStatusLabel.setText(device.getBusyTask());
            busyIndicator.setVisibility(View.VISIBLE);
        } else {
            deviceStatusLabel.setText(device.getStateString());
            busyIndicator.setVisibility(View.INVISIBLE);
        }

        //begin of action row
        //battery
        LinearLayout batteryStatusBox = (LinearLayout) view.findViewById(R.id.device_battery_status_box);
        batteryStatusBox.setVisibility(View.GONE);

        ImageView batteryIcon = (ImageView) view.findViewById(R.id.device_battery_status);

        short batteryLevel = device.getBatteryLevel();
        if (batteryLevel != GBDevice.BATTERY_UNKNOWN) {
            batteryStatusBox.setVisibility(View.VISIBLE);
            batteryStatusLabel.setText(device.getBatteryLevel() + "%");
            BatteryState batteryState = device.getBatteryState();
            if (BatteryState.BATTERY_LOW.equals(batteryState)) {
                //batteryIcon.setImageTintMode(olor.RED);
            } else {
                batteryStatusLabel.setTextColor(ContextCompat.getColor(getContext(), R.color.secondarytext));

                if (BatteryState.BATTERY_CHARGING.equals(batteryState) ||
                        BatteryState.BATTERY_CHARGING_FULL.equals(batteryState)) {
                    batteryStatusLabel.append(" CHG");
                }
            }
        }


        //fetch activity data
        ImageView fetchActivityData = (ImageView) view.findViewById(R.id.device_action_fetch_activity);
        LinearLayout fetchActivityDataBox = (LinearLayout) view.findViewById(R.id.device_action_fetch_activity_box);

        fetchActivityDataBox.setVisibility((device.isConnected() && coordinator.supportsActivityDataFetching()) ? View.VISIBLE : View.GONE);
        fetchActivityData.setOnClickListener(new View.OnClickListener() {
                                                  @Override
                                                  public void onClick(View v) {
                                                      GBApplication.deviceService().onFetchActivityData();
                                                  }
                                              }
        );


        //take screenshot
        ImageView takeScreenshotView = (ImageView) view.findViewById(R.id.device_action_take_screenshot);
        takeScreenshotView.setVisibility((device.isConnected() && coordinator.supportsScreenshots()) ? View.VISIBLE : View.GONE);
        takeScreenshotView.setOnClickListener(new View.OnClickListener() {
                                                  @Override
                                                  public void onClick(View v) {
                                                      GBApplication.deviceService().onScreenshotReq();
                                                  }
                                              }
        );

        //set alarms
        ImageView setAlarmsView = (ImageView) view.findViewById(R.id.device_action_set_alarms);
        setAlarmsView.setVisibility(coordinator.supportsAlarmConfiguration() ? View.VISIBLE : View.GONE);
        setAlarmsView.setOnClickListener(new View.OnClickListener() {
                                                  @Override
                                                  public void onClick(View v) {
                                                      Intent startIntent;
                                                      startIntent = new Intent(context, ConfigureAlarms.class);
                                                      context.startActivity(startIntent);
                                                  }
                                              }
        );

        //show graphs
        ImageView showActivityGraphs = (ImageView) view.findViewById(R.id.device_action_show_activity_graphs);
        showActivityGraphs.setVisibility(coordinator.supportsActivityTracking() ? View.VISIBLE : View.GONE);
        showActivityGraphs.setOnClickListener(new View.OnClickListener() {
                                                  @Override
                                                  public void onClick(View v) {
                                                      Intent startIntent;
                                                      startIntent = new Intent(context, ChartsActivity.class);
                                                      startIntent.putExtra(GBDevice.EXTRA_DEVICE, device);
                                                      context.startActivity(startIntent);                                                  }
                                              }
        );

        //Info icon is last in the row
        ImageView deviceInfoView = (ImageView) view.findViewById(R.id.device_info_image);
        final RelativeLayout deviceInfoBox = (RelativeLayout) view.findViewById(R.id.device_item_infos_box);
        final ListView deviceInfoList = (ListView) view.findViewById(R.id.device_item_infos);
        //TODO: can we spare all these additional layouts? find out more.
        ItemWithDetailsAdapter infoAdapter = new ItemWithDetailsAdapter(context, device.getDeviceInfos());
        infoAdapter.setHorizontalAlignment(true);
        deviceInfoList.setAdapter(infoAdapter);
        boolean showInfoIcon = device.hasDeviceInfos() && !device.isBusy();
        deviceInfoView.setVisibility(showInfoIcon ? View.VISIBLE : View.GONE);
        deviceInfoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (deviceInfoBox.getVisibility() == View.VISIBLE) {
                    deviceInfoBox.setVisibility(View.GONE);
                } else {
                    ArrayAdapter adapter = (ArrayAdapter) deviceInfoList.getAdapter();
                    adapter.clear();
                    List<ItemWithDetails> infos = device.getDeviceInfos();
                    Collections.sort(infos);
                    adapter.addAll(infos);
                    justifyListViewHeightBasedOnChildren(deviceInfoList);
                    deviceInfoBox.setVisibility(View.VISIBLE);
                    deviceInfoBox.setFocusable(false);
                }
            }
        });

        //remove device, hidden under details
        ImageView removeDevice = (ImageView) view.findViewById(R.id.device_action_remove);
        removeDevice.setOnClickListener(new View.OnClickListener() {
                                                  @Override
                                                  public void onClick(View v) {
                                                      //TODO: the logic is bolted to controlcenter, but I don't think it belongs here
                                                  }
                                              }
        );


        switch (device.getType()) {
            case PEBBLE:
                if (device.isConnected()) {
                    deviceImageView.setImageResource(R.drawable.ic_device_pebble);
                } else {
                    deviceImageView.setImageResource(R.drawable.ic_device_pebble_disabled);
                }
                break;
            case MIBAND:
            case MIBAND2:
                if (device.isConnected()) {
                    deviceImageView.setImageResource(R.drawable.ic_device_miband);
                } else {
                    deviceImageView.setImageResource(R.drawable.ic_device_miband_disabled);
                }
                break;
            case VIBRATISSIMO:
                if (device.isConnected()) {
                    deviceImageView.setImageResource(R.drawable.ic_device_lovetoy);
                } else {
                    deviceImageView.setImageResource(R.drawable.ic_device_lovetoy_disabled);
                }
                break;
            default:
                if (device.isConnected()) {
                    deviceImageView.setImageResource(R.drawable.ic_launcher);
                } else {
                    deviceImageView.setImageResource(R.drawable.ic_device_default_disabled);
                }
        }

        return view;
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
        for (int i = 0; i < getCount(); i++) {
            GBDevice item = getItem(i);
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
