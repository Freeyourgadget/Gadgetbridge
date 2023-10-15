/*  Copyright (C) 2023 Andreas Shimokawa

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.miwatch;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.SharedPreferences;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.proto.xiaomi.XiaomiProto;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiAuthService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSupport;
import nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class MiWatchLiteSupport extends XiaomiSupport {

    private static final Logger LOG = LoggerFactory.getLogger(MiWatchLiteSupport.class);

    private static final UUID UUID_CHARACTERISTIC_MAIN = UUID.fromString("16186f02-0000-1000-8000-00807f9b34fb");
    private static final UUID UUID_CHARACTERISTIC_UNK1 = UUID.fromString("16186f01-0000-1000-8000-00807f9b34fb");
    private static final UUID UUID_CHARACTERISTIC_UNK2 = UUID.fromString("16186f03-0000-1000-8000-00807f9b34fb");
    private static final UUID UUID_CHARACTERISTIC_UNK3 = UUID.fromString("16186f04-0000-1000-8000-00807f9b34fb");
    private static final UUID UUID_CHARACTERISTIC_UNK4 = UUID.fromString("16186f05-0000-1000-8000-00807f9b34fb");

    public MiWatchLiteSupport() {
        super(); // FIXME: no we do not want to do this!! This adds supported characteristics which we do not have - but we have to.
        addSupportedService(UUID.fromString("16186f00-0000-1000-8000-00807f9b34fb"));
    }

    @Override
    protected TransactionBuilder initializeDevice(final TransactionBuilder builder) {

        // FIXME why is this needed?
        getDevice().setFirmwareVersion("...");
        //getDevice().setFirmwareVersion2("...");
        enableNotifications(builder, true);
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));
        builder.requestMtu(247);
        String userId = getUserId(gbDevice);

        final XiaomiProto.Auth auth = XiaomiProto.Auth.newBuilder()
                .setUserId(userId)
                .build();

        final XiaomiProto.Command command = XiaomiProto.Command.newBuilder()
                .setType(XiaomiAuthService.COMMAND_TYPE)
                .setSubtype(XiaomiAuthService.CMD_SEND_USERID)
                .setAuth(auth)
                .build();
        sendCommand("send user id", command);

        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()));


        return builder;
    }

    private void enableNotifications(TransactionBuilder builder, boolean enable) {
        builder.notify(getCharacteristic(UUID_CHARACTERISTIC_MAIN), enable);
        builder.notify(getCharacteristic(UUID_CHARACTERISTIC_UNK1), enable);
        builder.notify(getCharacteristic(UUID_CHARACTERISTIC_UNK2), enable);
        builder.notify(getCharacteristic(UUID_CHARACTERISTIC_UNK3), enable);
        builder.notify(getCharacteristic(UUID_CHARACTERISTIC_UNK4), enable);
    }

    protected static String getUserId(final GBDevice device) {
        final SharedPreferences sharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(device.getAddress());

        final String authKey = sharedPrefs.getString("authkey", null);
        if (StringUtils.isNotBlank(authKey)) {
            return authKey;
        }

        return "0000000000";
    }

    @Override
    public void sendCommand(final TransactionBuilder builder, final nodomain.freeyourgadget.gadgetbridge.proto.xiaomi.XiaomiProto.Command command) {
        final byte[] commandBytes = command.toByteArray();
        final int commandLength = 2 + commandBytes.length;
        if (commandLength > getMTU()) {
            LOG.warn("Command with {} bytes is too large for MTU of {}", commandLength, getMTU());
        }
        builder.write(getCharacteristic(UUID_CHARACTERISTIC_MAIN), new byte[]{0x00, 0x00, 0x00, 0x00, 0x01, 0x00});
        builder.wait(500);

        final ByteBuffer buf = ByteBuffer.allocate(commandLength).order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) 1);
        buf.put((byte) 0);
        buf.put(commandBytes);
        LOG.debug("Sending command {} as {}", GB.hexdump(commandBytes), GB.hexdump(buf.array()));
        builder.write(getCharacteristic(UUID_CHARACTERISTIC_MAIN), buf.array());
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic) {
        if (super.onCharacteristicChanged(gatt, characteristic)) {
            return true;
        }

        UUID characteristicUUID = characteristic.getUuid();

        final byte[] success_bytes = new byte[]{0x00, 0x00, 0x01, 0x01, 0x00, 0x00};
        final byte[] ping_request = new byte[]{0x00, 0x00, 0x00, 0x00, 0x01, 0x00};
        if (characteristicUUID.equals(UUID_CHARACTERISTIC_UNK1)) {
            if (Arrays.equals(ping_request, characteristic.getValue())) {
                TransactionBuilder builder = new TransactionBuilder("reply ping");
                builder.write(getCharacteristic(UUID_CHARACTERISTIC_UNK1), new byte[]{0x00, 0x00, 0x01, 0x01});
                builder.queue(getQueue());
                return true;
            }
            if (ArrayUtils.startsWith(characteristic.getValue(), new byte[]{1, 0, 8})) {
                TransactionBuilder builder = new TransactionBuilder("ack whatever");
                builder.write(getCharacteristic(UUID_CHARACTERISTIC_UNK1), new byte[]{0x00, 0x00, 0x01, 0x00});
                builder.queue(getQueue());
                return true;
            }

        }
        LOG.info("Unhandled characteristic changed: " + characteristicUUID);
        return false;
    }
}
