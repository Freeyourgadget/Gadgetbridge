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
package nodomain.freeyourgadget.gadgetbridge.activities;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.entities.Contact;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class ContactDetails extends AbstractGBActivity {
    private static final Logger LOG = LoggerFactory.getLogger(ContactDetails.class);

    private Contact contact;
    private GBDevice device;

    EditText contactName;
    EditText contactNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_details);

        contact = (Contact) getIntent().getSerializableExtra(Contact.EXTRA_CONTACT);

        if (contact == null) {
            GB.toast("No contact provided to ContactDetails Activity", Toast.LENGTH_LONG, GB.ERROR);
            finish();
            return;
        }

        contactName = findViewById(R.id.contact_name);
        contactNumber = findViewById(R.id.contact_number);

        device = getIntent().getParcelableExtra(GBDevice.EXTRA_DEVICE);
        final DeviceCoordinator coordinator = device.getDeviceCoordinator();

        contactName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(final CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(final Editable s) {
                contact.setName(s.toString());
            }
        });

        contactNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(final CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(final Editable s) {
                contact.setNumber(s.toString());
            }
        });

        final FloatingActionButton fab = findViewById(R.id.fab_save);
        fab.setOnClickListener(view -> {
            if (StringUtils.isNullOrEmpty(contact.getName())) {
                GB.toast(getBaseContext().getString(R.string.contact_missing_name), Toast.LENGTH_LONG, GB.WARN);
                return;
            }

            if (StringUtils.isNullOrEmpty(contact.getNumber())) {
                GB.toast(getBaseContext().getString(R.string.contact_missing_number), Toast.LENGTH_LONG, GB.WARN);
                return;
            }

            updateContact();
            ContactDetails.this.setResult(Activity.RESULT_OK);
            finish();
        });

        updateUiFromContact();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // back button
                // TODO confirm when exiting without saving
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateContact() {
        DBHelper.store(contact);
    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle state) {
        super.onSaveInstanceState(state);
        state.putSerializable("contact", contact);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        contact = (Contact) savedInstanceState.getSerializable("contact");
        updateUiFromContact();
    }

    public void updateUiFromContact() {
        contactName.setText(contact.getName());
        contactNumber.setText(contact.getNumber());
    }
}
