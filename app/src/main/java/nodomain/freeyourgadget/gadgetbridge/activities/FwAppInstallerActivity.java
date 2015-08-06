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
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceCommunicationService;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.GB;


public class FwAppInstallerActivity extends Activity implements InstallActivity {

    private static final Logger LOG = LoggerFactory.getLogger(FwAppInstallerActivity.class);

    private TextView fwAppInstallTextView;
    private Button installButton;
    private Uri uri;
    private GBDevice device;
    private InstallHandler installHandler;
    private boolean mayConnect;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ControlCenter.ACTION_QUIT)) {
                finish();
            } else if (action.equals(GBDevice.ACTION_DEVICE_CHANGED)) {
                device = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
                if (device != null) {
                    if (!device.isConnected()) {
                        if (mayConnect) {
                            GB.toast(FwAppInstallerActivity.this, getString(R.string.connecting), Toast.LENGTH_SHORT, GB.INFO);
                            connect();
                        } else {
                            setInfoText(device.getStateString());
                        }
                    } else {
                        validateInstallation();
                    }
                }
            }
        }
    };

    private void connect() {
        mayConnect = false; // only do that once per #onCreate
        Intent startIntent = new Intent(FwAppInstallerActivity.this, DeviceCommunicationService.class);
        startIntent.setAction(DeviceCommunicationService.ACTION_CONNECT);
        if (device != null) {
            startIntent.putExtra(GBDevice.EXTRA_DEVICE, device);
        }
        startService(startIntent);
    }

    private void validateInstallation() {
        if (installHandler != null) {
            installHandler.validateInstallation(this, device);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appinstaller);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        GBDevice dev = getIntent().getParcelableExtra(GBDevice.EXTRA_DEVICE);
        if (dev != null) {
            device = dev;
        }
        mayConnect = true;
        fwAppInstallTextView = (TextView) findViewById(R.id.debugTextView);
        installButton = (Button) findViewById(R.id.installButton);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ControlCenter.ACTION_QUIT);
        filter.addAction(GBDevice.ACTION_DEVICE_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filter);

        installButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent startIntent = new Intent(FwAppInstallerActivity.this, DeviceCommunicationService.class);
                startIntent.setAction(DeviceCommunicationService.ACTION_INSTALL);
                startIntent.putExtra("uri", uri);
                startService(startIntent);
            }
        });

        uri = getIntent().getData();
        installHandler = findInstallHandlerFor(uri);
        if (installHandler == null) {
            setInfoText(getString(R.string.installer_activity_unable_to_find_handler));
            setInstallEnabled(false);
        } else {
            setInfoText(getString(R.string.installer_activity_wait_while_determining_status));
            setInstallEnabled(false);

            // needed to get the device
            if (device == null || !device.isConnected()) {
                Intent startIntent = new Intent(this, DeviceCommunicationService.class);
                startIntent.setAction(DeviceCommunicationService.ACTION_START);
                startService(startIntent);
                connect();
            }

            Intent versionInfoIntent = new Intent(this, DeviceCommunicationService.class);
            versionInfoIntent.setAction(DeviceCommunicationService.ACTION_REQUEST_VERSIONINFO);
            startService(versionInfoIntent);
        }
    }

    private InstallHandler findInstallHandlerFor(Uri uri) {
        for (DeviceCoordinator coordinator : DeviceHelper.getInstance().getAllCoordinators()) {
            InstallHandler handler = coordinator.findInstallHandler(uri, this);
            if (handler != null) {
                return handler;
            }
        }
        return null;
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

    @Override
    public void setInfoText(String text) {
        fwAppInstallTextView.setText(text);
    }

    @Override
    public void setInstallEnabled(boolean enable) {
        installButton.setEnabled(device != null && device.isConnected() && enable);
    }
}
