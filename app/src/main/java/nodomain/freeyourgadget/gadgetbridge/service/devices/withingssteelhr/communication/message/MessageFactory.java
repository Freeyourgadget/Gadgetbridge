/*  Copyright (C) 2023-2024 Frank Ertl

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.datastructures.DataStructureFactory;
import nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.datastructures.WithingsStructure;


public class MessageFactory {
    private static final Logger logger = LoggerFactory.getLogger(MessageFactory.class);
    private DataStructureFactory dataStructureFactory;

    public MessageFactory(DataStructureFactory dataStructureFactory) {
        this.dataStructureFactory = new DataStructureFactory();
    }

    public Message createMessageFromRawData(byte[] rawData) {
        if (rawData.length < 5 || rawData[0] != 0x01) {
            return null;
        }

        short messageTypeFromResponse = (short) (BLETypeConversions.toInt16(rawData[2], rawData[1]) & 16383);
        short totalDataLength = (short) BLETypeConversions.toInt16(rawData[4], rawData[3]);
        boolean isIncoming = rawData[1] == 65 || rawData[1] == -127;
        Message message = new WithingsMessage(messageTypeFromResponse, isIncoming);
        byte[] rawStructureData = Arrays.copyOfRange(rawData, 5, rawData.length);
        List<WithingsStructure> structures = dataStructureFactory.createStructuresFromRawData(rawStructureData);
        for (WithingsStructure structure : structures) {
            message.addDataStructure(structure);
        }

        return message;
    }
}
