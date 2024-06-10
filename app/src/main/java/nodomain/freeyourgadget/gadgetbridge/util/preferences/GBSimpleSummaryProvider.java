package nodomain.freeyourgadget.gadgetbridge.util.preferences;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;

/**
 * Like the EditTextPreference.SimpleSummaryProvider, but with a customizable not-set string.
 */
public final class GBSimpleSummaryProvider implements Preference.SummaryProvider<EditTextPreference> {
    private final String defaultText;

    @StringRes
    private final int templateString;

    public GBSimpleSummaryProvider(final String defaultText) {
        this(defaultText, 0);
    }

    public GBSimpleSummaryProvider(final String defaultText, final int templateString) {
        this.defaultText = defaultText;
        this.templateString = templateString;
    }

    @Nullable
    @Override
    public CharSequence provideSummary(@NonNull final EditTextPreference preference) {
        if (TextUtils.isEmpty(preference.getText())) {
            return defaultText;
        } else if (templateString != 0) {
            return preference.getContext().getString(templateString, preference.getText());
        } else {
            return preference.getText();
        }
    }
}
