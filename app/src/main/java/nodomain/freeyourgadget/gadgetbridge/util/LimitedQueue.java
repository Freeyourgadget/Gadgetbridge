/*  Copyright (C) 2015-2021 Andreas Shimokawa, Daniel Dakhno, Daniele Gobbetti,
    Julien Pivotto

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

import java.util.Iterator;
import java.util.LinkedList;

public class LimitedQueue<K, V> {
    private final int limit;
    private final LinkedList<Pair<K, V>> list = new LinkedList<>();

    public LimitedQueue(final int limit) {
        this.limit = limit;
    }

    synchronized public void add(final K id, final V obj) {
        if (list.size() > limit - 1) {
            list.removeFirst();
        }
        list.add(new Pair<>(id, obj));
    }

    synchronized public void remove(final K id) {
        for (final Iterator<Pair<K, V>> it = list.iterator(); it.hasNext(); ) {
            Pair<K, V> pair = it.next();
            if (id.equals(pair.first)) {
                it.remove();
            }
        }
    }

    synchronized public V lookup(final K id) {
        for (final Pair<K, V> entry : list) {
            if (id.equals(entry.first)) {
                return entry.second;
            }
        }
        return null;
    }

    synchronized public K lookupByValue(final V value){
        for (final Pair<K, V> entry : list) {
            if (value.equals(entry.second)) {
                return entry.first;
            }
        }
        return null;
    }
}
