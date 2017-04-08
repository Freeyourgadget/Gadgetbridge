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
        //input with cyrillic and diacritic letters
        String input = "בדיקה עברית";
        String output = LanguageUtils.transliterate(input);
        String result = "bdykh 'bryth";

        assertEquals("Transliteration failed", result, output);
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
