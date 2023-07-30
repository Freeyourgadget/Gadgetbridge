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
package nodomain.freeyourgadget.gadgetbridge.capabilities.loyaltycards;

import androidx.annotation.Nullable;

import org.apache.commons.lang3.builder.CompareToBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;

public class LoyaltyCard implements Serializable, Comparable<LoyaltyCard> {

    private final int id;
    private final String name;
    private final String note;
    private final Date expiry;
    private final BigDecimal balance;
    private final Currency balanceType;
    private final String cardId;

    @Nullable
    private final String barcodeId;

    @Nullable
    private final BarcodeFormat barcodeFormat;

    @Nullable
    private final Integer color;

    private final boolean starred;
    private final boolean archived;
    private final long lastUsed;

    public LoyaltyCard(final int id,
                       final String name,
                       final String note,
                       final Date expiry,
                       final BigDecimal balance,
                       final Currency balanceType,
                       final String cardId,
                       @Nullable final String barcodeId,
                       @Nullable final BarcodeFormat barcodeFormat,
                       @Nullable final Integer color,
                       final boolean starred,
                       final boolean archived,
                       final long lastUsed) {
        this.id = id;
        this.name = name;
        this.note = note;
        this.expiry = expiry;
        this.balance = balance;
        this.balanceType = balanceType;
        this.cardId = cardId;
        this.barcodeId = barcodeId;
        this.barcodeFormat = barcodeFormat;
        this.color = color;
        this.starred = starred;
        this.archived = archived;
        this.lastUsed = lastUsed;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getNote() {
        return note;
    }

    public Date getExpiry() {
        return expiry;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public Currency getBalanceType() {
        return balanceType;
    }

    public String getCardId() {
        return cardId;
    }

    @Nullable
    public String getBarcodeId() {
        return barcodeId;
    }

    @Nullable
    public BarcodeFormat getBarcodeFormat() {
        return barcodeFormat;
    }

    @Nullable
    public Integer getColor() {
        return color;
    }

    public boolean isStarred() {
        return starred;
    }

    public boolean isArchived() {
        return archived;
    }

    public long getLastUsed() {
        return lastUsed;
    }

    @Override
    public String toString() {
        return String.format(
                Locale.ROOT,
                "LoyaltyCard{id=%s, name=%s, cardId=%s}",
                id, name, cardId
        );
    }

    @Override
    public int compareTo(final LoyaltyCard o) {
        return new CompareToBuilder()
                .append(isStarred(), o.isStarred())
                .append(isArchived(), o.isArchived())
                .append(getName(), o.getName())
                .append(getCardId(), o.getCardId())
                .build();
    }
}
