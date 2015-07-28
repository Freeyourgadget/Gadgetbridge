package nodomain.freeyourgadget.gadgetbridge.miband;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.content.LocalBroadcastManager;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import nodomain.freeyourgadget.gadgetbridge.BluetoothCommunicationService;
import nodomain.freeyourgadget.gadgetbridge.ControlCenter;
import nodomain.freeyourgadget.gadgetbridge.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.R;


/*
TODO: This could be moved to activities package and merged with pebble/PebbleAppInstallerActivity.java
 */

public class FwUpgrade extends Activity {


    TextView fwUpgradeTextView;
    Button installButton;

    private MiBandFWHelper mFwReader = null;
    private GBDevice dev;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ControlCenter.ACTION_QUIT)) {
                finish();
            } else if (action.equals(GBDevice.ACTION_DEVICE_CHANGED)) {
                dev = intent.getParcelableExtra("device");
                if(dev.getType() == DeviceType.MIBAND) {
                    if (dev.isInitialized() && mFwReader != null) {
                        installButton.setEnabled(true);
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fw_upgrade);

        fwUpgradeTextView = (TextView) findViewById(R.id.fwUpgradeTextView);
        installButton = (Button) findViewById(R.id.installButton);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ControlCenter.ACTION_QUIT);
        filter.addAction(GBDevice.ACTION_DEVICE_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filter);

        final Uri uri = getIntent().getData();
        mFwReader = new MiBandFWHelper(uri, getApplicationContext());

        fwUpgradeTextView.setText(getString(R.string.fw_upgrade_notice, mFwReader.getHumanFirmwareVersion()));

        if (mFwReader.isFirmwareWhitelisted()) {
            fwUpgradeTextView.append(" " + getString(R.string.miband_firmware_known));
        }else {
            fwUpgradeTextView.append("  " + getString(R.string.miband_firmware_unknown_warning) + " " +
                    getString(R.string.miband_firmware_suggest_whitelist, mFwReader.getFirmwareVersion()));
        }

        installButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent startIntent = new Intent(FwUpgrade.this, BluetoothCommunicationService.class);
                startIntent.setAction(BluetoothCommunicationService.ACTION_INSTALL);
                startIntent.putExtra("uri", uri.toString());
                startService(startIntent);
            }
        });

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        super.onDestroy();
    }
}
