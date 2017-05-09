/*  Copyright (C) 2016-2017 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti

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
package nodomain.freeyourgadget.gadgetbridge.model;

import java.util.Objects;

public class MusicSpec {
    public static final int MUSIC_UNDEFINED = 0;
    public static final int MUSIC_PLAY = 1;
    public static final int MUSIC_PAUSE = 2;
    public static final int MUSIC_PLAYPAUSE = 3;
    public static final int MUSIC_NEXT = 4;
    public static final int MUSIC_PREVIOUS = 5;

    public String artist;
    public String album;
    public String track;
    public int duration;
    public int trackCount;
    public int trackNr;

    public MusicSpec() {

    }

    public MusicSpec(MusicSpec old) {
        this.duration = old.duration;
        this.trackCount = old.trackCount;
        this.trackNr = old.trackNr;
        this.track = old.track;
        this.album = old.album;
        this.artist = old.artist;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof MusicSpec)) {
            return false;
        }
        MusicSpec musicSpec = (MusicSpec) obj;

        return Objects.equals(this.artist, musicSpec.artist) &&
                Objects.equals(this.album, musicSpec.album) &&
                Objects.equals(this.track, musicSpec.track) &&
                this.duration == musicSpec.duration &&
                this.trackCount == musicSpec.trackCount &&
                this.trackNr == musicSpec.trackNr;
    }

    @Override
    public int hashCode() {
        int result = artist != null ? artist.hashCode() : 0;
        result = 31 * result + (album != null ? album.hashCode() : 0);
        result = 31 * result + (track != null ? track.hashCode() : 0);
        result = 31 * result + duration;
        result = 31 * result + trackCount;
        result = 31 * result + trackNr;
        return result;
    }
}
