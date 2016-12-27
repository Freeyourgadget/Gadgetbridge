package nodomain.freeyourgadget.gadgetbridge.activities;

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
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.adapter.ItemWithDetailsAdapter;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.GenericItem;
import nodomain.freeyourgadget.gadgetbridge.model.ItemWithDetails;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.GB;


public class FwAppInstallerActivity extends GBActivity implements InstallActivity {

    private static final Logger LOG = LoggerFactory.getLogger(FwAppInstallerActivity.class);
    private static final String ITEM_DETAILS = "details";

    private TextView fwAppInstallTextView;
    private Button installButton;
    private Uri uri;
    private GBDevice device;
    private InstallHandler installHandler;
    private boolean mayConnect;

    private ProgressBar mProgressBar;
    private ListView itemListView;
    private final List<ItemWithDetails> mItems = new ArrayList<>();
    private ItemWithDetailsAdapter mItemAdapter;

    private ListView detailsListView;
    private ItemWithDetailsAdapter mDetailsItemAdapter;
    private ArrayList<ItemWithDetails> mDetails = new ArrayList<>();

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (GBApplication.ACTION_QUIT.equals(action)) {
                finish();
            } else if (GBDevice.ACTION_DEVICE_CHANGED.equals(action)) {
                device = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
                if (device != null) {
                    refreshBusyState(device);
                    if (!device.isInitialized()) {
                        setInstallEnabled(false);
                        if (mayConnect) {
                            GB.toast(FwAppInstallerActivity.this, getString(R.string.connecting), Toast.LENGTH_SHORT, GB.INFO);
                            connect();
                        } else {
                            setInfoText(getString(R.string.fwappinstaller_connection_state, device.getStateString()));
                        }
                    } else {
                        validateInstallation();
                    }
                }
            } else if (GB.ACTION_DISPLAY_MESSAGE.equals(action)) {
                String message = intent.getStringExtra(GB.DISPLAY_MESSAGE_MESSAGE);
                int severity = intent.getIntExtra(GB.DISPLAY_MESSAGE_SEVERITY, GB.INFO);
                addMessage(message, severity);
            }
        }
    };

    private void refreshBusyState(GBDevice dev) {
        if (dev.isConnecting() || dev.isBusy()) {
            mProgressBar.setVisibility(View.VISIBLE);
        } else {
            boolean wasBusy = mProgressBar.getVisibility() != View.GONE;
            if (wasBusy) {
                mProgressBar.setVisibility(View.GONE);
                // done!
            }
        }
    }

    private void connect() {
        mayConnect = false; // only do that once per #onCreate
        GBApplication.deviceService().connect(device);
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

        GBDevice dev = getIntent().getParcelableExtra(GBDevice.EXTRA_DEVICE);
        if (dev != null) {
            device = dev;
        }
        if (savedInstanceState != null) {
            mDetails = savedInstanceState.getParcelableArrayList(ITEM_DETAILS);
            if (mDetails == null) {
                mDetails = new ArrayList<>();
            }
        }

        mayConnect = true;
        itemListView = (ListView) findViewById(R.id.itemListView);
        mItemAdapter = new ItemWithDetailsAdapter(this, mItems);
        itemListView.setAdapter(mItemAdapter);
        fwAppInstallTextView = (TextView) findViewById(R.id.infoTextView);
        installButton = (Button) findViewById(R.id.installButton);
        mProgressBar = (ProgressBar) findViewById(R.id.installProgressBar);
        detailsListView = (ListView) findViewById(R.id.detailsListView);
        mDetailsItemAdapter = new ItemWithDetailsAdapter(this, mDetails);
        mDetailsItemAdapter.setSize(ItemWithDetailsAdapter.SIZE_SMALL);
        detailsListView.setAdapter(mDetailsItemAdapter);
        setInstallEnabled(false);
        IntentFilter filter = new IntentFilter();
        filter.addAction(GBApplication.ACTION_QUIT);
        filter.addAction(GBDevice.ACTION_DEVICE_CHANGED);
        filter.addAction(GB.ACTION_DISPLAY_MESSAGE);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filter);

        installButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setInstallEnabled(false);
                installHandler.onStartInstall(device);
                GBApplication.deviceService().onInstallApp(uri);
            }
        });

        uri = getIntent().getData();
        if (uri == null) { //for "share" intent
            uri = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
        }
        installHandler = findInstallHandlerFor(uri);
        if (installHandler == null) {
            setInfoText(getString(R.string.installer_activity_unable_to_find_handler));
        } else {
            setInfoText(getString(R.string.installer_activity_wait_while_determining_status));

            // needed to get the device
            if (device == null || !device.isConnected()) {
                connect();
            } else {
                GBApplication.deviceService().requestDeviceInfo();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(ITEM_DETAILS, mDetails);
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
        boolean enabled = device != null && device.isConnected() && enable;
        installButton.setEnabled(enabled);
        installButton.setVisibility(enabled ? View.VISIBLE : View.GONE);
    }

    @Override
    public void clearInstallItems() {
        mItems.clear();
        mItemAdapter.notifyDataSetChanged();
    }

    @Override
    public void setInstallItem(ItemWithDetails item) {
        mItems.clear();
        mItems.add(item);
        mItemAdapter.notifyDataSetChanged();
    }

    private void addMessage(String message, int severity) {
        mDetails.add(new GenericItem(message));
        mDetailsItemAdapter.notifyDataSetChanged();
    }
}
