package nodomain.freeyourgadget.gadgetbridge.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBDeviceApp;
import nodomain.freeyourgadget.gadgetbridge.R;

public class GBDeviceAppAdapter extends ArrayAdapter<GBDeviceApp> {

    private final Context context;
    private final List<GBDeviceApp> appList;

    public GBDeviceAppAdapter(Context context, List<GBDeviceApp> appList) {
        super(context, 0, appList);

        this.context = context;
        this.appList = appList;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        GBDeviceApp deviceApp = getItem(position);

        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            view = inflater.inflate(R.layout.device_item, parent, false);
        }
        TextView deviceStatusLabel = (TextView) view.findViewById(R.id.device_status);
        TextView deviceNameLabel = (TextView) view.findViewById(R.id.device_name);
        ImageView deviceImageView = (ImageView) view.findViewById(R.id.device_image);

        deviceStatusLabel.setText(getContext().getString(R.string.appversion_by_creator, deviceApp.getVersion(), deviceApp.getCreator()));
        deviceNameLabel.setText(deviceApp.getName());
        switch (deviceApp.getType()) {
            case APP_ACTIVITYTRACKER:
                deviceImageView.setImageResource(R.drawable.ic_activitytracker);
                break;
            case WATCHFACE:
                deviceImageView.setImageResource(R.drawable.ic_watchface);
                break;
            default:
                deviceImageView.setImageResource(R.drawable.ic_device_pebble);
        }

        return view;
    }
}
