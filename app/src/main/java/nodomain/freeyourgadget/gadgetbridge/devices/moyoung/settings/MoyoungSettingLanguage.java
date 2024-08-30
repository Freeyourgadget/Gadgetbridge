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

import android.util.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.service.devices.moyoung.QuerySettingsOperation;

public class MoyoungSettingLanguage extends MoyoungSettingEnum<MoyoungEnumLanguage> {
    private static final Logger LOG = LoggerFactory.getLogger(MoyoungSettingLanguage.class);

    public MoyoungSettingLanguage(String name, byte cmdQuery, byte cmdSet) {
        super(name, cmdQuery, cmdSet, MoyoungEnumLanguage.class);
    }

    private Pair<MoyoungEnumLanguage, MoyoungEnumLanguage[]> decodeData(byte[] data) {
        if (data.length < 5)
            throw new IllegalArgumentException("Wrong data length, should be at least 5, was " + data.length);

        byte[] current = new byte[] { data[0] };
        byte[] supported = new byte[] { data[1], data[2], data[3], data[4] };

        ByteBuffer buffer = ByteBuffer.wrap(supported);
        int supportedNum = buffer.getInt();
        String supportedStr = new StringBuffer(Integer.toBinaryString(supportedNum)).reverse().toString();

        MoyoungEnumLanguage currentLanguage = super.decode(current);
        List<MoyoungEnumLanguage> supportedLanguages = new ArrayList<>();
        for (MoyoungEnumLanguage e : clazz.getEnumConstants()) {
            if (e.value() >= supportedStr.length())
                continue;
            if (Integer.parseInt(supportedStr.substring(e.value(), e.value() + 1)) != 0)
                supportedLanguages.add(e);
        }

        MoyoungEnumLanguage[] supportedLanguagesArr = new MoyoungEnumLanguage[supportedLanguages.size()];
        LOG.debug("Supported languages: {}", supportedLanguages);
        return Pair.create(currentLanguage, supportedLanguages.toArray(supportedLanguagesArr));
    }

    @Override
    public MoyoungEnumLanguage decode(byte[] data) {
        return decodeData(data).first;
    }

    @Override
    public MoyoungEnumLanguage[] decodeSupportedValues(byte[] data) {
        return decodeData(data).second;
    }
}
