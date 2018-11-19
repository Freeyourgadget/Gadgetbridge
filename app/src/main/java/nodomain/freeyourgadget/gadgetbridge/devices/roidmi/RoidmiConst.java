/*  Copyright (C) 2018 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.roidmi;

import android.graphics.Color;

public class RoidmiConst {
    public static final String ACTION_GET_LED_COLOR = "roidmi_get_led_color";
    public static final String ACTION_GET_FM_FREQUENCY = "roidmi_get_frequency";
    public static final String ACTION_GET_VOLTAGE = "roidmi_get_voltage";

    public static final int[] COLOR_PRESETS = new int[]{
            Color.rgb(0xFF, 0x00, 0x00), // red
            Color.rgb(0x00, 0xFF, 0x00), // green
            Color.rgb(0x00, 0x00, 0xFF), // blue
            Color.rgb(0xFF, 0xFF, 0x01), // yellow
            Color.rgb(0x00, 0xAA, 0xE5), // sky blue
            Color.rgb(0xF0, 0x6E, 0xAA), // pink
            Color.rgb(0xFF, 0xFF, 0xFF), // white
            Color.rgb(0x00, 0x00, 0x00), // black
    };
}
