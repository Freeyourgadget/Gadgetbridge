/*  Copyright (C) 2017-2018 Aniruddha Adhikary

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
package nodomain.freeyourgadget.gadgetbridge.util;

import java.util.HashMap;

public class BengaliLanguageUtils extends LanguageUtils {

    private final static char BENGALI_JOIN_CHAR = '্';

    private final static HashMap<Character, String> numbers = new HashMap<Character, String>() {
        {
            put('০',"0"); put('১',"1"); put('২',"2"); put('৩',"3"); put('৪',"4");
            put('৫',"5"); put('৬',"6"); put('৭',"7"); put('৮',"8");
            put('৯',"9");
        }
    };

    private final static HashMap<Character, String> vowels = new HashMap<Character, String>() {
        {
            put('অ', "o"); put('আ', "a"); put('ই', "i"); put('ঈ', "ee");
            put('উ', "u"); put('ঊ', "oo"); put('ঋ', "ri"); put('এ', "e");
            put('ঐ', "oi"); put('ও', "o"); put('ঔ', "ou"); put('া', "a");
            put('ি', "i"); put('ী', "ee"); put('ু', "u"); put('ূ', "oo");
            put('ৃ', "ri"); put('ে', "e"); put('ৈ', "oi"); put('ো', "o");
            put('ৌ', "ou");
        }
    };

    private final static HashMap<Character, String> consonants = new HashMap<Character, String>() {
        {
            put('ঁ', ""); put('ং', "ng"); put('ঃ', "");
            put('ক', "k"); put('খ', "kh"); put('গ', "g"); put('ঘ', "gh"); put('ঙ', "ng");
            put('চ', "ch"); put('ছ', "ch"); put('জ', "j"); put('ঝ', "jh"); put('ঞ', "ng");
            put('ট', "t"); put('ঠ', "th"); put('ড', "d"); put('ঢ', "dh"); put('ণ', "n");
            put('ত', "t"); put('থ', "th"); put('দ', "d"); put('ধ', "dh"); put('ন', "n");
            put('প', "p"); put('ফ', "f"); put('ব', "b"); put('ভ', "v"); put('ম', "m");
            put('য', "z"); put('র', "r"); put('ল', "l"); put('শ', "sh");
            put('ষ', "sh"); put('স', "s"); put('হ', "h");
            put('ৎ', "t"); put('ড়', "r"); put('ঢ়', "r"); put('য়', "y");
        }
    };

    private final static HashMap<Character, String> symbols = new HashMap<Character, String>() {
        {
            put('ব', "w");
            put('য়', "y");
        }
    };

    private final static HashMap<Character, String> joins = new HashMap<Character, String>() {
        {
            put('৳', "$");
        }
    };

    private static boolean hasJoinedInString(String string) {
        return string.contains(string);
    }

    public static String transliterate(String txt) {
        if (txt.isEmpty()) {
            return txt;
        }

        char[] charArray = txt.toCharArray();

        StringBuilder romanizedBuilder = new StringBuilder();
        char last = '\0';

        for(int i = 0; i < txt.length(); i++) {
            char currentChar = charArray[i];

            if (symbols.containsKey(currentChar)) {
                romanizedBuilder.append(symbols.get(currentChar));
            }
            else if (numbers.containsKey(currentChar)) {
                romanizedBuilder.append(numbers.get(currentChar));
            }
            else if (vowels.containsKey(currentChar)) {
                romanizedBuilder.append(vowels.get(currentChar));
            }
            else if (consonants.containsKey(currentChar)) {
                if (last != '\0' && consonants.containsKey(last)) {
                    romanizedBuilder.append('o');
                }
                romanizedBuilder.append(consonants.get(currentChar));
            } else if (currentChar == BENGALI_JOIN_CHAR) {
                if (i + 1 < txt.length() && joins.containsKey(charArray[i + 1])) {
                    romanizedBuilder.append(joins.get(charArray[i + 1]));
                    i++;
                    continue;
                }
            } else {
                romanizedBuilder.append(currentChar);
            }

            last = currentChar;
        }

        String romanized = romanizedBuilder.toString();

        if (vowels.containsKey(charArray[charArray.length - 1])
                && hasJoinedInString(txt)
                && romanized.toCharArray()[romanized.length() - 1] == 'y') {
            romanizedBuilder.append('o');
        }

        return romanizedBuilder.toString();
    }

}