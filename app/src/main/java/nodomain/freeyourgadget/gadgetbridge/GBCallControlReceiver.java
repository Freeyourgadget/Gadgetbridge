package nodomain.freeyourgadget.gadgetbridge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.ITelephony;

import java.lang.reflect.Method;

public class GBCallControlReceiver extends BroadcastReceiver {
    private final String TAG = this.getClass().getSimpleName();

    public static final String ACTION_CALLCONTROL = "nodomain.freeyourgadget.gadgetbridge.callcontrol";

    @Override
    public void onReceive(Context context, Intent intent) {
        GBCommand command = GBCommand.values()[intent.getIntExtra("command", 0)];
        int keyCode;
        switch (command) {
            case CALL_END:
                try {
                    TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                    Class clazz = Class.forName(telephonyManager.getClass().getName());
                    Method method = clazz.getDeclaredMethod("getITelephony");
                    method.setAccessible(true);
                    ITelephony telephonyService = (ITelephony) method.invoke(telephonyManager);
                    telephonyService.endCall();
                } catch (Exception e) {
                    Log.w(TAG, "could not hangup call");
                }
                break;
            default:
                return;
        }

    }
}
