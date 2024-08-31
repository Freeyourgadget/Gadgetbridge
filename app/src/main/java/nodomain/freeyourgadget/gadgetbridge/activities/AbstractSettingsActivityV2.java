/*  Copyright (C) 2023-2024 José Rebelo

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.bytehamster.lib.preferencesearch.SearchPreferenceResult;
import com.bytehamster.lib.preferencesearch.SearchPreferenceResultListener;

import nodomain.freeyourgadget.gadgetbridge.util.SearchPreferenceHighlighter;

public abstract class AbstractSettingsActivityV2 extends AbstractGBActivity implements
        PreferenceFragmentCompat.OnPreferenceStartScreenCallback,
        SearchPreferenceResultListener {

    public static final String EXTRA_PREF_SCREEN = "preferenceScreen";
    public static final String EXTRA_PREF_HIGHLIGHT = "preferenceToHighlight";

    protected abstract PreferenceFragmentCompat newFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            PreferenceFragmentCompat fragment = (PreferenceFragmentCompat) getSupportFragmentManager().findFragmentByTag(AbstractPreferenceFragment.FRAGMENT_TAG);
            if (fragment == null) {
                fragment = newFragment();
            }
            final String prefScreen = getIntent().getStringExtra(EXTRA_PREF_SCREEN);
            if (prefScreen != null) {
                final Bundle args;
                if (fragment.getArguments() != null) {
                    args = fragment.getArguments();
                } else {
                    args = new Bundle();
                    fragment.setArguments(args);
                }
                args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, prefScreen);
            }

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, fragment, AbstractPreferenceFragment.FRAGMENT_TAG)
                    .commit();

            final String highlightKey = getIntent().getStringExtra(EXTRA_PREF_HIGHLIGHT);
            if (highlightKey != null) {
                SearchPreferenceHighlighter.highlight(fragment, highlightKey);
            }
        }
    }

    @Override
    public boolean onPreferenceStartScreen(@NonNull final PreferenceFragmentCompat caller,
                                           @NonNull final PreferenceScreen preferenceScreen) {
        final PreferenceFragmentCompat fragment = newFragment();
        final Bundle args;
        if (fragment.getArguments() != null) {
            args = fragment.getArguments();
        } else {
            args = new Bundle();
        }
        args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, preferenceScreen.getKey());
        fragment.setArguments(args);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, fragment, preferenceScreen.getKey())
                .addToBackStack(preferenceScreen.getKey())
                .commit();

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            // Simulate a back press, so that we don't actually exit the activity when
            // in a nested PreferenceScreen
            this.onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSearchResultClicked(final SearchPreferenceResult result) {
        //result.closeSearchPage(this);
        //// FIXME not sure why we need this, but the search fragment stays in the back stack otherwise
        //getSupportFragmentManager().popBackStack(SearchPreferenceFragment.TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        final FragmentManager fragmentManager = getSupportFragmentManager();
        final Fragment currentFragment = fragmentManager.findFragmentByTag(AbstractPreferenceFragment.FRAGMENT_TAG);
        if (currentFragment == null) {
            return;
        }

        if (!(currentFragment instanceof PreferenceFragmentCompat)) {
            return;
        }

        final PreferenceFragmentCompat currentPreferenceFragment = (PreferenceFragmentCompat) currentFragment;
        final String currentScreen = currentPreferenceFragment.getPreferenceScreen().getKey();

        if (result.getScreen() != null && !result.getScreen().equals(currentScreen)) {
            final PreferenceFragmentCompat newFragmentForScreen = newFragment();
            final Bundle args;
            if (newFragmentForScreen.getArguments() != null) {
                args = newFragmentForScreen.getArguments();
            } else {
                args = new Bundle();
            }
            args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, result.getScreen());
            newFragmentForScreen.setArguments(args);

            fragmentManager.beginTransaction()
                    .replace(android.R.id.content, newFragmentForScreen)
                    .addToBackStack(null)
                    .commit();
            result.highlight(newFragmentForScreen);
        } else {
            final Preference preference = currentPreferenceFragment.findPreference(result.getKey());
            if (preference != null) {
                result.highlight(currentPreferenceFragment);
            }
        }
    }

    protected void openActivityAndHighlight(final Class<? extends AbstractSettingsActivityV2> clazz,
                                            final SearchPreferenceResult result) {
        final Intent intent = new Intent(this, clazz);
        intent.putExtra(EXTRA_PREF_SCREEN, result.getScreen());
        intent.putExtra(EXTRA_PREF_HIGHLIGHT, result.getKey());
        startActivity(intent);
    }

    public void setActionBarTitle(final CharSequence title) {
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(title);
        }
    }
}
