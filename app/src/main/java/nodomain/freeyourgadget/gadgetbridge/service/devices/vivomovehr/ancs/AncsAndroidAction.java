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

public enum AncsAndroidAction {
    REPLY_TEXT_MESSAGE(94),
    REPLY_INCOMING_CALL(95),
    ACCEPT_INCOMING_CALL(96),
    REJECT_INCOMING_CALL(97),
    DISMISS_NOTIFICATION(98),
    BLOCK_APPLICATION(99);

    private static final SparseArray<AncsAndroidAction> valueByCode;

    public final int code;

    AncsAndroidAction(int code) {
        this.code = code;
    }

    static {
        final AncsAndroidAction[] values = values();
        valueByCode = new SparseArray<>(values.length);
        for (AncsAndroidAction value : values) {
            valueByCode.append(value.code, value);
        }
    }

    public static AncsAndroidAction getByCode(int code) {
        return valueByCode.get(code);
    }
}
