/*  Copyright (C) 2023 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.model.Contact;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Support;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.AbstractZeppOsService;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class ZeppOsContactsService extends AbstractZeppOsService {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsContactsService.class);

    private static final short ENDPOINT = 0x0014;

    public static final byte CMD_CAPABILITIES_REQUEST = 0x01;
    public static final byte CMD_CAPABILITIES_RESPONSE = 0x02;
    public static final byte CMD_SET_LIST = 0x07;
    public static final byte CMD_SET_LIST_ACK = 0x08;

    private int version = 0;
    private int maxContacts = 0;

    public static final String PREF_CONTACTS_SLOT_COUNT = "zepp_os_contacts_slot_count";

    public ZeppOsContactsService(final Huami2021Support support) {
        super(support);
    }

    @Override
    public short getEndpoint() {
        return ENDPOINT;
    }

    @Override
    public boolean isEncrypted() {
        return true;
    }

    @Override
    public void handlePayload(final byte[] payload) {
        switch (payload[0]) {
            case CMD_CAPABILITIES_RESPONSE:
                version = payload[1];
                if (version != 1) {
                    LOG.warn("Unsupported contacts service version {}", version);
                    return;
                }
                maxContacts = BLETypeConversions.toUint16(payload, 2);
                LOG.info("Contacts version={}, maxContacts={}", version, maxContacts);
                getSupport().evaluateGBDeviceEvent(new GBDeviceEventUpdatePreferences(PREF_CONTACTS_SLOT_COUNT, maxContacts));
                break;
            case CMD_SET_LIST_ACK:
                LOG.info("Got contacts set list ack, status = {}", payload[1]);
                break;
            default:
                LOG.warn("Unexpected contacts byte {}", String.format("0x%02x", payload[0]));
        }
    }

    @Override
    public void initialize(final TransactionBuilder builder) {
        requestCapabilities(builder);
    }

    public void requestCapabilities(final TransactionBuilder builder) {
        write(builder, CMD_CAPABILITIES_REQUEST);
    }

    public void setContacts(final List<Contact> contacts) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        if (contacts.size() > maxContacts) {
            LOG.warn("Number of contacts {} larger than max contacts {}, list will be truncated", contacts.size(), maxContacts);
        }

        int numContacts = Math.min(contacts.size(), maxContacts);

        try {
            baos.write(CMD_SET_LIST);
            baos.write(BLETypeConversions.fromUint16(numContacts));
            for (int i = 0; i < numContacts; i++) {
                final Contact contact = contacts.get(i);
                if (!StringUtils.isNullOrEmpty(contact.getName())) {
                    baos.write(contact.getName().getBytes(StandardCharsets.UTF_8));
                }
                baos.write(0);
                if (!StringUtils.isNullOrEmpty(contact.getNumber())) {
                    baos.write(contact.getNumber().getBytes(StandardCharsets.UTF_8));
                }
                baos.write(0);
            }
        } catch (final Exception e) {
            LOG.error("Failed to create command", e);
            return;
        }

        write("set contacts", baos.toByteArray());
    }
}
