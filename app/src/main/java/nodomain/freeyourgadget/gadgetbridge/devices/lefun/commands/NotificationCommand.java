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
package nodomain.freeyourgadget.gadgetbridge.devices.lefun.commands;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.devices.lefun.LefunConstants;

public class NotificationCommand extends BaseCommand {
    public static final byte SERVICE_TYPE_CALL = 0;
    public static final byte SERVICE_TYPE_TEXT = 1;
    public static final byte SERVICE_TYPE_QQ = 2;
    public static final byte SERVICE_TYPE_WECHAT = 3;
    public static final byte SERVICE_TYPE_EXTENDED = 4;

    public static final byte EXTENDED_SERVICE_TYPE_FACEBOOK = 1;
    public static final byte EXTENDED_SERVICE_TYPE_TWITTER = 2;
    public static final byte EXTENDED_SERVICE_TYPE_LINKEDIN = 3;
    public static final byte EXTENDED_SERVICE_TYPE_WHATSAPP = 4;
    public static final byte EXTENDED_SERVICE_TYPE_LINE = 5;
    public static final byte EXTENDED_SERVICE_TYPE_KAKAOTALK = 6;

    public static final int MAX_PAYLOAD_LENGTH = 13;
    public static final int MAX_MESSAGE_LENGTH = 254;

    private byte serviceType;
    private byte totalPieces;
    private byte currentPiece;
    private byte extendedServiceType;
    private byte[] payload;

    public int getServiceType() {
        return getLowestSetBitIndex(serviceType);
    }

    public void setServiceType(int type) {
        if (type < 0 || type > 4)
            throw new IllegalArgumentException("Invalid service type");
        this.serviceType = (byte) (1 << type);
    }

    public byte getTotalPieces() {
        return totalPieces;
    }

    public void setTotalPieces(byte totalPieces) {
        // This check isn't on device, but should probably be added
        if (totalPieces == 0)
            throw new IllegalArgumentException("Total pieces must not be 0");
        this.totalPieces = totalPieces;
    }

    public byte getCurrentPiece() {
        return currentPiece;
    }

    public void setCurrentPiece(byte currentPiece) {
        // This check isn't on device, but should probably be added
        if (currentPiece == 0)
            throw new IllegalArgumentException("Current piece must not be 0");
        this.currentPiece = currentPiece;
    }

    public byte getExtendedServiceType() {
        return extendedServiceType;
    }

    public void setExtendedServiceType(byte extendedServiceType) {
        this.extendedServiceType = extendedServiceType;
    }

    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        if (payload == null)
            throw new IllegalArgumentException("Payload must not be null");
        if (payload.length > 13)
            throw new IllegalArgumentException("Payload is too long");
        this.payload = payload;
    }

    @Override
    protected void deserializeParams(byte id, ByteBuffer params) {
        // We should not receive a response for this
        throw new UnsupportedOperationException();
    }

    @Override
    protected byte serializeParams(ByteBuffer params) {
        boolean hasExtendedServiceType = (serviceType & (1 << SERVICE_TYPE_EXTENDED)) != 0
                && (extendedServiceType & 0x0f) != 0;
        int maxPayloadLength = MAX_PAYLOAD_LENGTH;
        if (hasExtendedServiceType) maxPayloadLength -= 1;

        if (payload.length > maxPayloadLength)
            throw new IllegalStateException("Payload is too long");

        params.put(serviceType);
        params.put(totalPieces);
        params.put(currentPiece);
        if (hasExtendedServiceType)
            params.put(extendedServiceType);
        params.put(payload);

        return LefunConstants.CMD_NOTIFICATION;
    }
}
