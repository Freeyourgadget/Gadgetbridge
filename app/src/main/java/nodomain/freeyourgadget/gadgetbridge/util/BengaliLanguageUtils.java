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
import java.util.regex.*;

// What's the reason to extending LanguageUtils?
// Just doing it because already done in the previous code.
public class BengaliLanguageUtils extends LanguageUtils {
        // Composite Letters.
    private final static HashMap<String, String> composites = new HashMap<String, String>() {
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
    // Vowels Only
    private final static HashMap<String, String> vowelsAndHasants = new HashMap<String, String>() {
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
            put("।", ".");
        }
    };

    // Single Character Letters.
    private final static HashMap<String, String> letters = new HashMap<String, String>() {
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
    private final static String pattern = "(র্){0,1}(([অ-হড়-য়])(্([অ-মশ-হড়-য়]))*)((‍){0,1}(্([য-ল]))){0,1}([া-ৌ]){0,1}|([্ঁঃংৎ০-৯।])|(\\s)";

    private final static Pattern bengaliRegex = Pattern.compile(pattern);

    private static String getVal(String key) {
        if (key != null) {
            String comp = composites.get(key);
            if (comp != null) {
                return comp;
            }
            String sl = letters.get(key);
            if (sl != null) {
                return letters.get(key);
            }
        }
        return null;
    }

    public static String transliterate(String txt) {
        if (txt.isEmpty()) {
            return txt;
        }

        Matcher m = bengaliRegex.matcher(txt);
        StringBuffer sb = new StringBuffer();
        String lastChar = "";
        Boolean lastHadComposition = false;
        Boolean lastHadKaar = false;
        Boolean nextNeedsO = false;
        while (m.find()) {
            Boolean thisNeedsO = false;
            Boolean changePronounciation = false;
            String appendableString = "";
            String reff = m.group(1);
            if (reff != null) {
                appendableString = appendableString + "rr";
            }
            // This is a filter-down approach. First considering larger groups,
            // If found any match breaks their. Else go to the next step.
            // Helpful to solve some corner-cases.
            String mainPart = getVal(m.group(2));
            if (mainPart != null) {
                appendableString = appendableString + mainPart;
            } else {
                String firstPart = getVal(m.group(3));
                if (firstPart != null) {
                    appendableString = appendableString + firstPart;
                }
                int g = 4;
                while (g < 6) {
                    String part = getVal(m.group(g));
                    if (part != null) {
                        appendableString = appendableString + part;
                        break;
                    }
                    g = g + 1;
                }
            }
            if (m.group(2) != null && m.group(2).equals("ক্ষ")) {
                changePronounciation = true;
                thisNeedsO = true;
            }
            int g = 6;
            while (g < 10) {
                String key = getVal(m.group(g));
                if (key != null) {
                    appendableString = appendableString + key;
                    break;
                }
                g = g + 1;
            }
            String phala = m.group(8);
            if (phala != null && phala.equals("্য")) {
                changePronounciation = true;
                thisNeedsO = true;
            }
            String jukto = m.group(4);
            if (jukto != null) {
                thisNeedsO = true;
            }
            String kaar = m.group(10);
            if (kaar != null) {
                String kaarStr = letters.get(kaar);
                if (kaarStr != null) {
                    appendableString = appendableString + kaarStr;
                }
                if (kaarStr.equals("i") || kaarStr.equals("ii") || kaarStr.equals("u") || kaarStr.equals("uu")) {
                    changePronounciation = true;
                }
            }
            String singleton = m.group(11);
            if (singleton != null) {
                String singleStr = letters.get(singleton);
                if (singleStr != null) {
                    appendableString = appendableString + singleStr;
                }
            }
            if (changePronounciation && lastChar.equals("a")) {
                sb.setCharAt(sb.length() - 1, 'o');
            }
            String others = m.group(0);
            if (others != null) {

                if (appendableString.length() <= 0) {
                    appendableString = appendableString + others;
                }
            }
            String whitespace = m.group(12);
            if (nextNeedsO && kaar == null && whitespace == null) {
                appendableString = appendableString + "o";
                thisNeedsO = false;
            }

            if (whitespace != null && !lastHadKaar && sb.length() > 0 && sb.charAt(sb.length() - 1) == 'o' && !lastHadComposition) {
                sb.setCharAt(sb.length() - 1, Character.MIN_VALUE);
            }
            nextNeedsO = false;
            if (thisNeedsO && kaar == null && whitespace == null) {
                appendableString = appendableString + "o";
            }
            if (appendableString.length() > 0 && !vowelsAndHasants.containsKey(m.group(0)) && kaar == null) {
                nextNeedsO = true;
            }
            if (reff != null || m.group(4) != null || m.group(6) != null) {
                lastHadComposition = true;
            } else {
                lastHadComposition = false;
            }
            if (kaar != null) {
                lastHadKaar = true;
            } else {
                lastHadKaar = false;
            }
            m.appendReplacement(sb, appendableString);
            lastChar = appendableString;
        }
        if (!lastHadKaar && sb.length() > 0 && sb.charAt(sb.length() - 1) == 'o' && !lastHadComposition) {
            sb.setCharAt(sb.length() - 1, Character.MIN_VALUE);
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
