/*  Copyright (C) 2021-2024 Daniel Dakhno

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
package nodomain.freeyourgadget.gadgetbridge.devices.vesc;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vesc.VescDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class VescControlActivity extends AbstractGBActivity {
    private static final String TAG = "VescControlActivity";
    private boolean volumeKeyPressed = false;
    private boolean volumeKeysControl = false;
    private int currentRPM = 0;
    private int currentBreakCurrentMa = 0;
    LocalBroadcastManager localBroadcastManager;

    private Logger logger = LoggerFactory.getLogger(getClass());

    EditText rpmEditText, breakCurrentEditText;

    private final int DELAY_SAVE = 1000;

    Prefs preferences;

    final String PREFS_KEY_LAST_RPM = "VESC_LAST_RPM";
    final String PREFS_KEY_LAST_BREAK_CURRENT = "VESC_LAST_BREAK_CURRENT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vesc_control);

        localBroadcastManager = LocalBroadcastManager.getInstance(this);

        preferences = GBApplication.getPrefs();

        initViews();

        restoreValues();
    }

    private void restoreValues(){
        rpmEditText.setText(String.valueOf(preferences.getInt(PREFS_KEY_LAST_RPM, 0)));
        breakCurrentEditText.setText(String.valueOf(preferences.getInt(PREFS_KEY_LAST_BREAK_CURRENT, 0) / 1000));
    }

    @Override
    protected void onPause() {
        super.onPause();
        setCurrent(0);
    }

    private boolean handleKeyPress(int keyCode, boolean isPressed) {
        if (!volumeKeysControl) {
            return false;
        }

        if (keyCode != 24 && keyCode != 25) {
            return false;
        }

        if (volumeKeyPressed == isPressed) {
            return true;
        }
        volumeKeyPressed = isPressed;

        logger.debug("volume " + (keyCode == 25 ? "down" : "up") + (isPressed ? " pressed" : " released"));
        if (!isPressed) {
            setCurrent(0);
            return true;
        }
        if (keyCode == 24) {
            setRPM(currentRPM);
        } else {
            setBreakCurrent(VescControlActivity.this.currentBreakCurrentMa);
        }

        return true;
    }

    Runnable rpmSaveRunnable = new Runnable() {
        @Override
        public void run() {
            preferences.getPreferences().edit().putInt(PREFS_KEY_LAST_RPM, currentRPM).apply();
        }
    };

    Runnable breakCurrentSaveRunnable = new Runnable() {
        @Override
        public void run() {
            preferences.getPreferences().edit().putInt(PREFS_KEY_LAST_BREAK_CURRENT, currentBreakCurrentMa).apply();
        }
    };

    private void initViews() {
        ((CheckBox) findViewById(R.id.vesc_control_checkbox_volume_keys))
                .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        VescControlActivity.this.volumeKeysControl = isChecked;
                        if (!isChecked) {
                            setRPM(0);
                        }
                    }
                });

        rpmEditText = ((EditText) findViewById(R.id.vesc_control_input_rpm));
        rpmEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                rpmEditText.removeCallbacks(rpmSaveRunnable);
                rpmEditText.postDelayed(rpmSaveRunnable, DELAY_SAVE);

                String text = s.toString();
                if (text.isEmpty()) {
                    currentRPM = 0;
                    return;
                }
                VescControlActivity.this.currentRPM = Integer.parseInt(text);
            }
        });

        breakCurrentEditText = ((EditText) findViewById(R.id.vesc_control_input_break_current));
        breakCurrentEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                breakCurrentEditText.removeCallbacks(breakCurrentSaveRunnable);
                breakCurrentEditText.postDelayed(breakCurrentSaveRunnable, DELAY_SAVE);

                String text = s.toString();
                if (text.isEmpty()) {
                    currentBreakCurrentMa = 0;
                    return;
                }
                try {
                    VescControlActivity.this.currentBreakCurrentMa = Integer.parseInt(text) * 1000;
                }catch (NumberFormatException e){
                    VescControlActivity.this.currentBreakCurrentMa = 0;
                }
            }
        });

        View.OnTouchListener controlTouchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (v.getId() == R.id.vesc_control_button_fwd) {
                        setRPM(VescControlActivity.this.currentRPM);
                    } else {
                        setBreakCurrent(VescControlActivity.this.currentBreakCurrentMa);
                    }
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    setCurrent(0);
                } else {
                    return false;
                }
                return true;
            }
        };

        findViewById(R.id.vesc_control_button_fwd).setOnTouchListener(controlTouchListener);
        findViewById(R.id.vesc_control_button_break).setOnTouchListener(controlTouchListener);
    }

    private void setBreakCurrent(int breakCurrentMa) {
        logger.debug("setting break current to {}", breakCurrentMa);
        Intent intent = new Intent(VescDeviceSupport.COMMAND_SET_BREAK_CURRENT);
        intent.putExtra(VescDeviceSupport.EXTRA_CURRENT, breakCurrentMa);
        sendLocalBroadcast(intent);
    }

    private void setCurrent(int currentMa) {
        logger.debug("setting current to {}", currentMa);
        Intent intent = new Intent(VescDeviceSupport.COMMAND_SET_CURRENT);
        intent.putExtra(VescDeviceSupport.EXTRA_CURRENT, currentMa);
        sendLocalBroadcast(intent);
    }

    private void setRPM(int rpm) {
        logger.debug("setting rpm to {}", rpm);
        Intent intent = new Intent(VescDeviceSupport.COMMAND_SET_RPM);
        intent.putExtra(VescDeviceSupport.EXTRA_RPM, rpm);
        sendLocalBroadcast(intent);
    }

    private void sendLocalBroadcast(Intent intent) {
        localBroadcastManager.sendBroadcast(intent);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return handleKeyPress(keyCode, false);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return handleKeyPress(keyCode, true);
    }
}
