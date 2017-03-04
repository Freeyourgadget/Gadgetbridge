package nodomain.freeyourgadget.gadgetbridge.util;

import org.apache.commons.lang3.text.WordUtils;

import java.util.HashMap;
import java.util.Map;
import java.text.Normalizer;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;

public class LanguageUtils {
    //transliteration map with english equivalent for unsupported chars
    private static Map<Character, String> transliterateMap = new HashMap<Character, String>(){
        {
            //extended ASCII characters
            put('æ', "ae"); put('œ', "oe"); put('ß', "B"); put('ª', "a"); put('º', "o"); put('«',"\""); put('»',"\"");

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
            
            //continue for other languages...
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

        return flattenToAscii(message.toString());
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
}
