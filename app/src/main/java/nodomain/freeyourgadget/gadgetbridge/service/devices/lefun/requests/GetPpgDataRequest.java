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
import nodomain.freeyourgadget.gadgetbridge.devices.lefun.commands.GetPpgDataCommand;
import nodomain.freeyourgadget.gadgetbridge.service.devices.lefun.LefunDeviceSupport;

public class GetPpgDataRequest extends MultiFetchRequest {
    private int ppgType;

    public GetPpgDataRequest(LefunDeviceSupport support) {
        super(support);
    }

    public int getPpgType() {
        return ppgType;
    }

    public void setPpgType(int ppgType) {
        this.ppgType = ppgType;
    }

    @Override
    public byte[] createRequest() {
        GetPpgDataCommand cmd = new GetPpgDataCommand();
        cmd.setPpgType(ppgType);
        return cmd.serialize();
    }

    @Override
    public void handleResponse(byte[] data) {
        GetPpgDataCommand cmd = new GetPpgDataCommand();
        cmd.deserialize(data);

        if (cmd.getPpgType() != ppgType) {
            throw new IllegalArgumentException("Mismatching PPG type");
        }

        if (totalRecords == -1) {
            totalRecords = cmd.getTotalRecords() & 0xffff;
        } else if (totalRecords != (cmd.getTotalRecords() & 0xffff)) {
            throw new IllegalArgumentException("Total records mismatch");
        }

        if (totalRecords != 0) {
            int currentRecord = cmd.getCurrentRecord() & 0xffff;
            if (lastRecord + 1 != currentRecord) {
                throw new IllegalArgumentException("Records received out of sequence");
            }
            lastRecord = currentRecord;

            getSupport().handlePpgData(cmd);
        } else {
            lastRecord = totalRecords;
        }

        if (lastRecord == totalRecords)
            operationFinished();
    }

    @Override
    public int getCommandId() {
        return LefunConstants.CMD_PPG_DATA;
    }

    @Override
    protected String getOperationName() {
        return "Getting PPG data";
    }
}
