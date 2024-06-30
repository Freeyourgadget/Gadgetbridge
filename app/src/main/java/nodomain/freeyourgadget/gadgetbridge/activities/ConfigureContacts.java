/*  Copyright (C) 2023-2024 Arjan Schrijver, Daniel Dakhno, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.adapter.GBContactListAdapter;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.entities.Contact;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;


public class ConfigureContacts extends AbstractGBActivity {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigureContacts.class);

    private GBContactListAdapter mGBContactListAdapter;
    private GBDevice gbDevice;

    private ActivityResultLauncher<Intent> configureContactLauncher;
    private final ActivityResultCallback<ActivityResult> configureContactCallback = result -> {
        if (result.getResultCode() == Activity.RESULT_OK) {
            updateContactsFromDB();
            sendContactsToDevice();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_configure_contacts);

        gbDevice = getIntent().getParcelableExtra(GBDevice.EXTRA_DEVICE);

        configureContactLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                configureContactCallback
        );

        mGBContactListAdapter = new GBContactListAdapter(this);

        final RecyclerView contactsRecyclerView = findViewById(R.id.contact_list);
        contactsRecyclerView.setHasFixedSize(true);
        contactsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        contactsRecyclerView.setAdapter(mGBContactListAdapter);
        updateContactsFromDB();

        final FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> {
            final DeviceCoordinator coordinator = gbDevice.getDeviceCoordinator();

            int deviceSlots = coordinator.getContactsSlotCount(gbDevice);

            if (mGBContactListAdapter.getItemCount() >= deviceSlots) {
                // No more free slots
                new MaterialAlertDialogBuilder(v.getContext())
                        .setTitle(R.string.reminder_no_free_slots_title)
                        .setMessage(getBaseContext().getString(R.string.contact_no_free_slots_description, String.format(Locale.getDefault(), "%d", deviceSlots)))
                        .setIcon(R.drawable.ic_warning)
                        .setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {
                        })
                        .show();
                return;
            }

            final Contact contact;
            try (DBHandler db = GBApplication.acquireDB()) {
                final DaoSession daoSession = db.getDaoSession();
                final Device device = DBHelper.getDevice(gbDevice, daoSession);
                final User user = DBHelper.getUser(daoSession);
                contact = createDefaultContact(device, user);
            } catch (final Exception e) {
                LOG.error("Error accessing database", e);
                return;
            }

            configureContact(contact);
        });
    }

    private Contact createDefaultContact(@NonNull Device device, @NonNull User user) {
        final Contact contact = new Contact();
        contact.setName("");
        contact.setNumber("");
        contact.setDeviceId(device.getId());
        contact.setUserId(user.getId());
        contact.setContactId(UUID.randomUUID().toString());

        return contact;
    }

    /**
     * Reads the available contacts from the database and updates the view afterwards.
     */
    private void updateContactsFromDB() {
        final List<Contact> contacts = DBHelper.getContacts(gbDevice);

        mGBContactListAdapter.setContactList(contacts);
        mGBContactListAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            // back button
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void configureContact(final Contact contact) {
        final Intent startIntent = new Intent(getApplicationContext(), ContactDetails.class);
        startIntent.putExtra(GBDevice.EXTRA_DEVICE, gbDevice);
        startIntent.putExtra(Contact.EXTRA_CONTACT, contact);
        configureContactLauncher.launch(startIntent);
    }

    public void deleteContact(final Contact contact) {
        DBHelper.delete(contact);
        updateContactsFromDB();
        sendContactsToDevice();
    }

    private void sendContactsToDevice() {
        if (gbDevice.isInitialized()) {
            GBApplication.deviceService(gbDevice).onSetContacts(mGBContactListAdapter.getContactList());
        }
    }
}
