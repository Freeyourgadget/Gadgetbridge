package nodomain.freeyourgadget.gadgetbridge.activities.workouts.entries;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import nodomain.freeyourgadget.gadgetbridge.activities.workouts.WorkoutValueFormatter;

public class ActivitySummaryProgressEntry extends ActivitySummarySimpleEntry {
    private final int progress;
    private int color;

    public ActivitySummaryProgressEntry(final Object value, final String unit, final int progress) {
        this(null, value, unit, progress);
    }

    public ActivitySummaryProgressEntry(final String group, final Object value, final String unit, final int progress) {
        super(group, value, unit);
        this.progress = progress;
    }

    public ActivitySummaryProgressEntry(final Object value, final String unit, final int progress, final int color) {
        this(null, value, unit, progress);
        this.color = color;
    }

    public int getProgress() {
        return progress;
    }

    @Override
    public int getColumnSpan() {
        return 2;
    }

    @Override
    public void populate(final String key, final LinearLayout linearLayout, final WorkoutValueFormatter workoutValueFormatter) {
        final Context context = linearLayout.getContext();

        // Label
        final TextView labelTextView = new TextView(context);
        labelTextView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        labelTextView.setTextSize(12);
        labelTextView.setText(workoutValueFormatter.getStringResourceByName(key));

        // Value
        final TextView valueTextView = new TextView(context);
        valueTextView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        valueTextView.setText(String.format("%s", "-"));
        valueTextView.setTextSize(12);
        valueTextView.setGravity(Gravity.END);
        valueTextView.setText(workoutValueFormatter.formatValue(getValue(), getUnit()));

        // Layout for the labels, so the value is at the right
        final LinearLayout labelsLinearLayout = new LinearLayout(context);
        labelsLinearLayout.setOrientation(LinearLayout.HORIZONTAL);
        labelsLinearLayout.addView(labelTextView);
        labelsLinearLayout.addView(valueTextView);

        final LinearLayout progressLayout = new LinearLayout(context);
        final ProgressBar progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setIndeterminate(false);
        progressBar.setProgress(progress);
        progressBar.setVisibility(View.VISIBLE);
        if (color != 0) {
            progressBar.setProgressTintList(ColorStateList.valueOf(color));
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        progressLayout.addView(progressBar, params);

        linearLayout.addView(labelsLinearLayout);
        linearLayout.addView(progressLayout);
    }
}
