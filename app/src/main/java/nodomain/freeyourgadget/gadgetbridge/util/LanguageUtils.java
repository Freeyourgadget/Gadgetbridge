/*  Copyright (C) 2017-2018 Andreas Shimokawa, Daniele Gobbetti, ivanovlev,
    lazarosfs, McSym28, Ted Stein, Yaron Shahrabani

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

import android.text.TextUtils;
import android.util.Pair;

import org.apache.commons.lang3.text.WordUtils;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;

public class LanguageUtils {
    //transliteration map with english equivalent for unsupported chars
    private static Map<Character, String> transliterateMap = new HashMap<Character, String>(){
        {
            //extended ASCII characters
            put('æ', "ae"); put('œ', "oe"); put('ª', "a"); put('º', "o"); put('«',"\""); put('»',"\"");

            //german characters
            put('ä',"ae"); put('ö',"oe"); put('ü',"ue");
            put('Ä',"Ae"); put('Ö',"Oe"); put('Ü',"Üe");
            put('ß',"ss"); put('ẞ',"SS");

            //russian chars
            put('а', "a"); put('б', "b"); put('в', "v");  put('г', "g"); put('д', "d"); put('е', "e"); put('ё', "jo"); put('ж', "zh");
            put('з', "z"); put('и', "i"); put('й', "jj"); put('к', "k"); put('л', "l"); put('м', "m"); put('н', "n");  put('о', "o");
            put('п', "p"); put('р', "r"); put('с', "s");  put('т', "t"); put('у', "u"); put('ф', "f"); put('х', "kh"); put('ц', "c");
            put('ч', "ch");put('ш', "sh");put('щ', "shh");put('ъ', "\"");put('ы', "y"); put('ь', "'"); put('э', "eh"); put('ю', "ju");
            put('я', "ja");
            
            //hebrew chars
            put('א', "a"); put('ב', "b"); put('ג', "g");  put('ד', "d"); put('ה', "h"); put('ו', "u"); put('ז', "z"); put('ח', "kh");
            put('ט', "t"); put('י', "y"); put('כ', "c"); put('ל', "l"); put('מ', "m"); put('נ', "n"); put('ס', "s");  put('ע', "'");
            put('פ', "p"); put('צ', "ts"); put('ק', "k");  put('ר', "r"); put('ש', "sh"); put('ת', "th"); put('ף', "f"); put('ץ', "ts");
            put('ך', "ch");put('ם', "m");put('ן', "n");

            // greek chars
            put('α',"a");put('ά',"a");put('β',"v");put('γ',"g");put('δ',"d");put('ε',"e");put('έ',"e");put('ζ',"z");put('η',"i");
            put('ή',"i");put('θ',"th");put('ι',"i");put('ί',"i");put('ϊ',"i");put('ΐ',"i");put('κ',"k");put('λ',"l");put('μ',"m");
            put('ν',"n");put('ξ',"ks");put('ο',"o");put('ό',"o");put('π',"p");put('ρ',"r");put('σ',"s");put('ς',"s");put('τ',"t");
            put('υ',"y");put('ύ',"y");put('ϋ',"y");put('ΰ',"y");put('φ',"f");put('χ',"ch");put('ψ',"ps");put('ω',"o");put('ώ',"o");
            put('Α',"A");put('Ά',"A");put('Β',"B");put('Γ',"G");put('Δ',"D");put('Ε',"E");put('Έ',"E");put('Ζ',"Z");put('Η',"I");
            put('Ή',"I");put('Θ',"TH");put('Ι',"I");put('Ί',"I");put('Ϊ',"I");put('Κ',"K");put('Λ',"L");put('Μ',"M");put('Ν',"N");
            put('Ξ',"KS");put('Ο',"O");put('Ό',"O");put('Π',"P");put('Ρ',"R");put('Σ',"S");put('Τ',"T");put('Υ',"Y");put('Ύ',"Y");
            put('Ϋ',"Y");put('Φ',"F");put('Χ',"CH");put('Ψ',"PS");put('Ω',"O");put('Ώ',"O");

            //ukrainian characters
            put('ґ', "gh"); put('є', "je"); put('і', "i"); put('ї', "ji"); put('Ґ', "GH"); put('Є', "JE"); put('І', "I"); put('Ї', "JI");

            // Arabic
            put('ا', "a"); put('ب', "b"); put('ت', "t"); put('ث', "th"); put('ج', "j"); put('ح', "7"); put('خ', "5");
            put('د', "d"); put('ذ', "th"); put('ر', "r"); put('ز', "z"); put('س', "s"); put('ش', "sh"); put('ص', "9");
            put('ض', "9'"); put('ط', "6"); put('ظ', "6'"); put('ع', "3"); put('غ', "3'"); put('ف', "f");
            put('ق', "q"); put('ك', "k"); put('ل', "l"); put('م', "m"); put('ن', "n"); put('ه', "h");
            put('و', "w"); put('ي', "y"); put('ى', "a"); put('ﺓ', "");
            put('آ', "2"); put('ئ', "2"); put('إ', "2"); put('ؤ', "2"); put('أ', "2"); put('ء', "2");

            // Farsi
            put('پ', "p"); put('چ', "ch"); put('ڜ', "ch"); put('ڤ', "v"); put('ڥ', "v");
            put('ڨ', "g"); put('گ', "g"); put('ݣ', "g");

            // Polish
            put('Ł', "L"); put('ł', "l");

            //Lithuanian
            put('ą', "a"); put('č', "c"); put('ę', "e"); put('ė', "e"); put('į', "i"); put('š', "s"); put('ų', "u"); put('ū', "u"); put('ž', "z");

            //TODO: these must be configurable. If someone wants to transliterate cyrillic it does not mean his device has no German umlauts
            //all or nothing is really bad here
        }
    };

    /**
     * Checks the status of transliteration option
     * @return true if transliterate option is On, and false, if Off or not exist
     */
    public static boolean transliterate()
    {
        return GBApplication.getPrefs().getBoolean("transliteration", false);
    }

    /**
     * Replaces unsupported symbols to english
     * @param txt input text
     * @return transliterated text
     */
    public static String transliterate(String txt){
        if (txt == null || txt.isEmpty()) {
            return txt;
        }

        StringBuilder message = new StringBuilder();

        char[] chars = txt.toCharArray();

        for (char c : chars)
        {
            message.append(transliterate(c));
        }

        String messageString = BengaliLanguageUtils.transliterate(message.toString());

        return flattenToAscii(messageString);
    }

    /**
     * Replaces unsupported symbol to english by {@code transliterateMap}
     * @param c input char
     * @return replacement text
     */
    private static String transliterate(char c){
        char lowerChar = Character.toLowerCase(c);

        if (transliterateMap.containsKey(lowerChar)) {
            String replace = transliterateMap.get(lowerChar);

            if (lowerChar != c)
            {
                return WordUtils.capitalize(replace);
            }

            return replace;
        }

        return String.valueOf(c);
    }

    /**
     * Converts the diacritics
     * @param string input text
     * @return converted text
     */
    private static String flattenToAscii(String string) {
        string = Normalizer.normalize(string, Normalizer.Form.NFD);
        return string.replaceAll("\\p{M}", "");
    }

    /**
     * Checks the status of right-to-left option
     * @return true if right-to-left option is On, and false, if Off or not exist
     */
    public static boolean rtlSupport()
    {
        return GBApplication.getPrefs().getBoolean("rtl", false);
    }

    //map with brackets chars to change there direction
    private static Map<Character, Character> directionSignsMap = new HashMap<Character, Character>(){
        {
            put('(', ')'); put(')', '('); put('[', ']'); put(']', '['); put('{','}'); put('}','{');


        }
    };

    //list of unicode ranges of rtl chars
    private static ArrayList <Pair<Character, Character>> rtlRange = new ArrayList<Pair<Character, Character>>() {
        {
            add(new Pair<Character, Character>('\u0590', '\u05F4'));
            add(new Pair<Character, Character>('\uFB1D', '\uFB4F'));
            add(new Pair<Character, Character>('\u0600', '\u06FF'));
            add(new Pair<Character, Character>('\u0750', '\u077F'));
            add(new Pair<Character, Character>('\u08A0', '\u08FF'));
            add(new Pair<Character, Character>('\uFB50', '\uFDFF'));
            add(new Pair<Character, Character>('\uFE70', '\uFEFF'));
        }
    };

    /**
     * @return true if the char is in the rtl range, otherwise false
     */
    private static Boolean isRtl(char c){
        for (Pair<Character, Character> rang: rtlRange) {
            if (rang.first <= c && c <= rang.second) {
                return true;
            }
        }
        return false;
    }

    //list of unicode ranges of punctuations chars
    private static ArrayList <Pair<Character, Character>> punctuationsRange = new ArrayList<Pair<Character, Character>>() {
        {
            add(new Pair<Character, Character>('\u0021', '\u002F'));
            add(new Pair<Character, Character>('\u003A', '\u0040'));
            add(new Pair<Character, Character>('\u005B', '\u0060'));
            add(new Pair<Character, Character>('\u007B', '\u007E'));
        }
    };

    /**
     * @return true if the char is in the punctuations range, otherwise false
     */
    private static Boolean isPunctuations(char c){
        for (Pair<Character, Character> rang: punctuationsRange) {
            if (rang.first <= c && c <= rang.second) {
                return true;
            }
        }
        return false;
    }

    //list of sign that ends a word
    private static ArrayList<Character> wordEndSigns = new ArrayList<Character>() {
        {
            add('\0');
            add('\n');
            add(' ');
        }
    };

    /**
     * @return true if the char is in the end of word list, otherwise false
     */
    private static Boolean isWordEndSign(char c){
        for (char sign: wordEndSigns){
            if (c == sign){
                return true;
            }
        }

        return false;
    }

    /**
     * The function get a string and reverse it.
     * in case of end-of-word sign, it will leave it at the end.
     * in case of sign with direction like brackets, it will change the direction.
     * @param s - the string to reverse
     * @return reversed string
     */
    private static String reverse(String s) {
        int j = s.length();
        int startWithSpace = 0;
        char[] newWord = new char[j];

        if (j == 0) {
            return s;
        }

        // remain end-of-word sign at the end
        if (isWordEndSign(s.charAt(s.length() - 1))){
            startWithSpace = 1;
            newWord[--j] = s.charAt(s.length() - 1);
        }

        for (int i = 0; i < s.length() - startWithSpace; i++) {
            if (LanguageUtils.directionSignsMap.containsKey(s.charAt(i))) {
                newWord[--j] = LanguageUtils.directionSignsMap.get(s.charAt(i));
            } else {
                newWord[--j] = s.charAt(i);
            }
        }
        return new String(newWord);
    }

    /**
     * The function get a string and fix the rtl words.
     * since simple reverse puts the beginning of the text at the end, the text should have been from bottom to top.
     * To avoid that, we save the text in lines (line max size can be change in the settings)
     * @param s - the string to fix.
     * @return a fix string.
     */
    public static String fixRtl(String s) {
        if (s == null || s.isEmpty()){
            return s;
        }
        int length = s.length();
        String oldString = s.substring(0, length);
        String newString = "";
        List<String> lines = new ArrayList<>();
        char[] newWord = new char[length];
        int line_max_size = GBApplication.getPrefs().getInt("rtl_max_line_length", 20);;

        int startPos = 0;
        int endPos = 0;
        Boolean isRtlState = LanguageUtils.isRtl(oldString.charAt(0));
        char c;
        String line = "";
        for (int i = 0; i < length; i++) {
            c = oldString.charAt(i);
            if ((LanguageUtils.isRtl(c) == isRtlState || LanguageUtils.isPunctuations(c)) && i < length - 1) {
                endPos++;
            } else {
                String word;

                if (isWordEndSign(c)){
                    endPos++;
                }

                if (i == length - 1){
                    endPos = length;
                }
                if (isRtlState) {
                    word = reverse(oldString.substring(startPos, endPos));
                } else {
                    word = (oldString.substring(startPos, endPos));
                }
                if (line.length() + word.length() > line_max_size) {
                    lines.add(line + "\n");
                    line = "";
                }
                line = String.format("%s%s", word, line);
                if (line.endsWith("\0") || line.endsWith("\n")) {
                    lines.add(line);
                    line = "";
                }
                startPos = endPos;
                if (!isWordEndSign(c)){
                    endPos++;
                    isRtlState = !isRtlState;
                }
            }
        }

        lines.add(line);

        newString = TextUtils.join("", lines);

        return newString;
    }
}
