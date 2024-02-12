/*  Copyright (C) 2023-2024 Andreas Shimokawa, Yoran Vulker

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.model.Contact;
import nodomain.freeyourgadget.gadgetbridge.proto.xiaomi.XiaomiProto;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSupport;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class XiaomiPhonebookService extends AbstractXiaomiService {

    public static final Integer COMMAND_TYPE = 21;
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiPhonebookService.class.getSimpleName());

    private static final int CMD_GET_CONTACT = 2;
    private static final int CMD_GET_CONTACT_RESPONSE = 3;
    private static final int CMD_ADD_CONTACT_LIST = 5;
    private static final int CMD_SET_CONTACT_LIST = 7;

    public XiaomiPhonebookService(final XiaomiSupport support) {
        super(support);
    }

    @Override
    public void handleCommand(XiaomiProto.Command cmd) {
        if (cmd.getType() != COMMAND_TYPE) {
            throw new IllegalArgumentException("Not a phonebook command");
        }

        XiaomiProto.Phonebook payload = cmd.getPhonebook();

        if (payload == null) {
            LOG.warn("Received phonebook command without phonebook payload");
        }

        switch (cmd.getSubtype()) {
            case CMD_GET_CONTACT:
                if (payload == null || TextUtils.isEmpty(payload.getRequestedPhoneNumber())) {
                    LOG.error("Receive request for contact info without payload or requested phone number");
                    return;
                }

                handleContactRequest(payload.getRequestedPhoneNumber());
                return;
        }

        LOG.warn("Unhandled Phonebook command {}", cmd.getSubtype());
    }

    public void handleContactRequest(String phoneNumber) {
        LOG.debug("Received request for contact info for {}", phoneNumber);

        XiaomiProto.ContactInfo contact = getContactInfoForPhoneNumber(phoneNumber);

        getSupport().sendCommand(
                "send requested contact information",
                XiaomiProto.Command.newBuilder()
                        .setType(COMMAND_TYPE)
                        .setSubtype(CMD_GET_CONTACT_RESPONSE)
                        .setPhonebook(XiaomiProto.Phonebook.newBuilder().setContactInfo(contact))
                        .build()
        );
    }

    /**
     * Returns contact information from Android contact list
     *
     * @param number contact number
     * @return the contact display name, if found, otherwise the phone number
     */
    private XiaomiProto.ContactInfo getContactInfoForPhoneNumber(String number) {
        Context context = getSupport().getContext();
        String currentPrivacyMode = GBApplication.getPrefs().getString("pref_call_privacy_mode", GBApplication.getContext().getString(R.string.p_call_privacy_mode_off));

        // mask the display name if complete privacy is set in preferences
        if (currentPrivacyMode.equals(context.getString(R.string.p_call_privacy_mode_complete))) {
            return XiaomiProto.ContactInfo.newBuilder().setDisplayName("********").setPhoneNumber(number).build();
        }

        // send empty contact name if name privacy is set in preferences, as the device will show
        // the phone number instead
        if (currentPrivacyMode.equals(context.getString(R.string.p_call_privacy_mode_name))) {
            return XiaomiProto.ContactInfo.newBuilder().setDisplayName("").setPhoneNumber(number).build();
        }

        String name = "";

        // prevent lookup of null or empty phone number
        if (!TextUtils.isEmpty(number)) {
            // search contact's display name in Android contact list
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.ENTERPRISE_CONTENT_FILTER_URI, Uri.encode(number));

            try (Cursor contactLookup = getSupport().getContext().getContentResolver().query(uri, new String[] { ContactsContract.Data.DISPLAY_NAME}, null, null, null)) {
                if (contactLookup != null && contactLookup.getCount() > 0) {
                    contactLookup.moveToNext();
                    name = contactLookup.getString(0);
                }
            } catch (SecurityException e) {
                // ignore, just return name below
            }
        }

        XiaomiProto.ContactInfo.Builder contactInfoBuilder = XiaomiProto.ContactInfo.newBuilder();

        // prevent the number from getting displayed if an empty contact name was retrieved from the
        // contact list
        if (TextUtils.isEmpty(name) && currentPrivacyMode.equals(context.getString(R.string.p_call_privacy_mode_number))) {
            name = "********";
        }

        contactInfoBuilder.setPhoneNumber(number);
        contactInfoBuilder.setDisplayName(name);
        return contactInfoBuilder.build();
    }

    public void setContacts(List<Contact> contacts) {
        final XiaomiProto.ContactList.Builder contactList = XiaomiProto.ContactList.newBuilder();
        int maxContacts = 10; // TODO:verify, do not copy and paste
        int numContacts = Math.min(contacts.size(), maxContacts);

        for (int i = 0; i < numContacts; i++) {
            final Contact contact = contacts.get(i);
            if (!StringUtils.isNullOrEmpty(contact.getName()) && !StringUtils.isNullOrEmpty(contact.getNumber())) {
                contactList.addContactInfo(XiaomiProto.ContactInfo.newBuilder().setDisplayName(contact.getName()).setPhoneNumber(contact.getNumber()));
            }
        }

        getSupport().sendCommand(
                "send contact list",
                XiaomiProto.Command.newBuilder()
                        .setType(COMMAND_TYPE)
                        .setSubtype(CMD_SET_CONTACT_LIST)
                        .setPhonebook(XiaomiProto.Phonebook.newBuilder().setContactList(contactList))
                        .build()
        );
    }
}
