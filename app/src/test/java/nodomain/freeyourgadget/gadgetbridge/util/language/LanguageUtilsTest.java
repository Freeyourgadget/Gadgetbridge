package nodomain.freeyourgadget.gadgetbridge.util.language;

import android.content.SharedPreferences;

import org.junit.Test;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.test.TestBase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_TRANSLITERATION_LANGUAGES;

/**
 * Tests LanguageUtils
 */
public class LanguageUtilsTest extends TestBase {

    private GBDevice dummyGBDevice;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        dummyGBDevice = createDummyGDevice("00:00:00:00:10");
    }


    @Test
    public void testStringTransliterateCyrillic() throws Exception {
        final Transliterator transliterator = LanguageUtils.getTransliterator("russian");

        // input with cyrillic and diacritic letters
        String input = "Прõсто текčт";
        String output = transliterator.transliterate(input);
        String result = "Prosto tekct";

        assertEquals("Transliteration failed", result, output);
    }

    @Test
    public void testStringTransliterateHebrew() throws Exception {
        final Transliterator transliterator = LanguageUtils.getTransliterator("hebrew");

        String input = "בדיקה עברית";
        String output = transliterator.transliterate(input);
        String result = "bdykh 'bryth";

        assertEquals("Transliteration failed", result, output);
    }

    @Test
    public void testStringTransliterateArabic() {
        final Transliterator transliterator = LanguageUtils.getTransliterator("arabic");

        String pangram = "نص حكيم له سر قاطع وذو شأن عظيم مكتوب على ثوب أخضر ومغلف بجلد أزرق";
        String pangramExpected = "n9 7kym lh sr qa63 wthw sh2n 36'ym mktwb 3la thwb 259'r wm3'lf bjld 2zrq";
        String pangramActual = transliterator.transliterate(pangram);
        assertEquals("Arabic pangram transliteration failed", pangramExpected, pangramActual);

        String taMarbutah = "ﺓ";
        String taMarbutahExpected = "";
        String taMarbutahActual = transliterator.transliterate(taMarbutah);
        assertEquals("ta marbutah transliteration failed", taMarbutahExpected, taMarbutahActual);

        String hamza = "ءأؤإئآ";
        String hamzaExpected = "222222";
        String hamzaActual = transliterator.transliterate(hamza);
        assertEquals("hamza transliteration failed", hamzaExpected, hamzaActual);

        String easternArabicNumeralsArabic = "٠١٢٣٤٥٦٧٨٩";
        String easternArabicNumeralsExpected = "0123456789";
        assertEquals("Eastern Arabic numerals (Arabic) failed", easternArabicNumeralsExpected,
                transliterator.transliterate(easternArabicNumeralsArabic));
    }

    public void testStringTransliteratePersian() {
        final Transliterator transliterator = LanguageUtils.getTransliterator("persian");

        String farsi = "گچپژ";
        String farsiExpected = "gchpzh";
        String farsiActual = transliterator.transliterate(farsi);
        assertEquals("Farsi transiteration failed", farsiExpected, farsiActual);

        String easternArabicNumeralsFarsi = "۰۱۲۳۴۵۶۷۸۹";
        String easternArabicNumeralsExpected = "0123456789";

        assertEquals("Eastern Arabic numerals (Farsi) failed", easternArabicNumeralsExpected,
                transliterator.transliterate(easternArabicNumeralsFarsi));
    }

    @Test
    public void testStringTransliterateBengali() throws Exception {
        final Transliterator transliterator = LanguageUtils.getTransliterator("bengali");

        // input with cyrillic and diacritic letters
        String[] inputs = { "অনিরুদ্ধ", "বিজ্ঞানযাত্রা চলছে চলবে।", "আমি সব দেখেশুনে ক্ষেপে গিয়ে করি বাঙলায় চিৎকার!",
                "আমার জাভা কোড is so bad! কী আর বলবো!" };
        String[] outputs = { "oniruddho", "biggaanJaatraa cholchhe cholbe.",
                "aami sob dekheshune kkhepe giye kori baanglaay chitkaar!",
                "aamaar jaabhaa koD is so bad! kii aar bolbo!"};

        String result;

        for (int i = 0; i < inputs.length; i++) {
            result = transliterator.transliterate(inputs[i]);
            assertEquals("Transliteration failed", outputs[i], result);
        }
    }

    @Test
    public void testStringTransliterateKorean() {
        final Transliterator transliterator = LanguageUtils.getTransliterator("korean");

        // A familiar phrase with no special provisions.
        String hello = "안녕하세요";
        String helloExpected = "annyeonghaseyo";
        String helloActual = transliterator.transliterate(hello);
        assertEquals("Korean hello transliteration failed", helloExpected, helloActual);

        // Korean pangram. Includes some ASCII punctuation which should not be changed by
        // transliteration.
        //
        // Translation: "Chocolate!? What I wanted was some rice puffs and clothes." "Child, why are
        // you complaining again?"
        String pangram = "\"웬 초콜릿? 제가 원했던 건 뻥튀기 쬐끔과 의류예요.\" \"얘야, 왜 또 불평?\"";
        String pangramExpected = "\"wen chokollit? jega wonhaetdeon geon ppeongtwigi jjoekkeumgwa uiryuyeyo.\" \"yaeya, wae tto bulpyeong?\"";
        String pangramActual = transliterator.transliterate(pangram);
        assertEquals("Korean pangram transliteration failed", pangramExpected, pangramActual);

        // Several words excercising special provisions, from Wikipedia.
        String special = "좋고, 놓다, 잡혀, 낳지";
        String specialExpected = "joko, nota, japhyeo, nachi";
        String specialActual = transliterator.transliterate(special);
        assertEquals("Korean special provisions transliteration failed", specialExpected, specialActual);

        // Isolated jamo.
        String isolatedJamo = "ㅋㅋㅋ";
        String isolatedJamoExpected = "kkk";
        String isolatedJamoActual = transliterator.transliterate(isolatedJamo);
        assertEquals("Korean isolated jamo transliteration failed", isolatedJamoExpected, isolatedJamoActual);

        // Korean transliteration shouldn't touch non-Hangul composites.
        String german = "schön";
        String germanExpected = german;
        String germanActual = transliterator.transliterate(german);
        assertEquals("Korean transliteration modified a non-Hangul composite", germanExpected, germanActual);
    }

    @Test
    public void testStringTransliterateLithuanian() {
        final Transliterator transliterator = LanguageUtils.getTransliterator("lithuanian");

        String input = "ą č ę ė į š ų ū ž";
        String output = transliterator.transliterate(input);
        String expected = "a c e e i s u u z";
        assertEquals("lithuanian translation failed", expected, output);

        input = "aąa cčc eęe eėe iįi sšs uųu uūu zžz";
        output = transliterator.transliterate(input);
        expected = "aaa ccc eee eee iii sss uuu uuu zzz";
        assertEquals("lithuanian translation failed", expected, output);
    }

    @Test
    public void testTransliterateOption() throws Exception {
        enableTransliteration(false);
        assertNull("Transliteration option fail! Expected 'Off' by default, but result is 'On'",
                getTransliteration());

        enableTransliteration(true);
        assertNotNull("Transliteration option fail! Expected 'On', but result is 'Off'", getTransliteration());
    }

    private void enableTransliteration(boolean enable) {
        SharedPreferences devicePrefs = GBApplication.getDeviceSpecificSharedPrefs(dummyGBDevice.getAddress());
        SharedPreferences.Editor editor = devicePrefs.edit();
        if (enable) {
            editor.putString(PREF_TRANSLITERATION_LANGUAGES, "extended_ascii,scandinavian,german,russian,hebrew,greek,ukranian,arabic,persian,lithuanian,polish,estonian,icelandic,czech,turkish,bengali,korean");
        } else {
            editor.remove(PREF_TRANSLITERATION_LANGUAGES);
        }
        editor.apply();
    }

    private Transliterator getTransliteration() {
        return LanguageUtils.getTransliterator(dummyGBDevice);
    }
}
