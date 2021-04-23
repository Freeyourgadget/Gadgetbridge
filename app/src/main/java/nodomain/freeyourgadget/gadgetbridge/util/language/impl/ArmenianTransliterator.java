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
import java.util.List;
import java.util.Map;

public class ArmenianTransliterator implements Transliterator {
    // Transliteration map ordered by priority
    // Armenian has some rules regarding reading of 'ո' in the middle of the word it reads as english O
    // But if word starts with it's read as sound of 'vo'
    // Or if it has 'ւ' symbol after it, then we should read it as 'u' (as double o in booze)
    private static final Map<String, String> transliterateMap = new LinkedHashMap<String, String>() {
        {
            // Letter + 'ու'
            put("աու","au");
            put("բու","bu");
            put("գու","gu");
            put("դու","du");
            put("եու","eu");
            put("զու","zu");
            put("էու","eu");
            put("ըու","yu");
            put("թու","tu");
            put("ժու","ju");
            put("իու","iu");
            put("լու","lu");
            put("խու","xu");
            put("ծու","cu");
            put("կու","ku");
            put("հու","hu");
            put("ձու","dzu");
            put("ղու","xu");
            put("ճու","cu");
            put("մու","mu");
            put("յու","yu");
            put("նու","nu");
            put("շու","shu");
            put("չու","chu");
            put("պու","pu");
            put("ջու","ju");
            put("ռու","ru");
            put("սու","su");
            put("վու","vu");
            put("տու","tu");
            put("րու","ru");
            put("ցու","cu");
            put("փու","pu");
            put("քու","qu");
            put("օու","ou");
            put("ևու","eu");
            put("ֆու","fu");
            put("ոու","vou");

            put("ու","u");

            // Letter + 'ո'
            put("բո","bo");
            put("գո","go");
            put("դո","do");
            put("զո","zo");
            put("թո","to");
            put("ժո","jo");
            put("լո","lo");
            put("խո","xo");
            put("ծո","co");
            put("կո","ko");
            put("հո","ho");
            put("ձո","dzo");
            put("ղո","xo");
            put("ճո","co");
            put("մո","mo");
            put("յո","yo");
            put("նո","no");
            put("շո","so");
            put("չո","co");
            put("պո","po");
            put("ջո","jo");
            put("ռո","ro");
            put("սո","so");
            put("վո","vo");
            put("տո","to");
            put("րո","ro");
            put("ցո","co");
            put("փո","po");
            put("քո","qo");
            put("ևո","eo");
            put("ֆո","fo");
            put("ո","vo");

            // Two different ways to write, we support all.
            put("եւ","ev");
            put("եվ","ev");

            // Simple substitutions
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

            // If this symbol wasn't used in the combination with others, then it's meaningless
            put("ւ","");

            // Add support for capitilazed words
            for (final Map.Entry<String,String> entry : ((Map<String, String>)this.clone()).entrySet()) {
                final String capitalKey = WordUtils.capitalize(entry.getKey());
                if(!capitalKey.equals(entry.getKey())) {
                    put(capitalKey, WordUtils.capitalize(entry.getValue()));
                }
            }

        }};

    private static final Map<String, Integer> transliterationPriorityMap = new HashMap<String, Integer>() {{
        int priority = 0;
        for( final String key : transliterateMap.keySet() ) {
            put(key, priority++);
        }
    }};

    // Aho-Corasick trie
    private static final Trie transliterationTrie;
    static {
        final Trie.TrieBuilder builder = Trie.builder();
        for( final String key :  ArmenianTransliterator.transliterateMap.keySet()) {
            builder.addKeyword(key);
        }
        transliterationTrie = builder.build();
    }

    private static String ahoCorasick(final String text) {
        // Create a buffer sufficiently large that re-allocations are minimized.
        final StringBuilder sb = new StringBuilder( text.length() * 10 / 12 );

        // The complexity of the Aho-Corasick algorithm O(N + L + Z)
        // Where N is the length of the text, L is the length of keywords and the Z is a number of matches.
        // This algorithm allows us to do fast substring search
        final List<Emit> emits = new ArrayList<Emit>(transliterationTrie.parseText( text ));

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

        for( final Emit emit : emits ) {
            final int matchIndex = emit.getStart();

            // Skip if we already substituted this part
            if(matchIndex < prevIndex) {
                continue;
            }

            // Add part which shouldn't be substituted
            sb.append(text.substring(prevIndex, matchIndex));

            // Substitute and append to the builder
            sb.append( ArmenianTransliterator.transliterateMap.get( emit.getKeyword() ) );

            prevIndex = emit.getEnd() + 1;
        }

        // Add the remainder of the string (contains no more matches).
        sb.append( text.substring( prevIndex ) );

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