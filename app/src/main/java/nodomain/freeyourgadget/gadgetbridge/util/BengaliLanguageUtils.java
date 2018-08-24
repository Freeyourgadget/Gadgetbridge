package nodomain.freeyourgadget.gadgetbridge.util;

import java.util.Hashtable;
import java.util.regex.*;

// What's the reason to extending LanguageUtils?
// Just doing it because already done in the previous code.
public class BengaliLanguageUtils extends LanguageUtils {
    // Composite Letters.
    private final static Hashtable<String, String> composites = new Hashtable<String, String>() {
        {
            put("ক্ষ", "kkh");
            put("ঞ্চ", "NC");
            put("ঞ্ছ", "NCh");
            put("ঞ্জ", "Ng");
            put("জ্ঞ", "gg");
            put("ঞ্ঝ", "Ngh");
            put("্র", "r");
            put("্ল", "l");
            put("ষ্ম", "SSh");
            put("র্", "r");
            put("্য", "y");
            put("্ব", "w");
        }
    };
    // Single Character Letters.
    private final static Hashtable<String, String> letters = new Hashtable<String, String>() {
        {
            put("আ", "aa");
            put("অ", "a");
            put("ই", "i");
            put("ঈ", "ii");
            put("উ", "u");
            put("ঊ", "uu");
            put("ঋ", "ri");
            put("এ", "e");
            put("ঐ", "oi");
            put("ও", "o");
            put("ঔ", "ou");
            put("ক", "k");
            put("খ", "kh");
            put("গ", "g");
            put("ঘ", "gh");
            put("ঙ", "ng");
            put("চ", "ch");
            put("ছ", "chh");
            put("জ", "j");
            put("ঝ", "jh");
            put("ঞ", "Ng");
            put("ট", "T");
            put("ঠ", "Th");
            put("ড", "D");
            put("ঢ", "Dh");
            put("ণ", "N");
            put("ত", "t");
            put("থ", "th");
            put("দ", "d");
            put("ধ", "dh");
            put("ন", "n");
            put("প", "p");
            put("ফ", "ph");
            put("ব", "b");
            put("ভ", "bh");
            put("ম", "m");
            put("য", "J");
            put("র", "r");
            put("ল", "l");
            put("শ", "sh");
            put("ষ", "Sh");
            put("স", "s");
            put("হ", "h");
            put("ড়", "rh");
            put("ঢ়", "rH");
            put("য়", "y");
            put("ৎ", "t");
            put("০", "0");
            put("১", "1");
            put("২", "2");
            put("৩", "3");
            put("৪", "4");
            put("৫", "5");
            put("৬", "6");
            put("৭", "7");
            put("৮", "8");
            put("৯", "9");
            put("া", "aa");
            put("ি", "i");
            put("ী", "ii");
            put("ু", "u");
            put("ূ", "uu");
            put("ৃ", "r");
            put("ে", "e");
            put("ো", "o");
            put("ৈ", "oi");
            put("ৗ", "ou");
            put("ৌ", "ou");
            put("ং", "ng");
            put("ঃ", "h");
            put("ঁ", "nN");
            put("।", ".");
        }
    };

    // The regex to extract Bengali characters in nested groups.
    private final static String pattern = "(র্){0,1}([অ-হড়-য়](্([অ-মশ-হড়-য়]))*)((‍){0,1}(্([য-ল]))){0,1}([া-ৌ]){0,1}|[্ঁঃংৎ০-৯।]| ";

    public static String transliterate(String txt) {
        if (txt.isEmpty()) {
            return txt;
        }

        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(txt);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String appendableString = "";
            String reff = m.group(1);
            if (reff != null) {
                appendableString = appendableString + "rr";
            }
            int g = 0;
            // This is a filter-down approach. First considering larger groups,
            // If found any match breaks their. Else go to the next step.
            // Helpful to solve some corner-cases.
            while (g < 5) {
                String key = m.group(g);
                if (key != null) {
                    boolean hasKey = composites.containsKey(key);
                    if (hasKey) {
                        appendableString = appendableString + composites.get(key);
                        break;
                    }
                    hasKey = letters.containsKey(key);
                    if (hasKey) {
                        appendableString = appendableString + letters.get(key);
                        break;
                    }
                }
                g = g + 1;
            }
            g = 5;
            while (g < 9) {
                String key = m.group(g);
                if (key != null) {
                    boolean hasKey = composites.containsKey(key);
                    if (hasKey) {
                        appendableString = appendableString + composites.get(key);
                        break;
                    }
                    hasKey = letters.containsKey(key);
                    if (hasKey) {
                        appendableString = appendableString + letters.get(key);
                        break;
                    }
                }
                g = g + 1;
            }
            String kaar = m.group(9);
            if (kaar != null) {
                boolean hasKey = letters.containsKey(kaar);
                if (hasKey) {
                    appendableString = appendableString + letters.get(kaar);
                }
            } else if (appendableString.length() > 0 && !appendableString.equals(".")) {
                // Adding 'a' like ITRANS if no vowel is present.
                // TODO: Have to add it dynamically using Bengali grammer rules.
                appendableString = appendableString + "a";
            }
            String others = m.group(0);
            if (others != null) {

                if (appendableString.length() <= 0) {
                    appendableString = appendableString + others;
                }
            }
            m.appendReplacement(sb, appendableString);
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
