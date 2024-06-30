/*  Copyright (C) 2023-2024 José Rebelo

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.activities.widgets;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.capabilities.widgets.WidgetLayout;
import nodomain.freeyourgadget.gadgetbridge.capabilities.widgets.WidgetManager;
import nodomain.freeyourgadget.gadgetbridge.capabilities.widgets.WidgetPart;
import nodomain.freeyourgadget.gadgetbridge.capabilities.widgets.WidgetScreen;
import nodomain.freeyourgadget.gadgetbridge.capabilities.widgets.WidgetType;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class WidgetScreensListActivity extends AbstractGBActivity {
    private static final Logger LOG = LoggerFactory.getLogger(WidgetScreensListActivity.class);

    private WidgetScreenListAdapter mGBWidgetScreenListAdapter;
    private GBDevice gbDevice;
    private WidgetManager widgetManager;

    private ActivityResultLauncher<Intent> configureWidgetScreenLauncher;
    private final ActivityResultCallback<ActivityResult> configureWidgetCallback = result -> {
        if (result.getResultCode() == Activity.RESULT_OK) {
            updateWidgetScreensFromManager();
            sendWidgetsToDevice();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_widget_screens_list);

        gbDevice = getIntent().getParcelableExtra(GBDevice.EXTRA_DEVICE);
        if (gbDevice == null) {
            LOG.error("gbDevice must not be null");
            finish();
            return;
        }

        widgetManager = gbDevice.getDeviceCoordinator().getWidgetManager(gbDevice);
        if (widgetManager == null) {
            LOG.error("widgetManager must not be null");
            finish();
            return;
        }

        configureWidgetScreenLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                configureWidgetCallback
        );

        mGBWidgetScreenListAdapter = new WidgetScreenListAdapter(this);

        final RecyclerView widgetsRecyclerView = findViewById(R.id.widget_screens_list);
        widgetsRecyclerView.setHasFixedSize(true);
        widgetsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        widgetsRecyclerView.setAdapter(mGBWidgetScreenListAdapter);
        updateWidgetScreensFromManager();

        final FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> {
            int deviceSlots = widgetManager.getMaxScreens();

            if (mGBWidgetScreenListAdapter.getItemCount() >= deviceSlots) {
                // No more free slots
                new MaterialAlertDialogBuilder(v.getContext())
                        .setTitle(R.string.reminder_no_free_slots_title)
                        .setMessage(getBaseContext().getString(R.string.widget_screen_no_free_slots_description, String.format(Locale.getDefault(), "%d", deviceSlots)))
                        .setIcon(R.drawable.ic_warning)
                        .setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {
                        })
                        .show();
                return;
            }

            final WidgetLayout defaultLayout = widgetManager.getSupportedWidgetLayouts().get(0);
            final ArrayList<WidgetPart> defaultParts = new ArrayList<>();
            for (final WidgetType widgetType : defaultLayout.getWidgetTypes()) {
                defaultParts.add(new WidgetPart(null, "", widgetType));
            }
            final WidgetScreen widgetScreen = new WidgetScreen(
                    null,
                    defaultLayout,
                    defaultParts
            );

            configureWidgetScreen(widgetScreen);
        });
    }

    /**
     * Reads the available widgets from the database and updates the view afterwards.
     */
    @SuppressLint("NotifyDataSetChanged")
    private void updateWidgetScreensFromManager() {
        final List<WidgetScreen> widgetScreens = widgetManager.getWidgetScreens();

        mGBWidgetScreenListAdapter.setWidgetScreenList(widgetScreens);
        mGBWidgetScreenListAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            // back button
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void configureWidgetScreen(final WidgetScreen widgetScreen) {
        final Intent startIntent = new Intent(getApplicationContext(), WidgetScreenDetailsActivity.class);
        startIntent.putExtra(GBDevice.EXTRA_DEVICE, gbDevice);
        startIntent.putExtra(WidgetScreen.EXTRA_WIDGET_SCREEN, widgetScreen);
        configureWidgetScreenLauncher.launch(startIntent);
    }

    public void deleteWidgetScreen(final WidgetScreen widgetScreen) {
        if (mGBWidgetScreenListAdapter.getItemCount() - 1 < widgetManager.getMinScreens()) {
            // Under minimum slots
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.widget_screen_delete_confirm_title)
                    .setMessage(this.getString(R.string.widget_screen_min_screens, String.format(Locale.getDefault(), "%d", widgetManager.getMinScreens())))
                    .setIcon(R.drawable.ic_warning)
                    .setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {
                    })
                    .show();
            return;
        }

        widgetManager.deleteScreen(widgetScreen);

        updateWidgetScreensFromManager();
        sendWidgetsToDevice();
    }

    private void sendWidgetsToDevice() {
        if (gbDevice.isInitialized()) {
            widgetManager.sendToDevice();
        }
    }
}
