package nodomain.freeyourgadget.gadgetbridge.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GB;
import nodomain.freeyourgadget.gadgetbridge.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.discovery.DeviceCandidate;

public class DeviceCandidateAdapter extends ArrayAdapter<DeviceCandidate> {

    private final Context context;
    private final List<DeviceCandidate> deviceCandidates;

    public DeviceCandidateAdapter(Context context, List<DeviceCandidate> deviceCandidates) {
        super(context, 0, deviceCandidates);

        this.context = context;
        this.deviceCandidates = deviceCandidates;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        DeviceCandidate device = getItem(position);

        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            view = inflater.inflate(R.layout.device_candidate_item, parent, false);
        }
        ImageView deviceImageView = (ImageView) view.findViewById(R.id.device_candidate_image);
        TextView deviceNameLabel = (TextView) view.findViewById(R.id.device_candidate_name);
        TextView deviceAddressLabel = (TextView) view.findViewById(R.id.device_candidate_address);

        String name = formatDeviceCandidate(device);
        deviceNameLabel.setText(name);
        deviceAddressLabel.setText(device.getMacAddress());

        switch (device.getDeviceType()) {
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

    private String formatDeviceCandidate(DeviceCandidate device) {
        if (device.getRssi() > GBDevice.RSSI_UNKNOWN) {
            return context.getString(R.string.device_with_rssi, device.getName(), GB.formatRssi(device.getRssi()));
        }
        return device.getName();
    }
}
