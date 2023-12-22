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
package nodomain.freeyourgadget.gadgetbridge.util;

import android.util.Pair;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A map of lower bounds for ranges.
 */
public class RangeMap<K extends Comparable<K>, V> {
    private final List<Pair<K, V>> list = new ArrayList<>();
    private boolean isSorted = false;

    public void put(final K key, final V value) {
        list.add(Pair.create(key, value));
        isSorted = false;
    }

    @Nullable
    public V get(final K key) {
        if (!isSorted) {
            Collections.sort(list, (a, b) -> {
                return a.first.compareTo(b.first);
            });
            isSorted = true;
        }

        for (int i = list.size() - 1; i >= 0; i--) {
            if (key.compareTo(list.get(i).first) > 0) {
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
}
