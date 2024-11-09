/*  Copyright (C) 2021-2024 Alik Aslanyan

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
package nodomain.freeyourgadget.gadgetbridge.util.language.impl;
import nodomain.freeyourgadget.gadgetbridge.util.language.Transliterator;

import org.ahocorasick.trie.Emit;
import org.ahocorasick.trie.Trie;
import org.apache.commons.lang3.text.WordUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ArmenianTransliterator implements Transliterator {
    // Transliteration map ordered by priority
    // Armenian has some rules regarding reading of 'ո' in the middle of the word it reads as english O
    // But if word starts with it's read as sound of 'vo'
    // Or if it has 'ւ' symbol after it, then we should read it as 'u' (as double o in booze)
    private static final Map<String, String> transliterateMap = new LinkedHashMap<String, String>() {
        {
            // Simple substitutions
            Map<String, String> simpleSubstitions = new HashMap<String, String>() {
                {
                    put("ա","a");
                    put("բ","b");
                    put("գ","g");
                    put("դ","d");
                    put("ե","e");
                    put("զ","z");
                    put("է","e");
                    put("ը","y");
                    put("թ","t");
                    put("ժ","j");
                    put("ի","i");
                    put("լ","l");
                    put("խ","x");
                    put("ծ","c");
                    put("կ","k");
                    put("հ","h");
                    put("ձ","dz");
                    put("ղ","x");
                    put("ճ","c");
                    put("մ","m");
                    put("յ","y");
                    put("ն","n");
                    put("շ","sh");
                    put("ո", "vo");
                    put("չ","ch");
                    put("պ","p");
                    put("ջ","j");
                    put("ռ","r");
                    put("ս","s");
                    put("վ","v");
                    put("տ","t");
                    put("ր","r");
                    put("ց","c");
                    put("փ","p");
                    put("ք","q");
                    put("օ","o");
                    put("և","ev");
                    put("ֆ","f");
                    put("՝", "`");
                    put("՞", "?");
                    put("։", ":");
                    put("․", ".");
                }
            };

            // Capitalize existing simple substitutions here
            for (final Entry<String, String> entry : new ArrayList<Entry<String, String>>(simpleSubstitions.entrySet())) {
                String capitalKey = entry.getKey().toUpperCase();
                if (!capitalKey.equals(entry.getKey())) {
                    simpleSubstitions.put(capitalKey, entry.getValue().toUpperCase());
                }
            }

            // Letter + 'ու'
            final String[] letterMapU = {
                "ա",
                "բ",
                "գ",
                "դ",
                "ե",
                "զ",
                "է",
                "ը",
                "թ",
                "ժ",
                "ի",
                "լ",
                "խ",
                "ծ",
                "կ",
                "հ",
                "ձ",
                "ղ",
                "ճ",
                "մ",
                "յ",
                "ն",
                "շ",
                "չ",
                "պ",
                "ջ",
                "ռ",
                "ս",
                "վ",
                "տ",
                "ր",
                "ց",
                "փ",
                "ք",
                "օ",
                "և",
                "ֆ",
                "ո"
            };

            for (final String letter : letterMapU) {
                final String capitalLetter = letter.toUpperCase();
                final String transliteratedLetter = Objects.requireNonNull(simpleSubstitions.get(letter), letter);
                final String transliteratedCapitalLetter = Objects.requireNonNull(simpleSubstitions.get(capitalLetter), capitalLetter);

                put(letter + "ու", transliteratedLetter + "u");
                put(capitalLetter + "ու", transliteratedCapitalLetter + "u");

                put(letter + "ՈՒ", transliteratedLetter + "U");
                put(capitalLetter + "ՈՒ", transliteratedCapitalLetter + "U");
                put(letter + "Ու", transliteratedLetter + "U");
                put(capitalLetter + "Ու", transliteratedCapitalLetter + "U");

                put(letter + "ոՒ", transliteratedLetter + "U");
                put(capitalLetter + "ոՒ", transliteratedCapitalLetter + "U");
            }

            put("ու","u");
            put("Ու","U");
            put("ոՒ","U");
            put("ՈՒ","U");

            // Letter + 'ո'
            final String[] letterMapVo = {
                "բ",
                "գ",
                "դ",
                "զ",
                "թ",
                "ժ",
                "լ",
                "խ",
                "ծ",
                "կ",
                "հ",
                "ձ",
                "ղ",
                "ճ",
                "մ",
                "յ",
                "ն",
                "շ",
                "ո", // ո + ո should be voo
                "չ",
                "պ",
                "ջ",
                "ռ",
                "ս",
                "վ",
                "տ",
                "ր",
                "ց",
                "փ",
                "ք",
                "և",
                "ֆ"
            };

            for (String letter : letterMapVo) {
                String capitalLetter = letter.toUpperCase();
                final String transliteratedLetter = Objects.requireNonNull(simpleSubstitions.get(letter));
                final String transliteratedCapitalLetter = Objects.requireNonNull(simpleSubstitions.get(capitalLetter));

                put(letter + "ո", transliteratedLetter + "o");
                put(capitalLetter + "ո", transliteratedCapitalLetter + "o");

                put(letter + "Ո", transliteratedLetter + "Օ");
                put(capitalLetter + "Ո", transliteratedCapitalLetter + "Օ");
            }

            put("ո","vo");
            put("Ո","VO");

            // Two different ways to write, we support all.
            put("եւ","ev");
            put("եվ","ev");
            put("Եւ","Ev");
            put("Եվ","Ev");
            put("ԵՒ","EV");
            put("ԵՎ","EV");

            // If this symbol wasn't used in the combination with others, then it's meaningless
            put("ւ","");
            put("Ւ","");

            // Simple substitutions have last priority
            for (final Map.Entry<String,String> entry : simpleSubstitions.entrySet()) {
                put(entry.getKey(), entry.getValue());
                put(entry.getKey().toUpperCase(), entry.getValue().toUpperCase());
            }
        }};

    private static final Map<String, Integer> transliterationPriorityMap = new HashMap<String, Integer>() {{
        int priority = 0;
        for (final String key : transliterateMap.keySet()) {
            put(key, priority++);
        }
    }};

    // Aho-Corasick trie
    private static final Trie transliterationTrie;
    static {
        final Trie.TrieBuilder builder = Trie.builder();
        for (final String key :  ArmenianTransliterator.transliterateMap.keySet()) {
            builder.addKeyword(key);
        }
        transliterationTrie = builder.build();
    }

    private static String ahoCorasick(final String text) {
        // Create a buffer sufficiently large that re-allocations are minimized.
        final StringBuilder sb = new StringBuilder(text.length() * 10 / 12);

        // The complexity of the Aho-Corasick algorithm O(N + L + Z)
        // Where N is the length of the text, L is the length of keywords and the Z is a number of matches.
        // This algorithm allows us to do fast substring search
        final List<Emit> emits = new ArrayList<Emit>(transliterationTrie.parseText(text));

        // Sort collection first by starting position, then by priority.
        Collections.sort(emits, new Comparator<Emit>() {
            @Override
            public int compare(Emit a, Emit b) {
                int cmp = Integer.compare(a.getStart(), b.getStart());
                if (cmp != 0) {
                    return cmp;
                }

                int priorityA = transliterationPriorityMap.get(a.getKeyword());
                int priorityB = transliterationPriorityMap.get(b.getKeyword());
                return Integer.compare(priorityA, priorityB);
            }
        });

        int prevIndex = 0;

        for (final Emit emit : emits) {
            final int matchIndex = emit.getStart();

            // Skip if we already substituted this part
            if (matchIndex < prevIndex) {
                continue;
            }

            // Add part which shouldn't be substituted
            sb.append(text.substring(prevIndex, matchIndex));

            // Substitute and append to the builder
            sb.append(Objects.requireNonNull(ArmenianTransliterator.transliterateMap.get(emit.getKeyword())));

            prevIndex = emit.getEnd() + 1;
        }

        // Add the remainder of the string (contains no more matches).
        sb.append(text.substring(prevIndex));

        return sb.toString();
    }

    @Override
    public String transliterate(String txt) {
        if (txt == null || txt.isEmpty()) {
            return txt;
        }

        return ahoCorasick(txt);
    }
}