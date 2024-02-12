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
package nodomain.freeyourgadget.gadgetbridge.capabilities.loyaltycards;

import static nodomain.freeyourgadget.gadgetbridge.activities.loyaltycards.LoyaltyCardsSettingsConst.LOYALTY_CARDS_CATIMA_PACKAGE;
import static nodomain.freeyourgadget.gadgetbridge.activities.loyaltycards.LoyaltyCardsSettingsConst.LOYALTY_CARDS_SYNC_ARCHIVED;
import static nodomain.freeyourgadget.gadgetbridge.activities.loyaltycards.LoyaltyCardsSettingsConst.LOYALTY_CARDS_SYNC_GROUPS;
import static nodomain.freeyourgadget.gadgetbridge.activities.loyaltycards.LoyaltyCardsSettingsConst.LOYALTY_CARDS_SYNC_GROUPS_ONLY;
import static nodomain.freeyourgadget.gadgetbridge.activities.loyaltycards.LoyaltyCardsSettingsConst.LOYALTY_CARDS_SYNC_STARRED;

import android.content.Context;
import android.content.pm.PackageManager;
import android.widget.Toast;

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
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class CatimaManager {
    private static final Logger LOG = LoggerFactory.getLogger(CatimaManager.class);

    private final Context context;

    public CatimaManager(final Context context) {
        this.context = context;
    }

    public void sync(final GBDevice gbDevice) {
        final List<CharSequence> installedCatimaPackages = findInstalledCatimaPackages();
        if (installedCatimaPackages.isEmpty()) {
            LOG.warn("Catima is not installed");
            return;
        }

        final Prefs prefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()));

        final boolean syncGroupsOnly = prefs.getBoolean(LOYALTY_CARDS_SYNC_GROUPS_ONLY, false);
        final Set<String> syncGroups = prefs.getStringSet(LOYALTY_CARDS_SYNC_GROUPS, Collections.emptySet());
        final boolean syncArchived = prefs.getBoolean(LOYALTY_CARDS_SYNC_ARCHIVED, false);
        final boolean syncStarred = prefs.getBoolean(LOYALTY_CARDS_SYNC_STARRED, false);

        final String catimaPackage = prefs.getString(LOYALTY_CARDS_CATIMA_PACKAGE, installedCatimaPackages.get(0).toString());
        final CatimaContentProvider catima = new CatimaContentProvider(context, catimaPackage);

        if (!catima.isCatimaCompatible()) {
            LOG.warn("Catima is not compatible");
            return;
        }

        final List<LoyaltyCard> cards = catima.getCards();
        final Map<String, List<Integer>> groupCards = catima.getGroupCards();

        final Set<Integer> cardsInGroupsToSync = new HashSet<>();
        if (syncGroupsOnly) {
            for (final Map.Entry<String, List<Integer>> groupCardsEntry : groupCards.entrySet()) {
                if (syncGroups.contains(groupCardsEntry.getKey())) {
                    cardsInGroupsToSync.addAll(groupCardsEntry.getValue());
                }
            }
        }

        final ArrayList<LoyaltyCard> cardsToSync = new ArrayList<>();
        for (final LoyaltyCard card : cards) {
            if (syncGroupsOnly && !cardsInGroupsToSync.contains(card.getId())) {
                continue;
            }
            if (!syncArchived && card.isArchived()) {
                continue;
            }
            if (syncStarred && !card.isStarred()) {
                continue;
            }
            cardsToSync.add(card);
        }

        Collections.sort(cardsToSync);

        LOG.debug("Will sync cards: {}", cardsToSync);

        GB.toast(context, context.getString(R.string.loyalty_cards_syncing, cardsToSync.size()), Toast.LENGTH_LONG, GB.INFO);

        GBApplication.deviceService(gbDevice).onSetLoyaltyCards(cardsToSync);
    }

    public List<CharSequence> findInstalledCatimaPackages() {
        final List<CharSequence> installedCatimaPackages = new ArrayList<>();
        for (final String knownPackage : CatimaContentProvider.KNOWN_PACKAGES) {
            if (isPackageInstalled(knownPackage)) {
                installedCatimaPackages.add(knownPackage);
            }
        }
        return installedCatimaPackages;
    }

    private boolean isPackageInstalled(final String packageName) {
        try {
            return context.getPackageManager().getApplicationInfo(packageName, 0).enabled;
        } catch (final PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
