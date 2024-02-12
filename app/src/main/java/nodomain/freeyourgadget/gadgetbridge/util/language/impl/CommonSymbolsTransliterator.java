/*  Copyright (C) 2023-2024 Daniel Dakhno, Davis Mosenkovs

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

public class CommonSymbolsTransliterator extends SimpleTransliterator {
    public CommonSymbolsTransliterator() {
        super(new HashMap<Character, String>() {{
            put('“', "\""); put('”', "\""); put('‟', "\""); put('„', "\""); put('‘', "'"); put('’', "'"); put('‛', "'"); put('‚', "'"); put('«', "<"); put('»', ">"); put('‹', "<"); put('›', ">");
            put('©', "(c)"); put('®', "(r)"); put('™', "(tm)"); put('°', "*"); put('€', "EUR");
            put('–', "-"); put('⸺', "-"); put('˗', "-"); put('ᐨ', "-"); put('‐', "-"); put('‑', "-"); put('‒', "-"); put('—', "-"); put('―', "-"); put('−', "-");
            put('⎯', "-"); put('⏤', "-"); put('─', "-"); put('➖', "-"); put('⸻', "-"); put('ㅡ', "-"); put('ᅳ', "-"); put('ー', "-"); put('一', "-"); put('﹘', "-");
            put('﹣', "-"); put('－', "-"); put('\udc4b', "-"); put('\udc52', "-"); put('˜', "~"); put('⁓', "~"); put('∼', "~"); put('〜', "~"); put('〰', "~~"); put('～', "~");
            put('⁰', "0"); put('¹', "1"); put('²', "2"); put('³', "3"); put('⁴', "4"); put('⁵', "5"); put('⁶', "6"); put('⁷', "7"); put('⁸', "8"); put('⁹', "9");
            put('₀', "0"); put('₁', "1"); put('₂', "2"); put('₃', "3"); put('₄', "4"); put('₅', "5"); put('₆', "6"); put('₇', "7"); put('₈', "8"); put('₉', "9");
        }});
    }
}
