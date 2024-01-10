/*  Copyright (C) 2023-2024 akasaka / Genjitsu Labs

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.settings;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.Wena3Packetable;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.settings.defines.DisplayDesign;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.settings.defines.DisplayOrientation;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.settings.defines.FontSize;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.settings.defines.Language;

public class DisplaySetting implements Wena3Packetable {
    public final boolean displayOnRaiseWrist;
    public final Language language;
    public final int displayDuration;
    public final DisplayOrientation orientation;
    public final DisplayDesign design;
    public final FontSize fontSize;
    public final boolean weatherInStatusBar;

    public DisplaySetting(boolean displayOnRaiseWrist, Language language, int displayDuration, DisplayOrientation orientation, DisplayDesign design, FontSize fontSize, boolean weatherInStatusBar) {
        this.displayOnRaiseWrist = displayOnRaiseWrist;
        this.language = language;
        this.displayDuration = displayDuration;
        this.orientation = orientation;
        this.design = design;
        this.fontSize = fontSize;
        this.weatherInStatusBar = weatherInStatusBar;
    }

    @Override
    public byte[] toByteArray() {
        return ByteBuffer
                .allocate(8)
                .order(ByteOrder.LITTLE_ENDIAN)
                .put((byte)0x1A)
                .put((byte) fontSize.ordinal())
                .put((byte) orientation.ordinal())
                .put((byte) language.ordinal())
                .put((byte) (displayOnRaiseWrist ? 0x1 : 0x0))
                .put((byte) design.ordinal())
                .put((byte) (weatherInStatusBar ? 0x1 : 0x0))
                .put((byte) displayDuration)
                .array();
    }
}

