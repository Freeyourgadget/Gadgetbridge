/*  Copyright (C) 2020-2024 Andreas Shimokawa

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

package nodomain.freeyourgadget.gadgetbridge.externalevents;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.model.Weather;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class TinyWeatherForecastGermanyReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                try {
                    WeatherSpec weatherSpec = bundle.getParcelable("WeatherSpec");
                    if (weatherSpec != null) {
                        ArrayList<WeatherSpec> weatherSpecs = new ArrayList<>(Collections.singletonList(weatherSpec));
                        weatherSpec.timestamp = (int) (System.currentTimeMillis() / 1000);
                        Weather.getInstance().setWeatherSpec(weatherSpecs);
                        GBApplication.deviceService().onSendWeather(weatherSpecs);
                    }
                } catch (Exception e) {
                    GB.toast("Gadgetbridge received broken or incompatible weather data", Toast.LENGTH_SHORT, GB.ERROR, e);
                }
            }
        }
    }
}