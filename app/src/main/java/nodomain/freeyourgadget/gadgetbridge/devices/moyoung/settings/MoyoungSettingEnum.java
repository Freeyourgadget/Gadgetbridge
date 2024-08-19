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

public class MoyoungSettingEnum<T extends Enum <?> & MoyoungEnum> extends MoyoungSetting<T> {
    protected final Class<T> clazz;

    public MoyoungSettingEnum(String name, byte cmdQuery, byte cmdSet, Class<T> clazz) {
        super(name, cmdQuery, cmdSet);
        this.clazz = clazz;
    }

    public T findByValue(byte value)
    {
        for (T e : clazz.getEnumConstants()) {
            if (e.value() == value) {
                return e;
            }
        }

        throw new IllegalArgumentException("No enum value for " + value);
    }

    @Override
    public byte[] encode(T value) {
        return new byte[] { value.value() };
    }

    @Override
    public T decode(byte[] data) {
        if (data.length != 1)
            throw new IllegalArgumentException("Wrong data length, should be 1, was " + data.length);

        return findByValue(data[0]);
    }

    public T[] decodeSupportedValues(byte[] data) {
        return clazz.getEnumConstants();
    }
}
