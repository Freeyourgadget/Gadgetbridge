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

public class SetRemoteCameraCommand extends BaseCommand {
    private boolean remoteCameraEnabled;

    private boolean setSuccess;

    public boolean getRemoteCameraEnabled() {
        return remoteCameraEnabled;
    }

    public void setRemoteCameraEnabled(boolean remoteCameraEnabled) {
        this.remoteCameraEnabled = remoteCameraEnabled;
    }

    public boolean isSetSuccess() {
        return setSuccess;
    }

    @Override
    protected void deserializeParams(byte id, ByteBuffer params) {
        validateIdAndLength(id, params, LefunConstants.CMD_REMOTE_CAMERA, 1);

        setSuccess = params.get() == 1;
    }

    @Override
    protected byte serializeParams(ByteBuffer params) {
        params.put((byte)(remoteCameraEnabled ? 1 : 0));
        return LefunConstants.CMD_REMOTE_CAMERA;
    }
}
