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
package nodomain.freeyourgadget.gadgetbridge.devices.moyoung.settings;

public class MoyoungSettingByte extends MoyoungSetting<Byte> {
    public MoyoungSettingByte(String name, byte cmdQuery, byte cmdSet) {
        super(name, cmdQuery, cmdSet);
    }

    @Override
    public byte[] encode(Byte value) {
        return new byte[] { value };
    }

    @Override
    public Byte decode(byte[] data) {
        if (data.length != 1)
            throw new IllegalArgumentException("Wrong data length, should be 1, was " + data.length);
        return data[0];
    }
}
