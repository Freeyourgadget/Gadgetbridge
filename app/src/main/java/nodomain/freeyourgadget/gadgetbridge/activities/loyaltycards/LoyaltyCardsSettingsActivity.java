/*  Copyright (C) 2023 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.activities.loyaltycards;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;


public class LoyaltyCardsSettingsActivity extends AbstractGBActivity implements
        PreferenceFragmentCompat.OnPreferenceStartScreenCallback,
        ActivityCompat.OnRequestPermissionsResultCallback {

    public static final int PERMISSION_REQUEST_CODE = 0;

    private GBDevice device;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        device = getIntent().getParcelableExtra(GBDevice.EXTRA_DEVICE);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loyalty_cards);
        if (savedInstanceState == null) {
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(LoyaltyCardsSettingsFragment.FRAGMENT_TAG);
            if (fragment == null) {
                fragment = LoyaltyCardsSettingsFragment.newInstance(device);
            }
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, fragment, LoyaltyCardsSettingsFragment.FRAGMENT_TAG)
                    .commit();
        }
    }

    @Override
    public boolean onPreferenceStartScreen(final PreferenceFragmentCompat caller, final PreferenceScreen preferenceScreen) {
        final PreferenceFragmentCompat fragment = LoyaltyCardsSettingsFragment.newInstance(device);
        final Bundle args = fragment.getArguments();
        if (args != null) {
            args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, preferenceScreen.getKey());
            fragment.setArguments(args);
        }

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

    @Override
    public void onRequestPermissionsResult(final int requestCode,
                                           @NonNull final String[] permissions,
                                           @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != PERMISSION_REQUEST_CODE) {
            return;
        }

        if (grantResults.length == 0) {
            return;
        }

        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            final FragmentManager fragmentManager = getSupportFragmentManager();
            final Fragment fragment = fragmentManager.findFragmentByTag(LoyaltyCardsSettingsFragment.FRAGMENT_TAG);
            if (fragment == null) {
                return;
            }

            if (fragment instanceof LoyaltyCardsSettingsFragment) {
                ((LoyaltyCardsSettingsFragment) fragment).reloadPreferences(null);
            }
        }
    }
}
