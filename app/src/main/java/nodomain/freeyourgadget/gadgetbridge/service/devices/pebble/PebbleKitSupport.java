package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import org.json.JSONArray;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventAppMessage;

class PebbleKitSupport {
    //private static final String PEBBLEKIT_ACTION_PEBBLE_CONNECTED = "com.getpebble.action.PEBBLE_CONNECTED";
    //private static final String PEBBLEKIT_ACTION_PEBBLE_DISCONNECTED = "com.getpebble.action.PEBBLE_DISCONNECTED";
    private static final String PEBBLEKIT_ACTION_APP_ACK = "com.getpebble.action.app.ACK";
    private static final String PEBBLEKIT_ACTION_APP_NACK = "com.getpebble.action.app.NACK";
    private static final String PEBBLEKIT_ACTION_APP_RECEIVE = "com.getpebble.action.app.RECEIVE";
    private static final String PEBBLEKIT_ACTION_APP_RECEIVE_ACK = "com.getpebble.action.app.RECEIVE_ACK";
    //private static final String PEBBLEKIT_ACTION_APP_RECEIVE_NACK = "com.getpebble.action.app.RECEIVE_NACK";
    private static final String PEBBLEKIT_ACTION_APP_SEND = "com.getpebble.action.app.SEND";
    private static final String PEBBLEKIT_ACTION_APP_START = "com.getpebble.action.app.START";
    private static final String PEBBLEKIT_ACTION_APP_STOP = "com.getpebble.action.app.STOP";

    private static final Logger LOG = LoggerFactory.getLogger(PebbleKitSupport.class);

    private final PebbleProtocol mPebbleProtocol;
    private final Context mContext;
    private final PebbleIoThread mPebbleIoThread;

    private final BroadcastReceiver mPebbleKitReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            LOG.info("Got action: " + action);
            UUID uuid;
            switch (action) {
                case PEBBLEKIT_ACTION_APP_START:
                case PEBBLEKIT_ACTION_APP_STOP:
                    uuid = (UUID) intent.getSerializableExtra("uuid");
                    if (uuid != null) {
                        mPebbleIoThread.write(mPebbleProtocol.encodeAppStart(uuid, action.equals(PEBBLEKIT_ACTION_APP_START)));
                    }
                    break;
                case PEBBLEKIT_ACTION_APP_SEND:
                    int transaction_id = intent.getIntExtra("transaction_id", -1);
                    uuid = (UUID) intent.getSerializableExtra("uuid");
                    String jsonString = intent.getStringExtra("msg_data");
                    LOG.info("json string: " + jsonString);

                    try {
                        JSONArray jsonArray = new JSONArray(jsonString);
                        mPebbleIoThread.write(mPebbleProtocol.encodeApplicationMessageFromJSON(uuid, jsonArray));
                        if (transaction_id >= 0 && transaction_id <= 255) {
                            sendAppMessageAck(transaction_id);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                case PEBBLEKIT_ACTION_APP_ACK:
                    transaction_id = intent.getIntExtra("transaction_id", -1);
                    if (transaction_id >= 0 && transaction_id <= 255) {
                        mPebbleIoThread.write(mPebbleProtocol.encodeApplicationMessageAck(null, (byte) transaction_id));
                    } else {
                        LOG.warn("illegal transaction id " + transaction_id);
                    }
                    break;

            }
        }
    };

    PebbleKitSupport(Context context, PebbleIoThread pebbleIoThread, PebbleProtocol pebbleProtocol) {
        mContext = context;
        mPebbleIoThread = pebbleIoThread;
        mPebbleProtocol = pebbleProtocol;

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PEBBLEKIT_ACTION_APP_ACK);
        intentFilter.addAction(PEBBLEKIT_ACTION_APP_NACK);
        intentFilter.addAction(PEBBLEKIT_ACTION_APP_SEND);
        intentFilter.addAction(PEBBLEKIT_ACTION_APP_START);
        intentFilter.addAction(PEBBLEKIT_ACTION_APP_STOP);
        mContext.registerReceiver(mPebbleKitReceiver, intentFilter);
    }

    void sendAppMessageIntent(GBDeviceEventAppMessage appMessage) {
        Intent intent = new Intent();
        intent.setAction(PEBBLEKIT_ACTION_APP_RECEIVE);
        intent.putExtra("uuid", appMessage.appUUID);
        intent.putExtra("msg_data", appMessage.message);
        intent.putExtra("transaction_id", appMessage.id);
        LOG.info("broadcasting to uuid " + appMessage.appUUID + " transaction id: " + appMessage.id + " JSON: " + appMessage.message);
        mContext.sendBroadcast(intent);
    }

    private void sendAppMessageAck(int transactionId) {
        Intent intent = new Intent();
        intent.setAction(PEBBLEKIT_ACTION_APP_RECEIVE_ACK);
        intent.putExtra("transaction_id", transactionId);
        LOG.info("broadcasting ACK (transaction id " + transactionId + ")");
        mContext.sendBroadcast(intent);
    }

    void close() {
        try {
            mContext.unregisterReceiver(mPebbleKitReceiver);
        } catch (IllegalArgumentException ignore) {
        }
    }

}
