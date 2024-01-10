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

public enum VibrationKind implements AbstractNotificationPattern {
    NONE(0, R.string.prefs_wena3_vibration_none),
    CONTINUOUS(1, R.string.prefs_wena3_vibration_continuous),
    BASIC(2, R.string.prefs_wena3_vibration_basic),
    RAPID(3, R.string.prefs_wena3_vibration_rapid),
    TRIPLE(4, R.string.prefs_wena3_vibration_triple),
    STEP_UP(5, R.string.prefs_wena3_vibration_step_up),
    STEP_DOWN(6, R.string.prefs_wena3_vibration_step_down),
    WARNING(7, R.string.prefs_wena3_vibration_warning),
    SIREN(8, R.string.prefs_wena3_vibration_siren),
    SHORT(9, R.string.prefs_wena3_vibration_short);

    public final byte value;
    private final int stringId;

    VibrationKind(int value, int stringId) {
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

