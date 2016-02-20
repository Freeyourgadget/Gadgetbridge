package nodomain.freeyourgadget.gadgetbridge.externalevents;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.model.ServiceCommand;


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
            if (TelephonyManager.EXTRA_STATE_IDLE.equals(stateStr)) {
                state = TelephonyManager.CALL_STATE_IDLE;
            } else if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(stateStr)) {
                state = TelephonyManager.CALL_STATE_OFFHOOK;
            } else if (TelephonyManager.EXTRA_STATE_RINGING.equals(stateStr)) {
                state = TelephonyManager.CALL_STATE_RINGING;
            }
            onCallStateChanged(context, state, number);
        }
    }

    public void onCallStateChanged(Context context, int state, String number) {
        if (mLastState == state) {
            return;
        }

        ServiceCommand callCommand = null;
        switch (state) {
            case TelephonyManager.CALL_STATE_RINGING:
                mSavedNumber = number;
                callCommand = ServiceCommand.CALL_INCOMING;
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                if (mLastState == TelephonyManager.CALL_STATE_RINGING) {
                    callCommand = ServiceCommand.CALL_START;
                } else {
                    callCommand = ServiceCommand.CALL_OUTGOING;
                    mSavedNumber = number;
                }
                break;
            case TelephonyManager.CALL_STATE_IDLE:
                if (mLastState == TelephonyManager.CALL_STATE_RINGING) {
                    //missed call would be correct here
                    callCommand = ServiceCommand.CALL_END;
                } else {
                    callCommand = ServiceCommand.CALL_END;
                }
                break;
        }
        if (callCommand != null) {
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
            if ("never".equals(sharedPrefs.getString("notification_mode_calls", "always"))) {
                return;
            }
            GBApplication.deviceService().onSetCallState(mSavedNumber, null, callCommand);
        }
        mLastState = state;
    }
}
