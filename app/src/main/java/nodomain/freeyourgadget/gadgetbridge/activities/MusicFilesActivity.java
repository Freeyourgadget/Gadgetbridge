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

import android.content.res.AssetFileDescriptor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;

public class MusicFilesActivity extends AbstractGBActivity {
    private static final Logger LOG = LoggerFactory.getLogger(MusicFilesActivity.class);

    private GBDevice gbDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gbDevice = getIntent().getParcelableExtra(GBDevice.EXTRA_DEVICE);
        setContentView(R.layout.activity_music_files);

        final ActivityResultLauncher<String> activityResultLauncher = this.registerForActivityResult(
                new ActivityResultContracts.GetMultipleContents(),
                urilist -> {
                    final MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();

                    LOG.info("Got {}", urilist);

                    for (final Uri uri : urilist) {
                        mediaMetadataRetriever.setDataSource(MusicFilesActivity.this, uri);

                        final String title = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                        final String artist = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                        final String album = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);

                        long fileSize;
                        try (AssetFileDescriptor fileDescriptor = getApplicationContext().getContentResolver().openAssetFileDescriptor(uri , "r")){
                            fileSize = fileDescriptor.getLength();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                        final JSONObject jsonObject = new JSONObject();
                        try {
                            jsonObject.put("title", title);
                            jsonObject.put("album", album);
                            jsonObject.put("artist", artist);
                            jsonObject.put("size", fileSize);
                        } catch (final JSONException e) {
                            throw new RuntimeException(e);
                        }

                        final String md5;
                        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
                            md5 = FileUtils.md5sum(inputStream);
                        } catch (final IOException e) {
                            throw new RuntimeException(e);
                        }

                        LOG.info("{}: {} {}", uri, md5, jsonObject);
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        mediaMetadataRetriever.close();
                    }
                }
        );

        final FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> {
            activityResultLauncher.launch("audio/*");
        });
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // back button
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
