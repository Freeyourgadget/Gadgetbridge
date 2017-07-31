/*  Copyright (C) 2015-2017 Andreas Shimokawa, Carsten Pfeiffer, Christian
    Fischer, Daniele Gobbetti, Lem Dulfo

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
package nodomain.freeyourgadget.gadgetbridge.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.support.v4.content.LocalBroadcastManager;
import android.text.InputType;
import android.view.MenuItem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.util.AndroidUtils;

/**
 * A settings activity with support for preferences directly displaying their value.
 * If you combine such preferences with a custom OnPreferenceChangeListener, you have
 * to set that listener in #onCreate, *not* in #onPostCreate, otherwise the value will
 * not be displayed.
 */
public abstract class AbstractSettingsActivity extends AppCompatPreferenceActivity {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractSettingsActivity.class);

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case GBApplication.ACTION_LANGUAGE_CHANGE:
                    setLanguage(GBApplication.getLanguage());
                    break;
                case GBApplication.ACTION_QUIT:
                    finish();
                    break;
            }
        }
    };


    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static class SimpleSetSummaryOnChangeListener implements Preference.OnPreferenceChangeListener {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            if (preference instanceof EditTextPreference) {
                if ((((EditTextPreference) preference).getEditText().getKeyListener().getInputType() & InputType.TYPE_CLASS_NUMBER) != 0) {
                    if ("".equals(String.valueOf(value))) {
                        // reject empty numeric input
                        return false;
                    }
                }
            }
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
        private final Preference.OnPreferenceChangeListener prefChangeListener;

        public ExtraSetSummaryOnChangeListener(Preference.OnPreferenceChangeListener prefChangeListener) {
            this.prefChangeListener = prefChangeListener;
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            boolean result = prefChangeListener.onPreferenceChange(preference, value);
            if (result) {
                return super.onPreferenceChange(preference, value);
            }
            return false;
        }
    }

    private static final SimpleSetSummaryOnChangeListener sBindPreferenceSummaryToValueListener = new SimpleSetSummaryOnChangeListener();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (GBApplication.isDarkThemeEnabled()) {
            setTheme(R.style.GadgetbridgeThemeDark);
        } else {
            setTheme(R.style.GadgetbridgeTheme);
        }

        IntentFilter filterLocal = new IntentFilter();
        filterLocal.addAction(GBApplication.ACTION_QUIT);
        filterLocal.addAction(GBApplication.ACTION_LANGUAGE_CHANGE);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filterLocal);

        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        for (String prefKey : getPreferenceKeysWithSummary()) {
            final Preference pref = findPreference(prefKey);
            if (pref != null) {
                bindPreferenceSummaryToValue(pref);
            } else {
                LOG.error("Unknown preference key: " + prefKey + ", unable to display value.");
            }
        }
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        super.onDestroy();
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

    private void setLanguage(Locale language) {
        AndroidUtils.setLanguage(this, language);
    }
}
