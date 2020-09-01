package nodomain.freeyourgadget.gadgetbridge.devices.sonyswr12;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.appcompat.widget.SwitchCompat;

import java.util.Arrays;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.SonySWR12Constants;

public class SonySWR12PrefActivity extends AbstractGBActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sonyswr12_settings);
        setTitle(getString(R.string.sonyswr12_settings_title));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }
        setVibrationSetting();
        setStaminaSetting();
        setAlarmIntervalSetting();
        GBDevice device = GBApplication.app().getDeviceManager().getSelectedDevice();
        int disablerVisibility = (device != null
                && device.isConnected()
                && device.getType() == DeviceType.SONY_SWR12) ? View.GONE : View.VISIBLE;
        findViewById(R.id.settingsDisabler).setVisibility(disablerVisibility);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void setVibrationSetting() {
        boolean isLow = GBApplication.getPrefs().getBoolean(SonySWR12Constants.VIBRATION_PREFERENCE, false);
        SwitchCompat switchVibration = ((SwitchCompat) findViewById(R.id.lowVibration));
        switchVibration.setChecked(isLow);
        switchVibration.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                GBApplication.getPrefs().getPreferences().edit()
                        .putBoolean(SonySWR12Constants.VIBRATION_PREFERENCE, isChecked).apply();
                GBApplication.deviceService().onSendConfiguration(SonySWR12Constants.VIBRATION_PREFERENCE);
            }
        });
    }

    private void setStaminaSetting() {
        boolean isOn = GBApplication.getPrefs().getBoolean(SonySWR12Constants.STAMINA_PREFERENCE, false);
        SwitchCompat switchStamina = ((SwitchCompat) findViewById(R.id.staminaOn));
        switchStamina.setChecked(isOn);
        switchStamina.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                GBApplication.getPrefs().getPreferences().edit()
                        .putBoolean(SonySWR12Constants.STAMINA_PREFERENCE, isChecked).apply();
                GBApplication.deviceService().onSendConfiguration(SonySWR12Constants.STAMINA_PREFERENCE);
            }
        });
    }

    private void setAlarmIntervalSetting() {
        String interval = GBApplication.getPrefs().getString(SonySWR12Constants.SMART_ALARM_INTERVAL_PREFERENCE, "0");
        List<String> intervalsArray = Arrays.asList(GBApplication.getContext().getResources().getStringArray(R.array.sonyswr12_smart_alarm_intervals));
        int position = intervalsArray.indexOf(interval);
        final AppCompatSpinner spinner = ((AppCompatSpinner) findViewById(R.id.smartAlarmSpinner));
        spinner.setSelection(position);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String interval = (String) spinner.getItemAtPosition(position);
                GBApplication.getPrefs().getPreferences().edit()
                        .putString(SonySWR12Constants.SMART_ALARM_INTERVAL_PREFERENCE, interval).apply();
                GBApplication.deviceService().onSendConfiguration(SonySWR12Constants.SMART_ALARM_INTERVAL_PREFERENCE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }
}
