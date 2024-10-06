/*  Copyright (C) 2020-2024 Petr Vaněk

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;

/**
 * Adapter for displaying generic ItemWithDetails instances.
 */
public abstract class AbstractActivityListingAdapter<T> extends RecyclerView.Adapter<AbstractActivityListingAdapter.AbstractActivityListingViewHolder<T>> {
    private final Context context;
    private final List<T> items;
    private final BitSet selectedItems = new BitSet();

    private OnItemClickListener onItemSingleClickListener;
    private OnItemClickListener onItemLongClickListener;

    public AbstractActivityListingAdapter(Context context) {
        this.context = context;
        this.items = new ArrayList<T>();
    }

    public void setOnItemClickListener(final OnItemClickListener onItemSingleClickListener) {
        this.onItemSingleClickListener = onItemSingleClickListener;
    }

    public void setOnItemLongClickListener(final OnItemClickListener onItemLongClickListener) {
        this.onItemLongClickListener = onItemLongClickListener;
    }

    public Context getContext() {
        return context;
    }

    public BitSet getSelectedItems() {
        return selectedItems;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public T getItem(final int position) {
        return items.get(position);
    }

    public int getPosition(final T item) {
        return items.indexOf(item);
    }

    public boolean isSelected(final int position) {
        return selectedItems.get(position);
    }

    @Override
    public int getItemViewType(final int position) {
        if (position == 0) {
            // First item is always the dashboard
            return 0;
        } else if (position == getItemCount() - 1) {
            // Last item is always an empty session (prevent overlap with fab)
            return 1;
        }

        return 2;
    }

    @NonNull
    @Override
    public AbstractActivityListingViewHolder<T> onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        switch (viewType) {
            case 1: // empty
                return new EmptyViewHolder(LayoutInflater.from(getContext()).inflate(R.layout.activity_list_item, parent, false));
        }

        throw new IllegalArgumentException("Unknown view type " + viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull final AbstractActivityListingViewHolder<T> holder, int position) {
        holder.fill(position, getItem(position), isSelected(position));
        if (position > 0 && position < getItemCount() - 1) {
            if (onItemSingleClickListener != null) {
                holder.itemView.setOnClickListener(v -> onItemSingleClickListener.onClick(position));
            }
            if (onItemLongClickListener != null) {
                holder.itemView.setOnLongClickListener(v -> {
                    onItemLongClickListener.onClick(position);
                    return true;
                });
            }
        }
    }

    public List<T> getItems() {
        return items;
    }

    public void loadItems() {
    }

    public void setItems(List<T> items, boolean notify) {
        this.items.clear();
        this.items.addAll(items);
        this.selectedItems.clear();
        if (notify) {
            notifyDataSetChanged();
        }
    }

    public void setActivityKindFilter(int activityKind) {
    }

    public void setDateFromFilter(long date) {
    }

    public void setDateToFilter(long date) {
    }

    public void setNameContainsFilter(String name) {
    }

    public void setItemsFilter(List<Long> items) {
    }

    public void setDeviceFilter(long device) {
    }

    public abstract static class AbstractActivityListingViewHolder<T> extends RecyclerView.ViewHolder {
        public AbstractActivityListingViewHolder(@NonNull final View itemView) {
            super(itemView);
        }

        public abstract void fill(int position, T item, final boolean selected);
    }

    public interface OnItemClickListener {
        void onClick(int position);
    }

    public class EmptyViewHolder extends AbstractActivityListingViewHolder<T> {
        final View rootView;

        public EmptyViewHolder(@NonNull final View itemView) {
            super(itemView);
            this.rootView = itemView;
        }

        @Override
        public void fill(final int position, final T item, final boolean selected) {
            rootView.setVisibility(View.GONE);
        }
    }
}
