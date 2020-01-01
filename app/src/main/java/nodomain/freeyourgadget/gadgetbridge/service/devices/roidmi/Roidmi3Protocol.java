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
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventFmFrequency;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventLEDColor;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class Roidmi3Protocol extends RoidmiProtocol {
    private static final Logger LOG = LoggerFactory.getLogger(Roidmi3Protocol.class);

    public Roidmi3Protocol(GBDevice device) {
        super(device);
    }

    // Commands below need to be wrapped in a packet

    private static final byte[] COMMAND_GET_COLOR = new byte[]{0x02, (byte) 0x81};
    private static final byte[] COMMAND_GET_FREQUENCY = new byte[]{0x05, (byte) 0x81};
    private static final byte[] COMMAND_GET_VOLTAGE = new byte[]{0x06, (byte) 0x81};

    private static final byte[] COMMAND_SET_COLOR = new byte[]{0x02, 0x01, 0x00, 0x00, 0x00};
    private static final byte[] COMMAND_SET_FREQUENCY = new byte[]{0x05, 0x01, 0x09, 0x64};
    private static final byte[] COMMAND_DENOISE_ON = new byte[]{0x05, 0x06, 0x12};
    private static final byte[] COMMAND_DENOISE_OFF = new byte[]{0x05, 0x06, 0x00};

    private static final byte RESPONSE_COLOR = 0x02;
    private static final byte RESPONSE_FREQUENCY = 0x05;
    private static final byte RESPONSE_VOLTAGE = 0x06;
    // Next response byte is always 0x81, followed by the value

    private static final int PACKET_MIN_LENGTH = 4;

    @Override
    public GBDeviceEvent[] decodeResponse(byte[] res) {
        if (res.length <= PACKET_MIN_LENGTH) {
            LOG.info("Response too small");
            return null;
        }

        if (calcChecksum(res) != res[res.length - 1]) {
            LOG.info("Invalid response checksum");
            return null;
        }

        if (res[0] + 2 != res.length) {
            LOG.info("Packet length doesn't match");
            return null;
        }

        if (res[2] != (byte) 0x81) {
            LOG.warn("Potentially unsupported response: " + GB.hexdump(res, 0, res.length));
        }

        if (res[1] == RESPONSE_VOLTAGE) {
            String voltageHex = GB.hexdump(res, 3, 2);
            float voltage = Float.valueOf(voltageHex) / 100.0f;
            LOG.debug("Got voltage: " + voltage);
            GBDeviceEventBatteryInfo evBattery = new GBDeviceEventBatteryInfo();
            evBattery.state = BatteryState.NO_BATTERY;
            evBattery.level = GBDevice.BATTERY_UNKNOWN;
            evBattery.voltage = voltage;
            return new GBDeviceEvent[]{evBattery};
        } else if (res[1] == RESPONSE_COLOR) {
            LOG.debug("Got color: #" + GB.hexdump(res, 3, 3));
            int color = 0xFF000000 | ((res[3] << 16) & 0xFF0000) | ((res[4] << 8) & 0xFF00) | (res[5] & 0xFF);
            GBDeviceEventLEDColor evColor = new GBDeviceEventLEDColor();
            evColor.color = color;
            return new GBDeviceEvent[]{evColor};
        } else if (res[1] == RESPONSE_FREQUENCY) {
            String frequencyHex = GB.hexdump(res, 3, 2);
            float frequency = Float.valueOf(frequencyHex) / 10.0f;
            LOG.debug("Got frequency: " + frequency);
            GBDeviceEventFmFrequency evFrequency = new GBDeviceEventFmFrequency();
            evFrequency.frequency = frequency;
            return new GBDeviceEvent[]{evFrequency};
        } else {
            LOG.error("Unrecognized response: " + GB.hexdump(res, 0, res.length));
            return null;
        }
    }

    @Override
    public byte[] encodeLedColor(int color) {
        byte[] cmd = COMMAND_SET_COLOR.clone();

        cmd[2] = (byte) (color >> 16);
        cmd[3] = (byte) (color >> 8);
        cmd[4] = (byte) color;

        return encodeCommand(cmd);
    }

    @Override
    public byte[] encodeFmFrequency(float frequency) {
        if (frequency < 87.5 || frequency > 108.0)
            throw new IllegalArgumentException("Frequency must be >= 87.5 and <= 180.0");

        byte[] cmd = COMMAND_SET_FREQUENCY.clone();
        byte[] freq = frequencyToBytes(frequency);
        cmd[2] = freq[0];
        cmd[3] = freq[1];

        return encodeCommand(cmd);
    }

    @Override
    public byte[] encodeGetLedColor() {
        return encodeCommand(COMMAND_GET_COLOR);
    }

    @Override
    public byte[] encodeGetFmFrequency() {
        return encodeCommand(COMMAND_GET_FREQUENCY);
    }

    @Override
    public byte[] packetHeader() {
        return new byte[0];
    }

    @Override
    public byte[] packetTrailer() {
        return new byte[0];
    }

    public byte[] encodeGetVoltage() {
        return encodeCommand(COMMAND_GET_VOLTAGE);
    }

    public byte[] encodeDenoise(boolean enabled) {
        byte[] cmd = enabled ? COMMAND_DENOISE_ON : COMMAND_DENOISE_OFF;
        return encodeCommand(cmd);
    }
}
