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
import nodomain.freeyourgadget.gadgetbridge.model.ItemWithDetails;

public class ItemWithDetailsAdapter extends ArrayAdapter<ItemWithDetails> {

    private final Context context;

    public ItemWithDetailsAdapter(Context context, List<ItemWithDetails> items) {
        super(context, 0, items);

        this.context = context;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        ItemWithDetails item = getItem(position);

        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            view = inflater.inflate(R.layout.item_with_details, parent, false);
        }
        ImageView iconView = (ImageView) view.findViewById(R.id.item_image);
        TextView nameView = (TextView) view.findViewById(R.id.item_name);
        TextView detailsView = (TextView) view.findViewById(R.id.item_details);

        nameView.setText(item.getName());
        detailsView.setText(item.getDetails());
        iconView.setImageResource(item.getIcon());

        return view;
    }
}
