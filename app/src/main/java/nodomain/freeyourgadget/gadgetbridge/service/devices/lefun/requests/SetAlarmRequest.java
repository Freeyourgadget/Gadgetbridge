/*  Copyright (C) 2020-2021 Yukai Li

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.lefun.requests;

import nodomain.freeyourgadget.gadgetbridge.devices.lefun.LefunConstants;
import nodomain.freeyourgadget.gadgetbridge.devices.lefun.commands.AlarmCommand;
import nodomain.freeyourgadget.gadgetbridge.devices.lefun.commands.BaseCommand;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.lefun.LefunDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.operations.OperationStatus;

public class SetAlarmRequest extends Request {
    private int index;
    private boolean enabled;
    private int dayOfWeek;
    private int hour;
    private int minute;
    public SetAlarmRequest(LefunDeviceSupport support, TransactionBuilder builder) {
        super(support, builder);
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(int dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public int getMinute() {
        return minute;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    @Override
    public byte[] createRequest() {
        AlarmCommand cmd = new AlarmCommand();
        cmd.setOp(BaseCommand.OP_SET);
        cmd.setIndex((byte) index);
        cmd.setEnabled(enabled);
        cmd.setNumOfSnoozes((byte) 0);
        cmd.setHour((byte) hour);
        cmd.setMinute((byte) minute);

        // Translate GB alarm day of week to Lefun day of week
        // GB starts on Monday, Lefun starts on Sunday
        for (int i = 0; i < 6; ++i) {
            if ((dayOfWeek & (1 << i)) != 0) {
                cmd.setDayOfWeek(i + 1, true);
            }
        }
        if ((dayOfWeek & Alarm.ALARM_SUN) != 0) {
            cmd.setDayOfWeek(AlarmCommand.DOW_SUNDAY, true);
        }

        return cmd.serialize();
    }

    @Override
    public void handleResponse(byte[] data) {
        AlarmCommand cmd = new AlarmCommand();
        cmd.deserialize(data);

        if (cmd.getOp() != BaseCommand.OP_SET || cmd.getIndex() != index || !cmd.isSetSuccess())
            reportFailure("Could not set alarm");

        operationStatus = OperationStatus.FINISHED;
    }

    @Override
    public int getCommandId() {
        return LefunConstants.CMD_ALARM;
    }
}
