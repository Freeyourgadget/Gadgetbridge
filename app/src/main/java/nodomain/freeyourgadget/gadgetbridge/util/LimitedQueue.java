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
