package nodomain.freeyourgadget.gadgetbridge.util;

import android.util.Pair;

import java.util.LinkedList;

public class LimitedQueue {
    private final int limit;
    private LinkedList<Pair> list = new LinkedList<>();

    public LimitedQueue(int limit) {
        this.limit = limit;
    }

    public void add(int id, Object sender) {
        if (list.size() > limit - 1) {
            list.removeFirst();
        }
        list.add(new Pair<>(id, sender));
    }

    public Object lookup(int id) {
        for (Pair entry : list) {
            if (id == (Integer) entry.first) {
                return entry.second;
            }
        }
        return null;
    }
}
