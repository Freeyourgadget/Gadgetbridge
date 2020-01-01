/*  Copyright (C) 2018-2019 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.roidmi;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btclassic.BtClassicIoThread;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class RoidmiIoThread extends BtClassicIoThread {
    private static final Logger LOG = LoggerFactory.getLogger(RoidmiIoThread.class);

    private final byte[] HEADER;
    private final byte[] TRAILER;

    public RoidmiIoThread(GBDevice gbDevice, Context context, RoidmiProtocol roidmiProtocol, RoidmiSupport roidmiSupport, BluetoothAdapter roidmiBtAdapter) {
        super(gbDevice, context, roidmiProtocol, roidmiSupport, roidmiBtAdapter);

        HEADER = roidmiProtocol.packetHeader();
        TRAILER = roidmiProtocol.packetTrailer();
    }

    @Override
    protected byte[] parseIncoming(InputStream inputStream) throws IOException {
        ByteArrayOutputStream msgStream = new ByteArrayOutputStream();

        boolean finished = false;
        byte[] incoming = new byte[1];

        while (!finished) {
            inputStream.read(incoming);
            msgStream.write(incoming);

            byte[] arr = msgStream.toByteArray();
            if (arr.length > HEADER.length) {
                int expectedLength = HEADER.length + TRAILER.length + arr[HEADER.length] + 2;
                if (arr.length == expectedLength) {
                    finished = true;
                }
            }
        }

        byte[] msgArray = msgStream.toByteArray();
        LOG.debug("Packet: " + GB.hexdump(msgArray, 0, msgArray.length));
        return msgArray;
    }
}
