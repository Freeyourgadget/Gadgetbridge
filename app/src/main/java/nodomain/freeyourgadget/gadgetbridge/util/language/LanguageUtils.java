/*  Copyright (C) 2017-2022 Andreas Shimokawa, Aniruddha Adhikary, Daniele
    Gobbetti, ivanovlev, kalaee, lazarosfs, McSym28, M. Hadi, Roi Greenberg,
    Taavi Eomäe, Ted Stein, Thomas, Yaron Shahrabani, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.util.language;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_TRANSLITERATION_LANGUAGES;

import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import nodomain.freeyourgadget.gadgetbridge.util.language.impl.ArabicTransliterator;
import nodomain.freeyourgadget.gadgetbridge.util.language.impl.BengaliTransliterator;
import nodomain.freeyourgadget.gadgetbridge.util.language.impl.CzechTransliterator;
import nodomain.freeyourgadget.gadgetbridge.util.language.impl.EstonianTransliterator;
import nodomain.freeyourgadget.gadgetbridge.util.language.impl.ExtendedAsciiTransliterator;
import nodomain.freeyourgadget.gadgetbridge.util.language.impl.FlattenToAsciiTransliterator;
import nodomain.freeyourgadget.gadgetbridge.util.language.impl.GermanTransliterator;
import nodomain.freeyourgadget.gadgetbridge.util.language.impl.GreekTransliterator;
import nodomain.freeyourgadget.gadgetbridge.util.language.impl.HebrewTransliterator;
import nodomain.freeyourgadget.gadgetbridge.util.language.impl.IcelandicTransliterator;
import nodomain.freeyourgadget.gadgetbridge.util.language.impl.KoreanTransliterator;
import nodomain.freeyourgadget.gadgetbridge.util.language.impl.LithuanianTransliterator;
import nodomain.freeyourgadget.gadgetbridge.util.language.impl.PersianTransliterator;
import nodomain.freeyourgadget.gadgetbridge.util.language.impl.PolishTransliterator;
import nodomain.freeyourgadget.gadgetbridge.util.language.impl.RussianTransliterator;
import nodomain.freeyourgadget.gadgetbridge.util.language.impl.ScandinavianTransliterator;
import nodomain.freeyourgadget.gadgetbridge.util.language.impl.TurkishTransliterator;
import nodomain.freeyourgadget.gadgetbridge.util.language.impl.UkranianTransliterator;

public class LanguageUtils {
    private static final Logger LOG = LoggerFactory.getLogger(LanguageUtils.class);

    private static final Map<String, Transliterator> TRANSLITERATORS_MAP = new HashMap<String, Transliterator>() {{
        put("arabic", new ArabicTransliterator());
        put("bengali", new BengaliTransliterator());
        put("czech", new CzechTransliterator());
        put("estonian", new EstonianTransliterator());
        put("extended_ascii", new ExtendedAsciiTransliterator());
        put("german", new GermanTransliterator());
        put("greek", new GreekTransliterator());
        put("hebrew", new HebrewTransliterator());
        put("icelandic", new IcelandicTransliterator());
        put("korean", new KoreanTransliterator());
        put("lithuanian", new LithuanianTransliterator());
        put("persian", new PersianTransliterator());
        put("polish", new PolishTransliterator());
        put("russian", new RussianTransliterator());
        put("scandinavian", new ScandinavianTransliterator());
        put("turkish", new TurkishTransliterator());
        put("ukranian", new UkranianTransliterator());
    }};

    /**
     * Get a {@link Transliterator} for a specific language.
     *
     * @param language the language
     * @return the transliterator, if it exists
     * @throws IllegalArgumentException if there is no transliterator for the provided language
     */
    public static Transliterator getTransliterator(final String language) {
        if (!TRANSLITERATORS_MAP.containsKey(language)) {
            throw new IllegalArgumentException(String.format("Transliterator for %s not found", language));
        }

        return TRANSLITERATORS_MAP.get(language);
    }

    /**
     * Get the configured transliterator for the provided {@link GBDevice}, if any.
     *
     * @param device the device
     * @return the configured transliterator, null if not configured
     */
    @Nullable
    public static Transliterator getTransliterator(final GBDevice device) {
        final Prefs devicePrefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(device.getAddress()));
        final String transliterateLanguagesPref = devicePrefs.getString(PREF_TRANSLITERATION_LANGUAGES, "");

        if (transliterateLanguagesPref.isEmpty()) {
            return null;
        }

        final List<String> languages = Arrays.asList(transliterateLanguagesPref.split(","));
        final List<Transliterator> transliterators = new ArrayList<>(languages.size());

        for (String language : languages) {
            if (!TRANSLITERATORS_MAP.containsKey(language)) {
                LOG.warn("Transliterator for {} not found", language);
                continue;
            }

            transliterators.add(TRANSLITERATORS_MAP.get(language));
        }

        transliterators.add(new FlattenToAsciiTransliterator());

        return new MultiTransliterator(transliterators);
    }
}
