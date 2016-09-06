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

        boolean hasOldDB = hasOldActivityDatabase();
        int oldDBVisibility = hasOldDB ? View.VISIBLE : View.GONE;

        View oldDBTitle = findViewById(R.id.mergeOldActivityDataTitle);
        oldDBTitle.setVisibility(oldDBVisibility);
        View oldDBText = findViewById(R.id.mergeOldActivityDataText);
        oldDBText.setVisibility(oldDBVisibility);

        importOldActivityDataButton = (Button) findViewById(R.id.mergeOldActivityData);
        importOldActivityDataButton.setVisibility(oldDBVisibility);
        importOldActivityDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mergeOldActivityDbContents();
            }
        });

        deleteOldActivityDBButton = (Button) findViewById(R.id.deleteOldActivityDB);
        deleteOldActivityDBButton.setVisibility(oldDBVisibility);
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

    private boolean hasOldActivityDatabase() {
        return new DBHelper(this).getOldActivityDatabaseHandler() != null;
    }

    private String getExternalPath() {
        try {
            return FileUtils.getExternalFilesDir().getAbsolutePath();
        } catch (Exception ex) {
            LOG.warn("Unable to get external files dir", ex);
        }
        return getString(R.string.dbmanagementactivvity_cannot_access_export_path);
    }

    private void exportDB() {
        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            DBHelper helper = new DBHelper(this);
            File dir = FileUtils.getExternalFilesDir();
            File destFile = helper.exportDB(dbHandler, dir);
            GB.toast(this, getString(R.string.dbmanagementactivity_exported_to, destFile.getAbsolutePath()), Toast.LENGTH_LONG, GB.INFO);
        } catch (Exception ex) {
            GB.toast(this, getString(R.string.dbmanagementactivity_error_exporting_db, ex.getMessage()), Toast.LENGTH_LONG, GB.ERROR, ex);
        }
    }

    private void importDB() {
        new AlertDialog.Builder(this)
                .setCancelable(true)
                .setTitle(R.string.dbmanagementactivity_import_data_title)
                .setMessage(R.string.dbmanagementactivity_overwrite_database_confirmation)
                .setPositiveButton(R.string.dbmanagementactivity_overwrite, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try (DBHandler dbHandler = GBApplication.acquireDB()) {
                            DBHelper helper = new DBHelper(DbManagementActivity.this);
                            File dir = FileUtils.getExternalFilesDir();
                            SQLiteOpenHelper sqLiteOpenHelper = dbHandler.getHelper();
                            File sourceFile = new File(dir, sqLiteOpenHelper.getDatabaseName());
                            helper.importDB(dbHandler, sourceFile);
                            helper.validateDB(sqLiteOpenHelper);
                            GB.toast(DbManagementActivity.this, getString(R.string.dbmanagementactivity_import_successful), Toast.LENGTH_LONG, GB.INFO);
                        } catch (Exception ex) {
                            GB.toast(DbManagementActivity.this, getString(R.string.dbmanagementactivity_error_importing_db, ex.getMessage()), Toast.LENGTH_LONG, GB.ERROR, ex);
                        }
                    }
                })
                .setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
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
            GB.toast(this, getString(R.string.dbmanagementactivity_no_old_activitydatabase_found), Toast.LENGTH_LONG, GB.ERROR);
            return;
        }
        selectDeviceForMergingActivityDatabaseInto(new DeviceSelectionCallback() {
            @Override
            public void invoke(final GBDevice device) {
                if (device == null) {
                    GB.toast(DbManagementActivity.this, getString(R.string.dbmanagementactivity_no_connected_device), Toast.LENGTH_LONG, GB.ERROR);
                    return;
                }
                try (DBHandler targetHandler = GBApplication.acquireDB()) {
                    final ProgressDialog progress = ProgressDialog.show(DbManagementActivity.this, getString(R.string.dbmanagementactivity_merging_activity_data_title), getString(R.string.dbmanagementactivity_please_wait_while_merging), true, false);
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
                    GB.toast(DbManagementActivity.this, getString(R.string.dbmanagementactivity_error_importing_old_activity_data), Toast.LENGTH_LONG, GB.ERROR, ex);
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
                .setTitle(R.string.dbmanagementactivity_associate_old_data_with_device)
                .setAdapter(adapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        GBDevice device = availableDevices.get(which);
                        callback.invoke(device);
                    }
                })
                .setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
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
                .setTitle(R.string.dbmanagementactivity_delete_activity_data_title)
                .setMessage(R.string.dbmanagementactivity_really_delete_entire_db)
                .setPositiveButton(R.string.Delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (GBApplication.deleteActivityDatabase(DbManagementActivity.this)) {
                            GB.toast(DbManagementActivity.this, getString(R.string.dbmanagementactivity_database_successfully_deleted), Toast.LENGTH_SHORT, GB.INFO);
                        } else {
                            GB.toast(DbManagementActivity.this, getString(R.string.dbmanagementactivity_db_deletion_failed), Toast.LENGTH_SHORT, GB.INFO);
                        }
                    }
                })
                .setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
    }

    private void deleteOldActivityDbFile() {
        new AlertDialog.Builder(this).setCancelable(true);
        new AlertDialog.Builder(this).setTitle(R.string.dbmanagementactivity_delete_old_activity_db);
        new AlertDialog.Builder(this).setMessage(R.string.dbmanagementactivity_delete_old_activitydb_confirmation);
        new AlertDialog.Builder(this).setPositiveButton(R.string.Delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (GBApplication.deleteOldActivityDatabase(DbManagementActivity.this)) {
                    GB.toast(DbManagementActivity.this, getString(R.string.dbmanagementactivity_old_activity_db_successfully_deleted), Toast.LENGTH_SHORT, GB.INFO);
                } else {
                    GB.toast(DbManagementActivity.this, getString(R.string.dbmanagementactivity_old_activity_db_deletion_failed), Toast.LENGTH_SHORT, GB.INFO);
                }
            }
        });
        new AlertDialog.Builder(this).setNegativeButton(R.string.Cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        new AlertDialog.Builder(this).show();
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
