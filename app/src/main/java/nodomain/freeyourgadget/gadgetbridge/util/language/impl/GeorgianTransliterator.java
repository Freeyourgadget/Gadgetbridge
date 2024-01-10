/*  Copyright (C) 2023-2024 Petr Vaněk, roolx

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
    
    public class GeorgianTransliterator extends SimpleTransliterator {
        public GeorgianTransliterator() {
            super(new HashMap<Character, String>() {{
                put('ა', "a"); put('ბ', "b"); put('გ', "g"); 
                put('დ', "d"); put('ე', "e"); put('ვ', "v"); 
                put('ზ', "z"); put('თ', "T"); put('ი', "i");
                put('კ', "k"); put('ლ', "l"); put('მ', "m"); 
                put('ნ', "n"); put('ო', "o"); put('პ', "p"); 
                put('ჟ', "J"); put('რ', "r"); put('ს', "s"); 
                put('ტ', "t"); put('უ', "u"); put('ფ', "f"); 
                put('ქ', "q"); put('ღ', "R"); put('ყ', "y"); 
                put('შ', "S"); put('ჩ', "C"); put('ც', "c"); 
                put('ძ', "Z"); put('წ', "w"); put('ჭ', "W"); 
                put('ხ', "x"); put('ჯ', "j"); put('ჰ', "h"); 
            }});
        }
    }
    