package nodomain.freeyourgadget.gadgetbridge.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

import nodomain.freeyourgadget.gadgetbridge.R;

public class SpinnerWithIconAdapter extends ArrayAdapter<SpinnerWithIconItem> {
    private static final Logger LOG = LoggerFactory.getLogger(SpinnerWithIconAdapter.class);
    int groupid;
    Activity context;
    ArrayList<SpinnerWithIconItem> list;
    LayoutInflater inflater;

    public SpinnerWithIconAdapter(Activity context, int groupid, int id, ArrayList<SpinnerWithIconItem>
            list) {
        super(context, id, list);
        this.list = list;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.groupid = groupid;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View itemView = inflater.inflate(groupid, parent, false);
        ImageView imageView = (ImageView) itemView.findViewById(R.id.spinner_item_icon);
        imageView.setImageResource(list.get(position).getImageId());
        TextView textView = (TextView) itemView.findViewById(R.id.spinner_item_text);
        textView.setText(list.get(position).getText());

        return itemView;
    }

    public View getDropDownView(int position, View convertView, ViewGroup
            parent) {
        return getView(position, convertView, parent);

    }

    public int getItemPositionForSelection(SpinnerWithIconItem item) {
        if (item == null) return -1;
        int index = 0;
        for (SpinnerWithIconItem listItem : list) {
            if (listItem.id.equals(item.id)) {
                return index;
            }
            index++;
        }
        return -1;
    }
}

