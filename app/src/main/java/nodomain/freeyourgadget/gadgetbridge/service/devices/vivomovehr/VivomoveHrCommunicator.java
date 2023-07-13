/*  Copyright (C) 2020-2023 Petr Kadlec

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr;

import android.bluetooth.BluetoothGattCharacteristic;
import nodomain.freeyourgadget.gadgetbridge.devices.vivomovehr.VivomoveConstants;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;

public class VivomoveHrCommunicator {
    private static final Logger LOG = LoggerFactory.getLogger(VivomoveHrCommunicator.class);

    private final AbstractBTLEDeviceSupport deviceSupport;

    private BluetoothGattCharacteristic characteristicMessageSender;
    private BluetoothGattCharacteristic characteristicMessageReceiver;
    private BluetoothGattCharacteristic characteristicHeartRate;
    private BluetoothGattCharacteristic characteristicSteps;
    private BluetoothGattCharacteristic characteristicCalories;
    private BluetoothGattCharacteristic characteristicStairs;
    private BluetoothGattCharacteristic characteristicHrVariation;
    private BluetoothGattCharacteristic char2_9;

    public VivomoveHrCommunicator(AbstractBTLEDeviceSupport deviceSupport) {
        this.deviceSupport = deviceSupport;

        this.characteristicMessageSender = deviceSupport.getCharacteristic(VivomoveConstants.UUID_CHARACTERISTIC_GARMIN_GFDI_SEND);
        this.characteristicMessageReceiver = deviceSupport.getCharacteristic(VivomoveConstants.UUID_CHARACTERISTIC_GARMIN_GFDI_RECEIVE);
        this.characteristicHeartRate = deviceSupport.getCharacteristic(VivomoveConstants.UUID_CHARACTERISTIC_GARMIN_HEART_RATE);
        this.characteristicSteps = deviceSupport.getCharacteristic(VivomoveConstants.UUID_CHARACTERISTIC_GARMIN_STEPS);
        this.characteristicCalories = deviceSupport.getCharacteristic(VivomoveConstants.UUID_CHARACTERISTIC_GARMIN_CALORIES);
        this.characteristicStairs = deviceSupport.getCharacteristic(VivomoveConstants.UUID_CHARACTERISTIC_GARMIN_STAIRS);
        this.characteristicHrVariation = deviceSupport.getCharacteristic(VivomoveConstants.UUID_CHARACTERISTIC_GARMIN_HEART_RATE_VARIATION);
        this.char2_9 = deviceSupport.getCharacteristic(VivomoveConstants.UUID_CHARACTERISTIC_GARMIN_2_9);
    }

    public void start(TransactionBuilder builder) {
        builder.notify(characteristicMessageReceiver, true);
//        builder.notify(characteristicHeartRate, true);
//        builder.notify(characteristicSteps, true);
//        builder.notify(characteristicCalories, true);
//        builder.notify(characteristicStairs, true);
        //builder.notify(char2_7, true);
        // builder.notify(char2_9, true);
    }

    public void sendMessage(byte[] messageBytes) {
        try {
            final TransactionBuilder builder = deviceSupport.performInitialized("sendMessage()");
            sendMessage(builder, messageBytes);
            builder.queue(deviceSupport.getQueue());
        } catch (IOException e) {
            LOG.error("Unable to send a message", e);
        }
    }

    private void sendMessage(TransactionBuilder builder, byte[] messageBytes) {
        final byte[] packet = GfdiPacketParser.wrapMessageToPacket(messageBytes);
        int remainingBytes = packet.length;
        if (remainingBytes > VivomoveConstants.MAX_WRITE_SIZE) {
            int position = 0;
            while (remainingBytes > 0) {
                final byte[] fragment = Arrays.copyOfRange(packet, position, position + Math.min(remainingBytes, VivomoveConstants.MAX_WRITE_SIZE));
                builder.write(characteristicMessageSender, fragment);
                position += fragment.length;
                remainingBytes -= fragment.length;
            }
        } else {
            builder.write(characteristicMessageSender, packet);
        }
    }

    public void enableRealtimeSteps(boolean enable) {
        try {
            deviceSupport.performInitialized((enable ? "Enable" : "Disable") + " realtime steps").notify(characteristicSteps, enable).queue(deviceSupport.getQueue());
        } catch (IOException e) {
            LOG.error("Unable to change realtime steps notification to: " + enable, e);
        }
    }

    public void enableRealtimeHeartRate(boolean enable) {
        try {
            deviceSupport.performInitialized((enable ? "Enable" : "Disable") + " realtime heartrate").notify(characteristicHeartRate, enable).queue(deviceSupport.getQueue());
        } catch (IOException ex) {
            LOG.error("Unable to change realtime steps notification to: " + enable, ex);
        }
    }
}
