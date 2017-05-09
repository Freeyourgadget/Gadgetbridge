/*  Copyright (C) 2015-2017 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, Lem Dulfo

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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


public class FwAppInstallerActivity extends AppCompatActivity implements InstallActivity {

    private static final Logger LOG = LoggerFactory.getLogger(FwAppInstallerActivity.class);
    private static final String ITEM_DETAILS = "details";
    private final List<ItemWithDetails> mItems = new ArrayList<>();
    private TextView fwAppInstallTextView;
    private Button installButton;
    private Uri uri;
    private GBDevice device;
    private InstallHandler installHandler;
    private boolean mayConnect;
    private ProgressBar mProgressBar;
    private ListView itemListView;
    private ItemWithDetailsAdapter mItemAdapter;

    private ListView detailsListView;
    private ItemWithDetailsAdapter mDetailsItemAdapter;
    private ArrayList<ItemWithDetails> mDetails = new ArrayList<>();

    private final BroadcastReceiver installationResultReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Receive installation fail/success intent
            if (mProgressBar != null)
                mProgressBar.setVisibility(View.INVISIBLE);
            if (installButton != null) {
                installButton.setVisibility(View.VISIBLE);
                installButton.setClickable(false);
                if (intent.getAction().equals("nodomain.freeyourgadget.gadgetbridge.INSTALLATION_FAILED")) {
                    installButton.setText(getString(R.string.installation_failed_));
                } else if (intent.getAction().equals("nodomain.freeyourgadget.gadgetbridge.INSTALLATION_SUCCESS")) {
                    installButton.setText(getString(R.string.installation_successful));
                }
            }
        }
    };

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
        if (GBApplication.isDarkThemeEnabled())
            setTheme(R.style.GadgetbridgeThemeDark_NoActionBar);

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
                mProgressBar.setVisibility(View.VISIBLE);
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction("nodomain.freeyourgadget.gadgetbridge.INSTALLATION_FAILED");
                intentFilter.addAction("nodomain.freeyourgadget.gadgetbridge.INSTALLATION_SUCCESS");
                registerReceiver(installationResultReceiver, intentFilter);
            }
        });

        uri = getIntent().getData();
        if (uri == null) { //for "share" intent
            uri = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
        }

        //Get apps count and app index from calling intent
        int appsCount = getIntent().getIntExtra("APPS_COUNT", 1);
        int currentAppIndex = getIntent().getIntExtra("APP_INDEX", 1);

        //Set up the toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(getResources().getString(R.string.installation_d_d, currentAppIndex, appsCount));
        toolbar.setNavigationIcon(VectorDrawableCompat.create(getResources(), R.drawable.ic_arrow_back, null));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        setSupportActionBar(toolbar);

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
    public CharSequence getInfoText() {
        return fwAppInstallTextView.getText();
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
