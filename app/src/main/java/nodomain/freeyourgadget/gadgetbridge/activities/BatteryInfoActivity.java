package nodomain.freeyourgadget.gadgetbridge.activities;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Date;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;

public class BatteryInfoActivity extends AbstractGBActivity {
    private static final Logger LOG = LoggerFactory.getLogger(BatteryInfoActivity.class);
    GBDevice gbDevice;
    private int timeFrom;
    private int timeTo;

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

        batteryInfoChartFragment.setDateAndGetData(gbDevice, timeFrom, timeTo);

        TextView battery_status_device_name_text = (TextView) findViewById(R.id.battery_status_device_name);
        TextView battery_status_battery_voltage = (TextView) findViewById(R.id.battery_status_battery_voltage);
        final TextView battery_status_date_from_text = (TextView) findViewById(R.id.battery_status_date_from_text);
        final TextView battery_status_date_to_text = (TextView) findViewById(R.id.battery_status_date_to_text);
        final SeekBar battery_status_time_span_seekbar = (SeekBar) findViewById(R.id.battery_status_time_span_seekbar);
        final TextView battery_status_time_span_text = (TextView) findViewById(R.id.battery_status_time_span_text);

        LinearLayout battery_status_date_to_layout = (LinearLayout) findViewById(R.id.battery_status_date_to_layout);

        battery_status_time_span_seekbar.setMax(5);
        battery_status_time_span_seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                String text;
                switch (i) {
                    case 0:
                        text = getString(R.string.calendar_day);
                        timeFrom = DateTimeUtils.shiftDays(timeTo, -1);
                        break;
                    case 1:
                        text = getString(R.string.calendar_week);
                        timeFrom = DateTimeUtils.shiftDays(timeTo, -7);
                        break;
                    case 2:
                        text = getString(R.string.calendar_two_weeks);
                        timeFrom = DateTimeUtils.shiftDays(timeTo, -14);
                        break;
                    case 3:
                        text = getString(R.string.calendar_month);
                        timeFrom = DateTimeUtils.shiftMonths(timeTo, -1);
                        break;
                    case 4:
                        text = getString(R.string.calendar_six_months);
                        timeFrom = DateTimeUtils.shiftMonths(timeTo, -6);
                        break;
                    case 5:
                        text = getString(R.string.calendar_year);
                        timeFrom = DateTimeUtils.shiftMonths(timeTo, -12);
                        break;
                    default:
                        text = getString(R.string.calendar_two_weeks);
                        timeFrom = DateTimeUtils.shiftDays(timeTo, -14);
                }

                battery_status_time_span_text.setText(text);
                battery_status_date_from_text.setText(DateTimeUtils.formatDate(new Date(timeFrom * 1000L)));
                battery_status_date_to_text.setText(DateTimeUtils.formatDate(new Date(timeTo * 1000L)));
                batteryInfoChartFragment.setDateAndGetData(gbDevice, timeFrom, timeTo);
            }


            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        //Button battery_status_calendar_button = findViewById(R.id.battery_status_calendar_button);
        battery_status_date_to_layout.setOnClickListener(new View.OnClickListener() {
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
                            battery_status_date_to_text.setText(DateTimeUtils.formatDate(new Date(timeTo * 1000L)));
                            battery_status_time_span_seekbar.setProgress(0);
                            battery_status_time_span_seekbar.setProgress(1);

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


