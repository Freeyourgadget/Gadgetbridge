package nodomain.freeyourgadget.gadgetbridge;

import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class ControlCenter extends ActionBarActivity {
    // SPP Serial Device UUID
    private static final UUID SERIAL_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    BluetoothAdapter mBtAdapter;
    String mBtDeviceAddress = null;
    BluetoothSocket mBtSocket;
    Button sendButton;
    Button testNotificationButton;
    EditText editTitle;
    EditText editContent;
    private NotificationReceiver nReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controlcenter);

        //Check the system status
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (!mBtAdapter.isEnabled()) {
            Toast.makeText(this, "Bluetooth is disabled.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            if (device.getName().indexOf("Pebble") == 0) {
                // Matching device found
                mBtDeviceAddress = device.getAddress();
            }
        }

        editTitle = (EditText) findViewById(R.id.editTitle);
        editContent = (EditText) findViewById(R.id.editContent);
        sendButton = (Button) findViewById(R.id.sendButton);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!mBtAdapter.isEnabled() || mBtDeviceAddress == null)
                    return;
                String title = editTitle.getText().toString();
                String content = editContent.getText().toString();
                try {
                    if (mBtSocket == null || !mBtSocket.isConnected()) {
                        BluetoothDevice btDevice = mBtAdapter.getRemoteDevice(mBtDeviceAddress);
                        mBtSocket = btDevice.createRfcommSocketToServiceRecord(SERIAL_UUID);
                        mBtSocket.connect();
                    }
                    ConnectedTask task = new ConnectedTask();
                    task.execute(mBtSocket, title, content);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        testNotificationButton = (Button) findViewById(R.id.testNotificationButton);
        testNotificationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testNotification();
            }
        });

        Intent enableIntent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");

        startActivity(enableIntent);
        nReceiver = new NotificationReceiver();

        IntentFilter filter = new IntentFilter();
        filter.addAction("nodomain.freeyourgadget.gadgetbridge.NOTIFICATION_LISTENER");
        registerReceiver(nReceiver, filter);
    }

    private void testNotification() {
        NotificationManager nManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder ncomp = new NotificationCompat.Builder(this);
        ncomp.setContentTitle("Test Notification");
        ncomp.setContentText("This is a Test Notification from Gadgetbridge");
        ncomp.setTicker("This is a Test Notificytion from Gadgetbridge");
        ncomp.setSmallIcon(R.drawable.ic_launcher);
        ncomp.setAutoCancel(true);
        nManager.notify((int) System.currentTimeMillis(), ncomp.build());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            //Intent intent = new Intent(this, SettingsActivity.class);
            //startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (mBtSocket != null) {
                mBtSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (nReceiver != null) {
            unregisterReceiver(nReceiver);
        }
    }


    //AsyncTask to receive a single line of data and post
    private class ConnectedTask extends
            AsyncTask<Object, Void, String> {
        @Override
        protected String doInBackground(
                Object... params) {
            InputStream in = null;
            OutputStream out = null;
            BluetoothSocket socket = (BluetoothSocket) params[0];
            String title = (String) params[1];
            String content = (String) params[2];
            try {
                byte[] buffer = new byte[1024];
                String result;
                in = socket.getInputStream();

                //in.read(buffer);
                //result = PebbleProtocol.decodeResponse(buffer);

                out = socket.getOutputStream();


                byte[] msg;
                msg = PebbleProtocol.encodeSMS(title, content);

                //msg = PebbleProtocol.encodeSetTime();
                //msg = PebbleProtocol.encodeIncomingCall("03012323", title);
                //msg = PebbleProtocol.encodeEmail(title, "subject", content);

                out.write(msg);
                SystemClock.sleep(500);
                //in.read(buffer);
                //result = PebbleProtocol.decodeResponse(buffer);
                result = "ok";
                //Close the connection
                return result.trim();
            } catch (Exception exc) {
                return "error";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(ControlCenter.this, result,
                    Toast.LENGTH_SHORT).show();

            try {
                mBtSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    class NotificationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!mBtAdapter.isEnabled() || mBtDeviceAddress == null)
                return;

            String title = intent.getStringExtra("notification_title");
            String content = intent.getStringExtra("notification_content");
            try {
                if (mBtSocket == null || !mBtSocket.isConnected()) {
                    BluetoothDevice btDevice = mBtAdapter.getRemoteDevice(mBtDeviceAddress);
                    mBtSocket = btDevice.createRfcommSocketToServiceRecord(SERIAL_UUID);
                    mBtSocket.connect();
                }
                ConnectedTask task = new ConnectedTask();
                task.execute(mBtSocket, title, content);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}