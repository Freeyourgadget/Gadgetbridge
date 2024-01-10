/*  Copyright (C) 2023-2024 Frank Ertl

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.message;

import nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.datastructures.WithingsStructure;

public class WithingsMessage extends AbstractMessage {
    private short type;
    private ExpectedResponse expectedResponse = ExpectedResponse.SIMPLE;
    private boolean isIncoming;

    public WithingsMessage(short type) {
        this.type = type;
    }

    public WithingsMessage(short type, boolean incoming) {
        this.type = type;
        this.isIncoming = incoming;
    }

    public WithingsMessage(short type, ExpectedResponse expectedResponse) {
        this.type = type;
        this.expectedResponse = expectedResponse;
    }

    public WithingsMessage(short type, WithingsStructure dataStructure) {
        this.type = type;
        this.addDataStructure(dataStructure);
    }

    @Override
    public boolean needsResponse() {
        return expectedResponse == ExpectedResponse.SIMPLE;
    }

    @Override
    public boolean needsEOT() {
        return expectedResponse == ExpectedResponse.EOT;
    }

    @Override
    public short getType() {
        return type;
    }

    @Override
    public boolean isIncomingMessage() {
        return isIncoming;
    }
}
