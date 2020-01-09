/*  Copyright (C) 2019-2020 Daniel Dakhno

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.notification;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.zip.CRC32;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FilePutRequest;

public class PlayNotificationRequest extends FilePutRequest {

    public PlayNotificationRequest(String packageName, FossilWatchAdapter adapter) {
        // super((short) 0x0900, createFile("org.telegram.messenger", "org.telegram.messenger", "org.telegram.messenger"), adapter);
        super((short) 0x0900, createFile(packageName), adapter);
    }

    private static byte[] createFile(String packageName){
        CRC32 crc = new CRC32();
        crc.update(packageName.getBytes());
        return createFile(packageName, packageName, packageName, (int)crc.getValue());
    }

    private static byte[] createFile(String title, String sender, String message, int packageCrc) {
        // return new byte[]{(byte) 0x57, (byte) 0x00, (byte) 0x0A, (byte) 0x03, (byte) 0x02, (byte) 0x04, (byte) 0x04, (byte) 0x17, (byte) 0x17, (byte) 0x17, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x49, (byte) 0x7B, (byte) 0x3B, (byte) 0x62, (byte) 0x6F, (byte) 0x72, (byte) 0x67, (byte) 0x2E, (byte) 0x74, (byte) 0x65, (byte) 0x6C, (byte) 0x65, (byte) 0x67, (byte) 0x72, (byte) 0x61, (byte) 0x6D, (byte) 0x2E, (byte) 0x6D, (byte) 0x65, (byte) 0x73, (byte) 0x73, (byte) 0x65, (byte) 0x6E, (byte) 0x67, (byte) 0x65, (byte) 0x72, (byte) 0x00, (byte) 0x6F, (byte) 0x72, (byte) 0x67, (byte) 0x2E, (byte) 0x74, (byte) 0x65, (byte) 0x6C, (byte) 0x65, (byte) 0x67, (byte) 0x72, (byte) 0x61, (byte) 0x6D, (byte) 0x2E, (byte) 0x6D, (byte) 0x65, (byte) 0x73, (byte) 0x73, (byte) 0x65, (byte) 0x6E, (byte) 0x67, (byte) 0x65, (byte) 0x72, (byte) 0x00, (byte) 0x6F, (byte) 0x72, (byte) 0x67, (byte) 0x2E, (byte) 0x74, (byte) 0x65, (byte) 0x6C, (byte) 0x65, (byte) 0x67, (byte) 0x72, (byte) 0x61, (byte) 0x6D, (byte) 0x2E, (byte) 0x6D, (byte) 0x65, (byte) 0x73, (byte) 0x73, (byte) 0x65, (byte) 0x6E, (byte) 0x67, (byte) 0x65, (byte) 0x72, (byte) 0x00};
        // gwb.k(var6, "ByteBuffer.allocate(10)");
        byte lengthBufferLength = (byte) 10;
        byte typeId = 3;
        byte flags = getFlags();
        byte uidLength = (byte) 4;
        byte appBundleCRCLength = (byte) 4;
        String nullTerminatedTitle = terminateNull(title);

        Charset charsetUTF8 = Charset.forName("UTF-8");
        byte[] titleBytes = nullTerminatedTitle.getBytes(charsetUTF8);
        // gwb.k(var13, "(this as java.lang.String).getBytes(charset)");
        String nullTerminatedSender = terminateNull(sender);
        byte[] senderBytes = nullTerminatedSender.getBytes(charsetUTF8);
        // gwb.k(var15, "(this as java.lang.String).getBytes(charset)");
        String nullTerminatedMessage = terminateNull(message);
        byte[] messageBytes = nullTerminatedMessage.getBytes(charsetUTF8);
        // gwb.k(var17, "(this as java.lang.String).getBytes(charset)");

        short mainBufferLength = (short) (lengthBufferLength + uidLength + appBundleCRCLength + titleBytes.length + senderBytes.length + messageBytes.length);

        ByteBuffer lengthBuffer = ByteBuffer.allocate(lengthBufferLength);
        lengthBuffer.order(ByteOrder.LITTLE_ENDIAN);
        lengthBuffer.putShort(mainBufferLength);
        lengthBuffer.put(lengthBufferLength);
        lengthBuffer.put(typeId);
        lengthBuffer.put(flags);
        lengthBuffer.put(uidLength);
        lengthBuffer.put(appBundleCRCLength);
        lengthBuffer.put((byte) titleBytes.length);
        lengthBuffer.put((byte) senderBytes.length);
        lengthBuffer.put((byte) messageBytes.length);

        ByteBuffer mainBuffer = ByteBuffer.allocate(mainBufferLength);
        // gwb.k(var11, "ByteBuffer.allocate(totalLen.toInt())");
        mainBuffer.order(ByteOrder.LITTLE_ENDIAN);
        mainBuffer.put(lengthBuffer.array());

        lengthBuffer = ByteBuffer.allocate(mainBufferLength - lengthBufferLength);
        // gwb.k(var6, "ByteBuffer.allocate(totalLen - headerLen)");
        lengthBuffer.order(ByteOrder.LITTLE_ENDIAN);
        lengthBuffer.putInt(0);
        lengthBuffer.putInt(packageCrc);
        lengthBuffer.put(titleBytes);
        lengthBuffer.put(senderBytes);
        lengthBuffer.put(messageBytes);
        mainBuffer.put(lengthBuffer.array());
        return mainBuffer.array();
    }

    private static byte getFlags(){
        return (byte) 2;
    }

    public static String terminateNull(String input){
        if(input.length() == 0){
            return new String(new byte[]{(byte) 0});
        }
        char lastChar = input.charAt(input.length() - 1);
        if(lastChar == 0) return input;

        byte[] newArray = new byte[input.length() + 1];
        System.arraycopy(input.getBytes(), 0, newArray, 0, input.length());

        newArray[newArray.length - 1] = 0;

        return new String(newArray);
    }
}
