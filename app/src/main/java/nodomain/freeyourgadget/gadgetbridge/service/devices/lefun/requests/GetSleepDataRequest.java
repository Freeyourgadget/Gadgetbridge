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
import nodomain.freeyourgadget.gadgetbridge.devices.lefun.commands.GetSleepDataCommand;
import nodomain.freeyourgadget.gadgetbridge.service.devices.lefun.LefunDeviceSupport;

public class GetSleepDataRequest extends MultiFetchRequest {
    private int daysAgo;

    public GetSleepDataRequest(LefunDeviceSupport support) {
        super(support);
    }

    public int getDaysAgo() {
        return daysAgo;
    }

    public void setDaysAgo(int daysAgo) {
        this.daysAgo = daysAgo;
    }

    @Override
    public byte[] createRequest() {
        GetSleepDataCommand cmd = new GetSleepDataCommand();
        cmd.setDaysAgo((byte) daysAgo);
        return cmd.serialize();
    }

    @Override
    public void handleResponse(byte[] data) {
        GetSleepDataCommand cmd = new GetSleepDataCommand();
        cmd.deserialize(data);

        if (daysAgo != (cmd.getDaysAgo() & 0xff)) {
            throw new IllegalArgumentException("Mismatching days ago");
        }

        if (totalRecords == -1) {
            totalRecords = cmd.getTotalRecords() & 0xff;
        } else if (totalRecords != (cmd.getTotalRecords() & 0xff)) {
            throw new IllegalArgumentException("Total records mismatch");
        }

        if (totalRecords != 0) {
            int currentRecord = cmd.getCurrentRecord() & 0xff;
            if (lastRecord + 1 != currentRecord) {
                throw new IllegalArgumentException("Records received out of sequence");
            }
            lastRecord = currentRecord;

            getSupport().handleSleepData(cmd);
        } else {
            lastRecord = totalRecords;
        }

        if (lastRecord == totalRecords)
            operationFinished();
    }

    @Override
    public int getCommandId() {
        return LefunConstants.CMD_SLEEP_DATA;
    }

    @Override
    protected String getOperationName() {
        return "Getting sleep data";
    }
}
