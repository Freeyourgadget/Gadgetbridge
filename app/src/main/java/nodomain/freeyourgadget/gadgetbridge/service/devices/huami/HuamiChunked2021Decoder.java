/*  Copyright (C) 2022 Andreas Shimokawa

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventNotificationControl;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.util.CryptoUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class HuamiChunked2021Decoder {
    private static final Logger LOG = LoggerFactory.getLogger(HuamiChunked2021Decoder.class);
    private Byte currentHandle;
    private int currentType;
    private int currentLength;
    ByteBuffer reassemblyBuffer;
    private final HuamiSupport huamiSupport;

    public HuamiChunked2021Decoder(HuamiSupport huamiSupport) {
        this.huamiSupport = huamiSupport;
    }


    public byte[] decode(byte[] data) {
        int i = 0;
        if (data[i++] != 0x03) {
            return null;
        }
        boolean encrypted = false;
        byte flags = data[i++];
        if ((flags & 0x08) == 0x08) {
            encrypted = true;
        }
        if (huamiSupport.force2021Protocol) {
            i++; // skip extended header
        }
        byte handle = data[i++];
        if (currentHandle != null && currentHandle != handle) {
            LOG.warn("ignoring handle " + handle + ", expected " + currentHandle);
            return null;
        }
        byte count = data[i++];
        if ((flags & 0x01) == 0x01) { // beginning
            int full_length = (data[i++] & 0xff) | ((data[i++] & 0xff) << 8) | ((data[i++] & 0xff) << 16) | ((data[i++] & 0xff) << 24);
            currentLength = full_length;
            if (encrypted) {
                int encrypted_length = full_length + 8;
                int overflow = encrypted_length % 16;
                if (overflow > 0) {
                    encrypted_length += (16 - overflow);
                }
                full_length = encrypted_length;
            }
            reassemblyBuffer = ByteBuffer.allocate(full_length);
            currentType = (data[i++] & 0xff) | ((data[i++] & 0xff) << 8);
            currentHandle = handle;
        }
        reassemblyBuffer.put(data, i, data.length - i);
        if ((flags & 0x02) == 0x02) { // end
            byte[] buf = reassemblyBuffer.array();
            if (encrypted) {
                byte[] messagekey = new byte[16];
                for (int j = 0; j < 16; j++) {
                    messagekey[j] = (byte) (huamiSupport.sharedSessionKey[j] ^ handle);
                }
                try {
                    buf = CryptoUtils.decryptAES(buf, messagekey);
                    buf = ArrayUtils.subarray(buf, 0, currentLength);
                    LOG.info("decrypted data: " + GB.hexdump(buf));
                } catch (Exception e) {
                    LOG.warn("error decrypting " + e);
                    return null;
                }
            }
            if (currentType == HuamiService.CHUNKED2021_ENDPOINT_COMPAT) {
                LOG.info("got configuration data");
                currentHandle = null;
                currentType = 0;
                return ArrayUtils.remove(buf, 0);
            }
            if (currentType == HuamiService.CHUNKED2021_ENDPOINT_SMSREPLY && false) { // unsafe for now, disabled, also we shoud return somehing and then parse in HuamiSupport instead of firing stuff here
                LOG.debug("got command for SMS reply");
                if (buf[0] == 0x0d) {
                    try {
                        TransactionBuilder builder = huamiSupport.performInitialized("allow sms reply");
                        huamiSupport.writeToChunked2021(builder, (short) 0x0013, huamiSupport.getNextHandle(), new byte[]{(byte) 0x0e, 0x01}, huamiSupport.force2021Protocol, false);
                        builder.queue(huamiSupport.getQueue());
                    } catch (IOException e) {
                        LOG.error("Unable to allow sms reply");
                    }
                } else if (buf[0] == 0x0b) {
                    String phoneNumber = null;
                    String smsReply = null;
                    for (i = 1; i < buf.length; i++) {
                        if (buf[i] == 0) {
                            phoneNumber = new String(buf, 1, i - 1);
                            // there are four unknown bytes between caller and reply
                            smsReply = new String(buf, i + 5, buf.length - i - 6);
                            break;
                        }
                    }
                    if (phoneNumber != null && !phoneNumber.isEmpty()) {
                        LOG.debug("will send message '" + smsReply + "' to number '" + phoneNumber + "'");
                        GBDeviceEventNotificationControl devEvtNotificationControl = new GBDeviceEventNotificationControl();
                        devEvtNotificationControl.handle = -1;
                        devEvtNotificationControl.phoneNumber = phoneNumber;
                        devEvtNotificationControl.reply = smsReply;
                        devEvtNotificationControl.event = GBDeviceEventNotificationControl.Event.REPLY;
                        huamiSupport.evaluateGBDeviceEvent(devEvtNotificationControl);
                        try {
                            TransactionBuilder builder = huamiSupport.performInitialized("ack sms reply");
                            byte[] ackSentCommand = new byte[]{0x0c, 0x01};
                            huamiSupport.writeToChunked2021(builder, (short) 0x0013, huamiSupport.getNextHandle(), ackSentCommand, huamiSupport.force2021Protocol, false);
                            builder.queue(huamiSupport.getQueue());
                        } catch (IOException e) {
                            LOG.error("Unable to ack sms reply");
                        }
                    }
                }
            }
            currentHandle = null;
            currentType = 0;
        }
        return null;
    }
}
