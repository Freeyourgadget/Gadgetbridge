/*  Copyright (C) 2015-2018 Andreas Shimokawa

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

package nodomain.freeyourgadget.gadgetbridge.devices.huami.miband3;

import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiService.DISPLAY_ITEM_BIT_CLOCK;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiService.ENDPOINT_DISPLAY;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiService.ENDPOINT_DISPLAY_ITEMS;

public class MiBand3Service {
    public static final byte[] COMMAND_CHANGE_SCREENS = new byte[]{ENDPOINT_DISPLAY_ITEMS, DISPLAY_ITEM_BIT_CLOCK, 0x30, 0x00, 0x00, 0x00, 0x06, 0x00, 0x00, 0x00};
    public static final byte[] COMMAND_ENABLE_BAND_SCREEN_UNLOCK = new byte[]{ENDPOINT_DISPLAY, 0x16, 0x00, 0x01};
    public static final byte[] COMMAND_DISABLE_BAND_SCREEN_UNLOCK = new byte[]{ENDPOINT_DISPLAY, 0x16, 0x00, 0x00};
    public static final byte[] COMMAND_NIGHT_MODE_OFF = new byte[]{0x1a, 0x00};
    public static final byte[] COMMAND_NIGHT_MODE_SUNSET = new byte[]{0x1a, 0x02};
    public static final byte[] COMMAND_NIGHT_MODE_SCHEDULED = new byte[]{0x1a, 0x01, 0x10, 0x00, 0x07, 0x00};
}
