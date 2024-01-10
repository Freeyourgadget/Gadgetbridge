/*  Copyright (C) 2022-2024 Arjan Schrijver, José Rebelo, Petr Kadlec

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.util.language.impl;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.HashMap;

import nodomain.freeyourgadget.gadgetbridge.util.language.SimpleTransliterator;
import nodomain.freeyourgadget.gadgetbridge.util.language.Transliterator;

public class FlattenToAsciiTransliterator implements Transliterator {
    @Override
    public String transliterate(String txt) {
        if (txt == null || txt.isEmpty()) {
            return txt;
        }

        // Decompose the string into its compatible decomposition (splits base characters from accents/marks, and changes some characters to compatibility version)
        txt = Normalizer.normalize(txt, Normalizer.Form.NFKD);
        // Remove all marks (characters intended to be combined with another character), keeping the base glyphs
        txt = txt.replaceAll("\\p{M}", "");
        // Flatten the resulting string to ASCII
        return new String(txt.getBytes(StandardCharsets.US_ASCII), StandardCharsets.US_ASCII);
    }
}
