package nodomain.freeyourgadget.gadgetbridge.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceApp;

/**
 * Adapter for displaying GBDeviceApp instances.
 */
public class GBDeviceAppAdapter extends ArrayAdapter<GBDeviceApp> {

    private final Context context;

    public GBDeviceAppAdapter(Context context, List<GBDeviceApp> appList) {
        super(context, 0, appList);

        this.context = context;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        GBDeviceApp deviceApp = getItem(position);

        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            view = inflater.inflate(R.layout.item_with_details, parent, false);
        }
        TextView deviceAppVersionAuthorLabel = (TextView) view.findViewById(R.id.item_details);
        TextView deviceAppNameLabel = (TextView) view.findViewById(R.id.item_name);
        ImageView deviceImageView = (ImageView) view.findViewById(R.id.item_image);

        deviceAppVersionAuthorLabel.setText(getContext().getString(R.string.appversion_by_creator, deviceApp.getVersion(), deviceApp.getCreator()));

        // FIXME: replace with small icons
        String appNameLabelText = deviceApp.getName();
        if (deviceApp.isInCache() || deviceApp.isOnDevice()) {
            appNameLabelText += " (" + (deviceApp.isInCache() ? "C" : "")
                    + (deviceApp.isOnDevice() ? "D" : "") + ")";
        }
        deviceAppNameLabel.setText(appNameLabelText);

        switch (deviceApp.getType()) {
            case APP_GENERIC:
                deviceImageView.setImageResource(R.drawable.ic_watchapp);
                break;
            case APP_ACTIVITYTRACKER:
                deviceImageView.setImageResource(R.drawable.ic_activitytracker);
                break;
            case APP_SYSTEM:
            case WATCHFACE_SYSTEM:
                deviceImageView.setImageResource(R.drawable.ic_systemapp);
                break;
            case WATCHFACE:
                deviceImageView.setImageResource(R.drawable.ic_watchface);
                break;
            default:
                deviceImageView.setImageResource(R.drawable.ic_watchapp);
        }

        return view;
    }
}
