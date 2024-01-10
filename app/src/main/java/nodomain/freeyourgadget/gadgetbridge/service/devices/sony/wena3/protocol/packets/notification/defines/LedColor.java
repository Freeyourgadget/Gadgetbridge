/*  Copyright (C) 2023-2024 akasaka / Genjitsu Labs

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.notification.defines;

import android.content.Context;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.model.AbstractNotificationPattern;

public enum LedColor implements AbstractNotificationPattern {
    NONE(0, R.string.prefs_wena3_led_none),
    RED(1, R.string.red),
    YELLOW(2, R.string.yellow),
    GREEN(3, R.string.green),
    CYAN(4, R.string.cyan),
    BLUE(5, R.string.blue),
    PURPLE(6, R.string.purple),
    WHITE(7, R.string.white);

    public final byte value;
    private final int stringId;

    LedColor(int value, int stringId) {
        this.value = (byte) value;
        this.stringId = stringId;
    }


    @Override
    public String getUserReadableName(Context context) {
        return context.getString(stringId);
    }

    @Override
    public String getValue() {
        return name();
    }
}

