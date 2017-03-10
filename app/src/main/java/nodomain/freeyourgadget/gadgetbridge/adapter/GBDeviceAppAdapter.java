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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.woxthebox.draglistview.DragItemAdapter;

import java.util.List;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.appmanager.AbstractAppManagerFragment;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceApp;

/**
 * Adapter for displaying GBDeviceApp instances.
 */

public class GBDeviceAppAdapter extends DragItemAdapter<GBDeviceApp, GBDeviceAppAdapter.ViewHolder> {

    private final int mLayoutId;
    private final int mGrabHandleId;
    private final Context mContext;
    private final AbstractAppManagerFragment mParentFragment;

    public GBDeviceAppAdapter(List<GBDeviceApp> list, int layoutId, int grabHandleId, Context context, AbstractAppManagerFragment parentFragment) {
        super(true); // longpress
        mLayoutId = layoutId;
        mGrabHandleId = grabHandleId;
        mContext = context;
        mParentFragment = parentFragment;
        setHasStableIds(true);
        setItemList(list);
    }

    @Override
    public long getItemId(int position) {
        return mItemList.get(position).getUUID().getLeastSignificantBits();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(mLayoutId, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        GBDeviceApp deviceApp = mItemList.get(position);


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
    }

    public class ViewHolder extends DragItemAdapter<GBDeviceApp, GBDeviceAppAdapter.ViewHolder>.ViewHolder {
        TextView mDeviceAppVersionAuthorLabel;
        TextView mDeviceAppNameLabel;
        ImageView mDeviceImageView;

        public ViewHolder(final View itemView) {
            super(itemView, mGrabHandleId);
            mDeviceAppVersionAuthorLabel = (TextView) itemView.findViewById(R.id.item_details);
            mDeviceAppNameLabel = (TextView) itemView.findViewById(R.id.item_name);
            mDeviceImageView = (ImageView) itemView.findViewById(R.id.item_image);
        }

        @Override
        public void onItemClicked(View view) {
            UUID uuid = mItemList.get(getAdapterPosition()).getUUID();
            GBApplication.deviceService().onAppStart(uuid, true);
        }

        @Override
        public boolean onItemLongClicked(View view) {
            return mParentFragment.openPopupMenu(view, getAdapterPosition());
        }
    }
}
