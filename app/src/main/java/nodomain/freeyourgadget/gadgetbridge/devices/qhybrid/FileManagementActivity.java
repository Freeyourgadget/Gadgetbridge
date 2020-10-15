package nodomain.freeyourgadget.gadgetbridge.devices.qhybrid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ExpandableListView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.Serializable;
import java.nio.file.attribute.FileTime;

import no.nordicsemi.android.dfu.FileType;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.file.FileHandle;

public class FileManagementActivity extends AbstractGBActivity implements View.OnClickListener {
    private final int REQUEST_CODE_PICK_UPLOAD_FILE = 0;

    private Spinner fileTypesSpinner;
    private Switch encryptedFile;

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
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode != REQUEST_CODE_PICK_UPLOAD_FILE) return;
        if(resultCode != RESULT_OK) return;

        Intent callIntent = new Intent(QHybridSupport.QHYBRID_COMMAND_UPLOAD_FILE);
        callIntent.putExtra("EXTRA_HANDLE", (FileHandle) fileTypesSpinner.getSelectedItem());
        callIntent.putExtra("EXTRA_ENCRYPTED", encryptedFile.isChecked());
        callIntent.putExtra("EXTRA_PATH", data.getData().getPath());
        // callIntent.setData(data.getData());

        LocalBroadcastManager.getInstance(this).sendBroadcast(callIntent);
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
                    .setType("*/*")
                    .setAction(Intent.ACTION_GET_CONTENT);

            Intent intent = Intent.createChooser(chooserIntent, "Select a file");

            startActivityForResult(intent, REQUEST_CODE_PICK_UPLOAD_FILE);
        }
    }
}