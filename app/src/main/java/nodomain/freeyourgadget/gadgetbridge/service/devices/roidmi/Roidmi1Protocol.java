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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventFmFrequency;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventLEDColor;
import nodomain.freeyourgadget.gadgetbridge.devices.roidmi.RoidmiConst;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class Roidmi1Protocol extends RoidmiProtocol {
    private static final Logger LOG = LoggerFactory.getLogger(Roidmi1Protocol.class);

    public Roidmi1Protocol(GBDevice device) {
        super(device);
    }

    private static final byte[] PACKET_HEADER = new byte[]{(byte) 0xaa, 0x55};
    private static final byte[] PACKET_TRAILER = new byte[]{(byte) 0xc3, 0x3c};
    private static final byte COMMAND_SET_FREQUENCY = 0x10;
    private static final byte COMMAND_GET_FREQUENCY = (byte) 0x80;
    private static final byte COMMAND_SET_COLOR = 0x11;
    private static final byte COMMAND_GET_COLOR = (byte) 0x81;

    private static final int PACKET_MIN_LENGTH = 6;

    private static final int LED_COLOR_RED = 1;
    private static final int LED_COLOR_GREEN = 2;
    private static final int LED_COLOR_BLUE = 3;
    private static final int LED_COLOR_YELLOW = 4; // not official
    private static final int LED_COLOR_SKY_BLUE = 5;
    private static final int LED_COLOR_PINK = 6; // not official
    private static final int LED_COLOR_WHITE = 7; // not official
    private static final int LED_COLOR_OFF = 8;

    // Other commands:
    // App periodically sends aa5502018588c33c and receives aa5506018515111804cec33c
    private static final byte[] COMMAND_PERIODIC = new byte[]{(byte) 0xaa, 0x55, 0x02, 0x01, (byte) 0x85, (byte) 0x88, (byte) 0xc3, 0x3c};

    @Override
    public GBDeviceEvent[] decodeResponse(byte[] responseData) {
        if (responseData.length <= PACKET_MIN_LENGTH) {
            LOG.info("Response too small");
            return null;
        }

        for (int i = 0; i < packetHeader().length; i++) {
            if (responseData[i] != packetHeader()[i]) {
                LOG.info("Invalid response header");
                return null;
            }
        }

        for (int i = 0; i < packetTrailer().length; i++) {
            if (responseData[responseData.length - packetTrailer().length + i] != packetTrailer()[i]) {
                LOG.info("Invalid response trailer");
                return null;
            }
        }

        if (calcChecksum(responseData) != responseData[responseData.length - packetTrailer().length - 1]) {
            LOG.info("Invalid response checksum");
            return null;
        }

        switch (responseData[3]) {
            case COMMAND_GET_COLOR:
                int color = responseData[5];
                LOG.debug("Got color: " + color);
                GBDeviceEventLEDColor evColor = new GBDeviceEventLEDColor();
                evColor.color = RoidmiConst.COLOR_PRESETS[color - 1];
                return new GBDeviceEvent[]{evColor};
            case COMMAND_GET_FREQUENCY:
                String frequencyHex = GB.hexdump(responseData, 4, 2);
                float frequency = Float.valueOf(frequencyHex) / 10.0f;
                LOG.debug("Got frequency: " + frequency);
                GBDeviceEventFmFrequency evFrequency = new GBDeviceEventFmFrequency();
                evFrequency.frequency = frequency;
                return new GBDeviceEvent[]{evFrequency};
            default:
                LOG.error("Unrecognized response type 0x" + GB.hexdump(responseData, packetHeader().length, 1));
                return null;
        }
    }

    @Override
    public byte[] encodeLedColor(int color) {
        int[] presets = RoidmiConst.COLOR_PRESETS;
        int color_id = -1;
        for (int i = 0; i < presets.length; i++) {
            if (presets[i] == color) {
                color_id = (i + 1) & 255;
                break;
            }
        }

        if (color_id < 0 || color_id > 8)
            throw new IllegalArgumentException("color must belong to RoidmiConst.COLOR_PRESETS");

        return encodeCommand(COMMAND_SET_COLOR, (byte) 0, (byte) color_id);
    }

    @Override
    public byte[] encodeFmFrequency(float frequency) {
        if (frequency < 87.5 || frequency > 108.0)
            throw new IllegalArgumentException("Frequency must be >= 87.5 and <= 180.0");

        byte[] freq = frequencyToBytes(frequency);

        return encodeCommand(COMMAND_SET_FREQUENCY, freq[0], freq[1]);
    }

    public byte[] encodeGetLedColor() {
        return encodeCommand(COMMAND_GET_COLOR, (byte) 0, (byte) 0);
    }

    public byte[] encodeGetFmFrequency() {
        return encodeCommand(COMMAND_GET_FREQUENCY, (byte) 0, (byte) 0);
    }

    public byte[] encodeGetVoltage() {
        return null;
    }

    public byte[] packetHeader() {
        return PACKET_HEADER;
    }

    public byte[] packetTrailer() {
        return PACKET_TRAILER;
    }
}
