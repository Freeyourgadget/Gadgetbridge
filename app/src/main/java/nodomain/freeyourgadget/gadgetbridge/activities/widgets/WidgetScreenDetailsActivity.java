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

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.capabilities.widgets.WidgetLayout;
import nodomain.freeyourgadget.gadgetbridge.capabilities.widgets.WidgetManager;
import nodomain.freeyourgadget.gadgetbridge.capabilities.widgets.WidgetPart;
import nodomain.freeyourgadget.gadgetbridge.capabilities.widgets.WidgetPartSubtype;
import nodomain.freeyourgadget.gadgetbridge.capabilities.widgets.WidgetScreen;
import nodomain.freeyourgadget.gadgetbridge.capabilities.widgets.WidgetType;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class WidgetScreenDetailsActivity extends AbstractGBActivity {
    private static final Logger LOG = LoggerFactory.getLogger(WidgetScreenDetailsActivity.class);

    private WidgetScreen widgetScreen;
    private WidgetManager widgetManager;

    private View cardWidgetTopLeft;
    private View cardWidgetTopRight;
    private View cardWidgetCenter;
    private View cardWidgetBotLeft;
    private View cardWidgetBotRight;

    private TextView labelWidgetScreenLayout;
    private TextView labelWidgetTopLeft;
    private TextView labelWidgetTopRight;
    private TextView labelWidgetCenter;
    private TextView labelWidgetBotLeft;
    private TextView labelWidgetBotRight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_widget_screen_details);

        final GBDevice device = getIntent().getParcelableExtra(GBDevice.EXTRA_DEVICE);
        if (device == null) {
            LOG.error("device must not be null");
            finish();
            return;
        }

        widgetScreen = (WidgetScreen) getIntent().getSerializableExtra(WidgetScreen.EXTRA_WIDGET_SCREEN);
        if (widgetScreen == null) {
            GB.toast("No widget screen provided to WidgetScreenDetailsActivity", Toast.LENGTH_LONG, GB.ERROR);
            finish();
            return;
        }

        widgetManager = device.getDeviceCoordinator().getWidgetManager(device);

        // Save button
        final FloatingActionButton fab = findViewById(R.id.fab_save);
        fab.setOnClickListener(view -> {
            for (final WidgetPart part : widgetScreen.getParts()) {
                if (part.getId() == null) {
                    GB.toast(getBaseContext().getString(R.string.widget_missing_parts), Toast.LENGTH_LONG, GB.WARN);
                    return;
                }
            }

            updateWidgetScreen();
            WidgetScreenDetailsActivity.this.setResult(Activity.RESULT_OK);
            finish();
        });

        // Layouts
        final List<WidgetLayout> supportedLayouts = widgetManager.getSupportedWidgetLayouts();
        final String[] layoutStrings = new String[supportedLayouts.size()];
        for (int i = 0; i < supportedLayouts.size(); i++) {
            layoutStrings[i] = getBaseContext().getString(supportedLayouts.get(i).getName());
        }
        final ArrayAdapter<String> layoutAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, layoutStrings);

        final View cardLayout = findViewById(R.id.card_layout);
        cardLayout.setOnClickListener(view -> {
            new MaterialAlertDialogBuilder(WidgetScreenDetailsActivity.this).setAdapter(layoutAdapter, (dialogInterface, i) -> {
                if (widgetScreen.getLayout() != supportedLayouts.get(i)) {
                    final ArrayList<WidgetPart> defaultParts = new ArrayList<>();
                    for (final WidgetType widgetType : supportedLayouts.get(i).getWidgetTypes()) {
                        defaultParts.add(new WidgetPart(null, "", widgetType));
                    }
                    widgetScreen.setParts(defaultParts);
                }
                widgetScreen.setLayout(supportedLayouts.get(i));
                updateUiFromWidget();
            }).setTitle(R.string.widget_layout).create().show();
        });

        cardWidgetTopLeft = findViewById(R.id.card_widget_top_left);
        cardWidgetTopRight = findViewById(R.id.card_widget_top_right);
        cardWidgetCenter = findViewById(R.id.card_widget_center);
        cardWidgetBotLeft = findViewById(R.id.card_widget_bottom_left);
        cardWidgetBotRight = findViewById(R.id.card_widget_bottom_right);

        labelWidgetScreenLayout = findViewById(R.id.widget_screen_layout);
        labelWidgetTopLeft = findViewById(R.id.widget_top_left);
        labelWidgetTopRight = findViewById(R.id.widget_top_right);
        labelWidgetCenter = findViewById(R.id.widget_center);
        labelWidgetBotLeft = findViewById(R.id.widget_bottom_left);
        labelWidgetBotRight = findViewById(R.id.widget_bottom_right);

        updateUiFromWidget();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            // back button
            // TODO confirm when exiting without saving
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateWidgetScreen() {
        widgetManager.saveScreen(widgetScreen);
    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle state) {
        super.onSaveInstanceState(state);
        state.putSerializable("widgetScreen", widgetScreen);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        widgetScreen = (WidgetScreen) savedInstanceState.getSerializable("widgetScreen");
        updateUiFromWidget();
    }

    private void updateUiFromWidget() {
        labelWidgetScreenLayout.setText(widgetScreen.getLayout().getName());

        switch (widgetScreen.getLayout()) {
            case TOP_1_BOT_2:
            case TOP_2X2_BOT_2:
                updateWidget(cardWidgetTopLeft, labelWidgetTopLeft, -1);
                updateWidget(cardWidgetTopRight, labelWidgetTopRight, -1);
                updateWidget(cardWidgetCenter, labelWidgetCenter, 0);
                updateWidget(cardWidgetBotLeft, labelWidgetBotLeft, 1);
                updateWidget(cardWidgetBotRight, labelWidgetBotRight, 2);
                break;
            case TOP_2_BOT_1:
            case TOP_2_BOT_2X2:
                updateWidget(cardWidgetTopLeft, labelWidgetTopLeft, 0);
                updateWidget(cardWidgetTopRight, labelWidgetTopRight, 1);
                updateWidget(cardWidgetCenter, labelWidgetCenter, 2);
                updateWidget(cardWidgetBotLeft, labelWidgetBotLeft, -1);
                updateWidget(cardWidgetBotRight, labelWidgetBotRight, -1);
                break;
            case TOP_2_BOT_2:
                updateWidget(cardWidgetTopLeft, labelWidgetTopLeft, 0);
                updateWidget(cardWidgetTopRight, labelWidgetTopRight, 1);
                updateWidget(cardWidgetCenter, labelWidgetCenter, -1);
                updateWidget(cardWidgetBotLeft, labelWidgetBotLeft, 2);
                updateWidget(cardWidgetBotRight, labelWidgetBotRight, 3);
                break;
            case ONE_BY_TWO_SINGLE:
            case TWO_BY_TWO_SINGLE:
            case TWO_BY_THREE_SINGLE:
                updateWidget(cardWidgetTopLeft, labelWidgetTopLeft, -1);
                updateWidget(cardWidgetTopRight, labelWidgetTopRight, -1);
                updateWidget(cardWidgetCenter, labelWidgetCenter, 0);
                updateWidget(cardWidgetBotLeft, labelWidgetBotLeft, -1);
                updateWidget(cardWidgetBotRight, labelWidgetBotRight, -1);
                break;
            case TWO:
            case TOP_1_BOT_2X2:
            case TOP_2X2_BOT_1:
                updateWidget(cardWidgetTopLeft, labelWidgetTopLeft, -1);
                updateWidget(cardWidgetTopRight, labelWidgetTopRight, 0);
                updateWidget(cardWidgetCenter, labelWidgetCenter, 1);
                updateWidget(cardWidgetBotLeft, labelWidgetBotLeft, -1);
                updateWidget(cardWidgetBotRight, labelWidgetBotRight, -1);
                break;
            default:
                throw new IllegalStateException("Unknown layout " + widgetScreen.getLayout());
        }
    }

    private void updateWidget(final View card, final TextView label, final int partIdx) {
        final boolean validPart = partIdx >= 0 && partIdx < widgetScreen.getParts().size();

        card.setVisibility(validPart ? View.VISIBLE : View.GONE);

        if (!validPart) {
            card.setOnClickListener(null);
            label.setText(R.string.not_set);
            return;
        }

        final WidgetPart widgetPart = widgetScreen.getParts().get(partIdx);

        if (widgetPart.getId() == null) {
            label.setText(R.string.not_set);
        } else {
            label.setText(widgetPart.getFullName());
        }

        // Select widget part

        final List<WidgetPart> supportedParts = widgetManager.getSupportedWidgetParts(widgetPart.getType());
        final String[] layoutStrings = new String[supportedParts.size()];
        for (int i = 0; i < supportedParts.size(); i++) {
            layoutStrings[i] = supportedParts.get(i).getName();
        }
        final ArrayAdapter<String> partAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, layoutStrings);

        card.setOnClickListener(view -> {
            new MaterialAlertDialogBuilder(WidgetScreenDetailsActivity.this).setAdapter(partAdapter, (dialogInterface, i) -> {
                final WidgetPart selectedPart = supportedParts.get(i);

                final List<WidgetPartSubtype> supportedSubtypes = selectedPart.getSupportedSubtypes();
                if (supportedSubtypes.isEmpty()) {
                    // No subtypes selected

                    widgetScreen.getParts().set(partIdx, selectedPart);
                    updateUiFromWidget();
                    return;
                }

                // If the selected part supports subtypes, the user must select a subtype

                final String[] subtypeStrings = new String[supportedSubtypes.size()];
                for (int j = 0; j < supportedSubtypes.size(); j++) {
                    subtypeStrings[j] = supportedSubtypes.get(j).getName();
                }
                final ArrayAdapter<String> subtypesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, subtypeStrings);

                new MaterialAlertDialogBuilder(WidgetScreenDetailsActivity.this).setAdapter(subtypesAdapter, (dialogInterface1, j) -> {
                    final WidgetPartSubtype selectedSubtype = supportedSubtypes.get(j);
                    selectedPart.setSubtype(selectedSubtype);
                    widgetScreen.getParts().set(partIdx, selectedPart);
                    updateUiFromWidget();
                }).setTitle(R.string.widget_subtype).create().show();
            }).setTitle(R.string.widget).create().show();
        });
    }
}
