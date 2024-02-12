/*  Copyright (C) 2023-2024 Andreas Shimokawa, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.activities.widgets;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.capabilities.widgets.WidgetPart;
import nodomain.freeyourgadget.gadgetbridge.capabilities.widgets.WidgetScreen;

public class WidgetScreenListAdapter extends RecyclerView.Adapter<WidgetScreenListAdapter.ViewHolder> {

    private final Context mContext;
    private ArrayList<WidgetScreen> widgetScreenList;

    public WidgetScreenListAdapter(Context context) {
        this.mContext = context;
    }

    public void setWidgetScreenList(List<WidgetScreen> widgetScreens) {
        this.widgetScreenList = new ArrayList<>(widgetScreens);
    }

    public ArrayList<WidgetScreen> getWidgetScreenList() {
        return widgetScreenList;
    }

    @NonNull
    @Override
    public WidgetScreenListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_widget_screen, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        final WidgetScreen widgetScreen = widgetScreenList.get(position);

        holder.container.setOnClickListener(v -> ((WidgetScreensListActivity) mContext).configureWidgetScreen(widgetScreen));

        holder.container.setOnLongClickListener(v -> {
            // TODO move up
            // TODO move down
            new MaterialAlertDialogBuilder(v.getContext())
                    .setTitle(R.string.widget_screen_delete_confirm_title)
                    .setMessage(mContext.getString(
                            R.string.widget_screen_delete_confirm_description,
                            mContext.getString(R.string.widget_screen_x, widgetScreen.getId())
                    ))
                    .setIcon(R.drawable.ic_warning)
                    .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                        ((WidgetScreensListActivity) mContext).deleteWidgetScreen(widgetScreen);
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .show();

            return true;
        });

        holder.widgetScreenName.setText(mContext.getString(R.string.widget_screen_x, widgetScreen.getId()));
        final List<String> widgetNames = new ArrayList<>();

        for (final WidgetPart part : widgetScreen.getParts()) {
            if (part.getId() != null) {
                widgetNames.add(part.getFullName());
            }
        }

        if (!widgetNames.isEmpty()) {
            holder.widgetScreenDescription.setText(TextUtils.join(", ", widgetNames));
        } else {
            holder.widgetScreenDescription.setText(R.string.unknown);
        }
    }

    @Override
    public int getItemCount() {
        return widgetScreenList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView container;

        final TextView widgetScreenName;
        final TextView widgetScreenDescription;

        ViewHolder(View view) {
            super(view);

            container = view.findViewById(R.id.card_widget_screen);

            widgetScreenName = view.findViewById(R.id.widget_screen_name);
            widgetScreenDescription = view.findViewById(R.id.widget_screen_description);
        }
    }
}
