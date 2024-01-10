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
package nodomain.freeyourgadget.gadgetbridge.util;

import java.util.HashMap;
import java.util.Map;

public final class MapUtils {
    /**
     * Reverses a map. If there are multiple values for the same key, the first one will take precedence.
     *
     * @param map the map to reverse
     * @param <V> the type for the values
     * @param <K> the type for the keys
     * @return the reversed map
     */
    public static <V, K> Map<V, K> reverse(final Map<K, V> map) {
        final Map<V, K> reversed = new HashMap<>();

        for (final Map.Entry<K, V> entry : map.entrySet()) {
            if (!reversed.containsKey(entry.getValue())) {
                reversed.put(entry.getValue(), entry.getKey());
            }
        }

        return reversed;
    }
}
