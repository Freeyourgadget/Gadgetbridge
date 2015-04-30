package nodomain.freeyourgadget.gadgetbridge.pebble;

import nodomain.freeyourgadget.gadgetbridge.BluetoothCommunicationService;
import nodomain.freeyourgadget.gadgetbridge.ControlCenter;
import nodomain.freeyourgadget.gadgetbridge.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.GBDeviceApp;
import nodomain.freeyourgadget.gadgetbridge.R;

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


public class PebbleAppInstallerActivity extends Activity {

    private final String TAG = this.getClass().getSimpleName();

    TextView debugTextView;
    Button installButton;

    private PBWReader mPBWReader = null;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ControlCenter.ACTION_QUIT)) {
                finish();
            } else if (action.equals(GBDevice.ACTION_DEVICE_CHANGED)) {
                GBDevice dev = intent.getParcelableExtra("device");
                if (mPBWReader != null) {
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
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appinstaller);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        debugTextView = (TextView) findViewById(R.id.debugTextView);
        installButton = (Button) findViewById(R.id.installButton);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ControlCenter.ACTION_QUIT);
        filter.addAction(GBDevice.ACTION_DEVICE_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filter);

        final Uri uri = getIntent().getData();
        mPBWReader = new PBWReader(uri, getApplicationContext());
        GBDeviceApp app = mPBWReader.getGBDeviceApp();

        if (mPBWReader.isFirmware()) {
            debugTextView.setText(getString(R.string.firmware_install_warning, mPBWReader.getHWRevision()));

        } else if (app != null) {
            debugTextView.setText(getString(R.string.app_install_info, app.getName(), app.getVersion(), app.getCreator()));
        }

        installButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent startIntent = new Intent(PebbleAppInstallerActivity.this, BluetoothCommunicationService.class);
                startIntent.setAction(BluetoothCommunicationService.ACTION_INSTALL_PEBBLEAPP);
                startIntent.putExtra("app_uri", uri.toString());
                startService(startIntent);
            }
        });
        Intent versionInfoIntent = new Intent(this, BluetoothCommunicationService.class);
        versionInfoIntent.setAction(BluetoothCommunicationService.ACTION_REQUEST_VERSIONINFO);
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
