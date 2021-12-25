/*  Copyright (C) 2015-2020 abettenburg, Andreas Shimokawa, Carsten Pfeiffer,
    Daniele Gobbetti, Lem Dulfo

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

import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;

public class GpxReceiverActivity extends AbstractGBActivity {
    private static final Logger LOG = LoggerFactory.getLogger(ActivitySummaryDetail.class);
    boolean toOverwrite = false;
    ArrayList<FileToProcess> fileList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gpx_receiver);
        Button gpx_receiver_ok = findViewById(R.id.gpx_receiver_ok);
        Button gpx_receiver_cancel = findViewById(R.id.gpx_receiver_cancel);
        View gpx_receiver_overwrite_label = findViewById(R.id.gpx_receiver_overwrite_label);
        TextView gpx_receiver_files_listing = findViewById(R.id.gpx_receiver_files_listing);
        TextView gpx_receiver_received_label = findViewById(R.id.gpx_receiver_received_label);

        gpx_receiver_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        gpx_receiver_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for (FileToProcess fileToProcess : fileList) {
                    save_file(fileToProcess.source, fileToProcess.destination);
                }
                finish();
            }
        });

        final Intent intent = getIntent();
        final ClipData intentClipData = intent.getClipData();
        StringBuilder fileListingText = new StringBuilder();
        ArrayList<Uri> documentUris = new ArrayList<>();

        if (savedInstanceState == null) {
            if (intent.getData() != null) {
                documentUris.add(intent.getData());
            } else {
                if (intentClipData != null && intentClipData.getItemCount() > 0) {
                    for (int i = 0; i < intentClipData.getItemCount(); i++) {
                        documentUris.add(intentClipData.getItemAt(i).getUri());
                    }
                }
            }
        }

        if (documentUris != null) {
            for (Uri uri : documentUris) {
                if (uri.getPath().toLowerCase().endsWith(".gpx")) {
                    FileToProcess file = new FileToProcess(uri);
                    fileList.add(file);
                    fileListingText.append(String.format("%s %s\n\n", file.name, file.exists ? getString(R.string.dbmanagementactivity_overwrite) : ""));
                }
            }
        }

        if (toOverwrite) {
            gpx_receiver_overwrite_label.setVisibility(View.VISIBLE);
        } else {
            gpx_receiver_overwrite_label.setVisibility(View.GONE);
        }

        gpx_receiver_received_label.setText(String.format("%s %s", getString(R.string.gpx_receiver_files_received), fileList.toArray().length));
        gpx_receiver_files_listing.setText(fileListingText.toString());
    }

    private String get_file_name(Uri source) {
        int cut = source.getPath().lastIndexOf("/");
        String fileName = null;
        if (cut != -1) {
            fileName = source.getPath().substring(cut + 1);
        }
        return fileName;
    }

    private File create_file_from_uri(Uri source) {
        File destination = null;
        try {
            File external = FileUtils.getExternalFilesDir();
            String fileName = get_file_name(source);

            if (fileName != null) {
                destination = new File(external + "/" + fileName);
            }
        } catch (IOException exception) {
            LOG.error("Error creating file", exception);
        }
        return destination;
    }

    private void save_file(Uri source, File destination) {
        try {
            String fileName = get_file_name(source);
            if (fileName != null) {
                FileUtils.copyURItoFile(GpxReceiverActivity.this, source, destination);
            }

        } catch (IOException exception) {
            LOG.error("Error copying file", exception);
        }
    }

    private class FileToProcess {
        String name;
        Uri source;
        File destination;
        boolean exists;

        FileToProcess(Uri file) {
            this.name = get_file_name(file);
            this.source = file;
            this.destination = create_file_from_uri(file);
            this.exists = this.destination.exists();

            if (this.exists) {
                toOverwrite = true;
            }
        }
    }
}



