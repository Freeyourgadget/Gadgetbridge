/*  Copyright (C) 2023-2024 akasaka / Genjitsu Labs

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.notification;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.Wena3Packetable;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.notification.defines.LedColor;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.notification.defines.NotificationFlags;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.notification.defines.NotificationKind;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.notification.defines.VibrationOptions;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.util.TimeUtil;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class NotificationArrival implements Wena3Packetable {
    public final NotificationKind kind;
    public final int id;
    public final String title;
    public final String message;
    public final String actionLabel;
    public final Date dateTime;
    public final VibrationOptions vibration;
    public final LedColor ledColor;
    public final NotificationFlags flags;

    public NotificationArrival(
            NotificationKind kind,
            int id,
            String title,
            String message,
            String actionLabel,
            Date dateTime,
            VibrationOptions vibration,
            LedColor ledColor,
            NotificationFlags flags
    ) {
        this.kind = kind;
        this.id = id;
        this.title = title;
        this.message = message;
        this.actionLabel = actionLabel;
        this.dateTime = dateTime;
        this.vibration = vibration;
        this.ledColor = ledColor;
        this.flags = flags;
    }

    @Override
    public byte[] toByteArray() {
        assert vibration.count <= 255;

        byte vibraFlags = 0;
        byte vibraCount = (byte)vibration.count;

        if(vibration.continuous) {
            vibraFlags = 0x3;
            vibraCount = 0;
        } else {
            if(vibraCount > 4) vibraCount = 4;
            else if (vibraCount < 0) vibraCount = 1;
        }

        byte[] encodedTitle = StringUtils.truncate(title.trim(), 31).getBytes(StandardCharsets.UTF_8);
        byte[] encodedMessage = StringUtils.truncate(message.trim(), 239).getBytes(StandardCharsets.UTF_8);
        byte[] encodedAction = StringUtils.truncate(actionLabel.trim(), 15).getBytes(StandardCharsets.UTF_8);

        ByteBuffer buf = ByteBuffer
                .allocate(18 + encodedTitle.length + encodedMessage.length + encodedAction.length)
                .order(ByteOrder.LITTLE_ENDIAN);

        buf.put((byte) 0x00) // marker
                .put((byte)ledColor.ordinal())
                .put(vibraFlags)
                .put((byte)vibration.kind.ordinal())
                .put(vibraCount)
                .put((byte)encodedTitle.length)
                .put((byte)encodedMessage.length)
                .put((byte)flags.value)
                .put((byte)kind.ordinal())
                .put((byte)encodedAction.length)
                .putInt(id)
                .putInt(TimeUtil.dateToWenaTime(dateTime))
                .put(encodedTitle)
                .put(encodedMessage)
                .put(encodedAction);

        return buf.array();
    }
}
