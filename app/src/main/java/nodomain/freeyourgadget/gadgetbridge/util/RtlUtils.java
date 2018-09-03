package nodomain.freeyourgadget.gadgetbridge.util;

import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;

class RtlUtils {

    enum characterType{
        ltr,
        rtl,
        rtl_arabic,
        punctuation,
        lineEnd,
        space,
    }

    public static characterType getCharacterType(Character c){
        characterType type;
        switch (Character.getDirectionality(c)) {
            case Character.DIRECTIONALITY_RIGHT_TO_LEFT:
                type = characterType.rtl;
                break;
            case Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC:
                type = characterType.rtl_arabic;
                break;
            case Character.DIRECTIONALITY_EUROPEAN_NUMBER_SEPARATOR:
            case Character.DIRECTIONALITY_EUROPEAN_NUMBER_TERMINATOR:
            case Character.DIRECTIONALITY_COMMON_NUMBER_SEPARATOR:
            case Character.DIRECTIONALITY_OTHER_NEUTRALS:
                type = characterType.punctuation;
                break;
            case Character.DIRECTIONALITY_BOUNDARY_NEUTRAL:
            case Character.DIRECTIONALITY_PARAGRAPH_SEPARATOR:
                type = characterType.lineEnd;
                break;
            case Character.DIRECTIONALITY_WHITESPACE:
                type = characterType.space;
                break;
            default:
                type = characterType.ltr;
        }

        return type;
    }

    /**
     * Checks the status of right-to-left option
     * @return true if right-to-left option is On, and false, if Off or not exist
     */
    public static boolean contextualSupport()
    {
        return GBApplication.getPrefs().getBoolean("contextualArabic", false);
    }

    //map with brackets chars to change there direction
    private static Map<Character, Character> directionSignsMap = new HashMap<Character, Character>(){
        {
            put('(', ')'); put(')', '('); put('[', ']'); put(']', '['); put('{','}'); put('}','{');


        }
    };

    /**
     * @return true if the char is in the rtl range, otherwise false
     */
    static boolean isHebrew(Character c){

        return getCharacterType(c) == characterType.rtl;
    }

    /**
     * @return true if the char is in the rtl range, otherwise false
     */
    static boolean isArabic(Character c){

        return getCharacterType(c) == characterType.rtl_arabic;
    }

    /**
     * @return true if the char is in the rtl range, otherwise false
     */
    static boolean isLtr(Character c){

        return getCharacterType(c) == characterType.ltr;
    }

    /**
     * @return true if the char is in the rtl range, otherwise false
     */
    static boolean isRtl(Character c){

        return (getCharacterType(c) == characterType.rtl) || (getCharacterType(c) == characterType.rtl_arabic);
    }

    /**
     * @return true if the char is in the punctuations range, otherwise false
     */
    static boolean isPunctuations(Character c){

        return getCharacterType(c) == characterType.punctuation;
    }


    /**
     * @return true if the char is in the end of word list, otherwise false
     */
    static boolean isSpaceSign(Character c){

        return getCharacterType(c) == characterType.space;
    }

    /**
     * @return true if the char is in the end of word list, otherwise false
     */
    static boolean isEndLineSign(Character c){

        return getCharacterType(c) == characterType.lineEnd;
    }

    //map from Arabian characters to their contextual form in the beginning of the word
    private static Map<Character, Character> contextualArabicIsolated = new HashMap<Character, Character>(){
        {
            put('ا', '\uFE8D');
            put('ب', '\uFE8F');
            put('ت', '\uFE95');
            put('ث', '\uFE99');
            put('ج', '\uFE9D');
            put('ح', '\uFEA1');
            put('خ', '\uFEA5');
            put('د', '\uFEA9');
            put('ذ', '\uFEAB');
            put('ر', '\uFEAD');
            put('ز', '\uFEAF');
            put('س', '\uFEB1');
            put('ش', '\uFEB5');
            put('ص', '\uFEB9');
            put('ض', '\uFEBD');
            put('ط', '\uFEC1');
            put('ظ', '\uFEC5');
            put('ع', '\uFEC9');
            put('غ', '\uFECD');
            put('ف', '\uFED1');
            put('ق', '\uFED5');
            put('ك', '\uFED9');
            put('ل', '\uFEDD');
            put('م', '\uFEE1');
            put('ن', '\uFEE5');
            put('ه', '\uFEE9');
            put('و', '\uFEED');
            put('ي', '\uFEF1');
            put('آ', '\uFE81');
            put('ة', '\uFE93');
            put('ى', '\uFEEF');
            put('ئ', '\uFE89');
            put('إ', '\uFE87');
            put('أ', '\uFE83');
            put('ء', '\uFE80');
            put('ؤ', '\uFE85');
            put((char)('ل' + 'آ'), '\uFEF5');
            put((char)('ل' + 'أ'), '\uFEF7');
            put((char)('ل' + 'إ'), '\uFEF9');
            put((char)('ل' + 'ا'), '\uFEFB');

        }
    };

