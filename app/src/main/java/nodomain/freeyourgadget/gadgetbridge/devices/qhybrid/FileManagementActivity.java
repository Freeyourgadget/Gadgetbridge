/*  Copyright (C) 2020-2021 Daniel Dakhno

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
package nodomain.freeyourgadget.gadgetbridge.devices.qhybrid;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.FileInputStream;
import java.io.IOException;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.file.FileHandle;
import nodomain.freeyourgadget.gadgetbridge.util.AndroidUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class FileManagementActivity extends AbstractGBActivity implements View.OnClickListener {
    private final int REQUEST_CODE_PICK_UPLOAD_FILE = 0;

    private Spinner fileTypesSpinner;
    private Switch encryptedFile;
    private boolean generateFileHeader = false;

    private boolean warningDisplayed = false;

    BroadcastReceiver fileResultReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(QHybridSupport.QHYBRID_ACTION_DOWNLOADED_FILE)) {
                boolean success = intent.getBooleanExtra("EXTRA_SUCCESS", false);
                if (!success) {
                    Toast.makeText(FileManagementActivity.this, "error downloading file, check logcat", Toast.LENGTH_LONG).show();
                    return;
                }
                String path = intent.getStringExtra("EXTRA_PATH");
                Toast.makeText(FileManagementActivity.this, "downloaded file " + path, Toast.LENGTH_LONG).show();
            }else if(intent.getAction().equals(QHybridSupport.QHYBRID_ACTION_UPLOADED_FILE)) {
                boolean success = intent.getBooleanExtra("EXTRA_SUCCESS", false);
                if (!success) {
                    Toast.makeText(FileManagementActivity.this, "error uploading file, check logcat", Toast.LENGTH_LONG).show();
                    return;
                }
                Toast.makeText(FileManagementActivity.this, "uploaded file", Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qhybrid_file_management);

        initViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(QHybridSupport.QHYBRID_ACTION_DOWNLOADED_FILE);
        filter.addAction(QHybridSupport.QHYBRID_ACTION_UPLOADED_FILE);
        LocalBroadcastManager.getInstance(this).registerReceiver(fileResultReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(fileResultReceiver);
    }

    private void initViews() {
        FileHandle[] handles = FileHandle.values();
        fileTypesSpinner = findViewById(R.id.qhybrid_file_types);
        fileTypesSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, handles));

        encryptedFile = findViewById(R.id.qhybrid_switch_encrypted_file);

        findViewById(R.id.qhybrid_button_download_file).setOnClickListener(this);
        findViewById(R.id.qhybrid_button_upload_file).setOnClickListener(this);

        ((Switch) findViewById(R.id.sqhybrid_switch_generate_file_header)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                generateFileHeader = isChecked;
                fileTypesSpinner.setClickable(isChecked);
                fileTypesSpinner.setAlpha(isChecked ? 1f : 0.2f);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_CODE_PICK_UPLOAD_FILE) return;
        if (resultCode != RESULT_OK) return;
        String fileName;
        try {
            fileName = AndroidUtils.getFilePath(this, data.getData());
            if (fileName == null) {
                return;
            }
        } catch (IllegalArgumentException e) {
            GB.toast("please choose a local file", Toast.LENGTH_LONG, GB.WARN);
            return;
        }

        try {
            if (!warningDisplayed) {
                FileInputStream fis = new FileInputStream(fileName);
                short fileHandle = (short) (fis.read() | (fis.read() << 8));
                fis.close();

                boolean handleFound = FileHandle.fromHandle(fileHandle) != null;
                if (handleFound == generateFileHeader) {
                    warningDisplayed = true;
                    String text = "File seems to contain file handle. Are you sure you want to generate a potentially already existing header?";
                    if (!handleFound)
                        text = "File does not start with a known handle. Are you sure the header is already generated?";
                    text += " Repeat to continue anyway.";
                    new AlertDialog.Builder(this)
                            .setTitle("warning")
                            .setMessage(text)
                            .setPositiveButton("ok", null)
                            .show();
                    return;
                }
            }

            Intent callIntent = new Intent(QHybridSupport.QHYBRID_COMMAND_UPLOAD_FILE);
            callIntent.putExtra("EXTRA_HANDLE", (FileHandle) fileTypesSpinner.getSelectedItem());
            callIntent.putExtra("EXTRA_ENCRYPTED", encryptedFile.isChecked());
            callIntent.putExtra("EXTRA_GENERATE_FILE_HEADER", generateFileHeader);
            callIntent.putExtra("EXTRA_PATH", AndroidUtils.getFilePath(this, data.getData()));
            // callIntent.setData(data.getData());

            LocalBroadcastManager.getInstance(this).sendBroadcast(callIntent);
        } catch (IOException e) {
            GB.toast("cannot open file", Toast.LENGTH_LONG, GB.ERROR, e);
        }
    }

    @Override
    public void onClick(View v) {
        boolean isEncrypted = encryptedFile.isChecked();

        if (v.getId() == R.id.qhybrid_button_download_file) {
            Intent fileIntent = new Intent();
            fileIntent.putExtra("EXTRA_ENCRYPTED", isEncrypted);
            fileIntent.setAction(QHybridSupport.QHYBRID_COMMAND_DOWNLOAD_FILE);
            fileIntent.putExtra("EXTRA_HANDLE", (FileHandle) fileTypesSpinner.getSelectedItem());
            LocalBroadcastManager.getInstance(this).sendBroadcast(fileIntent);
        } else if (v.getId() == R.id.qhybrid_button_upload_file) {
            Intent chooserIntent = new Intent()
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("*/*")
                    .setAction(Intent.ACTION_GET_CONTENT);

            Intent intent = Intent.createChooser(chooserIntent, "Select a file");

            startActivityForResult(intent, REQUEST_CODE_PICK_UPLOAD_FILE);
        }
    }
}