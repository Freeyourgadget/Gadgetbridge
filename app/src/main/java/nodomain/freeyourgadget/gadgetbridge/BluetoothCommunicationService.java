package nodomain.freeyourgadget.gadgetbridge;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.provider.ContactsContract;
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
    public static final String ACTION_NOTIFICATION_GENERIC
            = "nodomain.freeyourgadget.gadgetbride.bluetoothcommunicationservice.action.notification_generic";
    public static final String ACTION_NOTIFICATION_SMS
            = "nodomain.freeyourgadget.gadgetbride.bluetoothcommunicationservice.action.notification_sms";
    public static final String ACTION_NOTIFICATION_EMAIL
            = "nodomain.freeyourgadget.gadgetbride.bluetoothcommunicationservice.action.notification_email";
    public static final String ACTION_INCOMINGCALL
            = "nodomain.freeyourgadget.gadgetbride.bluetoothcommunicationservice.action.incomingcall";
    public static final String ACTION_SETTIME
            = "nodomain.freeyourgadget.gadgetbride.bluetoothcommunicationservice.action.settime";

    private BluetoothAdapter mBtAdapter = null;
    private String mBtDeviceAddress = null;
    private BluetoothSocket mBtSocket = null;
    private BtSocketIoThread mBtSocketIoThread = null;
    private static final UUID PEBBLE_UUID = UUID.fromString("00000000-deca-fade-deca-deafdecacafe");

    private void setReceiversEnableState(boolean enable) {
        final Class[] receiverClasses = {
                PhoneCallReceiver.class,
                SMSReceiver.class,
                K9Receiver.class,
        };

        int newState;

        if (enable) {
            newState = PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
        } else {
            newState = PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        }

        PackageManager pm = getPackageManager();

        for (Class receiverClass : receiverClasses) {
            ComponentName compName = new ComponentName(getApplicationContext(), receiverClass);

            pm.setComponentEnabledSetting(compName, newState, PackageManager.DONT_KILL_APP);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (!intent.getAction().equals(ACTION_START) && mBtSocketIoThread == null) {
            return START_STICKY;
        }

        if (intent.getAction().equals(ACTION_START)) {
            Intent notificationIntent = new Intent(this, ControlCenter.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                    notificationIntent, 0);

            Intent stopIntent = new Intent(this, StopServiceReceiver.class);
            PendingIntent pendingIntentStop = PendingIntent.getBroadcast(this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            Notification notification = new NotificationCompat.Builder(this)
                    .setContentTitle("Gadgetbridge")
                    .setTicker("Gadgetbridge Running")
                    .setContentText("Gadgetbrige Running")
                    .setSmallIcon(R.drawable.ic_launcher)
                    .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Quit", pendingIntentStop)
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

                        setReceiversEnableState(true); // enable BroadcastReceivers
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
                startForeground(1, notification); //FIXME: don't hardcode id
            }
        } else if (intent.getAction().equals(ACTION_NOTIFICATION_GENERIC)) {
            String title = intent.getStringExtra("notification_title");
            String body = intent.getStringExtra("notification_body");
            byte[] msg = PebbleProtocol.encodeSMS(title, body);
            mBtSocketIoThread.write(msg);
        } else if (intent.getAction().equals(ACTION_NOTIFICATION_SMS)) {
            String sender = intent.getStringExtra("notification_sender");
            String body = intent.getStringExtra("notification_body");
            String senderName = getContactDisplayNameByNumber(sender);
            byte[] msg = PebbleProtocol.encodeSMS(senderName, body);
            mBtSocketIoThread.write(msg);
        } else if (intent.getAction().equals(ACTION_NOTIFICATION_EMAIL)) {
            String sender = intent.getStringExtra("notification_sender");
            String subject = intent.getStringExtra("notification_subject");
            String body = intent.getStringExtra("notification_body");
            byte[] msg = PebbleProtocol.encodeEmail(sender, subject, body);
            mBtSocketIoThread.write(msg);
        } else if (intent.getAction().equals(ACTION_INCOMINGCALL)) {
            String phoneNumber = intent.getStringExtra("incomingcall_phonenumber");
            byte phoneState = intent.getByteExtra("incomingcall_state", (byte) 0);
            String callerName = getContactDisplayNameByNumber(phoneNumber);
            byte[] msg = PebbleProtocol.encodeIncomingCall(phoneNumber, callerName, phoneState);
            mBtSocketIoThread.write(msg);
        } else if (intent.getAction().equals(ACTION_SETTIME)) {
            byte[] msg = PebbleProtocol.encodeSetTime(-1);
            mBtSocketIoThread.write(msg);
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        setReceiversEnableState(false); // disable BroadcastReceivers

        if (mBtSocketIoThread != null) {
            try {
                mBtSocketIoThread.quit();
                mBtSocketIoThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (mBtSocket != null) {
            try {
                mBtSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private String getContactDisplayNameByNumber(String number) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
        String name = number;

        if (number == null || number.equals("")) {
            return name;
        }

        ContentResolver contentResolver = getContentResolver();
        Cursor contactLookup = contentResolver.query(uri, null, null, null, null);

        try {
            if (contactLookup != null && contactLookup.getCount() > 0) {
                contactLookup.moveToNext();
                name = contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
            }
        } finally {
            if (contactLookup != null) {
                contactLookup.close();
            }
        }

        return name;
    }


    private class BtSocketIoThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private boolean mQuit = false;

        public BtSocketIoThread(InputStream instream, OutputStream outstream) {
            mmInStream = instream;
            mmOutStream = outstream;
        }

        public void run() {
            byte[] buffer = new byte[8192];
            int bytes;

            while (!mQuit) {
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
                    } else if (endpoint != PebbleProtocol.ENDPOINT_DATALOG) {
                        Log.i(TAG, "unhandled message to endpoint " + endpoint + " (" + bytes + " bytes)");
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } catch (IOException e) {
                    if (e.getMessage().contains("socket closed")) { //FIXME: this does not feel right
                        mBtSocket = null;
                        setReceiversEnableState(false);
                        Log.i(TAG, "Bluetooth socket closed, will quit IO Thread");
                        mQuit = true;
                    }
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

        public void quit() {
            mQuit = true;
        }
    }
}
