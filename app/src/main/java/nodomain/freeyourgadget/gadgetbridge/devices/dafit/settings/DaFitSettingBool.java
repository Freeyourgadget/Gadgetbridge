/*  Copyright (C) 2019 krzys_h

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
package nodomain.freeyourgadget.gadgetbridge.devices.dafit.settings;

public class DaFitSettingBool extends DaFitSetting<Boolean> {
    public DaFitSettingBool(String name, byte cmdQuery, byte cmdSet) {
        super(name, cmdQuery, cmdSet);
    }

    @Override
    public byte[] encode(Boolean value) {
        return new byte[] { value ? (byte)1 : (byte)0 };
    }

    @Override
    public Boolean decode(byte[] data) {
        if (data.length != 1)
            throw new IllegalArgumentException("Wrong data length, should be 1, was " + data.length);
        if (data[0] != 0 && data[0] != 1)
            throw new IllegalArgumentException("Expected a boolean, got " + data[0]);
        return data[0] != 0;
    }
}
