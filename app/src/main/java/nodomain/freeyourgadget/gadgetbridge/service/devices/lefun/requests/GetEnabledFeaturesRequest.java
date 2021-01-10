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
import nodomain.freeyourgadget.gadgetbridge.devices.lefun.commands.BaseCommand;
import nodomain.freeyourgadget.gadgetbridge.devices.lefun.commands.FeaturesCommand;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.lefun.LefunDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.operations.OperationStatus;

public class GetEnabledFeaturesRequest extends Request {
    public GetEnabledFeaturesRequest(LefunDeviceSupport support, TransactionBuilder builder) {
        super(support, builder);
    }

    @Override
    public byte[] createRequest() {
        FeaturesCommand cmd = new FeaturesCommand();

        cmd.setOp(BaseCommand.OP_GET);

        return cmd.serialize();
    }

    @Override
    public void handleResponse(byte[] data) {
        FeaturesCommand cmd = new FeaturesCommand();
        cmd.deserialize(data);
        if (cmd.getOp() == BaseCommand.OP_GET) {
            getSupport().receiveEnabledFeaturesSetting(cmd);
        }

        operationStatus = OperationStatus.FINISHED;
    }

    @Override
    public int getCommandId() {
        return LefunConstants.CMD_FEATURES;
    }
}
