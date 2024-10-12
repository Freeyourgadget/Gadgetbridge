package nodomain.freeyourgadget.gadgetbridge.model;

import android.content.Context;
import android.content.res.Resources;
import android.text.format.DateUtils;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import org.apache.commons.lang3.StringUtils;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.FormatUtils;

public class ActivityListItem {
    private final View rootView;
    private final TextView timeFromView;
    private final TextView timeToView;
    private final TextView activityName;
    private final TextView activityLabelText;
    private final TextView stepLabel;
    private final TextView distanceLabel;
    private final TextView hrLabel;
    private final TextView intensityLabel;
    private final TextView durationLabel;
    private final TextView dateLabel;
    private final LinearLayout timeLayout;
    private final LinearLayout hrLayout;
    private final LinearLayout stepsLayout;
    private final LinearLayout distanceLayout;
    private final LinearLayout intensityLayout;
    private final RelativeLayout parentLayout;
    private final ImageView activityIcon;
    private final ImageView gpsIcon;

    private final int backgroundColor;
    private final int alternateColor;
    private final int selectedColor;

    public ActivityListItem(final View itemView) {
        this.rootView = itemView;

        this.timeFromView = itemView.findViewById(R.id.line_layout_time_from);
        this.timeToView = itemView.findViewById(R.id.line_layout_time_to);
        this.activityName = itemView.findViewById(R.id.line_layout_activity_name);
        this.activityLabelText = itemView.findViewById(R.id.line_layout_activity_label);
        this.stepLabel = itemView.findViewById(R.id.line_layout_step_label);
        this.distanceLabel = itemView.findViewById(R.id.line_layout_distance_label);
        this.hrLabel = itemView.findViewById(R.id.line_layout_hr_label);
        this.intensityLabel = itemView.findViewById(R.id.line_layout_intensity_label);
        this.durationLabel = itemView.findViewById(R.id.line_layout_duration_label);
        this.dateLabel = itemView.findViewById(R.id.line_layout_date_label);

        this.timeLayout = itemView.findViewById(R.id.line_layout_time);
        this.hrLayout = itemView.findViewById(R.id.line_layout_hr);
        this.stepsLayout = itemView.findViewById(R.id.line_layout_step);
        this.distanceLayout = itemView.findViewById(R.id.line_layout_distance);
        this.intensityLayout = itemView.findViewById(R.id.line_layout_intensity);

        this.parentLayout = itemView.findViewById(R.id.list_item_parent_layout);

        this.activityIcon = itemView.findViewById(R.id.line_layout_activity_icon);
        this.gpsIcon = itemView.findViewById(R.id.line_layout_gps_icon);

        this.backgroundColor = 0;
        this.alternateColor = getThemedColor(itemView.getContext(), R.attr.alternate_row_background);
        this.selectedColor = ContextCompat.getColor(itemView.getContext(), R.color.accent);
    }

    public void update(@Nullable final Date timeFrom,
                       @Nullable final Date timeTo,
                       final ActivityKind activityKind,
                       @Nullable final String activityLabel,
                       final int steps,
                       final float distance,
                       final int heartRate,
                       final float intensity,
                       final long duration,
                       final boolean hasGps,
                       @Nullable final Date date,
                       final boolean zebraStripe,
                       final boolean selected) {
        final String activityKindLabel = activityKind.getLabel(activityName.getContext());
        if (StringUtils.isNotBlank(activityLabel)) {
            activityLabelText.setText(String.format("%s", activityKindLabel));
            activityName.setText(String.format("%s", activityLabel));
        } else {
            activityLabelText.setVisibility(View.GONE);
            activityName.setText(String.format("%s", activityKindLabel));
        }
        durationLabel.setText(DateTimeUtils.formatDurationHoursMinutes(duration, TimeUnit.MILLISECONDS));

        if (heartRate > 0) {
            hrLabel.setText(String.valueOf(heartRate));
            hrLayout.setVisibility(View.VISIBLE);
        } else {
            hrLayout.setVisibility(View.GONE);
        }

        if (intensity >= 0) {
            final DecimalFormat df = new DecimalFormat("###");
            intensityLabel.setText(df.format(intensity));
            intensityLayout.setVisibility(View.VISIBLE);
        } else {
            intensityLayout.setVisibility(View.GONE);
        }

        if (distance > 0) {
            distanceLabel.setText(FormatUtils.getFormattedDistanceLabel(distance));
            distanceLayout.setVisibility(View.VISIBLE);
        } else {
            distanceLayout.setVisibility(View.GONE);
        }

        if (steps > 0) {
            stepLabel.setText(String.valueOf(steps));
            stepsLayout.setVisibility(View.VISIBLE);
        } else {
            stepsLayout.setVisibility(View.GONE);
        }

        if (date != null) {
            dateLabel.setText(DateTimeUtils.formatDateTimeRelative(rootView.getContext(), date));
            dateLabel.setVisibility(View.VISIBLE);
        } else {
            dateLabel.setVisibility(View.GONE);
        }

        if (timeFrom != null && timeTo != null) {
            timeFromView.setText(DateTimeUtils.formatTime(timeFrom.getHours(), timeFrom.getMinutes()));
            timeToView.setText(DateTimeUtils.formatTime(timeTo.getHours(), timeTo.getMinutes()));
            timeLayout.setVisibility(View.VISIBLE);
        } else {
            timeLayout.setVisibility(View.GONE);
        }

        if (hasGps) {
            gpsIcon.setVisibility(View.VISIBLE);
        } else {
            gpsIcon.setVisibility(View.GONE);
        }

        activityIcon.setImageResource(activityKind.getIcon());

        if (parentLayout != null) {
            if (selected) {
                parentLayout.setBackgroundColor(selectedColor);
            } else {
                parentLayout.setBackgroundColor(backgroundColor);
            }
        }
    }

    public static int getThemedColor(Context context, int resid) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(resid, typedValue, true);
        return typedValue.data;
    }
}
