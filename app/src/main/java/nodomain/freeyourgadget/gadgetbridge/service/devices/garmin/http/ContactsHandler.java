package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminPreferences;
import nodomain.freeyourgadget.gadgetbridge.entities.Contact;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GarminContacts;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminSupport;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class ContactsHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ContactsHandler.class);

    private final GarminSupport deviceSupport;

    public ContactsHandler(final GarminSupport deviceSupport) {
        this.deviceSupport = deviceSupport;
    }

    public GarminHttpResponse handleRequest(final GarminHttpRequest request) {
        if (!request.getPath().equals("/device-gateway/usercontact/contacts")) {
            LOG.warn("Unknown contacts path {}", request.getPath());
            return null;
        }

        if (!"application/octet-stream".equals(request.getHeaders().get("accept"))) {
            // We only support protobuf replies
            LOG.warn("Requested contacts content type '{}' not supported", request.getHeaders().get("accept"));
            return null;
        }

        // Mark contacts as supported
        final Prefs devicePrefs = deviceSupport.getDevicePrefs();
        devicePrefs.getPreferences().edit()
                .putBoolean(GarminPreferences.PREF_FEAT_CONTACTS, true)
                .apply();

        final GarminContacts.Response.Builder responseContacts = GarminContacts.Response.newBuilder();

        final List<Contact> contacts = DBHelper.getContacts(deviceSupport.getDevice());
        for (final Contact contact : contacts) {
            final GarminContacts.Contact.Builder responseContact = GarminContacts.Contact.newBuilder()
                    .setId(randomHex(32).toUpperCase(Locale.ROOT))
                    .setFullName(contact.getName())
                    .setFirstName(contact.getName())
                    .setLastName("")
                    .addPhone(
                            GarminContacts.Phone.newBuilder()
                                    .setNumber(contact.getNumber())
                                    .setUnk2(1)
                                    .setUnk3(1)
                                    .setUnk4(0)
                                    .setUnk5(new Random().nextInt(65535) + 65535)
                                    .setUnk6(0)
                                    .setUnk7(0)
                                    .setUnk8("")
                                    .setUnk9(1)
                                    .setId(UUID.randomUUID().toString())
                                    .setUnk11(1)
                                    .addUnk12(GarminContacts.Unk12.newBuilder().setUnk1(1).setUnk2(5))
                                    .addUnk12(GarminContacts.Unk12.newBuilder().setUnk1(2).setUnk2(5))
                    )
                    .setUnk8(0)
                    .setUnk9(0)
                    .setUpdateTime(System.currentTimeMillis())
                    .setUnk12(0)
                    .setUnk21(0);

            responseContacts.addContact(responseContact);
        }

        final ActivityUser activityUser = new ActivityUser();
        final GarminContacts.Contact.Builder self = GarminContacts.Contact.newBuilder()
                .setId("SELF")
                .setFullName(activityUser.getName())
                .setFirstName(activityUser.getName())
                .setLastName("")
                .setUnk7("")
                .setUnk8(0)
                .setUnk9(0)
                .setUnk10(0)
                .setUnk12(0)
                .setUnk21(0)
                .setUpdateTime(System.currentTimeMillis());

        responseContacts.setSelf(self);

        final GarminHttpResponse response = new GarminHttpResponse();
        response.setStatus(200);
        response.getHeaders().put("Content-Type", "application/octet-stream");
        response.setBody(responseContacts.build().toByteArray());
        return response;
    }

    public static String randomHex(final int numChars) {
        final Random r = new Random();
        final StringBuilder sb = new StringBuilder();
        while (sb.length() < numChars) {
            sb.append(Integer.toHexString(r.nextInt()));
        }
        return sb.toString().substring(0, numChars);
    }
}
