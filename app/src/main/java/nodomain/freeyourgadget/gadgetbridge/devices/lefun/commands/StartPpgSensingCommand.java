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

public class StartPpgSensingCommand extends BaseCommand {
    private byte ppgType;

    private boolean setSuccess;

    public int getPpgType() {
        return getLowestSetBitIndex(ppgType);
    }

    public void setPpgType(int type) {
        if (type < 0 || type > 2)
            throw new IllegalArgumentException("Invalid PPG type");
        this.ppgType = (byte)(1 << type);
    }

    public boolean isSetSuccess() {
        return setSuccess;
    }

    @Override
    protected void deserializeParams(byte id, ByteBuffer params) {
        validateIdAndLength(id, params, LefunConstants.CMD_PPG_START, 2);

        ppgType = params.get();
        setSuccess = params.get() == 1;
    }

    @Override
    protected byte serializeParams(ByteBuffer params) {
        params.put(ppgType);
        return LefunConstants.CMD_PPG_START;
    }
}
