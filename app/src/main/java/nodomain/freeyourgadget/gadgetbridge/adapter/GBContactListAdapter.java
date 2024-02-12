/*  Copyright (C) 2023-2024 Arjan Schrijver, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.adapter;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.ConfigureContacts;
import nodomain.freeyourgadget.gadgetbridge.entities.Contact;

/**
 * Adapter for displaying Contact instances.
 */
public class GBContactListAdapter extends RecyclerView.Adapter<GBContactListAdapter.ViewHolder> {

    private final Context mContext;
    private ArrayList<Contact> contactList;

    public GBContactListAdapter(Context context) {
        this.mContext = context;
    }

    public void setContactList(List<Contact> contacts) {
        this.contactList = new ArrayList<>(contacts);
    }

    public ArrayList<Contact> getContactList() {
        return contactList;
    }

    @NonNull
    @Override
    public GBContactListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        final Contact contact = contactList.get(position);

        holder.container.setOnClickListener(v -> ((ConfigureContacts) mContext).configureContact(contact));

        holder.container.setOnLongClickListener(v -> {
            new MaterialAlertDialogBuilder(v.getContext())
                    .setTitle(R.string.contact_delete_confirm_title)
                    .setMessage(mContext.getString(R.string.contact_delete_confirm_description, contact.getName()))
                    .setIcon(R.drawable.ic_warning)
                    .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                        ((ConfigureContacts) mContext).deleteContact(contact);
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .show();

            return true;
        });

        holder.contactName.setText(contact.getName());
        holder.contactNumber.setText(contact.getNumber());
    }

    @Override
    public int getItemCount() {
        return contactList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView container;

        final TextView contactName;
        final TextView contactNumber;

        ViewHolder(View view) {
            super(view);

            container = view.findViewById(R.id.card_contact);

            contactName = view.findViewById(R.id.contact_item_name);
            contactNumber = view.findViewById(R.id.contact_item_phone_number);
        }
    }
}
