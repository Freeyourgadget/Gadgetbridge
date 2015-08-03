package nodomain.freeyourgadget.gadgetbridge.activities;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.service.DeviceCommunicationService;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceApp;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandFWHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.pebble.PBWReader;


public class FwAppInstallerActivity extends Activity {

    private static final Logger LOG = LoggerFactory.getLogger(FwAppInstallerActivity.class);

    TextView fwAppInstallTextView;
    Button installButton;

    // FIXME: abstraction for these would make code cleaner in this class
    private PBWReader mPBWReader = null;
    private MiBandFWHelper mFwReader = null;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ControlCenter.ACTION_QUIT)) {
                finish();
            } else if (action.equals(GBDevice.ACTION_DEVICE_CHANGED)) {
                GBDevice dev = intent.getParcelableExtra("device");
                if (dev.getType() == DeviceType.PEBBLE && mPBWReader != null) {
                    if (mPBWReader.isFirmware()) {
                        String hwRevision = mPBWReader.getHWRevision();
                        if (hwRevision != null && hwRevision.equals(dev.getHardwareVersion()) && dev.isConnected()) {
                            installButton.setEnabled(true);
                        } else {
                            installButton.setEnabled(false);
                        }
                    } else {
                        installButton.setEnabled(dev.isConnected());
                    }
                } else if (dev.getType() == DeviceType.MIBAND && mFwReader != null) {
                    installButton.setEnabled(dev.isInitialized());
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appinstaller);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        fwAppInstallTextView = (TextView) findViewById(R.id.debugTextView);
        installButton = (Button) findViewById(R.id.installButton);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ControlCenter.ACTION_QUIT);
        filter.addAction(GBDevice.ACTION_DEVICE_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filter);

        final Uri uri = getIntent().getData();
        mPBWReader = new PBWReader(uri, getApplicationContext());
        if (mPBWReader.isValid()) {
            GBDeviceApp app = mPBWReader.getGBDeviceApp();

            if (mPBWReader.isFirmware()) {
                fwAppInstallTextView.setText(getString(R.string.firmware_install_warning, mPBWReader.getHWRevision()));

            } else if (app != null) {
                fwAppInstallTextView.setText(getString(R.string.app_install_info, app.getName(), app.getVersion(), app.getCreator()));
            }
        } else {
            mPBWReader = null;
            mFwReader = new MiBandFWHelper(uri, getApplicationContext());

            fwAppInstallTextView.setText(getString(R.string.fw_upgrade_notice, mFwReader.getHumanFirmwareVersion()));

            if (mFwReader.isFirmwareWhitelisted()) {
                fwAppInstallTextView.append(" " + getString(R.string.miband_firmware_known));
            } else {
                fwAppInstallTextView.append("  " + getString(R.string.miband_firmware_unknown_warning) + " " +
                        getString(R.string.miband_firmware_suggest_whitelist, mFwReader.getFirmwareVersion()));
            }
        }

        installButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent startIntent = new Intent(FwAppInstallerActivity.this, DeviceCommunicationService.class);
                startIntent.setAction(DeviceCommunicationService.ACTION_INSTALL);
                startIntent.putExtra("uri", uri.toString());
                startService(startIntent);
            }
        });
        Intent versionInfoIntent = new Intent(this, DeviceCommunicationService.class);
        versionInfoIntent.setAction(DeviceCommunicationService.ACTION_REQUEST_VERSIONINFO);
        startService(versionInfoIntent);
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
