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

import android.util.Pair;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class DaFitSettingLanguage extends DaFitSettingEnum<DaFitEnumLanguage> {
    public DaFitSettingLanguage(String name, byte cmdQuery, byte cmdSet) {
        super(name, cmdQuery, cmdSet, DaFitEnumLanguage.class);
    }

    private Pair<DaFitEnumLanguage, DaFitEnumLanguage[]> decodeData(byte[] data) {
        if (data.length != 5)
            throw new IllegalArgumentException("Wrong data length, should be 5, was " + data.length);

        byte[] current = new byte[] { data[0] };
        byte[] supported = new byte[] { data[1], data[2], data[3], data[4] };

        ByteBuffer buffer = ByteBuffer.wrap(supported);
        int supportedNum = buffer.getInt();
        String supportedStr = new StringBuffer(Integer.toBinaryString(supportedNum)).reverse().toString();

        DaFitEnumLanguage currentLanguage = super.decode(current);
        List<DaFitEnumLanguage> supportedLanguages = new ArrayList<>();
        for (DaFitEnumLanguage e : clazz.getEnumConstants()) {
            if (e.value() >= supportedStr.length())
                continue;
            if (Integer.parseInt(supportedStr.substring(e.value(), e.value() + 1)) != 0)
                supportedLanguages.add(e);
        }

        DaFitEnumLanguage[] supportedLanguagesArr = new DaFitEnumLanguage[supportedLanguages.size()];
        return Pair.create(currentLanguage, supportedLanguages.toArray(supportedLanguagesArr));
    }

    @Override
    public DaFitEnumLanguage decode(byte[] data) {
        return decodeData(data).first;
    }

    @Override
    public DaFitEnumLanguage[] decodeSupportedValues(byte[] data) {
        return decodeData(data).second;
    }
}
