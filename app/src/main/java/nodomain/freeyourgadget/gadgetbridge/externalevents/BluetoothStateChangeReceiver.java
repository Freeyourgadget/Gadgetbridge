package nodomain.freeyourgadget.gadgetbridge.externalevents;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.ControlCenter;

public class BluetoothStateChangeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_ON) {

                Intent refreshIntent = new Intent(ControlCenter.ACTION_REFRESH_DEVICELIST);
                LocalBroadcastManager.getInstance(context).sendBroadcast(refreshIntent);

                SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
                if (!sharedPrefs.getBoolean("general_autoconnectonbluetooth", false)) {
                    return;
                }

                GBApplication.deviceService().connect();
            } else if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF) {
                GBApplication.deviceService().quit();

                Intent quitIntent = new Intent(ControlCenter.ACTION_QUIT);

                LocalBroadcastManager.getInstance(context).sendBroadcast(quitIntent);
            }
        }
    }
}
