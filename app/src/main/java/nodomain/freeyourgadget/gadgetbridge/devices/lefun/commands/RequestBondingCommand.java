/*  Copyright (C) 2017-2021 Andreas Shimokawa, Yukai Li

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
package nodomain.freeyourgadget.gadgetbridge.devices.lefun.commands;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.devices.lefun.LefunConstants;

public class RequestBondingCommand extends BaseCommand {
    public static final byte STATUS_ALREADY_BONDED = 0;
    public static final byte STATUS_BONDING_SUCCESSFUL = 1;

    private byte status;

    public byte getStatus() {
        return status;
    }

    @Override
    protected void deserializeParams(byte id, ByteBuffer params) {
        validateIdAndLength(id, params, LefunConstants.CMD_BONDING_REQUEST, 1);

        status = params.get();
    }

    @Override
    protected byte serializeParams(ByteBuffer params) {
        return LefunConstants.CMD_BONDING_REQUEST;
    }
}
