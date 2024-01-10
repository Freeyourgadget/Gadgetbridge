/*  Copyright (C) 2022-2024 José Rebelo

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

public class HebrewTransliterator extends SimpleTransliterator {
    public HebrewTransliterator() {
        super(new HashMap<Character, String>() {{
            put('א', "a"); put('ב', "b"); put('ג', "g");  put('ד', "d"); put('ה', "h"); put('ו', "u"); put('ז', "z"); put('ח', "kh");
            put('ט', "t"); put('י', "y"); put('כ', "c"); put('ל', "l"); put('מ', "m"); put('נ', "n"); put('ס', "s");  put('ע', "'");
            put('פ', "p"); put('צ', "ts"); put('ק', "k");  put('ר', "r"); put('ש', "sh"); put('ת', "th"); put('ף', "f"); put('ץ', "ts");
            put('ך', "ch");put('ם', "m");put('ן', "n");
        }});
    }
}
