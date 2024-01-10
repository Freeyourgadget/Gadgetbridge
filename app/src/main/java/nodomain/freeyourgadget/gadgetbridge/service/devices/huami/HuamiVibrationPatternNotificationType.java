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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami;

/**
 * The notification types for which vibration patterns are customizable. If these change, the
 * constants in {@link nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst} need to be updated.
 */
public enum HuamiVibrationPatternNotificationType {
    APP_ALERTS(0x00),
    INCOMING_CALL(0x01),
    INCOMING_SMS(0x02),
    GOAL_NOTIFICATION(0x04),
    ALARM(0x05),
    IDLE_ALERTS(0x06),
    EVENT_REMINDER(0x08),
    FIND_BAND(0x09),
    TODO_LIST(0x0a),
    SCHEDULE(0x0c),
    ;

    private final byte code;

    HuamiVibrationPatternNotificationType(final int code) {
        this.code = (byte) code;
    }

    public byte getCode() {
        return code;
    }
}
