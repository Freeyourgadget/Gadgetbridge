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

public class PpgResultCommand extends BaseCommand {
    private byte ppgType;
    private byte[] ppgData;

    public int getPpgType() {
        return getLowestSetBitIndex(ppgType);
    }

    public byte[] getPpgData() {
        return ppgData;
    }

    @Override
    protected void deserializeParams(byte id, ByteBuffer params) {
        validateId(id, LefunConstants.CMD_PPG_RESULT);

        int paramsLength = params.limit() - params.position();
        if (paramsLength < 1)
            throwUnexpectedLength();

        ppgType = params.get();

        int typeIndex = getPpgType();
        int dataLength;
        switch (typeIndex) {
            case LefunConstants.PPG_TYPE_HEART_RATE:
            case LefunConstants.PPG_TYPE_BLOOD_OXYGEN:
                dataLength = 1;
                break;
            case LefunConstants.PPG_TYPE_BLOOD_PRESSURE:
                dataLength = 2;
                break;
            default:
                throw new IllegalArgumentException("Unknown PPG type");
        }

        if (paramsLength != dataLength + 1)
            throwUnexpectedLength();

        ppgData = new byte[dataLength];
        params.get(ppgData);
    }

    @Override
    protected byte serializeParams(ByteBuffer params) {
        // No handler on device side
        throw new UnsupportedOperationException();
    }
}
