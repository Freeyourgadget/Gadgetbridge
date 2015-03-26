package nodomain.freeyourgadget.gadgetbridge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.ITelephony;

import java.lang.reflect.Method;

import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceCommandCallControl;

public class GBCallControlReceiver extends BroadcastReceiver {
    public static final String ACTION_CALLCONTROL = "nodomain.freeyourgadget.gadgetbridge.callcontrol";
    private final String TAG = this.getClass().getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        GBDeviceCommandCallControl.Command callCmd = GBDeviceCommandCallControl.Command.values()[intent.getIntExtra("command", 0)];
        switch (callCmd) {
            case END:
            case START:
                try {
                    TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                    Class clazz = Class.forName(telephonyManager.getClass().getName());
                    Method method = clazz.getDeclaredMethod("getITelephony");
                    method.setAccessible(true);
                    ITelephony telephonyService = (ITelephony) method.invoke(telephonyManager);
                    if (callCmd == GBDeviceCommandCallControl.Command.END) {
                        telephonyService.endCall();
                    } else {
                        telephonyService.answerRingingCall();
                    }
                } catch (Exception e) {
                    Log.w(TAG, "could not start or hangup call");
                }
                break;
            default:
                return;
        }

    }
}
