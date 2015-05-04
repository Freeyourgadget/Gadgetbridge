package nodomain.freeyourgadget.gadgetbridge;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;

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

                String deviceAddress = sharedPrefs.getString("last_device_address", null);
                Intent startIntent = new Intent(context, BluetoothCommunicationService.class);
                startIntent.setAction(BluetoothCommunicationService.ACTION_START);
                context.startService(startIntent);
                if (deviceAddress != null) {
                    Intent connectIntent = new Intent(context, BluetoothCommunicationService.class);
                    connectIntent.setAction(BluetoothCommunicationService.ACTION_CONNECT);
                    connectIntent.putExtra(BluetoothCommunicationService.EXTRA_DEVICE_ADDRESS, deviceAddress);
                    context.startService(connectIntent);
                }
            } else if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF) {
                Intent stopIntent = new Intent(context, BluetoothCommunicationService.class);
                context.stopService(stopIntent);

                Intent quitIntent = new Intent(ControlCenter.ACTION_QUIT);

                LocalBroadcastManager.getInstance(context).sendBroadcast(quitIntent);
            }
        }
    }
}
