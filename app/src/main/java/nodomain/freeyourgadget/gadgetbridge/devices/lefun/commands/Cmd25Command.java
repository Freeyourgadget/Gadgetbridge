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
package nodomain.freeyourgadget.gadgetbridge.devices.lefun.commands;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.devices.lefun.LefunConstants;

public class Cmd25Command extends BaseCommand {
    private byte op;
    private int unknown;

    private boolean setSuccess;

    public byte getOp() {
        return op;
    }

    public void setOp(byte op) {
        if (op != OP_GET && op != OP_SET)
            throw new IllegalArgumentException("Operation must be get or set");
        this.op = op;
    }

    public int getUnknown() {
        return unknown;
    }

    public void setUnknown(int unknown) {
        if (unknown < 0 || unknown > 65000)
            throw new IllegalArgumentException("Value must be between 0 and 65000 inclusive");
        this.unknown = unknown;
    }

    public boolean isSetSuccess() {
        return setSuccess;
    }

    @Override
    protected void deserializeParams(byte id, ByteBuffer params) {
        validateId(id, LefunConstants.CMD_UNKNOWN_25);

        int paramsLength = params.limit() - params.position();
        if (paramsLength < 1)
            throwUnexpectedLength();

        op = params.get();
        if (op == OP_GET) {
            if (paramsLength != 5)
                throwUnexpectedLength();

            unknown = params.getInt();
        } else if (op == OP_SET) {
            if (paramsLength != 2)
                throwUnexpectedLength();

            setSuccess = params.get() == 1;
        } else {
            throw new IllegalArgumentException("Invalid operation type received");
        }
    }

    @Override
    protected byte serializeParams(ByteBuffer params) {
        params.put(op);
        if (op == OP_SET) {
            params.putInt(unknown);
        }
        return LefunConstants.CMD_UNKNOWN_25;
    }
}
