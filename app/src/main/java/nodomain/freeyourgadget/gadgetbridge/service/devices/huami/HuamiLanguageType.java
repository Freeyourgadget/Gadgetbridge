/*  Copyright (C) 2020-2021 Andreas Shimokawa, TinfoilSubmarine

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami;

import java.util.HashMap;
import java.util.Map;

public class HuamiLanguageType {
    public static final Map<String, Integer> idLookup = new HashMap<String, Integer>() {{
        put("zh_CN", 0x00);
        put("zh_TW", 0x01);
        put("zh_HK", 0x01);
        put("en_US", 0x02);
        put("en_GB", 0x02);
        put("en_AU", 0x02);
        put("es_ES", 0x03);
        put("ru_RU", 0x04);
        put("ko_KO", 0x05);
        put("fr_FR", 0x06);
        put("de_DE", 0x07);
        put("de_AT", 0x07);
        put("de_CH", 0x07);
        put("pl_PL", 0x09);
        put("it_IT", 0x0a);
        put("pt_PT", 0x0f);
        put("tr_TR", 0x11);
        put("pt_BR", 0x14);
        put("cs_CZ", 0x16);
    }};
}
