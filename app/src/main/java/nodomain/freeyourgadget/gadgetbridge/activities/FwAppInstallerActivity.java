/*  Copyright (C) 2015-2020 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, Lem Dulfo, Taavi Eomäe

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
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.NavUtils;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.adapter.ItemWithDetailsAdapter;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.GenericItem;
import nodomain.freeyourgadget.gadgetbridge.model.ItemWithDetails;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.GB;


public class FwAppInstallerActivity extends AbstractGBActivity implements InstallActivity {

    private static final Logger LOG = LoggerFactory.getLogger(FwAppInstallerActivity.class);
    private static final String ITEM_DETAILS = "details";

    private TextView fwAppInstallTextView;
    private Button installButton;
    private Uri uri;
    private GBDevice device;
    private InstallHandler installHandler;
    private boolean mayConnect;

    private ProgressBar progressBar;
    private TextView progressText;
    private ListView itemListView;
    private final List<ItemWithDetails> items = new ArrayList<>();
    private ItemWithDetailsAdapter itemAdapter;

    private ListView detailsListView;
    private ItemWithDetailsAdapter detailsAdapter;
    private ArrayList<ItemWithDetails> details = new ArrayList<>();

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (GBDevice.ACTION_DEVICE_CHANGED.equals(action)) {
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
            } else if (GB.ACTION_SET_PROGRESS_BAR.equals(action)) {
                if (intent.hasExtra(GB.PROGRESS_BAR_INDETERMINATE)) {
                    setProgressIndeterminate(intent.getBooleanExtra(GB.PROGRESS_BAR_INDETERMINATE, false));
                }

                if (intent.hasExtra(GB.PROGRESS_BAR_PROGRESS)) {
                    setProgressIndeterminate(false);
                    setProgressBar(intent.getIntExtra(GB.PROGRESS_BAR_PROGRESS, 0));
                }
            } else if (GB.ACTION_SET_PROGRESS_TEXT.equals(action)) {
                if (intent.hasExtra(GB.DISPLAY_MESSAGE_MESSAGE)) {
                    setProgressText(intent.getStringExtra(GB.DISPLAY_MESSAGE_MESSAGE));
                }
            } else if (GB.ACTION_SET_INFO_TEXT.equals(action)) {
                if (intent.hasExtra(GB.DISPLAY_MESSAGE_MESSAGE)) {
                    setInfoText(intent.getStringExtra(GB.DISPLAY_MESSAGE_MESSAGE));
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
            progressBar.setVisibility(View.VISIBLE);
        } else {
            boolean wasBusy = progressBar.getVisibility() != View.GONE;
            if (wasBusy) {
                progressBar.setVisibility(View.GONE);
                // done!
            }
        }
    }

    public void setProgressIndeterminate(boolean indeterminate) {
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setIndeterminate(indeterminate);
    }

    public void setProgressBar(int progress) {
        progressBar.setProgress(progress);
    }

    public void setProgressText(String text) {
        progressText.setVisibility(View.VISIBLE);
        progressText.setText(text);
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
            details = savedInstanceState.getParcelableArrayList(ITEM_DETAILS);
            if (details == null) {
                details = new ArrayList<>();
            }
        }

        mayConnect = true;
        itemListView = findViewById(R.id.itemListView);
        itemAdapter = new ItemWithDetailsAdapter(this, items);
        itemListView.setAdapter(itemAdapter);
        fwAppInstallTextView = findViewById(R.id.infoTextView);
        installButton = findViewById(R.id.installButton);
        progressBar = findViewById(R.id.installProgressBar);
        progressText = findViewById(R.id.installProgressText);
        detailsListView = findViewById(R.id.detailsListView);
        detailsAdapter = new ItemWithDetailsAdapter(this, details);
        detailsAdapter.setSize(ItemWithDetailsAdapter.SIZE_SMALL);
        detailsListView.setAdapter(detailsAdapter);

        setInstallEnabled(false);
        IntentFilter filter = new IntentFilter();
        filter.addAction(GBDevice.ACTION_DEVICE_CHANGED);
        filter.addAction(GB.ACTION_DISPLAY_MESSAGE);
        filter.addAction(GB.ACTION_SET_PROGRESS_BAR);
        filter.addAction(GB.ACTION_SET_PROGRESS_TEXT);
        filter.addAction(GB.ACTION_SET_INFO_TEXT);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);

        installButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setInstallEnabled(false);
                installHandler.onStartInstall(device);
                GBApplication.deviceService().onInstallApp(uri);
            }
        });

        uri = getIntent().getData();
        if (uri == null) { // For "share" intent
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
        outState.putParcelableArrayList(ITEM_DETAILS, details);
    }

    private InstallHandler findInstallHandlerFor(Uri uri) {
        for (DeviceCoordinator coordinator : getAllCoordinatorsConnectedFirst()) {
            InstallHandler handler = coordinator.findInstallHandler(uri, this);
            if (handler != null) {
                return handler;
            }
        }
        return null;
    }

    private List<DeviceCoordinator> getAllCoordinatorsConnectedFirst() {
        DeviceManager deviceManager = ((GBApplication) getApplicationContext()).getDeviceManager();
        List<DeviceCoordinator> connectedCoordinators = new ArrayList<>();
        List<DeviceCoordinator> allCoordinators = DeviceHelper.getInstance().getAllCoordinators();
        List<DeviceCoordinator> sortedCoordinators = new ArrayList<>(allCoordinators.size());

        GBDevice connectedDevice = deviceManager.getSelectedDevice();
        if (connectedDevice != null && connectedDevice.isConnected()) {
            DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(connectedDevice);
            if (coordinator != null) {
                connectedCoordinators.add(coordinator);
            }
        }

        sortedCoordinators.addAll(connectedCoordinators);
        for (DeviceCoordinator coordinator : allCoordinators) {
            if (!connectedCoordinators.contains(coordinator)) {
                sortedCoordinators.add(coordinator);
            }
        }
        return sortedCoordinators;
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
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        super.onDestroy();
    }

    @Override
    public void setInfoText(String text) {
        fwAppInstallTextView.setText(text);
    }

    @Override
    public CharSequence getInfoText() {
        return fwAppInstallTextView.getText();
    }

    @Override
    public void setInstallEnabled(boolean enable) {
        boolean enabled = device != null && device.isConnected() && enable;
        installButton.setEnabled(enabled);
        installButton.setVisibility(enabled ? View.VISIBLE : View.GONE);
    }

    @Override
    public void clearInstallItems() {
        items.clear();
        itemAdapter.notifyDataSetChanged();
    }

    @Override
    public void setInstallItem(ItemWithDetails item) {
        items.clear();
        items.add(item);
        itemAdapter.notifyDataSetChanged();
    }

    private void addMessage(String message, int severity) {
        details.add(new GenericItem(message));
        detailsAdapter.notifyDataSetChanged();
    }
}
