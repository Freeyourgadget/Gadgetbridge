/*  Copyright (C) 2021 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wh1000xm3;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.os.ParcelUuid;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btclassic.BtClassicIoThread;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class SonyWh1000Xm3IoThread extends BtClassicIoThread {
    private static final Logger LOG = LoggerFactory.getLogger(SonyWh1000Xm3IoThread.class);

    public SonyWh1000Xm3IoThread(GBDevice gbDevice, Context context, SonyWh1000Xm3Protocol xm3protocol, SonyWh1000Xm3Support xm3support, BluetoothAdapter roidmiBtAdapter) {
        super(gbDevice, context, xm3protocol, xm3support, roidmiBtAdapter);
    }

    @Override
    protected byte[] parseIncoming(InputStream inputStream) throws IOException {
        ByteArrayOutputStream msgStream = new ByteArrayOutputStream();
        byte[] incoming = new byte[1];

        while (true) {
            inputStream.read(incoming);

            if (incoming[0] == SonyWh1000Xm3Protocol.PACKET_HEADER) {
                msgStream.reset();
                continue;
            }

            if (incoming[0] == SonyWh1000Xm3Protocol.PACKET_TRAILER) {
                break;
            }

            msgStream.write(incoming);
        }

        byte[] msgArray = msgStream.toByteArray();
        LOG.debug("Received: " + GB.hexdump(msgArray, 0, msgArray.length));
        return msgArray;
    }

    @NonNull
    @Override
    protected UUID getUuidToConnect(@NonNull ParcelUuid[] uuids) {
        return UUID.fromString("96CC203E-5068-46ad-B32D-E316F5E069BA");
    }
}
