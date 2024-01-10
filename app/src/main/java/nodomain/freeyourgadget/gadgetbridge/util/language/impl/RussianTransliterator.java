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

public class RussianTransliterator extends SimpleTransliterator {
    public RussianTransliterator() {
        super(new HashMap<Character, String>() {{
            put('а', "a"); put('б', "b"); put('в', "v");  put('г', "g"); put('д', "d"); put('е', "e"); put('ё', "jo"); put('ж', "zh");
            put('з', "z"); put('и', "i"); put('й', "jj"); put('к', "k"); put('л', "l"); put('м', "m"); put('н', "n");  put('о', "o");
            put('п', "p"); put('р', "r"); put('с', "s");  put('т', "t"); put('у', "u"); put('ф', "f"); put('х', "kh"); put('ц', "c");
            put('ч', "ch");put('ш', "sh");put('щ', "shh");put('ъ', "\"");put('ы', "y"); put('ь', "'"); put('э', "eh"); put('ю', "ju");
            put('я', "ja");

            put('õ', "o"); put('č', "c");
        }});
    }
}
