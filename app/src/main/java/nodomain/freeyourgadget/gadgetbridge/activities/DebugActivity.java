package nodomain.freeyourgadget.gadgetbridge.activities;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.RemoteInput;
import android.support.v4.content.LocalBroadcastManager;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;
import nodomain.freeyourgadget.gadgetbridge.util.GB;


public class DebugActivity extends GBActivity {
    private static final Logger LOG = LoggerFactory.getLogger(DebugActivity.class);

    private static final String EXTRA_REPLY = "reply";
    private static final String ACTION_REPLY
            = "nodomain.freeyourgadget.gadgetbridge.DebugActivity.action.reply";

    private Spinner sendTypeSpinner;
    private Button sendButton;
    private Button incomingCallButton;
    private Button outgoingCallButton;
    private Button startCallButton;
    private Button endCallButton;
    private Button testNotificationButton;
    private Button setMusicInfoButton;
    private Button setTimeButton;
    private Button rebootButton;
    private Button HeartRateButton;
    private Button testNewFunctionalityButton;

    private EditText editContent;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case GBApplication.ACTION_QUIT: {
                    finish();
                    break;
                }
                case ACTION_REPLY: {
                    Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
                    CharSequence reply = remoteInput.getCharSequence(EXTRA_REPLY);
                    LOG.info("got wearable reply: " + reply);
                    GB.toast(context, "got wearable reply: " + reply, Toast.LENGTH_SHORT, GB.INFO);
                    break;
                }
                case DeviceService.ACTION_HEARTRATE_MEASUREMENT: {
                    int hrValue = intent.getIntExtra(DeviceService.EXTRA_HEART_RATE_VALUE, -1);
                    GB.toast(DebugActivity.this, "Heart Rate measured: " + hrValue, Toast.LENGTH_LONG, GB.INFO);
                    break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);

        IntentFilter filter = new IntentFilter();
        filter.addAction(GBApplication.ACTION_QUIT);
        filter.addAction(ACTION_REPLY);
        filter.addAction(DeviceService.ACTION_HEARTRATE_MEASUREMENT);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filter);
        registerReceiver(mReceiver, filter); // for ACTION_REPLY

        editContent = (EditText) findViewById(R.id.editContent);

        ArrayList<String> spinnerArray = new ArrayList<>();
        for (NotificationType notificationType : NotificationType.values()) {
            spinnerArray.add(notificationType.name());
        }
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, spinnerArray);
        sendTypeSpinner = (Spinner) findViewById(R.id.sendTypeSpinner);
        sendTypeSpinner.setAdapter(spinnerArrayAdapter);

        sendButton = (Button) findViewById(R.id.sendButton);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NotificationSpec notificationSpec = new NotificationSpec();
                String testString = editContent.getText().toString();
                notificationSpec.phoneNumber = testString;
                notificationSpec.body = testString;
                notificationSpec.sender = testString;
                notificationSpec.subject = testString;
                notificationSpec.type = NotificationType.values()[sendTypeSpinner.getSelectedItemPosition()];
                notificationSpec.id = -1;
                GBApplication.deviceService().onNotification(notificationSpec);
            }
        });

        incomingCallButton = (Button) findViewById(R.id.incomingCallButton);
        incomingCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CallSpec callSpec = new CallSpec();
                callSpec.command = CallSpec.CALL_INCOMING;
                callSpec.number = editContent.getText().toString();
                GBApplication.deviceService().onSetCallState(callSpec);
            }
        });
        outgoingCallButton = (Button) findViewById(R.id.outgoingCallButton);
        outgoingCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CallSpec callSpec = new CallSpec();
                callSpec.command = CallSpec.CALL_OUTGOING;
                callSpec.number = editContent.getText().toString();
                GBApplication.deviceService().onSetCallState(callSpec);
            }
        });

        startCallButton = (Button) findViewById(R.id.startCallButton);
        startCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CallSpec callSpec = new CallSpec();
                callSpec.command = CallSpec.CALL_START;
                GBApplication.deviceService().onSetCallState(callSpec);
            }
        });
        endCallButton = (Button) findViewById(R.id.endCallButton);
        endCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CallSpec callSpec = new CallSpec();
                callSpec.command = CallSpec.CALL_END;
                GBApplication.deviceService().onSetCallState(callSpec);
            }
        });

        rebootButton = (Button) findViewById(R.id.rebootButton);
        rebootButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GBApplication.deviceService().onReboot();
            }
        });
        HeartRateButton = (Button) findViewById(R.id.HearRateButton);
        HeartRateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GB.toast("Measuring heart rate, please wait...", Toast.LENGTH_LONG, GB.INFO);
                GBApplication.deviceService().onHeartRateTest();
            }
        });

        setMusicInfoButton = (Button) findViewById(R.id.setMusicInfoButton);
        setMusicInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MusicSpec musicSpec = new MusicSpec();
                musicSpec.artist = editContent.getText().toString() + "(artist)";
                musicSpec.album = editContent.getText().toString() + "(album)";
                musicSpec.track = editContent.getText().toString() + "(track)";
                musicSpec.duration = 10;
                musicSpec.trackCount = 5;
                musicSpec.trackNr = 2;

                GBApplication.deviceService().onSetMusicInfo(musicSpec);

                MusicStateSpec stateSpec = new MusicStateSpec();
                stateSpec.position = 0;
                stateSpec.state = 0x01; // playing
                stateSpec.playRate = 100;
                stateSpec.repeat = 1;
                stateSpec.shuffle = 1;

                GBApplication.deviceService().onSetMusicState(stateSpec);
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

        testNewFunctionalityButton = (Button) findViewById(R.id.testNewFunctionality);
        testNewFunctionalityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testNewFunctionality();
            }
        });
    }

    private void testNewFunctionality() {
        GBApplication.deviceService().onTestNewFunction();
    }

    private void testNotification() {
        Intent notificationIntent = new Intent(getApplicationContext(), DebugActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0,
                notificationIntent, 0);

        NotificationManager nManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        RemoteInput remoteInput = new RemoteInput.Builder(EXTRA_REPLY)
                .build();

        Intent replyIntent = new Intent(ACTION_REPLY);

        PendingIntent replyPendingIntent = PendingIntent.getBroadcast(this, 0, replyIntent, 0);

        NotificationCompat.Action action =
                new NotificationCompat.Action.Builder(android.R.drawable.ic_input_add, "Reply", replyPendingIntent)
                        .addRemoteInput(remoteInput)
                        .build();

        NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender().addAction(action);

        NotificationCompat.Builder ncomp = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.test_notification))
                .setContentText(getString(R.string.this_is_a_test_notification_from_gadgetbridge))
                .setTicker(getString(R.string.this_is_a_test_notification_from_gadgetbridge))
                .setSmallIcon(R.drawable.ic_notification)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .extend(wearableExtender);

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
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        unregisterReceiver(mReceiver);
    }

    public interface DeviceSelectionCallback {
        void invoke(GBDevice device);
    }
}
