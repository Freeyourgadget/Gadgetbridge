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
import nodomain.freeyourgadget.gadgetbridge.devices.lefun.commands.ProfileCommand;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.lefun.LefunDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.operations.OperationStatus;

public class SetProfileRequest extends Request {
    private ActivityUser user;

    public SetProfileRequest(LefunDeviceSupport support, TransactionBuilder builder) {
        super(support, builder);
    }

    public ActivityUser getUser() {
        return user;
    }

    public void setUser(ActivityUser user) {
        this.user = user;
    }

    @Override
    public byte[] createRequest() {
        ProfileCommand cmd = new ProfileCommand();

        cmd.setOp(BaseCommand.OP_SET);
        // No "other" option available, only male or female
        cmd.setGender(user.getGender() == ActivityUser.GENDER_FEMALE
                ? ProfileCommand.GENDER_FEMALE
                : ProfileCommand.GENDER_MALE);
        cmd.setHeight((byte) user.getHeightCm());
        cmd.setWeight((byte) user.getWeightKg());
        cmd.setAge((byte) user.getAge());

        return cmd.serialize();
    }

    @Override
    public void handleResponse(byte[] data) {
        ProfileCommand cmd = new ProfileCommand();
        cmd.deserialize(data);
        if (cmd.getOp() == BaseCommand.OP_SET && !cmd.isSetSuccess())
            reportFailure("Could not set profile");

        operationStatus = OperationStatus.FINISHED;
    }

    @Override
    public int getCommandId() {
        return LefunConstants.CMD_PROFILE;
    }
}
