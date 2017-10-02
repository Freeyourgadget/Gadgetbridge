/*  Copyright (C) 2016-2017 Carsten Pfeiffer, Daniele Gobbetti

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.liveview;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.devices.liveview.LiveviewConstants;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btclassic.BtClassicIoThread;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class LiveviewIoThread extends BtClassicIoThread {
    private static final Logger LOG = LoggerFactory.getLogger(LiveviewIoThread.class);

    private static final UUID SERIAL = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public LiveviewIoThread(GBDevice gbDevice, Context context, GBDeviceProtocol lvProtocol, LiveviewSupport lvSupport, BluetoothAdapter lvBtAdapter) {
        super(gbDevice, context, lvProtocol, lvSupport, lvBtAdapter);
    }

    protected byte[] parseIncoming(InputStream inputStream) throws IOException {
        ByteArrayOutputStream msgStream = new ByteArrayOutputStream();

        boolean finished = false;
        ReaderState state = ReaderState.ID;
        byte[] incoming = new byte[1];

        while (!finished) {
            inputStream.read(incoming);
            msgStream.write(incoming);

            switch (state) {
                case ID:
                    state = ReaderState.HEADER_LEN;
                    incoming = new byte[1];
                    break;
                case HEADER_LEN:
                    int headerSize = 0xff & incoming[0];
                    state = ReaderState.HEADER;
                    incoming = new byte[headerSize];
                    break;
                case HEADER:
                    int payloadSize = getLastInt(msgStream);
                    if (payloadSize < 0 || payloadSize > 8000) //this will possibly be changed in the future
                        throw new IOException();
                    state = ReaderState.PAYLOAD;
                    incoming = new byte[payloadSize];
                    break;
                case PAYLOAD: //read is blocking, if we are here we have all the data
                    finished = true;
                    break;
            }
        }
        byte[] msgArray = msgStream.toByteArray();
        LOG.debug("received: " + GB.hexdump(msgArray, 0, msgArray.length));
        return msgArray;
    }


    /**
     * Enumeration containing the possible internal status of the reader.
     */
    private enum ReaderState {
        ID, HEADER_LEN, HEADER, PAYLOAD;
    }

    private int getLastInt(ByteArrayOutputStream stream) {
        byte[] array = stream.toByteArray();
        ByteBuffer buffer = ByteBuffer.wrap(array, array.length - 4, 4);
        buffer.order(LiveviewConstants.BYTE_ORDER);
        return buffer.getInt();
    }
}
