/*  Copyright (C) 2018 José Rebelo

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;

public abstract class RoidmiProtocol extends GBDeviceProtocol {
    private static final Logger LOG = LoggerFactory.getLogger(RoidmiProtocol.class);

    // Packet structure: HEADER N_PARAMS PARAM_1 ... PARAM_N CHECKSUM TRAILER

    public RoidmiProtocol(GBDevice device) {
        super(device);
    }

    @Override
    public abstract GBDeviceEvent[] decodeResponse(byte[] responseData);

    @Override
    public abstract byte[] encodeLedColor(int color);

    @Override
    public abstract byte[] encodeFmFrequency(float frequency);

    public abstract byte[] encodeGetLedColor();

    public abstract byte[] encodeGetFmFrequency();

    public abstract byte[] encodeGetVoltage();

    public abstract byte[] packetHeader();

    public abstract byte[] packetTrailer();

    public byte[] encodeCommand(byte... params) {
        byte[] cmd = new byte[packetHeader().length + packetTrailer().length + params.length + 2];

        for (int i = 0; i < packetHeader().length; i++)
            cmd[i] = packetHeader()[i];
        for (int i = 0; i < packetTrailer().length; i++)
            cmd[cmd.length - packetTrailer().length + i] = packetTrailer()[i];

        cmd[packetHeader().length] = (byte) params.length;
        for (int i = 0; i < params.length; i++) {
            cmd[packetHeader().length + 1 + i] = params[i];
        }
        cmd[cmd.length - packetTrailer().length - 1] = calcChecksum(cmd);

        return cmd;
    }

    public byte calcChecksum(byte[] packet) {
        int chk = 0;
        for (int i = packetHeader().length; i < packet.length - packetTrailer().length - 1; i++) {
            chk += packet[i] & 255;
        }
        return (byte) chk;
    }

    public byte[] frequencyToBytes(float frequency) {
        byte[] res = new byte[2];
        String format = String.format(Locale.getDefault(), "%04d", (int) (10.0f * frequency));
        try {
            res[0] = (byte) (Integer.parseInt(format.substring(0, 2), 16) & 255);
            res[1] = (byte) (Integer.parseInt(format.substring(2), 16) & 255);
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }

        return res;
    }
}
