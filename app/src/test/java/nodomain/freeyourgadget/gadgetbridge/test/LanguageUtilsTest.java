package nodomain.freeyourgadget.gadgetbridge.test;

import android.content.SharedPreferences;

import org.junit.Test;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.util.LanguageUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests LanguageUtils
 */
public class LanguageUtilsTest extends TestBase {
    @Test
    public void testStringTransliterateCyrillic() throws Exception {
        //input with cyrillic and diacritic letters
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
        assertEquals("pangram transliteration failed", pangramExpected, pangramActual);

        String taMarbutah = "ﺓ";
        String taMarbutahExpected = "";
        String taMarbutahActual = LanguageUtils.transliterate(taMarbutah);
        assertEquals("ta marbutah transliteration failed", taMarbutahExpected, taMarbutahActual);

        String hamza = "ءأؤإئآ";
        String hamzaExpected = "222222";
        String hamzaActual = LanguageUtils.transliterate(hamza);
        assertEquals("hamza transliteration failed", hamzaExpected, hamzaActual);

        String farsi = "پچڜڤڥڨگݣ";
        String farsiExpected = "pchchvvggg";
        String farsiActual = LanguageUtils.transliterate(farsi);
        assertEquals("Farsi transiteration failed", farsiExpected, farsiActual);
    }

    public void testStringTransliterateBengali() throws Exception {
        //input with cyrillic and diacritic letters
        String input = "অনিরুদ্ধ";
        String output = LanguageUtils.transliterate(input);
        String result = "oniruddho";

        assertEquals("Transliteration failed", result, output);
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
        assertFalse("Transliteration option fail! Expected 'Off' by default, but result is 'On'", LanguageUtils.transliterate());

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
