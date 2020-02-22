/*  Copyright (C) 2015-2020 Andreas Shimokawa, Carsten Pfeiffer

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
package nodomain.freeyourgadget.gadgetbridge.service.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;

import androidx.annotation.RequiresApi;

import com.android.internal.telephony.ITelephony;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventCallControl;

public class GBCallControlReceiver extends BroadcastReceiver {
    public static final String ACTION_CALLCONTROL = "nodomain.freeyourgadget.gadgetbridge.callcontrol";
    private static final Logger LOG = LoggerFactory.getLogger(GBCallControlReceiver.class);
    private Context mContext = GBApplication.getContext();

    @Override
    public void onReceive(Context context, Intent intent) {
        GBDeviceEventCallControl.Event callCmd = GBDeviceEventCallControl.Event.values()[intent.getIntExtra("event", 0)];

        if (GBApplication.isRunningPieOrLater()) {
            handleCallCmdTelecomManager(callCmd);
        } else {
            switch (callCmd) {
                case END:
                case REJECT:
                case START:
                    try {
                        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                        Class clazz = Class.forName(telephonyManager.getClass().getName());
                        Method method = clazz.getDeclaredMethod("getITelephony");
                        method.setAccessible(true);
                        ITelephony telephonyService = (ITelephony) method.invoke(telephonyManager);
                        if (callCmd == GBDeviceEventCallControl.Event.END || callCmd == GBDeviceEventCallControl.Event.REJECT) {
                            telephonyService.endCall();
                        } else {
                            telephonyService.answerRingingCall();
                        }
                    } catch (Exception e) {
                        LOG.warn("could not start or hangup call");
                    }
                    break;
                default:
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    public void handleCallCmdTelecomManager(GBDeviceEventCallControl.Event callCmd) {
        try {
            TelecomManager tm = (TelecomManager) mContext.getSystemService(Context.TELECOM_SERVICE);

            if (callCmd == GBDeviceEventCallControl.Event.END || callCmd == GBDeviceEventCallControl.Event.REJECT) {
                tm.endCall();
            } else if (callCmd == GBDeviceEventCallControl.Event.START || callCmd == GBDeviceEventCallControl.Event.ACCEPT) {
                tm.acceptRingingCall();
            }

        } catch (SecurityException e) {
            LOG.warn("no permission to start or hangup call");
        } catch (Exception e) {
            LOG.warn("could not start or hangup call");
        }
    }
}
