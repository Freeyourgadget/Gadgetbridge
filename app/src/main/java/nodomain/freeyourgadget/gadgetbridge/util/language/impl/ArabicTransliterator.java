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
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.util.language.SimpleTransliterator;

public class ArabicTransliterator extends SimpleTransliterator {
    public ArabicTransliterator() {
        super(new HashMap<Character, String>() {{
            put('ا', "a"); put('ب', "b"); put('ت', "t"); put('ث', "th"); put('ج', "j"); put('ح', "7"); put('خ', "5");
            put('د', "d"); put('ذ', "th"); put('ر', "r"); put('ز', "z"); put('س', "s"); put('ش', "sh"); put('ص', "9");
            put('ض', "9'"); put('ط', "6"); put('ظ', "6'"); put('ع', "3"); put('غ', "3'"); put('ف', "f");
            put('ق', "q"); put('ك', "k"); put('ل', "l"); put('م', "m"); put('ن', "n"); put('ه', "h");
            put('و', "w"); put('ي', "y"); put('ى', "a"); put('ﺓ', "");
            put('آ', "2"); put('ئ', "2"); put('إ', "2"); put('ؤ', "2"); put('أ', "2"); put('ء', "2");
            put('٠', "0"); put('١', "1"); put('٢', "2"); put('٣', "3"); put('٤', "4"); put('٥', "5");
            put('٦', "6"); put('٧', "7"); put('٨', "8"); put('٩', "9");
        }});
    }
}
