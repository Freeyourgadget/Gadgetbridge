/*  Copyright (C) 2020-2023 Petr Kadlec

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.ancs;

import android.util.SparseArray;

public enum AncsAttribute {
    APP_IDENTIFIER(0),
    TITLE(1, true),
    SUBTITLE(2, true),
    MESSAGE(3, true),
    MESSAGE_SIZE(4),
    DATE(5),
    POSITIVE_ACTION_LABEL(6),
    NEGATIVE_ACTION_LABEL(7),
    // Garmin extensions
    PHONE_NUMBER(126, true),
    ACTIONS(127, false, true);

    private static final SparseArray<AncsAttribute> valueByCode;

    public final int code;
    public final boolean hasLengthParam;
    public final boolean hasAdditionalParams;

    AncsAttribute(int code) {
        this(code, false, false);
    }

    AncsAttribute(int code, boolean hasLengthParam) {
        this(code, hasLengthParam, false);
    }

    AncsAttribute(int code, boolean hasLengthParam, boolean hasAdditionalParams) {
        this.code = code;
        this.hasLengthParam = hasLengthParam;
        this.hasAdditionalParams = hasAdditionalParams;
    }

    static {
        final AncsAttribute[] values = values();
        valueByCode = new SparseArray<>(values.length);
        for (AncsAttribute value : values) {
            valueByCode.append(value.code, value);
        }
    }

    public static AncsAttribute getByCode(int code) {
        return valueByCode.get(code);
    }
}
