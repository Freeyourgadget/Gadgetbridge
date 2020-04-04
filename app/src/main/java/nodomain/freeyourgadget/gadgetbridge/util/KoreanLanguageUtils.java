/*  Copyright (C) 2020 Ted Stein

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

import java.util.Optional;
import java.text.Normalizer;
import java.text.Normalizer.Form;

// Implements Revised Romanization of Korean as well as we can without understanding any grammar.
//
// https://en.wikipedia.org/wiki/Revised_Romanization_of_Korean
public class KoreanLanguageUtils {
    // https://en.wikipedia.org/wiki/Hangul_Jamo_%28Unicode_block%29
    private static final char JAMO_BLOCK_START = 0x1100;
    private static final char JAMO_BLOCK_END = 0x11FF;
    // https://en.wikipedia.org/wiki/Hangul_Syllables
    private static final char SYLLABLES_BLOCK_START = 0xAC00;
    private static final char SYLLABLES_BLOCK_END = 0xD7A3;
    // https://en.wikipedia.org/wiki/Hangul_Compatibility_Jamo
    private static final char COMPAT_JAMO_BLOCK_START = 0x3131;
    private static final char COMPAT_JAMO_BLOCK_END = 0x318E;

    // Returns whether a char is in the given block. Both bounds are inclusive.
    private static boolean inRange(char c, char start, char end) {
        return c >= start && c <= end;
    }

    // User input consisting of isolated jamo is usually mapped to the KS X 1001 compatibility
    // block, but jamo resulting from decomposed syllables are mapped to the modern one. This
    // function maps compat jamo to modern ones where possible and returns all other characters
    // unmodified.
    //
    // https://en.wikipedia.org/wiki/Hangul_Compatibility_Jamo
    // https://en.wikipedia.org/wiki/Hangul_Jamo_%28Unicode_block%29
    private static char decompatJamo(char jamo) {
        // KS X 1001 Hangul filler, not used in modern Unicode. A useful landmark in the
        // compatibility jamo block.
        // https://en.wikipedia.org/wiki/KS_X_1001#Hangul_Filler
        final char HANGUL_FILLER = 0x3164;

        // Don't do anything to characters outside the compatibility jamo block.
        if (!inRange(jamo, COMPAT_JAMO_BLOCK_START, COMPAT_JAMO_BLOCK_END)) { return jamo; }

        // Vowels are contiguous, in the same order, and unambiguous, so it's a simple offset.
        if (jamo >= 0x314F && jamo < HANGUL_FILLER) {
            return (char)(jamo - 0x1FEE);
        }

        // Consonants are organized differently. No clean way to do this.
        //
        // The compatibility jamo block doesn't distinguish between Choseong (leading) and Jongseong
        // (final) positions, but the modern block does. We map to Choseong here.
        switch (jamo) {
            case 0x3131: return 0x1100;     // ㄱ
            case 0x3132: return 0x1101;     // ㄲ
            case 0x3134: return 0x1102;     // ㄴ
            case 0x3137: return 0x1103;     // ㄷ
            case 0x3138: return 0x1104;     // ㄸ
            case 0x3139: return 0x1105;     // ㄹ
            case 0x3141: return 0x1106;     // ㅁ
            case 0x3142: return 0x1107;     // ㅂ
            case 0x3143: return 0x1108;     // ㅃ
            case 0x3145: return 0x1109;     // ㅅ
            case 0x3146: return 0x110A;     // ㅆ
            case 0x3147: return 0x110B;     // ㅇ
            case 0x3148: return 0x110C;     // ㅈ
            case 0x3149: return 0x110D;     // ㅉ
            case 0x314A: return 0x110E;     // ㅊ
            case 0x314B: return 0x110F;     // ㅋ
            case 0x314C: return 0x1110;     // ㅌ
            case 0x314D: return 0x1111;     // ㅍ
            case 0x314E: return 0x1112;     // ㅎ
        }

        // The rest of the compatibility block consists of archaic compounds that are unlikely to be
        // encountered in modern systems. Just leave them alone.
        return jamo;
    }

    // Transliterates jamo one at a time. Returns its input if it isn't in the modern jamo block.
    private static String transliterateSingleJamo(char jamo) {
        jamo = decompatJamo(jamo);

        switch (jamo) {
            // Choseong (leading position consonants)
            case 0x1100: return "g";    // ㄱ
            case 0x1101: return "kk";   // ㄲ
            case 0x1102: return "n";    // ㄴ
            case 0x1103: return "d";    // ㄷ
            case 0x1104: return "tt";   // ㄸ
            case 0x1105: return "r";    // ㄹ
            case 0x1106: return "m";    // ㅁ
            case 0x1107: return "b";    // ㅂ
            case 0x1108: return "pp";   // ㅃ
            case 0x1109: return "s";    // ㅅ
            case 0x110A: return "ss";   // ㅆ
            case 0x110B: return "";     // ㅇ
            case 0x110C: return "j";    // ㅈ
            case 0x110D: return "jj";   // ㅉ
            case 0x110E: return "ch";   // ㅊ
            case 0x110F: return "k";    // ㅋ
            case 0x1110: return "t";    // ㅌ
            case 0x1111: return "p";    // ㅍ
            case 0x1112: return "h";    // ㅎ
            // Jungseong (vowels)
            case 0x1161: return "a";    // ㅏ
            case 0x1162: return "ae";   // ㅐ
            case 0x1163: return "ya";   // ㅑ
            case 0x1164: return "yae";  // ㅒ
            case 0x1165: return "eo";   // ㅓ
            case 0x1166: return "e";    // ㅔ
            case 0x1167: return "yeo";  // ㅕ
            case 0x1168: return "ye";   // ㅖ
            case 0x1169: return "o";    // ㅗ
            case 0x116A: return "wa";   // ㅘ
            case 0x116B: return "wae";  // ㅙ
            case 0x116C: return "oe";   // ㅚ
            case 0x116D: return "yo";   // ㅛ
            case 0x116E: return "u";    // ㅜ
            case 0x116F: return "wo";   // ㅝ
            case 0x1170: return "we";   // ㅞ
            case 0x1171: return "wi";   // ㅟ
            case 0x1172: return "yu";   // ㅠ
            case 0x1173: return "eu";   // ㅡ
            case 0x1174: return "ui";   // ㅢ
            case 0x1175: return "i";    // ㅣ
            // Jongseong (final position consonants)
            case 0x11A8: return "k";    // ㄱ
            case 0x11A9: return "k";    // ㄲ
            case 0x11AB: return "n";    // ㄴ
            case 0x11AE: return "t";    // ㄷ
            case 0x11AF: return "l";    // ㄹ
            case 0x11B7: return "m";    // ㅁ
            case 0x11B8: return "p";    // ㅂ
            case 0x11BA: return "t";    // ㅅ
            case 0x11BB: return "t";    // ㅆ
            case 0x11BC: return "ng";   // ㅇ
            case 0x11BD: return "t";    // ㅈ
            case 0x11BE: return "t";    // ㅊ
            case 0x11BF: return "k";    // ㅋ
            case 0x11C0: return "t";    // ㅌ
            case 0x11C1: return "p";    // ㅍ
            case 0x11C2: return "t";    // ㅎ
        }

        // Input was not jamo.
        return String.valueOf(jamo);
    }

    // Some combinations of ending jamo in one syllable and initial jamo in the next are romanized
    // irregularly. These exceptions are called "special provisions". In cases where multiple
    // romanizations are permitted, we use the one that's least commonly used elsewhere.
    //
    // Returns no value if either character is not in the modern jamo block, or if there is no
    // special provision for that pair of jamo.
    public static Optional<String> transliterateSpecialProvisions(char previousEnding, char nextInitial) {
        // Special provisions only apply if both characters are in the modern jamo block.
        if (!inRange(previousEnding, JAMO_BLOCK_START, JAMO_BLOCK_END)) { return Optional.empty(); }
        if (!inRange(nextInitial, JAMO_BLOCK_START, JAMO_BLOCK_END)) { return Optional.empty(); }

        // Jongseong (final position) ㅎ has a number of special provisions.
        if (previousEnding == 0x11C2) { // ㅎ
            switch (nextInitial) {
                case 0x110B: return Optional.of("h");       // ㅇ
                case 0x1100: return Optional.of("k");       // ㄱ
                case 0x1102: return Optional.of("nn");      // ㄴ
                case 0x1103: return Optional.of("t");       // ㄷ
                case 0x1105: return Optional.of("nn");      // ㄹ
                case 0x1106: return Optional.of("nm");      // ㅁ
                case 0x1107: return Optional.of("p");       // ㅂ
                case 0x1109: return Optional.of("hs");      // ㅅ
                case 0x110C: return Optional.of("ch");      // ㅈ
                case 0x1112: return Optional.of("t");       // ㅎ
                default: return Optional.empty();
            }
        }

        // Otherwise, special provisions are denser when grouped by the second jamo.
        switch (nextInitial) {
            case 0x1100: // ㄱ
                switch (previousEnding) {
                    case 0x11AB: return Optional.of("n-g"); // ㄴ
                    default: return Optional.empty();
                }
            case 0x1102: // ㄴ
                switch (previousEnding) {
                    case 0x11A8: return Optional.of("ngn"); // ㄱ
                    case 0x11AE:                            // ㄷ
                    case 0x11BA:                            // ㅅ
                    case 0x11BD:                            // ㅈ
                    case 0x11BE:                            // ㅊ
                    case 0x11C0:                            // ㅌ
                        return Optional.of("nn");
                    case 0x11AF: return Optional.of("ll");  // ㄹ
                    case 0x11B8: return Optional.of("mn");  // ㅂ
                    default: return Optional.empty();
                }
            case 0x1105: // ㄹ
                switch (previousEnding) {
                    case 0x11A8:                            // ㄱ
                    case 0x11AB:                            // ㄴ
                    case 0x11AF:                            // ㄹ
                        return Optional.of("ll");
                    case 0x11AE:                            // ㄷ
                    case 0x11BA:                            // ㅅ
                    case 0x11BD:                            // ㅈ
                    case 0x11BE:                            // ㅊ
                    case 0x11C0:                            // ㅌ
                        return Optional.of("nn");
                    case 0x11B7:                            // ㅁ
                    case 0x11B8:                            // ㅂ
                        return Optional.of("mn");
                    case 0x11BC: return Optional.of("ngn"); // ㅇ
                    default: return Optional.empty();
                }
            case 0x1106: // ㅁ
                switch (previousEnding) {
                    case 0x11A8: return Optional.of("ngm"); // ㄱ
                    case 0x11AE:                            // ㄷ
                    case 0x11BA:                            // ㅅ
                    case 0x11BD:                            // ㅈ
                    case 0x11BE:                            // ㅊ
                    case 0x11C0:                            // ㅌ
                        return Optional.of("nm");
                    case 0x11B8: return Optional.of("mm");  // ㅂ
                    default: return Optional.empty();
                }
            case 0x110B: // ㅇ
                switch (previousEnding) {
                    case 0x11A8: return Optional.of("g");   // ㄱ
                    case 0x11AE: return Optional.of("d");   // ㄷ
                    case 0x11AF: return Optional.of("r");   // ㄹ
                    case 0x11B8: return Optional.of("b");   // ㅂ
                    case 0x11BA: return Optional.of("s");   // ㅅ
                    case 0x11BC: return Optional.of("ng-"); // ㅇ
                    case 0x11BD: return Optional.of("j");   // ㅈ
                    case 0x11BE: return Optional.of("ch");  // ㅊ
                    default: return Optional.empty();
                }
            case 0x110F: // ㅋ
                switch (previousEnding) {
                    case 0x11A8: return Optional.of("k-k"); // ㄱ
                    default: return Optional.empty();
                }
            case 0x1110: // ㅌ
                switch (previousEnding) {
                    case 0x11AE:                            // ㄷ
                    case 0x11BA:                            // ㅅ
                    case 0x11BD:                            // ㅈ
                    case 0x11BE:                            // ㅊ
                    case 0x11C0:                            // ㅌ
                        return Optional.of("t-t");
                    default: return Optional.empty();
                }
            case 0x1111: // ㅍ
                switch (previousEnding) {
                    case 0x11B8: return Optional.of("p-p"); // ㅂ
                    default: return Optional.empty();
                }
            default: return Optional.empty();
        }
    }

    // Decompose a syllable into several jamo. Returns its input if that isn't possible.
    public static char[] decompose(char syllable) {
        String normalized = Normalizer.normalize(String.valueOf(syllable), Normalizer.Form.NFD);
        return normalized.toCharArray();
    }

    // Transliterate any Hangul in the given string. Leaves any non-Hangul characters unmodified.
    public static String transliterate(String txt) {
        if (txt == null || txt.isEmpty()) {
            return txt;
        }

        // Most of the bulk of these loops is for handling special provisions - situations where the
        // last jamo of one syllable and the first of the next need to be romanized as a pair in an
        // irregular way.
        StringBuilder builder = new StringBuilder();
        boolean nextInitialJamoConsumed = false;
        char[] syllables = txt.toCharArray();
        for (int i = 0; i < syllables.length; i++) {
            char thisSyllable = syllables[i];
            // If this isn't in any of the Hangul blocks we know about, emit it as-is.
            if (!inRange(thisSyllable, JAMO_BLOCK_START, JAMO_BLOCK_END)
                    && !inRange(thisSyllable, SYLLABLES_BLOCK_START, SYLLABLES_BLOCK_END)
                    && !inRange(thisSyllable, COMPAT_JAMO_BLOCK_START, COMPAT_JAMO_BLOCK_END)) {
                builder.append(thisSyllable);
                continue;
            }

            char[] theseJamo = decompose(thisSyllable);
            for (int j = 0; j < theseJamo.length; j++) {
                char thisJamo = theseJamo[j];

                // If we already transliterated the first jamo of this syllable as part of a special
                // provision, skip it. Otherwise, handle it in the unconditional else branch.
                if (j == 0 && nextInitialJamoConsumed) {
                    nextInitialJamoConsumed = false;
                    continue;
                }

                // If this is the last jamo of this syllable and not the last syllable of the
                // string, check for special provisions. If the next char is whitespace or not
                // Hangul, it's the responsibility of transliterateSpecialProvisions() to return no
                // value.
                if (j == theseJamo.length - 1 && i < syllables.length - 1) {
                    char nextSyllable = syllables[i + 1];
                    char nextJamo = decompose(nextSyllable)[0];
                    Optional<String> specialProvision = transliterateSpecialProvisions(thisJamo, nextJamo);
                    if (specialProvision.isPresent()) {
                        builder.append(specialProvision.get());
                        nextInitialJamoConsumed = true;
                    } else {
                        // No special provision applies. Transliterate in isolation.
                        builder.append(transliterateSingleJamo(thisJamo));
                    }
                    continue;
                }

                // Everything else is transliterated in isolation.
                builder.append(transliterateSingleJamo(thisJamo));
            }
        }

        return builder.toString();
    }
}