    //map from Arabian characters to their contextual form in the beginning of the word
    private static Map<Character, Character> contextualArabicBeginning = new HashMap<Character, Character>(){
        {
            put('ب', '\uFE91');
            put('ت', '\uFE97');
            put('ث', '\uFE9B');
            put('ج', '\uFE9F');
            put('ح', '\uFEA3');
            put('خ', '\uFEA7');
            put('س', '\uFEB3');
            put('ش', '\uFEB7');
            put('ص', '\uFEBB');
            put('ض', '\uFEBF');
            put('ط', '\uFEC3');
            put('ظ', '\uFEC7');
            put('ع', '\uFECB');
            put('غ', '\uFECF');
            put('ف', '\uFED3');
            put('ق', '\uFED7');
            put('ك', '\uFEDB');
            put('ل', '\uFEDF');
            put('م', '\uFEE3');
            put('ن', '\uFEE7');
            put('ه', '\uFEEB');
            put('ي', '\uFEF3');
            put('ئ', '\uFE8B');
        }
    };

    //map from Arabian characters to their contextual form in the middle of the word
    private static Map<Character, Character> contextualArabicMiddle = new HashMap<Character, Character>(){
        {
            put('ب', '\uFE92');
            put('ت', '\uFE98');
            put('ث', '\uFE9C');
            put('ج', '\uFEA0');
            put('ح', '\uFEA4');
            put('خ', '\uFEA8');
            put('س', '\uFEB4');
            put('ش', '\uFEB8');
            put('ص', '\uFEBC');
            put('ض', '\uFEC0');
            put('ط', '\uFEC4');
            put('ظ', '\uFEC8');
            put('ع', '\uFECC');
            put('غ', '\uFED0');
            put('ف', '\uFED4');
            put('ق', '\uFED8');
            put('ك', '\uFEDC');
            put('ل', '\uFEE0');
            put('م', '\uFEE4');
            put('ن', '\uFEE8');
            put('ه', '\uFEEC');
            put('ي', '\uFEF4');
            put('ئ', '\uFE8C');
        }
    };

    //map from Arabian characters to their contextual form in the end of the word
    private static Map<Character, Character> contextualArabicEnd = new HashMap<Character, Character>(){
        {
            put('ا', '\uFE8E');
            put('ب', '\uFE90');
            put('ت', '\uFE96');
            put('ث', '\uFE9A');
            put('ج', '\uFE9E');
            put('ح', '\uFEA2');
            put('خ', '\uFEA6');
            put('د', '\uFEAA');
            put('ذ', '\uFEAC');
            put('ر', '\uFEAE');
            put('ز', '\uFEB0');
            put('س', '\uFEB2');
            put('ش', '\uFEB6');
            put('ص', '\uFEBA');
            put('ض', '\uFEBE');
            put('ط', '\uFEC2');
            put('ظ', '\uFEC6');
            put('ع', '\uFECA');
            put('غ', '\uFECE');
            put('ف', '\uFED2');
            put('ق', '\uFED6');
            put('ك', '\uFEDA');
            put('ل', '\uFEDE');
            put('م', '\uFEE2');
            put('ن', '\uFEE6');
            put('ه', '\uFEEA');
            put('و', '\uFEEE');
            put('ي', '\uFEF2');
            put('آ', '\uFE82');
            put('ة', '\uFE94');
            put('ى', '\uFEF0');
            put('ئ', '\uFE8A');
            put('إ', '\uFE88');
            put('أ', '\uFE84');
            put('ؤ', '\uFE86');
            put((char)('ل' + 'آ'), '\uFEF6');
            put((char)('ل' + 'أ'), '\uFEF8');
            put((char)('ل' + 'إ'), '\uFEFA');
            put((char)('ل' + 'ا'), '\uFEFC');
        }
    };
    enum contextualState{
        isolate,
        begin,
        middle,
        end
    }

