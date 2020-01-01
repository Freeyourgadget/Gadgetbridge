/*  Copyright (C) 2015-2019 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.DrawableRes;
import nodomain.freeyourgadget.gadgetbridge.R;

/**
 * Adapter for displaying generic ItemWithDetails instances.
 */
public abstract class AbstractItemAdapter<T> extends ArrayAdapter<T> {

    public static final int SIZE_SMALL = 1;
    public static final int SIZE_MEDIUM = 2;
    public static final int SIZE_LARGE = 3;
    private final Context context;
    private final List<T> items;
    private boolean horizontalAlignment;
    private int size = SIZE_MEDIUM;

    public AbstractItemAdapter(Context context) {
        this (context, new ArrayList<T>());
    }

    public AbstractItemAdapter(Context context, List<T> items) {
        super(context, 0, items);

        this.context = context;
        this.items = items;
    }

    public void setHorizontalAlignment(boolean horizontalAlignment) {
        this.horizontalAlignment = horizontalAlignment;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        T item = getItem(position);

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

        nameView.setText(getName(item));
        detailsView.setText(getDetails(item));
        iconView.setImageResource(getIcon(item));

        return view;
    }

    protected abstract String getName(T item);

    protected abstract String getDetails(T item);

    @DrawableRes
    protected abstract int getIcon(T item);

    public void setSize(int size) {
        this.size = size;
    }

    public int getSize() {
        return size;
    }

    public List<T> getItems() {
        return items;
    }

    public void loadItems() {
    }

    public void setItems(List<T> items, boolean notify) {
        this.items.clear();
        this.items.addAll(items);
        if (notify) {
            notifyDataSetChanged();
        }
    }
}
