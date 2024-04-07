/*  Copyright (C) 2023-2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.xiaomi;

import androidx.annotation.StringRes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiPreferences;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class XiaomiWorkoutType {
    private final int code;
    private final String name;

    public XiaomiWorkoutType(final int code, final String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    @StringRes
    public static int mapWorkoutName(final int code) {
        switch (code) {
            case 1:
                return R.string.activity_type_outdoor_running;
            case 2:
                return R.string.activity_type_walking;
            case 3:
                return R.string.activity_type_hiking;
            case 4:
                return R.string.activity_type_trekking;
            case 5:
                return R.string.activity_type_trail_run;
            case 6:
                return R.string.activity_type_outdoor_cycling;
            case 501:
                return R.string.activity_type_wrestling;
        }

        return -1;
    }

    public static Collection<XiaomiWorkoutType> getWorkoutTypesSupportedByDevice(final GBDevice device) {
        final Prefs prefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(device.getAddress()));
        final List<String> codes = prefs.getList(XiaomiPreferences.PREF_WORKOUT_TYPES, Collections.emptyList());
        final List<XiaomiWorkoutType> ret = new ArrayList<>(codes.size());

        for (final String code : codes) {
            final int codeInt = Integer.parseInt(code);
            final int codeNameStringRes = XiaomiWorkoutType.mapWorkoutName(codeInt);
            ret.add(new XiaomiWorkoutType(
                    codeInt,
                    codeNameStringRes != -1 ?
                            GBApplication.getContext().getString(codeNameStringRes) :
                            GBApplication.getContext().getString(R.string.widget_unknown_workout, code)
            ));
        }

        return ret;
    }
}
