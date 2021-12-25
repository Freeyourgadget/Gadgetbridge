package nodomain.freeyourgadget.gadgetbridge.test;

import android.content.SharedPreferences;

import org.junit.Test;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;
import nodomain.freeyourgadget.gadgetbridge.util.RtlUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests RtlUtils
 */
public class RtlUtilsTest extends TestBase {

//    @Test
    private Character hebrew1 = 'א';
    private Character hebrew2 = 'ם';

    private Character arabic1 = 'ا';
    private Character arabic2 = 'ن';

    private Character english1 = 'a';
    private Character english2 = 'N';

    private Character punctuation1 = '.';
    private Character punctuation2 = '-';

    private Character space = ' ';

    private Character endLine1 = '\0';
    private Character endLine2 = '\n';

    private Character[] contextualIsolated = {'ء', '\uFE80'};
    private Character[] contextualBeginning = {'س', '\uFEB3'};
    private Character[] contextualMiddle = {'ض', '\uFEC0'};
    private Character[] contextualEndNoMiddle = {'آ', '\uFE82'};
    private Character[] contextualLam = {(char)('ل' + 'أ'), '\uFEF8'};

    private String hebrewWord1 = "שלום";
    private String hebrewWord2 = "וברכה";
    private String hebrewPhrase = hebrewWord1 + " " + hebrewWord2;

    private String arabicWord1 = "سلام";
    private String arabicWord2 = "البركة";
    private String arabicPhrase = arabicWord1 + " " + arabicWord2;

    private String englishWord1 = "Hello";
    private String englishWord2 = "Welcome";
    private String englishPhrase = englishWord1 + " and " + englishWord2;

    @Test
    public void testGetCharacterType() throws Exception {

        RtlUtils.characterType actual = RtlUtils.getCharacterType(hebrew1);
        RtlUtils.characterType expected = RtlUtils.characterType.rtl;
        assertEquals("Get Hebrew Character type failed", expected, actual);

        actual = RtlUtils.getCharacterType(arabic1);
        expected = RtlUtils.characterType.rtl_arabic;
        assertEquals("Get Arabic Character type failed", expected, actual);

        actual = RtlUtils.getCharacterType(english1);
        expected = RtlUtils.characterType.ltr;
        assertEquals("Get Hebrew Character type failed", expected, actual);

        actual = RtlUtils.getCharacterType(' ');
        expected = RtlUtils.characterType.space;
        assertEquals("Get Hebrew Character type failed", expected, actual);
    }


    @Test
    public void testIsHebrew() throws Exception {
        boolean result = RtlUtils.isHebrew(hebrew1);
        assertTrue("Is Hebrew Character failed", result);

        result = RtlUtils.isHebrew(hebrew2);
        assertTrue("Is Hebrew Character failed", result);

        result = RtlUtils.isHebrew(arabic1);
        assertFalse("Is Hebrew Character failed", result);

        result = RtlUtils.isHebrew(english1);
        assertFalse("Is Hebrew Character failed", result);
    }

    @Test
    public void testIsArabic() throws Exception {
        boolean result = RtlUtils.isArabic(arabic1);
        assertTrue("Is Arabic Character failed", result);

        result = RtlUtils.isArabic(arabic2);
        assertTrue("Is Arabic Character failed", result);

        result = RtlUtils.isArabic(hebrew1);
        assertFalse("Is Arabic Character failed", result);

        result = RtlUtils.isArabic(english1);
        assertFalse("Is Arabic Character failed", result);
    }

    @Test
    public void testIsLtr() throws Exception {
        boolean result = RtlUtils.isLtr(english1);
        assertTrue("Is Ltr Character failed", result);

        result = RtlUtils.isLtr(english2);
        assertTrue("Is Ltr Character failed", result);

        result = RtlUtils.isLtr(hebrew1);
        assertFalse("Is Ltr Character failed", result);

        result = RtlUtils.isLtr(arabic1);
        assertFalse("Is Ltr Character failed", result);
    }

    @Test
    public void testIsRtl() throws Exception {
        boolean result = RtlUtils.isRtl(hebrew1);
        assertTrue("Is Rtl Character failed", result);

        result = RtlUtils.isRtl(arabic1);
        assertTrue("Is Rtl Character failed", result);

        result = RtlUtils.isRtl(english1);
        assertFalse("Is Rtl Character failed", result);

        result = RtlUtils.isRtl(punctuation1);
        assertFalse("Is Rtl Character failed", result);
    }

