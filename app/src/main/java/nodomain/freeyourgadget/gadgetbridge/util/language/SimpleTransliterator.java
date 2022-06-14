/*  Copyright (C) 2022 José Rebelo

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

import org.apache.commons.lang3.text.WordUtils;

import java.text.Normalizer;
import java.util.Map;

public class SimpleTransliterator implements Transliterator {
    private final Map<Character, String> transliterateMap;

    public SimpleTransliterator(final Map<Character, String> transliterateMap) {
        this.transliterateMap = transliterateMap;
    }

    @Override
    public String transliterate(String txt) {
        if (txt == null || txt.isEmpty()) {
            return txt;
        }

        final StringBuilder messageBuilder = new StringBuilder();

        // Simple, char-by-char transliteration.
        final char[] chars = txt.toCharArray();
        for (char c : chars) {
            messageBuilder.append(transliterate(c));
        }
        final String message = messageBuilder.toString();

        return message;
    }

    private String transliterate(char c) {
        final char lowerChar = Character.toLowerCase(c);

        if (transliterateMap.containsKey(lowerChar)) {
            final String replace = transliterateMap.get(lowerChar);

            if (lowerChar != c) {
                return WordUtils.capitalize(replace);
            }

            return replace;
        }

        return String.valueOf(c);
    }
}
