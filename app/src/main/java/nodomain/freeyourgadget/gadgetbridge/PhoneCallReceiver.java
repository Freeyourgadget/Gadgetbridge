package nodomain.freeyourgadget.gadgetbridge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

public class PhoneCallReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String phoneState = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        byte state = 0;
        if (phoneState.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
            state = PebbleProtocol.PHONECONTROL_INCOMINGCALL;
        } else if (phoneState.equals(TelephonyManager.EXTRA_STATE_IDLE) || phoneState.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
            state = PebbleProtocol.PHONECONTROL_END;
        }

        if (state != 0) {
            String phoneNumber = intent.hasExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ? intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) : "";
            Intent startIntent = new Intent(context, BluetoothCommunicationService.class);
            startIntent.setAction(BluetoothCommunicationService.ACTION_INCOMINGCALL);
            startIntent.putExtra("incomingcall_phonenumber", phoneNumber);
            startIntent.putExtra("incomingcall_state", state);
            context.startService(startIntent);
        }
    }

}
