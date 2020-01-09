/*  Copyright (C) 2019-2020 Daniel Dakhno

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit;


import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.Request;

public class RequestHandControlRequest extends Request {
    public RequestHandControlRequest(byte priority, boolean moveCompleteNotify, boolean controlLostNOtify){
        super();
        init(priority, moveCompleteNotify, controlLostNOtify);
    }

    public RequestHandControlRequest(){
        super();
        init((byte)1, false, false);
    }

    private void init(byte priority, boolean moveCompleteNotify, boolean controlLostNOtify) {
        ByteBuffer buffer = createBuffer();
        buffer.put(priority);
        buffer.put(moveCompleteNotify ? (byte)1 : 0);
        buffer.put(controlLostNOtify ? (byte)1 : 0);
        this.data = buffer.array();
    }

    @Override
    public byte[] getStartSequence() {
        return new byte[]{2, 21, 1};
    }

    @Override
    public int getPayloadLength() {
        return 6;
    }
}
