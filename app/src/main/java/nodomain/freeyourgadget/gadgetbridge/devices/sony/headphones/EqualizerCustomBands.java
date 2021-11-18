/*  Copyright (C) 2021 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones;

import java.util.List;

public class EqualizerCustomBands {
    private List<Integer> bands;
    private int clearBass;

    public EqualizerCustomBands(List<Integer> bands, int clearBass) {
        if (bands.size() != 5) {
            throw new IllegalArgumentException("Equalizer needs exactly 5 bands");
        }

        for (Integer band : bands) {
            if (band < -10 || band > 10) {
                throw new IllegalArgumentException(String.format("Bands should be between -10 and 10, got %d", band));
            }
        }

        if (clearBass < -10 || clearBass > 10) {
            throw new IllegalArgumentException(String.format("Clear Bass value shoulud be between -10 and 10, got %d", clearBass));
        }

        this.bands = bands;
        this.clearBass = clearBass;
    }

    public List<Integer> getBands() {
        return bands;
    }

    public int getClearBass() {
        return clearBass;
    }
}
