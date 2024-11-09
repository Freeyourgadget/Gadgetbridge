package nodomain.freeyourgadget.gadgetbridge.test;

import junit.framework.TestCase;

import org.apache.commons.lang3.text.WordUtils;
import org.junit.Test;
import org.junit.Assert;

import java.util.LinkedHashMap;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.util.language.impl.ArmenianTransliterator;

public class ArmenianTransliteratorTest extends TestCase {
    @Test
    public void testSimpleCases() {
        Assert.assertEquals("aybuben", new ArmenianTransliterator().transliterate("այբուբեն"));
        Assert.assertEquals("vorotan", new ArmenianTransliterator().transliterate("որոտան"));
        Assert.assertEquals("voroshel", new ArmenianTransliterator().transliterate("որոշել"));
        Assert.assertEquals("uzox", new ArmenianTransliterator().transliterate("ուզող"));
        Assert.assertEquals(
                "AVO", new ArmenianTransliterator().transliterate("ԱՈ")
        );
    }

    @Test
    public void testMultipleWords() {
        Assert.assertEquals("vory karucum en Viqipedia kayqic ogtvoxnery azat xmbagrman dzevachapov",
                new ArmenianTransliterator().transliterate("որը կառուցում են Վիքիպեդիա կայքից օգտվողները ազատ խմբագրման ձևաչափով"));
    }

    @Test
    public void testMixedStrings() {
        Assert.assertEquals("vor1voshel 12 uzox", new ArmenianTransliterator().transliterate("որ1ոշել 12 ուզող"));
        Assert.assertEquals("vory jet iridescent karucum en sheen Viqipedia kayqic ogtvoxnery and a distinctive azat xmbagrman dzevachapov",
                new ArmenianTransliterator().transliterate("որը jet iridescent կառուցում են sheen Վիքիպեդիա կայքից օգտվողները and a distinctive ազատ խմբագրման ձևաչափով"));
    }

    @Test
    public void testMixedCaseWords() {
        Assert.assertEquals(
                "Inchpes", new ArmenianTransliterator().transliterate("Ինչպես")
        );
        Assert.assertEquals(
                "VOrՕSHEL", new ArmenianTransliterator().transliterate("ՈրՈՇԵԼ")
        );
        Assert.assertEquals(
                "Ushadir", new ArmenianTransliterator().transliterate("Ուշադիր")
        );
        Assert.assertEquals(
                "AU", new ArmenianTransliterator().transliterate("ԱՈւ")
        );
    }

    @Test
    public void testTop100Words() {
        final Map<String,String> topWords = new LinkedHashMap<String,String>() {{
            put("ինչպես", "inchpes");
            put("ես", "es");
            put("նրա", "nra");
            put("որ", "vor");
            put("նա", "na");
            put("էր", "er");
            put("համար", "hamar");
            put("ին", "in");
            put("հետ", "het");
            put("նրանք", "nranq");
            put("լինել", "linel");
            put("մեկ", "mek");
            put("ունենալ", "unenal");
            put("այս", "ays");
            put("ից", "ic");
            put("ի", "i");
            put("տաք", "taq");
            put("բառ", "bar");
            put("բայց", "bayc");
            put("ինչ", "inch");
            put("մի", "mi");
            put("քանի", "qani");
            put("է", "e");
            put("այն", "ayn");
            put("դուք", "duq");
            put("կամ", "kam");
            put("եւ", "ev");
            put("մինչեւ", "minchev");
            put("իսկ", "isk");
            put("ա", "a");
            put("մենք", "menq");
            put("կարող", "karox");
            put("այլ", "ayl");
            put("են", "en");
            put("որը", "vory");
            put("անել", "anel");
            put("իրենց", "irenc");
            put("ժամանակ", "jamanak");
            put("եթե", "ete");
            put("կամք", "kamq");
            put("յուրաքանչյուր", "yuraqanchyur");
            put("ասել", "asel");
            put("շարք", "sharq");
            put("երեք", "ereq");
            put("ուզում", "uzum");
            put("օդի", "odi");
            put("լավ", "lav");
            put("նույնպես", "nuynpes");
            put("խաղալ", "xaxal");
            put("փոքր", "poqr");
            put("վերջ", "verj");
            put("կարդալ", "kardal");
            put("ձեռք", "dzerq");
            put("նավահանգիստ", "navahangist");
            put("տառ", "tar");
            put("առ", "ar");
            put("ավելացնել", "avelacnel");
            put("նույնիսկ", "nuynisk");
            put("այստեղ", "aystex");
            put("պետք", "petq");
            put("մեծ", "mec");
            put("բարձր", "bardzr");
            put("այդպիսի", "aydpisi");
            put("հետեւել", "hetevel");
            put("գործ", "gorc");
            put("ինչու", "inchu");
            put("խնդրել", "xndrel");
            put("տղամարդիկ", "txamardik");
            put("փոփոխություն", "popoxutyun");
            put("գնաց", "gnac");
            put("լույս", "luys");
            put("բարի", "bari");
            put("դուրս", "durs");
            put("անհրաժեշտ", "anhrajesht");
            put("տուն", "tun");
            put("նկար", "nkar");
            put("փորձել", "pordzel");
            put("մեզ", "mez");
            put("կրկին", "krkin");
            put("կենդանի", "kendani");
            put("կետ", "ket");
            put("մայր", "mayr");
            put("աշխարհ", "ashxarh");
            put("մոտ", "mot");
            put("կառուցել", "karucel");
            put("ինքնուրույն", "inqnuruyn");
            put("երկիր", "erkir");
            put("հայր", "hayr");
            put("ցանկացած", "cankacac");
            put("նոր", "nor");
            put("աշխատանք", "ashxatanq");
            put("մաս", "mas");
            put("վերցնել", "vercnel");
            put("ստանալ", "stanal");
            put("տեղ", "tex");
            put("ապրել", "aprel");
            put("որտեղ", "vortex");
            put("երբ", "erb");
            put("Վերադառնալ", "Veradarnal");
            put("միայն", "miayn");
        }};

        for (final Map.Entry<String,String> entry : topWords.entrySet()) {
            Assert.assertEquals(entry.getValue(), new ArmenianTransliterator().transliterate(entry.getKey()));
        }

        for (final Map.Entry<String,String> entry : topWords.entrySet()) {
            Assert.assertEquals(WordUtils.capitalize(entry.getValue()), WordUtils.capitalize(new ArmenianTransliterator().transliterate(entry.getKey())));
        }
    }
}