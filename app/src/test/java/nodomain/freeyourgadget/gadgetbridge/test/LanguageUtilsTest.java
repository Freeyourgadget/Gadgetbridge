package nodomain.freeyourgadget.gadgetbridge.test;

import android.content.SharedPreferences;

import org.junit.Test;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.util.LanguageUtils;
import nodomain.freeyourgadget.gadgetbridge.util.KoreanLanguageUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests LanguageUtils
 */
public class LanguageUtilsTest extends TestBase {
    @Test
    public void testStringTransliterateCyrillic() throws Exception {
        // input with cyrillic and diacritic letters
        String input = "Прõсто текčт";
        String output = LanguageUtils.transliterate(input);
        String result = "Prosto tekct";

        assertEquals("Transliteration failed", result, output);
    }

    @Test
    public void testStringTransliterateHebrew() throws Exception {
        String input = "בדיקה עברית";
        String output = LanguageUtils.transliterate(input);
        String result = "bdykh 'bryth";

        assertEquals("Transliteration failed", result, output);
    }

    @Test
    public void testStringTransliterateArabic() {
        String pangram = "نص حكيم له سر قاطع وذو شأن عظيم مكتوب على ثوب أخضر ومغلف بجلد أزرق";
        String pangramExpected = "n9 7kym lh sr qa63 wthw sh2n 36'ym mktwb 3la thwb 259'r wm3'lf bjld 2zrq";
        String pangramActual = LanguageUtils.transliterate(pangram);
        assertEquals("Arabic pangram transliteration failed", pangramExpected, pangramActual);

        String taMarbutah = "ﺓ";
        String taMarbutahExpected = "";
        String taMarbutahActual = LanguageUtils.transliterate(taMarbutah);
        assertEquals("ta marbutah transliteration failed", taMarbutahExpected, taMarbutahActual);

        String hamza = "ءأؤإئآ";
        String hamzaExpected = "222222";
        String hamzaActual = LanguageUtils.transliterate(hamza);
        assertEquals("hamza transliteration failed", hamzaExpected, hamzaActual);

        String easternArabicNumeralsArabic = "٠١٢٣٤٥٦٧٨٩";
        String easternArabicNumeralsFarsi = "۰۱۲۳۴۵۶۷۸۹";
        String easternArabicNumeralsExpected = "0123456789";
        assertEquals("Eastern Arabic numerals (Arabic) failed", easternArabicNumeralsExpected,
                LanguageUtils.transliterate(easternArabicNumeralsArabic));
        assertEquals("Eastern Arabic numerals (Farsi) failed", easternArabicNumeralsExpected,
                LanguageUtils.transliterate(easternArabicNumeralsFarsi));

        String farsi = "گچپژ";
        String farsiExpected = "gchpzh";
        String farsiActual = LanguageUtils.transliterate(farsi);
        assertEquals("Farsi transiteration failed", farsiExpected, farsiActual);
    }

    @Test
    public void testStringTransliterateBengali() throws Exception {
        // input with cyrillic and diacritic letters
        String[] inputs = { "অনিরুদ্ধ", "বিজ্ঞানযাত্রা চলছে চলবে।", "আমি সব দেখেশুনে ক্ষেপে গিয়ে করি বাঙলায় চিৎকার!",
                "আমার জাভা কোড is so bad! কী আর বলবো!" };
        String[] outputs = { "oniruddho", "biggaanJaatraa cholchhe cholbe.",
                "aami sob dekheshune kkhepe giye kori baanglaay chitkaar!",
                "aamaar jaabhaa koD is so bad! kii aar bolbo!"};

        String result;

        for (int i = 0; i < inputs.length; i++) {
            result = LanguageUtils.transliterate(inputs[i]);
            assertEquals("Transliteration failed", outputs[i], result);
        }
    }

    @Test
    public void testStringTransliterateKorean() {
        // A familiar phrase with no special provisions.
        String hello = "안녕하세요";
        String helloExpected = "annyeonghaseyo";
        String helloActual = LanguageUtils.transliterate(hello);
        assertEquals("Korean hello transliteration failed", helloExpected, helloActual);

        // Korean pangram. Includes some ASCII punctuation which should not be changed by
        // transliteration.
        //
        // Translation: "Chocolate!? What I wanted was some rice puffs and clothes." "Child, why are
        // you complaining again?"
        String pangram = "\"웬 초콜릿? 제가 원했던 건 뻥튀기 쬐끔과 의류예요.\" \"얘야, 왜 또 불평?\"";
        String pangramExpected = "\"wen chokollit? jega wonhaetdeon geon ppeongtwigi jjoekkeumgwa uiryuyeyo.\" \"yaeya, wae tto bulpyeong?\"";
        String pangramActual = LanguageUtils.transliterate(pangram);
        assertEquals("Korean pangram transliteration failed", pangramExpected, pangramActual);

        // Several words excercising special provisions, from Wikipedia.
        String special = "좋고, 놓다, 잡혀, 낳지";
        String specialExpected = "joko, nota, japhyeo, nachi";
        String specialActual = LanguageUtils.transliterate(special);
        assertEquals("Korean special provisions transliteration failed", specialExpected, specialActual);

        // Isolated jamo.
        String isolatedJamo = "ㅋㅋㅋ";
        String isolatedJamoExpected = "kkk";
        String isolatedJamoActual = LanguageUtils.transliterate(isolatedJamo);
        assertEquals("Korean isolated jamo transliteration failed", isolatedJamoExpected, isolatedJamoActual);

        // Korean transliteration shouldn't touch non-Hangul composites.
        String german = "schön";
        String germanExpected = german;
        String germanActual = KoreanLanguageUtils.transliterate(german);
        assertEquals("Korean transliteration modified a non-Hangul composite", germanExpected, germanActual);
    }

    @Test
    public void testStringTransliterateLithuanian() {
        String input = "ą č ę ė į š ų ū ž";
        String output = LanguageUtils.transliterate(input);
        String expected = "a c e e i s u u z";
        assertEquals("lithuanian translation failed", expected, output);

        input = "aąa cčc eęe eėe iįi sšs uųu uūu zžz";
        output = LanguageUtils.transliterate(input);
        expected = "aaa ccc eee eee iii sss uuu uuu zzz";
        assertEquals("lithuanian translation failed", expected, output);
    }

    @Test
    public void testTransliterateOption() throws Exception {
        setDefaultTransliteration();
        assertFalse("Transliteration option fail! Expected 'Off' by default, but result is 'On'",
                LanguageUtils.transliterate());

        enableTransliteration(true);
        assertTrue("Transliteration option fail! Expected 'On', but result is 'Off'", LanguageUtils.transliterate());
    }

    private void setDefaultTransliteration() {
        SharedPreferences settings = GBApplication.getPrefs().getPreferences();
        SharedPreferences.Editor editor = settings.edit();
        editor.remove("transliteration");
        editor.apply();
    }

    private void enableTransliteration(boolean enable) {
        SharedPreferences settings = GBApplication.getPrefs().getPreferences();
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("transliteration", enable);
        editor.apply();
    }
}
