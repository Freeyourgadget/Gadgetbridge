package nodomain.freeyourgadget.gadgetbridge.util;

import java.util.HashMap;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;

public class LanguageUtils {
    //transliteration map with english equivalent for unsupported chars
    private static Map<Character, String> transliterateMap = new HashMap<Character, String>(){
        {
            //russian chars
            put('а', "a"); put('б', "b"); put('в', "v");  put('г', "g"); put('д', "d"); put('е', "e"); put('ё', "jo"); put('ж', "zh");
            put('з', "z"); put('и', "i"); put('й', "jj"); put('к', "k"); put('л', "l"); put('м', "m"); put('н', "n");  put('о', "o");
            put('п', "p"); put('р', "r"); put('с', "s");  put('т', "t"); put('у', "u"); put('ф', "f"); put('х', "kh"); put('ц', "c");
            put('ч', "ch");put('ш', "sh");put('щ', "shh");put('ъ', "\"");put('ы', "y"); put('ь', "'"); put('э', "eh"); put('ю', "ju");
            put('я', "ja");

            //continue for other languages...
        }
    };

    //check transliterate option status
    public static boolean transliterate()
    {
        return GBApplication.getPrefs().getBoolean("transliteration", true);
    }

    //replace unsupported symbols to english analog
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

        return message.toString();
    }

    //replace unsupported symbol to english analog text
    private static String transliterate(char c){
        char lowerChar = Character.toLowerCase(c);

        if (transliterateMap.containsKey(lowerChar)) {
            String replace = transliterateMap.get(lowerChar);

            if (lowerChar != c)
            {
                return replace.toUpperCase();
            }

            return replace;
        }

        return String.valueOf(c);
    }
}
