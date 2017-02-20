package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventAppMessage;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.pebble.GBDeviceEventDataLogging;

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

    private static final String PEBBLEKIT_ACTION_DL_RECEIVE_DATA_NEW = "com.getpebble.action.dl.RECEIVE_DATA_NEW";
    private static final String PEBBLEKIT_ACTION_DL_RECEIVE_DATA = "com.getpebble.action.dl.RECEIVE_DATA";
    private static final String PEBBLEKIT_ACTION_DL_ACK_DATA = "com.getpebble.action.dl.ACK_DATA";
    private static final String PEBBLEKIT_ACTION_DL_REQUEST_DATA = "com.getpebble.action.dl.REQUEST_DATA";
    private static final String PEBBLEKIT_ACTION_DL_FINISH_SESSION = "com.getpebble.action.dl.FINISH_SESSION_NEW";

    private static final Logger LOG = LoggerFactory.getLogger(PebbleKitSupport.class);

    private final PebbleProtocol mPebbleProtocol;
    private final Context mContext;
    private final PebbleIoThread mPebbleIoThread;

    private int dataLogTransactionId = 1;

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
                        //  if (transaction_id >= 0 && transaction_id <= 255) {
                        sendAppMessageAck(transaction_id);
                        //  }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                case PEBBLEKIT_ACTION_APP_ACK:
                    transaction_id = intent.getIntExtra("transaction_id", -1);
                    if (!mPebbleProtocol.mAlwaysACKPebbleKit) {
                        if (transaction_id >= 0 && transaction_id <= 255) {
                            mPebbleIoThread.write(mPebbleProtocol.encodeApplicationMessageAck(null, (byte) transaction_id));
                        } else {
                            LOG.warn("illegal transaction id " + transaction_id);
                        }
                    }
                    break;
                case PEBBLEKIT_ACTION_DL_ACK_DATA:
                    LOG.info("GOT DL DATA ACK");
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
        intentFilter.addAction(PEBBLEKIT_ACTION_DL_ACK_DATA);
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

    void sendDataLoggingIntent(GBDeviceEventDataLogging dataLogging) {
        Intent intent = new Intent();
        intent.putExtra("data_log_timestamp", dataLogging.timestamp);
        intent.putExtra("uuid", dataLogging.appUUID);
        intent.putExtra("data_log_uuid", dataLogging.appUUID); // Is that really the same?
        intent.putExtra("data_log_tag", dataLogging.tag);

        switch (dataLogging.command) {
            case GBDeviceEventDataLogging.COMMAND_RECEIVE_DATA:
                intent.setAction(PEBBLEKIT_ACTION_DL_RECEIVE_DATA_NEW);
                intent.putExtra("pbl_data_id", dataLogTransactionId++);
                intent.putExtra("pbl_data_type", dataLogging.pebbleDataType);
                switch (dataLogging.pebbleDataType) {
                    case PebbleProtocol.TYPE_BYTEARRAY:
                        intent.putExtra("pbl_data_object", Base64.encodeToString((byte[]) dataLogging.data, Base64.NO_WRAP));
                        break;
                    case PebbleProtocol.TYPE_UINT:
                        intent.putExtra("pbl_data_object", (Long) dataLogging.data);
                        break;
                    case PebbleProtocol.TYPE_INT:
                        intent.putExtra("pbl_data_object", (Integer) dataLogging.data);
                        break;
                }
                LOG.info("broadcasting datalogging to uuid " + dataLogging.appUUID + " tag: " + dataLogging.tag + "transaction id: " + dataLogTransactionId + " type: " + dataLogging.pebbleDataType);
                break;
            case GBDeviceEventDataLogging.COMMAND_FINISH_SESSION:
                intent.setAction(PEBBLEKIT_ACTION_DL_FINISH_SESSION);
                LOG.info("broadcasting datalogging finish session to uuid " + dataLogging.appUUID + " tag: " + dataLogging.tag);

                break;
            default:
                LOG.warn("invalid datalog command");
                return;
        }
        mContext.sendBroadcast(intent);
    }
}
