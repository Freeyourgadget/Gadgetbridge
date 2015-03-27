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


public class DebugActivity extends Activity {
    Button sendSMSButton;
    Button sendEmailButton;
    Button incomingCallButton;
    Button outgoingCallButton;
    Button startCallButton;
    Button endCallButton;
    Button testNotificationButton;
    Button setMusicInfoButton;
    Button setTimeButton;
    EditText editContent;

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
                startIntent.setAction(BluetoothCommunicationService.ACTION_NOTIFICATION_GENERIC);
                startIntent.putExtra("notification_title", "Gadgetbridge");
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
                startIntent.putExtra("notification_sender", "Gadgetbridge");
                startIntent.putExtra("notification_subject", "Test");
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

    private void testNotification() {
        NotificationManager nManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder ncomp = new NotificationCompat.Builder(this);
        ncomp.setContentTitle("Test Notification");
        ncomp.setContentText("This is a Test Notification from Gadgetbridge");
        ncomp.setTicker("This is a Test Notification from Gadgetbridge");
        ncomp.setSmallIcon(R.drawable.ic_launcher);
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
