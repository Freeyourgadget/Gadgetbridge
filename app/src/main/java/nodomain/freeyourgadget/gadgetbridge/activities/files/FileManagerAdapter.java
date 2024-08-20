/*  Copyright (C) 2023-2024 akasaka / Genjitsu Labs

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
package nodomain.freeyourgadget.gadgetbridge.activities.files;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.util.AndroidUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class FileManagerAdapter extends RecyclerView.Adapter<FileManagerAdapter.FileManagerViewHolder> {
    protected static final Logger LOG = LoggerFactory.getLogger(FileManagerAdapter.class);

    private final List<File> fileList;
    private final Context mContext;

    public FileManagerAdapter(final Context context, final File directory) {
        mContext = context;

        // FIXME: This can be slow, make it async
        fileList = Arrays.asList(directory.listFiles());
        fileList.sort((f1, f2) -> {
            if (f1.isDirectory() && f2.isFile())
                return -1;
            if (f1.isFile() && f2.isDirectory())
                return 1;

            return String.CASE_INSENSITIVE_ORDER.compare(f1.getName(), f2.getName());
        });
    }

    @NonNull
    @Override
    public FileManagerViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        final View view = LayoutInflater.from(mContext).inflate(R.layout.item_file_manager, parent, false);
        return new FileManagerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final FileManagerViewHolder holder, int position) {
        final File file = fileList.get(position);

        holder.name.setText(file.getName());
        if (file.isDirectory()) {
            holder.icon.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_folder));
            holder.description.setVisibility(View.GONE);
            holder.menu.setVisibility(View.GONE);
        } else {
            holder.icon.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_file_open));
            holder.description.setText(formatFileSize(file.length()));
            holder.description.setVisibility(View.VISIBLE);
            holder.menu.setVisibility(View.VISIBLE);
            holder.menu.setOnClickListener(view -> {
                final PopupMenu menu = new PopupMenu(mContext, holder.menu);
                menu.inflate(R.menu.file_manager_file);
                menu.setOnMenuItemClickListener(item -> {
                    final int itemId = item.getItemId();
                    if (itemId == R.id.file_manager_file_menu_share) {
                        try {
                            AndroidUtils.shareFile(mContext, file, "*/*");
                        } catch (final IOException e) {
                            GB.toast("Failed to share file", Toast.LENGTH_LONG, GB.ERROR, e);
                        }
                        return true;
                    }

                    return false;
                });
                menu.show();
            });
        }

        holder.itemView.setOnClickListener(v -> {
            if (file.isDirectory()) {
                final Intent fileManagerIntent = new Intent(mContext, FileManagerActivity.class);
                fileManagerIntent.putExtra(FileManagerActivity.EXTRA_PATH, file.getPath());
                mContext.startActivity(fileManagerIntent);
            } else {
                try {
                    AndroidUtils.viewFile(file.getAbsolutePath(), "*/*", mContext);
                } catch (final IOException e) {
                    GB.toast("Failed to open file", Toast.LENGTH_LONG, GB.ERROR, e);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }

    public static class FileManagerViewHolder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView name;
        final TextView description;
        final ImageView menu;

        FileManagerViewHolder(View itemView) {
            super(itemView);

            icon = itemView.findViewById(R.id.file_icon);
            name = itemView.findViewById(R.id.file_name);
            description = itemView.findViewById(R.id.file_description);
            menu = itemView.findViewById(R.id.file_menu);
        }
    }

    private static final DecimalFormat SIZE_FORMAT = new DecimalFormat("#,##0.#");

    public static String formatFileSize(final long size) {
        if (size <= 0) return "0";
        final String[] units = new String[]{"B", "kB", "MB", "GB", "TB", "PB", "EB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return SIZE_FORMAT.format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}
