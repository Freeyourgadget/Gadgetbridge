/*  Copyright (C) 2022-2024 José Rebelo, Petr Kadlec

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
package nodomain.freeyourgadget.gadgetbridge.util.language.impl;

import java.util.HashMap;

import nodomain.freeyourgadget.gadgetbridge.util.language.SimpleTransliterator;

public class CzechTransliterator extends SimpleTransliterator {
    public CzechTransliterator() {
        super(new HashMap<Character, String>() {{
            put('ř',"r"); put('ě',"e"); put('ý',"y"); put('á',"a"); put('í',"i"); put('é',"e");
            put('ó',"o"); put('ú',"u"); put('ů',"u"); put('ď',"d"); put('ť',"t"); put('ň',"n");
            put('„', "\""); put('“', "\""); put('‚', "'"); put('‘', "'");
        }});
    }
}
