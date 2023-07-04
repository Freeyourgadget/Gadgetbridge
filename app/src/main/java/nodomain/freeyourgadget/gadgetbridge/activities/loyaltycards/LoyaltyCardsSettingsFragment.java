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
import static nodomain.freeyourgadget.gadgetbridge.activities.loyaltycards.LoyaltyCardsSettingsConst.LOYALTY_CARDS_SYNC_ARCHIVED;
import static nodomain.freeyourgadget.gadgetbridge.activities.loyaltycards.LoyaltyCardsSettingsConst.LOYALTY_CARDS_SYNC_GROUPS;
import static nodomain.freeyourgadget.gadgetbridge.activities.loyaltycards.LoyaltyCardsSettingsConst.LOYALTY_CARDS_SYNC_GROUPS_ONLY;
import static nodomain.freeyourgadget.gadgetbridge.activities.loyaltycards.LoyaltyCardsSettingsConst.LOYALTY_CARDS_SYNC_STARRED;
import static nodomain.freeyourgadget.gadgetbridge.activities.loyaltycards.LoyaltyCardsSettingsConst.PREF_KEY_HEADER_LOYALTY_CARDS_CATIMA;
import static nodomain.freeyourgadget.gadgetbridge.activities.loyaltycards.LoyaltyCardsSettingsConst.PREF_KEY_HEADER_LOYALTY_CARDS_SYNC;
import static nodomain.freeyourgadget.gadgetbridge.activities.loyaltycards.LoyaltyCardsSettingsConst.PREF_KEY_HEADER_LOYALTY_CARDS_SYNC_OPTIONS;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.capabilities.loyaltycards.CatimaContentProvider;
import nodomain.freeyourgadget.gadgetbridge.capabilities.loyaltycards.CatimaManager;
import nodomain.freeyourgadget.gadgetbridge.capabilities.loyaltycards.LoyaltyCard;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class LoyaltyCardsSettingsFragment extends PreferenceFragmentCompat {
    private static final Logger LOG = LoggerFactory.getLogger(LoyaltyCardsSettingsFragment.class);

    static final String FRAGMENT_TAG = "LOYALTY_CARDS_SETTINGS_FRAGMENT";

    private GBDevice device;

    private void setSettingsFileSuffix(final String settingsFileSuffix) {
        Bundle args = new Bundle();
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

    protected void reloadPreferences(String catimaPackageName) {
        final CatimaManager catimaManager = new CatimaManager(requireContext());

        final List<CharSequence> installedCatimaPackages = catimaManager.findInstalledCatimaPackages();
        final boolean catimaInstalled = !installedCatimaPackages.isEmpty();

        final ListPreference catimaPackage = findPreference(LOYALTY_CARDS_CATIMA_PACKAGE);
        CatimaContentProvider catima = null;

        if (catimaPackage != null) {
            catimaPackage.setEntries(installedCatimaPackages.toArray(new CharSequence[0]));
            catimaPackage.setEntryValues(installedCatimaPackages.toArray(new CharSequence[0]));
            catimaPackage.setOnPreferenceChangeListener((preference, newValue) -> {
                LOG.info("Catima package changed to {}", newValue);
                reloadPreferences((String) newValue);
                return true;
            });

            if (catimaInstalled) {
                // Ensure the currently selected value is actually an installed Catima package
                if (StringUtils.isNullOrEmpty(catimaPackage.getValue()) || !installedCatimaPackages.contains(catimaPackage.getValue())) {
                    catimaPackage.setValue(installedCatimaPackages.get(0).toString());
                }
            }

            if (installedCatimaPackages.size() <= 1) {
                catimaPackage.setVisible(false);
            }

            catima = new CatimaContentProvider(requireContext(), catimaPackageName != null ? catimaPackageName : catimaPackage.getValue());
        }

        final Preference openCatima = findPreference(LOYALTY_CARDS_OPEN_CATIMA);
        if (openCatima != null) {
            openCatima.setVisible(catimaInstalled);
            openCatima.setOnPreferenceClickListener(preference -> {
                if (catimaPackage != null) {
                    final PackageManager packageManager = requireContext().getPackageManager();
                    final Intent launchIntent = packageManager.getLaunchIntentForPackage(catimaPackageName != null ? catimaPackageName : catimaPackage.getValue());
                    if (launchIntent != null) {
                        startActivity(launchIntent);
                    }
                }
                return true;
            });
        }

        final Preference catimaNotInstalled = findPreference(LOYALTY_CARDS_CATIMA_NOT_INSTALLED);
        if (catimaNotInstalled != null) {
            catimaNotInstalled.setVisible(!catimaInstalled);
        }

        final Preference installCatima = findPreference(LOYALTY_CARDS_INSTALL_CATIMA);
        if (installCatima != null) {
            installCatima.setVisible(!catimaInstalled);
            installCatima.setOnPreferenceClickListener(preference -> {
                installCatima();
                return true;
            });
        }

        final boolean permissionGranted = ContextCompat.checkSelfPermission(requireContext(), CatimaContentProvider.PERMISSION_READ_CARDS) == PackageManager.PERMISSION_GRANTED;
        final Preference catimaPermissions = findPreference(LOYALTY_CARDS_CATIMA_PERMISSIONS);
        if (catimaPermissions != null) {
            catimaPermissions.setVisible(catimaInstalled && !permissionGranted);
            catimaPermissions.setOnPreferenceClickListener(preference -> {
                ActivityCompat.requestPermissions(
                        requireActivity(),
                        new String[]{CatimaContentProvider.PERMISSION_READ_CARDS},
                        LoyaltyCardsSettingsActivity.PERMISSION_REQUEST_CODE
                );
                return true;
            });
        }

        final boolean catimaCompatible = catima != null && catima.isCatimaCompatible();
        final Preference catimaNotCompatible = findPreference(LOYALTY_CARDS_CATIMA_NOT_COMPATIBLE);
        if (catimaNotCompatible != null) {
            catimaNotCompatible.setVisible(catimaInstalled && permissionGranted && !catimaCompatible);
        }

        final Preference sync = findPreference(LOYALTY_CARDS_SYNC);
        if (sync != null) {
            sync.setEnabled(catimaInstalled);
            sync.setOnPreferenceClickListener(preference -> {
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
                syncGroups.setSummary(String.join(", ", values));
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
