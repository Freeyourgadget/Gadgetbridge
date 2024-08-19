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

public abstract class MoyoungSetting<T> {
    public final String name;
    public final byte cmdQuery;
    public final byte cmdSet;

    public MoyoungSetting(String name, byte cmdQuery, byte cmdSet) {
        this.name = name;
        this.cmdQuery = cmdQuery;
        this.cmdSet = cmdSet;
    }

    public abstract byte[] encode(T value);
    public abstract T decode(byte[] data);
}
