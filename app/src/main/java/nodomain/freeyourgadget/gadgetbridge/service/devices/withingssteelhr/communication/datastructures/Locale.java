/*  Copyright (C) 2023-2024 Frank Ertl

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.datastructures;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class Locale extends WithingsStructure {

    private String locale = "en";

    public Locale(String locale) {
        this.locale = locale;
    }

    @Override
    public short getLength() {
        return (short) ((locale != null ? locale.getBytes().length : 0) + 1 + HEADER_SIZE);
    }

    @Override
    protected void fillinTypeSpecificData(ByteBuffer rawDataBuffer) {
        addStringAsBytesWithLengthByte(rawDataBuffer, locale);
    }

    @Override
    public short getType() {
        return WithingsStructureType.LOCALE;
    }
}
