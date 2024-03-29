/*  Copyright (C) 2020-2024 Yukai Li

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
package nodomain.freeyourgadget.gadgetbridge.devices.lefun.commands;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.devices.lefun.LefunConstants;

public class FeaturesCommand extends BaseCommand {
    public static final int FEATURE_RAISE_TO_WAKE = 0;
    public static final int FEATURE_SEDENTARY_REMINDER = 1;
    public static final int FEATURE_HYDRATION_REMINDER = 2;
    public static final int FEATURE_REMOTE_CAMERA = 3;
    public static final int FEATURE_UNKNOWN_4 = 4;
    public static final int FEATURE_ANTI_LOST = 5;

    private byte op;
    private short features;

    private boolean setSuccess;

    public byte getOp() {
        return op;
    }

    public void setOp(byte op) {
        if (op != OP_GET && op != OP_SET)
            throw new IllegalArgumentException("Operation must be get or set");
        this.op = op;
    }

    public boolean getFeature(int index) {
        if (index < 0 || index > 5)
            throw new IllegalArgumentException("Index must be between 0 and 5 inclusive");
        return getBit(features, 1 << index);
    }

    public void setFeature(int index, boolean enabled) {
        if (index < 0 || index > 5)
            throw new IllegalArgumentException("Index must be between 0 and 5 inclusive");
        features = setBit(features, 1 << index, enabled);
    }

    public boolean isSetSuccess() {
        return setSuccess;
    }

    @Override
    protected void deserializeParams(byte id, ByteBuffer params) {
        validateId(id, LefunConstants.CMD_FEATURES);

        int paramsLength = params.limit() - params.position();
        if (paramsLength < 1)
            throwUnexpectedLength();

        op = params.get();
        if (op == OP_GET) {
            if (paramsLength != 3)
                throwUnexpectedLength();

            features = params.getShort();
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
            params.putShort(features);
        }
        return LefunConstants.CMD_FEATURES;
    }
}