    @Test
    public void testIsPunctuation() throws Exception {
        boolean result = RtlUtils.isPunctuations(punctuation1);
        assertTrue("Is Punctuation Character failed", result);

        result = RtlUtils.isPunctuations(punctuation2);
        assertTrue("Is Punctuation Character failed", result);

        result = RtlUtils.isPunctuations(english1);
        assertFalse("Is Punctuation Character failed", result);

        result = RtlUtils.isPunctuations(space);
        assertFalse("Is Punctuation Character failed", result);
    }

    @Test
    public void testIsSpaceSign() throws Exception {
        boolean result = RtlUtils.isSpaceSign(space);
        assertTrue("Is Space Sign Character failed", result);

        result = RtlUtils.isSpaceSign(punctuation1);
        assertFalse("Is Space Sign Character failed", result);

        result = RtlUtils.isSpaceSign(english1);
        assertFalse("Is SpaceSign Character failed", result);

        result = RtlUtils.isSpaceSign(hebrew1);
        assertFalse("Is SpaceSign Character failed", result);
    }

    @Test
    public void testIsEndLineSign() throws Exception {
        boolean result = RtlUtils.isEndLineSign(endLine1);
        assertTrue("Is End Line Sign Character failed", result);

        result = RtlUtils.isEndLineSign(endLine2);
        assertTrue("Is End Line Sign Character failed", result);

        result = RtlUtils.isEndLineSign(english1);
        assertFalse("Is End Line Sign Character failed", result);

        result = RtlUtils.isEndLineSign(space);
        assertFalse("Is End Line Sign Character failed", result);
    }

    @Test
    public void testExceptionAfterLam() throws Exception {
        boolean result = RtlUtils.exceptionAfterLam('\u0623');
        assertTrue("Is Exception After Lam failed", result);

        result = RtlUtils.exceptionAfterLam('\u0630');
        assertFalse("Is Exception After Lam failed", result);

        result = RtlUtils.exceptionAfterLam('\u062A');
        assertFalse("Is Exception After Lam failed", result);
    }


    @Test
    public void testGetContextualSymbol() throws Exception {

        Character actual = RtlUtils.getContextualSymbol(contextualIsolated[0], RtlUtils.contextualState.isolate);
        Character expected = contextualIsolated[1];
        assertEquals("Get Contextual Symbol failed", expected, actual);

        actual = RtlUtils.getContextualSymbol(contextualBeginning[0], RtlUtils.contextualState.begin);
        expected = contextualBeginning[1];
        assertEquals("Get Contextual Symbol failed", expected, actual);

        actual = RtlUtils.getContextualSymbol(contextualMiddle[0], RtlUtils.contextualState.middle);
        expected = contextualMiddle[1];
        assertEquals("Get Contextual Symbol failed", expected, actual);

        actual = RtlUtils.getContextualSymbol(contextualEndNoMiddle[0], RtlUtils.contextualState.end);
        expected = contextualEndNoMiddle[1];
        assertEquals("Get Contextual Symbol failed", expected, actual);

        actual = RtlUtils.getContextualSymbol(contextualEndNoMiddle[0], RtlUtils.contextualState.middle);
        expected = contextualEndNoMiddle[0];
        assertEquals("Get Contextual Symbol failed", expected, actual);
    }

    @Test
    public void testGetCharContextualState() throws Exception {

        RtlUtils.contextualState actual = RtlUtils.getCharContextualState(RtlUtils.contextualState.isolate, contextualBeginning[0], contextualEndNoMiddle[0]);
        RtlUtils.contextualState expected = RtlUtils.contextualState.begin;
        assertEquals("Get Char Contextual State failed", expected, actual);

        actual = RtlUtils.getCharContextualState(RtlUtils.contextualState.begin, contextualMiddle[0], contextualEndNoMiddle[0]);
        expected = RtlUtils.contextualState.middle;
        assertEquals("Get Char Contextual State failed", expected, actual);

        actual = RtlUtils.getCharContextualState(RtlUtils.contextualState.begin, contextualMiddle[0], contextualIsolated[0]);
        expected = RtlUtils.contextualState.end;
        assertEquals("Get Char Contextual State failed", expected, actual);

        actual = RtlUtils.getCharContextualState(RtlUtils.contextualState.begin, contextualIsolated[0], contextualEndNoMiddle[0]);
        expected = RtlUtils.contextualState.isolate;
        assertEquals("Get Char Contextual State failed", expected, actual);
    }

