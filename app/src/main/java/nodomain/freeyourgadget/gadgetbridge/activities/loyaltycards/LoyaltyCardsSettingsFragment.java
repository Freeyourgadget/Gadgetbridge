/*  Copyright (C) 2022 José Rebelo

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

import static nodomain.freeyourgadget.gadgetbridge.activities.loyaltycards.LoyaltyCardsSettingsConst.LOYALTY_CARDS_CATIMA_NOT_COMPATIBLE;
import static nodomain.freeyourgadget.gadgetbridge.activities.loyaltycards.LoyaltyCardsSettingsConst.LOYALTY_CARDS_CATIMA_NOT_INSTALLED;
import static nodomain.freeyourgadget.gadgetbridge.activities.loyaltycards.LoyaltyCardsSettingsConst.LOYALTY_CARDS_CATIMA_PACKAGE;
import static nodomain.freeyourgadget.gadgetbridge.activities.loyaltycards.LoyaltyCardsSettingsConst.LOYALTY_CARDS_CATIMA_PERMISSIONS;
import static nodomain.freeyourgadget.gadgetbridge.activities.loyaltycards.LoyaltyCardsSettingsConst.LOYALTY_CARDS_INSTALL_CATIMA;
import static nodomain.freeyourgadget.gadgetbridge.activities.loyaltycards.LoyaltyCardsSettingsConst.LOYALTY_CARDS_OPEN_CATIMA;
import static nodomain.freeyourgadget.gadgetbridge.activities.loyaltycards.LoyaltyCardsSettingsConst.LOYALTY_CARDS_SYNC;
import static nodomain.freeyourgadget.gadgetbridge.activities.loyaltycards.LoyaltyCardsSettingsConst.LOYALTY_CARDS_SYNC_GROUPS;
import static nodomain.freeyourgadget.gadgetbridge.activities.loyaltycards.LoyaltyCardsSettingsConst.PREF_KEY_HEADER_LOYALTY_CARDS_CATIMA;
import static nodomain.freeyourgadget.gadgetbridge.activities.loyaltycards.LoyaltyCardsSettingsConst.PREF_KEY_HEADER_LOYALTY_CARDS_SYNC;
import static nodomain.freeyourgadget.gadgetbridge.activities.loyaltycards.LoyaltyCardsSettingsConst.PREF_KEY_HEADER_LOYALTY_CARDS_SYNC_OPTIONS;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractPreferenceFragment;
import nodomain.freeyourgadget.gadgetbridge.capabilities.loyaltycards.CatimaContentProvider;
import nodomain.freeyourgadget.gadgetbridge.capabilities.loyaltycards.CatimaManager;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class LoyaltyCardsSettingsFragment extends AbstractPreferenceFragment {
    private static final Logger LOG = LoggerFactory.getLogger(LoyaltyCardsSettingsFragment.class);

    static final String FRAGMENT_TAG = "LOYALTY_CARDS_SETTINGS_FRAGMENT";

    private GBDevice device;

    private void setSettingsFileSuffix(final String settingsFileSuffix) {
        final Bundle args = new Bundle();
        args.putString("settingsFileSuffix", settingsFileSuffix);
        setArguments(args);
    }

    private void setDevice(final GBDevice device) {
        final Bundle args = getArguments() != null ? getArguments() : new Bundle();
        args.putParcelable("device", device);
        setArguments(args);
    }

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        final Bundle arguments = getArguments();
        if (arguments == null) {
            return;
        }
        final String settingsFileSuffix = arguments.getString("settingsFileSuffix", null);
        this.device = arguments.getParcelable("device");

        if (settingsFileSuffix == null) {
            return;
        }

        getPreferenceManager().setSharedPreferencesName("devicesettings_" + settingsFileSuffix);
        setPreferencesFromResource(R.xml.loyalty_cards, rootKey);

        reloadPreferences(null);
    }

    static LoyaltyCardsSettingsFragment newInstance(GBDevice device) {
        final String settingsFileSuffix = device.getAddress();
        final LoyaltyCardsSettingsFragment fragment = new LoyaltyCardsSettingsFragment();
        fragment.setSettingsFileSuffix(settingsFileSuffix);
        fragment.setDevice(device);

        return fragment;
    }

    protected void reloadPreferences(final String catimaPackageName) {
        final CatimaManager catimaManager = new CatimaManager(requireContext());

        final List<CharSequence> installedCatimaPackages = catimaManager.findInstalledCatimaPackages();
        final boolean catimaInstalled = !installedCatimaPackages.isEmpty();

        final ListPreference catimaPackagePreference = findPreference(LOYALTY_CARDS_CATIMA_PACKAGE);

        if (catimaPackagePreference != null) {
            catimaPackagePreference.setEntries(installedCatimaPackages.toArray(new CharSequence[0]));
            catimaPackagePreference.setEntryValues(installedCatimaPackages.toArray(new CharSequence[0]));
            catimaPackagePreference.setVisible(installedCatimaPackages.size() > 1);
            catimaPackagePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                LOG.info("Catima package preference changed to {}", newValue);
                reloadPreferences((String) newValue);
                return true;
            });

            if (catimaInstalled) {
                // Ensure the currently selected value is actually an installed Catima package
                if (StringUtils.isNullOrEmpty(catimaPackagePreference.getValue()) || !installedCatimaPackages.contains(catimaPackagePreference.getValue())) {
                    catimaPackagePreference.setValue(installedCatimaPackages.get(0).toString());
                }
            }
        }

        final String finalCatimaPackageName;

        if (catimaPackageName != null) {
            finalCatimaPackageName = catimaPackageName;
        } else if (catimaPackagePreference != null) {
            finalCatimaPackageName = catimaPackagePreference.getValue();
        } else {
            LOG.warn("This should never happen - catima package not found");
            finalCatimaPackageName = "this.should.never.happen";
        }

        final CatimaContentProvider catima = new CatimaContentProvider(requireContext(), finalCatimaPackageName);

        final Preference openCatimaPreference = findPreference(LOYALTY_CARDS_OPEN_CATIMA);
        if (openCatimaPreference != null) {
            openCatimaPreference.setVisible(catimaInstalled);
            openCatimaPreference.setOnPreferenceClickListener(preference -> {
                if (catimaPackagePreference != null) {
                    final PackageManager packageManager = requireContext().getPackageManager();
                    final Intent launchIntent = packageManager.getLaunchIntentForPackage(finalCatimaPackageName);
                    if (launchIntent != null) {
                        startActivity(launchIntent);
                    }
                }
                return true;
            });
        }

        final Preference catimaNotInstalledPreference = findPreference(LOYALTY_CARDS_CATIMA_NOT_INSTALLED);
        if (catimaNotInstalledPreference != null) {
            catimaNotInstalledPreference.setVisible(!catimaInstalled);
        }

        final Preference installCatimaPreference = findPreference(LOYALTY_CARDS_INSTALL_CATIMA);
        if (installCatimaPreference != null) {
            installCatimaPreference.setVisible(!catimaInstalled);
            installCatimaPreference.setOnPreferenceClickListener(preference -> {
                installCatima();
                return true;
            });
        }

        final boolean permissionGranted = ContextCompat.checkSelfPermission(requireContext(), catima.getReadPermission()) == PackageManager.PERMISSION_GRANTED;
        final Preference catimaPermissionsPreference = findPreference(LOYALTY_CARDS_CATIMA_PERMISSIONS);
        if (catimaPermissionsPreference != null) {
            catimaPermissionsPreference.setVisible(catimaInstalled && !permissionGranted);
            catimaPermissionsPreference.setOnPreferenceClickListener(preference -> {
                ActivityCompat.requestPermissions(
                        requireActivity(),
                        new String[]{catima.getReadPermission()},
                        LoyaltyCardsSettingsActivity.PERMISSION_REQUEST_CODE
                );
                return true;
            });
        }

        final boolean catimaCompatible = catima.isCatimaCompatible();
        final Preference catimaNotCompatiblePreference = findPreference(LOYALTY_CARDS_CATIMA_NOT_COMPATIBLE);
        if (catimaNotCompatiblePreference != null) {
            catimaNotCompatiblePreference.setVisible(catimaInstalled && permissionGranted && !catimaCompatible);
        }

        final Preference syncPreference = findPreference(LOYALTY_CARDS_SYNC);
        if (syncPreference != null) {
            syncPreference.setEnabled(catimaInstalled && catimaCompatible && permissionGranted);
            syncPreference.setOnPreferenceClickListener(preference -> {
                catimaManager.sync(device);
                return true;
            });
        }

        final PreferenceCategory headerCatima = findPreference(PREF_KEY_HEADER_LOYALTY_CARDS_CATIMA);
        if (headerCatima != null) {
            boolean allHidden = true;
            for (int i = 0; i < headerCatima.getPreferenceCount(); i++) {
                if (headerCatima.getPreference(i).isVisible()) {
                    allHidden = false;
                    break;
                }
            }
            headerCatima.setVisible(!allHidden);
        }

        if (catimaInstalled && catimaCompatible && permissionGranted) {
            final MultiSelectListPreference syncGroups = findPreference(LOYALTY_CARDS_SYNC_GROUPS);
            if (syncGroups != null) {
                final List<String> groups = catima.getGroups();
                final CharSequence[] entries = groups.toArray(new CharSequence[0]);
                syncGroups.setEntries(entries);
                syncGroups.setEntryValues(entries);

                // Remove groups that do not exist anymore from the summary
                final Set<String> values = new HashSet<>(syncGroups.getValues());
                final Set<String> toRemove = new HashSet<>();
                for (final String group : values) {
                    if (!groups.contains(group)) {
                        toRemove.add(group);
                    }
                }
                values.removeAll(toRemove);
                syncGroups.setSummary(TextUtils.join(", ", values));
            }
        }

        final boolean allowSync = catimaInstalled && permissionGranted && catimaCompatible;
        final PreferenceCategory syncCategory = findPreference(PREF_KEY_HEADER_LOYALTY_CARDS_SYNC);
        if (syncCategory != null) {
            for (int i = 0; i < syncCategory.getPreferenceCount(); i++) {
                syncCategory.getPreference(i).setEnabled(allowSync);
            }
        }
        final PreferenceCategory syncOptionsCategory = findPreference(PREF_KEY_HEADER_LOYALTY_CARDS_SYNC_OPTIONS);
        if (syncOptionsCategory != null) {
            for (int i = 0; i < syncOptionsCategory.getPreferenceCount(); i++) {
                syncOptionsCategory.getPreference(i).setEnabled(allowSync);
            }
        }
    }

    private void installCatima() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=me.hackerchick.catima")));
        } catch (final ActivityNotFoundException e) {
            GB.toast(requireContext(), requireContext().getString(R.string.loyalty_cards_install_catima_fail), Toast.LENGTH_LONG, GB.WARN);
        }
    }
}
