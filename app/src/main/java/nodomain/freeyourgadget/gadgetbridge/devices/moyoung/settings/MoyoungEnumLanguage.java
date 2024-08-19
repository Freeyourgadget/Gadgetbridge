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

public enum MoyoungEnumLanguage implements MoyoungEnum {
    LANGUAGE_ENGLISH((byte)0),
    LANGUAGE_CHINESE((byte)1),
    LANGUAGE_JAPANESE((byte)2),
    LANGUAGE_KOREAN((byte)3),
    LANGUAGE_GERMAN((byte)4),
    LANGUAGE_FRENCH((byte)5),
    LANGUAGE_SPANISH((byte)6),
    LANGUAGE_ARABIC((byte)7),
    LANGUAGE_RUSSIAN((byte)8),
    LANGUAGE_TRADITIONAL((byte)9),
    LANGUAGE_UKRAINIAN((byte)10),
    LANGUAGE_ITALIAN((byte)11),
    LANGUAGE_PORTUGUESE((byte)12),
    LANGUAGE_DUTCH((byte)13),
    LANGUAGE_POLISH((byte)14),
    LANGUAGE_SWEDISH((byte)15),
    LANGUAGE_FINNISH((byte)16),
    LANGUAGE_DANISH((byte)17),
    LANGUAGE_NORWEGIAN((byte)18),
    LANGUAGE_HUNGARIAN((byte)19),
    LANGUAGE_CZECH((byte)20),
    LANGUAGE_BULGARIAN((byte)21),
    LANGUAGE_ROMANIAN((byte)22),
    LANGUAGE_SLOVAK_LANGUAGE((byte)23),
    LANGUAGE_LATVIAN((byte)24);

    public final byte value;

    MoyoungEnumLanguage(byte value) {
        this.value = value;
    }

    @Override
    public byte value() {
        return value;
    }
}
