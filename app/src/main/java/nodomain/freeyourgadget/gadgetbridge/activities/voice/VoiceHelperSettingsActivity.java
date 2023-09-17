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
package nodomain.freeyourgadget.gadgetbridge.activities.voice;

import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceFragmentCompat;

import nodomain.freeyourgadget.gadgetbridge.activities.AbstractSettingsActivityV2;

public class VoiceHelperSettingsActivity extends AbstractSettingsActivityV2 implements
        PreferenceFragmentCompat.OnPreferenceStartScreenCallback,
        ActivityCompat.OnRequestPermissionsResultCallback {
    public static final int PERMISSION_REQUEST_CODE = 0;

    @Override
    protected String fragmentTag() {
        return VoiceHelperSettingsFragment.FRAGMENT_TAG;
    }

    @Override
    protected PreferenceFragmentCompat newFragment() {
        return new VoiceHelperSettingsFragment();
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
            final Fragment fragment = fragmentManager.findFragmentByTag(VoiceHelperSettingsFragment.FRAGMENT_TAG);
            if (fragment == null) {
                return;
            }

            if (fragment instanceof VoiceHelperSettingsFragment) {
                ((VoiceHelperSettingsFragment) fragment).reloadPreferences(null);
            }
        }
    }
}
