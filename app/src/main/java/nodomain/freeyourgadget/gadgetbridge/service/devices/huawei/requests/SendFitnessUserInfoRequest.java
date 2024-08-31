/*  Copyright (C) 2024 Vitalii Tomin

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.FitnessData;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.FitnessData.UserInfo;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SendFitnessUserInfoRequest extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(SendFitnessUserInfoRequest.class);

    public SendFitnessUserInfoRequest(HuaweiSupportProvider support) {
        super(support);
        this.serviceId = FitnessData.id;
        this.commandId = UserInfo.id;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            // Hardcoded values till interface for goal
            ActivityUser activityUser = new ActivityUser();
            byte gender = 3;
            if (activityUser.getGender() == ActivityUser.GENDER_FEMALE) {
                gender = 2;
            } else if (activityUser.getGender() == ActivityUser.GENDER_MALE) {
                gender = 1;
            }

            LocalDate dateOfBirth = activityUser.getDateOfBirth();
            int birthdayEncoded = dateOfBirth.getYear() << 16;
            birthdayEncoded += dateOfBirth.getMonthValue() << 8;
            birthdayEncoded += dateOfBirth.getDayOfMonth();

            return new UserInfo.Request(paramsProvider,
                    activityUser.getHeightCm(),
                    activityUser.getWeightKg(),
                    activityUser.getAge(),
                    birthdayEncoded,
                    gender
            ).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() {
        LOG.debug("handle Send Fitness UserInfo Request");
    }
}