    @Test
    public void testConvertToContextual() throws Exception {

        String actual = RtlUtils.convertToContextual("");
        String expected = "";
        assertEquals("Convert To Contextual failed", expected, actual);

        char[] nonContextual = {'ج', 'ج', 'ج'};
        char[] contextual = {'\uFE9F', '\uFEA0', '\uFE9E'};
        actual = RtlUtils.convertToContextual(new StringBuilder().append(nonContextual).toString());
        expected = new StringBuilder().append(contextual).toString();
        assertEquals("Convert To Contextual failed", expected, actual);

        char[] nonContextual2 = {'ج', 'ج', 'ل', '\u0622','ج'};
        char[] contextual2 = {'\uFE9F', '\uFEA0', '\uFEF6', '\uFE9D'};
        actual = RtlUtils.convertToContextual(new StringBuilder().append(nonContextual2).toString());
        expected = new StringBuilder().append(contextual2).toString();
        assertEquals("Convert To Contextual failed", expected, actual);
    }

    @Test
    public void testReverse() throws Exception {

        String actual = RtlUtils.reverse(hebrewWord1);
        String expected = "םולש";
        assertEquals("Reverse failed", expected, actual);

        actual = RtlUtils.reverse(hebrewPhrase);
        expected = "הכרבו םולש";
        assertEquals("Reverse failed", expected, actual);

        actual = RtlUtils.reverse("טקסט עם (סוגריים) וסימן שאלה?");
        expected = "?הלאש ןמיסו (םיירגוס) םע טסקט";
        assertEquals("Reverse failed", expected, actual);
    }

    @Test
    public void testFixWhitespace() throws Exception {

        String actual = RtlUtils.fixWhitespace(englishPhrase + " ");
        String expected = " " + englishPhrase;
        assertEquals("fix whitespace failed", expected, actual);

        actual = RtlUtils.fixWhitespace(englishPhrase);
        expected = englishPhrase;
        assertEquals("fix whitespace failed", expected, actual);
    }

    @Test
    public void testFixRtl() throws Exception {

        String actual = RtlUtils.fixRtl("Only english");
        String expected = "Only english";
        assertEquals("fix rtl failed", expected, actual);

        actual = RtlUtils.fixRtl("Only very long english with more than 18 characters");
        expected = " Only very long\n" +
                " english with more\n" +
                " than 18\n" +
                "characters";
        assertEquals("fix rtl failed", expected, actual);

        actual = RtlUtils.fixRtl("רק עברית");
        expected = "תירבע קר";
        assertEquals("fix rtl failed", expected, actual);

        actual = RtlUtils.fixRtl("English and עברית");
        expected = "תירבע English and";
        assertEquals("fix rtl failed", expected, actual);

        actual = RtlUtils.fixRtl("משפט ארוך עם עברית and English וגם קצת סימנים כמו ?!$ (וגם ^.)");
        expected = " םע ךורא טפשמ\n" +
                " and English תירבע\n" +
                " םינמיס תצק םגו\n" +
                "(.^ םגו) $!? ומכ";
        assertEquals("fix rtl failed", expected, actual);



        actual = RtlUtils.fixRtl("משפט עם כותרת\0ושמכיל גם ירידת\nשורה");
        expected = "תרתוכ םע טפשמ\0תדירי םג ליכמשו\n" +
                "הרוש";
        assertEquals("fix rtl failed", expected, actual);
    }

    @Test
    public void testRtlSupport() throws Exception {
        setDefaultRtl();
        assertFalse("Rtl option fail! Expected 'Off' by default, but result is 'On'", RtlUtils.rtlSupport());

        enableRtl(true);
        assertTrue("Rtl option fail! Expected 'On', but result is 'Off'", RtlUtils.rtlSupport());
    }

    private void setDefaultRtl() {
        SharedPreferences settings = GBApplication.getPrefs().getPreferences();
        SharedPreferences.Editor editor = settings.edit();
        editor.remove(GBPrefs.RTL_SUPPORT);
        editor.apply();
    }

    private void enableRtl(boolean enable) {
        SharedPreferences settings = GBApplication.getPrefs().getPreferences();
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(GBPrefs.RTL_SUPPORT, enable);
        editor.apply();
    }

    @Test
    public void testContextualSupport() throws Exception {
        setDefaultContextual();
        assertFalse("Contextual option fail! Expected 'Off' by default, but result is 'On'", RtlUtils.contextualSupport());

        enableContextual(true);
        assertTrue("Contextual option fail! Expected 'On', but result is 'Off'", RtlUtils.contextualSupport());
    }

    private void setDefaultContextual() {
        SharedPreferences settings = GBApplication.getPrefs().getPreferences();
        SharedPreferences.Editor editor = settings.edit();
        editor.remove(GBPrefs.RTL_CONTEXTUAL_ARABIC);
        editor.apply();
    }

    private void enableContextual(boolean enable) {
        SharedPreferences settings = GBApplication.getPrefs().getPreferences();
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(GBPrefs.RTL_CONTEXTUAL_ARABIC, enable);
        editor.apply();
    }
}
