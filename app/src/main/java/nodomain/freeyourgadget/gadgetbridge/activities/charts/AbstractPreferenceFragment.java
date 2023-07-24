/*  Copyright (C) 2019-2023 Andreas Shimokawa, Cre3per, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreference;

import com.mobeta.android.dslv.DragSortListPreference;
import com.mobeta.android.dslv.DragSortListPreferenceFragment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.util.XTimePreference;
import nodomain.freeyourgadget.gadgetbridge.util.XTimePreferenceFragment;

public abstract class AbstractPreferenceFragment extends PreferenceFragmentCompat {
    protected static final Logger LOG = LoggerFactory.getLogger(AbstractPreferenceFragment.class);

    private final SharedPreferencesChangeHandler sharedPreferencesChangeHandler = new SharedPreferencesChangeHandler();

    @Override
    public void onStart() {
        super.onStart();

        final SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();

        reloadPreferences(sharedPreferences, getPreferenceScreen());

        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferencesChangeHandler);
    }

    @Override
    public void onStop() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(sharedPreferencesChangeHandler);

        super.onStop();
    }

    @Override
    public void onDisplayPreferenceDialog(final Preference preference) {
        DialogFragment dialogFragment;
        if (preference instanceof XTimePreference) {
            dialogFragment = new XTimePreferenceFragment();
            final Bundle bundle = new Bundle(1);
            bundle.putString("key", preference.getKey());
            dialogFragment.setArguments(bundle);
            dialogFragment.setTargetFragment(this, 0);
            if (getFragmentManager() != null) {
                dialogFragment.show(getFragmentManager(), "androidx.preference.PreferenceFragment.DIALOG");
            }
        } else if (preference instanceof DragSortListPreference) {
            dialogFragment = new DragSortListPreferenceFragment();
            final Bundle bundle = new Bundle(1);
            bundle.putString("key", preference.getKey());
            dialogFragment.setArguments(bundle);
            dialogFragment.setTargetFragment(this, 0);
            if (getFragmentManager() != null) {
                dialogFragment.show(getFragmentManager(), "androidx.preference.PreferenceFragment.DIALOG");
            }
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }

    /**
     * Keys of preferences which should print its values as a summary below the preference name.
     */
    protected Set<String> getPreferenceKeysWithSummary() {
        return Collections.emptySet();
    }

    protected void onSharedPreferenceChanged(final Preference preference) {
        // Nothing to do
    }

    public void setInputTypeFor(final String preferenceKey, final int editTypeFlags) {
        final EditTextPreference textPreference = findPreference(preferenceKey);
        if (textPreference != null) {
            textPreference.setOnBindEditTextListener(editText -> editText.setInputType(editTypeFlags));
        }
    }

    /**
     * Reload the preferences in the current screen. This is needed when the user enters or exists a PreferenceScreen,
     * otherwise the settings won't be reloaded by the {@link SharedPreferencesChangeHandler}, as the preferences return
     * null, since they're not visible.
     *
     * @param sharedPreferences the {@link SharedPreferences} instance
     * @param preferenceGroup   the {@link PreferenceGroup} for which preferences will be reloaded
     */
    private void reloadPreferences(final SharedPreferences sharedPreferences, final PreferenceGroup preferenceGroup) {
        if (preferenceGroup == null) {
            return;
        }

        for (int i = 0; i < preferenceGroup.getPreferenceCount(); i++) {
            final Preference preference = preferenceGroup.getPreference(i);

            LOG.debug("Reloading {}", preference.getKey());

            if (preference instanceof PreferenceCategory) {
                reloadPreferences(sharedPreferences, (PreferenceCategory) preference);
                continue;
            }

            sharedPreferencesChangeHandler.onSharedPreferenceChanged(sharedPreferences, preference.getKey());
        }
    }

    /**
     * Handler for preference changes, update UI accordingly (if device updates the preferences).
     */
    private class SharedPreferencesChangeHandler implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onSharedPreferenceChanged(final SharedPreferences prefs, final String key) {
            LOG.debug("Preference changed: {}", key);

            if (key == null) {
                LOG.warn("Preference null, ignoring");
                return;
            }

            final Preference preference = findPreference(key);
            if (preference == null) {
                LOG.warn("Preference {} not found", key);

                return;
            }

            if (preference instanceof SeekBarPreference) {
                final SeekBarPreference seekBarPreference = (SeekBarPreference) preference;
                seekBarPreference.setValue(prefs.getInt(key, seekBarPreference.getValue()));
            } else if (preference instanceof SwitchPreference) {
                final SwitchPreference switchPreference = (SwitchPreference) preference;
                switchPreference.setChecked(prefs.getBoolean(key, switchPreference.isChecked()));
            } else if (preference instanceof ListPreference) {
                final ListPreference listPreference = (ListPreference) preference;
                listPreference.setValue(prefs.getString(key, listPreference.getValue()));
            } else if (preference instanceof EditTextPreference) {
                final EditTextPreference editTextPreference = (EditTextPreference) preference;
                editTextPreference.setText(prefs.getString(key, editTextPreference.getText()));
            } else if (preference instanceof PreferenceScreen) {
                // Ignoring
            } else {
                LOG.warn("Unknown preference class {} for {}, ignoring", preference.getClass(), key);
            }

            if (getPreferenceKeysWithSummary().contains(key)) {
                final String summary = prefs.getString(key, preference.getSummary() != null ? preference.getSummary().toString() : "");
                preference.setSummary(summary);
            }

            AbstractPreferenceFragment.this.onSharedPreferenceChanged(preference);
        }
    }
}