    private static boolean exceptionAfterLam(Character c){
        switch (c){
            case '\u0622':
            case '\u0623':
            case '\u0625':
            case '\u0627':
                return true;
            default:
                return false;

        }
    }

    /**
     * This function return the contextual form of Arabic characters in a given state
     * @param c - the character to convert
     * @param state - the character state: beginning, middle, end or isolated
     * @return the contextual character
     */
    private static Character getContextualSymbol(Character c, contextualState state) {
        Character newChar;
        switch (state){
            case begin:
                newChar = contextualArabicBeginning.get(c);
                break;
            case middle:
                newChar = contextualArabicMiddle.get(c);
                break;
            case end:
                newChar = contextualArabicEnd.get(c);
                break;
            case isolate:
            default:
                newChar  = contextualArabicIsolated.get(c);;
        }
        if (newChar != null){
            return newChar;
        } else{
            return c;
        }
    }

    /**
     * This function return the contextual state of a given character, depend of the previous
     * character state and the next charachter.
     * @param prevState - previous character state or isolated if none
     * @param curChar - the current character
     * @param nextChar - the next character or null if none
     * @return the current character contextual state
     */
    private static contextualState getCharContextualState(contextualState prevState, Character curChar, Character nextChar) {
        contextualState curState;
        if ((prevState == contextualState.isolate || prevState == contextualState.end) &&
                contextualArabicBeginning.containsKey(curChar) &&
                contextualArabicEnd.containsKey(nextChar)){

            curState = contextualState.begin;

        } else if ((prevState == contextualState.begin || prevState == contextualState.middle) &&
                contextualArabicEnd.containsKey(curChar)){

            if (contextualArabicMiddle.containsKey(curChar) && contextualArabicEnd.containsKey(nextChar)){
                curState = contextualState.middle;
            }else{
                curState = contextualState.end;
            }
        }else{
            curState = contextualState.isolate;
        }
        return curState;
    }

    /**
     * this function convert given string to it's contextual form
     * @param s - the given string
     * @return the contextual form of the string
     */
    static String converToContextual(String s){
        if (s == null || s.isEmpty() || s.length() == 1){
            return s;
        }

        int length = s.length();
        StringBuilder newWord = new StringBuilder(length);

        Character curChar, nextChar = s.charAt(0);
        contextualState prevState = contextualState.isolate;
        contextualState curState = contextualState.isolate;

        for (int i = 0; i < length - 1; i++){
            curChar = nextChar;
            nextChar = s.charAt(i + 1);

            if (curChar == 'ل' && exceptionAfterLam(nextChar)){
                i++;
                curChar = (char)(nextChar + curChar);
                if (i < length - 1) {
                    nextChar = s.charAt(i + 1);
                }else{
                    nextChar = curChar;
                    prevState = curState;
                    break;
                }

            }

            curState = getCharContextualState(prevState, curChar, nextChar);
            newWord.append(getContextualSymbol(curChar, curState));
            prevState = curState;


        }
        curState = getCharContextualState(prevState, nextChar, null);
        newWord.append(getContextualSymbol(nextChar, curState));

        return newWord.toString();
    }


    /**
     * The function get a string and reverse it.
     * in case of end-of-word sign, it will leave it at the end.
     * in case of sign with direction like brackets, it will change the direction.
     * @param s - the string to reverse
     * @return reversed string
     */
    static String reverse(String s) {
        int j = s.length();
        int isEndLine = 0;
        char[] newWord = new char[j];

        if (j == 0) {
            return s;
        }

        // remain end-of-word sign at the end
//        if (isEndLineSign(s.charAt(s.length() - 1))){
//            isEndLine = 1;
//            newWord[--j] = s.charAt(s.length() - 1);
//        }

        for (int i = 0; i < s.length() - isEndLine; i++) {
            if (directionSignsMap.containsKey(s.charAt(i))) {
                newWord[--j] = directionSignsMap.get(s.charAt(i));
            } else {
                newWord[--j] = s.charAt(i);
            }
        }

        return new String(newWord);
    }

    static String fixWhitespace(String s){
        int length = s.length();

        if (length > 0 && isSpaceSign(s.charAt(length - 1))){
            return s.charAt(length - 1) + s.substring(0, length - 1);
        } else {
            return s;
        }
    }
}
