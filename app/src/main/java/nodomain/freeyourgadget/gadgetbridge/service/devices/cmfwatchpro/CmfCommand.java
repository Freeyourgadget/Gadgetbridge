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
package nodomain.freeyourgadget.gadgetbridge.service.devices.cmfwatchpro;

import androidx.annotation.Nullable;

public enum CmfCommand {
    ACTIVITY_DATA(0x0056, 0x0001),
    ACTIVITY_FETCH_1(0xffff, 0x8005),
    ACTIVITY_FETCH_2(0xffff, 0x9057),
    ACTIVITY_FETCH_ACK_1(0xffff, 0x0005),
    ACTIVITY_FETCH_ACK_2(0xffff, 0xa057),
    ALARMS_GET(0x0063, 0x0002),
    ALARMS_SET(0x0063, 0x0001),
    APP_NOTIFICATION(0x0065, 0x0001),
    AUTH_NONCE_REPLY(0xffff, 0x004c),
    AUTH_NONCE_REQUEST(0xffff, 0x804b),
    AUTH_PAIR_REPLY(0xffff, 0x0048),
    AUTH_PAIR_REQUEST(0xffff, 0x8047),
    AUTH_PHONE_NAME(0xffff, 0x8049),
    AUTH_WATCH_MAC(0xffff, 0x0049),
    AUTHENTICATED_CONFIRM_REPLY(0xffff, 0x0004),
    AUTHENTICATED_CONFIRM_REQUEST(0xffff, 0x804d),
    BATTERY(0x005c, 0x0001),
    CALL_REMINDER(0xffff, 0x9066),
    CONTACTS_GET(0x00d5, 0x0002),
    CONTACTS_SET(0x00d5, 0x0001),
    DATA_CHUNK_REQUEST_AGPS(0xffff, 0xa05f),
    DATA_CHUNK_REQUEST_WATCHFACE(0xffff, 0xa064),
    DATA_CHUNK_WRITE_AGPS(0xffff, 0x905f),
    DATA_CHUNK_WRITE_WATCHFACE(0xffff, 0x9064),
    DATA_TRANSFER_AGPS_FINISH_ACK_1(0xffff, 0xa060),
    DATA_TRANSFER_AGPS_FINISH_ACK_2(0xffff, 0x9060),
    DATA_TRANSFER_AGPS_INIT_REPLY(0xffff, 0xa05e),
    DATA_TRANSFER_AGPS_INIT_REQUEST(0xffff, 0x905e),
    DATA_TRANSFER_WATCHFACE_FINISH_ACK_1(0xffff, 0xa065),
    DATA_TRANSFER_WATCHFACE_FINISH_ACK_2(0xffff, 0x9065),
    DATA_TRANSFER_WATCHFACE_INIT_1_REQUEST(0xffff, 0x8052),
    DATA_TRANSFER_WATCHFACE_INIT_1_REPLY(0xffff, 0x0052),
    DATA_TRANSFER_WATCHFACE_INIT_2_REPLY(0xffff, 0xa063),
    DATA_TRANSFER_WATCHFACE_INIT_2_REQUEST(0xffff, 0x9063),
    DO_NOT_DISTURB(0x0099, 0x0001),
    FACTORY_RESET(0x009a, 0x0001),
    FIND_PHONE(0x005b, 0x0001),
    FIND_WATCH(0x005d, 0x0001),
    FIRMWARE_VERSION_GET(0xffff, 0x8006),
    FIRMWARE_VERSION_RET(0xffff, 0x0006),
    GOALS_SET(0x005e, 0x0001),
    GPS_COORDS(0xffff, 0x906a),
    HEART_MONITORING_ALERTS(0xffff, 0x9059),
    HEART_MONITORING_ENABLED_GET(0x009b, 0x0002),
    HEART_MONITORING_ENABLED_SET(0x009b, 0x0001),
    HEART_RATE_RESTING(0x00da, 0x0001),
    HEART_RATE_MANUAL_AUTO(0x0053, 0x0001),
    HEART_RATE_WORKOUT(0x00e0, 0x0001),
    LANGUAGE_RET(0xffff, 0xa06b),
    LANGUAGE_SET(0xffff, 0x9058),
    MUSIC_BUTTON(0xffff, 0xa05d),
    MUSIC_INFO_ACK(0xffff, 0xa05c),
    MUSIC_INFO_SET(0xffff, 0x905c),
    SERIAL_NUMBER_GET(0x00de, 0x0002),
    SERIAL_NUMBER_RET(0x00de, 0x0001),
    SLEEP_DATA(0x0058, 0x0001),
    SPO2(0x0055, 0x0001),
    SPORTS_SET(0x00dc, 0x0001),
    STANDING_REMINDER_GET(0x0060, 0x0002),
    STANDING_REMINDER_SET(0x0060, 0x0001),
    STRESS(0x009d, 0x0001),
    TIME_FORMAT(0x005f, 0x0001),
    TIME(0xffff, 0x8004),
    TRIGGER_SYNC(0x005c, 0x0002),
    UNIT_LENGTH(0xffff, 0x9067),
    UNIT_TEMPERATURE(0xffff, 0x9068),
    WAKE_ON_WRIST_RAISE(0x0062, 0x0001),
    WATCHFACE(0x009f, 0x0001),
    WATER_REMINDER_GET(0x0061, 0x0002),
    WATER_REMINDER_SET(0x0061, 0x0001),
    WEATHER_SET_1(0xffff, 0x906b),
    WEATHER_SET_2(0x0066, 0x0001),
    WORKOUT_GPS(0xffff, 0xa05a),
    WORKOUT_SUMMARY(0x0057, 0x0001),
    ;

    private final int cmd1;
    private final int cmd2;

    CmfCommand(final int cmd1, final int cmd2) {
        this.cmd1 = cmd1;
        this.cmd2 = cmd2;
    }

    public int getCmd1() {
        return cmd1;
    }

    public int getCmd2() {
        return cmd2;
    }

    @Nullable
    public static CmfCommand fromCodes(final int cmd1, final int cmd2) {
        for (final CmfCommand cmd : CmfCommand.values()) {
            if (cmd.getCmd1() == cmd1 && cmd.getCmd2() == cmd2) {
                return cmd;
            }
        }

        return null;
    }
}
