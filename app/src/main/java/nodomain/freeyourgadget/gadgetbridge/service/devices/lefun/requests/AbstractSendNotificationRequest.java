/*  Copyright (C) 2020-2021 Yukai Li

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.lefun.requests;

import android.bluetooth.BluetoothGattCharacteristic;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.lefun.LefunConstants;
import nodomain.freeyourgadget.gadgetbridge.devices.lefun.commands.NotificationCommand;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.lefun.LefunDeviceSupport;

public abstract class AbstractSendNotificationRequest extends Request {
    protected AbstractSendNotificationRequest(LefunDeviceSupport support, TransactionBuilder builder) {
        super(support, builder);
    }

    protected abstract String getMessage();

    protected abstract byte getNotificationType();

    protected abstract byte getExtendedNotificationType();

    @Override
    public byte[] createRequest() {
        return new byte[0];
    }

    @Override
    protected void doPerform() throws IOException {
        byte notificationType = getNotificationType();
        byte extendedNotificationType = getExtendedNotificationType();
        boolean reserveSpaceForExtended = notificationType == NotificationCommand.SERVICE_TYPE_EXTENDED;
        byte[] encoded = getMessage().getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.wrap(encoded);

        BluetoothGattCharacteristic characteristic = getSupport()
                .getCharacteristic(LefunConstants.UUID_CHARACTERISTIC_LEFUN_WRITE);

        List<NotificationCommand> commandList = new ArrayList<>();
        int charsWritten = 0;
        for (int i = 0; i < 0xff; ++i) {
            int maxPayloadLength = NotificationCommand.MAX_PAYLOAD_LENGTH;
            if (reserveSpaceForExtended) maxPayloadLength -= 1;
            maxPayloadLength = Math.min(maxPayloadLength, buffer.limit() - buffer.position());
            maxPayloadLength = Math.min(maxPayloadLength, NotificationCommand.MAX_MESSAGE_LENGTH - charsWritten);
            if (maxPayloadLength == 0 && i != 0) break;

            byte[] payload = new byte[maxPayloadLength];
            buffer.get(payload);

            NotificationCommand cmd = new NotificationCommand();
            cmd.setServiceType(notificationType);
            cmd.setExtendedServiceType(extendedNotificationType);
            cmd.setCurrentPiece((byte) (i + 1));
            cmd.setPayload(payload);
            charsWritten += maxPayloadLength;

            commandList.add(cmd);
        }

        for (NotificationCommand cmd : commandList) {
            cmd.setTotalPieces((byte) commandList.size());
            builder.write(characteristic, cmd.serialize());
        }

        if (isSelfQueue())
            getSupport().performConnected(builder.getTransaction());
    }

    @Override
    public int getCommandId() {
        return LefunConstants.CMD_NOTIFICATION;
    }

    @Override
    public boolean expectsResponse() {
        return false;
    }
}
