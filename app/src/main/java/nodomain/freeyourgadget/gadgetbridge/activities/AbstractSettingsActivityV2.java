/*  Copyright (C) 2019-2023 Andreas Shimokawa, José Rebelo

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

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import nodomain.freeyourgadget.gadgetbridge.R;

public abstract class AbstractSettingsActivityV2 extends AbstractGBActivity implements
        PreferenceFragmentCompat.OnPreferenceStartScreenCallback {

    protected abstract String fragmentTag();
    protected abstract PreferenceFragmentCompat newFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_settings);
        if (savedInstanceState == null) {
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(fragmentTag());
            if (fragment == null) {
                fragment = newFragment();
            }
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, fragment, fragmentTag())
                    .commit();
        }
    }

    @Override
    public boolean onPreferenceStartScreen(final PreferenceFragmentCompat caller, final PreferenceScreen preferenceScreen) {
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
                .replace(R.id.settings_container, fragment, preferenceScreen.getKey())
                .addToBackStack(preferenceScreen.getKey())
                .commit();

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Simulate a back press, so that we don't actually exit the activity when
                // in a nested PreferenceScreen
                this.onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void setActionBarTitle(final CharSequence title) {
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(title);
        }
    }
}
