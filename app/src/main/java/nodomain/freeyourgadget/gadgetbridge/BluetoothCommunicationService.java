package nodomain.freeyourgadget.gadgetbridge;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

public class BluetoothCommunicationService extends Service {
    private static final String TAG = "BluetoothCommunicationService";

    // TODO: put in separate static class
    public static final String ACTION_STARTBLUETOOTHCOMMUNITCATIONSERVICE
            = "nodomain.freeyourgadget.gadgetbride.bluetoothcommunicationservice.action.start";
    public static final String ACTION_STOPBLUETOOTHCOMMUNITCATIONSERVICE
            = "nodomain.freeyourgadget.gadgetbride.bluetoothcommunicationservice.action.stop";
    public static final String ACTION_SENDBLUETOOTHMESSAGE
            = "nodomain.freeyourgadget.gadgetbride.bluetoothcommunicationservice.action.sendbluetoothmessage";

    private BluetoothAdapter mBtAdapter = null;
    private String mBtDeviceAddress = null;
    private BluetoothSocket mBtSocket = null;
    private BtSocketIoThread mBtSocketIoThread = null;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction().equals(ACTION_STARTBLUETOOTHCOMMUNITCATIONSERVICE)) {
            Intent notificationIntent = new Intent(this, ControlCenter.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                    notificationIntent, 0);

            Notification notification = new NotificationCompat.Builder(this)
                    .setContentTitle("Gadgetbridge")
                    .setTicker("Gadgetbridge Running")
                    .setContentText("Gadgetbrige Running")
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true).build();


            //Check the system status
            mBtAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBtAdapter == null) {
                Toast.makeText(this, "Bluetooth is not supported.", Toast.LENGTH_SHORT).show();
            } else if (!mBtAdapter.isEnabled()) {
                Toast.makeText(this, "Bluetooth is disabled.", Toast.LENGTH_SHORT).show();
            } else {
                Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
                for (BluetoothDevice device : pairedDevices) {
                    if (device.getName().indexOf("Pebble") == 0) {
                        // Matching device found
                        mBtDeviceAddress = device.getAddress();
                    }
                }

                try {
                    if (mBtSocket == null || !mBtSocket.isConnected()) {
                        BluetoothDevice btDevice = mBtAdapter.getRemoteDevice(mBtDeviceAddress);
                        ParcelUuid uuids[] = btDevice.getUuids();
                        mBtSocket = btDevice.createRfcommSocketToServiceRecord(uuids[0].getUuid());
                        mBtSocket.connect();
                        mBtSocketIoThread = new BtSocketIoThread(mBtSocket.getInputStream(), mBtSocket.getOutputStream());
                        mBtSocketIoThread.start();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
                startForeground(1, notification); //FIXME: don't hardcode id
            }
        } else if (intent.getAction().equals(ACTION_STOPBLUETOOTHCOMMUNITCATIONSERVICE)) {
            try {
                mBtSocketIoThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                mBtSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mBtSocket = null;
            mBtSocketIoThread = null;

            stopForeground(true);
            stopSelf();
        } else if (intent.getAction().equals(ACTION_SENDBLUETOOTHMESSAGE)) {
            String title = intent.getStringExtra("notification_title");
            String content = intent.getStringExtra("notification_content");
            if (mBtSocketIoThread != null) {
                byte[] msg;
                msg = PebbleProtocol.encodeSMS(title, content);
                mBtSocketIoThread.write(msg);
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private class BtSocketIoThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public BtSocketIoThread(InputStream instream, OutputStream outstream) {
            mmInStream = instream;
            mmOutStream = outstream;
        }

        public void run() {
            byte[] buffer = new byte[1000];  // buffer store for the stream
            int bytes; // bytes returned from read()

            while (true) {
                try {
                    byte[] ping = {0, 0, 0, 0};
                    // Read from the InputStream
                    //bytes = mmInStream.read(buffer,0,2);
                    //ByteBuffer buf = ByteBuffer.wrap(buffer);
                    //buf.order(ByteOrder.BIG_ENDIAN);
                    //short length = buf.getShort();
                    //Log.e(TAG,Integer.toString(length+4)  + " as total length");
                    bytes = mmInStream.read(buffer);
                    Log.e(TAG, Integer.toString(bytes) + ": " + PebbleProtocol.decodeResponse(buffer));
                    write(ping);
                } catch (IOException e) {
                }
            }
        }

        synchronized public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
                mmOutStream.flush();
            } catch (IOException e) {
            }
        }
    }
}