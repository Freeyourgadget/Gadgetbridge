/*  Copyright (C) 2015-2017 Andreas Shimokawa, Carsten Pfeiffer, Normano64

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.externalevents;

import android.app.NotificationManager;
import android.app.NotificationManager.Policy;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;


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

        int callCommand = CallSpec.CALL_UNDEFINED;
        switch (state) {
            case TelephonyManager.CALL_STATE_RINGING:
                mSavedNumber = number;
                callCommand = CallSpec.CALL_INCOMING;
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                if (mLastState == TelephonyManager.CALL_STATE_RINGING) {
                    callCommand = CallSpec.CALL_START;
                } else {
                    callCommand = CallSpec.CALL_OUTGOING;
                    mSavedNumber = number;
                }
                break;
            case TelephonyManager.CALL_STATE_IDLE:
                if (mLastState == TelephonyManager.CALL_STATE_RINGING) {
                    //missed call would be correct here
                    callCommand = CallSpec.CALL_END;
                } else {
                    callCommand = CallSpec.CALL_END;
                }
                break;
        }
        if (callCommand != CallSpec.CALL_UNDEFINED) {
            Prefs prefs = GBApplication.getPrefs();
            if ("never".equals(prefs.getString("notification_mode_calls", "always"))) {
                return;
            }
            switch (GBApplication.getGrantedInterruptionFilter()) {
                case NotificationManager.INTERRUPTION_FILTER_ALL:
                    break;
                case NotificationManager.INTERRUPTION_FILTER_ALARMS:
                case NotificationManager.INTERRUPTION_FILTER_NONE:
                    return;
                case NotificationManager.INTERRUPTION_FILTER_PRIORITY:
                    if (GBApplication.isPriorityNumber(Policy.PRIORITY_CATEGORY_CALLS, mSavedNumber)) {
                        break;
                    }
                    // FIXME: Handle Repeat callers if it is enabled in Do Not Disturb
                    return;
            }
            CallSpec callSpec = new CallSpec();
            callSpec.number = mSavedNumber;
            callSpec.command = callCommand;
            GBApplication.deviceService().onSetCallState(callSpec);
        }
        mLastState = state;
    }
}
