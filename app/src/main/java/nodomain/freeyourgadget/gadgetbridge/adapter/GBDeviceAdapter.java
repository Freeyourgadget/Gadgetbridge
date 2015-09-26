package nodomain.freeyourgadget.gadgetbridge.adapter;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;

public class GBDeviceAdapter extends ArrayAdapter<GBDevice> {

    private final Context context;

    public GBDeviceAdapter(Context context, List<GBDevice> deviceList) {
        super(context, 0, deviceList);

        this.context = context;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        GBDevice device = getItem(position);

        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            view = inflater.inflate(R.layout.device_item, parent, false);
        }
        TextView deviceStatusLabel = (TextView) view.findViewById(R.id.device_status);
        TextView deviceNameLabel = (TextView) view.findViewById(R.id.device_name);
        TextView deviceInfoLabel = (TextView) view.findViewById(R.id.device_info);
        TextView batteryStatusLabel = (TextView) view.findViewById(R.id.battery_status);
        ImageView deviceImageView = (ImageView) view.findViewById(R.id.device_image);
        ProgressBar busyIndicator = (ProgressBar) view.findViewById(R.id.device_busy_indicator);

        deviceNameLabel.setText(device.getName());
        deviceInfoLabel.setText(device.getInfoString());

        if (device.isBusy()) {
            deviceStatusLabel.setText(device.getBusyTask());
            busyIndicator.setVisibility(View.VISIBLE);
            batteryStatusLabel.setVisibility(View.GONE);
            deviceInfoLabel.setVisibility(View.GONE);
        } else {
            deviceStatusLabel.setText(device.getStateString());
            busyIndicator.setVisibility(View.GONE);
            batteryStatusLabel.setVisibility(View.VISIBLE);
            deviceInfoLabel.setVisibility(View.VISIBLE);
        }

        short batteryLevel = device.getBatteryLevel();
        if (batteryLevel != GBDevice.BATTERY_UNKNOWN) {
            batteryStatusLabel.setText("BAT: " + device.getBatteryLevel() + "%");
            BatteryState batteryState = device.getBatteryState();
            if (BatteryState.BATTERY_LOW.equals(batteryState)) {
                batteryStatusLabel.setTextColor(Color.RED);
            } else {
                batteryStatusLabel.setTextColor(ContextCompat.getColor(getContext(), R.color.secondarytext));

                if (BatteryState.BATTERY_CHARGING.equals(batteryState) ||
                        BatteryState.BATTERY_CHARGING_FULL.equals(batteryState)) {
                    batteryStatusLabel.append(" CHG");
                }
            }
        } else {
            batteryStatusLabel.setText("");
        }

        switch (device.getType()) {
            case PEBBLE:
                deviceImageView.setImageResource(R.drawable.ic_device_pebble);
                break;
            case MIBAND:
                deviceImageView.setImageResource(R.drawable.ic_device_miband);
                break;
            default:
                deviceImageView.setImageResource(R.drawable.ic_launcher);
        }

        return view;
    }
}
