package nodomain.freeyourgadget.gadgetbridge.activities;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.ActivityDatabaseHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class OnboardingActivity extends GBActivity {

    private Button importOldActivityDataButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        Bundle extras = getIntent().getExtras();

        GBDevice device = null;
        if (extras != null) {
            device = extras.getParcelable(GBDevice.EXTRA_DEVICE);
        } else {
            throw new IllegalArgumentException("Must provide a device when invoking this activity");
        }

        importOldActivityDataButton = (Button) findViewById(R.id.button_import_old_activitydata);
        importOldActivityDataButton.setText(String.format(getString(R.string.import_old_db_information), device.getName()));
        final GBDevice finalDevice = device;
        importOldActivityDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mergeOldActivityDbContents(finalDevice);
            }
        });
    }

    private void mergeOldActivityDbContents(final GBDevice device) {
        if (device == null) {
            return;
        }

        final DBHelper helper = new DBHelper(getBaseContext());
        final ActivityDatabaseHandler oldHandler = helper.getOldActivityDatabaseHandler();
        if (oldHandler == null) {
            GB.toast(this, "No old activity database found, nothing to import.", Toast.LENGTH_LONG, GB.ERROR);
            return;
        }

        try (DBHandler targetHandler = GBApplication.acquireDB()) {
            final ProgressDialog progress = ProgressDialog.show(OnboardingActivity.this, "Merging Activity Data", "Please wait while merging your activity data...", true, false);
            new AsyncTask<Object, ProgressDialog, Object>() {
                @Override
                protected Object doInBackground(Object[] params) {
                    helper.importOldDb(oldHandler, device, targetHandler);
                    progress.dismiss();
                    finish();
                    return null;
                }
            }.execute((Object[]) null);
        } catch (Exception ex) {
            GB.toast(OnboardingActivity.this, "Error importing old activity data into new database.", Toast.LENGTH_LONG, GB.ERROR, ex);
        }
    }

}
