package nodomain.freeyourgadget.gadgetbridge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;


public class PhoneCallReceiver extends BroadcastReceiver {

    private static int mLastState = TelephonyManager.CALL_STATE_IDLE;
    private static String mSavedNumber;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.NEW_OUTGOING_CALL")) {
            mSavedNumber = intent.getExtras().getString("android.intent.extra.PHONE_NUMBER");
        } else {
            String stateStr = intent.getExtras().getString(TelephonyManager.EXTRA_STATE);
            String number = intent.getExtras().getString(TelephonyManager.EXTRA_INCOMING_NUMBER);
            int state = 0;
            if (stateStr.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                state = TelephonyManager.CALL_STATE_IDLE;
            } else if (stateStr.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                state = TelephonyManager.CALL_STATE_OFFHOOK;
            } else if (stateStr.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                state = TelephonyManager.CALL_STATE_RINGING;
            }

            onCallStateChanged(context, state, number);
        }
    }

    public void onCallStateChanged(Context context, int state, String number) {
        if (mLastState == state) {
            return;
        }

        GBCommand callCommand = null;
        switch (state) {
            case TelephonyManager.CALL_STATE_RINGING:
                mSavedNumber = number;
                callCommand = GBCommand.CALL_INCOMING;
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                if (mLastState == TelephonyManager.CALL_STATE_RINGING) {
                    callCommand = GBCommand.CALL_START;
                } else {
                    callCommand = GBCommand.CALL_OUTGOING;
                }
                break;
            case TelephonyManager.CALL_STATE_IDLE:
                if (mLastState == TelephonyManager.CALL_STATE_RINGING) {
                    //missed call would be correct here
                    callCommand = GBCommand.CALL_END;
                } else {
                    callCommand = GBCommand.CALL_END;
                }
                break;
        }
        if (callCommand != null) {
            Intent startIntent = new Intent(context, BluetoothCommunicationService.class);
            startIntent.setAction(BluetoothCommunicationService.ACTION_CALLSTATE);
            startIntent.putExtra("call_phonenumber", mSavedNumber);
            startIntent.putExtra("call_command", callCommand.ordinal());
            context.startService(startIntent);
        }
        mLastState = state;
    }
}
