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

    private enum ValueDisplay{
        VOLTAGE("voltage", "%.3fV", R.id.um25_text_voltage, 1000),
        CURRENT("current", "%.4fA", R.id.um25_text_current, 10000),
        WATTAGE("wattage", "%.3fW", R.id.um25_text_wattage, 1000),
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
    }

    private BroadcastReceiver measurementReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            MeasurementData data = (MeasurementData) intent.getSerializableExtra(UM25Support.EXTRA_KEY_MEASUREMENT_DATA);
            displayMeasurementData(data);
        }
    };
}
