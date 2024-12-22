/*  Copyright (C) 2024 Andreas Shimokawa

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
package nodomain.freeyourgadget.gadgetbridge.devices.marstek;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.gridlayout.widget.GridLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.card.MaterialCardView;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.GaugeDrawer;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class SolarEquipmentStatusActivity extends AbstractGBActivity {
    public static String ACTION_SEND_SOLAR_EQUIPMENT_STATUS = "send_solar_equipment_status";
    public static String EXTRA_BATTERY_PCT = "battery_pct";
    public static String EXTRA_PANEL1_WATT = "panel1_watt";
    public static String EXTRA_PANEL2_WATT = "panel2_watt";
    public static String EXTRA_OUTPUT1_WATT = "output1_watt";
    public static String EXTRA_OUTPUT2_WATT = "output2_watt";

    private final Map<String, View> widgetMap = new HashMap<>();
    private GridLayout gridLayout;
    private SwipeRefreshLayout swipeLayout;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Objects.requireNonNull(action).equals(ACTION_SEND_SOLAR_EQUIPMENT_STATUS)) {
                Bundle extras = intent.getExtras();
                if (extras != null) {
                    int battery_pct = extras.getInt(EXTRA_BATTERY_PCT);
                    int panel1_watt = extras.getInt(EXTRA_PANEL1_WATT);
                    int panel2_watt = extras.getInt(EXTRA_PANEL2_WATT);
                    int output1_watt = extras.getInt(EXTRA_OUTPUT1_WATT);
                    int output2_watt = extras.getInt(EXTRA_OUTPUT2_WATT);
                    updateWidget("battery", battery_pct + "%", (float) (battery_pct / 100.0));
                    updateWidget("panel1", panel1_watt + "W", (float) (panel1_watt / 380.0));
                    updateWidget("panel2", panel2_watt + "W", (float) (panel1_watt / 380.0));
                    updateWidget("output1", output1_watt + "W", (float) (output1_watt / 400.0));
                    updateWidget("output2", output2_watt + "W", (float) (output1_watt / 400.0));

                    swipeLayout.setRefreshing(false);
                }
            }
        }
    };
    private GBDevice gBDevice = null;

    public static int[] getColors() {
        return new int[]{
                ContextCompat.getColor(GBApplication.getContext(), R.color.chart_stress_unknown),
                ContextCompat.getColor(GBApplication.getContext(), R.color.vo2max_value_poor_color),
                ContextCompat.getColor(GBApplication.getContext(), R.color.vo2max_value_fair_color),
                ContextCompat.getColor(GBApplication.getContext(), R.color.vo2max_value_good_color),
        };
    }

    public static int[] getColorsOutput() {
        return new int[]{
                ContextCompat.getColor(GBApplication.getContext(), R.color.chart_stress_unknown),
                ContextCompat.getColor(GBApplication.getContext(), R.color.vo2max_value_good_color),
                ContextCompat.getColor(GBApplication.getContext(), R.color.vo2max_value_fair_color),
                ContextCompat.getColor(GBApplication.getContext(), R.color.vo2max_value_poor_color),
        };
    }

    public static float[] getSegments() {
        return new float[]{
                0.01f,
                0.09f,
                0.2f,
                0.7f,
        };
    }

    public static float[] getSegmentsOutput() {
        return new float[]{
                0.01f,
                0.33f,
                0.33f,
                0.33f,
        };
    }

    private void updateWidget(String name, String value, float gaugeFill) {
        View view = widgetMap.get(name);
        if (view != null) {
            TextView gaugeValue = view.findViewById(R.id.gauge_value);
            gaugeValue.setText(value);
            GaugeDrawer gaugeDrawer = new GaugeDrawer();
            ImageView gaugeBar = view.findViewById(R.id.gauge_bar);
            gaugeDrawer.drawSegmentedGauge(gaugeBar, name.startsWith("output") ? getColorsOutput() : getColors(), name.startsWith("output") ? getSegmentsOutput() : getSegments(), gaugeFill, true, false);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            gBDevice = extras.getParcelable(GBDevice.EXTRA_DEVICE);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_solar_equipment_status);
        gridLayout = findViewById(R.id.solarequipmentview_gridlayout);
        createWidget("panel1", "Panel 1", 1);
        createWidget("panel2", "Panel 2", 1);
        createWidget("battery", "Battery", 2);
        createWidget("output1", "Output 1", 1);
        createWidget("output2", "Output 2", 1);

        // Set pull-down-to-refresh action
        swipeLayout = findViewById(R.id.solarequipmentview_swipe_layout);
        swipeLayout.setOnRefreshListener(() -> {
            if (gBDevice == null || !gBDevice.isInitialized()) {
                swipeLayout.setRefreshing(false);
                GB.toast(getString(R.string.info_no_devices_connected), Toast.LENGTH_LONG, GB.WARN);
                return;
            }
            GBApplication.deviceService(gBDevice).onFetchRecordedData(0);
        });

        IntentFilter filterLocal = new IntentFilter();
        filterLocal.addAction(ACTION_SEND_SOLAR_EQUIPMENT_STATUS);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filterLocal);
    }

    private void createWidget(String name, String label, int columnSpan) {
        final float scale = getResources().getDisplayMetrics().density;

        GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL, 1f),
                GridLayout.spec(GridLayout.UNDEFINED, columnSpan, GridLayout.FILL, 1f)
        );
        layoutParams.width = 0;
        int pixels_8dp = (int) (8 * scale + 0.5f);
        layoutParams.setMargins(pixels_8dp, pixels_8dp, pixels_8dp, pixels_8dp);

        MaterialCardView card = new MaterialCardView(this);
        int pixels_4dp = (int) (4 * scale + 0.5f);
        card.setRadius(pixels_4dp);
        card.setCardElevation(pixels_4dp);
        card.setContentPadding(pixels_4dp, pixels_4dp, pixels_4dp, pixels_4dp);
        card.setLayoutParams(layoutParams);
        LayoutInflater inflater = getLayoutInflater();

        final View gaugeView = inflater.inflate(R.layout.dashboard_widget_generic_gauge, card, false);
        final TextView gaugeLabel = gaugeView.findViewById(R.id.gauge_label);
        gaugeLabel.setText(label);

        card.addView(gaugeView);
        widgetMap.put(name, gaugeView);
        gridLayout.addView(card);
    }

}
