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
package nodomain.freeyourgadget.gadgetbridge.util;

import android.util.Pair;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A map of bounds for ranges. Returns the value closest to the key, in upper or lower bound mode.
 */
public class RangeMap<K extends Comparable<K>, V> {
    private final List<Pair<K, V>> list = new ArrayList<>();
    private boolean isSorted = false;
    private final Comparator<K> comparator;

    public RangeMap() {
        this(Mode.LOWER_BOUND);
    }

    public RangeMap(final Mode mode) {
        switch (mode) {
            case LOWER_BOUND:
                comparator = (k1, k2) -> k1.compareTo(k2);
                break;
            case UPPER_BOUND:
                comparator = (k1, k2) -> k2.compareTo(k1);
                break;
            default:
                throw new IllegalArgumentException("Unknown mode " + mode);
        }
    }

    public void put(final K key, final V value) {
        list.add(Pair.create(key, value));
        isSorted = false;
    }

    @Nullable
    public V get(final K key) {
        if (!isSorted) {
            Collections.sort(list, (a, b) -> comparator.compare(a.first, b.first));
            isSorted = true;
        }

        for (int i = list.size() - 1; i >= 0; i--) {
            if (comparator.compare(key, list.get(i).first) >= 0) {
                return list.get(i).second;
            }
        }

        return null;
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    public int size() {
        return list.size();
    }

    public enum Mode {
        LOWER_BOUND,
        UPPER_BOUND,
        ;
    }
}
