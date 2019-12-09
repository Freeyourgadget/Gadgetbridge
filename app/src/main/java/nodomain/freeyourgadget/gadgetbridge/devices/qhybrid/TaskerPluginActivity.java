/*  Copyright (C) 2019 Daniel Dakhno

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
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.devices.qhybrid;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.twofortyfouram.locale.sdk.client.ui.activity.AbstractPluginActivity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit.PlayNotificationRequest;

public class TaskerPluginActivity extends AbstractPluginActivity {
    public static final String key_hours = "qhybrid_hours";
    public static final String key_minute = "qhybrid_minutes";
    public static final String key_vibration = "qhybrid_vibration";

    RadioGroup group;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasker_plugin);

        group = findViewById(R.id.qhybrid_tasker_vibration);
        for(PlayNotificationRequest.VibrationType type : PlayNotificationRequest.VibrationType.values()){
            RadioButton button = new RadioButton(this);
            button.setText(type.name() + " (" + type.name() + ")");
            button.setId(type.getValue());
            group.addView(button);
        }
        group.check(PlayNotificationRequest.VibrationType.NO_VIBE.getValue());
        RadioButton custom = new RadioButton(this);
        custom.setText("variable %vibration");
        custom.setId(10);
        group.addView(custom);

        Intent intent = getIntent();
        if(intent.hasExtra(key_hours)){
            ((TextView) findViewById(R.id.qhybrid_hour_degrees)).setText(intent.getStringExtra(key_hours));
        }
        if(intent.hasExtra(key_minute)){
            ((TextView) findViewById(R.id.qhybrid_minute_degrees)).setText(intent.getStringExtra(key_minute));
        }
        if(intent.hasExtra(key_vibration)){
            String vibe = intent.getStringExtra(key_vibration);
            if(vibe.equals("%vibration")){
                group.check(10);
            }else {
                group.check(Integer.parseInt(vibe));
            }
        }
    }

    @Override
    public boolean isBundleValid(@NonNull Bundle bundle) {
        return true;
    }

    @Override
    public void onPostCreateWithPreviousResult(@NonNull Bundle bundle, @NonNull String s) {

    }

    @Nullable
    @Override
    public Bundle getResultBundle() {
        int vibration = group.getCheckedRadioButtonId();

        Bundle bundle = new Bundle();
        bundle.putString(key_hours, ((EditText) findViewById(R.id.qhybrid_hour_degrees)).getText().toString());
        bundle.putString(key_minute, ((EditText) findViewById(R.id.qhybrid_minute_degrees)).getText().toString());

        if(vibration == 10){
            bundle.putString(key_vibration, "%vibration");
        }else{
            bundle.putString(key_vibration, String.valueOf(vibration));
        }
        TaskerPlugin.Setting.setVariableReplaceKeys(bundle, new String[]{key_hours, key_minute, key_vibration});

        return bundle;
    }

    @NonNull
    @Override
    public String getResultBlurb(@NonNull Bundle bundle) {
        return "nope";
    }
}
