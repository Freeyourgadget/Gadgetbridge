/*  Copyright (C) 2020-2023 Petr Kadlec

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.notifications;

import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.SparseLongArray;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.ancs.AncsCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class NotificationStorage {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationStorage.class);

    private static final long EXPIRATION = 30 * 1000;

    private final Object lock = new Object();
    private final SparseArray<NotificationData> storage = new SparseArray<>();
    private final LinkedHashMap<Long, Set<Integer>> expirationQueue = new LinkedHashMap<>();
    private final SparseLongArray expirationForNotification = new SparseLongArray();

    private final SparseIntArray categoryCounts = new SparseIntArray(AncsCategory.values().length);

    public void registerNewNotification(NotificationData notificationData) {
        final long now = System.currentTimeMillis();
        final long expiration = now + EXPIRATION;

        final int notificationId = notificationData.spec.getId();

        synchronized (lock) {
            cleanup();
            storage.put(notificationId, notificationData);
            final int category = notificationData.category.ordinal();
            categoryCounts.put(category, categoryCounts.get(category) + 1);
            expirationForNotification.put(notificationId, expiration);

            Set<Integer> expirationSet = expirationQueue.get(expiration);
            if (expirationSet == null) {
                expirationSet = new HashSet<>(1);
                expirationQueue.put(expiration, expirationSet);
            }
            expirationSet.add(notificationId);
        }
    }

    public void deleteNotification(int id) {
        synchronized (lock) {
            final NotificationData notificationData = storage.get(id);
            if (notificationData != null) {
                storage.delete(id);
                final int categoryOrdinal = notificationData.category.ordinal();
                categoryCounts.put(categoryOrdinal, categoryCounts.get(categoryOrdinal) - 1);
            }
            final long expiration = expirationForNotification.get(id);
            final Set<Integer> expirationSet = expirationQueue.get(expiration);
            if (expirationSet != null) {
                expirationSet.remove(id);
            }
            cleanup();
        }
    }

    public NotificationData retrieveNotification(int id) {
        synchronized (lock) {
            cleanup();
            return storage.get(id);
        }
    }

    public int getCountInCategory(AncsCategory category) {
        synchronized (lock) {
            return categoryCounts.get(category.ordinal());
        }
    }

    private void cleanup() {
        final long now = System.currentTimeMillis();
        Set<Integer> expiredNotifications = null;
        for (final Map.Entry<Long, Set<Integer>> entry : expirationQueue.entrySet()) {
            final long expiration = entry.getKey();
            if (expiration > now) break;

            final Set<Integer> setToExpire = entry.getValue();
            if (expiredNotifications == null) expiredNotifications = new HashSet<>(setToExpire.size());

            expiredNotifications.addAll(setToExpire);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("{}/{}/{} notification(s) in storage, removing {}", storage.size(), expirationQueue.size(), expirationForNotification.size(), expiredNotifications == null ? 0 : expiredNotifications.size());
        }
        if (expiredNotifications == null) return;

        for (final Integer toExpire : expiredNotifications) {
            deleteNotification(toExpire);
        }
    }
}
