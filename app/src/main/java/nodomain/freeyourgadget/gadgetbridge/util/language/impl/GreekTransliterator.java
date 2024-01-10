/*  Copyright (C) 2022-2024 José Rebelo

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

import java.util.HashMap;

import nodomain.freeyourgadget.gadgetbridge.util.language.SimpleTransliterator;

public class GreekTransliterator extends SimpleTransliterator {
    public GreekTransliterator() {
        super(new HashMap<Character, String>() {{
            put('α',"a");put('ά',"a");put('β',"v");put('γ',"g");put('δ',"d");put('ε',"e");put('έ',"e");put('ζ',"z");put('η',"i");
            put('ή',"i");put('θ',"th");put('ι',"i");put('ί',"i");put('ϊ',"i");put('ΐ',"i");put('κ',"k");put('λ',"l");put('μ',"m");
            put('ν',"n");put('ξ',"ks");put('ο',"o");put('ό',"o");put('π',"p");put('ρ',"r");put('σ',"s");put('ς',"s");put('τ',"t");
            put('υ',"y");put('ύ',"y");put('ϋ',"y");put('ΰ',"y");put('φ',"f");put('χ',"ch");put('ψ',"ps");put('ω',"o");put('ώ',"o");
            put('Α',"A");put('Ά',"A");put('Β',"B");put('Γ',"G");put('Δ',"D");put('Ε',"E");put('Έ',"E");put('Ζ',"Z");put('Η',"I");
            put('Ή',"I");put('Θ',"TH");put('Ι',"I");put('Ί',"I");put('Ϊ',"I");put('Κ',"K");put('Λ',"L");put('Μ',"M");put('Ν',"N");
            put('Ξ',"KS");put('Ο',"O");put('Ό',"O");put('Π',"P");put('Ρ',"R");put('Σ',"S");put('Τ',"T");put('Υ',"Y");put('Ύ',"Y");
            put('Ϋ',"Y");put('Φ',"F");put('Χ',"CH");put('Ψ',"PS");put('Ω',"O");put('Ώ',"O");
        }});
    }
}
