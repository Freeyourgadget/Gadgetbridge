package nodomain.freeyourgadget.gadgetbridge.adapter;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.model.ItemWithDetails;

/**
 * Adapter for displaying GBDevice instances.
 */
public class GBDeviceAdapter extends ArrayAdapter<GBDevice> {

    private final Context context;

    public GBDeviceAdapter(Context context, List<GBDevice> deviceList) {
        super(context, 0, deviceList);

        this.context = context;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        final GBDevice device = getItem(position);

        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            view = inflater.inflate(R.layout.device_item, parent, false);
        }
        TextView deviceStatusLabel = (TextView) view.findViewById(R.id.device_status);
        TextView deviceNameLabel = (TextView) view.findViewById(R.id.device_name);
        final ListView deviceInfoList = (ListView) view.findViewById(R.id.device_item_infos);
        ItemWithDetailsAdapter infoAdapter = new ItemWithDetailsAdapter(context, device.getDeviceInfos());
        infoAdapter.setHorizontalAlignment(true);
        deviceInfoList.setAdapter(infoAdapter);
        TextView batteryLabel = (TextView) view.findViewById(R.id.battery_label);
        TextView batteryStatusLabel = (TextView) view.findViewById(R.id.battery_status);
        final ImageView deviceImageView = (ImageView) view.findViewById(R.id.device_image);
        ImageView deviceInfoView = (ImageView) view.findViewById(R.id.device_info_image);
        ProgressBar busyIndicator = (ProgressBar) view.findViewById(R.id.device_busy_indicator);

        deviceNameLabel.setText(getUniqueDeviceName(device));

        if (device.isBusy()) {
            deviceStatusLabel.setText(device.getBusyTask());
            busyIndicator.setVisibility(View.VISIBLE);
            batteryLabel.setVisibility(View.INVISIBLE);
            batteryStatusLabel.setVisibility(View.INVISIBLE);
        } else {
            deviceStatusLabel.setText(device.getStateString());
            busyIndicator.setVisibility(View.INVISIBLE);
            batteryLabel.setVisibility(View.VISIBLE);
            batteryStatusLabel.setVisibility(View.VISIBLE);
        }

        boolean showInfoIcon = device.hasDeviceInfos() && !device.isBusy();
        deviceInfoView.setVisibility(showInfoIcon ? View.VISIBLE : View.GONE);
        deviceInfoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (deviceInfoList.getVisibility() == View.VISIBLE) {
                    deviceInfoList.setVisibility(View.GONE);
                } else {
                    ArrayAdapter adapter = (ArrayAdapter) deviceInfoList.getAdapter();
                    adapter.clear();
                    List<ItemWithDetails> infos = device.getDeviceInfos();
                    Collections.sort(infos);
                    adapter.addAll(infos);
                    justifyListViewHeightBasedOnChildren(deviceInfoList);
                    deviceInfoList.setVisibility(View.VISIBLE);
                    deviceInfoList.setFocusable(false);
                }
            }
        });

        short batteryLevel = device.getBatteryLevel();
        if (batteryLevel != GBDevice.BATTERY_UNKNOWN) {
            batteryLabel.setText("BAT:");
            batteryStatusLabel.setText(device.getBatteryLevel() + "%");
            BatteryState batteryState = device.getBatteryState();
            if (BatteryState.BATTERY_LOW.equals(batteryState)) {
                batteryLabel.setTextColor(Color.RED);
                batteryStatusLabel.setTextColor(Color.RED);
            } else {
                batteryLabel.setTextColor(ContextCompat.getColor(getContext(), R.color.secondarytext));
                batteryStatusLabel.setTextColor(ContextCompat.getColor(getContext(), R.color.secondarytext));

                if (BatteryState.BATTERY_CHARGING.equals(batteryState) ||
                        BatteryState.BATTERY_CHARGING_FULL.equals(batteryState)) {
                    batteryStatusLabel.append(" CHG");
                }
            }
        } else {
            batteryLabel.setText("");
            batteryStatusLabel.setText("");
        }

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
            if (device.getHardwareVersion() != null) {
                deviceName = deviceName + " " + device.getHardwareVersion();
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
