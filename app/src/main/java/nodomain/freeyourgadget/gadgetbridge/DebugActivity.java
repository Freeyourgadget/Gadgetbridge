package nodomain.freeyourgadget.gadgetbridge;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

import nodomain.freeyourgadget.gadgetbridge.database.ActivityDatabaseHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;


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

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
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
                Intent startIntent = new Intent(DebugActivity.this, BluetoothCommunicationService.class);
                startIntent.setAction(BluetoothCommunicationService.ACTION_NOTIFICATION_SMS);
                startIntent.putExtra("notification_sender", getResources().getText(R.string.app_name));
                startIntent.putExtra("notification_body", editContent.getText().toString());
                startService(startIntent);
            }
        });
        sendEmailButton = (Button) findViewById(R.id.sendEmailButton);
        sendEmailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent startIntent = new Intent(DebugActivity.this, BluetoothCommunicationService.class);
                startIntent.setAction(BluetoothCommunicationService.ACTION_NOTIFICATION_EMAIL);
                startIntent.putExtra("notification_sender", getResources().getText(R.string.app_name));
                startIntent.putExtra("notification_subject", getResources().getText(R.string.test));
                startIntent.putExtra("notification_body", editContent.getText().toString());
                startService(startIntent);
            }
        });

        incomingCallButton = (Button) findViewById(R.id.incomingCallButton);
        incomingCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent startIntent = new Intent(DebugActivity.this, BluetoothCommunicationService.class);
                startIntent.setAction(BluetoothCommunicationService.ACTION_CALLSTATE);
                startIntent.putExtra("call_phonenumber", editContent.getText().toString());
                startIntent.putExtra("call_command", GBCommand.CALL_INCOMING.ordinal());
                startService(startIntent);
            }
        });
        outgoingCallButton = (Button) findViewById(R.id.outgoingCallButton);
        outgoingCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent startIntent = new Intent(DebugActivity.this, BluetoothCommunicationService.class);
                startIntent.setAction(BluetoothCommunicationService.ACTION_CALLSTATE);
                startIntent.putExtra("call_phonenumber", editContent.getText().toString());
                startIntent.putExtra("call_command", GBCommand.CALL_OUTGOING.ordinal());
                startService(startIntent);
            }
        });

        startCallButton = (Button) findViewById(R.id.startCallButton);
        startCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent startIntent = new Intent(DebugActivity.this, BluetoothCommunicationService.class);
                startIntent.setAction(BluetoothCommunicationService.ACTION_CALLSTATE);
                startIntent.putExtra("call_command", GBCommand.CALL_START.ordinal());
                startService(startIntent);
            }
        });
        endCallButton = (Button) findViewById(R.id.endCallButton);
        endCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent startIntent = new Intent(DebugActivity.this, BluetoothCommunicationService.class);
                startIntent.setAction(BluetoothCommunicationService.ACTION_CALLSTATE);
                startIntent.putExtra("call_command", GBCommand.CALL_END.ordinal());
                startService(startIntent);
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
                Intent startIntent = new Intent(DebugActivity.this, BluetoothCommunicationService.class);
                startIntent.setAction(BluetoothCommunicationService.ACTION_REBOOT);
                startService(startIntent);
            }
        });

        setMusicInfoButton = (Button) findViewById(R.id.setMusicInfoButton);
        setMusicInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent startIntent = new Intent(DebugActivity.this, BluetoothCommunicationService.class);
                startIntent.setAction(BluetoothCommunicationService.ACTION_SETMUSICINFO);
                startIntent.putExtra("music_artist", editContent.getText().toString() + "(artist)");
                startIntent.putExtra("music_album", editContent.getText().toString() + "(album)");
                startIntent.putExtra("music_track", editContent.getText().toString() + "(track)");
                startService(startIntent);
            }
        });

        setTimeButton = (Button) findViewById(R.id.setTimeButton);
        setTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent startIntent = new Intent(DebugActivity.this, BluetoothCommunicationService.class);
                startIntent.setAction(BluetoothCommunicationService.ACTION_SETTIME);
                startService(startIntent);
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
        try {
            ActivityDatabaseHandler dbHandler = GBApplication.getActivityDatabaseHandler();
            DBHelper helper = new DBHelper(this);
            File dir = FileUtils.getExternalFilesDir();
            File destFile = helper.exportDB(dbHandler, dir);
            Toast.makeText(this, "Exported to: " + destFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception ex) {
            LOG.error("Unable to export db", ex);
            Toast.makeText(this, "Error exporting DB: " + ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void importDB() {
        try {
            ActivityDatabaseHandler dbHandler = GBApplication.getActivityDatabaseHandler();
            DBHelper helper = new DBHelper(this);
            File dir = FileUtils.getExternalFilesDir();
            File sourceFile = new File(dir, dbHandler.getDatabaseName());
            helper.importDB(dbHandler, sourceFile);
            helper.validateDB(dbHandler);
            Toast.makeText(this, "Import successful.", Toast.LENGTH_LONG).show();
        } catch (Exception ex) {
            LOG.error("Unable to import db", ex);
            Toast.makeText(this, "Error importing DB: " + ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void testNotification() {
        NotificationManager nManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder ncomp = new NotificationCompat.Builder(this);
        ncomp.setContentTitle(getString(R.string.test_notification));
        ncomp.setContentText(getString(R.string.this_is_a_test_notification_from_gadgetbridge));
        ncomp.setTicker(getString(R.string.this_is_a_test_notification_from_gadgetbridge));
        ncomp.setSmallIcon(R.drawable.ic_notification);
        ncomp.setAutoCancel(true);
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
