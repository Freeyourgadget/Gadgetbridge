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

public enum AncsCommand {
    GET_NOTIFICATION_ATTRIBUTES(0),
    GET_APP_ATTRIBUTES(1),
    PERFORM_NOTIFICATION_ACTION(2),
    // Garmin extensions
    PERFORM_ANDROID_ACTION(128);

    private static final SparseArray<AncsCommand> valueByCode;

    public final int code;

    AncsCommand(int code) {
        this.code = code;
    }

    static {
        final AncsCommand[] values = values();
        valueByCode = new SparseArray<>(values.length);
        for (AncsCommand value : values) {
            valueByCode.append(value.code, value);
        }
    }

    public static AncsCommand getByCode(int code) {
        return valueByCode.get(code);
    }
}
