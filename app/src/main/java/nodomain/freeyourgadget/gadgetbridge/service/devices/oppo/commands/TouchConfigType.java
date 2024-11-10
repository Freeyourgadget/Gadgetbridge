/*  Copyright (C) 2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.oppo.commands;

import androidx.annotation.Nullable;

public enum TouchConfigType {
    UNK_1(0x0101),
    TAP_2(0x0201),
    TAP_3(0x0301),
    HOLD(0x0401),
    ;

    private final int code;

    TouchConfigType(final int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    @Nullable
    public static TouchConfigType fromCode(final int code) {
        for (final TouchConfigType param : TouchConfigType.values()) {
            if (param.code == code) {
                return param;
            }
        }

        return null;
    }
}
