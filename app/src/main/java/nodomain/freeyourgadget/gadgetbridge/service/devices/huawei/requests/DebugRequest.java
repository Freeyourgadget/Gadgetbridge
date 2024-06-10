/*  Copyright (C) 2024 Damien Gaignon, Martin.JM

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiConstants;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiTLV;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class DebugRequest extends Request {

    public DebugRequest(HuaweiSupportProvider support) {
        super(support);
        this.serviceId = 0;
        this.commandId = 0;
        this.addToResponse = false;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        String debugString = GBApplication
            .getDeviceSpecificSharedPrefs(supportProvider.getDevice().getAddress())
            .getString(HuaweiConstants.PREF_HUAWEI_DEBUG_REQUEST, "1,1,false,(1,/),(2,/),(3,/),(4,/)");
        HuaweiPacket packet = parseDebugString(debugString);
        try {
            return packet.serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    /*
        DebugString		:= [service_id] "," [command id] "," [encryptflag] "," [sliceflag] ("," [tlv])*
        service_id		:= int
                         | "0x" hex
        command_id		:= int
                         | "0x" hex
        encryptflag     := [boolean]
        sliceflag       := [boolean]
        boolean 		:= "true"
                         | "t"
                         | "false"
                         | "f"
        tlv				:= "(" [tag] "," [typevalue] ")" ("," [tlv])*
        tag				:= int
                         | "0x" hex
        typevalue		:= [type] [value]
                         | [tlv]
        type			:= "/"                                  # Empty tag
                         | "B"									# Byte (1 byte)
                         | "S"									# Short (2 bytes)
                         | "I"									# Integer (4 bytes)
                         | "b"                                  # Boolean
                         | "a"									# Array of bytes (in hex)
                         | "-"									# String
        value			:= [any]
     */

    public HuaweiPacket parseDebugString(String debugString) throws RequestCreationException {
        HuaweiPacket packet = new HuaweiPacket(paramsProvider);

        int current = 0;
        int nextComma = debugString.indexOf(',');

        if (nextComma < 1 || debugString.length() - current < 2)
            throw new RequestCreationException("Invalid debug command");

        if (debugString.charAt(current+1) == 'x')
            packet.serviceId = Short.valueOf(debugString.substring(current+2, nextComma), 16).byteValue();
        else
            packet.serviceId = Short.valueOf(debugString.substring(current, nextComma)).byteValue();

        current = nextComma + 1;
        nextComma = debugString.indexOf(',', current);

        if (nextComma < 1 || debugString.length() - current < 2)
            throw new RequestCreationException("Invalid debug command");

        if (debugString.charAt(current+1) == 'x')
            packet.commandId = Short.valueOf(debugString.substring(current+2, nextComma), 16).byteValue();
        else
            packet.commandId = Short.valueOf(debugString.substring(current, nextComma)).byteValue();

        current = nextComma + 1;
        nextComma = debugString.indexOf(',', current);

        if (nextComma < 1 || debugString.length() - current < 2)
            throw new RequestCreationException("Invalid debug command");

        switch (debugString.substring(current, nextComma)) {
            case "true":
            case "t":
                packet.setEncryption(true);
                break;
            case "false":
            case "f":
                packet.setEncryption(false);
                break;
            default:
                throw new RequestCreationException("Boolean is not a boolean");
        }

        current = nextComma + 1;
        nextComma = debugString.indexOf(',', current);
        if (debugString.length() - current < 1)
            throw new RequestCreationException("Invalid debug command");
        if (nextComma < 0)
            nextComma = debugString.length(); // For no TLVs

        switch (debugString.substring(current, nextComma)) {
            case "true":
            case "t":
                packet.setSliced(true);
                break;
            case "false":
            case "f":
                packet.setSliced(false);
                break;
            default:
                throw new RequestCreationException("Boolean is not a boolean");
        }

        current = nextComma + 1;

        if (current < debugString.length()) {
            HuaweiTlvParseReturn retv = parseTlv(debugString.substring(current));
            if (current + retv.parsedCount != debugString.length())
                throw new RequestCreationException("Invalid debug command");
            packet.setTlv(retv.tlv);
        }

        packet.complete = true;
        return packet;
    }

    private HuaweiTlvParseReturn parseTlv(String tlvString) throws RequestCreationException {
        HuaweiTLV tlv = new HuaweiTLV();
        int current = 0;
        int nextDelim;

        while (current < tlvString.length()) {
            if (tlvString.charAt(current) != '(')
                throw new RequestCreationException("Invalid debug command");

            current += 1;
            nextDelim = tlvString.indexOf(',', current);

            if (nextDelim < 1 || tlvString.length() - current < 2)
                throw new RequestCreationException("Invalid debug command");

            byte tag;
            // Short in between is because Java doesn't like unsigned numbers
            if (tlvString.charAt(current+1) == 'x')
                tag = Short.valueOf(tlvString.substring(current+2, nextDelim), 16).byteValue();
            else
                tag = Short.valueOf(tlvString.substring(current, nextDelim)).byteValue();

            current = nextDelim + 1;
            nextDelim = tlvString.indexOf(')', current);

            if (nextDelim < 1)
                throw new RequestCreationException("Invalid debug command");

            if (tlvString.charAt(current) != '(') {
                char type = tlvString.charAt(current);
                String value = tlvString.substring(current + 1, nextDelim);

                switch (type) {
                    case '/':
                        tlv.put(tag);
                        break;
                    case 'B':
                        tlv.put(tag, Byte.parseByte(value));
                        break;
                    case 'S':
                        tlv.put(tag, Short.parseShort(value));
                        break;
                    case 'I':
                        tlv.put(tag, Integer.parseInt(value));
                        break;
                    case 'b':
                        tlv.put(tag, value.equals("1"));
                        break;
                    case 'a':
                        tlv.put(tag, GB.hexStringToByteArray(value));
                        break;
                    case '-':
                        tlv.put(tag, value);
                        break;
                    default:
                        throw new RequestCreationException("Invalid tag type");
                }

                current = nextDelim + 1;
            } else {
                HuaweiTlvParseReturn retv = parseTlv(tlvString.substring(current));
                tlv.put(tag, retv.tlv);
                current += retv.parsedCount + 1;
            }

            if (current == tlvString.length())
                break;
            if (tlvString.charAt(current) == ')')
                break;
            if (tlvString.charAt(current) != ',')
                throw new RequestCreationException("Invalid debug command");

            current += 1;
        }

        return new HuaweiTlvParseReturn(tlv, current);
    }

    private static class HuaweiTlvParseReturn {
        public HuaweiTLV tlv;
        public Integer parsedCount;

        HuaweiTlvParseReturn(HuaweiTLV tlv, Integer parsedCount) {
            this.tlv = tlv;
            this.parsedCount = parsedCount;
        }
    }
}
