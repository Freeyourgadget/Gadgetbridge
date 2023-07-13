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
package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.ancs;

import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.MessageReader;
import nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public abstract class AncsControlCommand {
    private static final Logger LOG = LoggerFactory.getLogger(AncsControlCommand.class);

    private static final AncsAppAttribute[] APP_ATTRIBUTE_VALUES = AncsAppAttribute.values();
    private static final AncsAction[] ACTION_VALUES = AncsAction.values();

    public final AncsCommand command;

    protected AncsControlCommand(AncsCommand command) {
        this.command = command;
    }

    public static AncsControlCommand parseCommand(byte[] buffer, int offset, int size) {
        final int commandID = BLETypeConversions.toUnsigned(buffer, offset);
        final AncsCommand command = AncsCommand.getByCode(commandID);
        if (command == null) {
            LOG.error("Unknown ANCS command {}", commandID);
            return null;
        }
        switch (command) {
            case GET_NOTIFICATION_ATTRIBUTES:
                return createGetNotificationAttributesCommand(buffer, offset + 1, size - 1);
            case GET_APP_ATTRIBUTES:
                return createGetAppAttributesCommand(buffer, offset + 1, size - 1);
            case PERFORM_NOTIFICATION_ACTION:
                return createPerformNotificationAction(buffer, offset + 1, size - 1);
            case PERFORM_ANDROID_ACTION:
                return createPerformAndroidAction(buffer, offset + 1, size - 1);
            default:
                LOG.error("Unknown ANCS command {}", command);
                return null;
        }
    }

    private static AncsPerformAndroidAction createPerformAndroidAction(byte[] buffer, int offset, int size) {
        final int notificationUID = BLETypeConversions.toUint32(buffer, offset);
        final int actionID = BLETypeConversions.toUnsigned(buffer, offset + 4);
        final AncsAndroidAction action = AncsAndroidAction.getByCode(actionID);
        if (action == null) {
            LOG.error("Unknown ANCS Android action {}", actionID);
            return null;
        }
        int zero = ArrayUtils.indexOf((byte) 0, buffer, offset + 6, size - offset - 6);
        if (zero < 0) zero = size;
        final String text = new String(buffer, offset + 6, zero - offset - 6);

        return new AncsPerformAndroidAction(notificationUID, action, text);
    }

    private static AncsPerformNotificationAction createPerformNotificationAction(byte[] buffer, int offset, int size) {
        final MessageReader reader = new MessageReader(buffer, offset);
        final int notificationUID = reader.readInt();
        final int actionID = reader.readByte();
        if (actionID < 0 || actionID >= ACTION_VALUES.length) {
            LOG.error("Unknown ANCS action {}", actionID);
            return null;
        }
        return new AncsPerformNotificationAction(notificationUID, ACTION_VALUES[actionID]);
    }

    private static AncsGetAppAttributesCommand createGetAppAttributesCommand(byte[] buffer, int offset, int size) {
        int zero = ArrayUtils.indexOf((byte) 0, buffer, offset, size - offset);
        if (zero < 0) zero = size;
        final String appIdentifier = new String(buffer, offset, zero - offset, StandardCharsets.UTF_8);
        final int attributeCount = size - (zero - offset);
        final List<AncsAppAttribute> requestedAttributes = new ArrayList<>(attributeCount);
        for (int i = 0; i < attributeCount; ++i) {
            final int attributeID = BLETypeConversions.toUnsigned(buffer, zero + 1 + i);
            if (attributeID < 0 || attributeID >= APP_ATTRIBUTE_VALUES.length) {
                LOG.error("Unknown ANCS app attribute {}", attributeID);
                return null;
            }
            final AncsAppAttribute attribute = APP_ATTRIBUTE_VALUES[attributeID];
            requestedAttributes.add(attribute);
        }
        return new AncsGetAppAttributesCommand(appIdentifier, requestedAttributes);
    }

    private static AncsGetNotificationAttributeCommand createGetNotificationAttributesCommand(byte[] buffer, int offset, int size) {
        final MessageReader reader = new MessageReader(buffer, offset);
        final int notificationUID = reader.readInt();
        int pos = 4;
        final List<AncsAttributeRequest> attributes = new ArrayList<>(size);
        while (pos < size) {
            final int attributeID = reader.readByte();
            ++pos;
            final AncsAttribute attribute = AncsAttribute.getByCode(attributeID);
            if (attribute == null) {
                LOG.error("Unknown ANCS attribute {}", attributeID);
                return null;
            }
            final int maxLength;
            if (attribute.hasLengthParam) {
                maxLength = reader.readShort();
                pos += 2;
            } else if (attribute.hasAdditionalParams) {
                maxLength = reader.readByte();
                // TODO: What is this??
                reader.readByte();
                reader.readByte();
                pos += 3;
            } else {
                maxLength = 0;
            }
            attributes.add(new AncsAttributeRequest(attribute, maxLength));
        }
        return new AncsGetNotificationAttributeCommand(notificationUID, attributes);
    }
}
