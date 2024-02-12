/*  Copyright (C) 2021-2024 Daniele Gobbetti, Petr Vaněk

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.nothing;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.os.ParcelUuid;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btclassic.BtClassicIoThread;

import static nodomain.freeyourgadget.gadgetbridge.util.GB.hexdump;

public class NothingIOThread extends BtClassicIoThread {
    private static final Logger LOG = LoggerFactory.getLogger(NothingIOThread.class);

    private final NothingProtocol mNothingProtocol;

    @NonNull
    protected UUID getUuidToConnect(@NonNull ParcelUuid[] uuids) {
        return mNothingProtocol.UUID_DEVICE_CTRL;
    }

    @Override
    protected void initialize() {
        write(mNothingProtocol.encodeBatteryStatusReq());
        write(mNothingProtocol.encodeAudioModeStatusReq());
        setUpdateState(GBDevice.State.INITIALIZED);
    }

    public NothingIOThread(GBDevice device, Context context, NothingProtocol deviceProtocol,
                           Ear1Support ear1Support, BluetoothAdapter bluetoothAdapter) {
        super(device, context, deviceProtocol, ear1Support, bluetoothAdapter);
        mNothingProtocol = deviceProtocol;
    }

    @Override
    protected byte[] parseIncoming(InputStream inStream) throws IOException {
        byte[] buffer = new byte[1048576]; //HUGE read
        int bytes = inStream.read(buffer);
        LOG.debug("read " + bytes + " bytes. " + hexdump(buffer, 0, bytes));
        return Arrays.copyOf(buffer, bytes);
    }

}
