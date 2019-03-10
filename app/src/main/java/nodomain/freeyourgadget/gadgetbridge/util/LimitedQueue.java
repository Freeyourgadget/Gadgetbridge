/*  Copyright (C) 2015-2019 Andreas Shimokawa, Daniele Gobbetti, Julien Pivotto

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

public class LimitedQueue {
    private final int limit;
    private LinkedList<Pair> list = new LinkedList<>();

    public LimitedQueue(int limit) {
        this.limit = limit;
    }

    synchronized public void add(int id, Object obj) {
        if (list.size() > limit - 1) {
            list.removeFirst();
        }
        list.add(new Pair<>(id, obj));
    }

    synchronized public void remove(int id) {
        for (Iterator<Pair> iter = list.iterator(); iter.hasNext(); ) {
            Pair pair = iter.next();
            if ((Integer) pair.first == id) {
                iter.remove();
            }
        }
    }

    synchronized public Object lookup(int id) {
        for (Pair entry : list) {
            if (id == (Integer) entry.first) {
                return entry.second;
            }
        }
        return null;
    }
}
