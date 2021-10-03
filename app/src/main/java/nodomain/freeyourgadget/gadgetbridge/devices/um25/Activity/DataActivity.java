package nodomain.freeyourgadget.gadgetbridge.devices.um25.Activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.lang.reflect.Field;
import java.util.HashMap;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.service.devices.um25.Data.MeasurementData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.um25.Support.UM25Support;

public class DataActivity extends AbstractGBActivity {
    private HashMap<Integer, TextView> valueViews = new HashMap<>(ValueDisplay.values().length);

    private TextView chargeDurationTextView;

    private enum ValueDisplay{
        VOLTAGE("voltage", "%.3fV", R.id.um25_text_voltage, 1000),
        CURRENT("current", "%.4fA", R.id.um25_text_current, 10000),
        CURRENT_SUM("chargedCurrent", "%.0fmAh", R.id.um25_text_current_sum, 1),
        WATTAGE("wattage", "%.3fW", R.id.um25_text_wattage, 1000),
        WATTAGE_SUM("chargedWattage", "%.3fWh", R.id.um25_text_wattage_sum, 1000),
        TEMPERATURE_CELCIUS("temperatureCelcius", "%.0f°", R.id.um25_text_temperature, 1),
        CABLE_RESISTANCE("cableResistance", "%.1fΩ", R.id.um25_cable_resistance, 10),
        ;

        private String variableName;
        private String formatString;
        private int textViewResource;
        private float divisor;

        ValueDisplay(String variableName, String formatString, int textViewResource, float divisor) {
            this.variableName = variableName;
            this.formatString = formatString;
            this.textViewResource = textViewResource;
            this.divisor = divisor;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_um25_data);

        chargeDurationTextView = findViewById(R.id.um25_text_charge_duration);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(
                        measurementReceiver,
                        new IntentFilter(UM25Support.ACTION_MEASUREMENT_TAKEN)
                );
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(measurementReceiver);
        for(TextView view : valueViews.values()){
            view.setText("-");
        }
    }

    private void displayMeasurementData(MeasurementData data){
        for(ValueDisplay display : ValueDisplay.values()){
            try {
                TextView textView = valueViews.get(display.textViewResource);
                if(textView == null){
                    valueViews.put(display.textViewResource, textView = findViewById(display.textViewResource));
                }
                Field field = data.getClass().getDeclaredField(display.variableName);
                field.setAccessible(true);
                float value = ((int) field.get(data)) / display.divisor;
                String result = String.format(display.formatString, value);
                textView.setText(result);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        int durationSeconds = data.getChargingSeconds();
        int hours = durationSeconds / 3600;
        int minutes = durationSeconds % 3600 / 60;
        int seconds = durationSeconds % 60;

        String timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds);

        int thresholdCurrent = data.getThresholdCurrent();
        int current = data.getCurrent() / 10;

        chargeDurationTextView.setTextColor(current > thresholdCurrent ? 0xff669900 : 0xffcc0000);
        chargeDurationTextView.setText(timeString);
    }

    private BroadcastReceiver measurementReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            MeasurementData data = (MeasurementData) intent.getSerializableExtra(UM25Support.EXTRA_KEY_MEASUREMENT_DATA);
            displayMeasurementData(data);
        }
    };
}
