/*  Copyright (C) 2015-2020 Andreas Böhler, Andreas Shimokawa, Carsten
    Pfeiffer, Daniele Gobbetti, Johannes Tysiak, Normano64

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
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.telephony.TelephonyManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;


public class PhoneCallReceiver extends BroadcastReceiver {
    private static final Logger LOG = LoggerFactory.getLogger(PhoneCallReceiver.class);

    private static int mLastState = TelephonyManager.CALL_STATE_IDLE;
    private static String mSavedNumber;
    private boolean mRestoreMutedCall = false;
    private int mLastRingerMode;

    private final Handler delayHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onReceive(Context context, Intent intent) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Service.TELEPHONY_SERVICE);
        if (intent.getAction().equals("android.intent.action.NEW_OUTGOING_CALL")) {
            mSavedNumber = intent.getExtras().getString("android.intent.extra.PHONE_NUMBER");
        } else if (intent.getAction().equals("nodomain.freeyourgadget.gadgetbridge.MUTE_CALL")) {
            // Handle the mute request only if the phone is currently ringing
            if (mLastState != TelephonyManager.CALL_STATE_RINGING)
                return;

            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            mLastRingerMode = audioManager.getRingerMode();
            try {
                audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            } catch (SecurityException e) {
                LOG.error("SecurityException when trying to set ringer (no permission granted :/ ?), not setting it then.");
            }
            mRestoreMutedCall = true;
        } else {
            if (intent.hasExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)) {
                String number = intent.getExtras().getString(TelephonyManager.EXTRA_INCOMING_NUMBER);
                int state = tm.getCallState();
                onCallStateChanged(context, state, number);
            }
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
                if (mRestoreMutedCall) {
                    mRestoreMutedCall = false;
                    AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                    audioManager.setRingerMode(mLastRingerMode);
                }
                break;
        }
        if (callCommand != CallSpec.CALL_UNDEFINED) {
            Prefs prefs = GBApplication.getPrefs();
            if ("never".equals(prefs.getString("notification_mode_calls", "always"))) {
                return;
            }
            int dndSuppressed = 0;
            switch (GBApplication.getGrantedInterruptionFilter()) {
                case NotificationManager.INTERRUPTION_FILTER_ALL:
                    break;
                case NotificationManager.INTERRUPTION_FILTER_ALARMS:
                case NotificationManager.INTERRUPTION_FILTER_NONE:
                    dndSuppressed = 1;
                    break;
                case NotificationManager.INTERRUPTION_FILTER_PRIORITY:
                    if (GBApplication.isPriorityNumber(Policy.PRIORITY_CATEGORY_CALLS, mSavedNumber)) {
                        break;
                    }
                    // FIXME: Handle Repeat callers if it is enabled in Do Not Disturb
                    dndSuppressed = 1;
            }
            if (prefs.getBoolean("notification_filter", false) && dndSuppressed == 1) {
                return;
            }
            CallSpec callSpec = new CallSpec();
            callSpec.number = mSavedNumber;
            callSpec.command = callCommand;
            callSpec.dndSuppressed = dndSuppressed;

            int callDelay = prefs.getInt("notification_delay_calls", 0);
            if (callCommand == CallSpec.CALL_INCOMING) {
                // Delay incoming call notifications by a configurable number of seconds
                if (callDelay <= 0) {
                    GBApplication.deviceService().onSetCallState(callSpec);
                } else {
                    scheduleOnSetCallState(callSpec, callDelay);
                }
            } else {
                if (callCommand == CallSpec.CALL_START || callCommand == CallSpec.CALL_END) {
                    // Call started or ended, unschedule any outstanding notifications
                    unscheduleAllOnSetCallState();
                }

                // propagate the event to the device
                GBApplication.deviceService().onSetCallState(callSpec);
            }
        }
        mLastState = state;
    }

    private void scheduleOnSetCallState(final CallSpec callSpec, final int delaySeconds) {
        final Runnable runnable = new Runnable() {
            @Override public void run() {
                GBApplication.deviceService().onSetCallState(callSpec);
            }
        };

        delayHandler.postDelayed(runnable, delaySeconds * 1000);
    }

    private void unscheduleAllOnSetCallState() {
        delayHandler.removeCallbacksAndMessages(null);
    }
}
