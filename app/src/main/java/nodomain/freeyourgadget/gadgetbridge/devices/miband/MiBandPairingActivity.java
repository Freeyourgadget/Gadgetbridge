package nodomain.freeyourgadget.gadgetbridge.devices.miband;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.ControlCenter;
import nodomain.freeyourgadget.gadgetbridge.activities.DiscoveryActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class MiBandPairingActivity extends Activity {
    private static final Logger LOG = LoggerFactory.getLogger(MiBandPairingActivity.class);

    private static final int REQ_CODE_USER_SETTINGS = 52;
    private static final String STATE_MIBAND_ADDRESS = "mibandMacAddress";
    private TextView message;
    private boolean isPairing;
    private String macAddress;
    private final BroadcastReceiver mPairingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (GBDevice.ACTION_DEVICE_CHANGED.equals(intent.getAction())) {
                GBDevice device = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
                LOG.debug("pairing activity: device changed: " + device);
                if (macAddress.equals(device.getAddress()) && device.isInitialized()) {
                    pairingFinished(true);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mi_band_pairing);

        message = (TextView) findViewById(R.id.miband_pair_message);
        Intent intent = getIntent();
        macAddress = intent.getStringExtra(DeviceCoordinator.EXTRA_DEVICE_MAC_ADDRESS);
        if (macAddress == null && savedInstanceState != null) {
            macAddress = savedInstanceState.getString(STATE_MIBAND_ADDRESS, null);
        }
        if (macAddress == null) {
            Toast.makeText(this, getString(R.string.message_cannot_pair_no_mac), Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, DiscoveryActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
            return;
        }

        if (!MiBandCoordinator.hasValidUserInfo()) {
            Intent userSettingsIntent = new Intent(this, MiBandPreferencesActivity.class);
            startActivityForResult(userSettingsIntent, REQ_CODE_USER_SETTINGS, null);
            return;
        }

        // already valid user info available, use that and pair
        startPairing();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_MIBAND_ADDRESS, macAddress);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        macAddress = savedInstanceState.getString(STATE_MIBAND_ADDRESS, macAddress);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // start pairing immediately when we return from the user settings
        if (requestCode == REQ_CODE_USER_SETTINGS) {
            if (!MiBandCoordinator.hasValidUserInfo()) {
                Toast.makeText(this, getString(R.string.miband_pairing_using_dummy_userdata), Toast.LENGTH_SHORT).show();
            }
            startPairing();
        }
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mPairingReceiver);
        if (isPairing) {
            stopPairing();
        }
        super.onDestroy();
    }

    private void startPairing() {
        isPairing = true;
        message.setText(getString(R.string.miband_pairing, macAddress));
        IntentFilter filter = new IntentFilter(GBDevice.ACTION_DEVICE_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mPairingReceiver, filter);

        GBApplication.deviceService().connect(macAddress, true);
    }

    private void pairingFinished(boolean pairedSuccessfully) {
        isPairing = false;
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mPairingReceiver);

        if (pairedSuccessfully) {
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
            sharedPrefs.edit().putString(MiBandConst.PREF_MIBAND_ADDRESS, macAddress).apply();
        }

        Intent intent = new Intent(this, ControlCenter.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private void stopPairing() {
        // TODO
        isPairing = false;
    }
}
