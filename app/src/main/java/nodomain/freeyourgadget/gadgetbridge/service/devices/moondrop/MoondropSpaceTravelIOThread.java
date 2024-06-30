/*  Copyright (C) 2024 Severin von Wnuck-Lipinski

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.moondrop;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btclassic.BtClassicIoThread;

import static nodomain.freeyourgadget.gadgetbridge.util.GB.hexdump;

public class MoondropSpaceTravelIOThread extends BtClassicIoThread {
    private static final Logger LOG = LoggerFactory.getLogger(MoondropSpaceTravelIOThread.class);

    private final MoondropSpaceTravelProtocol protocol;

    public MoondropSpaceTravelIOThread(
            GBDevice device,
            Context context,
            MoondropSpaceTravelProtocol protocol,
            MoondropSpaceTravelDeviceSupport support,
            BluetoothAdapter adapter) {
        super(device, context, protocol, support, adapter);
        this.protocol = protocol;
    }

    @Override
    protected void initialize() {
        write(protocol.encodeGetEqualizerPreset());
        write(protocol.encodeGetTouchActions());
        setUpdateState(GBDevice.State.INITIALIZED);
    }

    @Override
    protected byte[] parseIncoming(InputStream stream) throws IOException {
        byte[] buffer = new byte[1048576];
        int bytes = stream.read(buffer);
        LOG.debug("read " + bytes + " bytes. " + hexdump(buffer, 0, bytes));

        return Arrays.copyOf(buffer, bytes);
    }
}
