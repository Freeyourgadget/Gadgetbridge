/*  Copyright (C) 2019-2021 Arjan Schrijver

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.quickreply;

import java.util.UUID;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.FossilRequest;


public class QuickReplyConfirmationPutRequest extends FossilRequest {
    /**
     * Contains the bytes to confirm to the watch that a quick reply SMS has been sent.
     * @param callId
     */
    public QuickReplyConfirmationPutRequest(byte callId) {
        this.data = new byte[]{
                (byte) 0x02,
                (byte) 0x04,
                callId,
                (byte) 0x00,
                (byte) 0x00,
                (byte) 0x00,
                (byte) 0x03,
                (byte) 0x00
        };
    }

    @Override
    public boolean isFinished() {
        return true;
    }

    @Override
    public byte[] getStartSequence() {
        return null;
    }

    @Override
    public UUID getRequestUUID() {
        return UUID.fromString("3dda0006-957f-7d4a-34a6-74696673696d");
    }
}