/*  Copyright (C) 2022-2024 Arjan Schrijver, José Rebelo

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
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.ConfigureWorldClocks;
import nodomain.freeyourgadget.gadgetbridge.entities.WorldClock;

/**
 * Adapter for displaying WorldClock instances.
 */
public class GBWorldClockListAdapter extends RecyclerView.Adapter<GBWorldClockListAdapter.ViewHolder> {

    private final Context mContext;
    private ArrayList<WorldClock> worldClockList;

    public GBWorldClockListAdapter(final Context context) {
        this.mContext = context;
    }

    public void setWorldClockList(final List<WorldClock> worldClocks) {
        this.worldClockList = new ArrayList<>(worldClocks);
    }

    public ArrayList<WorldClock> getWorldClockList() {
        return worldClockList;
    }

    @NonNull
    @Override
    public GBWorldClockListAdapter.ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_world_clock, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        final WorldClock worldClock = worldClockList.get(position);

        holder.container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((ConfigureWorldClocks) mContext).configureWorldClock(worldClock);
            }
        });

        holder.container.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                new MaterialAlertDialogBuilder(v.getContext())
                        .setTitle(v.getContext().getString(R.string.world_clock_delete_confirm_title, worldClock.getLabel()))
                        .setMessage(R.string.world_clock_delete_confirm_description)
                        .setIcon(R.drawable.ic_warning)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, final int whichButton) {
                                ((ConfigureWorldClocks) mContext).deleteWorldClock(worldClock);
                            }
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .show();

                return true;
            }
        });

        holder.worldClockLabel.setText(worldClock.getLabel());
        holder.worldClockTimezone.setText(worldClock.getTimeZoneId());

        final DateFormat df = new SimpleDateFormat("HH:mm", GBApplication.getLanguage());
        df.setTimeZone(TimeZone.getTimeZone(worldClock.getTimeZoneId()));
        holder.worldClockCurrentTime.setText(df.format(new Date()));
    }

    @Override
    public int getItemCount() {
        return worldClockList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView container;

        final TextView worldClockTimezone;
        final TextView worldClockLabel;
        final TextView worldClockCurrentTime;

        ViewHolder(View view) {
            super(view);

            container = view.findViewById(R.id.card_view);

            worldClockTimezone = view.findViewById(R.id.world_clock_item_timezone);
            worldClockLabel = view.findViewById(R.id.world_clock_item_label);
            worldClockCurrentTime = view.findViewById(R.id.world_clock_current_time);
        }
    }
}
