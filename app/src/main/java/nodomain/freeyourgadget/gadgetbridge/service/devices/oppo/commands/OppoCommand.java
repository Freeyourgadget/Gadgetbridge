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

public enum OppoCommand {
    BATTERY_REQ(0x0106),
    BATTERY_RET(0x8106),
    DEVICE_INFO(0x0204),
    FIRMWARE_GET(0x0105),
    FIRMWARE_RET(0x8105),
    TOUCH_CONFIG_REQ(0x0108),
    TOUCH_CONFIG_SET(0x0401),
    TOUCH_CONFIG_RET(0x8108),
    TOUCH_CONFIG_ACK(0x8401),
    FIND_DEVICE_REQ(0x0400),
    FIND_DEVICE_ACK(0x8400),
    ;

    private final short code;

    OppoCommand(final int code) {
        this.code = (short) code;
    }

    public short getCode() {
        return code;
    }

    @Nullable
    public static OppoCommand fromCode(final short code) {
        for (final OppoCommand cmd : OppoCommand.values()) {
            if (cmd.code == code) {
                return cmd;
            }
        }

        return null;
    }
}
