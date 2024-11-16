package nodomain.freeyourgadget.gadgetbridge.impl;

import java.io.Serializable;

public class GBDeviceMusic implements Serializable {
    private final int id;
    private final String title;
    private final String artist;
    private final String fileName;

    public GBDeviceMusic(int id, String title, String artist, String fileName) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.fileName = fileName;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getFileName() {
        return fileName;
    }
}
