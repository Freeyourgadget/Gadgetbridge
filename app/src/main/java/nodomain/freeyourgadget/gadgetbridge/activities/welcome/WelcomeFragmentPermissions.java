/*  Copyright (C) 2024 Arjan Schrijver

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
package nodomain.freeyourgadget.gadgetbridge.activities.welcome;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.util.PermissionsUtils;

public class WelcomeFragmentPermissions extends Fragment {
    private static final Logger LOG = LoggerFactory.getLogger(WelcomeFragmentPermissions.class);

    private RecyclerView permissionsListView;
    private PermissionAdapter permissionAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_welcome_permissions, container, false);

        Button requestAllButton = view.findViewById(R.id.button_request_all);
        requestAllButton.setOnClickListener(v -> PermissionsUtils.requestAllPermissions(requireActivity()));

        // Initialize RecyclerView and data
        permissionsListView = view.findViewById(R.id.permissions_list);

        // Set up RecyclerView
        permissionAdapter = new PermissionAdapter(PermissionsUtils.getRequiredPermissionsList(requireActivity()), requireContext());
        permissionsListView.setLayoutManager(new LinearLayoutManager(requireContext()));
        permissionsListView.setAdapter(permissionAdapter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        permissionAdapter.notifyDataSetChanged();
    }

    private class PermissionHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView summaryTextView;
        ImageView checkmarkImageView;
        Button requestButton;

        public PermissionHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.permission_title);
            summaryTextView = itemView.findViewById(R.id.permission_summary);
            checkmarkImageView = itemView.findViewById(R.id.permission_check);
            requestButton = itemView.findViewById(R.id.permission_request);
        }
    }

    private class PermissionAdapter extends RecyclerView.Adapter<PermissionHolder> {
        private List<PermissionsUtils.PermissionDetails> permissionList;
        private Context context;

        public PermissionAdapter(List<PermissionsUtils.PermissionDetails> permissionList, Context context) {
            this.permissionList = permissionList;
            this.context = context;
        }

        @NonNull
        @Override
        public PermissionHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.fragment_welcome_permission_row, parent, false);
            return new PermissionHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull PermissionHolder holder, int position) {
            PermissionsUtils.PermissionDetails permissionData = permissionList.get(position);
            holder.titleTextView.setText(permissionData.getTitle());
            holder.summaryTextView.setText(permissionData.getSummary());
            if (PermissionsUtils.checkPermission(requireContext(), permissionData.getPermission())) {
                holder.requestButton.setVisibility(View.INVISIBLE);
                holder.requestButton.setEnabled(false);
                holder.checkmarkImageView.setVisibility(View.VISIBLE);
            } else {
                holder.requestButton.setOnClickListener(view -> {
                    PermissionsUtils.requestPermission(requireActivity(), permissionData.getPermission());
                });
            }
        }

        @Override
        public int getItemCount() {
            return permissionList.size();
        }
    }
}
