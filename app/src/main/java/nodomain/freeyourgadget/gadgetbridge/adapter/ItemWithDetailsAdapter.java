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

/**
 * Adapter for displaying generic ItemWithDetails instances.
 */
public class ItemWithDetailsAdapter extends ArrayAdapter<ItemWithDetails> {

    public static final int SIZE_SMALL = 1;
    public static final int SIZE_MEDIUM = 2;
    public static final int SIZE_LARGE = 3;
    private final Context context;
    private boolean horizontalAlignment;
    private int size = SIZE_MEDIUM;

    public ItemWithDetailsAdapter(Context context, List<ItemWithDetails> items) {
        super(context, 0, items);

        this.context = context;
    }

    public void setHorizontalAlignment(boolean horizontalAlignment) {
        this.horizontalAlignment = horizontalAlignment;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        ItemWithDetails item = getItem(position);

        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            if (horizontalAlignment) {
                view = inflater.inflate(R.layout.item_with_details_horizontal, parent, false);
            } else {
                switch (size) {
                    case SIZE_SMALL:
                        view = inflater.inflate(R.layout.item_with_details_small, parent, false);
                        break;
                    default:
                        view = inflater.inflate(R.layout.item_with_details, parent, false);
                        break;
                }
            }
        }
        ImageView iconView = (ImageView) view.findViewById(R.id.item_image);
        TextView nameView = (TextView) view.findViewById(R.id.item_name);
        TextView detailsView = (TextView) view.findViewById(R.id.item_details);

        nameView.setText(item.getName());
        detailsView.setText(item.getDetails());
        iconView.setImageResource(item.getIcon());

        return view;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getSize() {
        return size;
    }
}
