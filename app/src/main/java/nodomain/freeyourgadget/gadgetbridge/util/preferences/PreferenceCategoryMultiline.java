package nodomain.freeyourgadget.gadgetbridge.util.preferences;

import android.content.Context;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceViewHolder;

public class PreferenceCategoryMultiline extends PreferenceCategory {

    private int maxSummaryLines = 5;

    public PreferenceCategoryMultiline(Context ctx) {
        super(ctx, null);
    }

    public PreferenceCategoryMultiline(Context ctx, int maxSummaryLines) {
        super(ctx, null);
        this.maxSummaryLines = maxSummaryLines;
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        TextView summary = (TextView) holder.findViewById(android.R.id.summary);
        if (summary == null)
            return;
        if (maxSummaryLines == 0 || maxSummaryLines == 1)
            return;
        summary.setSingleLine(false);
        summary.setMaxLines(maxSummaryLines);
    }
}
