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

import java.util.Calendar;
import java.util.TimeZone;

import nodomain.freeyourgadget.gadgetbridge.devices.lefun.LefunConstants;
import nodomain.freeyourgadget.gadgetbridge.devices.lefun.commands.BaseCommand;
import nodomain.freeyourgadget.gadgetbridge.devices.lefun.commands.TimeCommand;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.lefun.LefunDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.operations.OperationStatus;

public class SetTimeRequest extends Request {
    public SetTimeRequest(LefunDeviceSupport support, TransactionBuilder builder) {
        super(support, builder);
    }

    @Override
    public byte[] createRequest() {
        TimeCommand cmd = new TimeCommand();
        Calendar c = Calendar.getInstance();

        cmd.setOp(BaseCommand.OP_SET);
        cmd.setYear((byte)(c.get(Calendar.YEAR) - 2000));
        cmd.setMonth((byte)(c.get(Calendar.MONTH) + 1));
        cmd.setDay((byte)c.get(Calendar.DAY_OF_MONTH));
        cmd.setHour((byte)c.get(Calendar.HOUR_OF_DAY));
        cmd.setMinute((byte)c.get(Calendar.MINUTE));
        cmd.setSecond((byte)c.get(Calendar.SECOND));

        return cmd.serialize();
    }

    @Override
    public void handleResponse(byte[] data) {
        TimeCommand cmd = new TimeCommand();
        cmd.deserialize(data);
        if (cmd.getOp() == BaseCommand.OP_SET && !cmd.isSetSuccess())
            reportFailure("Could not set time");

        operationStatus = OperationStatus.FINISHED;
    }

    @Override
    public int getCommandId() {
        return LefunConstants.CMD_TIME;
    }
}
