package nodomain.freeyourgadget.gadgetbridge.activities;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A settings activity with support for preferences directly displaying their value.
 * If you combine such preferences with a custom OnPreferenceChangeListener, you have
 * to set that listener in #onCreate, *not* in #onPostCreate, otherwise the value will
 * not be displayed.
 */
public abstract class AbstractSettingsActivity extends PreferenceActivity {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractSettingsActivity.class);

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static class SimpleSetSummaryOnChangeListener implements Preference.OnPreferenceChangeListener {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            updateSummary(preference, value);
            return true;
        }

        public void updateSummary(Preference preference, Object value) {
            String stringValue = String.valueOf(value);

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
        }
    }

    private static class ExtraSetSummaryOnChangeListener extends SimpleSetSummaryOnChangeListener {
        private final Preference.OnPreferenceChangeListener delegate;

        public ExtraSetSummaryOnChangeListener(Preference.OnPreferenceChangeListener delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            boolean result = delegate.onPreferenceChange(preference, value);
            if (result) {
                return super.onPreferenceChange(preference, value);
            }
            return false;
        }
    }

    private static final SimpleSetSummaryOnChangeListener sBindPreferenceSummaryToValueListener = new SimpleSetSummaryOnChangeListener();

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        for (String prefKey : getPreferenceKeysWithSummary()) {
            final Preference pref = findPreference(prefKey);
            if (pref != null) {
                bindPreferenceSummaryToValue(pref);
            } else {
                LOG.error("Unknown preference key: " + prefKey + ", unable to display value.");
            }
        }
    }

    /**
     * Subclasses should reimplement this to return the keys of those
     * preferences which should print its values as a summary below the
     * preference name.
     */
    protected String[] getPreferenceKeysWithSummary() {
        return new String[0];
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        SimpleSetSummaryOnChangeListener listener = null;
        Preference.OnPreferenceChangeListener existingListener = preference.getOnPreferenceChangeListener();
        if (existingListener != null) {
            listener = new ExtraSetSummaryOnChangeListener(existingListener);
        } else {
            listener = sBindPreferenceSummaryToValueListener;
        }
        preference.setOnPreferenceChangeListener(listener);

        // Trigger the listener immediately with the preference's current value.
        try {
            listener.updateSummary(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getString(preference.getKey(), ""));
        } catch (ClassCastException cce) {
            //the preference is not a string, use the provided summary
            //TODO: it shows true/false instead of the xml summary
            listener.updateSummary(preference, preference.getSummary());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
