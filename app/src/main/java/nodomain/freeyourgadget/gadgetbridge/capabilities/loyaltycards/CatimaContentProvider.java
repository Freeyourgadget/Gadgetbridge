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

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CatimaContentProvider {
    private static final Logger LOG = LoggerFactory.getLogger(CatimaContentProvider.class);

    public static final List<String> KNOWN_PACKAGES = new ArrayList<String>() {{
        add("me.hackerchick.catima");
        add("me.hackerchick.catima.debug");
    }};

    private final Context mContext;
    private final Uri versionUri;
    private final Uri cardsUri;
    private final Uri groupsUri;
    private final Uri cardGroupsUri;
    private final String readPermission;

    public CatimaContentProvider(final Context context, final String catimaPackageName) {
        this.mContext = context;
        final String catimaAuthority = catimaPackageName + ".contentprovider.cards";
        this.versionUri = Uri.parse(String.format(Locale.ROOT, "content://%s/version", catimaAuthority));
        this.cardsUri = Uri.parse(String.format(Locale.ROOT, "content://%s/cards", catimaAuthority));
        this.groupsUri = Uri.parse(String.format(Locale.ROOT, "content://%s/groups", catimaAuthority));
        this.cardGroupsUri = Uri.parse(String.format(Locale.ROOT, "content://%s/card_groups", catimaAuthority));
        this.readPermission = String.format(Locale.ROOT, "%s.READ_CARDS", catimaPackageName);
    }

    public String getReadPermission() {
        return this.readPermission;
    }

    public boolean isCatimaCompatible() {
        final ContentResolver contentResolver = mContext.getContentResolver();
        try (Cursor cursor = contentResolver.query(versionUri, null, null, null, null)) {
            if (cursor == null || cursor.getCount() == 0) {
                LOG.warn("Catima content provider version not found");
                return false;
            }

            cursor.moveToNext();
            final int major = cursor.getInt(cursor.getColumnIndexOrThrow("major"));
            final int minor = cursor.getInt(cursor.getColumnIndexOrThrow("minor"));

            LOG.info("Got catima content provider version: {}.{}", major, minor);

            // We only support version 1.x for now
            return major == 1;
        } catch (final Exception e) {
            LOG.error("Failed to get content provider version from Catima", e);
        }

        return false;
    }

    public List<LoyaltyCard> getCards() {
        final List<LoyaltyCard> cards = new ArrayList<>();
        final ContentResolver contentResolver = mContext.getContentResolver();
        try (Cursor cursor = contentResolver.query(cardsUri, null, null, null, null)) {
            if (cursor == null || cursor.getCount() == 0) {
                LOG.debug("No cards found");
                return cards;
            }

            while (cursor.moveToNext()) {
                final LoyaltyCard loyaltyCard = toLoyaltyCard(cursor);
                cards.add(loyaltyCard);
            }
        } catch (final Exception e) {
            LOG.error("Failed to list cards from Catima", e);
            return cards;
        }

        return cards;
    }

    public List<String> getGroups() {
        final List<String> groups = new ArrayList<>();
        final ContentResolver contentResolver = mContext.getContentResolver();
        try (Cursor cursor = contentResolver.query(groupsUri, null, null, null, null)) {
            if (cursor == null || cursor.getCount() == 0) {
                LOG.debug("No groups found");
                return groups;
            }

            while (cursor.moveToNext()) {
                final String groupId = cursor.getString(cursor.getColumnIndexOrThrow(LoyaltyCardDbGroups.ID));
                groups.add(groupId);
            }
        } catch (final Exception e) {
            LOG.error("Failed to list groups from Catima", e);
            return groups;
        }

        return groups;
    }

    /**
     * Gets the mapping of group to list of card IDs.
     *
     * @return the mapping of group to list of card IDS.
     */
    public Map<String, List<Integer>> getGroupCards() {
        final Map<String, List<Integer>> groupCards = new HashMap<>();
        final ContentResolver contentResolver = mContext.getContentResolver();
        try (Cursor cursor = contentResolver.query(cardGroupsUri, null, null, null, null)) {
            if (cursor == null || cursor.getCount() == 0) {
                LOG.debug("No card groups found");
                return groupCards;
            }

            while (cursor.moveToNext()) {
                final int cardId = cursor.getInt(cursor.getColumnIndexOrThrow(LoyaltyCardDbIdsGroups.cardID));
                final String groupId = cursor.getString(cursor.getColumnIndexOrThrow(LoyaltyCardDbIdsGroups.groupID));
                final List<Integer> group;
                if (groupCards.containsKey(groupId)) {
                    group = groupCards.get(groupId);
                } else {
                    group = new ArrayList<>();
                    groupCards.put(groupId, group);
                }
                group.add(cardId);
            }
        } catch (final Exception e) {
            LOG.error("Failed to get group cards from Catima", e);
            return groupCards;
        }

        return groupCards;
    }

    public static LoyaltyCard toLoyaltyCard(final Cursor cursor) {
        final int id = cursor.getInt(cursor.getColumnIndexOrThrow(LoyaltyCardDbIds.ID));
        final String name = cursor.getString(cursor.getColumnIndexOrThrow(LoyaltyCardDbIds.STORE));
        final String note = cursor.getString(cursor.getColumnIndexOrThrow(LoyaltyCardDbIds.NOTE));
        final long expiryLong = cursor.getLong(cursor.getColumnIndexOrThrow(LoyaltyCardDbIds.EXPIRY));
        final BigDecimal balance = new BigDecimal(cursor.getString(cursor.getColumnIndexOrThrow(LoyaltyCardDbIds.BALANCE)));
        final String cardId = cursor.getString(cursor.getColumnIndexOrThrow(LoyaltyCardDbIds.CARD_ID));
        final String barcodeId = cursor.getString(cursor.getColumnIndexOrThrow(LoyaltyCardDbIds.BARCODE_ID));
        final int starred = cursor.getInt(cursor.getColumnIndexOrThrow(LoyaltyCardDbIds.STAR_STATUS));
        final long lastUsed = cursor.getLong(cursor.getColumnIndexOrThrow(LoyaltyCardDbIds.LAST_USED));
        final int archiveStatus = cursor.getInt(cursor.getColumnIndexOrThrow(LoyaltyCardDbIds.ARCHIVE_STATUS));

        int barcodeTypeColumn = cursor.getColumnIndexOrThrow(LoyaltyCardDbIds.BARCODE_TYPE);
        int balanceTypeColumn = cursor.getColumnIndexOrThrow(LoyaltyCardDbIds.BALANCE_TYPE);
        int headerColorColumn = cursor.getColumnIndexOrThrow(LoyaltyCardDbIds.HEADER_COLOR);

        BarcodeFormat barcodeFormat = null;
        Currency balanceType = null;
        Date expiry = null;
        Integer headerColor = null;

        if (!cursor.isNull(barcodeTypeColumn)) {
            try {
                barcodeFormat = BarcodeFormat.valueOf(cursor.getString(barcodeTypeColumn));
            } catch (final IllegalArgumentException e) {
                LOG.error("Unknown barcode format {}", barcodeTypeColumn);
            }
        }

        if (!cursor.isNull(balanceTypeColumn)) {
            balanceType = Currency.getInstance(cursor.getString(balanceTypeColumn));
        }

        if (expiryLong > 0) {
            expiry = new Date(expiryLong);
        }

        if (!cursor.isNull(headerColorColumn)) {
            headerColor = cursor.getInt(headerColorColumn);
        }

        return new LoyaltyCard(
                id,
                name,
                note,
                expiry,
                balance,
                balanceType,
                cardId,
                barcodeId,
                barcodeFormat,
                headerColor,
                starred != 0,
                archiveStatus != 0,
                lastUsed
        );
    }

    /**
     * Copied from Catima, protect.card_locker.DBHelper.LoyaltyCardDbGroups.
     * Commit: 8607e1c2
     */
    public static class LoyaltyCardDbGroups {
        public static final String TABLE = "groups";
        public static final String ID = "_id";
        public static final String ORDER = "orderId";
    }

    /**
     * Copied from Catima, protect.card_locker.DBHelper.LoyaltyCardDbIds.
     * Commit: 8607e1c2
     */
    public static class LoyaltyCardDbIds {
        public static final String TABLE = "cards";
        public static final String ID = "_id";
        public static final String STORE = "store";
        public static final String EXPIRY = "expiry";
        public static final String BALANCE = "balance";
        public static final String BALANCE_TYPE = "balancetype";
        public static final String NOTE = "note";
        public static final String HEADER_COLOR = "headercolor";
        public static final String HEADER_TEXT_COLOR = "headertextcolor";
        public static final String CARD_ID = "cardid";
        public static final String BARCODE_ID = "barcodeid";
        public static final String BARCODE_TYPE = "barcodetype";
        public static final String STAR_STATUS = "starstatus";
        public static final String LAST_USED = "lastused";
        public static final String ZOOM_LEVEL = "zoomlevel";
        public static final String ARCHIVE_STATUS = "archive";
    }

    /**
     * Copied from Catima, protect.card_locker.DBHelper.LoyaltyCardDbIdsGroups.
     * Commit: 8607e1c2
     */
    public static class LoyaltyCardDbIdsGroups {
        public static final String TABLE = "cardsGroups";
        public static final String cardID = "cardId";
        public static final String groupID = "groupId";
    }
}
