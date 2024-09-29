package nodomain.freeyourgadget.gadgetbridge.activities.workouts.entries;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.TextView;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.workouts.WorkoutValueFormatter;

public class ActivitySummarySimpleEntry extends ActivitySummaryEntry {
    private final Object value;
    private final String unit;

    public ActivitySummarySimpleEntry(final Object value, final String unit) {
        this(null, value, unit);
    }

    public ActivitySummarySimpleEntry(final String group, final Object value, final String unit) {
        super(group);
        this.value = value;
        this.unit = unit;
    }

    public Object getValue() {
        return value;
    }

    public String getUnit() {
        return unit;
    }

    @Override
    public int getColumnSpan() {
        return 1;
    }

    @Override
    public void populate(final String key, final LinearLayout linearLayout, final WorkoutValueFormatter workoutValueFormatter) {
        final Context context = linearLayout.getContext();

        // Value
        final TextView valueTextView = new TextView(context);
        valueTextView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        valueTextView.setText(context.getString(R.string.stats_empty_value));
        valueTextView.setTextSize(20);
        valueTextView.setText(workoutValueFormatter.formatValue(value, unit));

        // Label
        final TextView labelTextView = new TextView(context);
        labelTextView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        labelTextView.setTextSize(12);
        labelTextView.setText(workoutValueFormatter.getStringResourceByName(key));

        linearLayout.addView(valueTextView);
        linearLayout.addView(labelTextView);
    }
}
