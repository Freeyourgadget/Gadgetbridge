package nodomain.freeyourgadget.gadgetbridge;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;
import java.util.UUID;

public class BluetoothCommunicationService extends Service {
    private static final String TAG = "BluetoothCommunicationService";

    public static final String ACTION_START
            = "nodomain.freeyourgadget.gadgetbride.bluetoothcommunicationservice.action.start";
    public static final String ACTION_STOP
            = "nodomain.freeyourgadget.gadgetbride.bluetoothcommunicationservice.action.stop";
    public static final String ACTION_SENDMESSAGE
            = "nodomain.freeyourgadget.gadgetbride.bluetoothcommunicationservice.action.sendbluetoothmessage";
    public static final String ACTION_SETTIME
            = "nodomain.freeyourgadget.gadgetbride.bluetoothcommunicationservice.action.settime";

    private BluetoothAdapter mBtAdapter = null;
    private String mBtDeviceAddress = null;
    private BluetoothSocket mBtSocket = null;
    private BtSocketIoThread mBtSocketIoThread = null;
    private BtSocketAcceptThread mBtSocketAcceptThread = null;
    private static final UUID PEBBLE_UUID = UUID.fromString("00000000-deca-fade-deca-deafdecacafe");
    private boolean mPassiveMode = false;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction().equals(ACTION_START)) {
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
                    if (mPassiveMode) {
                        mBtSocketAcceptThread = new BtSocketAcceptThread();
                        mBtSocketAcceptThread.start();
                    } else if (mBtSocket == null || !mBtSocket.isConnected()) {
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
        } else if (intent.getAction().equals(ACTION_STOP)) {
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
        } else if (intent.getAction().equals(ACTION_SENDMESSAGE)) {
            String title = intent.getStringExtra("notification_title");
            String content = intent.getStringExtra("notification_content");
            if (mBtSocketIoThread != null) {
                byte[] msg = PebbleProtocol.encodeSMS(title, content);
                mBtSocketIoThread.write(msg);
            }
        } else if (intent.getAction().equals(ACTION_SETTIME)) {
            if (mBtSocketIoThread != null) {
                byte[] msg = PebbleProtocol.encodeSetTime(-1);
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

    private class BtSocketAcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public BtSocketAcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = mBtAdapter.listenUsingRfcommWithServiceRecord("PebbleListener", PEBBLE_UUID);
            } catch (IOException e) {
            }
            mmServerSocket = tmp;
        }

        public void run() {
            while (true) {
                try {
                    mBtSocket = mmServerSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
                if (mBtSocket != null) {
                    try {
                        mBtSocketIoThread = new BtSocketIoThread(mBtSocket.getInputStream(), mBtSocket.getOutputStream());
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }
                    mBtSocketIoThread.start();
                    break;
                }
            }
        }
    }

    private class BtSocketIoThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public BtSocketIoThread(InputStream instream, OutputStream outstream) {
            mmInStream = instream;
            mmOutStream = outstream;
        }

        public void run() {
            byte[] buffer = new byte[8192];
            int bytes;

            while (true) {
                try {
                    bytes = mmInStream.read(buffer, 0, 4);
                    if (bytes < 4)
                        continue;

                    ByteBuffer buf = ByteBuffer.wrap(buffer);
                    buf.order(ByteOrder.BIG_ENDIAN);
                    short length = buf.getShort();
                    short endpoint = buf.getShort();
                    if (length < 0 || length > 8192) {
                        Log.i(TAG, "invalid length " + length);
                        while (mmInStream.available() > 0) {
                            mmInStream.read(buffer); // read all
                        }
                        continue;
                    }

                    bytes = mmInStream.read(buffer, 4, length);
                    if (bytes < length) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Log.i(TAG, "Read " + bytes + ", expected " + length + " reading remaining " + (length - bytes));
                        int bytes_rest = mmInStream.read(buffer, 4 + bytes, length - bytes);
                        bytes += bytes_rest;
                    }

                    if (length == 1 && endpoint == PebbleProtocol.ENDPOINT_PHONEVERSION) {
                        Log.i(TAG, "Pebble asked for Phone/App Version - repLYING!");
                        write(PebbleProtocol.encodePhoneVersion(PebbleProtocol.PHONEVERSION_REMOTE_OS_ANDROID));
                    } else {
                        Log.i(TAG, "unhandled message to endpoint " + endpoint + " (" + bytes + " bytes)");
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
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