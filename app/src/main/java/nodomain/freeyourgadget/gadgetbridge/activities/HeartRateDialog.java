package nodomain.freeyourgadget.gadgetbridge.activities;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;

public class HeartRateDialog extends Dialog {
    protected static final Logger LOG = LoggerFactory.getLogger(HeartRateDialog.class);
    LinearLayout heart_rate_dialog_results_layout;
    RelativeLayout heart_rate_dialog_loading_layout;

    TextView heart_rate_widget_hr_value;
    TextView heart_rate_widget_spo2_value;
    TextView heart_rate_widget_pressure_value;

    LinearLayout heart_rate_hr;
    LinearLayout heart_rate_spo2;
    LinearLayout heart_rate_pressure;

    TextView heart_rate_dialog_label;

    final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (Objects.requireNonNull(intent.getAction())) {
                case DeviceService.ACTION_REALTIME_SAMPLES:
                    setMeasurementResults(intent.getSerializableExtra(DeviceService.EXTRA_REALTIME_SAMPLE));
                    break;
                default:
                    LOG.info("ignoring intent action " + intent.getAction());
                    break;
            }
        }
    };

    public HeartRateDialog(@NonNull Context context) {
        super(context);
    }

    private void setMeasurementResults(Serializable result) {
        heart_rate_dialog_results_layout.setVisibility(View.VISIBLE);
        heart_rate_dialog_loading_layout.setVisibility(View.GONE);
        heart_rate_dialog_label.setText(getContext().getString(R.string.heart_rate_result));

        if (result instanceof ActivitySample) {
            ActivitySample sample = (ActivitySample) result;
            heart_rate_hr.setVisibility(View.VISIBLE);
            if (HeartRateUtils.getInstance().isValidHeartRateValue(sample.getHeartRate()))
                heart_rate_widget_hr_value.setText(String.valueOf(sample.getHeartRate()));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        IntentFilter filter = new IntentFilter();
        filter.addAction(DeviceService.ACTION_REALTIME_SAMPLES);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mReceiver, filter);
        getContext().registerReceiver(mReceiver, filter);

        setContentView(R.layout.heart_rate_dialog);
        heart_rate_dialog_results_layout = findViewById(R.id.heart_rate_dialog_results_layout);
        heart_rate_dialog_loading_layout = findViewById(R.id.heart_rate_dialog_loading_layout);

        heart_rate_hr = findViewById(R.id.heart_rate_measurements1);
        heart_rate_spo2 = findViewById(R.id.heart_rate_measurements2);
        heart_rate_pressure = findViewById(R.id.heart_rate_measurements3);

        TextView heart_rate_widget_hr_title = heart_rate_hr.findViewById(R.id.generic_widget_title);
        TextView heart_rate_widget_spo2_title = heart_rate_spo2.findViewById(R.id.generic_widget_title);
        TextView heart_rate_widget_pressure_title = heart_rate_pressure.findViewById(R.id.generic_widget_title);

        heart_rate_widget_hr_value = heart_rate_hr.findViewById(R.id.generic_widget_value);
        heart_rate_widget_spo2_value = heart_rate_spo2.findViewById(R.id.generic_widget_value);
        heart_rate_widget_pressure_value = heart_rate_pressure.findViewById(R.id.generic_widget_value);

        ImageView heart_rate_widget_hr_icon = heart_rate_hr.findViewById(R.id.generic_widget_icon);
        ImageView heart_rate_widget_spo2_icon = heart_rate_spo2.findViewById(R.id.generic_widget_icon);
        ImageView heart_rate_widget_pressure_icon = heart_rate_pressure.findViewById(R.id.generic_widget_icon);

        heart_rate_widget_hr_icon.setImageResource(R.drawable.ic_heart);
        heart_rate_widget_spo2_icon.setImageResource(R.drawable.ic_circle);
        heart_rate_widget_pressure_icon.setImageResource(R.drawable.ic_heartrate);

        heart_rate_hr.setVisibility(View.VISIBLE);
        heart_rate_spo2.setVisibility(View.GONE);
        heart_rate_pressure.setVisibility(View.GONE);

        heart_rate_widget_hr_title.setText(R.string.heart_rate);
        heart_rate_widget_spo2_title.setText(R.string.menuitem_spo2);
        heart_rate_widget_pressure_title.setText(R.string.blood_pressure);

        heart_rate_dialog_label = findViewById(R.id.heart_rate_dialog_title);
        heart_rate_dialog_results_layout.setVisibility(View.GONE);
        heart_rate_dialog_loading_layout.setVisibility(View.VISIBLE);

        setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mReceiver);
                getContext().unregisterReceiver(mReceiver);
            }
        });
    }
}