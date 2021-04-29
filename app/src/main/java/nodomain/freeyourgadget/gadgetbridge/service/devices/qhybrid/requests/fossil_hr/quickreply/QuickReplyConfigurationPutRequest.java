/*  Copyright (C) 2019-2021 Arjan Schrijver

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.quickreply;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil_hr.FossilHRWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.file.FileHandle;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FilePutRequest;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class QuickReplyConfigurationPutRequest extends FilePutRequest {
    public QuickReplyConfigurationPutRequest(String[] replies, FossilHRWatchAdapter adapter) {
        super(FileHandle.REPLY_MESSAGES, createFile(replies), adapter);
    }

    private static byte[] createFile(String[] replies) {
        String[] processedReplies = new String[replies.length];
        int fileLength = 0;

        byte[] mysteryHeader = new byte[]{(byte) 0x02, (byte) 0x0b, (byte) 0x46, (byte) 0x00, (byte) 0x03, (byte) 0x19, (byte) 0x00, (byte) 0x00, (byte) 0x00};

        Charset charsetUTF8 = StandardCharsets.UTF_8;
        String iconName = StringUtils.terminateNull("icMessage.icon");
        byte[] iconNameBytes = iconName.getBytes(charsetUTF8);

        for (int index=0; index< replies.length; index++) {
            String reply = replies[index];
            if (reply.length() > 50) {
                reply = reply.substring(0, 50);
            }
            processedReplies[index] = StringUtils.terminateNull(reply);
            fileLength += 8 + processedReplies[index].getBytes(charsetUTF8).length + iconNameBytes.length;
        }

        ByteBuffer mainBuffer = ByteBuffer.allocate(mysteryHeader.length + 4 + fileLength);
        mainBuffer.order(ByteOrder.LITTLE_ENDIAN);

        mainBuffer.put(mysteryHeader);
        mainBuffer.putInt(fileLength);

        for (int index=0; index < processedReplies.length; index++) {
            byte[] msgBytes = processedReplies[index].getBytes(charsetUTF8);
            mainBuffer.putShort((short) (8 + msgBytes.length + iconNameBytes.length));
            mainBuffer.put((byte) 0x08);
            mainBuffer.put((byte) index);
            mainBuffer.putShort((short) msgBytes.length);
            mainBuffer.putShort((short) iconNameBytes.length);
            mainBuffer.put(msgBytes);
            mainBuffer.put(iconNameBytes);
        }

        return mainBuffer.array();
    }
}
