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

public enum VibrationCount implements AbstractNotificationPattern {
    INDEFINITE(0, R.string.prefs_wena3_notification_vibration_repetition_0),
    ONCE(1, R.string.prefs_wena3_notification_vibration_repetition_1),
    TWICE(2, R.string.prefs_wena3_notification_vibration_repetition_2),
    THREE(3, R.string.prefs_wena3_notification_vibration_repetition_3),
    FOUR(4, R.string.prefs_wena3_notification_vibration_repetition_4),
    ;

    public int value;
    public int stringId;
    VibrationCount(int value, int stringId) {
        this.value = value;
        this.stringId = stringId;
    }


    @Override
    public String getUserReadableName(Context context) {
        return context.getString(stringId);
    }

    @Override
    public String getValue() {
        return String.valueOf(value);
    }
}
