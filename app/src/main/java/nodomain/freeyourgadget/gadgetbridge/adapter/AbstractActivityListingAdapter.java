/*  Copyright (C) 2015-2020 Andreas Shimokawa, Carsten Pfeiffer, Daniele
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
import android.content.res.Resources;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySession;

/**
 * Adapter for displaying generic ItemWithDetails instances.
 */
public abstract class AbstractActivityListingAdapter<T> extends ArrayAdapter<T> {

    private final Context context;
    private final List<T> items;
    private final int SESSION_SUMMARY = ActivitySession.SESSION_SUMMARY;
    private int backgroundColor = 0;
    private int alternateColor = 0;
    private boolean zebraStripes = true;
    private boolean showTime = true;

    public AbstractActivityListingAdapter(Context context) {
        this(context, new ArrayList<T>());
    }

    public AbstractActivityListingAdapter(Context context, List<T> items) {
        super(context, 0, items);

        this.context = context;
        this.items = items;
        alternateColor = getAlternateColor(context);
    }

    public static int getAlternateColor(Context context) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(R.attr.alternate_row_background, typedValue, true);
        return typedValue.data;
    }


    @Override
    public View getView(int position, View view, ViewGroup parent) {
        T item = getItem(position);

        if (isSummary(item, position)) {
            view = fill_dashboard(item, position, view, parent, context);
        } else if (isEmptySession(item, position)) {
            view = fill_empty(parent);
        } else {
            view = fill_item(item, position, view, parent);
        }

        return view;
    }

    public View fill_item(T item, int position, View view, ViewGroup parent) {
        if (parent != null) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.activity_list_item, parent, false);
        }
        TextView timeFrom = view.findViewById(R.id.line_layout_time_from);
        TextView timeTo = view.findViewById(R.id.line_layout_time_to);
        TextView activityName = view.findViewById(R.id.line_layout_activity_name);
        TextView stepLabel = view.findViewById(R.id.line_layout_step_label);
        TextView distanceLabel = view.findViewById(R.id.line_layout_distance_label);
        TextView hrLabel = view.findViewById(R.id.line_layout_hr_label);
        TextView intensityLabel = view.findViewById(R.id.line_layout_intensity_label);
        TextView durationLabel = view.findViewById(R.id.line_layout_duration_label);
        TextView dateLabel = view.findViewById(R.id.line_layout_date_label);

        LinearLayout timeLayout = view.findViewById(R.id.line_layout_time);
        LinearLayout hrLayout = view.findViewById(R.id.line_layout_hr);
        LinearLayout stepsLayout = view.findViewById(R.id.line_layout_step);
        LinearLayout distanceLayout = view.findViewById(R.id.line_layout_distance);
        LinearLayout intensityLayout = view.findViewById(R.id.line_layout_intensity);
        LinearLayout dateLayout = view.findViewById(R.id.line_layout_date);
        LinearLayout list_item_subparent_layout = view.findViewById(R.id.list_item_subparent_layout);

        RelativeLayout parentLayout = view.findViewById(R.id.list_item_parent_layout);

        ImageView activityIcon = view.findViewById(R.id.line_layout_activity_icon);
        ImageView gpsIcon = view.findViewById(R.id.line_layout_gps_icon);

        timeFrom.setText(getTimeFrom(item));
        timeTo.setText(getTimeTo(item));
        activityName.setText(getActivityName(item));
        stepLabel.setText(getStepLabel(item));
        distanceLabel.setText(getDistanceLabel(item));
        hrLabel.setText(getHrLabel(item));
        intensityLabel.setText(getIntensityLabel(item));
        durationLabel.setText(getDurationLabel(item));
        dateLabel.setText(getDateLabel(item));


        if (!hasHR(item)) {
            hrLayout.setVisibility(View.GONE);
        } else {
            hrLayout.setVisibility(View.VISIBLE);
        }

        if (!hasIntensity(item)) {
            intensityLayout.setVisibility(View.GONE);
        } else {
            intensityLayout.setVisibility(View.VISIBLE);
        }

        if (!hasDistance(item)) {
            distanceLayout.setVisibility(View.GONE);
        } else {
            distanceLayout.setVisibility(View.VISIBLE);
        }

        if (!hasSteps(item)) {
            stepsLayout.setVisibility(View.GONE);
        } else {
            stepsLayout.setVisibility(View.VISIBLE);
        }

        if (!hasDate(item)) {
            dateLayout.setVisibility(View.GONE);
        } else {
            dateLayout.setVisibility(View.VISIBLE);
        }

        if (!showTime) {
            timeLayout.setVisibility(View.GONE);
        } else {
            timeLayout.setVisibility(View.VISIBLE);
        }

        if (!hasGPS(item)) {
            gpsIcon.setVisibility(View.GONE);
        } else {
            gpsIcon.setVisibility(View.VISIBLE);
        }
        activityIcon.setImageResource(getIcon(item));

        if (zebraStripes && position % 2 != 0) {
            parentLayout.setBackgroundColor(alternateColor);
        }

        return view;
    }

    private View fill_empty(ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.activity_list_item, parent, false);
        view.setVisibility(View.GONE);
        return view;
    }

    protected abstract View fill_dashboard(T item, int position, View view, ViewGroup parent, Context context);

    protected abstract String getDateLabel(T item);

    protected abstract boolean hasGPS(T item);

    protected abstract boolean hasDate(T item);

    protected abstract String getTimeFrom(T item);

    protected abstract String getTimeTo(T item);

    protected abstract String getActivityName(T item);

    protected abstract String getStepLabel(T item);

    protected abstract String getDistanceLabel(T item);

    protected abstract String getHrLabel(T item);

    protected abstract String getIntensityLabel(T item);

    protected abstract String getDurationLabel(T item);

    protected abstract String getSpeedLabel(T item);

    protected abstract String getSessionCountLabel(T item);

    protected abstract boolean hasHR(T item);

    protected abstract boolean hasIntensity(T item);

    protected abstract boolean hasDistance(T item);

    protected abstract boolean hasSteps(T item);

    protected abstract boolean hasTotalSteps(T item);

    protected abstract boolean isSummary(T item, int position);

    protected abstract boolean isEmptySession(T item, int position);

    protected abstract boolean isEmptySummary(T item);

    protected abstract String getStepTotalLabel(T item);

    public void setZebraStripes(boolean enable) {
        zebraStripes = enable;
    }

    public void setShowTime(boolean enable) {
        showTime = enable;
    }

    @DrawableRes
    protected abstract int getIcon(T item);

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

    public void setActivityKindFilter(int activityKind) {
        this.setActivityKindFilter(activityKind);
    }

    public void setDateFromFilter(long date) {
    }

    public void setDateToFilter(long date) {
    }

    public void setNameContainsFilter(String name) {
    }

    public void setItemsFilter(List items) {
    }

    public void setDeviceFilter(long device) {
    }
}
