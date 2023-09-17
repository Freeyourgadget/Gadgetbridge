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

import static nodomain.freeyourgadget.gadgetbridge.activities.voice.VoiceHelperSettingsConst.VOICE_HELPER_INSTALL;
import static nodomain.freeyourgadget.gadgetbridge.activities.voice.VoiceHelperSettingsConst.VOICE_HELPER_NOT_INSTALLED;
import static nodomain.freeyourgadget.gadgetbridge.activities.voice.VoiceHelperSettingsConst.VOICE_HELPER_PACKAGE;
import static nodomain.freeyourgadget.gadgetbridge.activities.voice.VoiceHelperSettingsConst.VOICE_HELPER_PERMISSIONS;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractPreferenceFragment;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;
import nodomain.freeyourgadget.gadgetbridge.voice.VoiceHelper;

public class VoiceHelperSettingsFragment extends AbstractPreferenceFragment {
    private static final Logger LOG = LoggerFactory.getLogger(VoiceHelperSettingsFragment.class);

    static final String FRAGMENT_TAG = "VOICE_HELPER_SETTINGS_FRAGMENT";

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        setPreferencesFromResource(R.xml.voice_helper, rootKey);

        reloadPreferences(null);
    }

    protected void reloadPreferences(final String voiceHelperPackageName) {
        final List<CharSequence> installedPackages = VoiceHelper.findInstalledPackages(requireContext());
        final boolean voiceHelperInstalled = !installedPackages.isEmpty();

        final ListPreference packagePreference = findPreference(VOICE_HELPER_PACKAGE);

        if (packagePreference != null) {
            packagePreference.setEntries(installedPackages.toArray(new CharSequence[0]));
            packagePreference.setEntryValues(installedPackages.toArray(new CharSequence[0]));
            packagePreference.setVisible(!installedPackages.isEmpty());
            packagePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                LOG.info("Voice Helper package preference changed to {}", newValue);
                reloadPreferences((String) newValue);
                return true;
            });

            if (voiceHelperInstalled) {
                // Ensure the currently selected value is actually an installed package
                if (StringUtils.isNullOrEmpty(packagePreference.getValue()) || !installedPackages.contains(packagePreference.getValue())) {
                    packagePreference.setValue(installedPackages.get(0).toString());
                }
            }
        }

        final String finalPackageName;

        if (voiceHelperPackageName != null) {
            finalPackageName = voiceHelperPackageName;
        } else if (packagePreference != null) {
            finalPackageName = packagePreference.getValue();
        } else {
            LOG.warn("This should never happen - package not found");
            finalPackageName = "this.should.never.happen";
        }

        final Preference notInstalledPreference = findPreference(VOICE_HELPER_NOT_INSTALLED);
        if (notInstalledPreference != null) {
            notInstalledPreference.setVisible(!voiceHelperInstalled);
        }

        final Preference installPreference = findPreference(VOICE_HELPER_INSTALL);
        if (installPreference != null) {
            installPreference.setVisible(!voiceHelperInstalled);
            installPreference.setOnPreferenceClickListener(preference -> {
                installVoiceHelper();
                return true;
            });
        }

        final boolean permissionGranted = ContextCompat.checkSelfPermission(requireContext(), VoiceHelper.getPermission(finalPackageName)) == PackageManager.PERMISSION_GRANTED;
        final Preference permissionsPreference = findPreference(VOICE_HELPER_PERMISSIONS);
        if (permissionsPreference != null) {
            permissionsPreference.setVisible(voiceHelperInstalled && !permissionGranted);
            permissionsPreference.setOnPreferenceClickListener(preference -> {
                ActivityCompat.requestPermissions(
                        requireActivity(),
                        new String[]{VoiceHelper.getPermission(finalPackageName)},
                        VoiceHelperSettingsActivity.PERMISSION_REQUEST_CODE
                );
                return true;
            });
        }
    }

    private void installVoiceHelper() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=nodomain.freeyourgadget.voice")));
        } catch (final ActivityNotFoundException e) {
            GB.toast(requireContext(), requireContext().getString(R.string.voice_helper_install_fail), Toast.LENGTH_LONG, GB.WARN);
        }
    }
}
