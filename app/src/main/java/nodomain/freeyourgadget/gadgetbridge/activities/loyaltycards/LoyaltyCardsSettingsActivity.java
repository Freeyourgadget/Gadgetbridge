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
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractSettingsActivityV2;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;


public class LoyaltyCardsSettingsActivity extends AbstractSettingsActivityV2 implements
        PreferenceFragmentCompat.OnPreferenceStartScreenCallback,
        ActivityCompat.OnRequestPermissionsResultCallback {

    public static final int PERMISSION_REQUEST_CODE = 0;

    private GBDevice device;

    @Override
    protected String fragmentTag() {
        return LoyaltyCardsSettingsFragment.FRAGMENT_TAG;
    }

    @Override
    protected PreferenceFragmentCompat newFragment() {
        return LoyaltyCardsSettingsFragment.newInstance(device);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        device = getIntent().getParcelableExtra(GBDevice.EXTRA_DEVICE);

        super.onCreate(savedInstanceState);
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
