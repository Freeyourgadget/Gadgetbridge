package nodomain.freeyourgadget.gadgetbridge.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.adapter.GBDeviceAdapter;
import nodomain.freeyourgadget.gadgetbridge.database.ActivityDatabaseHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;


public class DbManagementActivity extends GBActivity {
    private static final Logger LOG = LoggerFactory.getLogger(DbManagementActivity.class);

    private Button exportDBButton;
    private Button importDBButton;
    private Button importOldActivityDataButton;
    private Button deleteOldActivityDBButton;
    private Button deleteDBButton;
    private TextView dbPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_db_management);

        IntentFilter filter = new IntentFilter();
        filter.addAction(GBApplication.ACTION_QUIT);

        dbPath = (TextView) findViewById(R.id.activity_db_management_path);
        dbPath.setText(getExternalPath());

        exportDBButton = (Button) findViewById(R.id.exportDBButton);
        exportDBButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportDB();
            }
        });
        importDBButton = (Button) findViewById(R.id.importDBButton);
        importDBButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                importDB();
            }
        });

        importOldActivityDataButton = (Button) findViewById(R.id.mergeOldActivityData);
        importOldActivityDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mergeOldActivityDbContents();
            }
        });

        deleteOldActivityDBButton = (Button) findViewById(R.id.deleteOldActivityDB);
        deleteOldActivityDBButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteOldActivityDbFile();
            }
        });

        deleteDBButton = (Button) findViewById(R.id.emptyDBButton);
        deleteDBButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteActivityDatabase();
            }
        });
    }

    private String getExternalPath() {
        try {
            return FileUtils.getExternalFilesDir().getAbsolutePath();
        } catch (Exception ex) {
        }
        return "Cannot access export path. Please contact the developers.";
    }

    private void exportDB() {
        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            DBHelper helper = new DBHelper(this);
            File dir = FileUtils.getExternalFilesDir();
            File destFile = helper.exportDB(dbHandler, dir);
            GB.toast(this, "Exported to: " + destFile.getAbsolutePath(), Toast.LENGTH_LONG, GB.INFO);
        } catch (Exception ex) {
            GB.toast(this, "Error exporting DB: " + ex.getMessage(), Toast.LENGTH_LONG, GB.ERROR, ex);
        }
    }

    private void importDB() {
        new AlertDialog.Builder(this)
                .setCancelable(true)
                .setTitle("Import Activity Data?")
                .setMessage("Really overwrite the current activity database? All your activity data (if any) will be lost.")
                .setPositiveButton("Overwrite", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try (DBHandler dbHandler = GBApplication.acquireDB()) {
                            DBHelper helper = new DBHelper(DbManagementActivity.this);
                            File dir = FileUtils.getExternalFilesDir();
                            SQLiteOpenHelper sqLiteOpenHelper = dbHandler.getHelper();
                            File sourceFile = new File(dir, sqLiteOpenHelper.getDatabaseName());
                            helper.importDB(dbHandler, sourceFile);
                            helper.validateDB(sqLiteOpenHelper);
                            GB.toast(DbManagementActivity.this, "Import successful.", Toast.LENGTH_LONG, GB.INFO);
                        } catch (Exception ex) {
                            GB.toast(DbManagementActivity.this, "Error importing DB: " + ex.getMessage(), Toast.LENGTH_LONG, GB.ERROR, ex);
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    private void mergeOldActivityDbContents() {
        final DBHelper helper = new DBHelper(getBaseContext());
        final ActivityDatabaseHandler oldHandler = helper.getOldActivityDatabaseHandler();
        if (oldHandler == null) {
            GB.toast(this, "No old activity database found, nothing to import.", Toast.LENGTH_LONG, GB.ERROR);
            return;
        }
        selectDeviceForMergingActivityDatabaseInto(new DeviceSelectionCallback() {
            @Override
            public void invoke(final GBDevice device) {
                if (device == null) {
                    GB.toast(DbManagementActivity.this, "No connected device to associate old activity data with.", Toast.LENGTH_LONG, GB.ERROR);
                    return;
                }
                try (DBHandler targetHandler = GBApplication.acquireDB()) {
                    final ProgressDialog progress = ProgressDialog.show(DbManagementActivity.this, "Merging Activity Data", "Please wait while merging your activity data...", true, false);
                    new AsyncTask<Object, ProgressDialog, Object>() {
                        @Override
                        protected Object doInBackground(Object[] params) {
                            helper.importOldDb(oldHandler, device, targetHandler);
                            if (!isFinishing() && !isDestroyed()) {
                                progress.dismiss();
                            }
                            return null;
                        }
                    }.execute((Object[]) null);
                } catch (Exception ex) {
                    GB.toast(DbManagementActivity.this, "Error importing old activity data into new database.", Toast.LENGTH_LONG, GB.ERROR, ex);
                }
            }
        });
    }

    private void selectDeviceForMergingActivityDatabaseInto(final DeviceSelectionCallback callback) {
        GBDevice connectedDevice = GBApplication.getDeviceManager().getSelectedDevice();
        if (connectedDevice == null) {
            callback.invoke(null);
            return;
        }
        final List<GBDevice> availableDevices = Collections.singletonList(connectedDevice);
        GBDeviceAdapter adapter = new GBDeviceAdapter(getBaseContext(), availableDevices);

        new AlertDialog.Builder(this)
                .setCancelable(true)
                .setTitle("Associate old Data with Device")
                .setAdapter(adapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        GBDevice device = availableDevices.get(which);
                        callback.invoke(device);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // ignore, just return
                    }
                })
                .show();
    }

    private void deleteActivityDatabase() {
        new AlertDialog.Builder(this)
                .setCancelable(true)
                .setTitle("Delete Activity Data?")
                .setMessage("Really delete the entire activity database? All your activity data will be lost.")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (GBApplication.deleteActivityDatabase(DbManagementActivity.this)) {
                            GB.toast(DbManagementActivity.this, "Activity database successfully deleted.", Toast.LENGTH_SHORT, GB.INFO);
                        } else {
                            GB.toast(DbManagementActivity.this, "Activity database deletion failed.", Toast.LENGTH_SHORT, GB.INFO);
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    private void deleteOldActivityDbFile() {
        new AlertDialog.Builder(this)
                .setCancelable(true)
                .setTitle("Delete old Activity Database?")
                .setMessage("Really delete the old activity database? Activity data that were not imported will be lost.")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (GBApplication.deleteOldActivityDatabase(DbManagementActivity.this)) {
                            GB.toast(DbManagementActivity.this, "Old Activity database successfully deleted.", Toast.LENGTH_SHORT, GB.INFO);
                        } else {
                            GB.toast(DbManagementActivity.this, "Old Activity database deletion failed.", Toast.LENGTH_SHORT, GB.INFO);
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public interface DeviceSelectionCallback {
        void invoke(GBDevice device);
    }
}
