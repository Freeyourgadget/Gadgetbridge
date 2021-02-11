package nodomain.freeyourgadget.gadgetbridge.activities;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class BatteryInfoActivity extends AbstractGBActivity {
    private static final Logger LOG = LoggerFactory.getLogger(BatteryInfoActivity.class);
    GBDevice gbDevice;
    private int timeFrom;
    private int timeTo;
    private int timeSpanDays = 14;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        final Context appContext = this.getApplicationContext();
        if (appContext instanceof GBApplication) {
            setContentView(R.layout.activity_battery_info);
        }

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            gbDevice = bundle.getParcelable(GBDevice.EXTRA_DEVICE);
        } else {
            throw new IllegalArgumentException("Must provide a device when invoking this activity");
        }

        final BatteryInfoChartFragment batteryInfoChartFragment = new BatteryInfoChartFragment();

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.batteryChartFragmentHolder, batteryInfoChartFragment)
                .commit();

        timeTo = (int) (System.currentTimeMillis() / 1000);
        timeFrom = timeTo - 24 * 3600 * timeSpanDays;

        batteryInfoChartFragment.setDateAndGetData(gbDevice, timeFrom, timeTo);

        TextView battery_status_device_name_text = (TextView) findViewById(R.id.battery_status_device_name);
        TextView battery_status_battery_voltage = (TextView) findViewById(R.id.battery_status_battery_voltage);
        SeekBar battery_status_time_span_seekbar = (SeekBar) findViewById(R.id.battery_status_time_span_seekbar);
        final TextView battery_status_time_span_text = (TextView) findViewById(R.id.battery_status_time_span_text);

        battery_status_time_span_seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                String text;
                switch (i) {
                    case 0:
                        text = "Day";
                        timeSpanDays = 1;
                        break;
                    case 1:
                        text = "Week";
                        timeSpanDays = 7;
                        break;
                    case 2:
                        text = "Two weeks";
                        timeSpanDays = 14;
                        break;
                    case 3:
                        text = "Month";
                        timeSpanDays = 30;
                        break;
                    case 4:
                        text = "Six months";
                        timeSpanDays = 182;
                        break;
                    case 5:
                        text = "Year";
                        timeSpanDays = 365;
                        break;
                    default:
                        text = "Two weeks";
                        timeSpanDays = 14;
                }

                battery_status_time_span_text.setText(text);
                timeFrom = timeTo - 24 * 3600 * timeSpanDays;
                batteryInfoChartFragment.setDateAndGetData(gbDevice, timeFrom, timeTo);
            }


            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        Button battery_status_calendar_button = findViewById(R.id.battery_status_calendar_button);
        battery_status_calendar_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final Calendar currentDate = Calendar.getInstance();
                currentDate.setTimeInMillis(timeTo * 1000L);
                Context context = getApplicationContext();

                if (context instanceof GBApplication) {
                    new DatePickerDialog(BatteryInfoActivity.this, new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {

                            Calendar date = Calendar.getInstance();
                            date.set(year, monthOfYear, dayOfMonth);
                            timeTo = (int) (date.getTimeInMillis() / 1000);
                            timeFrom = timeTo - 24 * 3600 * timeSpanDays;
                            batteryInfoChartFragment.setDateAndGetData(gbDevice, timeFrom, timeTo);
                        }
                    }, currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH), currentDate.get(Calendar.DATE)).show();
                }
            }
        });


        battery_status_time_span_seekbar.setProgress(2);

        ImageView battery_status_device_icon = findViewById(R.id.battery_status_device_icon);
        battery_status_device_icon.setImageResource(gbDevice.isInitialized() ? gbDevice.getType().getIcon() : gbDevice.getType().getDisabledIcon());
        TextView battery_status_battery_level_text = (TextView) findViewById(R.id.battery_status_battery_level);

        String level = gbDevice.getBatteryLevel() > 0 ? String.format("%1s%%", gbDevice.getBatteryLevel()) : "";
        String voltage = gbDevice.getBatteryVoltage() > 0 ? String.format("%1sV", gbDevice.getBatteryVoltage()) : "";

        battery_status_device_name_text.setText(gbDevice.getName());
        battery_status_battery_level_text.setText(level);
        battery_status_battery_voltage.setText(voltage);
    }

}


