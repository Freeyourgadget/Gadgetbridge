/*  Copyright (C) 2016-2020 Andreas Shimokawa, Avamander, Carsten Pfeiffer,
    Daniele Gobbetti, Steffen Liebergeld

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

public class MusicStateSpec {
    public static final int STATE_UNKNOWN = -1;

    public static final int STATE_PLAYING = 0;
    public static final int STATE_PAUSED = 1;
    public static final int STATE_STOPPED = 2;

    public static final int STATE_SHUFFLE_ENABLED = 1;

    public byte state = STATE_UNKNOWN;
    /**
     * Position of the current media in seconds
     */
    public int position = STATE_UNKNOWN;
    /**
     * Speed of playback, usually 0 or 100 (full speed)
     */
    public int playRate = STATE_UNKNOWN;
    public byte shuffle = STATE_UNKNOWN;
    public byte repeat = STATE_UNKNOWN;

    public MusicStateSpec() {

    }

    public MusicStateSpec(MusicStateSpec old) {
        this.state = old.state;
        this.position = old.position;
        this.playRate = old.playRate;
        this.shuffle = old.shuffle;
        this.repeat = old.repeat;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof MusicStateSpec)) {
            return false;
        }
        MusicStateSpec stateSpec = (MusicStateSpec) obj;

        return this.state == stateSpec.state &&
                Math.abs(this.position - stateSpec.position)<=2 &&
                this.playRate == stateSpec.playRate &&
                this.shuffle == stateSpec.shuffle &&
                this.repeat == stateSpec.repeat;
    }

    @Override
    public int hashCode() {
        int result = (int) state;
//ignore the position -- it is taken into account in equals()
//result = 31 * result + position;
        result = 31 * result + playRate;
        result = 31 * result + (int) shuffle;
        result = 31 * result + (int) repeat;
        return result;
    }

    @Override
    public String toString() {
        return "MusicStateSpec{" +
                "state=" + state +
                ", position=" + position +
                ", playRate=" + playRate +
                ", shuffle=" + shuffle +
                ", repeat=" + repeat +
                '}';
    }
}
