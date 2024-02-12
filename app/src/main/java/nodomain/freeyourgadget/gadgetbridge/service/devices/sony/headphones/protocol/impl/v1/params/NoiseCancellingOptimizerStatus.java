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
package nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol.impl.v1.params;

import android.content.Context;

import java.util.Locale;

public enum NoiseCancellingOptimizerStatus {
    NOT_RUNNING(0x00),
    WEARING_CONDITION(0x01),
    ATMOSPHERIC_PRESSURE(0x02),
    ANALYZING(0x10),
    FINISHED(0x11),
    ;

    private final byte code;

    NoiseCancellingOptimizerStatus(final int code) {
        this.code = (byte) code;
    }

    public byte getCode() {
        return this.code;
    }

    public String i18n(final Context context) {
        final String stringName = String.format("sony_anc_optimizer_status_%s", name().toLowerCase(Locale.ROOT));
        final int stringId = context.getResources().getIdentifier(stringName, "string", context.getPackageName());
        return context.getString(stringId);
    }

    public static NoiseCancellingOptimizerStatus fromCode(final byte code) {
        for (final NoiseCancellingOptimizerStatus audioCodec : values()) {
            if (audioCodec.code == code) {
                return audioCodec;
            }
        }

        return null;
    }
}
