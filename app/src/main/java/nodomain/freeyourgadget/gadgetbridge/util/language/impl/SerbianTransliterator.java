/*  Copyright (C) 2024 José Rebelo

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

public class SerbianTransliterator extends SimpleTransliterator {
    public SerbianTransliterator() {
        super(new HashMap<Character, String>() {{
            // As per https://en.wikipedia.org/wiki/Serbian_Cyrillic_alphabet#Modern_alphabet
            put('А', "A");   put('а', "a");
            put('Б', "B");   put('б', "b");
            put('В', "V");   put('в', "v");
            put('Г', "G");   put('г', "g");
            put('Д', "D");   put('д', "d");
            put('Ђ', "Dj");   put('ђ', "dj"); // from Đ / đ - from suggestion in #3727
            put('Е', "E");   put('е', "e");
            put('Ж', "Z");   put('ж', "z"); // from Ž / ž
            put('З', "Z");   put('з', "z");
            put('И', "I");   put('и', "i");
            put('Ј', "J");   put('ј', "j");
            put('К', "K");   put('к', "k");
            put('Л', "L");   put('л', "l");
            put('Љ', "Lj");  put('љ', "lj");
            put('М', "M");   put('м', "m");
            put('Н', "N");   put('н', "n");
            put('Њ', "Nj");  put('њ', "nj");
            put('О', "O");   put('о', "o");
            put('П', "P");   put('п', "p");
            put('Р', "R");   put('р', "r");
            put('С', "S");   put('с', "s");
            put('Т', "T");   put('т', "t");
            put('Ћ', "C");   put('ћ', "c"); // from Ć / ć
            put('У', "U");   put('у', "u");
            put('Ф', "F");   put('ф', "f");
            put('Х', "H");   put('х', "h");
            put('Ц', "C");   put('ц', "c");
            put('Ч', "C");   put('ч', "c"); // from Č / č
            put('Џ', "Dz");  put('џ', "dz"); // from Dž / dž
            put('Ш', "S");   put('ш', "s"); // From Š / š

            // Not in the table, pulled from Croatian
            put('Ć', "C");  put('ć', "c");
            put('Đ', "Dj");  put('đ', "dj");
            put('Š', "S");  put('š', "s");
            put('Ž', "z");  put('ž', "z");

            // Suggested in #3727
            put('Č', "C"); put('č', "c");
        }}, false);
    }
}
