/*  Copyright (C) 2015-2017 Andreas Shimokawa, Carsten Pfeiffer

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

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.appmanager.AbstractAppManagerFragment;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceApp;

/**
 * Adapter for displaying GBDeviceApp instances.
 */

public class GBDeviceAppAdapter extends RecyclerView.Adapter<GBDeviceAppAdapter.AppViewHolder> {

    private final int mLayoutId;
    private final List<GBDeviceApp> appList;
    private final AbstractAppManagerFragment mParentFragment;

    public List<GBDeviceApp> getAppList() {
        return appList;
    }

    public GBDeviceAppAdapter(List<GBDeviceApp> list, int layoutId, AbstractAppManagerFragment parentFragment) {
        mLayoutId = layoutId;
        appList = list;
        mParentFragment = parentFragment;
    }

    @Override
    public long getItemId(int position) {
        return appList.get(position).getUUID().getLeastSignificantBits();
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }

    @Override
    public GBDeviceAppAdapter.AppViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(mLayoutId, parent, false);
        return new AppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final AppViewHolder holder, int position) {
        final GBDeviceApp deviceApp = appList.get(position);

        holder.mDeviceAppVersionAuthorLabel.setText(GBApplication.getContext().getString(R.string.appversion_by_creator, deviceApp.getVersion(), deviceApp.getCreator()));
        // FIXME: replace with small icons
        String appNameLabelText = deviceApp.getName();
        holder.mDeviceAppNameLabel.setText(appNameLabelText);

        switch (deviceApp.getType()) {
            case APP_GENERIC:
                holder.mDeviceImageView.setImageResource(R.drawable.ic_watchapp);
                break;
            case APP_ACTIVITYTRACKER:
                holder.mDeviceImageView.setImageResource(R.drawable.ic_activitytracker);
                break;
            case APP_SYSTEM:
                holder.mDeviceImageView.setImageResource(R.drawable.ic_systemapp);
                break;
            case WATCHFACE:
                holder.mDeviceImageView.setImageResource(R.drawable.ic_watchface);
                break;
            default:
                holder.mDeviceImageView.setImageResource(R.drawable.ic_watchapp);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                UUID uuid = deviceApp.getUUID();
                GBApplication.deviceService().onAppStart(uuid, true);
            }
        });

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                return mParentFragment.openPopupMenu(view, deviceApp);
            }
        });

        holder.mDragHandle.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                mParentFragment.startDragging(holder);
                return true;
            }
        });

    }

    public void onItemMove(int from, int to) {
        Collections.swap(appList, from, to);
        notifyItemMoved(from, to);
    }

    public class AppViewHolder extends RecyclerView.ViewHolder {
        final TextView mDeviceAppVersionAuthorLabel;
        final TextView mDeviceAppNameLabel;
        final ImageView mDeviceImageView;
        final ImageView mDragHandle;

        AppViewHolder(View itemView) {
            super(itemView);
            mDeviceAppVersionAuthorLabel = (TextView) itemView.findViewById(R.id.item_details);
            mDeviceAppNameLabel = (TextView) itemView.findViewById(R.id.item_name);
            mDeviceImageView = (ImageView) itemView.findViewById(R.id.item_image);
            mDragHandle = (ImageView) itemView.findViewById(R.id.drag_handle);
        }

    }

}
