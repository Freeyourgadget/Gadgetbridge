package nodomain.freeyourgadget.gadgetbridge.activities;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.app.NotificationCompat;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;
import nodomain.freeyourgadget.gadgetbridge.model.ServiceCommand;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;


public class DebugActivity extends Activity {
    private static final Logger LOG = LoggerFactory.getLogger(DebugActivity.class);

    private Button sendSMSButton;
    private Button sendEmailButton;
    private Button incomingCallButton;
    private Button outgoingCallButton;
    private Button startCallButton;
    private Button endCallButton;
    private Button testNotificationButton;
    private Button setMusicInfoButton;
    private Button setTimeButton;
    private Button rebootButton;
    private Button exportDBButton;
    private Button importDBButton;
    private EditText editContent;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ControlCenter.ACTION_QUIT)) {
                finish();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        registerReceiver(mReceiver, new IntentFilter(ControlCenter.ACTION_QUIT));

        editContent = (EditText) findViewById(R.id.editContent);
        sendSMSButton = (Button) findViewById(R.id.sendSMSButton);
        sendSMSButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NotificationSpec notificationSpec = new NotificationSpec();
                notificationSpec.sender = getResources().getText(R.string.app_name).toString();
                notificationSpec.body = editContent.getText().toString();
                notificationSpec.type = NotificationType.SMS;
                notificationSpec.id = -1;
                GBApplication.deviceService().onNotification(notificationSpec);
            }
        });
        sendEmailButton = (Button) findViewById(R.id.sendEmailButton);
        sendEmailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NotificationSpec notificationSpec = new NotificationSpec();
                notificationSpec.sender = getResources().getText(R.string.app_name).toString();
                notificationSpec.subject = editContent.getText().toString();
                notificationSpec.body = editContent.getText().toString();
                notificationSpec.type = NotificationType.EMAIL;
                notificationSpec.id = -1;
                GBApplication.deviceService().onNotification(notificationSpec);
            }
        });

        incomingCallButton = (Button) findViewById(R.id.incomingCallButton);
        incomingCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GBApplication.deviceService().onSetCallState(
                        editContent.getText().toString(),
                        null,
                        ServiceCommand.CALL_INCOMING);
            }
        });
        outgoingCallButton = (Button) findViewById(R.id.outgoingCallButton);
        outgoingCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GBApplication.deviceService().onSetCallState(
                        editContent.getText().toString(),
                        null,
                        ServiceCommand.CALL_OUTGOING);
            }
        });

        startCallButton = (Button) findViewById(R.id.startCallButton);
        startCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GBApplication.deviceService().onSetCallState(
                        null,
                        null,
                        ServiceCommand.CALL_START);
            }
        });
        endCallButton = (Button) findViewById(R.id.endCallButton);
        endCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GBApplication.deviceService().onSetCallState(
                        null,
                        null,
                        ServiceCommand.CALL_END);
            }
        });

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

        rebootButton = (Button) findViewById(R.id.rebootButton);
        rebootButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GBApplication.deviceService().onReboot();
            }
        });

        setMusicInfoButton = (Button) findViewById(R.id.setMusicInfoButton);
        setMusicInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GBApplication.deviceService().onSetMusicInfo(
                        editContent.getText().toString() + "(artist)",
                        editContent.getText().toString() + "(album)",
                        editContent.getText().toString() + "(tracl)");
            }
        });

        setTimeButton = (Button) findViewById(R.id.setTimeButton);
        setTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GBApplication.deviceService().onSetTime();
            }
        });

        testNotificationButton = (Button) findViewById(R.id.testNotificationButton);
        testNotificationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testNotification();
            }
        });
    }

    private void exportDB() {
        DBHandler dbHandler = null;
        try {
            dbHandler = GBApplication.acquireDB();
            DBHelper helper = new DBHelper(this);
            File dir = FileUtils.getExternalFilesDir();
            File destFile = helper.exportDB(dbHandler.getHelper(), dir);
            GB.toast(this, "Exported to: " + destFile.getAbsolutePath(), Toast.LENGTH_LONG, GB.INFO);
        } catch (Exception ex) {
            GB.toast(this, "Error exporting DB: " + ex.getMessage(), Toast.LENGTH_LONG, GB.ERROR, ex);
        } finally {
            if (dbHandler != null) {
                dbHandler.release();
            }
        }
    }

    private void importDB() {
        DBHandler dbHandler = null;
        try {
            dbHandler = GBApplication.acquireDB();
            DBHelper helper = new DBHelper(this);
            File dir = FileUtils.getExternalFilesDir();
            SQLiteOpenHelper sqLiteOpenHelper = dbHandler.getHelper();
            File sourceFile = new File(dir, sqLiteOpenHelper.getDatabaseName());
            helper.importDB(sqLiteOpenHelper, sourceFile);
            helper.validateDB(sqLiteOpenHelper);
            GB.toast(this, "Import successful.", Toast.LENGTH_LONG, GB.INFO);
        } catch (Exception ex) {
            GB.toast(this, "Error importing DB: " + ex.getMessage(), Toast.LENGTH_LONG, GB.ERROR, ex);
        } finally {
            if (dbHandler != null) {
                dbHandler.release();
            }
        }
    }

    private void testNotification() {
        Intent notificationIntent = new Intent(getApplicationContext(), DebugActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0,
                notificationIntent, 0);

        NotificationManager nManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder ncomp = new NotificationCompat.Builder(this);
        ncomp.setContentTitle(getString(R.string.test_notification));
        ncomp.setContentText(getString(R.string.this_is_a_test_notification_from_gadgetbridge));
        ncomp.setTicker(getString(R.string.this_is_a_test_notification_from_gadgetbridge));
        ncomp.setSmallIcon(R.drawable.ic_notification);
        ncomp.setAutoCancel(true);
        ncomp.setContentIntent(pendingIntent);
        nManager.notify((int) System.currentTimeMillis(), ncomp.build());
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

}
