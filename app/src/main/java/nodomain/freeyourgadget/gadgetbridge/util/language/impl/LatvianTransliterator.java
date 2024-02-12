/*  Copyright (C) 2023-2024 Davis Mosenkovs

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

public class LatvianTransliterator extends SimpleTransliterator {
    public LatvianTransliterator() {
        super(new HashMap<Character, String>() {{
            put('ā', "a"); put('č', "c"); put('ē', "e"); put('ģ', "g"); put('ī', "i"); put('ķ', "k"); put('ļ', "l"); put('ņ', "n"); put('š', "s"); put('ū', "u"); put('ž', "z");
            put('Ā', "A"); put('Č', "C"); put('Ē', "E"); put('Ģ', "G"); put('Ī', "I"); put('Ķ', "K"); put('Ļ', "L"); put('Ņ', "N"); put('Š', "S"); put('Ū', "U"); put('Ž', "Z");
        }});
    }
}
