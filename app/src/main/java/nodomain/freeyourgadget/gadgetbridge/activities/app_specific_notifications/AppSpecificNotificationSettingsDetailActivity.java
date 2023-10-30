/*
 *   Copyright (C) 2023 akasaka / Genjitsu Labs
 *
 *     This file is part of Gadgetbridge.
 *
 *     Gadgetbridge is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published
 *     by the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Gadgetbridge is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package nodomain.freeyourgadget.gadgetbridge.activities.app_specific_notifications;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.adapter.AppSpecificNotificationSettingsAppListAdapter;
import nodomain.freeyourgadget.gadgetbridge.database.AppSpecificNotificationSettingsRepository;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.entities.AppSpecificNotificationSetting;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.AbstractNotificationPattern;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;

public class AppSpecificNotificationSettingsDetailActivity extends AbstractGBActivity {
    private AppSpecificNotificationSettingsRepository repository = null;
    private String bundleId = null;

    private GBDevice mDevice;
    private DeviceCoordinator mCoordinator;

    private List<String> mLedPatternValues = new ArrayList<>();
    private List<String> mVibrationPatternValues = new ArrayList<>();
    private List<String> mVibrationCountValues = new ArrayList<>();

    private Spinner mSpinnerLedPattern;
    private Spinner mSpinnerVibrationPattern;
    private Spinner mSpinnerVibrationCount;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_per_app_setting_detail);
        Button mButtonSave = findViewById(R.id.buttonSaveSettings);
        Button mButtonDelete = findViewById(R.id.buttonDeleteSettings);
        mSpinnerLedPattern = findViewById(R.id.spinnerLedType);
        mSpinnerVibrationPattern = findViewById(R.id.spinnerVibraType);
        mSpinnerVibrationCount = findViewById(R.id.spinnerVibraCount);

        mDevice = getIntent().getParcelableExtra(GBDevice.EXTRA_DEVICE);
        mCoordinator = mDevice.getDeviceCoordinator();

        mLedPatternValues.clear();
        for(AbstractNotificationPattern p: mCoordinator.getNotificationLedPatterns())
            mLedPatternValues.add(p.getValue());

        mVibrationPatternValues.clear();
        for(AbstractNotificationPattern p: mCoordinator.getNotificationVibrationPatterns())
            mVibrationPatternValues.add(p.getValue());

        mVibrationCountValues.clear();
        for(AbstractNotificationPattern p: mCoordinator.getNotificationVibrationRepetitionPatterns())
            mVibrationCountValues.add(p.getValue());

        if(!mCoordinator.supportsNotificationLedPatterns()) {
            mSpinnerLedPattern.setEnabled(false);
        } else {
            mSpinnerLedPattern.setAdapter(
                    createAdapterFromArrayAddingDefault(mCoordinator.getNotificationLedPatterns())
            );
        }

        if(!mCoordinator.supportsNotificationVibrationPatterns()) {
            mSpinnerVibrationPattern.setEnabled(false);
        } else {
            mSpinnerVibrationPattern.setAdapter(
                    createAdapterFromArrayAddingDefault(mCoordinator.getNotificationVibrationPatterns())
            );
        }

        if(!mCoordinator.supportsNotificationVibrationRepetitionPatterns()) {
            mSpinnerVibrationCount.setEnabled(false);
        } else {
            mSpinnerVibrationCount.setAdapter(
                    createAdapterFromArrayAddingDefault(mCoordinator.getNotificationVibrationRepetitionPatterns())
            );
        }

        String title = getIntent().getStringExtra(AppSpecificNotificationSettingsAppListAdapter.STRING_EXTRA_PACKAGE_TITLE);
        setTitle(title);
        bundleId = getIntent().getStringExtra(AppSpecificNotificationSettingsAppListAdapter.STRING_EXTRA_PACKAGE_NAME);

        repository = new AppSpecificNotificationSettingsRepository(mDevice);
        mButtonDelete.setOnClickListener(view -> {
            repository.setSettingsForAppId(bundleId, null);
            finish();
        });

        mButtonSave.setOnClickListener(view -> {
            saveSettings();
            finish();
        });

        AppSpecificNotificationSetting setting = repository.getSettingsForAppId(bundleId);
        if(setting != null) {
            if(setting.getLedPattern() != null) {
                int idx = mLedPatternValues.indexOf(setting.getLedPattern());
                if(idx >= 0) {
                    mSpinnerLedPattern.setSelection(idx + 1);
                }
            } else {
                mSpinnerLedPattern.setSelection(0);
            }

            if(setting.getVibrationPattern() != null) {
                int idx = mVibrationPatternValues.indexOf(setting.getVibrationPattern());
                if(idx >= 0) {
                    mSpinnerVibrationPattern.setSelection(idx + 1);
                }
            } else {
                mSpinnerVibrationPattern.setSelection(0);
            }

            if(setting.getVibrationRepetition() != null) {
                int idx = mVibrationCountValues.indexOf(setting.getVibrationRepetition());
                if(idx >= 0) {
                    mSpinnerVibrationCount.setSelection(idx + 1);
                }
            } else {
                mSpinnerVibrationCount.setSelection(0);
            }
        }
    }

    private ArrayAdapter<String> createAdapterFromArrayAddingDefault(AbstractNotificationPattern[] array) {
        List<String> allOptions = new ArrayList<>();
        allOptions.add(getString(R.string.pref_default));
        for(AbstractNotificationPattern s: array) allOptions.add(s.getUserReadableName(getApplicationContext()));

        return new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, allOptions);
    }

    private void saveSettings() {
        String led = null;
        String vibra = null;
        String vibraTimes = null;

        if(mSpinnerLedPattern.getSelectedItemPosition() > 0) {
            led = mLedPatternValues.get(mSpinnerLedPattern.getSelectedItemPosition() - 1);
        }

        if(mSpinnerVibrationPattern.getSelectedItemPosition() > 0) {
            vibra = mVibrationPatternValues.get(mSpinnerVibrationPattern.getSelectedItemPosition() - 1);
        }

        if(mSpinnerVibrationCount.getSelectedItemPosition() > 0) {
            vibraTimes = mVibrationCountValues.get(mSpinnerVibrationCount.getSelectedItemPosition() - 1);
        }

        AppSpecificNotificationSetting setting = new AppSpecificNotificationSetting(
                bundleId,
                0,
                led,
                vibra,
                vibraTimes
        );
        repository.setSettingsForAppId(bundleId, setting);
    }
}
