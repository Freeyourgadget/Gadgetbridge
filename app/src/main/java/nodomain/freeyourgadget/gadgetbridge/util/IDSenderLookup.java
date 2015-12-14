package nodomain.freeyourgadget.gadgetbridge.util;

import android.util.Pair;

import java.util.LinkedList;

public class IDSenderLookup {
    private static final int LIMIT = 16;
    private LinkedList<Pair> list = new LinkedList<>();

    public void add(int id, String sender) {
        if (list.size() > LIMIT - 1) {
            list.removeFirst();
        }
        list.add(new Pair<>(id, sender));
    }

    public String lookup(int id) {
        for (Pair entry : list) {
            if (id == (Integer) entry.first) {
                return (String) entry.second;
            }
        }
        return null;
    }
}
