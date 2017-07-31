/*  Copyright (C) 2015-2017 Andreas Shimokawa, Carsten Pfeiffer, Lem Dulfo

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
package nodomain.freeyourgadget.gadgetbridge.activities;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.LocaleList;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;

import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager;
import nodomain.freeyourgadget.gadgetbridge.util.AndroidUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;


public class GBActivity extends AppCompatActivity {

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case GBApplication.ACTION_LANGUAGE_CHANGE:
                    setLanguage(GBApplication.getLanguage());
                    break;
                case GBApplication.ACTION_QUIT:
                    finish();
                    break;
            }
        }
    };

    private void setLanguage(Locale language) {
        AndroidUtils.setLanguage(this, language);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        IntentFilter filterLocal = new IntentFilter();
        filterLocal.addAction(GBApplication.ACTION_QUIT);
        filterLocal.addAction(GBApplication.ACTION_LANGUAGE_CHANGE);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filterLocal);

        if (GBApplication.isDarkThemeEnabled()) {
            setTheme(R.style.GadgetbridgeThemeDark);
        } else {
            setTheme(R.style.GadgetbridgeTheme);
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        super.onDestroy();
    }
}
