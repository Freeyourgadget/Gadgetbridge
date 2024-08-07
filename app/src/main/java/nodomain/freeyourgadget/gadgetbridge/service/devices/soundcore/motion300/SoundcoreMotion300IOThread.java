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
package nodomain.freeyourgadget.gadgetbridge.service.devices.soundcore.motion300;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.os.Handler;
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

public class SoundcoreMotion300IOThread extends BtClassicIoThread {
    private static final Logger LOG = LoggerFactory.getLogger(SoundcoreMotion300IOThread.class);

    private final SoundcoreMotion300Protocol protocol;
    private final Handler handler = new Handler();

    public SoundcoreMotion300IOThread(
            GBDevice device,
            Context context,
            SoundcoreMotion300Protocol protocol,
            SoundcoreMotion300DeviceSupport support,
            BluetoothAdapter adapter) {
        super(device, context, protocol, support, adapter);
        this.protocol = protocol;
    }

    @Override
    protected void initialize() {
        setUpdateState(GBDevice.State.INITIALIZING);

        // Device requires a little delay to respond to commands
        handler.postDelayed(() -> write(protocol.encodeGetDeviceInfo()), 500);
    }

    @NonNull
    protected UUID getUuidToConnect(@NonNull ParcelUuid[] uuids) {
        return UUID.fromString("0cf12d31-fac3-4553-bd80-d6832e7b3135");
    }

    @Override
    protected byte[] parseIncoming(InputStream stream) throws IOException {
        byte[] buffer = new byte[1048576];
        int bytes = stream.read(buffer);
        LOG.debug("read " + bytes + " bytes. " + hexdump(buffer, 0, bytes));

        return Arrays.copyOf(buffer, bytes);
    }
}
