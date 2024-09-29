package nodomain.freeyourgadget.gadgetbridge.activities.workouts.entries;

import android.graphics.Typeface;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.gridlayout.widget.GridLayout;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.activities.workouts.WorkoutValueFormatter;

public class ActivitySummaryTableRowEntry extends ActivitySummaryEntry {
    private final List<ActivitySummaryValue> columns;
    private final boolean isHeader;
    private final boolean boldFirstColumn;

    public ActivitySummaryTableRowEntry(final List<ActivitySummaryValue> columns) {
        this(null, columns, false, false);
    }

    public ActivitySummaryTableRowEntry(final String group,
                                        final List<ActivitySummaryValue> columns,
                                        final boolean isHeader,
                                        final boolean boldFirstColumn) {
        super(group);
        this.columns = columns;
        this.isHeader = isHeader;
        this.boldFirstColumn = boldFirstColumn;
    }

    @Override
    public int getColumnSpan() {
        return 2;
    }

    @Override
    public void populate(final String key, final LinearLayout linearLayout, final WorkoutValueFormatter workoutValueFormatter) {
        final GridLayout rowLayout = new GridLayout(linearLayout.getContext());
        rowLayout.setColumnCount(columns.size());
        rowLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        for (int i = 0; i < columns.size(); i++) {
            final LinearLayout cellLayout = new LinearLayout(linearLayout.getContext());
            final GridLayout.LayoutParams columnParams = new GridLayout.LayoutParams();
            columnParams.columnSpec = GridLayout.spec(i, columns.size());
            final GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL, 1f),
                    GridLayout.spec(GridLayout.UNDEFINED, 1, GridLayout.FILL, 1f)
            );
            layoutParams.width = 0;
            cellLayout.setLayoutParams(layoutParams);
            cellLayout.setOrientation(LinearLayout.VERTICAL);
            cellLayout.setGravity(Gravity.CENTER);

            final TextView columnTextView = new TextView(linearLayout.getContext());
            columnTextView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            columnTextView.setText(columns.get(i).format(workoutValueFormatter));
            columnTextView.setTextSize(12);
            if (isHeader || (i == 0 && boldFirstColumn)) {
                columnTextView.setTypeface(null, Typeface.BOLD);
            }

            cellLayout.addView(columnTextView);
            rowLayout.addView(cellLayout);
        }

        linearLayout.addView(rowLayout);
    }
}
