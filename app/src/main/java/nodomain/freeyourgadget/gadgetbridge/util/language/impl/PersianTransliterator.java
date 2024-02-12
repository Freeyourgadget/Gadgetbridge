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

public class PersianTransliterator extends SimpleTransliterator {
    public PersianTransliterator() {
        super(new HashMap<Character, String>() {{
            // Persian (Farsi)
            put('پ', "p"); put('چ', "ch"); put('ژ', "zh"); put('ک', "k"); put('گ', "g"); put('ی', "y"); put('‌', " ");
            put('؟', "?"); put('٪', "%"); put('؛', ";"); put('،', ","); put('۱', "1"); put('۲', "2"); put('۳', "3");
            put('۴', "4"); put('۵', "5"); put('۶', "6"); put('۷', "7"); put('۸', "8"); put('۹', "9"); put('۰', "0");
            put('»', "<"); put('«', ">"); put('ِ', "e"); put('َ', "a"); put('ُ', "o"); put('ّ', "");
        }});
    }
}
