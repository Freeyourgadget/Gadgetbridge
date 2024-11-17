/*  Copyright (C) 2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.realme;

import android.util.Pair;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.oppo.OppoHeadphonesCoordinator;
import nodomain.freeyourgadget.gadgetbridge.service.devices.oppo.commands.TouchConfigSide;
import nodomain.freeyourgadget.gadgetbridge.service.devices.oppo.commands.TouchConfigType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.oppo.commands.TouchConfigValue;

public class RealmeBudsT110Coordinator extends OppoHeadphonesCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("realme Buds T110", Pattern.LITERAL);
    }

    @Override
    public String getManufacturer() {
        return "Realme";
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_realme_buds_t110;
    }

    @Override
    protected Map<Pair<TouchConfigSide, TouchConfigType>, List<TouchConfigValue>> getTouchOptions() {
        return new LinkedHashMap<Pair<TouchConfigSide, TouchConfigType>, List<TouchConfigValue>>() {{
            final List<TouchConfigValue> options = Arrays.asList(
                    TouchConfigValue.OFF,
                    TouchConfigValue.PLAY_PAUSE,
                    TouchConfigValue.PREVIOUS,
                    TouchConfigValue.NEXT,
                    TouchConfigValue.VOLUME_UP,
                    TouchConfigValue.VOLUME_DOWN,
                    TouchConfigValue.VOICE_ASSISTANT_REALME
            );
            put(Pair.create(TouchConfigSide.LEFT, TouchConfigType.TAP_2), options);
            put(Pair.create(TouchConfigSide.LEFT, TouchConfigType.TAP_3), options);
            put(Pair.create(TouchConfigSide.LEFT, TouchConfigType.HOLD), options);
            put(Pair.create(TouchConfigSide.RIGHT, TouchConfigType.TAP_2), options);
            put(Pair.create(TouchConfigSide.RIGHT, TouchConfigType.TAP_3), options);
            put(Pair.create(TouchConfigSide.RIGHT, TouchConfigType.HOLD), options);
            put(Pair.create(TouchConfigSide.BOTH, TouchConfigType.HOLD), Arrays.asList(
                    TouchConfigValue.OFF,
                    TouchConfigValue.GAME_MODE
            ));
        }};
    }
}
