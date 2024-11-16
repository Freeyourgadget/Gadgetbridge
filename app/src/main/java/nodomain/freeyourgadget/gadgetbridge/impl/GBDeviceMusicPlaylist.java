package nodomain.freeyourgadget.gadgetbridge.impl;

import java.io.Serializable;
import java.util.ArrayList;

public class GBDeviceMusicPlaylist implements Serializable {
    private final int id;
    private String name;
    private ArrayList<Integer> musicIds;

    public GBDeviceMusicPlaylist(int id, String name, ArrayList<Integer> musicIds) {
        this.id = id;
        this.name = name;
        this.musicIds = musicIds;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<Integer> getMusicIds() {
        return musicIds;
    }

    public void setMusicIds(ArrayList<Integer> musicIds) {
        this.musicIds = musicIds;
    }

    @Override
    public String toString() {
        return name;
    }
}
